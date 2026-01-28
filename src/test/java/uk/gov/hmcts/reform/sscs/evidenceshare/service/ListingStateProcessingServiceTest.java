package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private SscsCaseData caseData;
    private CaseDetails<SscsCaseData> caseDetails;
    private Callback<SscsCaseData> callback;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> caseDetailsConsumerCaptor;

    @InjectMocks
    private ListingStateProcessingService service;

    @BeforeEach
    void setup() {
        caseData = buildCaseData("Bloggs");
        caseData.setCcdCaseId(CCD_CASE_ID);
        caseData.getAppeal().getAppellant().getIdentity().setNino(NINO);
        caseData.setDirectionDueDate(DIRECTION_DUE_DATE);
        caseData.setState(State.APPEAL_CREATED);

        refreshCallback(EventType.READY_TO_LIST);
    }

    @Test
    void givenResponseReceivedCase_whenProcessing_thenInterlocReviewSetToNone() {
        caseData.setState(RESPONSE_RECEIVED);
        refreshCallback(EventType.READY_TO_LIST);

        service.processCaseState(callback, caseData, CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService).updateCaseV2(anyLong(), eq(EventType.INTERLOC_REVIEW_STATE_AMEND.getType()), eq(""), eq(""),
            any(), caseDetailsConsumerCaptor.capture());

        applyCapturedConsumerTo(caseData);

        assertThat(caseData.getInterlocReviewState()).isEqualTo(InterlocReviewState.NONE);
    }

    @Test
    void givenDormantCase_whenProcessing_thenNoUpdatesTriggered() {
        caseData.setState(State.DORMANT_APPEAL_STATE);
        refreshCallback(EventType.READY_TO_LIST);

        service.processCaseState(callback, caseData, CONFIRM_PANEL_COMPOSITION);

        verifyNoInteractions(updateCcdCaseService);
    }

    @Test
    void givenNonDormantCase_whenProcessing_thenTriggersUpdate() {
        service.processCaseState(callback, caseData, CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService, times(1)).triggerCaseEventV2(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void givenWithDwpCaseAndFqpmNull_whenProcessing_thenDoesNotUpdateCaseV2() {
        caseData.setIsFqpmRequired(null);
        caseData.setState(State.WITH_DWP);
        refreshCallback(EventType.READY_TO_LIST);

        service.processCaseState(callback, caseData, CONFIRM_PANEL_COMPOSITION);

        verifyNoInteractions(updateCcdCaseService);
    }

    @Test
    void givenFqpmNotSetYet_whenProcessing_thenTriggersNotListable() {
        caseData.setIsFqpmRequired(null);

        service.processCaseState(callback, caseData, CONFIRM_PANEL_COMPOSITION);

        verifyNotListableTriggered();
    }

    @Test
    void givenOtherPartyWithoutMeaningfulHearingOptions_whenProcessing_thenTriggersNotListable() {
        withOtherPartyHearingOptions(HearingOptions.builder().build());
        withFqpmRequired(YesNo.NO);

        service.processCaseState(callback, caseData, CONFIRM_PANEL_COMPOSITION);

        verifyNotListableTriggered();
    }

    @Test
    void givenWithDwpCase_whenProcessing_thenDoesNotTriggerNotListable() {
        caseData.setIsFqpmRequired(null);
        caseData.setState(State.WITH_DWP);
        refreshCallback(EventType.READY_TO_LIST);

        service.processCaseState(callback, caseData, CONFIRM_PANEL_COMPOSITION);

        verifyNoInteractions(updateCcdCaseService);
    }

    @ParameterizedTest
    @MethodSource("hearingOptionsThatRequireDirectionDueDateCleared")
    void givenFqpmRequiredAndOtherPartyOptions_whenUpdateOtherPartyData_thenDirectionDueDateCleared(HearingOptions options) {
        withOtherPartyHearingOptions(options);
        withFqpmRequired(YesNo.YES);

        service.processCaseState(callback, caseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verifyReadyToListUpdateCaseV2Captured();
        applyCapturedConsumerTo(caseData);

        assertThat(caseData.getDirectionDueDate()).isNull();
    }

    @ParameterizedTest
    @MethodSource("hearingOptionsThatTriggerNotListable")
    void givenFqpmRequiredAndOtherPartyOptions_whenUpdateOtherPartyData_thenNotListableAndDirectionDueDateKept(
        HearingOptions options) {
        withOtherPartyHearingOptions(options);
        withFqpmRequired(YesNo.YES);

        service.processCaseState(callback, caseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verifyNotListableTriggered();
        assertThat(caseData.getDirectionDueDate()).isEqualTo(DIRECTION_DUE_DATE);
    }

    @ParameterizedTest
    @MethodSource("emptyOrNullOtherParties")
    void givenNoOrNullOtherPartiesAndFqpmRequired_whenUpdateOtherPartyData_thenNotListableAndDirectionDueDateKept(
        List<CcdValue<OtherParty>> otherParties) {
        caseData.setOtherParties(otherParties);
        withFqpmRequired(YesNo.YES);

        service.processCaseState(callback, caseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verifyNotListableTriggered();
        assertThat(caseData.getDirectionDueDate()).isEqualTo(DIRECTION_DUE_DATE);
    }

    @Test
    void givenDirectionDueDateNull_whenReadyToListUpdateApplied_thenStillNull() {
        withOtherPartyHearingOptions(HearingOptions.builder().scheduleHearing("Yes").build());
        withFqpmRequired(YesNo.YES);
        caseData.setDirectionDueDate(null);

        service.processCaseState(callback, caseData, CONFIRM_PANEL_COMPOSITION);

        verifyReadyToListUpdateCaseV2Captured();
        applyCapturedConsumerTo(caseData);

        assertThat(caseData.getDirectionDueDate()).isNull();
    }

    @Test
    void givenChildSupport_whenConfirmPanelComposition_thenDoesNotUpdateCase() {
        withOtherPartyHearingOptions(HearingOptions.builder().scheduleHearing("Yes").build());
        withFqpmRequired(YesNo.YES);
        setBenefit(Benefit.CHILD_SUPPORT);

        refreshCallback(CONFIRM_PANEL_COMPOSITION);

        service.processCaseState(callback, caseData, CONFIRM_PANEL_COMPOSITION);

        verifyNoMoreInteractions(updateCcdCaseService);
    }

    @ParameterizedTest
    @MethodSource("benefitAndEventTypes")
    void givenNotChildSupportOrDifferentEvent_whenProcessing_thenUpdatesCase(Benefit benefit, EventType eventType) {
        withOtherPartyHearingOptions(HearingOptions.builder().scheduleHearing("Yes").build());
        withFqpmRequired(YesNo.YES);
        setBenefit(benefit);

        refreshCallback(eventType);

        service.processCaseState(callback, caseData, eventType);

        verifyReadyToListUpdateCaseV2Captured();
    }

    private static CcdValue<OtherParty> otherPartyWith(HearingOptions hearingOptions) {
        return CcdValue.<OtherParty>builder().value(OtherParty.builder().id("1").hearingOptions(hearingOptions).build()).build();
    }

    private static Stream<HearingOptions> hearingOptionsThatRequireDirectionDueDateCleared() {
        return Stream.of(HearingOptions.builder().scheduleHearing("Yes").build(),
            HearingOptions.builder().wantsSupport("Yes").build(), HearingOptions.builder().wantsToAttend("Yes").build(),
            HearingOptions.builder().languageInterpreter("Yes").build(), HearingOptions.builder().languages("Telugu").build(),
            HearingOptions.builder().signLanguageType("British Sign Language (BSL)").build(),
            HearingOptions.builder().arrangements(List.of("any")).build(),
            HearingOptions.builder().agreeLessNotice("Yes").build(), HearingOptions.builder().other("Yes").build(),
            HearingOptions.builder().excludeDates(List.of(ExcludeDate.builder().build())).build());
    }

    private static Stream<HearingOptions> hearingOptionsThatTriggerNotListable() {
        return Stream.of(null, HearingOptions.builder().build(), HearingOptions.builder().arrangements(List.of()).build(),
            HearingOptions.builder().excludeDates(List.of()).build());
    }

    private static Stream<Arguments> benefitAndEventTypes() {
        return Stream.of(Arguments.of(Benefit.PIP, EventType.CONFIRM_PANEL_COMPOSITION),
            Arguments.of(Benefit.CHILD_SUPPORT, EventType.UPDATE_OTHER_PARTY_DATA));
    }

    private static Stream<Arguments> emptyOrNullOtherParties() {
        return Stream.of(Arguments.of(List.of()), Arguments.of((List<CcdValue<OtherParty>>) null));
    }

    private void withFqpmRequired(YesNo fqpmRequired) {
        caseData.setIsFqpmRequired(fqpmRequired);
    }

    private void withOtherPartyHearingOptions(HearingOptions hearingOptions) {
        caseData.setOtherParties(List.of(otherPartyWith(hearingOptions)));
    }

    private void setBenefit(Benefit benefit) {
        caseData.getAppeal().setBenefitType(BenefitType.builder().code(benefit.getShortName()).build());
    }

    private void refreshCallback(EventType eventType) {
        caseDetails = new CaseDetails<>(CASE_ID, JURISDICTION, caseData.getState(), caseData, now(), CASE_TYPE);
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

    private void applyCapturedConsumerTo(SscsCaseData data) {
        final SscsCaseDetails details = SscsCaseDetails.builder().data(data).build();
        caseDetailsConsumerCaptor.getValue().accept(details);
    }
}
