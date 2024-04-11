package uk.gov.hmcts.reform.sscs.callback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;

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
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.IssueFurtherEvidenceHandler;
import uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.RoboticsCallbackHandler;
import uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.SendToBulkPrintHandler;

@RunWith(JUnitParamsRunner.class)
public class CallbackDispatcherTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private RoboticsCallbackHandler roboticsHandler;
    @Mock
    private SendToBulkPrintHandler sendToBulkPrintHandler;
    @Mock
    private IssueFurtherEvidenceHandler issueAppellantAppointeeFurtherEvidenceHandler;

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
        List<CallbackHandler<SscsCaseData>> handlers = Arrays.asList(
            roboticsHandler, sendToBulkPrintHandler, issueAppellantAppointeeFurtherEvidenceHandler);
        CallbackDispatcher<SscsCaseData> callbackDispatcher = new CallbackDispatcher<>(handlers);
        callbackDispatcher.handle(CallbackType.SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().build(), INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
        verifyMethodsAreCalledCorrectNumberOfTimes();
        verifyHandlersAreExecutedInPriorityOrder(handlers);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void verifyMethodsAreCalledCorrectNumberOfTimes() {
        then(roboticsHandler).should(times(DispatchPriority.values().length)).getPriority();
        then(sendToBulkPrintHandler).should(times(DispatchPriority.values().length)).getPriority();
        then(issueAppellantAppointeeFurtherEvidenceHandler).should(times(DispatchPriority.values().length)).getPriority();
    }

    private void verifyHandlersAreExecutedInPriorityOrder(List<CallbackHandler<SscsCaseData>> handlers) {
        InOrder orderVerifier = inOrder(roboticsHandler, sendToBulkPrintHandler, issueAppellantAppointeeFurtherEvidenceHandler);
        verifyPriorityOrder(handlers, orderVerifier, EARLIEST);
        verifyPriorityOrder(handlers, orderVerifier, EARLY);
        verifyPriorityOrder(handlers, orderVerifier, LATE);
        verifyPriorityOrder(handlers, orderVerifier, LATEST);
    }

    private void verifyPriorityOrder(List<CallbackHandler<SscsCaseData>> handlers, InOrder orderVerifier,
                                     DispatchPriority priority) {
        List<CallbackHandler<SscsCaseData>> handlersForGivenPriority = getHandlerForGivenPriority(handlers, priority);
        if (handlersForGivenPriority != null) {
            handlersForGivenPriority.forEach(handler -> verifyCalls(orderVerifier, handler));
        }
    }

    private void verifyCalls(InOrder orderVerifier, CallbackHandler<SscsCaseData> handler) {
        orderVerifier.verify(handler, times(1)).canHandle(any(), any());
        orderVerifier.verify(handler, times(1)).handle(any(), any());
        orderVerifier.verify(handler, times(0)).canHandle(any(), any());
        orderVerifier.verify(handler, times(0)).handle(any(), any());
    }

    private void mockHandlers(DispatchPriority priority1, DispatchPriority priority2, DispatchPriority priority3) {
        given(roboticsHandler.getPriority()).willReturn(priority1);
        given(roboticsHandler.canHandle(any(), any())).willReturn(true);

        given(sendToBulkPrintHandler.getPriority()).willReturn(priority2);
        given(sendToBulkPrintHandler.canHandle(any(), any())).willReturn(true);

        given(issueAppellantAppointeeFurtherEvidenceHandler.getPriority()).willReturn(priority3);
        given(issueAppellantAppointeeFurtherEvidenceHandler.canHandle(any(), any())).willReturn(true);
    }

    private List<CallbackHandler<SscsCaseData>> getHandlerForGivenPriority(List<CallbackHandler<SscsCaseData>> handlers,
                                                                           DispatchPriority priority) {
        return handlers.stream()
            .filter(handler -> handler.getPriority().equals(priority))
            .collect(Collectors.toList());
    }
}
