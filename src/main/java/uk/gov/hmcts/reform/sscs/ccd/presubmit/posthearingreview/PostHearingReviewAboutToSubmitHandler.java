package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
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

        updateCaseStatus(caseData);
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        String caseId = caseData.getCcdCaseId();

        PostHearingReviewType typeSelected = caseData.getPostHearing().getReviewType();
        log.info("Review Post Hearing App: handling action {} for case {}", typeSelected,  caseId);

        SscsUtil.addDocumentToDocumentTab(footerService, caseData,
            getPostHearingReviewDocumentType(caseData.getPostHearing()));

        return response;
    }

    protected void updateCaseStatus(SscsCaseData caseData) {
        PostHearing postHearing = caseData.getPostHearing();
        if (nonNull(postHearing) && nonNull(postHearing.getSetAside().getAction())
                && postHearing.getSetAside().getAction().equals(SetAsideActions.GRANT)) {
            caseData.setState(State.NOT_LISTABLE);
            caseData.setDwpState(DwpState.SET_ASIDE_GRANTED);
            caseData.setInterlocReviewState(InterlocReviewState.AWAITING_ADMIN_ACTION);
        }
    }

    private DocumentType getPostHearingReviewDocumentType(PostHearing postHearing) {
        if (SetAsideActions.REFUSE.equals(postHearing.getSetAside().getAction())) {
            return DocumentType.SET_ASIDE_REFUSED;
        } else if (CorrectionActions.REFUSE.equals(postHearing.getCorrection().getAction())) {
            return DocumentType.CORRECTION_REFUSED;
        }

        return DocumentType.DECISION_NOTICE;
    }
}
