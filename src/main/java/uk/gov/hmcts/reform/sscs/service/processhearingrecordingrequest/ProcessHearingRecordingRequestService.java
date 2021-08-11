package uk.gov.hmcts.reform.sscs.service.processhearingrecordingrequest;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.GRANTED;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REFUSED;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REQUESTED;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.model.RequestStatus;

@Service
public class ProcessHearingRecordingRequestService {

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
                .flatMap(r -> r.getValue().getSscsHearingRecordingList().stream())
                .anyMatch(hr -> hr.getValue().getHearingId().equals(hearing.getValue().getHearingId()));
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
