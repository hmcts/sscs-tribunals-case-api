package uk.gov.hmcts.reform.sscs.service.servicebus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.callback.CallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.IssueFurtherEvidenceException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.PdfStoreException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.PostIssueFurtherEvidenceTasksException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.UnableToContactThirdPartyException;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;
import uk.gov.hmcts.reform.sscs.service.exceptions.ClientAuthorisationException;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.servicebus.NotificationsMessageProcessor;

@RunWith(MockitoJUnitRunner.class)
public class SendCallbackHandlerTest {

    private static final Exception EXCEPTION = new RuntimeException("blah");
    private static final int RETRY_THREE_TIMES = 3;

    @Mock
    private CallbackDispatcher<SscsCaseData> dispatcher;

    @Mock
    private NotificationsMessageProcessor notificationsMessageProcessor;

    private SendCallbackHandler sendCallbackHandler;
    private Exception exception;
    private Callback<SscsCaseData> callback;

    @Before
    public void setup() {
        sendCallbackHandler = new SendCallbackHandler(RETRY_THREE_TIMES, dispatcher, notificationsMessageProcessor);
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
            123L,
            "jurisdiction",
            null,
            SscsCaseData.builder().build(),
            null,
            "Benefit"
        );
        callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED, false);
    }

    @Test
    public void bulkPrintExceptionWillBeCaught() {
        exception = new BulkPrintException("message", EXCEPTION);
        doThrow(exception).when(dispatcher).handle(any(), any());
        sendCallbackHandler.handle(callback);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void givenIssueFurtherEvidenceException_shouldNotRetry() {
        doThrow(IssueFurtherEvidenceException.class).when(dispatcher).handle(any(), any());
        sendCallbackHandler.handle(callback);
        verify(dispatcher, times(1)).handle(any(), any());
    }

    @Test
    public void givenPostIssueFurtherEvidenceTaskException_shouldNotRetry() {
        doThrow(PostIssueFurtherEvidenceTasksException.class).when(dispatcher).handle(any(), any());
        sendCallbackHandler.handle(callback);
        verify(dispatcher, times(1)).handle(any(), any());
    }

    @Test
    public void pdfStoreExceptionWillBeCaught() {
        exception = new PdfStoreException("message", EXCEPTION);
        doThrow(exception).when(dispatcher).handle(any(), any());
        sendCallbackHandler.handle(callback);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void dwpAddressLookupExceptionWillBeCaught() {
        exception = new DwpAddressLookupException("message");
        doThrow(exception).when(dispatcher).handle(any(), any());
        sendCallbackHandler.handle(callback);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void noMrnDetailsExceptionWillBeCaught() {
        exception = new NoMrnDetailsException(SscsCaseData.builder().ccdCaseId("123").build());
        doThrow(exception).when(dispatcher).handle(any(), any());
        sendCallbackHandler.handle(callback);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void unableToContactThirdPartyExceptionWillBeCaught() {
        exception = new UnableToContactThirdPartyException("dm-store", new RuntimeException());
        doThrow(exception).when(dispatcher).handle(any(), any());
        sendCallbackHandler.handle(callback);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void nullPointerExceptionWillBeCaught() {
        exception = new NullPointerException();
        doThrow(exception).when(dispatcher).handle(any(), any());
        sendCallbackHandler.handle(callback);
        verify(dispatcher, atLeast(RETRY_THREE_TIMES)).handle(any(), any());
    }

    @Test
    public void clientAuthorisationExceptionWillBeCaught() {
        exception = new ClientAuthorisationException(EXCEPTION);
        doThrow(exception).when(dispatcher).handle(any(), any());
        sendCallbackHandler.handle(callback);
        verify(dispatcher, atLeast(RETRY_THREE_TIMES)).handle(any(), any());
    }

    @Test
    public void handleValidRequest() {
        sendCallbackHandler.handle(callback);
        verify(dispatcher).handle(any(), any());
    }


    @Test
    public void shouldProcessMessageForNotifications() {
        sendCallbackHandler = new SendCallbackHandler(RETRY_THREE_TIMES, dispatcher, notificationsMessageProcessor);
        sendCallbackHandler.handle(callback);
        verify(notificationsMessageProcessor).processMessage(callback);
    }
}
