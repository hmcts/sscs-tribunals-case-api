package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType.LIBERTY_TO_APPLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType.SET_ASIDE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostHearingReviewAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final FooterService footerService;

    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

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
                caseData.getDocumentStaging().getPostHearingPreviewDocument(),
                SscsUtil.getPostHearingReviewDocumentType(postHearing, isPostHearingsEnabled));

            updatePanelMemberList(caseData);
        }

        return response;
    }

    private void updatePanelMemberList(SscsCaseData caseData) {
        PostHearing postHearing = caseData.getPostHearing();

        PostHearingReviewType reviewType = postHearing.getReviewType();
        if (SET_ASIDE.equals(reviewType) && SetAsideActions.GRANT.equals(postHearing.getSetAside().getAction())) {
            SscsUtil.addPanelMembersToExclusions(caseData, false);
        } else if (LIBERTY_TO_APPLY.equals(reviewType) && LibertyToApplyActions.GRANT.equals(postHearing.getLibertyToApply().getAction())) {
            SscsUtil.addPanelMembersToExclusions(caseData, true);
        }
    }
}
