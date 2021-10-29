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
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.model.RequestStatus;

@Service
public class ActionHearingRecordingRequestService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter RESULT_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public Optional<RequestStatus> getRequestStatus(PartyItemList party, Hearing hearing, SscsCaseData sscsCaseData) {
        return hasGrantedHearingRecordings(party, hearing, sscsCaseData)
                .or(() -> hasRefusedHearingRecordings(party, hearing, sscsCaseData))
                .or(() -> hasRequestedHearingRecordings(party, hearing, sscsCaseData));
    }

    private Optional<RequestStatus> hasRequestedHearingRecordings(PartyItemList party, Hearing hearing, SscsCaseData sscsCaseData) {
        boolean hasRequestedHearingRecordings = hasHearingRequestInCollection(party, hearing, sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings());
        return hasRequestedHearingRecordings ? Optional.of(REQUESTED) : Optional.empty();
    }

    private Optional<RequestStatus> hasGrantedHearingRecordings(PartyItemList party, Hearing hearing, SscsCaseData sscsCaseData) {
        List<HearingRecordingRequest> releasedHearings = (party == PartyItemList.DWP) ? sscsCaseData.getSscsHearingRecordingCaseData().getDwpReleasedHearings() : sscsCaseData.getSscsHearingRecordingCaseData().getCitizenReleasedHearings();
        boolean hasGrantedHearingRecordings = hasHearingRequestInCollection(party, hearing, releasedHearings);
        return hasGrantedHearingRecordings ? Optional.of(GRANTED) : Optional.empty();
    }

    private Optional<RequestStatus> hasRefusedHearingRecordings(PartyItemList party, Hearing hearing, SscsCaseData sscsCaseData) {
        boolean hasRefusedHearings = hasHearingRequestInCollection(party, hearing, sscsCaseData.getSscsHearingRecordingCaseData().getRefusedHearings());
        return hasRefusedHearings ? Optional.of(REFUSED) : Optional.empty();
    }

    private boolean hasHearingRequestInCollection(PartyItemList party, Hearing hearing, List<HearingRecordingRequest> hearingRecordingCollection) {
        return emptyIfNull(hearingRecordingCollection).stream()
                .filter(r -> r.getValue().getRequestingParty().equals(party.getCode()))
                .filter(hr -> nonNull(hr.getValue().getSscsHearingRecording()))
                .filter(hr -> nonNull(hr.getValue().getSscsHearingRecording().getHearingId()))
                .anyMatch(hr -> hr.getValue().getSscsHearingRecording().getHearingId().equals(hearing.getValue().getHearingId()));
    }

    public Optional<RequestStatus> getChangedRequestStatus(PartyItemList party, ProcessHearingRecordingRequest processHearingRecordingRequest) {
        switch (party) {
            case DWP:
                return toRequestStatus(processHearingRecordingRequest.getValue().getDwp());
            case JOINT_PARTY:
                return toRequestStatus(processHearingRecordingRequest.getValue().getJointParty());
            case REPRESENTATIVE:
                return toRequestStatus(processHearingRecordingRequest.getValue().getRep());
            case APPELLANT: default:
                return toRequestStatus(processHearingRecordingRequest.getValue().getAppellant());
        }
    }

    private Optional<RequestStatus> toRequestStatus(DynamicList dynamicList) {
        if (dynamicList != null && dynamicList.getValue() != null && dynamicList.getValue().getCode() != null) {
            return Arrays.stream(RequestStatus.values())
                    .filter(s -> s.getLabel().equals(dynamicList.getValue().getCode()))
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
