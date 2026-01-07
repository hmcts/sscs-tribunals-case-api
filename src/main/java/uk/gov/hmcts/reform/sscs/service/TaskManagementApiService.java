package uk.gov.hmcts.reform.sscs.service;

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

    public List<Task> getTasksByCaseId(String caseId) throws TaskManagementException {

        TaskRequestPayload request = TaskRequestPayload.builder()
                .searchParameters(List.of(
                        TaskSearchParameterList.builder()
                                .key(TaskSearchParameterKey.CASE_ID)
                                .operator(TaskSearchOperator.IN)
                                .values(List.of(caseId))
                                .build()))
                .build();

        log.info("Wa task search request: {} for case id: {} ", request, caseId);

        GetTasksResponse response = null;
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
        return response.getTasks();
    }

    public void cancelTask(String taskId) {
        try {
            taskManagementApi.cancelTask(
                    idamService.getIdamWaTokens().getServiceAuthorization(),
                    idamService.getIdamWaTokens().getIdamOauth2Token(),
                    taskId
            );
        } catch (TaskManagementException e) {
            log.error("There was an issue cancelling task in task management api: {}", e.getMessage());
        }
    }
}
