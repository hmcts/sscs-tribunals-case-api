package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingrequest;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.RequestFormat.UPLOAD;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.clearPostHearingRequestFormatAndContentFields;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.util.PdfRequestUtil;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostHearingRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;
    private final FooterService footerService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.POST_HEARING_REQUEST
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
        Callback<SscsCaseData> callback,
        String userAuthorisation
    ) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        String caseId = caseData.getCcdCaseId();
        PostHearingRequestType requestType = caseData.getPostHearing().getRequestType();
        log.info("Post Hearing Request: handling action {} for case {}", requestType,  caseId);

        final PreSubmitCallbackResponse<SscsCaseData> response = validatePostHearingRequest(caseData);

        if (response.getErrors().isEmpty()) {
            SscsUtil.addDocumentToDocumentTabAndBundle(footerService, caseData,
                caseData.getDocumentStaging().getPreviewDocument(),
                PdfRequestUtil.getPostHearingDocumentType(caseData.getPostHearing().getRequestType()),
                callback.getEvent());
        }

        clearPostHearingRequestFormatAndContentFields(caseData, caseData.getPostHearing().getRequestType());

        return response;
    }

    @NotNull
    private PreSubmitCallbackResponse<SscsCaseData> validatePostHearingRequest(SscsCaseData sscsCaseData) {
        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        String postHearingRequestTypeDescription = sscsCaseData.getPostHearing().getRequestType().getDescriptionEn();
        renameDocumentIfUpload(sscsCaseData, postHearingRequestTypeDescription);
        DocumentLink previewDocument = sscsCaseData.getDocumentStaging().getPreviewDocument();
        if (previewDocument == null) {
            response.addError("There is no preview document");
        } else if (!previewDocument.getDocumentFilename().contains(postHearingRequestTypeDescription)) {
            response.addError("There is no post hearing request document");
        }
        return response;
    }

    private void renameDocumentIfUpload(SscsCaseData caseData, String requestTypeDescription) {
        if (Objects.equals(UPLOAD, caseData.getPostHearing().getRequestFormat())) {
            DocumentLink previewDocument = caseData.getDocumentStaging().getPreviewDocument();
            String filename = String.format("%s%s", requestTypeDescription, PdfRequestUtil.POST_HEARING_REQUEST_FILE_SUFFIX);

            log.info("Renaming uploaded Preview Document from '{}' to '{}'", previewDocument.getDocumentFilename(), filename);

            DocumentLink renamedPreviewDoc = DocumentLink.builder()
                .documentUrl(previewDocument.getDocumentUrl())
                .documentBinaryUrl(previewDocument.getDocumentBinaryUrl())
                .documentFilename(filename)
                .documentHash(previewDocument.getDocumentHash())
                .build();
            caseData.getDocumentStaging().setPreviewDocument(renamedPreviewDoc);
        }
    }

}
