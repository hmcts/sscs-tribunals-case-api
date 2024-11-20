package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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


public class UpdateOtherPartyHandlerTest {

    UpdateOtherPartyHandler handler;

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

        handler = new UpdateOtherPartyHandler(new PanelCompositionService(updateCcdCaseService, idamService));

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(callback.getEvent()).thenReturn(EventType.CONFIRM_PANEL_COMPOSITION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @ParameterizedTest
    public void givenAValidSubmittedEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(YES)
                .directionDueDate(LocalDate.now().toString())
                .otherParties(Arrays.asList(buildOtherParty("2", HearingOptions.builder().build())))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, UPDATE_OTHER_PARTY_DATA)));
    }

    @ParameterizedTest
    @MethodSource("generateAllPossibleOtherPartyWithHearingOptions")
    public void givenFqpmSetAndDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(HearingOptions hearingOptions) {

        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(YesNo.YES)
                .directionDueDate(LocalDate.now().toString())
                .otherParties(Arrays.asList(buildOtherParty("2", hearingOptions)))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, UPDATE_OTHER_PARTY_DATA);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), consumerArgumentCaptor.capture());

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(callback.getCaseDetails().getCaseData()).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertThat(sscsCaseDetails.getData().getDirectionDueDate(), is(nullValue()));
    }

    @ParameterizedTest
    @MethodSource("generateAllPossibleOtherPartyWithHearingOptions")
    public void givenFqpmSetAndNoDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(HearingOptions hearingOptions) {

        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(YesNo.YES)
                .directionDueDate(null)
                .otherParties(Arrays.asList(buildOtherParty("2", hearingOptions)))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, UPDATE_OTHER_PARTY_DATA);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), any(Consumer.class));
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"YES", "NO"})
    public void givenFqpmSetAndNoDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {

        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YES : YesNo.NO)
                .directionDueDate(null)
                .otherParties(Arrays.asList(buildOtherParty("2",
                    HearingOptions.builder().scheduleHearing("Yes").build()), buildOtherParty("1",
                    HearingOptions.builder().build())))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, UPDATE_OTHER_PARTY_DATA);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.READY_TO_LIST.getCcdType()), anyString(), anyString(), any(), any(Consumer.class));
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"YES", "NO"})
    public void givenFqpmSetAndDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable(String isFqpmRequired) {

        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YES : YesNo.NO)
                .directionDueDate(LocalDate.now().toString())
                .otherParties(Arrays.asList(buildOtherParty("1",
                    HearingOptions.builder().excludeDates(new ArrayList<>()).build())))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, UPDATE_OTHER_PARTY_DATA);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).triggerCaseEventV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.NOT_LISTABLE.getCcdType()), anyString(), anyString(), any());
    }

    @ParameterizedTest
    public void givenNoFqpmSetAndNoDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable() {

        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(null)
                .directionDueDate(null)
                .otherParties(Arrays.asList(buildOtherParty("1", null)))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, UPDATE_OTHER_PARTY_DATA);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).triggerCaseEventV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.NOT_LISTABLE.getCcdType()), anyString(), anyString(), any());
    }

    @ParameterizedTest
    public void givenNoFqpmSetAndDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable() {

        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(null)
                .directionDueDate(LocalDate.now().toString())
                .otherParties(Arrays.asList(buildOtherParty("1", null)))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, UPDATE_OTHER_PARTY_DATA);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).triggerCaseEventV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.NOT_LISTABLE.getCcdType()), anyString(), anyString(), any());
    }

    @ParameterizedTest
    @MethodSource("generateAllPossibleOtherPartyWithHearingOptions")
    public void givenNoFqpmSetAndNoDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable(HearingOptions hearingOptions) {

        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(null)
                .directionDueDate(null)
                .otherParties(Arrays.asList(buildOtherParty("2", hearingOptions)))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, UPDATE_OTHER_PARTY_DATA);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).triggerCaseEventV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.NOT_LISTABLE.getCcdType()), anyString(), anyString(), any());
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"APPEAL_CREATED, 1", "DORMANT_APPEAL_STATE, 0", "DRAFT, 1", "DRAFT_ARCHIVED, 1",
        "HEARING, 1", "INCOMPLETE_APPLICATION, 1", "INCOMPLETE_APPLICATION_INFORMATION_REQUESTED, 1",
        "INTERLOCUTORY_REVIEW_STATE, 1", "POST_HEARING, 1", "READY_TO_LIST, 1", "RESPONSE_RECEIVED, 0",
        "VALID_APPEAL, 1", "WITH_DWP, 0"})
    public void givenAnInitialStateThenTriggerNonListableEventType(State currentState, int wantedNumberOfInvocations) {
        final Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(
            SscsCaseData.builder()
                .ccdCaseId("1")
                .isFqpmRequired(null)
                .directionDueDate(null)
                .otherParties(Arrays.asList(buildOtherParty("2", HearingOptions.builder().wantsToAttend("Yes").build())))
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build())
                    .build()).build(), currentState, UPDATE_OTHER_PARTY_DATA);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService, times(wantedNumberOfInvocations)).triggerCaseEventV2(eq(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId())),
            eq(EventType.NOT_LISTABLE.getCcdType()), anyString(), anyString(), any());
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
                .hearingOptions(hearingOptions)
                .unacceptableCustomerBehaviour(YesNo.YES)
                .build())
            .build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .isAppointee(YES.getValue())
                .appointee(Appointee.builder().id(appointeeId).build())
                .rep(Representative.builder().id(repId).hasRepresentative(YES.getValue()).build())
                .build())
            .build();
    }
}
