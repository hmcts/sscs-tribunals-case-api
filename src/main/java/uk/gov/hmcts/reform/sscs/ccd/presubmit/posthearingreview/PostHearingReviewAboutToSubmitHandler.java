package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getDocumentTypeFromReviewType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getEventTypeFromDocumentReviewTypeAndAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostHearingReviewAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;
    @Value("${feature.postHearingsB.enabled}")
    private final boolean isPostHearingsBEnabled;
    private final DocumentConfiguration documentConfiguration;
    private final FooterService footerService;
    private final GenerateFile generateFile;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");
        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.POST_HEARING_REVIEW
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        PostHearing postHearing = caseData.getPostHearing();
        log.info("Review Post Hearing App: handling action {} for case {}", postHearing.getReviewType(), caseData.getCcdCaseId());

        if (response.getErrors().isEmpty()) {
            SscsUtil.addDocumentToDocumentTabAndBundle(footerService, caseData,
                SscsUtil.getPostHearingReviewDocumentType(postHearing, isPostHearingsEnabled));
        }

        PostHearingReviewType reviewType = postHearing.getReviewType();
        CcdCallbackMap action = SscsUtil.getCcdCallbackMap(postHearing, reviewType);
        handlePostHearingReview(caseData, action, reviewType, userAuthorisation);

        return response;
    }

    private void handlePostHearingReview(SscsCaseData caseData,
                                         CcdCallbackMap action,
                                         PostHearingReviewType reviewType,
                                         String userAuthorisation) {

        DocumentType documentType = getDocumentTypeFromReviewType(reviewType);
        String actionName = action.getCcdDefinition();
        EventType eventType = getEventTypeFromDocumentReviewTypeAndAction(reviewType, actionName);

        String templateId = documentConfiguration.getDocuments()
            .get(caseData.getLanguagePreference())
            .get(eventType);
        issueDocument(caseData, documentType, templateId, generateFile, userAuthorisation, isPostHearingsEnabled, isPostHearingsBEnabled);
    }
}
