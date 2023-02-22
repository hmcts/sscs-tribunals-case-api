package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostHearingReviewAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
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

        excludePanelMembers(caseData, response);

        String caseId = caseData.getCcdCaseId();
        PostHearingReviewType typeSelected = caseData.getPostHearing().getReviewType();
        log.info("Review Post Hearing App: handling action {} for case {}", typeSelected,  caseId);

        SscsUtil.clearDocumentTransientFields(caseData);

        return response;
    }

    protected void updateCaseStatus(SscsCaseData caseData) {
        PostHearing postHearing = caseData.getPostHearing();
        if (nonNull(postHearing) && nonNull(postHearing.getSetAside().getAction())) {
            caseData.setState(State.NOT_LISTABLE);
            caseData.setInterlocReviewState(InterlocReviewState.AWAITING_ADMIN_ACTION);
        }
    }

    protected void excludePanelMembers(SscsCaseData caseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        PostHearing postHearing = response.getData().getPostHearing();
        if (PostHearingReviewType.SET_ASIDE.equals(postHearing.getReviewType())
            && SetAsideActions.GRANT.equals(postHearing.getSetAside().getAction())
        ) {
            log.info("Set Aside is granted. Excluding panel members for case {}", caseData.getCcdCaseId());

            HearingDetails latestHearing = caseData.getLatestHearing().getValue();
            List<String> panelMemberIds = latestHearing.getPanelMemberIds(); // TODO test this list to confirm working as expected
            String judgeId = latestHearing.getJudgeId();
            ArrayList<JudicialUserBase> panelMembersToExclude = panelMemberIds.stream()
                .map(id -> JudicialUserBase.builder().personalCode(id).build())
                .collect(Collectors.toCollection(ArrayList::new));
            JudicialUserBase judgeToExclude = JudicialUserBase.builder().personalCode(judgeId).build();
            panelMembersToExclude.add(judgeToExclude);

            PanelMemberExclusions exclusions = caseData.getSchedulingAndListingFields().getPanelMemberExclusions();
            SscsUtil.excludePanelMembers(exclusions, panelMembersToExclude);

            caseData.setPanel(null);
            exclusions.setArePanelMembersReserved(YesNo.NO);
            exclusions.setArePanelMembersExcluded(YesNo.YES);
        }
    }
}
