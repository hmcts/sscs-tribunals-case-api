package uk.gov.hmcts.reform.sscs.service.citizenrequest;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.stripUrl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.model.tya.CitizenHearingRecording;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecording;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecordingResponse;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;

@Slf4j
@Service
public class CitizenRequestService {
    private static final String UPLOAD_DATE_FORMATTER = "yyyy-MM-dd";

    private final OnlineHearingService onlineHearingService;
    private final CcdService ccdService;
    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;
    @Value("${feature.hearings-recording-request.case-updateV2.enabled:false}")
    private boolean hearingsRecordingReqCaseUpdateV2Enabled;

    public CitizenRequestService(OnlineHearingService onlineHearingService,
                                 CcdService ccdService,
                                 IdamService idamService,
                                 UpdateCcdCaseService updateCcdCaseService) {
        this.onlineHearingService = onlineHearingService;
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.updateCcdCaseService = updateCcdCaseService;
    }

    /**
     * Find hearing recordings for given case and release and outstanding hearing recording request made by the user.
     * @param identifier case id
     * @param authorisation user authorisation token
     * @return {code}HearingRecordingResponse that contains
      */
    public Optional<HearingRecordingResponse> findHearingRecordings(String identifier, String authorisation) {
        Optional<SscsCaseDetails> caseDetails = onlineHearingService.getCcdCaseByIdentifier(identifier);
        UserDetails user = idamService.getUserDetails(authorisation);
        return caseDetails.map(x -> mapToHearingRecording(x.getData(), user.getEmail()));
    }

    /**
     * Submit a new hearing recording request.
     * @param identifier case id
     * @param hearingIds requesting hearing id
     * @param authorisation user authorisation token
     * @return boolean
     */
    public boolean requestHearingRecordings(String identifier, List<String> hearingIds, String authorisation) {
        Optional<SscsCaseDetails> caseDetails = onlineHearingService.getCcdCaseByIdentifier(identifier);
        UserDetails user = idamService.getUserDetails(authorisation);

        if (hearingsRecordingReqCaseUpdateV2Enabled) {
            return caseDetails.map(sscsCase -> submitHearingRecordingRequest(sscsCase.getId(), hearingIds, user.getEmail())).orElse(false);
        } else {
            return caseDetails.map(sscsCase -> submitHearingRecordingRequest(sscsCase.getData(), sscsCase.getId(), hearingIds, user.getEmail())).orElse(false);
        }
    }

    private HearingRecordingResponse mapToHearingRecording(SscsCaseData sscsCaseData, String idamEmail) {
        if (sscsCaseData.getHearings() == null || sscsCaseData.getHearings().isEmpty()) {
            return new HearingRecordingResponse();
        } else {
            PartyItemList party = workRequestedParty(sscsCaseData, idamEmail);
            Optional<String> otherPartyId = getOtherPartyIdBySubscriptionEmail(sscsCaseData, idamEmail);

            List<HearingRecordingRequest> releasedHearingRecordings = sscsCaseData.getSscsHearingRecordingCaseData().getCitizenReleasedHearings();
            List<CitizenHearingRecording> releasedRecordings = CollectionUtils.isEmpty(releasedHearingRecordings) ? List.of() :
                    releasedHearingRecordings.stream()
                    .filter(hasRequestedByParty(party, otherPartyId))
                    .map(request -> populateCitizenHearingRecordings(request.getValue().getSscsHearingRecording()))
                    .toList();

            List<HearingRecordingRequest> requestedHearingRecordings = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings();
            List<CitizenHearingRecording> requestedRecordings = CollectionUtils.isEmpty(requestedHearingRecordings) ? List.of() :
                    requestedHearingRecordings.stream()
                    .filter(hasRequestedByParty(party, otherPartyId))
                    .map(request -> populateCitizenHearingRecordings(request.getValue().getSscsHearingRecording()))
                    .toList();

            List<String> allRequestedHearingIds = Stream.of(releasedRecordings, requestedRecordings)
                    .flatMap(Collection::stream)
                    .map(CitizenHearingRecording::getHearingId)
                    .toList();

            List<CitizenHearingRecording> requestableRecordings = sscsCaseData.getHearings().stream()
                    .filter(hearing -> isHearingWithRecording(hearing, sscsCaseData.getSscsHearingRecordingCaseData()))
                    .filter(hearing -> !allRequestedHearingIds.contains(hearing.getValue().getHearingId()))
                    .map(this::populateCitizenHearingRecordings)
                    .toList();

            return new HearingRecordingResponse(releasedRecordings, requestedRecordings, requestableRecordings);
        }
    }

    private Predicate<? super HearingRecordingRequest> hasRequestedByParty(PartyItemList party, Optional<String> otherPartyId) {
        return request -> party.getCode().equals(request.getValue().getRequestingParty())
                && (otherPartyId.isEmpty() || otherPartyId.get().equals(request.getValue().getOtherPartyId()));
    }

    private Optional<String> getOtherPartyIdBySubscriptionEmail(SscsCaseData sscsCaseData, String idamEmail) {
        return Stream.ofNullable(sscsCaseData.getOtherParties()).flatMap(Collection::stream)
                .map(CcdValue::getValue)
                .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getId(), getSubscriptionEmail(op.getOtherPartyAppointeeSubscription())) : null,
                        Pair.of(op.getId(), getSubscriptionEmail(op.getOtherPartySubscription())),
                        (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), getSubscriptionEmail(op.getOtherPartyRepresentativeSubscription())) : null))
                .filter(Objects::nonNull)
                .filter(p -> p.getLeft() != null && p.getRight() != null)
                .filter(p -> idamEmail.equals(p.getRight()))
                .map(Pair::getLeft)
                .findFirst();
    }

    @Nullable
    private String getSubscriptionEmail(Subscription subscription) {
        return subscription != null ? subscription.getEmail() : null;
    }

    private boolean submitHearingRecordingRequest(SscsCaseData sscsCaseData, Long ccdCaseId, List<String> hearingIds, String idamEmail) {
        log.info("Updating ccd case using updateCase for {} with event {}", ccdCaseId,
                EventType.CITIZEN_REQUEST_HEARING_RECORDING.getCcdType());
        updateCaseDataWithHearingRecordingRequest(sscsCaseData, hearingIds, idamEmail);

        ccdService.updateCase(sscsCaseData, ccdCaseId, CITIZEN_REQUEST_HEARING_RECORDING.getCcdType(),
                "SSCS - hearing recording request from MYA",
                "Requested hearing recordings", idamService.getIdamTokens());
        return true;
    }

    private boolean submitHearingRecordingRequest(Long ccdCaseId, List<String> hearingIds, String idamEmail) {
        log.info("Updating ccd case using v2 for {} with event {}", ccdCaseId,
                EventType.CITIZEN_REQUEST_HEARING_RECORDING.getCcdType());

        updateCcdCaseService.updateCaseV2(ccdCaseId, CITIZEN_REQUEST_HEARING_RECORDING.getCcdType(),
                "SSCS - hearing recording request from MYA",
                "Requested hearing recordings", idamService.getIdamTokens(),
                caseData -> updateCaseDataWithHearingRecordingRequest(caseData, hearingIds, idamEmail));

        return true;
    }

    private void updateCaseDataWithHearingRecordingRequest(SscsCaseData sscsCaseData, List<String> hearingIds, String idamEmail) {
        List<HearingRecordingRequest> newHearingRequests = new ArrayList<>();
        for (String hearingId : hearingIds) {
            Optional<SscsHearingRecording> sscsHearingRecording = sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings()
                    .stream().filter(r -> hearingId.equals(r.getValue().getHearingId())).findFirst();

            PartyItemList party = workRequestedParty(sscsCaseData, idamEmail);
            Optional<String> otherPartyId = getOtherPartyIdBySubscriptionEmail(sscsCaseData, idamEmail);

            if (sscsHearingRecording.isPresent()) {
                HearingRecordingRequest hearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                        .requestingParty(party.getCode())
                        .otherPartyId(otherPartyId.orElse(null))
                        .dateRequested(LocalDateTime.now().format(DateTimeFormatter.ofPattern(UPLOAD_DATE_FORMATTER)))
                        .sscsHearingRecording(sscsHearingRecording.get().getValue()).build()).build();
                newHearingRequests.add(hearingRecordingRequest);
            }
        }

        List<HearingRecordingRequest> hearingRecordingRequests = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings();
        if (hearingRecordingRequests == null) {
            hearingRecordingRequests = new ArrayList<>();
        }
        hearingRecordingRequests.addAll(newHearingRequests);

        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(hearingRecordingRequests);
        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordingRequestOutstanding(YesNo.YES);
    }

    private CitizenHearingRecording populateCitizenHearingRecordings(SscsHearingRecordingDetails recording) {
        return CitizenHearingRecording.builder()
                .hearingId(recording.getHearingId())
                .venue(recording.getVenue())
                .hearingDate(recording.getHearingDate())
                .hearingRecordings(recording.getRecordings()
                        .stream()
                        .map(r -> HearingRecording.builder()
                                .fileName(r.getValue().getDocumentFilename())
                                .fileType(getExtension(r.getValue().getDocumentFilename()))
                                .documentUrl(stripUrl(r.getValue().getDocumentBinaryUrl()))
                                .build())
                        .toList())
                .build();
    }

    private CitizenHearingRecording populateCitizenHearingRecordings(Hearing hearing) {
        return CitizenHearingRecording.builder()
                .hearingId(hearing.getValue().getHearingId())
                .hearingDate(hearing.getValue().getHearingDate())
                .venue(hearing.getValue().getVenue().getName())
                .build();
    }

    private boolean isHearingWithRecording(Hearing hearing, SscsHearingRecordingCaseData hearingRecordingsData) {
        List<SscsHearingRecording> sscsHearingRecordings = hearingRecordingsData.getSscsHearingRecordings();

        if (sscsHearingRecordings != null) {
            return sscsHearingRecordings.stream().anyMatch(r -> r.getValue().getHearingId().equals(hearing.getValue().getHearingId()));
        }
        return false;
    }

    @NotNull
    private PartyItemList workRequestedParty(SscsCaseData caseData, String idamEmail) {
        PartyItemList party = PartyItemList.APPELLANT;
        Subscriptions subscriptions = caseData.getSubscriptions();
        if (subscriptions != null) {
            Subscription repSubs = subscriptions.getRepresentativeSubscription();
            Subscription jpSubs = subscriptions.getJointPartySubscription();
            if (repSubs != null && idamEmail.equalsIgnoreCase(repSubs.getEmail())) {
                party = PartyItemList.REPRESENTATIVE;
            } else if (jpSubs != null && idamEmail.equalsIgnoreCase(jpSubs.getEmail())) {
                party = PartyItemList.JOINT_PARTY;
            }
        }

        Optional<PartyItemList> otherParty = findOtherPartyUserSubscriptionForUserEmail(caseData, idamEmail);

        return otherParty.orElse(party);
    }

    private Optional<PartyItemList> findOtherPartyUserSubscriptionForUserEmail(SscsCaseData sscsCaseData, String idamEmail) {
        return Stream.ofNullable(sscsCaseData.getOtherParties()).flatMap(Collection::stream)
                .map(CcdValue::getValue)
                .flatMap(o -> Stream.of(Pair.of(PartyItemList.OTHER_PARTY, getSubscriptionEmail(o.getOtherPartySubscription())),
                                Pair.of(PartyItemList.OTHER_PARTY, getSubscriptionEmail(o.getOtherPartyAppointeeSubscription())),
                                Pair.of(PartyItemList.OTHER_PARTY_REPRESENTATIVE, getSubscriptionEmail(o.getOtherPartyRepresentativeSubscription()))))
                .filter(p -> idamEmail.equalsIgnoreCase(p.getRight()))
                .map(Pair::getLeft)
                .findAny();
    }
}
