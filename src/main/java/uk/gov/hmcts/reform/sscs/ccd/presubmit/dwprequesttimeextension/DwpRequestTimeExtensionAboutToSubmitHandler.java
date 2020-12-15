package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwprequesttimeextension;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class DwpRequestTimeExtensionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

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

        caseData.setDwpState(DwpState.EXTENSION_REQUESTED.getId());
        caseData.setInterlocReferralReason(InterlocReferralReason.TIME_EXTENSION.getId());
        caseData.setTimeExtensionRequested("Yes");

        DwpResponseDocument tl1Form = caseData.getTl1Form();
        List<DwpDocument> updatedDocuments = addDocument(caseData.getDwpDocuments(), tl1Form);

        caseData.setTl1Form(null);
        caseData.setDwpDocuments(updatedDocuments);


        return caseData;
    }

    public List<DwpDocument> addDocument(List<DwpDocument> existingDwpDocuments, DwpResponseDocument tl1Form) {

        DwpDocumentDetails docDetails = new DwpDocumentDetails(
                DwpDocumentType.TL1_FORM.getValue(),
                "TL1-Form",
                LocalDate.now().toString(),
                tl1Form.getDocumentLink(),
                null,
                null,
                null
        );

        DwpDocument newDoc = new DwpDocument(docDetails);

        if (isNull(existingDwpDocuments)) {
            existingDwpDocuments = new ArrayList<>();
        }

        existingDwpDocuments.add(newDoc);

        return existingDwpDocuments;
    }
}
