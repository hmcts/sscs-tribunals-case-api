package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatenotlistable;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.CallbackValidationService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class UpdateNotListableMidEventValidationHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private CallbackValidationService callbackValidationService;

    @Autowired
    public UpdateNotListableMidEventValidationHandler(CallbackValidationService callbackValidationService) {
        this.callbackValidationService = callbackValidationService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
                && callback.getEvent() == EventType.UPDATE_NOT_LISTABLE
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

        if (callbackValidationService.isDueDateInvalid(sscsCaseData.getUpdateNotListableDueDate())) {
            preSubmitCallbackResponse.addError("Directions due date must be in the future");
        }

        return preSubmitCallbackResponse;
    }

}
