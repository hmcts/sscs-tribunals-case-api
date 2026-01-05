package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.requireNonNull;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private WaTaskManagementApi waTaskManagementApi;

    private final IdamService idamService;
    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @Value("${feature.work-allocation.enabled}")
    private boolean isWorkAllocationEnabled;


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
            if (TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY.equals(sscsCaseData.getCommunicationFields().getTribunalRequestType())) {
                String caseId = String.valueOf(callback.getCaseDetails().getId());

                log.info("Masked wa token: {}***** and: {}*****",
                        idamService.getIdamWaTokens().getIdamOauth2Token().substring(0,15),
                        idamService.getIdamWaTokens().getServiceAuthorization().substring(0,15));

                String camundaTaskRequestVariables = "caseId_eq_" + caseId + ",jurisdiction_eq_SSCS" + ",caseTypeId_eq_Benefit";

                List<CamundaTask> camundaTaskList = waTaskManagementApi.getTasksByTaskVariables(
                        idamService.getIdamWaTokens().getServiceAuthorization(),
                        idamService.getIdamWaTokens().getIdamOauth2Token(),
                        camundaTaskRequestVariables,
                        "created",
                        "desc");

                log.info("Camunda tasks found for caseID: {}, Task List: {}", caseId, camundaTaskList);

                String taskProcessCategoryId = "ftaCommunicationId_" + sscsCaseData.getCommunicationFields().getWaTaskFtaCommunicationId();

                if (!camundaTaskList.isEmpty()) {
                    String taskIdToBeCancelled = camundaTaskList.stream().filter(
                                    task -> taskProcessCategoryId.equals(task.getProcessInstanceId()))
                            .findFirst().orElse(null).getId();
                    log.info("Cancelling Camunda task for caseID: {}, Task ID: {}", caseId, taskIdToBeCancelled);

                    waTaskManagementApi.cancelTask(
                            idamService.getIdamTokens().getServiceAuthorization(),
                            idamService.getIdamWaTokens().getIdamOauth2Token(),
                            taskIdToBeCancelled);
                }
            }
            sscsCaseData.getCommunicationFields().setTribunalRequestType(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        return callbackResponse;
    }
}
