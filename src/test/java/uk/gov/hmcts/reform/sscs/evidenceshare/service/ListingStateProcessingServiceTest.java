package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIRM_PANEL_COMPOSITION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@ExtendWith(MockitoExtension.class)
class ListingStateProcessingServiceTest {

    private static final long CASE_ID = 1234L;
    private static final String CCD_CASE_ID = "1";
    private static final String NINO = "789123";
    private static final String DIRECTION_DUE_DATE = "11/01/2023";
    private static final String JURISDICTION = "SSCS";
    private static final String CASE_TYPE = "Benefit";

    private static final String READY_TO_LIST_SUMMARY = "Ready to list";
    private static final String READY_TO_LIST_DESCRIPTION = "Update to ready to list event as there is no further information to assist the tribunal and no dispute.";

    private static final String NOT_LISTABLE_SUMMARY = "Not listable";
    private static final String NOT_LISTABLE_DESCRIPTION = "Update to Not Listable as the case is either awaiting hearing enquiry form or for FQPM to be set";

    private SscsCaseData sscsCaseData;
    private CaseDetails<SscsCaseData> caseDetails;
    private Callback<SscsCaseData> callback;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> caseDetailsConsumerCaptor;

    @InjectMocks
    private ListingStateProcessingService listingStateProcessingService;

    @BeforeEach
    void setup() {
        sscsCaseData = buildCaseData("Bloggs");
        sscsCaseData.setCcdCaseId(CCD_CASE_ID);
        sscsCaseData.getAppeal().getAppellant().getIdentity().setNino(NINO);
        sscsCaseData.setDirectionDueDate(DIRECTION_DUE_DATE);
        sscsCaseData.setState(State.APPEAL_CREATED);

        refreshCallback(EventType.READY_TO_LIST);
    }

    @Test
    void givenResponseReceivedCase_thenInterlocReviewIsNone() {
        sscsCaseData.setState(RESPONSE_RECEIVED);
        refreshCallback(EventType.READY_TO_LIST);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService).updateCaseV2(anyLong(), eq(EventType.INTERLOC_REVIEW_STATE_AMEND.getType()), eq(""), eq(""),
            any(), caseDetailsConsumerCaptor.capture());

        applyCapturedConsumer();

        assertThat(sscsCaseData.getInterlocReviewState()).isEqualTo(InterlocReviewState.NONE);
    }

    @Test
    void givenDormantCase_thenCaseShouldNotUpdate() {
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        refreshCallback(EventType.READY_TO_LIST);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
        verify(updateCcdCaseService, never()).triggerCaseEventV2(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void givenNonDormantCase_thenCaseShouldUpdate() {
        listingStateProcessingService.processCaseState(callback, sscsCaseData, CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService, times(1)).triggerCaseEventV2(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void givenWithDwpCaseAndFqpmNull_thenCaseShouldNotUpdateCaseV2() {
        sscsCaseData.setIsFqpmRequired(null);
        sscsCaseData.setState(State.WITH_DWP);
        refreshCallback(EventType.READY_TO_LIST);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService, never()).updateCaseV2(any(), any(), anyString(), anyString(), any(), any());
    }

    @Test
    void givenFqpmNotRequiredYet_thenTriggerNotListable() {
        sscsCaseData.setIsFqpmRequired(null);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, CONFIRM_PANEL_COMPOSITION);

        verifyNotListableTriggered();
    }

    @Test
    void givenDueDateSetAndOtherPartyWithoutHearingOptions_thenTriggerNotListable() {
        sscsCaseData.setOtherParties(List.of(otherPartyWith(HearingOptions.builder().build())));
        sscsCaseData.setIsFqpmRequired(YesNo.NO);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, CONFIRM_PANEL_COMPOSITION);

        verifyNotListableTriggered();
    }

    @Test
    void givenWithDwpCase_thenNoNotListableTrigger() {
        sscsCaseData.setIsFqpmRequired(null);
        sscsCaseData.setState(State.WITH_DWP);
        refreshCallback(EventType.READY_TO_LIST);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService, never()).triggerCaseEventV2(any(), any(), anyString(), anyString(), any());
    }

    @ParameterizedTest
    @MethodSource("hearingOptionsWithoutScheduleHearing")
    void givenCaseFqpmRequiredWithOtherPartyHearingOptions_andUpdateOtherPartyDataEvent_thenRemoveDirectionDueDate(
        HearingOptions hearingOptions) {
        sscsCaseData.setOtherParties(List.of(otherPartyWith(hearingOptions)));
        sscsCaseData.setIsFqpmRequired(YesNo.YES);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verifyReadyToListUpdateCaseV2Captured();
        applyCapturedConsumer();

        assertThat(sscsCaseData.getDirectionDueDate()).isNull();
    }

    @Test
    void givenCaseFqpmRequiredWithScheduleHearingYes_andConfirmPanelCompositionEvent_thenKeepDirectionDueDate() {
        sscsCaseData.setOtherParties(List.of(otherPartyWith(HearingOptions.builder().scheduleHearing("Yes").build())));
        sscsCaseData.setIsFqpmRequired(YesNo.YES);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, CONFIRM_PANEL_COMPOSITION);

        verifyReadyToListUpdateCaseV2Captured();
        applyCapturedConsumer();

        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(DIRECTION_DUE_DATE);
    }

    @ParameterizedTest
    @MethodSource("hearingOptionsWithScheduleHearing")
    void givenCaseFqpmRequiredWithOtherPartyHearingOptions_andUpdateOtherPartyDataEvent_thenTriggerNotListableAndKeepDirectionDueDate(
        HearingOptions hearingOptions) {
        sscsCaseData.setOtherParties(List.of(otherPartyWith(hearingOptions)));
        sscsCaseData.setIsFqpmRequired(YesNo.YES);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verifyNotListableTriggered();
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(DIRECTION_DUE_DATE);
    }

    @Test
    void givenNoOtherPartiesAndFqpmRequired_thenTriggerNotListableAndKeepDirectionDueDate() {
        sscsCaseData.setOtherParties(List.of());
        sscsCaseData.setIsFqpmRequired(YesNo.YES);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verifyNotListableTriggered();
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(DIRECTION_DUE_DATE);
    }

    @Test
    void givenNullOtherPartiesAndFqpmRequired_thenTriggerNotListableAndKeepDirectionDueDate() {
        sscsCaseData.setOtherParties(null);
        sscsCaseData.setIsFqpmRequired(YesNo.YES);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verifyNotListableTriggered();
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(DIRECTION_DUE_DATE);
    }

    @Test
    void givenDirectionDueDateNull_thenReadyToListUpdateKeepsItNull() {
        sscsCaseData.setOtherParties(List.of(otherPartyWith(HearingOptions.builder().scheduleHearing("Yes").build())));
        sscsCaseData.setIsFqpmRequired(YesNo.YES);
        sscsCaseData.setDirectionDueDate(null);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, CONFIRM_PANEL_COMPOSITION);

        verifyReadyToListUpdateCaseV2Captured();
        applyCapturedConsumer();

        assertThat(sscsCaseData.getDirectionDueDate()).isNull();
    }

    @Test
    void givenChildSupportForConfirmPanelComposition_thenShouldNotUpdateCase() {
        sscsCaseData.setOtherParties(List.of(otherPartyWith(HearingOptions.builder().scheduleHearing("Yes").build())));
        sscsCaseData.setIsFqpmRequired(YesNo.YES);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

        refreshCallback(CONFIRM_PANEL_COMPOSITION);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, CONFIRM_PANEL_COMPOSITION);

        verifyNoMoreInteractions(updateCcdCaseService);
    }

    @ParameterizedTest
    @MethodSource("benefitAndEventTypes")
    void givenNotChildSupportAndConfirmPanelComposition_thenUpdateCase(Benefit benefit, EventType eventType) {
        sscsCaseData.setOtherParties(List.of(otherPartyWith(HearingOptions.builder().scheduleHearing("Yes").build())));
        sscsCaseData.setIsFqpmRequired(YesNo.YES);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(benefit.getShortName()).build());

        refreshCallback(eventType);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, eventType);

        verifyReadyToListUpdateCaseV2Captured();
    }

    private static CcdValue<OtherParty> otherPartyWith(HearingOptions hearingOptions) {
        return CcdValue.<OtherParty>builder().value(OtherParty.builder().id("1").hearingOptions(hearingOptions).build()).build();
    }

    private static Stream<HearingOptions> hearingOptionsWithScheduleHearing() {
        return Stream.of(null, HearingOptions.builder().build(), HearingOptions.builder().arrangements(List.of()).build(),
            HearingOptions.builder().excludeDates(List.of()).build());
    }

    private static Stream<HearingOptions> hearingOptionsWithoutScheduleHearing() {
        return Stream.of(HearingOptions.builder().scheduleHearing("Yes").build(),
            HearingOptions.builder().wantsSupport("Yes").build(), HearingOptions.builder().wantsToAttend("Yes").build(),
            HearingOptions.builder().languageInterpreter("Yes").build(), HearingOptions.builder().languages("Telugu").build(),
            HearingOptions.builder().signLanguageType("British Sign Language (BSL)").build(),
            HearingOptions.builder().arrangements(List.of("any")).build(),
            HearingOptions.builder().agreeLessNotice("Yes").build(), HearingOptions.builder().other("Yes").build(),
            HearingOptions.builder().excludeDates(List.of(ExcludeDate.builder().build())).build());
    }

    private static Stream<Arguments> benefitAndEventTypes() {
        return Stream.of(
            Arguments.of(Benefit.PIP, EventType.CONFIRM_PANEL_COMPOSITION),
            Arguments.of(Benefit.CHILD_SUPPORT, EventType.UPDATE_OTHER_PARTY_DATA)
        );
    }

    private void refreshCallback(EventType eventType) {
        caseDetails = new CaseDetails<>(CASE_ID, JURISDICTION, sscsCaseData.getState(), sscsCaseData, now(), CASE_TYPE);
        callback = new Callback<>(caseDetails, empty(), eventType, false);
    }

    private void verifyNotListableTriggered() {
        verify(updateCcdCaseService).triggerCaseEventV2(anyLong(), eq(EventType.NOT_LISTABLE.getType()), eq(NOT_LISTABLE_SUMMARY),
            eq(NOT_LISTABLE_DESCRIPTION), any());
    }

    private void verifyReadyToListUpdateCaseV2Captured() {
        verify(updateCcdCaseService).updateCaseV2(anyLong(), eq(EventType.READY_TO_LIST.getType()), eq(READY_TO_LIST_SUMMARY),
            eq(READY_TO_LIST_DESCRIPTION), any(), caseDetailsConsumerCaptor.capture());
    }

    private void applyCapturedConsumer() {
        final SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        caseDetailsConsumerCaptor.getValue().accept(sscsCaseDetails);
    }
}
