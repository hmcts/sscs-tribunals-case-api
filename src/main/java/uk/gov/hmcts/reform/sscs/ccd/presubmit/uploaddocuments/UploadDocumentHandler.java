package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class UploadDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        log.info("UploadDocumentHandler.canHandle begin...");
        log.info("callbackType: " + callbackType);
        log.info("caseId: " + callback.getCaseDetails().getId());
        log.info("caseData: " + callback.getCaseDetails().getCaseData());
        boolean result = callbackType != null && callback != null && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.UPLOAD_DOCUMENT)
            && State.WITH_DWP.equals(callback.getCaseDetails().getState());
        log.info("UploadDocumentHandler.canHandle.result: " + result);
        return result;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        log.info("callbackType: " + callbackType);
        log.info("caseId: " + callback.getCaseDetails().getId());
        log.info("caseData: " + callback.getCaseDetails().getCaseData());
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setDwpState(DwpState.FE_RECEIVED.getValue());
        log.info("UploadDocumentHandler.handle FE_RECEIVED end");
        return new PreSubmitCallbackResponse<>(caseData);
    }
}
