package uk.gov.hmcts.reform.sscs.ccd.presubmit.notlistable;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.LocalDate;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class NotListableMidEventValidationHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
            && callback.getEvent() == EventType.NOT_LISTABLE
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (isNotListableDueDateInvalid(sscsCaseData)) {
            preSubmitCallbackResponse.addError("Directions due date must be in the future");
        }

        return preSubmitCallbackResponse;
    }


    private boolean isNotListableDueDateInvalid(SscsCaseData sscsCaseData) {
        if (!isBlank(sscsCaseData.getNotListableDueDate())) {
            LocalDate dueDate = LocalDate.parse(sscsCaseData.getNotListableDueDate());
            LocalDate now = LocalDate.now();
            return !dueDate.isAfter(now);
        }
        return false;
    }

}
