package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;

@Service
@Slf4j
public class UploadHearingRecordingAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static DateTimeFormatter resultFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static DateTimeFormatter hearingTimeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${feature.hearing-recording-filter.enabled}")
    private boolean hearingRecordingFilterEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.UPLOAD_HEARING_RECORDING;
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

        //Clear hearing recordings for the flow
        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecording(null);
        if (sscsCaseData.getHearings() == null || sscsCaseData.getHearings().isEmpty()) {
            response.addError("No hearing has been conducted on this case");
            return response;
        }

        List<DynamicListItem> validHearings = sscsCaseData.getHearings().stream()
                .filter(hearing -> hearing.getValue().getHearingStatus() != HearingStatus.CANCELLED)
                .filter(this::isHearingInThePast)
                .map(hearing -> new DynamicListItem(hearing.getValue().getHearingId(), selectHearing(hearing)))
                .toList();
        if (validHearings.isEmpty()) {
            response.addError("No hearing has been conducted on this case");
            return response;
        }
        sscsCaseData.getSscsHearingRecordingCaseData().setSelectHearingDetails(
                new DynamicList(new DynamicListItem("", ""), validHearings));

        return response;
    }

    @NotNull
    private String selectHearing(Hearing hearing) {
        String venueName = hearing.getValue().getVenue().getName();
        String hearingTime = checkHearingTime(hearing.getValue().getTime());
        String hearingDate = LocalDate.parse(hearing.getValue().getHearingDate(), formatter).format(resultFormatter);
        String returnValue = venueName;

        if (!hearingTime.equals("")) {
            returnValue += " " + hearingTime;
        }
        return returnValue + " " + hearingDate;
    }

    private boolean isHearingInThePast(Hearing hearing) {
        String hearingDate = hearing.getValue().getHearingDate();
        String hearingTime = hearing.getValue().getTime();
        if (hearingRecordingFilterEnabled && isBlank(hearingDate)) {
            return false;
        }
        if (isBlank(hearingTime)) {
            LocalDate localDate = LocalDate.parse(hearingDate);
            return DateTimeUtils.isDateInThePast(localDate.atStartOfDay());
        }
        LocalDateTime parsedHearingDateAndTime = LocalDateTime.parse(hearingDate + " " + checkHearingTime(hearingTime), hearingTimeformatter);
        return DateTimeUtils.isDateInThePast(parsedHearingDateAndTime);
    }

    @NotNull
    private String checkHearingTime(String hearingTime) {
        if (isBlank(hearingTime)) {
            return "";
        }
        return (hearingTime.length() > 5 ? hearingTime.substring(0, 5) : hearingTime) + ":00";
    }
}
