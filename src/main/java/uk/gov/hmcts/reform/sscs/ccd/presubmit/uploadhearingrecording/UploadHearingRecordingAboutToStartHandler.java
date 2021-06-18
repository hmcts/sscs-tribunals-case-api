package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;

@Service
@Slf4j
public class UploadHearingRecordingAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static DateTimeFormatter resultFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static DateTimeFormatter hearingTimeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final IdamService idamService;

    @Autowired
    public UploadHearingRecordingAboutToStartHandler(IdamService idamService) {
        this.idamService = idamService;
    }

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

        if (sscsCaseData.getHearings() == null || sscsCaseData.getHearings().isEmpty()) {
            response.addError("No hearing has been conducted on this case");
            return response;
        }

        List<DynamicListItem> validHearings = sscsCaseData.getHearings().stream()
            .filter(hearing -> DateTimeUtils.isDateInThePast(
                LocalDateTime.parse(hearing.getValue().getHearingDate()
                        + " " + checkHearingTime(hearing.getValue().getTime()),
                    hearingTimeformatter)))
            .map(hearing -> new DynamicListItem(hearing.getValue().getHearingId(), selectHearing(hearing)))
            .collect(Collectors.toList());
        if (validHearings.isEmpty()) {
            response.addError("No hearing has been conducted on this case");
            return response;
        }
        sscsCaseData.getHearingRecordingsData().setSelectHearingDetails(new DynamicList(new DynamicListItem("", ""), validHearings));

        return response;
    }

    @NotNull
    private String selectHearing(Hearing hearing) {
        return hearing.getValue().getVenue().getName() + " "
            + hearing.getValue().getTime() + " "
            + LocalDate.parse(hearing.getValue().getHearingDate(), formatter).format(resultFormatter);
    }

    @NotNull
    private String checkHearingTime(String hearingTime) {
        return (hearingTime.length() == 5) ? (hearingTime + ":00") : hearingTime;
    }

}
