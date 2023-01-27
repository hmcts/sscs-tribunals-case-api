package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingrequest;

import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
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
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        String caseId = caseData.getCcdCaseId();
        PostHearingRequestType requestType = caseData.getPostHearing().getRequestType();
        log.info("Post Hearing Request: handling action {} for case {}", requestType,  caseId);

        final PreSubmitCallbackResponse<SscsCaseData> response = validatePostHearingRequest(caseData);

        if (response.getErrors().isEmpty()) {
            SscsUtil.addDocumentToDocumentTab(footerService, caseData, PdfRequestUtil.getPostHearingDocumentType(caseData));
        }

        return response;
    }

    @NotNull
    private PreSubmitCallbackResponse<SscsCaseData> validatePostHearingRequest(SscsCaseData sscsCaseData) {
        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        DocumentLink previewDocument = sscsCaseData.getDocumentStaging().getPreviewDocument();
        if (previewDocument == null
            || !previewDocument.getDocumentFilename().contains(sscsCaseData.getPostHearing().getRequestType().getDescriptionEn())
        ) {
            response.addError("There is no post hearing request document");
        }
        return response;
    }
}
