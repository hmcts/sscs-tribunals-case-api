package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.UPDATE_HEARING;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.isMissingListingRequirements;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateListingRequirementsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.gaps-switchover.enabled}")
    private boolean gapsSwitchOverFeature;

    @Value("${feature.snl.enabled}")
    private boolean isScheduleListingEnabled;

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

        SchedulingAndListingFields schedulingAndListingFields = sscsCaseData.getSchedulingAndListingFields();

        State state = callback.getCaseDetails().getState();
        HearingRoute hearingRoute = schedulingAndListingFields.getHearingRoute();

        if (gapsSwitchOverFeature
            && state == State.READY_TO_LIST
            && hearingRoute == LIST_ASSIST
            && nonNull(schedulingAndListingFields.getOverrideFields())) {
            String caseId = sscsCaseData.getCcdCaseId();
            log.info("UpdateListingRequirements List Assist request, Update Hearing,"
                    + "amend reasons: {}, for case ID: {}",
                schedulingAndListingFields.getAmendReasons(), caseId);

            HearingState hearingState = UPDATE_HEARING;

            boolean messageSuccess = listAssistHearingMessageHelper.sendHearingMessage(
                caseId,
                hearingRoute,
                hearingState,
                null);

            if (messageSuccess) {
                schedulingAndListingFields.setHearingState(hearingState);
            } else {
                callbackResponse.addError("An error occurred during message publish. Please try again.");
            }
        }

        if (isScheduleListingEnabled && isMissingListingRequirements(schedulingAndListingFields)) {
            sscsCaseData.setState(State.LISTING_ERROR);

            callbackResponse.addWarning("Missing information in Listing Requirements. Case state will be: "
                + State.LISTING_ERROR);
        }

        return callbackResponse;
    }
}
