package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.config.SscsCallbackOrchestratorService;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.GRANT;

@Slf4j
@Component
public class ActionPostponementRequestSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ListAssistHearingMessageHelper hearingMessageHelper;

    private final boolean isSchedulingAndListingEnabled;

    private final SscsCallbackOrchestratorService sscsCallbackOrchestratorService;

    private final ObjectMapper objectMapper;

    public ActionPostponementRequestSubmittedHandler(ListAssistHearingMessageHelper hearingMessageHelper,
                                                     @Value("${feature.snl.enabled}") boolean isSchedulingAndListingEnabled,
                                                     SscsCallbackOrchestratorService sscsCallbackOrchestratorService,
                                                     ObjectMapper objectMapper) {
        this.hearingMessageHelper = hearingMessageHelper;
        this.isSchedulingAndListingEnabled = isSchedulingAndListingEnabled;
        this.sscsCallbackOrchestratorService = sscsCallbackOrchestratorService;
        this.objectMapper =  objectMapper;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callback type must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.ACTION_POSTPONEMENT_REQUEST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        ResponseEntity<String> callbackOrchestratorResponse;

        //check ids------------------------------
        //welsh???????????

        try {
            callbackOrchestratorResponse =
                sscsCallbackOrchestratorService.send(objectMapper.writeValueAsString(sscsCaseData));
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage());
            response.addError("Unable to serialise case data for case: " + caseDetails.getId());
            return response;
        }

        if (callbackOrchestratorResponse.getStatusCode() != HttpStatus.OK) {
            log.error("Unsuccessful callback to ccd callback orchestrator. Response code: {}, body: {} for  case {}",
                callbackOrchestratorResponse.getStatusCode(), callbackOrchestratorResponse.getBody(), caseDetails.getId());
            response.addError("Callback to orchestrator unsuccessful for case: " + caseDetails.getId());
            return response;
        }

        if (isSchedulingAndListingEnabled) {
            PostponementRequest postponementRequest = sscsCaseData.getPostponementRequest();

            if (isGrantPostponement(postponementRequest) && sscsCaseData.getState() == State.READY_TO_LIST) {
                log.info("Postponement grandted for S&L case {}, sending new hearing request.", caseDetails.getId());
                hearingMessageHelper.sendHearingMessage(sscsCaseData.getCcdCaseId(),
                    HearingRoute.LIST_ASSIST, HearingState.CREATE_HEARING, null);
            }
        }

        return response;
    }

    private boolean isGrantPostponement(PostponementRequest postponementRequest) {
        return postponementRequest.getActionPostponementRequestSelected().equals(GRANT.getValue());
    }
}
