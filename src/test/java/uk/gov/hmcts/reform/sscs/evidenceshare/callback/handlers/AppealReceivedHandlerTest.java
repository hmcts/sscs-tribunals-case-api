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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@ExtendWith(MockitoExtension.class)
class AppealReceivedHandlerTest {

    @Mock
    private CcdService ccdService;

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
    @ValueSource(strings = {"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    void givenCallbackIsNotSubmitted_willThrowAnException(final String callbackTypeName) {
        final CallbackType callbackType = CallbackType.valueOf(callbackTypeName);
        final var callback = HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, INTERLOC_VALID_APPEAL);
        assertThatThrownBy(() -> handler.handle(callbackType, callback)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    void givenAValidAppealReceivedEventForDigitalCase_thenReturnTrue(final EventType eventType) {
        assertThat(handler.canHandle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(),
                INTERLOCUTORY_REVIEW_STATE, eventType))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    void givenAValidAppealReceivedEventForNonDigitalCase_thenReturnFalse(final EventType eventType) {
        assertThat(handler.canHandle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(VALID_APPEAL.getId()).build(),
                INTERLOCUTORY_REVIEW_STATE, eventType))).isFalse();
    }

    @Test
    void givenANonAppealEvent_thenReturnFalse() {
        assertThat(handler.canHandle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(),
                INTERLOCUTORY_REVIEW_STATE, DECISION_ISSUED))).isFalse();
    }

    @Test
    void getPriority_returnsLatest() {
        assertThat(handler.getPriority()).isEqualTo(DispatchPriority.LATEST);
    }

    @Test
    void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        assertThatThrownBy(() -> handler.canHandle(SUBMITTED, null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    void givenValidEventAndDigitalCaseAndTriggerEventV2IsEnabled_thenTriggerAppealReceivedEventUsingTriggerEventV2(
        final EventType eventType) {
        handler.handle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(),
                INTERLOCUTORY_REVIEW_STATE, eventType));

        verify(updateCcdCaseService).triggerCaseEventV2(eq(1L), eq(EventType.APPEAL_RECEIVED.getCcdType()), eq("Appeal received"),
            eq("Appeal received event has been triggered from Tribunals API for digital case"), any());
    }

    @Test
    void givenValidEventAndNonDigitalCaseAndTriggerEventV2IsEnabled_thenThrowException() {
        final var callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder().createdInGapsFrom(VALID_APPEAL.getId()).build(), INTERLOCUTORY_REVIEW_STATE,
            EventType.VALID_APPEAL_CREATED);
        assertThatThrownBy(() -> handler.handle(SUBMITTED, callback)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(ccdService, updateCcdCaseService);
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    void givenChildSupportCaseAndFlagEnabled_canHandleReturnsFalse(final EventType eventType) {
        final AppealReceivedHandler handlerWithFlagOn = new AppealReceivedHandler(updateCcdCaseService, idamService,
            true);
        assertThat(handlerWithFlagOn.canHandle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(childSupportCaseData(), INTERLOCUTORY_REVIEW_STATE,
                eventType))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    void givenChildSupportCaseAndFlagDisabled_canHandleReturnsTrue(final EventType eventType) {
        assertThat(handler.canHandle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(childSupportCaseData(), INTERLOCUTORY_REVIEW_STATE, eventType))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    void givenNonChildSupportCaseAndFlagEnabled_canHandleReturnsTrue(final EventType eventType) {
        final AppealReceivedHandler handlerWithFlagOn = new AppealReceivedHandler(updateCcdCaseService, idamService,
            true);
        assertThat(handlerWithFlagOn.canHandle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(),
                INTERLOCUTORY_REVIEW_STATE, eventType))).isTrue();
    }

    @Test
    void givenChildSupportCaseAndFlagEnabled_handleThrowsIllegalStateException() {
        final AppealReceivedHandler handlerWithFlagOn = new AppealReceivedHandler(updateCcdCaseService, idamService,
            true);
        final var callback = HandlerHelper.buildTestCallbackForGivenData(childSupportCaseData(), INTERLOCUTORY_REVIEW_STATE,
            EventType.VALID_APPEAL_CREATED);
        assertThatThrownBy(() -> handlerWithFlagOn.handle(SUBMITTED, callback)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(updateCcdCaseService);
    }

    @Test
    void givenChildSupportCaseAndConfidentialityConfirmedEventAndFlagEnabled_canHandleReturnsTrue() {
        final AppealReceivedHandler handlerWithFlagOn = new AppealReceivedHandler(updateCcdCaseService, idamService,
            true);
        assertThat(handlerWithFlagOn.canHandle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(childSupportCaseData(), INTERLOCUTORY_REVIEW_STATE,
                EventType.CONFIDENTIALITY_CONFIRMED))).isTrue();
    }

    @Test
    void givenChildSupportCaseAndConfidentialityConfirmedEventAndFlagEnabled_handleTriggersAppealReceived() {
        final AppealReceivedHandler handlerWithFlagOn = new AppealReceivedHandler(updateCcdCaseService, idamService,
            true);
        handlerWithFlagOn.handle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(childSupportCaseData(), INTERLOCUTORY_REVIEW_STATE,
                EventType.CONFIDENTIALITY_CONFIRMED));
        verify(updateCcdCaseService).triggerCaseEventV2(eq(1L), eq(EventType.APPEAL_RECEIVED.getCcdType()),
            eq("Appeal received"), eq("Appeal received event has been triggered from Tribunals API for digital case"), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    void givenChildSupportCaseAndFlagEnabledAndNotSubmitted_canHandleReturnsFalse(final String callbackTypeName) {
        final AppealReceivedHandler handlerWithFlagOn = new AppealReceivedHandler(updateCcdCaseService, idamService, true);
        final CallbackType callbackType = CallbackType.valueOf(callbackTypeName);
        assertThat(handlerWithFlagOn.canHandle(callbackType,
            HandlerHelper.buildTestCallbackForGivenData(childSupportCaseData(), INTERLOCUTORY_REVIEW_STATE,
                EventType.CONFIDENTIALITY_CONFIRMED))).isFalse();
    }

    @Test
    void givenChildSupportCaseAndFlagEnabledAndNonDigitalCase_canHandleReturnsFalse() {
        final AppealReceivedHandler handlerWithFlagOn = new AppealReceivedHandler(updateCcdCaseService, idamService, true);
        final SscsCaseData nonDigitalChildSupportCase = SscsCaseData.builder()
            .createdInGapsFrom(VALID_APPEAL.getId())
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
            .build();
        assertThat(handlerWithFlagOn.canHandle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(nonDigitalChildSupportCase, INTERLOCUTORY_REVIEW_STATE,
                EventType.CONFIDENTIALITY_CONFIRMED))).isFalse();
    }

    @Test
    void givenNonChildSupportCaseAndFlagEnabled_confidentialityConfirmedEvent_canHandleReturnsFalse() {
        final AppealReceivedHandler handlerWithFlagOn = new AppealReceivedHandler(updateCcdCaseService, idamService, true);
        assertThat(handlerWithFlagOn.canHandle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(),
                INTERLOCUTORY_REVIEW_STATE, EventType.CONFIDENTIALITY_CONFIRMED))).isFalse();
    }

    @Test
    void givenChildSupportCaseAndFlagDisabled_confidentialityConfirmedEvent_canHandleReturnsFalse() {
        assertThat(handler.canHandle(SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(childSupportCaseData(), INTERLOCUTORY_REVIEW_STATE,
                EventType.CONFIDENTIALITY_CONFIRMED))).isFalse();
    }

    private static SscsCaseData childSupportCaseData() {
        return SscsCaseData
            .builder()
            .createdInGapsFrom(READY_TO_LIST.getId())
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
            .build();
    }
}
