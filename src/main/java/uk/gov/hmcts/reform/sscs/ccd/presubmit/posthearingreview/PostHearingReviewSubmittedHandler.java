package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType.SET_ASIDE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdCallbackMap;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAside;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostHearingReviewSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdCallbackMapService ccdCallbackMapService;

    private final CcdService ccdService;

    private final IdamService idamService;

    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.POST_HEARING_REVIEW
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        Long caseId = Long.valueOf(caseData.getCcdCaseId());

        PostHearing postHearing = caseData.getPostHearing();
        PostHearingReviewType typeSelected = postHearing.getReviewType();
        log.info("Review Post Hearing App: handling actionPostHearing {} for case {}", typeSelected,  caseId);

        CcdCallbackMap callbackMap = getCcdCallbackMap(postHearing, typeSelected);

        if (isNull(callbackMap)) {
            response.addError(String.format("Invalid Action Post Hearing Application Type Selected %s or action "
                    + "selected as callback is null",
                typeSelected));
            return response;
        }

        boolean isSetAsideRefusedSor = isSetAsideRefusedSor(postHearing);

        SscsUtil.clearPostHearingFields(caseData, isPostHearingsEnabled);

        caseData = ccdCallbackMapService.handleCcdCallbackMap(callbackMap, caseData);

        if (isSetAsideRefusedSor) {
            handleSetAsideRefusedSor(caseData);
        }

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void handleSetAsideRefusedSor(SscsCaseData caseData) {
        ccdService.updateCase(caseData, Long.valueOf(caseData.getCcdCaseId()),
            EventType.SOR_REQUEST.getCcdType(), "Send to hearing Judge for statement of reasons", "",
            idamService.getIdamTokens());
    }

    @Nullable
    private static CcdCallbackMap getCcdCallbackMap(PostHearing postHearing,
                                                    PostHearingReviewType typeSelected) {
        if (isNull(typeSelected)) {
            return null;
        }

        switch (typeSelected) {
            case SET_ASIDE:
                if (isSetAsideRefusedSor(postHearing)) {
                    return SetAsideActions.REFUSE_SOR;
                } else {
                    return postHearing.getSetAside().getAction();
                }
            case CORRECTION:
                return postHearing.getCorrection().getAction();
            case STATEMENT_OF_REASONS:
                return postHearing.getStatementOfReasons().getAction();
            case PERMISSION_TO_APPEAL:
                return postHearing.getPermissionToAppeal().getAction();
            case LIBERTY_TO_APPLY:
                return postHearing.getLibertyToApply().getAction();
            default:
                return null;
        }
    }

    private static boolean isSetAsideRefusedSor(PostHearing postHearing) {
        SetAside setAside = postHearing.getSetAside();
        return SET_ASIDE.equals(postHearing.getReviewType())
            && SetAsideActions.REFUSE.equals(setAside.getAction())
            && isYes(setAside.getRequestStatementOfReasons());
    }
}
