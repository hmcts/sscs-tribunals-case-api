package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;

import feign.FeignException;
import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(JUnitParamsRunner.class)
public class IssueDirectionHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    private IssueDirectionHandler handler;

    @Captor
    ArgumentCaptor<Consumer<SscsCaseData>> caseDataConsumerCaptor;

    @Before
    public void setUp() {
        handler = new IssueDirectionHandler(updateCcdCaseService, idamService);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, DIRECTION_ISSUED));
    }

    @Test
    public void givenAValidDirectionIssuedEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString())).build(), INTERLOCUTORY_REVIEW_STATE, DIRECTION_ISSUED)));
    }

    @Test
    public void givenANonDirectionIssuedEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString())).build(), INTERLOCUTORY_REVIEW_STATE, APPEAL_RECEIVED)));
    }

    @Test
    public void givenAnIssueDirectionEventForPostValidCase_thenDoNotTriggerAppealToProceedEvent() {
        assertFalse(handler.canHandle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString())).build(), WITH_DWP, DIRECTION_ISSUED)));
    }

    @Test
    public void handleEmptyDirectionTypeDlValue() {
        assertFalse(handler.canHandle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().directionTypeDl(null).build(), INTERLOCUTORY_REVIEW_STATE, DIRECTION_ISSUED)));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(SUBMITTED, null);
    }

    @Test
    public void givenAnIssueDirectionEventForInterlocCase_thenTriggerAppealToProceedEvent() {
        var sscsCaseData = SscsCaseData.builder().directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString())).build();
        handler.handle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, INTERLOCUTORY_REVIEW_STATE, DIRECTION_ISSUED));

        verify(updateCcdCaseService).updateCaseV2(
                eq(1L),
                eq(EventType.APPEAL_TO_PROCEED.getCcdType()),
                eq("Appeal to proceed"),
                eq("Appeal proceed event triggered"),
                any(),
                caseDataConsumerCaptor.capture()
        );

        caseDataConsumerCaptor.getValue().accept(sscsCaseData);

        assertNull((sscsCaseData.getDirectionTypeDl()));
    }

    @Test(expected = FeignException.UnprocessableEntity.class)
    public void unProcessableEntityErrorIsReThrown() {
        when(updateCcdCaseService.updateCaseV2(
                        eq(1L),
                        eq(EventType.APPEAL_TO_PROCEED.getCcdType()),
                        eq("Appeal to proceed"),
                        eq("Appeal proceed event triggered"),
                        any(),
                        any(Consumer.class)
                )
        ).thenThrow(FeignException.UnprocessableEntity.class);

        handler.handle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString())).build(), INTERLOCUTORY_REVIEW_STATE, DIRECTION_ISSUED));
    }
}
