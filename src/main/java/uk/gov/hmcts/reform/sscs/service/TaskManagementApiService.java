package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.exception.TaskManagementException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.task.management.GetTasksResponse;
import uk.gov.hmcts.reform.sscs.model.task.management.Task;
import uk.gov.hmcts.reform.sscs.model.task.management.TaskRequestPayload;
import uk.gov.hmcts.reform.sscs.model.task.management.TaskSearchOperator;
import uk.gov.hmcts.reform.sscs.model.task.management.TaskSearchParameterKey;
import uk.gov.hmcts.reform.sscs.model.task.management.TaskSearchParameterList;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskManagementApiService {

    private final IdamService idamService;
    private final TaskManagementApi taskManagementApi;

    public List<Task> getTaskListByCaseId(String caseId) throws TaskManagementException {

        TaskRequestPayload request = TaskRequestPayload.builder()
                .searchParameters(List.of(
                        TaskSearchParameterList.builder()
                                .key(TaskSearchParameterKey.CASE_ID)
                                .operator(TaskSearchOperator.IN)
                                .values(List.of(caseId))
                                .build()))
                .build();

        log.info("Fetching tasks for caseID: {}", caseId);

        GetTasksResponse response;
        try {
            response = taskManagementApi.getTasksByCaseId(
                    idamService.getIdamWaTokens().getServiceAuthorization(),
                    idamService.getIdamWaTokens().getIdamOauth2Token(),
                    request
            );
        } catch (TaskManagementException e) {
            log.error("There was an issue retrieving tasks from task management api: {}", e.getMessage());
            throw new TaskManagementException(e.getMessage());
        }
        if (isNull(response)) {
            throw new TaskManagementException("No tasks found for case ID: " + caseId);
        }
        return response.getTasks();
    }

    public void cancelTaskByTaskId(String taskId) {
        log.info("Cancelling task with task ID: {}", taskId);
        taskManagementApi.cancelTask(
                idamService.getIdamWaTokens().getServiceAuthorization(),
                idamService.getIdamWaTokens().getIdamOauth2Token(),
                taskId
        );
    }

    public void cancelTasksByTaskProperties(String caseId, String additionalPropertyKey) {

        List<Task> taskList = getTaskListByCaseId(caseId);

        if (nonNull(taskList) && !taskList.isEmpty()) {
            taskList.stream().filter(
                            task -> nonNull(task.getAdditionalProperties()) && nonNull(task.getAdditionalProperties().get(additionalPropertyKey)))
                    .forEach(task -> {
                        cancelTaskByTaskId(task.getId());
                        log.info("Cancelling task for case ID: {}, task ID: {}, additional property key: {}, fta communication id: {}",
                                caseId, task.getId(), additionalPropertyKey,
                                task.getAdditionalProperties().get(additionalPropertyKey));
                    }
            );
        }
    }
}
