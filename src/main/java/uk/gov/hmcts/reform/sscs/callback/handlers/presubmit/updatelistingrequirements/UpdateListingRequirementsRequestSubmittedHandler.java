package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.updatelistingrequirements;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.UPDATE_HEARING;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.resendtogaps.ListAssistHearingMessageHelper;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateListingRequirementsRequestSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ListAssistHearingMessageHelper listAssistHearingMessageHelper;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent() == EventType.UPDATE_LISTING_REQUIREMENTS;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SchedulingAndListingFields caseDataSnlFields = sscsCaseData.getSchedulingAndListingFields();

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        State state = callback.getCaseDetails().getState();
        HearingRoute hearingRoute = caseDataSnlFields.getHearingRoute();
        if (state == State.READY_TO_LIST && hearingRoute == LIST_ASSIST
                && shouldSendHmcRequest(sscsCaseData, callback.getCaseDetailsBefore().orElse(null),
                caseDataSnlFields)) {
            String caseId = sscsCaseData.getCcdCaseId();
            log.info("UpdateListingRequirements List Assist request, Update Hearing,"
                            + "amend reasons: {}, for case ID: {}",
                    caseDataSnlFields.getAmendReasons(), caseId);

            boolean messageSuccess = listAssistHearingMessageHelper.sendHearingMessage(
                    caseId,
                    hearingRoute,
                    UPDATE_HEARING,
                    null);

            if (messageSuccess) {
                caseDataSnlFields.setHearingState(UPDATE_HEARING);
            } else {
                callbackResponse.addError("An error occurred during message publish. Please try again.");
            }
            callbackResponse.getErrors();
        }
        return callbackResponse;
    }

    private boolean shouldSendHmcRequest(SscsCaseData caseData, CaseDetails<SscsCaseData> caseDetailsBefore,
                                         SchedulingAndListingFields snlFields) {
        return (nonNull(caseDetailsBefore) && !Objects.equals(caseData.getPanelMemberComposition(),
                caseDetailsBefore.getCaseData().getPanelMemberComposition())) || nonNull(snlFields.getOverrideFields());
    }
}
