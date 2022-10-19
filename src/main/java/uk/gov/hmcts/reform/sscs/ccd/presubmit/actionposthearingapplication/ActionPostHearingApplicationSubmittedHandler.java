package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionposthearingapplication;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.CORRECTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.LIBERTY_TO_APPLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.PERMISSION_TO_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.SET_ASIDE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.STATEMENT_OF_REASONS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingApplication;
import uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdCallbackMap;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActionPostHearingApplicationSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdService ccdService;
    private final IdamService idamService;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.ACTION_POST_HEARING_APPLICATION
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        Long caseId = Long.valueOf(caseData.getCcdCaseId());
        if (!SscsUtil.isSAndLCase(caseData)) {
            log.info("Action Post Hearing Application: Cannot process non Scheduling & Listing Case for Case ID {}",
                caseId);
            response.addError("Cannot process Action Post Hearing Application on non Scheduling & Listing Case");
            return response;
        }

        ActionPostHearingApplication actionPostHearing = caseData.getActionPostHearingApplication();
        ActionPostHearingTypes typeSelected = actionPostHearing.getTypeSelected();
        log.info("Action Post Hearing Application: handing actionPostHearing {} for case {}", typeSelected,  caseId);

        CcdCallbackMap action = getCcdCallbackMap(actionPostHearing, typeSelected);

        if (isNull(action)) {
            response.addError(String.format("Invalid Action Post Hearing Application Type Selected %s or action "
                    + "selected as callback is null",
                typeSelected));
            return response;
        }

        if (nonNull(action.getCallbackEvent())) {
            SscsCaseDetails updateCaseDetails = ccdService.updateCase(
                caseData, caseId, action.getCallbackEvent().getCcdType(), action.getCallbackSummary(),
                action.getCallbackDescription(), idamService.getIdamTokens());
            return new PreSubmitCallbackResponse<>(updateCaseDetails.getData());
        }

        return response;
    }

    @Nullable
    private static CcdCallbackMap getCcdCallbackMap(ActionPostHearingApplication actionPostHearing,
                                                    ActionPostHearingTypes typeSelected) {
        if (isNull(typeSelected)) {
            return null;
        }
        switch (typeSelected) {
            case SET_ASIDE:
                CcdCallbackMap action = actionPostHearing.getActionSetAside().getAction();
                if (action == SetAsideActions.REFUSE
                    && isYes(actionPostHearing.getActionSetAside().getRequestStatementOfReasons())) {
                    action = SetAsideActions.REFUSE_SOR;
                }
                return action;
            case CORRECTION:
                return actionPostHearing.getActionCorrection().getAction();
            case STATEMENT_OF_REASONS:
                return actionPostHearing.getActionStatementOfReasons().getAction();
            case PERMISSION_TO_APPEAL:
                return actionPostHearing.getActionPermissionToAppeal().getAction();
            case LIBERTY_TO_APPLY:
                return actionPostHearing.getActionLibertyToApply().getAction();
            default:
                return null;
        }
    }
}
