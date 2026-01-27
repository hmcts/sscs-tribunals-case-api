package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
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

    private SscsCaseData sscsCaseData;
    private CaseDetails<SscsCaseData> caseDetails;
    private Callback<SscsCaseData> callback;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> consumerArgumentCaptor;

    @InjectMocks
    private ListingStateProcessingService listingStateProcessingService;

    @BeforeEach
    void setup() {
        sscsCaseData = buildCaseData("Bloggs");
        sscsCaseData.setCcdCaseId("1");
        sscsCaseData.getAppeal().getAppellant().getIdentity().setNino("789123");
        sscsCaseData.setDirectionDueDate("11/01/2023");
        sscsCaseData.setState(State.APPEAL_CREATED);
        updateArguments();
    }

    @Test
    void givenResponseReceivedCase_thenInterLocReviewIsNone() {
        sscsCaseData.setState(RESPONSE_RECEIVED);
        caseDetails = new CaseDetails<>(1234L, "SSCS", RESPONSE_RECEIVED, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), EventType.READY_TO_LIST, false);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService).updateCaseV2(anyLong(), eq(EventType.INTERLOC_REVIEW_STATE_AMEND.getType()), eq(""), eq(""),
            any(), consumerArgumentCaptor.capture());
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertThat(sscsCaseData.getInterlocReviewState()).isEqualTo(InterlocReviewState.NONE);
    }

    @Test
    void givenStateNotDormantCase_andxxx_caseShouldNotUpdate() {
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        updateArguments();
        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);
        verify(updateCcdCaseService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void givenNonDormantCase_caseShouldUpdate() {
        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);
        verify(updateCcdCaseService, times(1)).triggerCaseEventV2(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void givenNonDormantWithDwpCase_caseShouldNotUpdate() {
        sscsCaseData.setIsFqpmRequired(null);
        sscsCaseData.setState(State.WITH_DWP);
        updateArguments();

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService, never()).updateCaseV2(any(), any(), anyString(), anyString(), any(), any());
    }

    @Test
    void givenFqpmNotRequiredYet_thenTriggerNotListable() {
        sscsCaseData.setIsFqpmRequired(null);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService).triggerCaseEventV2(anyLong(), eq(EventType.NOT_LISTABLE.getType()), eq("Not listable"),
            eq("Update to Not Listable as the case is either awaiting hearing enquiry form or for FQPM to be set"), any());
    }

    @Test
    void givenDueDateSetAndOtherPartyWithoutHearingOptions_thenTriggerNotListable() {
        CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder()
            .value(OtherParty.builder().id("2").hearingOptions(HearingOptions.builder().build()).build()).build();
        sscsCaseData.setOtherParties(Collections.singletonList(otherParty));
        sscsCaseData.setIsFqpmRequired(YesNo.NO);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService).triggerCaseEventV2(anyLong(), eq(EventType.NOT_LISTABLE.getType()), eq("Not listable"),
            eq("Update to Not Listable as the case is either awaiting hearing enquiry form or for FQPM to be set"), any());
    }

    // @Test
    // void xxx() {
    //     CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder()
    //         .value(OtherParty.builder().id("2").hearingOptions(HearingOptions.builder().build()).build()).build();
    //     sscsCaseData.setOtherParties(Collections.singletonList(otherParty));
    //     sscsCaseData.setIsFqpmRequired(YesNo.NO);
    //     sscsCaseData.setState(RESPONSE_RECEIVED);
    //     updateArguments();
    //
    //     listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);
    //
    //     verify(updateCcdCaseService).triggerCaseEventV2(anyLong(), eq(EventType.NOT_LISTABLE.getType()), eq("Not listable"),
    //         eq("Update to Not Listable as the case is either awaiting hearing enquiry form or for FQPM to be set"), any());
    // }

    @Test
    void givenWithDwpCase_thenNoNotListableTrigger() {
        sscsCaseData.setIsFqpmRequired(null);
        sscsCaseData.setState(State.WITH_DWP);
        updateArguments();

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService, never()).triggerCaseEventV2(any(), any(), anyString(), anyString(), any());
    }

    @ParameterizedTest
    @MethodSource("hearingOptionsWithoutScheduleHearing")
    void givenCaseFqpmRequiredWithOtherPartHearing_thenRemoveDirectionDueDate(HearingOptions hearingOptions) {
        CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder()
            .value(OtherParty.builder().id("1").hearingOptions(hearingOptions).build())
            .build();
        sscsCaseData.setOtherParties(Collections.singletonList(otherParty));
        sscsCaseData.setIsFqpmRequired(YesNo.YES);
        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verify(updateCcdCaseService).updateCaseV2(anyLong(), eq(EventType.READY_TO_LIST.getType()), eq("Ready to list"),
            eq("Update to ready to list event as there is no further information to assist the tribunal and no dispute."), any(),
            consumerArgumentCaptor.capture());
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertThat(sscsCaseData.getDirectionDueDate()).isNull();
    }

    @Test
    void givenCaseFqpmRequiredWithOtherPartHearing_thenKeepDirectionDueDate() {
        CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder()
            .value(OtherParty.builder().id("1").hearingOptions(HearingOptions.builder().scheduleHearing("Yes").build()).build())
            .build();
        sscsCaseData.setOtherParties(Collections.singletonList(otherParty));
        sscsCaseData.setIsFqpmRequired(YesNo.YES);
        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService).updateCaseV2(anyLong(), eq(EventType.READY_TO_LIST.getType()), eq("Ready to list"),
            eq("Update to ready to list event as there is no further information to assist the tribunal and no dispute."), any(),
            consumerArgumentCaptor.capture());
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo("11/01/2023");
    }

    @ParameterizedTest
    @MethodSource("hearingOptionsWithScheduleHearing")
    void givenCaseFqpmRequiredWithOtherPartHearing_andNonOtherPartyEvent_thenKeepDirectionDueDate(HearingOptions hearingOptions) {
        CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder()
            .value(OtherParty.builder().id("1").hearingOptions(hearingOptions).build()).build();
        sscsCaseData.setOtherParties(Collections.singletonList(otherParty));
        sscsCaseData.setIsFqpmRequired(YesNo.YES);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verify(updateCcdCaseService).triggerCaseEventV2(anyLong(), eq(EventType.NOT_LISTABLE.getType()), eq("Not listable"),
            eq("Update to Not Listable as the case is either awaiting hearing enquiry form or for FQPM to be set"), any());

        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo("11/01/2023");
    }

    @Test
    void xxxxxxxx() {

        sscsCaseData.setOtherParties(List.of());
        sscsCaseData.setIsFqpmRequired(YesNo.YES);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verify(updateCcdCaseService).triggerCaseEventV2(anyLong(), eq(EventType.NOT_LISTABLE.getType()), eq("Not listable"),
            eq("Update to Not Listable as the case is either awaiting hearing enquiry form or for FQPM to be set"), any());

        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo("11/01/2023");
    }

    @Test
    void xxxxxxxxx() {

        sscsCaseData.setOtherParties(null);
        sscsCaseData.setIsFqpmRequired(YesNo.YES);

        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verify(updateCcdCaseService).triggerCaseEventV2(anyLong(), eq(EventType.NOT_LISTABLE.getType()), eq("Not listable"),
            eq("Update to Not Listable as the case is either awaiting hearing enquiry form or for FQPM to be set"), any());

        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo("11/01/2023");
    }

    @Test
    void xx() {
        CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder()
            .value(OtherParty.builder().id("1").hearingOptions(HearingOptions.builder().scheduleHearing("Yes").build()).build())
            .build();
        sscsCaseData.setOtherParties(Collections.singletonList(otherParty));
        sscsCaseData.setIsFqpmRequired(YesNo.YES);
        sscsCaseData.setDirectionDueDate(null);
        listingStateProcessingService.processCaseState(callback, sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService).updateCaseV2(anyLong(), eq(EventType.READY_TO_LIST.getType()), eq("Ready to list"),
            eq("Update to ready to list event as there is no further information to assist the tribunal and no dispute."), any(),
            consumerArgumentCaptor.capture());
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertThat(sscsCaseData.getDirectionDueDate()).isNull();
    }

    private static Stream<HearingOptions> hearingOptionsWithScheduleHearing() {
        return Stream.of(null,
            HearingOptions.builder().build(), HearingOptions.builder().arrangements(List.of()).build(),
            HearingOptions.builder().excludeDates(List.of()).build());
    }

    private static Stream<HearingOptions> hearingOptionsWithoutScheduleHearing() {
        return Stream.of(
            HearingOptions.builder().scheduleHearing("Yes").build(),
            HearingOptions.builder().wantsSupport("Yes").build(),
            HearingOptions.builder().wantsToAttend("Yes").build(),
            HearingOptions.builder().languageInterpreter("Yes").build(),
            HearingOptions.builder().languages("Telugu").build(),
            HearingOptions.builder().signLanguageType("British Sign Language (BSL)").build(),
            HearingOptions.builder().arrangements(List.of("any")).build(),
            HearingOptions.builder().signLanguageType("British Sign Language (BSL)").build(),
            HearingOptions.builder().agreeLessNotice("Yes").build(),
            HearingOptions.builder().other("Yes").build(),
            HearingOptions.builder().excludeDates(List.of(ExcludeDate.builder().build())).build());
    }

    private void updateArguments() {
        caseDetails = new CaseDetails<>(1234L, "SSCS", sscsCaseData.getState(), sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), EventType.READY_TO_LIST, false);
    }
}
