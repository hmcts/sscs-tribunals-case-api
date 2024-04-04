package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class CreateCaseWorkAllocationHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean workAllocationFeature;

    public CreateCaseWorkAllocationHandler(@Value("${feature.work-allocation.enabled}") boolean workAllocationFeature) {
        this.workAllocationFeature = workAllocationFeature;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && (callback.getEvent() == EventType.VALID_APPEAL_CREATED
                || callback.getEvent() == EventType.DRAFT_TO_VALID_APPEAL_CREATED
                || callback.getEvent() == EventType.NON_COMPLIANT
                || callback.getEvent() == EventType.DRAFT_TO_NON_COMPLIANT
                || callback.getEvent() == EventType.INCOMPLETE_APPLICATION_RECEIVED
                || callback.getEvent() == EventType.DRAFT_TO_INCOMPLETE_APPLICATION);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setPreWorkAllocation(workAllocationFeature ? YesNo.NO : YesNo.YES);

        return new PreSubmitCallbackResponse<>(caseData);
    }
}
