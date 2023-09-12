package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearing;

import static java.util.Objects.requireNonNull;
import static org.apache.logging.log4j.util.Strings.isNotEmpty;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class AddHearingAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ADD_HEARING;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        log.info("Setting outcome on appeal to abated for case id: {}", callback.getCaseDetails().getId());

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        for (Hearing hearing : caseData.getHearings()) {
            String time = hearing.getValue().getTime();

            if (isNotEmpty(time)) {
                try {
                    LocalTime.parse(time);
                } catch (DateTimeParseException | NullPointerException e) {
                    String errorMessage = "Invalid time format used, please use the format HH:mm";

                    log.error(errorMessage);
                    preSubmitCallbackResponse.addError(errorMessage);
                }
            }
        }

        return preSubmitCallbackResponse;
    }
}
