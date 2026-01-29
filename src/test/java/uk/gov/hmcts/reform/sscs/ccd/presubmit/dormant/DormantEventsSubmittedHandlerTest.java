package uk.gov.hmcts.reform.sscs.ccd.presubmit.dormant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.TaskManagementApiService;
import uk.gov.hmcts.reform.sscs.service.servicebus.SendCallbackHandler;

class DormantEventsSubmittedHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String CASE_ID = "1234";
    private DormantEventsSubmittedHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private TaskManagementApiService taskManagementApiService;

    @Mock
    private SendCallbackHandler sendCallbackHandler;

    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new DormantEventsSubmittedHandler(taskManagementApiService, sendCallbackHandler, true);
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId(CASE_ID).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getId()).thenReturn(Long.valueOf(CASE_ID));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"WITHDRAWN", "DORMANT", "CONFIRM_LAPSED"})
    void givenAValidSubmittedEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @Test
    void givenAnInvalidSubmitedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenAValidAboutToStartEvent_thenReturnFalse() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @ParameterizedTest
    @CsvSource(
        {"WITHDRAWN,true", "WITHDRAWN,false",
         "DORMANT,true", "DORMANT,false",
         "CONFIRM_LAPSED,true", "CONFIRM_LAPSED,false"})
    void givenCanHandle_thenSendCallbackMessage(EventType eventType, Boolean isWorkAllocationEnabled) {
        when(callback.getEvent()).thenReturn(eventType);
        handler = new DormantEventsSubmittedHandler(taskManagementApiService, sendCallbackHandler, isWorkAllocationEnabled);
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        verify(sendCallbackHandler, times(1)).handle(callback);
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"WITHDRAWN", "DORMANT", "CONFIRM_LAPSED"})
    void givenWorkAllocationEnabled_thenCancelTasks(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        verify(taskManagementApiService, times(1)).cancelTasksByTaskProperties(CASE_ID, "ftaCommunicationId");
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"WITHDRAWN", "DORMANT", "CONFIRM_LAPSED"})
    void givenWorkAllocationNotEnabled_thenDoNotCancelTasks(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        handler = new DormantEventsSubmittedHandler(taskManagementApiService, sendCallbackHandler, false);
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        verify(taskManagementApiService, times(0)).cancelTasksByTaskProperties(CASE_ID, "ftaCommunicationId");
    }
}
