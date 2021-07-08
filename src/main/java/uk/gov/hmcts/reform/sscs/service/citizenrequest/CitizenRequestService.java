package uk.gov.hmcts.reform.sscs.service.citizenrequest;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
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


    public CitizenRequestService(OnlineHearingService onlineHearingService, CcdService ccdService, IdamService idamService) {
        this.onlineHearingService = onlineHearingService;
        this.ccdService = ccdService;
        this.idamService = idamService;
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
        return caseDetails.map(sscsCase -> submitHearingRecordingRequest(sscsCase.getData(), sscsCase.getId(), hearingIds, user.getEmail())).orElse(false);
    }

    private HearingRecordingResponse mapToHearingRecording(SscsCaseData sscsCaseData, String idamEmail) {
        if (sscsCaseData.getHearings() == null || sscsCaseData.getHearings().isEmpty()) {
            return new HearingRecordingResponse();
        } else {
            PartyItemList party = workRequestedParty(sscsCaseData, idamEmail);

            List<HearingRecordingRequest> releasedHearingRecordings = sscsCaseData.getSscsHearingRecordingCaseData().getReleasedHearings();
            List<CitizenHearingRecording> releasedRecordings = CollectionUtils.isEmpty(releasedHearingRecordings) ? List.of() :
                    releasedHearingRecordings.stream()
                    .filter(request -> party.getCode().equals(request.getValue().getRequestingParty()))
                    .flatMap(request -> request.getValue().getSscsHearingRecordingList().stream())
                    .map(request -> populateCitizenHearingRecordings(request.getValue()))
                    .collect(Collectors.toList());

            List<HearingRecordingRequest> requestedHearingRecordings = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings();
            List<CitizenHearingRecording> requestedRecordings = CollectionUtils.isEmpty(requestedHearingRecordings) ? List.of() :
                    requestedHearingRecordings.stream()
                    .filter(request -> party.getCode().equals(request.getValue().getRequestingParty()))
                    .flatMap(request -> request.getValue().getSscsHearingRecordingList().stream())
                    .map(request -> populateCitizenHearingRecordings(request.getValue()))
                    .collect(Collectors.toList());

            List<String> allRequestedHearingIds = Stream.of(releasedRecordings, requestedRecordings)
                    .flatMap(Collection::stream)
                    .map(CitizenHearingRecording::getHearingId)
                    .collect(Collectors.toList());

            List<CitizenHearingRecording> requestabledRecordings = sscsCaseData.getHearings().stream()
                    .filter(hearing -> isHearingWithRecording(hearing, sscsCaseData.getSscsHearingRecordingCaseData()))
                    .filter(hearing -> !allRequestedHearingIds.contains(hearing.getValue().getHearingId()))
                    .map(this::populateCitizenHearingRecordings)
                    .collect(Collectors.toList());

            return new HearingRecordingResponse(releasedRecordings, requestedRecordings, requestabledRecordings);
        }
    }

    private boolean submitHearingRecordingRequest(SscsCaseData sscsCaseData, Long ccdCaseId, List<String> hearingIds, String idamEmail) {

        List<HearingRecordingRequest> newHearingRequests = new ArrayList<>();
        for (String hearingId : hearingIds) {
            List<SscsHearingRecording> sscsHearingRecordingList = sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings()
                    .stream().filter(r -> hearingId.equals(r.getValue().getHearingId())).collect(Collectors.toList());

            PartyItemList party = workRequestedParty(sscsCaseData, idamEmail);

            HearingRecordingRequest hearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                    .requestingParty(party.getCode()).status("requested")
                    .dateRequested(LocalDateTime.now().format(DateTimeFormatter.ofPattern(UPLOAD_DATE_FORMATTER)))
                    .sscsHearingRecordingList(sscsHearingRecordingList).build()).build();
            newHearingRequests.add(hearingRecordingRequest);
        }

        List<HearingRecordingRequest> hearingRecordingRequests = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings();
        if (hearingRecordingRequests == null) {
            hearingRecordingRequests = new ArrayList<>();
        }
        hearingRecordingRequests.addAll(newHearingRequests);

        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(hearingRecordingRequests);
        ccdService.updateCase(sscsCaseData, ccdCaseId, CITIZEN_REQUEST_HEARING_RECORDING.getCcdType(),
                "SSCS - hearing recording request from MYA",
                "Requested hearing recordings", idamService.getIdamTokens());
        return true;
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
                                .documentUrl(r.getValue().getDocumentUrl())
                                .documentBinaryUrl(r.getValue().getDocumentBinaryUrl())
                                .build())
                        .collect(Collectors.toList()))
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
        return party;
    }
}
