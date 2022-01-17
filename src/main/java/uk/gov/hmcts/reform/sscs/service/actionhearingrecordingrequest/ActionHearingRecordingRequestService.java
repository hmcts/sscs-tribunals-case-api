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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.validation.constraints.NotNull;
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
        return r -> (PartyItemList.OTHER_PARTY.equals(party) || PartyItemList.OTHER_PARTY_REPRESENTATIVE.equals(party))
                ? (r.getValue().getRequestingParty().equals(party.getCode()) && otherPartyId.equals(r.getValue().getOtherPartyId()))
                : r.getValue().getRequestingParty().equals(party.getCode());
    }

    public Optional<RequestStatus> getChangedRequestStatus(PartyItemList party, String otherPartyId, ProcessHearingRecordingRequest processHearingRecordingRequest, List<OtherPartyHearingRecordingReqUi> otherPartyHearingRecordingReqUi) {
        switch (party) {
            case DWP:
                return toRequestStatus(Optional.ofNullable(processHearingRecordingRequest.getDwp()));
            case JOINT_PARTY:
                return toRequestStatus(Optional.ofNullable(processHearingRecordingRequest.getJointParty()));
            case REPRESENTATIVE:
                return toRequestStatus(Optional.ofNullable(processHearingRecordingRequest.getRep()));
            case OTHER_PARTY:
                return toRequestStatus(getOtherPartyRequest(otherPartyId, otherPartyHearingRecordingReqUi));
            case OTHER_PARTY_REPRESENTATIVE:
                return toRequestStatus(getOtherPartyRequest(otherPartyId, otherPartyHearingRecordingReqUi));
            case APPELLANT: default:
                return toRequestStatus(Optional.ofNullable(processHearingRecordingRequest.getAppellant()));
        }
    }

    private Optional<DynamicList> getOtherPartyRequest(String otherPartyId, List<OtherPartyHearingRecordingReqUi> otherPartyHearingRecordingReqUi) {
        return otherPartyHearingRecordingReqUi.stream()
                .map(OtherPartyHearingRecordingReqUi::getValue)
                .filter(r -> r.getOtherPartyId().equals(otherPartyId))
                .map(OtherPartyHearingRecordingReqUiDetails::getHearingRecordingStatus)
                .map(Optional::ofNullable)
                .findFirst()
                .flatMap(Function.identity());
    }

    private Optional<RequestStatus> toRequestStatus(Optional<DynamicList> dynamicList) {
        if (dynamicList.isPresent() && dynamicList.get().getValue() != null && dynamicList.get().getValue().getCode() != null) {
            return Arrays.stream(RequestStatus.values())
                    .filter(s -> s.getLabel().equals(dynamicList.get().getValue().getCode()))
                    .findFirst();
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
