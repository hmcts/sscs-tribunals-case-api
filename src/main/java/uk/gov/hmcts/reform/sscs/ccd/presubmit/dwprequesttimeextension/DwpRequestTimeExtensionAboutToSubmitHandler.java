package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwprequesttimeextension;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@Component
public class DwpRequestTimeExtensionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private DwpDocumentService dwpDocumentService;

    @Autowired
    public DwpRequestTimeExtensionAboutToSubmitHandler(DwpDocumentService dwpDocumentService) {
        this.dwpDocumentService = dwpDocumentService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");

        return CallbackType.ABOUT_TO_SUBMIT == callbackType
            && EventType.DWP_REQUEST_TIME_EXTENSION == callback.getEvent()
                && !isNull(callback.getCaseDetails().getCaseData().getTl1Form());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData updatedCaseData = updateCaseData(callback.getCaseDetails().getCaseData());

        return new PreSubmitCallbackResponse<>(updatedCaseData);
    }


    public SscsCaseData updateCaseData(SscsCaseData caseData) {

        caseData.setDwpState(DwpState.EXTENSION_REQUESTED);
        caseData.setInterlocReferralReason(InterlocReferralReason.TIME_EXTENSION);
        caseData.setTimeExtensionRequested("Yes");

        DwpResponseDocument tl1Form = caseData.getTl1Form();
        dwpDocumentService.addToDwpDocuments(caseData, tl1Form, DwpDocumentType.TL1_FORM);

        caseData.setTl1Form(null);

        return caseData;
    }
}
