package uk.gov.hmcts.reform.sscs.service;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;

@Slf4j
@Service
public class HearingRecordingRequestService {

    public PreSubmitCallbackResponse<SscsCaseData> buildHearingRecordingUi(PreSubmitCallbackResponse<SscsCaseData> response, PartyItemList selectedPartyItem) {

        SscsCaseData sscsCaseData = response.getData();

        if (sscsCaseData.getHearings() == null) {
            return returnError(response, "There are no hearings on this case");
        }

        List<DynamicListItem> validHearings = sscsCaseData.getHearings().stream()
                .filter(hearing -> isHearingWithRecording(hearing, sscsCaseData.getSscsHearingRecordingCaseData()))
                .map(hearing -> new DynamicListItem(hearing.getValue().getHearingId(), selectHearing(hearing)))
                .collect(Collectors.toList());

        if (validHearings.isEmpty()) {
            return returnError(response, "There are no hearings with hearing recordings on this case");
        }

        List<HearingRecordingRequest> requestedHearingsCollection = emptyIfNull(sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings());
        StringBuilder requestedHearingText = new StringBuilder();
        StringBuilder releasedHearingText = new StringBuilder();

        List<HearingRecordingRequest> filteredRequestedHearingsCollection = requestedHearingsCollection.stream()
                .filter(r -> r.getValue().getRequestingParty().equals(selectedPartyItem.getCode())).collect(Collectors.toList());

        filteredRequestedHearingsCollection.forEach(r -> removeFromListAndAddText(r.getValue().getSscsHearingRecording(), validHearings, requestedHearingText));

        List<HearingRecordingRequest> releasedHearingsCollection = PartyItemList.DWP.getCode().equals(selectedPartyItem.getCode())
                ? emptyIfNull(sscsCaseData.getSscsHearingRecordingCaseData().getDwpReleasedHearings())
                : emptyIfNull(sscsCaseData.getSscsHearingRecordingCaseData().getCitizenReleasedHearings());

        List<HearingRecordingRequest> filteredReleasedHearingsCollection = releasedHearingsCollection.stream()
                .filter(r -> r.getValue().getRequestingParty().equals(selectedPartyItem.getCode())).collect(Collectors.toList());

        filteredReleasedHearingsCollection.forEach(r -> removeFromListAndAddText(r.getValue().getSscsHearingRecording(), validHearings, releasedHearingText));

        if (validHearings.isEmpty()) {
            return returnError(response, "There are no hearing recordings available for request");
        }

        sscsCaseData.getSscsHearingRecordingCaseData().setRequestableHearingDetails(new DynamicList(new DynamicListItem("", ""), validHearings));
        if (isNotEmpty(filteredRequestedHearingsCollection)) {
            sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearingsTextList(requestedHearingText.substring(0, requestedHearingText.length() - 2));
        } else {
            sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearingsTextList("There are no outstanding " + selectedPartyItem.getLabel() + " hearing recording requests on this case");
        }
        if (isNotEmpty(filteredReleasedHearingsCollection)) {
            sscsCaseData.getSscsHearingRecordingCaseData().setReleasedHearingsTextList(releasedHearingText.substring(0, releasedHearingText.length() - 2));
        } else {
            sscsCaseData.getSscsHearingRecordingCaseData().setReleasedHearingsTextList("No hearing recordings have been released to " + selectedPartyItem.getLabel() + " on this case");
        }
        return response;
    }

    private PreSubmitCallbackResponse<SscsCaseData> returnError(PreSubmitCallbackResponse<SscsCaseData> response, String message) {
        response.addError(message);
        return response;
    }

    private boolean isHearingWithRecording(Hearing hearing, SscsHearingRecordingCaseData hearingRecordingsData) {
        List<SscsHearingRecording> sscsHearingRecordings = hearingRecordingsData.getSscsHearingRecordings();

        if (sscsHearingRecordings != null) {
            return sscsHearingRecordings.stream().anyMatch(r -> r.getValue().getHearingId().equals(hearing.getValue().getHearingId()));
        }
        return false;
    }

    private void removeFromListAndAddText(SscsHearingRecordingDetails request, List<DynamicListItem> validHearings, StringBuilder stringBuilder) {
        stringBuilder.append(request.getVenue() + " " + request.getHearingDate());
        stringBuilder.append(", ");
        validHearings.stream().filter(i -> i.getCode().equals(request.getHearingId())).findFirst().ifPresent(validHearings::remove);
    }

    @NotNull
    private String selectHearing(Hearing hearing) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter resultFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        return hearing.getValue().getVenue().getName() + " "
                + hearing.getValue().getTime() + " "
                + LocalDate.parse(hearing.getValue().getHearingDate(), formatter).format(resultFormatter);
    }
}


