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
import uk.gov.hmcts.reform.sscs.callback.EvidenceCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.exception.IssueFurtherEvidenceException;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.exception.PdfStoreException;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.exception.PostIssueFurtherEvidenceTasksException;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.exception.UnableToContactThirdPartyException;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;
import uk.gov.hmcts.reform.sscs.service.exceptions.ClientAuthorisationException;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.NotificationEventsManager;

@RunWith(MockitoJUnitRunner.class)
public class EvidenceNotifyCallbackProcessorTest {

    private static final Exception EXCEPTION = new RuntimeException("blah");
    private static final int RETRY_THREE_TIMES = 3;

    @Mock
    private EvidenceCallbackDispatcher<SscsCaseData> dispatcher;

    @Mock
    private NotificationEventsManager notificationEventsManager;

    private EvidenceNotifyCallbackProcessor evidenceNotifyCallbackProcessor;
    private Exception exception;
    private Callback<SscsCaseData> callback;

    @Before
    public void setup() {
        evidenceNotifyCallbackProcessor = new EvidenceNotifyCallbackProcessor(RETRY_THREE_TIMES, dispatcher, notificationEventsManager);
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
        evidenceNotifyCallbackProcessor.handle(callback);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void givenIssueFurtherEvidenceException_shouldNotRetry() {
        doThrow(IssueFurtherEvidenceException.class).when(dispatcher).handle(any(), any());
        evidenceNotifyCallbackProcessor.handle(callback);
        verify(dispatcher, times(1)).handle(any(), any());
    }

    @Test
    public void givenPostIssueFurtherEvidenceTaskException_shouldNotRetry() {
        doThrow(PostIssueFurtherEvidenceTasksException.class).when(dispatcher).handle(any(), any());
        evidenceNotifyCallbackProcessor.handle(callback);
        verify(dispatcher, times(1)).handle(any(), any());
    }

    @Test
    public void pdfStoreExceptionWillBeCaught() {
        exception = new PdfStoreException("message", EXCEPTION);
        doThrow(exception).when(dispatcher).handle(any(), any());
        evidenceNotifyCallbackProcessor.handle(callback);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void dwpAddressLookupExceptionWillBeCaught() {
        exception = new DwpAddressLookupException("message");
        doThrow(exception).when(dispatcher).handle(any(), any());
        evidenceNotifyCallbackProcessor.handle(callback);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void noMrnDetailsExceptionWillBeCaught() {
        exception = new NoMrnDetailsException(SscsCaseData.builder().ccdCaseId("123").build());
        doThrow(exception).when(dispatcher).handle(any(), any());
        evidenceNotifyCallbackProcessor.handle(callback);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void unableToContactThirdPartyExceptionWillBeCaught() {
        exception = new UnableToContactThirdPartyException("dm-store", new RuntimeException());
        doThrow(exception).when(dispatcher).handle(any(), any());
        evidenceNotifyCallbackProcessor.handle(callback);
        verify(dispatcher, atLeastOnce()).handle(any(), any());
    }

    @Test
    public void nullPointerExceptionWillBeCaught() {
        exception = new NullPointerException();
        doThrow(exception).when(dispatcher).handle(any(), any());
        evidenceNotifyCallbackProcessor.handle(callback);
        verify(dispatcher, atLeast(RETRY_THREE_TIMES)).handle(any(), any());
    }

    @Test
    public void clientAuthorisationExceptionWillBeCaught() {
        exception = new ClientAuthorisationException(EXCEPTION);
        doThrow(exception).when(dispatcher).handle(any(), any());
        evidenceNotifyCallbackProcessor.handle(callback);
        verify(dispatcher, atLeast(RETRY_THREE_TIMES)).handle(any(), any());
    }

    @Test
    public void handleValidRequest() {
        evidenceNotifyCallbackProcessor.handle(callback);
        verify(dispatcher).handle(any(), any());
    }


    @Test
    public void shouldProcessMessageForNotifications() {
        evidenceNotifyCallbackProcessor = new EvidenceNotifyCallbackProcessor(RETRY_THREE_TIMES, dispatcher, notificationEventsManager);
        evidenceNotifyCallbackProcessor.handle(callback);
        verify(notificationEventsManager).processMessage(callback);
    }
}
