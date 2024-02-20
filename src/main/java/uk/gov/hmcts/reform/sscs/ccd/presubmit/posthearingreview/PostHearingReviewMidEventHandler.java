package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.isFileAPdf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.util.PdfRequestUtil;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@RequiredArgsConstructor
public class PostHearingReviewMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String PAGE_ID_GENERATE_NOTICE = "generateNotice";
    public static final String PAGE_ID_VERIFY_NOTICE = "verifyNotice";
    private final DocumentConfiguration documentConfiguration;
    private final GenerateFile generateFile;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;
    @Value("${feature.postHearingsB.enabled}")
    private final boolean isPostHearingsBEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.POST_HEARING_REVIEW
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String pageId = callback.getPageId();
        String caseId = caseData.getCcdCaseId();
        log.info("Review Post Hearing App: handling callback with pageId {} for caseId {}", pageId, caseId);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        PostHearingReviewType typeSelected = caseData.getPostHearing().getReviewType();
        log.info("Review Post Hearing App: handling action {} for case {}", typeSelected,  caseId);

        if (PAGE_ID_VERIFY_NOTICE.equals(pageId) && !isFileAPdf(caseData.getDocumentStaging().getPostHearingPreviewDocument())) {
            response.addError("You need to upload PDF documents only");
        } else if (PAGE_ID_GENERATE_NOTICE.equals(pageId) && isYes(PdfRequestUtil.getGenerateNotice(caseData, isPostHearingsEnabled, isPostHearingsBEnabled))) {
            log.info("Review Post Hearing App: Generating notice for caseId {}", caseId);

            caseData.getDocumentStaging().setPostHearingPreviewDocument(null);

            String templateId = documentConfiguration.getDocuments()
                .get(caseData.getLanguagePreference()).get(DECISION_ISSUED);

            response = issueDocument(callback, SscsUtil.getPostHearingReviewDocumentType(caseData.getPostHearing(), isPostHearingsEnabled), templateId, generateFile, userAuthorisation, isPostHearingsEnabled, isPostHearingsBEnabled);
        }

        return response;
    }

    @Override
    protected void setDocumentOnCaseData(SscsCaseData caseData, DocumentLink file) {
        caseData.getDocumentStaging().setPostHearingPreviewDocument(file);
    }

    @Override
    protected DocumentLink getDocumentFromCaseData(SscsCaseData caseData) {
        return caseData.getDocumentStaging().getPostHearingPreviewDocument();
    }

}
