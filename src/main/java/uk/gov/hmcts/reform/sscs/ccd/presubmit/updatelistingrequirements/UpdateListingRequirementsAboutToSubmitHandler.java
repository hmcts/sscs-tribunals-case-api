package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.UPDATE_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateListingRequirementsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Value("${feature.gaps-switchover.enabled}")
    private boolean gapsSwitchOverFeature;
    @Value("${feature.snl.adjournment.enabled}")
    private boolean isAdjournmentEnabled;

    private final ListAssistHearingMessageHelper listAssistHearingMessageHelper;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && (callback.getEvent() == EventType.UPDATE_LISTING_REQUIREMENTS);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
        Callback<SscsCaseData> callback,
        String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        ReserveTo callbackReserveTo = callbackResponse.getData().getSchedulingAndListingFields().getReserveTo();
        SchedulingAndListingFields caseDataSnlFields = sscsCaseData.getSchedulingAndListingFields();

        if (nonNull(callbackReserveTo)) {
            YesNo callbackReservedDtj = callbackReserveTo.getReservedDistrictTribunalJudge();
            ReserveTo caseDataReserveTo = caseDataSnlFields.getReserveTo();
            caseDataReserveTo.setReservedDistrictTribunalJudge(callbackReservedDtj);

            if (isYes(callbackReservedDtj)) {
                caseDataReserveTo.setReservedJudge(null);
            }
        }

        if (isAdjournmentEnabled) {
            OverrideFields overrideFields = caseDataSnlFields.getOverrideFields();

            if (nonNull(overrideFields)) {
                HearingChannel hearingChannel = overrideFields.getAppellantHearingChannel();
                SscsUtil.updateHearingChannel(sscsCaseData, hearingChannel);
            }
        }

        State state = callback.getCaseDetails().getState();
        HearingRoute hearingRoute = caseDataSnlFields.getHearingRoute();
        if (gapsSwitchOverFeature
            && state == State.READY_TO_LIST
            && hearingRoute == LIST_ASSIST
            && nonNull(caseDataSnlFields.getOverrideFields())) {
            String caseId = sscsCaseData.getCcdCaseId();
            log.info("UpdateListingRequirements List Assist request, Update Hearing,"
                    + "amend reasons: {}, for case ID: {}",
                caseDataSnlFields.getAmendReasons(), caseId);

            HearingState hearingState = UPDATE_HEARING;

            boolean messageSuccess = listAssistHearingMessageHelper.sendHearingMessage(
                caseId,
                hearingRoute,
                hearingState,
                null);

            if (messageSuccess) {
                caseDataSnlFields.setHearingState(hearingState);
            } else {
                callbackResponse.addError("An error occurred during message publish. Please try again.");
            }
        }
        return callbackResponse;
    }
}
