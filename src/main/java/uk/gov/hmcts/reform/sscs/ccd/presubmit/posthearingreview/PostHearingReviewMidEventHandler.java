package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;

@Component
@Slf4j
@RequiredArgsConstructor
public class PostHearingReviewMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String PAGE_ID_GENERATE_NOTICE = "generateNotice";
    private final DocumentConfiguration documentConfiguration;
    private final GenerateFile generateFile;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

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

        caseData.getDocumentStaging().setPreviewDocument(null);

        YesNo generateNotice = getGenerateNotice(caseData);

        if (PAGE_ID_GENERATE_NOTICE.equals(pageId) && isYes(generateNotice)) {
            log.info("Review Post Hearing App: Generating notice for caseId {}", caseId);
            String templateId = documentConfiguration.getDocuments()
                .get(caseData.getLanguagePreference()).get(DECISION_ISSUED);
            response = issueDocument(callback, DECISION_NOTICE, templateId, generateFile, userAuthorisation);
        }

        return response;
    }

    private YesNo getGenerateNotice(SscsCaseData caseData) {
        PostHearingReviewType postHearingReviewType = caseData.getPostHearing().getReviewType();
        switch (postHearingReviewType) {
            case SET_ASIDE:
                return caseData.getDocumentGeneration().getGenerateNotice();
            case CORRECTION:
                return caseData.getDocumentGeneration().getCorrectionGenerateNotice();
            case STATEMENT_OF_REASONS:
            case PERMISSION_TO_APPEAL:
            case LIBERTY_TO_APPLY:
            default:
                return null;
        }
    }
}
