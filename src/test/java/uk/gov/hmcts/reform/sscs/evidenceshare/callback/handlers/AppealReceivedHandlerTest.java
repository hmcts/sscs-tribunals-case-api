package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INTERLOC_VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@ExtendWith(MockitoExtension.class)
class AppealReceivedHandlerTest {

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    private AppealReceivedHandler handler;


    @BeforeEach
    void setUp() {
        handler = new AppealReceivedHandler(updateCcdCaseService, idamService, false);
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        final Callback<SscsCaseData> sscsCaseDataCallback = buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE,
            INTERLOC_VALID_APPEAL);
        assertThatThrownBy(() -> handler.handle(callbackType, sscsCaseDataCallback)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL", "CONFIDENTIALITY_CONFIRMED"})
    void givenAValidAppealReceivedEventForDigitalCase_thenReturnTrue(EventType eventType) {
        assertThat(handler.canHandle(SUBMITTED,
            buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(),
                INTERLOCUTORY_REVIEW_STATE, eventType))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL", "CONFIDENTIALITY_CONFIRMED"})
    void givenAValidAppealReceivedEventForNonDigitalCase_thenReturnFalse(EventType eventType) {
        assertThat(handler.canHandle(SUBMITTED,
            buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(VALID_APPEAL.getId()).build(),
                INTERLOCUTORY_REVIEW_STATE, eventType))).isFalse();
    }

    @Test
    void givenANonAppealEvent_thenReturnFalse() {
        assertThat(handler.canHandle(SUBMITTED,
            buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(),
                INTERLOCUTORY_REVIEW_STATE, DECISION_ISSUED))).isFalse();
    }

    @Test
    void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        assertThatThrownBy(() -> handler.canHandle(SUBMITTED, null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL", "CONFIDENTIALITY_CONFIRMED"})
    void givenValidEventAndDigitalCaseAndTriggerEventV2IsEnabled_thenTriggerAppealReceivedEventUsingTriggerEventV2(
        EventType eventType) {
        handler.handle(SUBMITTED,
            buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(),
                INTERLOCUTORY_REVIEW_STATE, eventType));

        verify(updateCcdCaseService).triggerCaseEventV2(eq(1L), eq(EventType.APPEAL_RECEIVED.getCcdType()), eq("Appeal received"),
            eq("Appeal received event has been triggered from Tribunals API for digital case"), any());
    }

    @Test
    void givenValidEventAndNonDigitalCaseAndTriggerEventV2IsEnabled_thenThrowException() {
        final Callback<SscsCaseData> sscsCaseDataCallback = buildTestCallbackForGivenData(
            SscsCaseData.builder().createdInGapsFrom(VALID_APPEAL.getId()).build(), INTERLOCUTORY_REVIEW_STATE,
            EventType.VALID_APPEAL_CREATED);

        assertThatThrownBy(() -> handler.handle(SUBMITTED, sscsCaseDataCallback)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(updateCcdCaseService);
    }

    @Test
    void shouldReturnLatestPriority() {
        assertThat(handler.getPriority()).isEqualTo(DispatchPriority.LATEST);
    }
}