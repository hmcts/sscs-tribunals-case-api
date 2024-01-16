package uk.gov.hmcts.reform.sscs.ccd.presubmit.generatecoversheet;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.coversheet.CoversheetService;

@Service
public class GenerateCoversheetAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final String FILENAME = "coversheet.pdf";

    private final CoversheetService coversheetService;
    private final PdfStoreService pdfStoreService;

    @Autowired
    public GenerateCoversheetAboutToStartHandler(CoversheetService coversheetService, PdfStoreService pdfStoreService) {
        this.coversheetService = coversheetService;
        this.pdfStoreService = pdfStoreService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.GENERATE_COVERSHEET;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        Optional<byte[]> urlByte = coversheetService.createCoverSheet(caseData.getCcdCaseId());
        SscsDocument sscsDocument = null;
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);


        if (urlByte.isPresent()) {
            sscsDocument = pdfStoreService.storeDocument(urlByte.get(), FILENAME, null);
        }

        if (sscsDocument != null && sscsDocument.getValue() != null
                && sscsDocument.getValue().getDocumentLink() != null) {
            String location = sscsDocument.getValue().getDocumentLink().getDocumentUrl();
            DocumentLink newDoc = DocumentLink.builder().documentFilename(FILENAME).documentUrl(location).documentBinaryUrl(location + "/binary").build();
            caseData.getDocumentStaging().setPreviewDocument(newDoc);
        } else {
            response.addError("Error while trying to generate coversheet");
        }

        return response;
    }
}
