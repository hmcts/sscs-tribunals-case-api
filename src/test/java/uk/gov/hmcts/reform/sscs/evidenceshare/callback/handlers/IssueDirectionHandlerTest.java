package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.jupiter.api.Assertions.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@RunWith(JUnitParamsRunner.class)
public class IssueDirectionHandlerTest {

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    private IssueDirectionHandler handler;

    @Captor
    ArgumentCaptor<Consumer<SscsCaseDetails>> caseDataConsumerCaptor;

    @BeforeEach
    public void setUp() {
        handler = new IssueDirectionHandler(updateCcdCaseService, idamService);
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        assertThrows(IllegalStateException.class, () ->
            handler.handle(callbackType,
                HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, DIRECTION_ISSUED)));
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

    @Test
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        assertThrows(NullPointerException.class, () ->
            handler.canHandle(SUBMITTED, null));
    }

    @Test
    public void givenAnIssueDirectionEventForInterlocCase_thenTriggerAppealToProceedEvent() {
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(
                SscsCaseData.builder().directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString())).build()
        ).build();
        SscsCaseData sscsCaseData = sscsCaseDetails.getData();
        handler.handle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(sscsCaseData, INTERLOCUTORY_REVIEW_STATE, DIRECTION_ISSUED));

        verify(updateCcdCaseService).updateCaseV2(
                eq(1L),
                eq(EventType.APPEAL_TO_PROCEED.getCcdType()),
                eq("Appeal to proceed"),
                eq("Appeal proceed event triggered"),
                any(),
                caseDataConsumerCaptor.capture()
        );

        caseDataConsumerCaptor.getValue().accept(sscsCaseDetails);

        assertNull((sscsCaseData.getDirectionTypeDl()));
    }

    @Test
    public void unProcessableEntityErrorIsReThrown() {
        assertThrows(FeignException.UnprocessableEntity.class, () -> {
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
        });
    }
}
