package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CREATE_HEARING;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadyToListSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.gaps-switchover.enabled}")
    private boolean gapsSwitchOverFeature;

    private final ListAssistHearingMessageHelper listAssistHearingMessageHelper;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.READY_TO_LIST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (gapsSwitchOverFeature) {
            HearingRoute hearingRoute = sscsCaseData.getSchedulingAndListingFields().getHearingRoute();
            HearingState hearingState = CREATE_HEARING;

            if (hearingRoute != GAPS) {
                log.info("Handling {} route {} request for Case ID: {}", hearingRoute, hearingState, sscsCaseData.getCcdCaseId());
                boolean messageSuccess = listAssistHearingMessageHelper.sendHearingMessage(
                    sscsCaseData.getCcdCaseId(),
                    hearingRoute,
                    hearingState);

                if (messageSuccess) {
                    sscsCaseData.getSchedulingAndListingFields().setHearingState(hearingState);
                } else {
                    callbackResponse.addError("An error occurred during message publish. Please try again.");
                }
            }
        }

        return callbackResponse;
    }
}
