package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.TribunalRequestType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.domain.CamundaTask;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.WaTaskManagementApi;

@Slf4j
@Service
@RequiredArgsConstructor
public class TribunalCommunicationSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final WaTaskManagementApi waTaskManagementApi;

    private final IdamService idamService;
    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String KEY_CASE_ID = "caseId";

    @Value("${feature.work-allocation.enabled}")
    private final boolean isWorkAllocationEnabled;


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

        if (isWorkAllocationEnabled) {
            log.info("Work Allocation is enabled - checking for Camunda tasks to cancel for Case ID: {}",
                    callback.getCaseDetails().getId());

            if (TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY.equals(sscsCaseData.getCommunicationFields().getTribunalRequestType())) {
                String caseId = String.valueOf(callback.getCaseDetails().getId());

                Map<String, Object> camundaRequestBodyMap = Map.of(
                        "processVariables", List.of(Map.of(
                                "name", KEY_CASE_ID,
                                "operator", "eq",
                                "value", caseId
                        )));

                String camundaRequestBodyString =
                        "{\n"
                        + "    \"search_parameters\": [\n"
                        + "    {\n"
                        + "      \"key\": \"jurisdiction\",\n"
                        + "      \"operator\": \"IN\",\n"
                        + "      \"values\": [\n"
                        + "          \"SSCS\"\n"
                        + "       ]\n"
                        + "    },\n"
                        + "    {\n"
                        + "      \"key\": \"caseId\",\n"
                        + "      \"operator\": \"IN\",\n"
                        + "      \"values\": [\n"
                        + "          \"" + caseId + "\"\n"
                        + "       ]\n"
                        + "    }    \n"
                        + "  ],\n"
                        + "  \"sorting_parameters\": [\n"
                        + "    {\n"
                        + "      \"sort_by\": \"due_date\",\n"
                        + "      \"sort_order\": \"asc\"\n"
                        + "    }\n"
                        + "  ]\n"
                        + "}";

                log.info("Fetching Camunda tasks for caseID: {} with variables: \n{}", caseId, camundaRequestBodyMap);

                List<CamundaTask> camundaTaskList = waTaskManagementApi.getTasksByTaskVariables(
                        idamService.getIdamWaTokens().getServiceAuthorization(),
                        camundaRequestBodyMap
                        );

                log.info("Camunda tasks found for caseID: {}, Task List: {}", caseId, camundaTaskList);

                String taskProcessCategoryId = "ftaCommunicationId_" + sscsCaseData.getCommunicationFields().getWaTaskFtaCommunicationId();

                if (nonNull(camundaTaskList) && !camundaTaskList.isEmpty()) {
                    CamundaTask camundaTaskToBeCancelled = camundaTaskList.stream().filter(
                                    task -> taskProcessCategoryId.equals(task.getProcessInstanceId()))
                            .findFirst().orElse(null);

                    if (nonNull(camundaTaskToBeCancelled)) {
                        String taskIdToBeCancelled = camundaTaskToBeCancelled.getId();
                        log.info("Cancelling Camunda task for caseID: {}, Task ID: {}", caseId, taskIdToBeCancelled);

                        waTaskManagementApi.cancelTask(
                                idamService.getIdamWaTokens().getServiceAuthorization(),
                                idamService.getIdamWaTokens().getIdamOauth2Token(),
                                taskIdToBeCancelled);
                    }
                }
            }
            sscsCaseData.getCommunicationFields().setTribunalRequestType(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        return callbackResponse;
    }
}
