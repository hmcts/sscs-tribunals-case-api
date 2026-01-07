package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.List;
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
import uk.gov.hmcts.reform.sscs.model.task.management.Task;
import uk.gov.hmcts.reform.sscs.service.TaskManagementApiService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TribunalCommunicationSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final TaskManagementApiService taskManagementApiService;

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

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        log.info("Handling Tribunal Communication Submitted callback for Case ID: {}",
                callback.getCaseDetails().getId());
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (isWorkAllocationEnabled) {
            log.info("Work Allocation is enabled - checking for Camunda tasks to cancel for Case ID: {}",
                    callback.getCaseDetails().getId());

            if (TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY.equals(sscsCaseData.getCommunicationFields().getTribunalRequestType())) {
                String caseId = String.valueOf(callback.getCaseDetails().getId());

                List<Task> taskList = taskManagementApiService.getTaskListByCaseId(caseId);

                log.info("Camunda tasks found for caseID: {}, Task List: {}", caseId, taskList);

                String taskFtaCommunicationId = sscsCaseData.getCommunicationFields().getWaTaskFtaCommunicationId();

                if (nonNull(taskList) && !taskList.isEmpty()) {
                    Task taskToBeCancelled = taskList.stream().filter(
                                    task -> task.getAdditionalProperties().get("ftaCommunicationId").equals(taskFtaCommunicationId))
                            .findFirst().orElse(null);

                    if (nonNull(taskToBeCancelled)) {
                        String taskIdToBeCancelled = taskToBeCancelled.getId();
                        log.info("Cancelling Camunda task for caseID: {}, Task ID: {}", caseId, taskIdToBeCancelled);
                        taskManagementApiService.cancelTaskByTaskId(taskIdToBeCancelled);
                    }
                }
            }
            sscsCaseData.getCommunicationFields().setTribunalRequestType(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        return callbackResponse;
    }
}
