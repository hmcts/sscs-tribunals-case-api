package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpRequestTimeExtension;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.TL1_FORM;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class DwpRequestTimeExtensionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        return CallbackType.ABOUT_TO_SUBMIT == callbackType
            && EventType.DWP_REQUEST_TIME_EXTENSION == callback.getEvent();
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        transformTl1FormToSscsDocument(callback);
        return new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
    }

    private void transformTl1FormToSscsDocument(Callback<SscsCaseData> callback) {
        DwpResponseDocument tl1Form = callback.getCaseDetails().getCaseData().getTl1Form();
        List<SscsDocument> sscsDocuments = callback.getCaseDetails().getCaseData().getSscsDocument();
        if (sscsDocuments == null) {
            sscsDocuments = new ArrayList<>();
        }
        sscsDocuments.add(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(tl1Form.getDocumentLink())
                .documentType(TL1_FORM.getId())
                .build())
            .build());
        callback.getCaseDetails().getCaseData().setSscsDocument(sscsDocuments);
        callback.getCaseDetails().getCaseData().setTl1Form(null);
    }
}
