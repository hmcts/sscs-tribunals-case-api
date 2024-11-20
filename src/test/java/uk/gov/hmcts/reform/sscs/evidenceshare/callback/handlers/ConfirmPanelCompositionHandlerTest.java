package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIRM_PANEL_COMPOSITION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import junitparams.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.PanelCompositionService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


public class ConfirmPanelCompositionHandlerTest {

    ConfirmPanelCompositionHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;
    @Mock
    private IdamService idamService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> consumerArgumentCaptor;

    @BeforeEach
    public void setUp() {
        openMocks(this);

        handler = new ConfirmPanelCompositionHandler(new PanelCompositionService(updateCcdCaseService, idamService));

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(callback.getEvent()).thenReturn(EventType.CONFIRM_PANEL_COMPOSITION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @ParameterizedTest
    public void givenAValidSubmittedEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(YesNo.YES)
                .directionDueDate(LocalDate.now().toString())
                .otherParties(Arrays.asList(buildOtherParty("1", null)))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION)));
    }

    @ParameterizedTest
    @MethodSource("generateAllPossibleOtherPartyWithHearingOptions")
    public void givenFqpmSetAndDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(HearingOptions hearingOptions) {

        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(YesNo.YES)
                .directionDueDate(LocalDate.now().toString())
                .otherParties(Arrays.asList(buildOtherParty("2", hearingOptions)))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), any(Consumer.class));
    }

    @ParameterizedTest
    @MethodSource("generateAllPossibleOtherPartyWithHearingOptions")
    public void givenFqpmSetAndNoDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(HearingOptions hearingOptions) {

        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(YesNo.YES)
                .directionDueDate(null)
                .otherParties(Arrays.asList(buildOtherParty("2", hearingOptions), buildOtherParty("1", null)))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), any(Consumer.class));
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"YES", "NO"})
    public void givenFqpmSetAndNoDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {

        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO)
                .directionDueDate(null)
                .otherParties(Arrays.asList(buildOtherParty("2", null), buildOtherParty("1", HearingOptions.builder().build())))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), any(Consumer.class));
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"YES", "NO"})
    public void givenFqpmSetAndDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable(String isFqpmRequired) {

        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO)
                .directionDueDate(LocalDate.now().toString())
                .otherParties(Arrays.asList(buildOtherParty("2", null), buildOtherParty("1",
                    HearingOptions.builder().excludeDates(new ArrayList<>()).build())))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).triggerCaseEventV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.NOT_LISTABLE.getCcdType()), anyString(), anyString(), any());
    }

    @ParameterizedTest
    @MethodSource("generateOtherPartyOptions")
    public void givenFqpmSetAndDueDateSetAndNoOtherParty_thenCaseStateIsNotListable(List<CcdValue<OtherParty>> otherParties) {

        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(YesNo.YES)
                .directionDueDate(LocalDate.now().toString())
                .otherParties(otherParties)
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, CONFIRM_PANEL_COMPOSITION);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).triggerCaseEventV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
                eq(EventType.NOT_LISTABLE.getCcdType()), anyString(), anyString(), any());
    }

    @ParameterizedTest
    @MethodSource("generateOtherPartyOptions")
    public void givenFqpmSetAndDueDateSetAndDormantAndNoOtherParty_thenCaseStateIsRemainDormant(List<CcdValue<OtherParty>> otherParties) {
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .state(DORMANT_APPEAL_STATE)
                .ccdCaseId("1")
                .isFqpmRequired(YesNo.YES)
                .directionDueDate(LocalDate.now().toString())
                .otherParties(otherParties)
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), DORMANT_APPEAL_STATE, CONFIRM_PANEL_COMPOSITION);

        handler.handle(CallbackType.SUBMITTED, callback);
        verify(updateCcdCaseService, times(0)).updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any(Consumer.class));
    }

    @ParameterizedTest
    public void givenCaseStateIsResponseReceived_thenCaseStateIsUnchangedAndReviewStateIsNone() {
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .state(RESPONSE_RECEIVED)
                .ccdCaseId("1")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), RESPONSE_RECEIVED, CONFIRM_PANEL_COMPOSITION);

        handler.handle(CallbackType.SUBMITTED, callback);
        verify(updateCcdCaseService, times(1)).updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(),
                consumerArgumentCaptor.capture());
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(callback.getCaseDetails().getCaseData()).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(sscsCaseDetails.getData().getInterlocReviewState(), InterlocReviewState.NONE);
    }

    private static Object[] generateOtherPartyOptions() {
        return new Object[]{
            new Object[]{
                null
            },
            new Object[]{
                Collections.emptyList()
            }
        };
    }

    private static Object[] generateAllPossibleOtherPartyWithHearingOptions() {
        return new Object[]{
            new Object[]{
                HearingOptions.builder().wantsToAttend("Yes").build()
            },
            new Object[]{
                HearingOptions.builder().wantsSupport("No").build()
            },
            new Object[]{
                HearingOptions.builder().languageInterpreter("No").build()
            },
            new Object[]{
                HearingOptions.builder().languages("Yes").build()
            },
            new Object[]{
                HearingOptions.builder().arrangements(Collections.singletonList("Arrange")).build()
            },
            new Object[]{
                HearingOptions.builder().scheduleHearing("Yes").build()
            },
            new Object[]{
                HearingOptions.builder().scheduleHearing("Yes").build()
            },
            new Object[]{
                HearingOptions.builder().excludeDates(Collections.singletonList(ExcludeDate.builder().build())).build()
            },
            new Object[]{
                HearingOptions.builder().agreeLessNotice("Yes").build()
            },
            new Object[]{
                HearingOptions.builder().other("Yes").build()
            },
        };
    }

    private CcdValue<OtherParty> buildOtherParty(String id, HearingOptions hearingOptions) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .unacceptableCustomerBehaviour(YesNo.YES)
                .hearingOptions(hearingOptions)
                .build()).build();
    }
}
