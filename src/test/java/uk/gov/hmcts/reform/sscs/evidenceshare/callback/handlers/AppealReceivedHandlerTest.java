package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INTERLOC_VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@RunWith(JUnitParamsRunner.class)
public class AppealReceivedHandlerTest {

    @Mock
    private CcdService ccdService;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    private AppealReceivedHandler handler;


    @BeforeEach
    public void setUp() {
        handler = new AppealReceivedHandler(ccdService, updateCcdCaseService, idamService);
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        assertThrows(IllegalStateException.class, () ->
            handler.handle(callbackType,
                HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, INTERLOC_VALID_APPEAL)));
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    public void givenAValidAppealReceivedEventForDigitalCase_thenReturnTrue(EventType eventType) {
        assertTrue(handler.canHandle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), INTERLOCUTORY_REVIEW_STATE, eventType)));
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    public void givenAValidAppealReceivedEventForNonDigitalCase_thenReturnFalse(EventType eventType) {
        assertFalse(handler.canHandle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(VALID_APPEAL.getId()).build(), INTERLOCUTORY_REVIEW_STATE, eventType)));
    }

    @Test
    public void givenANonAppealEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), INTERLOCUTORY_REVIEW_STATE, DECISION_ISSUED)));
    }

    @Test
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        assertThrows(NullPointerException.class, () ->
            handler.canHandle(SUBMITTED, null));
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    public void givenValidEventAndDigitalCaseAndTriggerEventV2IsDisabled_thenTriggerAppealReceivedEventUsingCcdServiceUpdate(EventType eventType) {
        ReflectionTestUtils.setField(handler, "isTriggerEventV2Enabled", false);

        handler.handle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), INTERLOCUTORY_REVIEW_STATE, eventType));

        verify(ccdService).updateCase(any(), eq(1L), eq(EventType.APPEAL_RECEIVED.getCcdType()), eq("Appeal received"), eq("Appeal received event has been triggered from Evidence Share for digital case"), any());
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    public void givenValidEventAndDigitalCaseAndTriggerEventV2IsEnabled_thenTriggerAppealReceivedEventUsingTriggerEventV2(EventType eventType) {
        ReflectionTestUtils.setField(handler, "isTriggerEventV2Enabled", true);

        handler.handle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), INTERLOCUTORY_REVIEW_STATE, eventType));

        verify(updateCcdCaseService).triggerCaseEventV2(eq(1L), eq(EventType.APPEAL_RECEIVED.getCcdType()), eq("Appeal received"), eq("Appeal received event has been triggered from Tribunals API for digital case"), any());
    }

    @Test
    public void givenValidEventAndNonDigitalCaseAndTriggerEventV2IsDisabled_thenThrowException() {
        assertThrows(IllegalStateException.class, () -> {
            ReflectionTestUtils.setField(handler, "isTriggerEventV2Enabled", false);

            handler.handle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(VALID_APPEAL.getId()).build(), INTERLOCUTORY_REVIEW_STATE, EventType.VALID_APPEAL_CREATED));

            verifyNoInteractions(ccdService, updateCcdCaseService);
        });
    }

    @Test
    public void givenValidEventAndNonDigitalCaseAndTriggerEventV2IsEnabled_thenThrowException() {
        assertThrows(IllegalStateException.class, () -> {
            ReflectionTestUtils.setField(handler, "isTriggerEventV2Enabled", true);

            handler.handle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(VALID_APPEAL.getId()).build(), INTERLOCUTORY_REVIEW_STATE, EventType.VALID_APPEAL_CREATED));

            verifyNoInteractions(ccdService, updateCcdCaseService);
        });
    }
}
