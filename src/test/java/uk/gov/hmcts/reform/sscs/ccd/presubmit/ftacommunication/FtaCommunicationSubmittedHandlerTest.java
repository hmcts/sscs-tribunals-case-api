package uk.gov.hmcts.reform.sscs.ccd.presubmit.ftacommunication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.FTA_COMMUNICATION;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.ftacommunication.FtaCommunicationSubmittedHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestTopic;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;

class FtaCommunicationSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private FtaCommunicationSubmittedHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    @Mock
    private AddNoteService addNoteService;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> consumerArgumentCaptor;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<ILoggingEvent> captorLoggingEvent;

    @BeforeEach
    void setUp() {
        openMocks(this);

        handler = new FtaCommunicationSubmittedHandler(idamService, addNoteService, updateCcdCaseService);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("5015").build();
        when(idamService.getIdamOauth2Token()).thenReturn(USER_AUTHORISATION);
        when(callback.getEvent()).thenReturn(EventType.FTA_COMMUNICATION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getId()).thenReturn(5015L);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    void givenAnInvalidAboutToSubmitEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    void givenAValidAboutToStartEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    void throwsExceptionIfItCannotHandleEvent() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
    }

    @Test
    void throwsExceptionIfItCannotHandleCallbackType() {
        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));
    }

    @Test
    void givenRequestToDelete_shouldUpdateCaseAndAddNote() {
        CommunicationRequestTopic expectedTopic = CommunicationRequestTopic.APPEAL_TYPE;
        String expectedUserRole = "Test Role";
        String expectedUserName = "Test User";
        String expectedReason = "Some reason to delete";

        CommunicationRequestDetails requestDetails = CommunicationRequestDetails.builder()
            .requestTopic(expectedTopic)
            .requestMessage("Test Message")
            .requestDateTime(LocalDateTime.of(2025,1,1, 1, 1))
            .requestUserName(expectedUserName)
            .requestUserRole(expectedUserRole)
            .build();

        final String expectedNote = "Request deleted: "
            + requestDetails.toString()
            + "\nReason for deletion: \n"
            + expectedReason;

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .deleteCommRequestReadOnlyStored(requestDetails)
            .deleteCommRequestTextAreaStored(expectedReason)
            .ftaRequestType(FtaRequestType.DELETE_REQUEST_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(sscsCaseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(idamService).getIdamTokens();
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.CASE_UPDATED.getCcdType()),
            eq("Tribunal/FTA communications updated"),
            eq("Tribunal/FTA communication deleted"),
            any(), consumerArgumentCaptor.capture());

        consumerArgumentCaptor.getValue().accept(SscsCaseDetails.builder().data(sscsCaseData).build());

        verify(addNoteService).addNote(USER_AUTHORISATION, sscsCaseData, expectedNote);
    }

    @Test
    void givenRequestReadOnlyNotSet_shouldDoNothing() {
        String expectedReason = "Some reason to delete";

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .deleteCommRequestTextAreaStored(expectedReason)
            .ftaRequestType(FtaRequestType.DELETE_REQUEST_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().id(1L)
            .data(sscsCaseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(idamService, never()).getIdamTokens();
        verify(updateCcdCaseService, never()).updateCaseV2(any(), any(), any(), any(), any(), any());
        verify(addNoteService, never()).addNote(any(), any(), any());
    }

    @Test
    void givenUpdateCaseThrows_thenShouldLogAndThrow() {
        Request request = Request.create(Request.HttpMethod.GET, "url",
            new HashMap<>(), null, new RequestTemplate());
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        String errorMessage = "some error message";
        when(updateCcdCaseService.updateCaseV2(any(), any(), any(), any(), any(), any()))
            .thenThrow(new FeignException.InternalServerError(errorMessage, request, null, null));
        Logger logger = (Logger) LoggerFactory.getLogger(FtaCommunicationSubmittedHandler.class.getName());
        logger.addAppender(mockAppender);

        FtaCommunicationFields fields = FtaCommunicationFields.builder()
            .deleteCommRequestReadOnlyStored(CommunicationRequestDetails.builder().build())
            .deleteCommRequestTextAreaStored("expectedReason")
            .ftaRequestType(FtaRequestType.DELETE_REQUEST_REPLY)
            .build();

        sscsCaseData.setCommunicationFields(fields);
        FeignException exception = assertThrows(FeignException.class, () -> handler.handle(SUBMITTED, callback, USER_AUTHORISATION));
        assertEquals(errorMessage, exception.getMessage());

        verify(mockAppender).doAppend(captorLoggingEvent.capture());
        List<ILoggingEvent> logEvents = captorLoggingEvent.getAllValues();
        assertEquals(1, logEvents.size());
        ILoggingEvent logEvent = logEvents.getFirst();
        assertEquals(Level.ERROR, logEvent.getLevel());
        String expectedErrorLog = "Could not add note from event " + FTA_COMMUNICATION.name()
            + " for case " + sscsCaseData.getCcdCaseId() + ". CCD response: " + errorMessage;
        assertEquals(expectedErrorLog, logEvent.getFormattedMessage());
    }
}
