package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.PostponeRequestTemplateBody;

@Service
public class ValidSendToInterlocMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final String TITLE = "Postponement Request";
    static final String FILENAME = "Postponement Request.pdf";
    private final GenerateFile generateFile;
    private final String templateId;

    public ValidSendToInterlocMidEventHandler(GenerateFile generateFile, @Value("${doc_assembly.postponementrequest}") String templateId) {
        this.generateFile = generateFile;
        this.templateId = templateId;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && (callback.getEvent() == EventType.VALID_SEND_TO_INTERLOC
                || callback.getEvent() == EventType.ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId().equals(sscsCaseData.getSelectWhoReviewsCase().getValue().getCode())) {
            final String requestDetails = sscsCaseData.getPostponementRequest().getPostponementRequestDetails();

            if (isBlank(requestDetails)) {
                response.addError("Please enter request details to generate a postponement request document");
                return response;
            }

            generatePostponementRequestPdfAndSetPreviewDocument(userAuthorisation, sscsCaseData, requestDetails);
        }

        return response;
    }

    private void generatePostponementRequestPdfAndSetPreviewDocument(String userAuthorisation, SscsCaseData sscsCaseData, String requestDetails) {
        GenerateFileParams params = getGenerateFileParams(userAuthorisation, requestDetails);
        final String generatedFileUrl = generateFile.assemble(params);
        setPostponementPreviewDocument(sscsCaseData, generatedFileUrl);
    }

    private void setPostponementPreviewDocument(SscsCaseData sscsCaseData, String generatedFileUrl) {
        sscsCaseData.getPostponementRequest().setPostponementPreviewDocument(getPreviewDocumentLink(generatedFileUrl));
    }

    private DocumentLink getPreviewDocumentLink(String generatedFileUrl) {
        return DocumentLink.builder()
                .documentFilename(FILENAME)
                .documentBinaryUrl(generatedFileUrl + "/binary")
                .documentUrl(generatedFileUrl)
                .build();
    }

    private GenerateFileParams getGenerateFileParams(String userAuthorisation, String requestDetails) {
        return GenerateFileParams.builder()
                .renditionOutputLocation(null)
                .templateId(templateId)
                .formPayload(getFormPayload(requestDetails))
                .userAuthentication(userAuthorisation)
                .build();
    }

    private PostponeRequestTemplateBody getFormPayload(String requestDetails) {
        return PostponeRequestTemplateBody.builder().title(TITLE).text(requestDetails).build();
    }

}