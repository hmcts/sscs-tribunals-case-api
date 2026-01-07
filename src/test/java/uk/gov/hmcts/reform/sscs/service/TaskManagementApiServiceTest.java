package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.exception.TaskManagementException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.task.management.GetTasksResponse;
import uk.gov.hmcts.reform.sscs.model.task.management.Task;
import uk.gov.hmcts.reform.sscs.model.task.management.TaskRequestPayload;
import uk.gov.hmcts.reform.sscs.model.task.management.TaskSearchOperator;
import uk.gov.hmcts.reform.sscs.model.task.management.TaskSearchParameterKey;
import uk.gov.hmcts.reform.sscs.model.task.management.TaskSearchParameterList;

@ExtendWith(MockitoExtension.class)
public class TaskManagementApiServiceTest {

    private static final String SERVICE_AUTHORIZATION = "Bearer ServiceAuthorization";
    private static final String IDAM_OAUTH2_TOKEN = "Bearer Oauth2Token";
    private static final String TEST_CASE_ID = "1234";
    private static final String TASK_ID_1 = "taskId1";
    private static final String TASK_ID_2 = "taskId2";
    private static final String TASK_ID_3 = "taskId3";
    private static final String TASK_ID_4 = "taskId4";
    private static final String ADDITIONAL_PROP_KEY = "additionalPropKey";
    private static final String ADDITIONAL_PROP_VALUE = "additionalPropValue";

    @Mock
    TaskManagementApi taskManagementApi;

    @Mock
    private IdamService idamService;

    @InjectMocks
    private TaskManagementApiService taskManagementApiService;

    @BeforeEach
    void setUp() {
        given(idamService.getIdamWaTokens()).willReturn(IdamTokens.builder()
                .serviceAuthorization(SERVICE_AUTHORIZATION)
                .idamOauth2Token(IDAM_OAUTH2_TOKEN)
                .build());
    }


    @DisplayName("When getTaskListByCaseId is given the correct parameters it returns a valid response without error")
    @Test
    void testGetTaskListByCaseId() {
        TaskRequestPayload taskRequestPayload = TaskRequestPayload.builder()
                .searchParameters(List.of(
                        TaskSearchParameterList.builder()
                                .key(TaskSearchParameterKey.CASE_ID)
                                .operator(TaskSearchOperator.IN)
                                .values(List.of(TEST_CASE_ID))
                                .build()))
                .build();

        Task task = Task.builder()
                .id(TASK_ID_1)
                .additionalProperties(
                        java.util.Map.of(ADDITIONAL_PROP_KEY, ADDITIONAL_PROP_VALUE)
                )
                .build();

        List<Task> responseTaskList = List.of(task);

        GetTasksResponse getTasksResponse = GetTasksResponse.builder()
                .tasks(responseTaskList)
                .build();

        given(taskManagementApi.getTasksByCaseId(SERVICE_AUTHORIZATION, IDAM_OAUTH2_TOKEN, taskRequestPayload)).willReturn(getTasksResponse);

        List<Task> taskList = taskManagementApiService.getTaskListByCaseId(TEST_CASE_ID);

        assertThat(taskList)
                .isNotNull()
                .isEqualTo(responseTaskList);

        verify(taskManagementApi, times(1)).getTasksByCaseId(SERVICE_AUTHORIZATION, IDAM_OAUTH2_TOKEN, taskRequestPayload);
    }

    @DisplayName("When getTaskListByCaseId returns null the correct error and message is thrown")
    @Test
    void testGetHearingRequestNullResponse() {
        TaskRequestPayload taskRequestPayload = TaskRequestPayload.builder()
                .searchParameters(List.of(
                        TaskSearchParameterList.builder()
                                .key(TaskSearchParameterKey.CASE_ID)
                                .operator(TaskSearchOperator.IN)
                                .values(List.of(TEST_CASE_ID))
                                .build()))
                .build();

        given(taskManagementApi.getTasksByCaseId(SERVICE_AUTHORIZATION, IDAM_OAUTH2_TOKEN, taskRequestPayload)).willReturn(null);

        assertThatExceptionOfType(TaskManagementException.class)
                .isThrownBy(() -> taskManagementApiService.getTaskListByCaseId(TEST_CASE_ID))
                .withMessageContaining("No tasks found for case ID: " + TEST_CASE_ID);
    }

    @DisplayName("When getTaskListByCaseId returns null the correct error and message is thrown")
    @Test
    void getAllTasks_shouldThrowInternalServerErrorException_whenRemoteProcessEngineExceptionIsThrown() {
        TaskRequestPayload taskRequestPayload = TaskRequestPayload.builder()
                .searchParameters(List.of(
                        TaskSearchParameterList.builder()
                                .key(TaskSearchParameterKey.CASE_ID)
                                .operator(TaskSearchOperator.IN)
                                .values(List.of(TEST_CASE_ID))
                                .build()))
                .build();

        given(taskManagementApi.getTasksByCaseId(SERVICE_AUTHORIZATION, IDAM_OAUTH2_TOKEN, taskRequestPayload)).willThrow(
                new TaskManagementException("Task management error"));

        assertThatExceptionOfType(TaskManagementException.class)
                .isThrownBy(() -> taskManagementApiService.getTaskListByCaseId(TEST_CASE_ID))
                .withMessageContaining("Task management error");
    }

    @DisplayName("When cancelTaskByTaskId should send cancellation request successfully")
    @Test
    void testCancelTaskByTaskId() {

        taskManagementApiService.cancelTaskByTaskId(TASK_ID_1);

        verify(taskManagementApi, times(1)).cancelTask(SERVICE_AUTHORIZATION, IDAM_OAUTH2_TOKEN, TASK_ID_1);

    }

    @DisplayName("When cancelTasksByTaskProperties should send request successfully")
    @Test
    void cancelTasksByTaskProperties() {

        TaskRequestPayload taskRequestPayload = TaskRequestPayload.builder()
                .searchParameters(List.of(
                        TaskSearchParameterList.builder()
                                .key(TaskSearchParameterKey.CASE_ID)
                                .operator(TaskSearchOperator.IN)
                                .values(List.of(TEST_CASE_ID))
                                .build()))
                .build();

        Task task1 = Task.builder()
                .id(TASK_ID_1)
                .additionalProperties(
                        java.util.Map.of(ADDITIONAL_PROP_KEY, ADDITIONAL_PROP_VALUE)
                )
                .build();

        Task task2 = Task.builder()
                .id(TASK_ID_2)
                .additionalProperties(
                        java.util.Map.of(ADDITIONAL_PROP_KEY, "additionalPropValue2")
                )
                .build();

        Task task3 = Task.builder()
                .id(TASK_ID_3)
                .additionalProperties(
                        java.util.Map.of("DifferentPropKey", "differentPropValue")
                )
                .build();

        Task task4 = Task.builder()
                .id(TASK_ID_4)
                .build();

        List<Task> responseTaskList = List.of(task1, task2, task3, task4);

        GetTasksResponse getTasksResponse = GetTasksResponse.builder()
                .tasks(responseTaskList)
                .build();

        given(taskManagementApi.getTasksByCaseId(SERVICE_AUTHORIZATION, IDAM_OAUTH2_TOKEN, taskRequestPayload)).willReturn(getTasksResponse);

        taskManagementApiService.cancelTasksByTaskProperties(TEST_CASE_ID, ADDITIONAL_PROP_KEY);

        verify(taskManagementApi, times(1)).getTasksByCaseId(SERVICE_AUTHORIZATION, IDAM_OAUTH2_TOKEN, taskRequestPayload);
        verify(taskManagementApi, times(1)).cancelTask(SERVICE_AUTHORIZATION, IDAM_OAUTH2_TOKEN, TASK_ID_1);
        verify(taskManagementApi, times(1)).cancelTask(SERVICE_AUTHORIZATION, IDAM_OAUTH2_TOKEN, TASK_ID_2);
        verify(taskManagementApi, times(0)).cancelTask(SERVICE_AUTHORIZATION, IDAM_OAUTH2_TOKEN, TASK_ID_3);
    }
}
