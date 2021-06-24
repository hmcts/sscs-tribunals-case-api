package uk.gov.hmcts.reform.sscs.ccd.presubmit.generatecoversheet;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.coversheet.CoversheetService;

@Service
public class GenerateCoversheetAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final String DM_STORE_USER_ID = "sscs";
    private static final String FILENAME = "coversheet.pdf";

    private final CoversheetService coversheetService;
    private final EvidenceManagementService evidenceManagementService;

    @Autowired
    public GenerateCoversheetAboutToStartHandler(CoversheetService coversheetService, EvidenceManagementService evidenceManagementService) {
        this.coversheetService = coversheetService;
        this.evidenceManagementService = evidenceManagementService;
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
        UploadResponse uploadResponse = null;
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        if (urlByte.isPresent()) {
            ByteArrayMultipartFile file = ByteArrayMultipartFile.builder()
                    .content(urlByte.get())
                    .name(FILENAME)
                    .contentType(APPLICATION_PDF).build();

            uploadResponse = evidenceManagementService.upload(singletonList(file), DM_STORE_USER_ID);
        }

        if (uploadResponse != null && uploadResponse.getEmbedded() != null && isNotEmpty(uploadResponse.getEmbedded().getDocuments())) {
            String location = uploadResponse.getEmbedded().getDocuments().get(0).links.self.href;
            DocumentLink newDoc = DocumentLink.builder().documentFilename(FILENAME).documentUrl(location).documentBinaryUrl(location + "/binary").build();
            caseData.setPreviewDocument(newDoc);
        } else {
            response.addError("Error while trying to generate coversheet");
        }

        return response;
    }
}
