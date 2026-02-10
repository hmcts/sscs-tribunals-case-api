package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIRM_PANEL_COMPOSITION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;

import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.handlers.evidenceshare.ConfirmPanelCompositionHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.ListingStateProcessingService;

@ExtendWith(MockitoExtension.class)
class ConfirmPanelCompositionHandlerTest {

    @InjectMocks
    private ConfirmPanelCompositionHandler handler;

    @Mock
    private ListingStateProcessingService listingStateProcessingService;

    @Test
    void givenAValidSubmittedEvent_thenReturnTrue() {
        assertThat(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").isFqpmRequired(YesNo.YES).directionDueDate(LocalDate.now().toString())
                .otherParties(singletonList(buildOtherParty()))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build()).build(),
            INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidCanHandleCallbacks")
    void givenACallbackThatThisHandlerDoesNotHandle_whenCanHandleCalled_thenReturnFalse(CallbackType callbackType,
        SscsCaseData caseData, EventType eventType) {
        assertThat(handler.canHandle(callbackType,
            buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, eventType))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("invalidCanHandleCallbacks")
    void givenACallbackThatThisHandlerDoesNotHandle_whenHandleCalled_thenReturnFalse(CallbackType callbackType,
        SscsCaseData caseData, EventType eventType) {
        final Callback<SscsCaseData> sscsCaseDataCallback = buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE,
            eventType);

        final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
            () -> handler.handle(callbackType, sscsCaseDataCallback));

        assertThat(illegalStateException.getMessage()).isEqualTo("Cannot handle callback");

    }

    @Test
    void givenAValidSubmittedEvent_thenProcessesTheCaseState() {
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder().ccdCaseId("1").isFqpmRequired(YesNo.YES).directionDueDate(LocalDate.now().toString())
                .otherParties(singletonList(buildOtherParty()))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build()).build(),
            INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION);

        handler.handle(SUBMITTED, callback);

        verify(listingStateProcessingService).processCaseState(callback, callback.getCaseDetails().getCaseData(),
            callback.getEvent());
    }

    @Test
    void shouldHaveAPriorityOfLatest() {
        assertThat(handler.getPriority()).isEqualTo(DispatchPriority.LATEST);
    }

    private static Stream<Arguments> invalidCanHandleCallbacks() {
        SscsCaseData validChildSupportCaseData = SscsCaseData.builder().ccdCaseId("1").isFqpmRequired(YesNo.YES)
            .directionDueDate(LocalDate.now().toString()).otherParties(singletonList(buildOtherParty()))
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build()).build();

        return Stream.of(Arguments.of(ABOUT_TO_START, validChildSupportCaseData, CONFIRM_PANEL_COMPOSITION),
            Arguments.of(SUBMITTED, validChildSupportCaseData, APPEAL_RECEIVED), Arguments.of(SUBMITTED,
                SscsCaseData.builder().ccdCaseId("1").isFqpmRequired(YesNo.YES).directionDueDate(LocalDate.now().toString())
                    .otherParties(singletonList(buildOtherParty())).build(), CONFIRM_PANEL_COMPOSITION), Arguments.of(SUBMITTED,
                SscsCaseData.builder().ccdCaseId("1").isFqpmRequired(YesNo.YES).directionDueDate(LocalDate.now().toString())
                    .otherParties(singletonList(buildOtherParty())).appeal(Appeal.builder().build()).build(),
                CONFIRM_PANEL_COMPOSITION), Arguments.of(SUBMITTED,
                SscsCaseData.builder().ccdCaseId("1").isFqpmRequired(YesNo.YES).directionDueDate(LocalDate.now().toString())
                    .otherParties(singletonList(buildOtherParty()))
                    .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build()).build(),
                CONFIRM_PANEL_COMPOSITION));
    }

    private static CcdValue<OtherParty> buildOtherParty() {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder().id("1").unacceptableCustomerBehaviour(YesNo.YES).build()).build();
    }
}
