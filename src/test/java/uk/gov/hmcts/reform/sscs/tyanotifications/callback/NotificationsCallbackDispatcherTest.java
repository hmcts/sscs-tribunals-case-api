package uk.gov.hmcts.reform.sscs.tyanotifications.callback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;

@RunWith(JUnitParamsRunner.class)
public class NotificationsCallbackDispatcherTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private CallbackHandler roboticsHandler;

    @Mock
    private CallbackHandler sendToBulkPrintHandler;

    @Mock
    private CallbackHandler issueAppellantAppointeeFurtherEvidenceHandler;

    @Test
    @Parameters({
        "EARLIEST,LATE,LATEST",
        "LATE,LATEST,EARLIEST",
        "LATEST,EARLIEST,LATE",
        "LATEST,EARLIEST,EARLY",
        "EARLY,EARLIEST,LATEST",
        "EARLY,EARLY,LATEST",
        "EARLIEST,LATEST,LATEST"
    })
    public void givenHandlers_shouldBeHandledInDispatchPriority(DispatchPriority p1, DispatchPriority p2,
                                                                DispatchPriority p3) {
        mockHandlers(p1, p2, p3);
        List<CallbackHandler> handlers = Arrays.asList(
            roboticsHandler, sendToBulkPrintHandler, issueAppellantAppointeeFurtherEvidenceHandler);
        NotificationsCallbackDispatcher callbackDispatcher = new NotificationsCallbackDispatcher(handlers);
        callbackDispatcher.handle(buildTestCallbackForGivenData(SscsCaseData.builder().build(), INTERLOCUTORY_REVIEW_STATE, NotificationEventType.APPEAL_LAPSED));
        verifyMethodsAreCalledCorrectNumberOfTimes();
        verifyHandlersAreExecutedInPriorityOrder(handlers);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void verifyMethodsAreCalledCorrectNumberOfTimes() {
        then(roboticsHandler).should(times(DispatchPriority.values().length)).getPriority();
    }

    private void verifyHandlersAreExecutedInPriorityOrder(List<CallbackHandler> handlers) {
        InOrder orderVerifier = inOrder(roboticsHandler, sendToBulkPrintHandler, issueAppellantAppointeeFurtherEvidenceHandler);
        verifyPriorityOrder(handlers, orderVerifier, EARLIEST);
        verifyPriorityOrder(handlers, orderVerifier, EARLY);
        verifyPriorityOrder(handlers, orderVerifier, LATE);
        verifyPriorityOrder(handlers, orderVerifier, LATEST);
    }

    private void verifyPriorityOrder(List<CallbackHandler> handlers, InOrder orderVerifier,
                                     DispatchPriority priority) {
        List<CallbackHandler> handlersForGivenPriority = getHandlerForGivenPriority(handlers, priority);
        if (handlersForGivenPriority != null) {
            handlersForGivenPriority.forEach(handler -> verifyCalls(orderVerifier, handler));
        }
    }

    private void verifyCalls(InOrder orderVerifier, CallbackHandler handler) {
        orderVerifier.verify(handler, times(1)).canHandle(any());
        orderVerifier.verify(handler, times(1)).handle(any());
        orderVerifier.verify(handler, times(0)).canHandle(any());
        orderVerifier.verify(handler, times(0)).handle(any());
    }

    private void mockHandlers(DispatchPriority priority1, DispatchPriority priority2, DispatchPriority priority3) {
        given(roboticsHandler.getPriority()).willReturn(priority1);
        given(roboticsHandler.canHandle(any())).willReturn(true);

        given(sendToBulkPrintHandler.getPriority()).willReturn(priority2);
        given(sendToBulkPrintHandler.canHandle(any())).willReturn(true);

        given(issueAppellantAppointeeFurtherEvidenceHandler.getPriority()).willReturn(priority3);
        given(issueAppellantAppointeeFurtherEvidenceHandler.canHandle(any())).willReturn(true);
    }

    private List<CallbackHandler> getHandlerForGivenPriority(List<CallbackHandler> handlers,
                                                             DispatchPriority priority) {
        return handlers.stream()
            .filter(handler -> handler.getPriority().equals(priority))
            .collect(Collectors.toList());
    }
}
