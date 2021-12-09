package uk.gov.hmcts.reform.sscs.service.actionhearingrecordingrequest;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.GRANTED;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REFUSED;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REQUESTED;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.validation.constraints.NotNull;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.model.RequestStatus;

@Service
public class ActionHearingRecordingRequestService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter RESULT_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public Optional<RequestStatus> getRequestStatus(PartyItemList party, String otherPartyId, Hearing hearing, SscsCaseData sscsCaseData) {
        return hasGrantedHearingRecordings(party, otherPartyId, hearing, sscsCaseData)
                .or(() -> hasRefusedHearingRecordings(party, otherPartyId, hearing, sscsCaseData))
                .or(() -> hasRequestedHearingRecordings(party, otherPartyId, hearing, sscsCaseData));
    }

    private Optional<RequestStatus> hasRequestedHearingRecordings(PartyItemList party, String otherPartyId, Hearing hearing, SscsCaseData sscsCaseData) {
        boolean hasRequestedHearingRecordings = hasHearingRequestInCollection(party, otherPartyId, hearing, sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings());
        return hasRequestedHearingRecordings ? Optional.of(REQUESTED) : Optional.empty();
    }

    private Optional<RequestStatus> hasGrantedHearingRecordings(PartyItemList party, String otherPartyId, Hearing hearing, SscsCaseData sscsCaseData) {
        List<HearingRecordingRequest> releasedHearings = (party == PartyItemList.DWP) ? sscsCaseData.getSscsHearingRecordingCaseData().getDwpReleasedHearings() : sscsCaseData.getSscsHearingRecordingCaseData().getCitizenReleasedHearings();
        boolean hasGrantedHearingRecordings = hasHearingRequestInCollection(party, otherPartyId, hearing, releasedHearings);
        return hasGrantedHearingRecordings ? Optional.of(GRANTED) : Optional.empty();
    }

    private Optional<RequestStatus> hasRefusedHearingRecordings(PartyItemList party, String otherPartyId, Hearing hearing, SscsCaseData sscsCaseData) {
        boolean hasRefusedHearings = hasHearingRequestInCollection(party, otherPartyId, hearing, sscsCaseData.getSscsHearingRecordingCaseData().getRefusedHearings());
        return hasRefusedHearings ? Optional.of(REFUSED) : Optional.empty();
    }

    private boolean hasHearingRequestInCollection(PartyItemList party, String otherPartyId, Hearing hearing, List<HearingRecordingRequest> hearingRecordingCollection) {
        return emptyIfNull(hearingRecordingCollection).stream()
                .filter(isRequestingParty(party, otherPartyId))
                .filter(hr -> nonNull(hr.getValue().getSscsHearingRecording()))
                .filter(hr -> nonNull(hr.getValue().getSscsHearingRecording().getHearingId()))
                .anyMatch(hr -> hr.getValue().getSscsHearingRecording().getHearingId().equals(hearing.getValue().getHearingId()));
    }

    private Predicate<? super HearingRecordingRequest> isRequestingParty(PartyItemList party, String otherPartyId) {
        return r -> (PartyItemList.OTHER_PARTY.equals(party) || PartyItemList.OTHER_PARTY_REPRESENTATIVE.equals(party)) ?
                (r.getValue().getRequestingParty().equals(party.getCode()) && otherPartyId.equals(r.getValue().getOtherPartyId())) :
                r.getValue().getRequestingParty().equals(party.getCode());
    }

    public Optional<RequestStatus> getChangedRequestStatus(PartyItemList party, String otherPartyId, ProcessHearingRecordingRequest processHearingRecordingRequest) {
        switch (party) {
            case DWP:
                return toRequestStatus(processHearingRecordingRequest.getDwp());
            case JOINT_PARTY:
                return toRequestStatus(processHearingRecordingRequest.getJointParty());
            case REPRESENTATIVE:
                return toRequestStatus(processHearingRecordingRequest.getRep());
            case OTHER_PARTY:
                return toRequestStatus(getOtherPartyRequest(otherPartyId, processHearingRecordingRequest), PartyItemList.OTHER_PARTY);
            case OTHER_PARTY_REPRESENTATIVE:
                return toRequestStatus(getOtherPartyRequest(otherPartyId, processHearingRecordingRequest), PartyItemList.OTHER_PARTY_REPRESENTATIVE);
            case APPELLANT: default:
                return toRequestStatus(processHearingRecordingRequest.getAppellant());
        }
    }

    private Optional<OtherPartyRequest> getOtherPartyRequest(String otherPartyId, ProcessHearingRecordingRequest processHearingRecordingRequest) {
        return processHearingRecordingRequest.getValue().getOtherPartyRequests().stream()
                .map(CcdValue::getValue)
                .filter(r -> r.getOtherParty().getId().equals(otherPartyId))
                .findFirst();
    }

    private Optional<RequestStatus> toRequestStatus(DynamicList dynamicList) {
        if (dynamicList != null && dynamicList.getValue() != null && dynamicList.getValue().getCode() != null) {
            return Arrays.stream(RequestStatus.values())
                    .filter(s -> s.getLabel().equals(dynamicList.getValue().getCode()))
                    .findFirst();
        }
        return Optional.empty();
    }

    private Optional<RequestStatus> toRequestStatus(Optional<OtherPartyRequest> request, PartyItemList party) {
        if (request.isPresent()) {
            DynamicList dynamicList = PartyItemList.OTHER_PARTY.equals(party) ? request.get().getRequestStatus() : request.get().getRepRequestStatus();
            if (dynamicList != null && dynamicList.getValue() != null && dynamicList.getValue().getCode() != null) {
                return Arrays.stream(RequestStatus.values())
                        .filter(s -> s.getLabel().equals(dynamicList.getValue().getCode()))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    @NotNull
    public String getFormattedHearingInformation(Hearing hearing) {
        return hearing.getValue().getVenue().getName() + " "
                + checkHearingTime(hearing.getValue().getTime()) + " "
                + LocalDate.parse(hearing.getValue().getHearingDate(), FORMATTER).format(RESULT_FORMATTER);
    }

    @NotNull
    private String checkHearingTime(String hearingTime) {
        return (hearingTime.length() == 5) ? (hearingTime + ":00") : hearingTime;
    }
}
