package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest;


import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.actionhearingrecordingrequest.ActionHearingRecordingRequestService;

@Service
@Slf4j
public class ActionHearingRecordingRequestAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ActionHearingRecordingRequestService actionHearingRecordingRequestService;

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static DateTimeFormatter resultFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Autowired
    public ActionHearingRecordingRequestAboutToStartHandler(ActionHearingRecordingRequestService actionHearingRecordingRequestService) {
        this.actionHearingRecordingRequestService = actionHearingRecordingRequestService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.ACTION_HEARING_RECORDING_REQUEST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (isEmpty(sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings())) {
            response.addError("No hearing recordings on this case");
            return response;
        }

        List<String> hearingIdsWithRecording = emptyIfNull(sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings()).stream()
                .map(d -> d.getValue().getHearingId())
                .distinct()
                .toList();

        List<Hearing> hearingsWithRecording = emptyIfNull(sscsCaseData.getHearings()).stream()
                .filter(hearing -> hearingIdsWithRecording.contains(hearing.getValue().getHearingId()))
                .toList();

        List<DynamicListItem> validHearingsDynamicList = hearingsWithRecording.stream()
                .map(hearing -> new DynamicListItem(hearing.getValue().getHearingId(), selectHearing(hearing)))
                .toList();

        sscsCaseData.getSscsHearingRecordingCaseData().setSelectHearingDetails(
                new DynamicList(new DynamicListItem("", ""), validHearingsDynamicList));

        return response;
    }

    @NotNull
    private String selectHearing(Hearing hearing) {
        return hearing.getValue().getVenue().getName() + " "
                + checkHearingTime(hearing.getValue().getTime()) + " "
                + LocalDate.parse(hearing.getValue().getHearingDate(), formatter).format(resultFormatter);
    }

    @NotNull
    private String checkHearingTime(String hearingTime) {
        return (hearingTime.length() == 5) ? (hearingTime + ":00") : hearingTime;
    }
}
