package uk.gov.hmcts.reform.sscs.ccd.presubmit.tribunalcommunication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestTopic;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.TribunalRequestType;
import uk.gov.hmcts.reform.sscs.model.task.management.Task;
import uk.gov.hmcts.reform.sscs.service.TaskManagementApiService;

class TribunalCommunicationSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String CASE_ID = "1234";

    private static final String TASK_ID_1 = "taskId1";

    private static final String TASK_ID_2 = "taskId2";

    private static final String FTA_COMMUNICATION_ID_1 = "ftaCommunicationId1";
    private static final String FTA_COMMUNICATION_ID_2 = "ftaCommunicationId2";

    private TribunalCommunicationSubmittedHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private TaskManagementApiService taskManagementApiService;

    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new TribunalCommunicationSubmittedHandler(taskManagementApiService, true);
        sscsCaseData = SscsCaseData.builder().ccdCaseId(CASE_ID).build();

        when(callback.getEvent()).thenReturn(EventType.TRIBUNAL_COMMUNICATION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAValidSubmittedEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    void givenAnInvalidSubmitedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    void givenAValidAboutToStartEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    void givenWorkAllocationisEnabled_thenClearTribunalRequestType() {
        List<CommunicationRequest> existingComs = new ArrayList<>();
        FtaCommunicationFields details = FtaCommunicationFields.builder()
                .tribunalCommunications(existingComs)
                .tribunalRequestType(TribunalRequestType.NEW_REQUEST)
                .build();
        sscsCaseData.setCommunicationFields(details);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCommunicationFields().getTribunalRequestType()).isNull();
    }

    @Test
    void givenWorkAllocationEnabledAndTribunalRequestTypeIsReply_thenRequestTaskList() {
        List<CommunicationRequest> existingComs = new ArrayList<>();
        FtaCommunicationFields details = FtaCommunicationFields.builder()
                .tribunalCommunications(existingComs)
                .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
                .build();
        sscsCaseData.setCommunicationFields(details);

        Task task = Task.builder().id(TASK_ID_1).build();

        when(taskManagementApiService.getTaskListByCaseId(any())).thenReturn(List.of(task));
        when(callback.getCaseDetails().getId()).thenReturn(Long.valueOf(CASE_ID));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCommunicationFields().getTribunalRequestType()).isNull();
        verify(taskManagementApiService, times(1)).getTaskListByCaseId(CASE_ID);
        verify(taskManagementApiService, times(0)).cancelTaskByTaskId(any());
    }

    @Test
    void givenWorkAllocationEnabledAndATaskExists_thenRequestTaskCancellation() {
        CommunicationRequest communicationRequest = CommunicationRequest.builder()
                .id(FTA_COMMUNICATION_ID_1)
                .value(CommunicationRequestDetails.builder()
                        .requestTopic(CommunicationRequestTopic.MRN_REVIEW_DECISION_NOTICE_DETAILS)
                        .requestMessage("Test Message")
                        .build())
                .build();
        FtaCommunicationFields details = FtaCommunicationFields.builder()
                .tribunalCommunications(List.of(communicationRequest))
                .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
                .waTaskFtaCommunicationId(FTA_COMMUNICATION_ID_1)
                .build();
        sscsCaseData.setCommunicationFields(details);

        Task task1 = Task.builder().id(TASK_ID_1).additionalProperties(Map.of("ftaCommunicationId",FTA_COMMUNICATION_ID_1)).build();
        Task task2 = Task.builder().id(TASK_ID_2).additionalProperties(Map.of("ftaCommunicationId",FTA_COMMUNICATION_ID_2)).build();

        when(taskManagementApiService.getTaskListByCaseId(any())).thenReturn(List.of(task1, task2));
        when(callback.getCaseDetails().getId()).thenReturn(Long.valueOf(CASE_ID));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCommunicationFields().getTribunalRequestType()).isNull();
        verify(taskManagementApiService, times(1)).getTaskListByCaseId(CASE_ID);
        verify(taskManagementApiService, times(1)).cancelTaskByTaskId(TASK_ID_1);
        verify(taskManagementApiService, times(0)).cancelTaskByTaskId(TASK_ID_2);
    }


    @Test
    void givenWorkAllocationDisabled_thenDoNotClearRequestTypeOrRequestTaskListOrCancellation() {

        handler = new TribunalCommunicationSubmittedHandler(taskManagementApiService, false);

        CommunicationRequest communicationRequest = CommunicationRequest.builder()
                .id(FTA_COMMUNICATION_ID_1)
                .value(CommunicationRequestDetails.builder()
                        .requestTopic(CommunicationRequestTopic.MRN_REVIEW_DECISION_NOTICE_DETAILS)
                        .requestMessage("Test Message")
                        .build())
                .build();
        FtaCommunicationFields details = FtaCommunicationFields.builder()
                .tribunalCommunications(List.of(communicationRequest))
                .tribunalRequestType(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY)
                .waTaskFtaCommunicationId(FTA_COMMUNICATION_ID_1)
                .build();
        sscsCaseData.setCommunicationFields(details);

        Task task = Task.builder().id("taskId123").additionalProperties(Map.of("ftaCommunicationId",FTA_COMMUNICATION_ID_1)).build();

        when(taskManagementApiService.getTaskListByCaseId(any())).thenReturn(List.of(task));
        when(callback.getCaseDetails().getId()).thenReturn(Long.valueOf(CASE_ID));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCommunicationFields().getTribunalRequestType()).isEqualTo(TribunalRequestType.REPLY_TO_TRIBUNAL_QUERY);
        verify(taskManagementApiService, times(0)).getTaskListByCaseId(any());
        verify(taskManagementApiService, times(0)).cancelTaskByTaskId(any());

    }
}
