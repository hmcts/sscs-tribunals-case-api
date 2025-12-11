package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.requireNonNull;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.client.CamundaClient;
import uk.gov.hmcts.reform.sscs.domain.CamundaTask;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TribunalCommunicationSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Autowired
    private CamundaClient camundaClient;

    private final IdamService idamService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent() == EventType.TRIBUNAL_COMMUNICATION;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        log.info("Handling Tribunal Communication Submitted callback for Case ID: {}",
                callback.getCaseDetails().getId());
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        String caseId = String.valueOf(callback.getCaseDetails().getId());

        List<CamundaTask> camundaTaskList = camundaClient.getTasksByTaskVariables(
                idamService.getIdamTokens().getServiceAuthorization(),
                "caseId_eq_" + caseId
                        + ",jurisdiction_eq_SSCS"
                        + ",caseTypeId_eq_Benefit",
                "created",
                "desc");

        log.info("Camunda tasks found for caseID: {}, Task List: {}", caseId, camundaTaskList);

        String taskProcessCategoryId = "ftaCommunicationId_" + sscsCaseData.getCommunicationFields().getLatestFtaCommunicationId();

        if (!camundaTaskList.isEmpty()) {
            String taskIdToBeCancelled = camundaTaskList.stream().filter(
                    task -> taskProcessCategoryId.equals(task.getProcessInstanceId()))
                    .findFirst().orElse(null).getId();
            log.info("Cancelling Camunda task for caseID: {}, Task ID: {}", caseId, taskIdToBeCancelled);

            camundaClient.cancelTask(
                    idamService.getIdamTokens().getServiceAuthorization(),
                    taskIdToBeCancelled);
        }


        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        return callbackResponse;
    }
}
