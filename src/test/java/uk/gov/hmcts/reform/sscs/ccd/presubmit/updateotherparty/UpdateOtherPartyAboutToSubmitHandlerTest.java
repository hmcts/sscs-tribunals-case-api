package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_1;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_2;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_3;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_4;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.Role;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;

class UpdateOtherPartyAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String SSCS5_ROLE_WARNING = "You have entered a role for the Other Party which is not valid "
        + "for an SSCS5 case. This role will be ignored when the event completes.";
    private static final String SSCS2_ROLE_ERROR = "Role is required for the selected case";
    private static final int UUID_SIZE = 36;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm:ss a", Locale.UK);
    private final Appeal appeal = Appeal.builder()
        .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build();
    private UpdateOtherPartyAboutToSubmitHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;
    @Mock
    private IdamService idamService;
    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new UpdateOtherPartyAboutToSubmitHandler(idamService, false);
        when(idamService.getUserDetails(any())).thenReturn(
            UserDetails.builder().roles(singletonList(UserRole.SUPER_USER.getValue())).build());

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        Appeal appeal = Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
            .build();
        sscsCaseData.setAppeal(appeal);
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION)).isInstanceOf(
            IllegalStateException.class);
    }

    @Test
    void givenANonUpdateOtherPartyEvent_thenReturnFalse() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty(ID_2), buildOtherParty(ID_1)));
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAnUpdateOtherPartyEvent_thenReturnTrue() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty(ID_2), buildOtherParty(ID_1)));
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenAUpdateOtherPartyEventWithNullOtherPartiesList_thenReturnFalse() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonAboutToSubmitEvent_thenReturnFalse(CallbackType callbackType) {
        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void givenOtherPartiesUcbIsYes_thenUpdateCaseDataOtherPartyUcb() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty(ID_2), buildOtherParty(ID_1)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getOtherPartyUcb()).isEqualTo(YesNo.YES.getValue());
        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @Test
    void givenNewOtherPartyAdded_thenAssignAnId() {
        sscsCaseData.setOtherParties(singletonList(buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOtherParties()).hasSize(1).extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).hasSize(UUID_SIZE);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });

        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @Test
    void givenExistingOtherParties_thenNewOtherPartyAssignedNewId() {
        sscsCaseData.setOtherParties(
            Arrays.asList(buildOtherParty(ID_2), buildOtherParty(ID_1), buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOtherParties()).hasSize(3).extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_1);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                ;
            }).anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_2);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            }).anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isNotEqualTo(ID_1).isNotEqualTo(ID_2).hasSize(UUID_SIZE);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });

        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @Test
    void givenExistingOtherPartiesWithAppointeeAndRep_thenNewOtherPartyAssignedNewId() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty(ID_2), buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4),
            buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<CcdValue<OtherParty>> otherParties = response.getData().getOtherParties();

        assertThat(response.getData().getOtherParties()).hasSize(3).extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_1);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                ;
                assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
                assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
            }).anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_2);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            }).anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isNotEqualTo(ID_1).isNotEqualTo(ID_2).hasSize(UUID_SIZE);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });

        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @Test
    void givenExistingOtherParties_thenNewOtherPartyAppointeeAndRepAssignedNewId() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithAppointeeAndRep(ID_2, null, null),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4), buildOtherParty(null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOtherParties()).hasSize(3).extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_1);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                ;
                assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
                assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
            }).anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_2);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            }).anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isNotEqualTo(ID_1).isNotEqualTo(ID_2).hasSize(UUID_SIZE);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            });

        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @Test
    void givenEmptyOtherParties_thenSetToNullRatherThanEmpty() {
        sscsCaseData.setOtherParties(emptyList());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getOtherParties()).isNull();
        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @Test
    void givenOtherPartyWantsConfidentiality_thenCaseIsConfidential() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(
                Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build())
            .otherParties(Arrays.asList(buildConfidentialOtherParty(ID_2, YES), buildConfidentialOtherParty(ID_1, NO))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getIsConfidentialCase()).isEqualTo(YES);
        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @Test
    void givenNoOtherPartyWantsConfidentiality_thenCaseIsNotConfidential() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(
                Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build())
            .otherParties(Arrays.asList(buildConfidentialOtherParty(ID_2, NO), buildConfidentialOtherParty(ID_1, NO))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getIsConfidentialCase()).isEqualTo(null);
        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection", "childBenefit", "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    void givenSscs5CaseOtherPartyWithRole_thenWarningIsReturned(String benefit) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, null))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getWarnings().size()).isEqualTo(1);
        assertThat(response.getWarnings().stream().findFirst().get()).isEqualTo(SSCS5_ROLE_WARNING);
        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection", "childBenefit", "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    void givenSscs5CaseOtherPartyWithNoRole_thenNoErrorsOrWarningIsReturned(String benefit) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, null), buildSscs5OtherParty(ID_1, null))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getErrors().size()).isEqualTo(0);
        assertThat(response.getData().getOtherParties().size()).isEqualTo(2);
        assertThat(isSscs5CaseValidated(response.getData().getOtherParties())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection", "childBenefit", "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    void givenSscs5CaseOtherPartyWithRoleWarningIgnored_thenCaseIsUpdated(String benefit) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, null))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.isIgnoreWarnings()).thenReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getErrors().size()).isEqualTo(0);
        assertThat(response.getData().getOtherParties().size()).isEqualTo(2);
        assertThat(isSscs5CaseValidated(response.getData().getOtherParties())).isTrue();
    }

    @Test
    void givenSscs2CaseOtherPartyWithRoleMissing_thenErrorReturned() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(
                Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, null))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getErrors().size()).isEqualTo(1);
        assertThat(response.getErrors().stream().findFirst().get()).isEqualTo(SSCS2_ROLE_ERROR);
    }

    @Test
    void givenSscs2CaseOtherPartyWithRoleEntered_thenNoError() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(
                Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, ""))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @Test
    void givenSscsIbaCaseOtherPartyWithRoleEntered_thenNoRoleCheck() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.INFECTED_BLOOD_COMPENSATION.getShortName()).build())
                .build()).otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, null), buildSscs5OtherParty(ID_1, null))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection", "childBenefit", "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    void givenNonSscs1PaperCaseOtherPartyWantsToAttendYes_thenCaseIsOralAndWarningShown(String shortName) {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(
            Appeal.builder().benefitType(BenefitType.builder().code(shortName).build()).hearingType(HearingType.PAPER.getValue())
                .build()).otherParties(Arrays.asList(buildOtherParty("No", null), buildOtherParty("Yes", NO))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size()).isEqualTo(1);
        assertThat(response.getWarnings().stream().anyMatch(m -> m.contains(
            "The hearing type will be changed from Paper to Oral as at least one of the"
                + " parties to the case would like to attend the hearing"))).isTrue();
        assertThat(response.getData().getAppeal().getHearingType()).isEqualTo(HearingType.ORAL.getValue());
    }

    @Test
    void givenNonSscs1PaperCaseOtherPartyWantsToAttendYesCaseLoader_thenCaseIsOralAndNoWarningShown() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(
                Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build())
                    .hearingType(HearingType.PAPER.getValue()).build())
            .otherParties(Arrays.asList(buildOtherParty("No", null), buildOtherParty("Yes", NO))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getUserDetails(any())).thenReturn(
            UserDetails.builder().roles(List.of("caseworker-sscs-systemupdate")).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getData().getAppeal().getHearingType()).isEqualTo(HearingType.ORAL.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection", "childBenefit", "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    void givenNonSscs1PaperCaseOtherPartyWantsToAttendNo_thenCaseIsNotChangedAndNoWarningShown(String shortName) {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(
            Appeal.builder().benefitType(BenefitType.builder().code(shortName).build()).hearingType(HearingType.PAPER.getValue())
                .build()).otherParties(Arrays.asList(buildOtherParty("No", null), buildOtherParty("No", NO))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getData().getAppeal().getHearingType()).isEqualTo(HearingType.PAPER.getValue());
    }

    @ParameterizedTest
    @CsvSource({"paper,No,Yes", "oral,No,No", "online,Yes,Yes"})
    void givenSscs1CaseOtherPartyWantsToAttendYes_thenHearingTypeNotChangedAndNoWarningShown(String hearingType,
        String wantsToAttend1, String wantsToAttend2) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).hearingType(hearingType).build())
            .otherParties(Arrays.asList(buildOtherParty(wantsToAttend1, null), buildOtherParty(wantsToAttend2, NO))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getData().getAppeal().getHearingType()).isEqualTo(hearingType);
    }

    @Test
    void givenAnyCaseUnavailableDatesSelectedButEmptyExcludedDates_thenThrowError() {
        CcdValue<OtherParty> otherParty1 = buildOtherParty("Yes", NO);
        CcdValue<OtherParty> otherParty2 = buildOtherParty("Yes", NO);
        CcdValue<OtherParty> otherParty3 = buildOtherParty("Yes", NO);

        otherParty1.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty2.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty3.getValue().getHearingOptions().setScheduleHearing("Yes");

        otherParty1.getValue().setRole(Role.builder().name("ReceivingParent").build());
        otherParty2.getValue().setRole(Role.builder().name("PayingParent").build());
        otherParty3.getValue().setRole(Role.builder().name("Other").build());

        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(appeal)
            .otherParties(Arrays.asList(otherParty1, otherParty2, otherParty3)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getErrors().size()).isEqualTo(2);

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        String error2 = iterator.next();
        assertThat(error1).isEqualTo("Add a start date for unavailable dates");
        assertThat(error2).isEqualTo("Add an end date for unavailable dates");
    }

    @Test
    void givenAnyCaseUnavailableDatesSelectedButNoDatesProvided_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("").end("").build()).build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start(null).end(null).build()).build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("2023-01-02").build())
            .build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));

        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(appeal).otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getErrors().size()).isEqualTo(2);

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        String error2 = iterator.next();
        assertThat(error1).isEqualTo("Add a start date for unavailable dates");
        assertThat(error2).isEqualTo("Add an end date for unavailable dates");
    }

    @Test
    void givenAnyCaseUnavailableDatesSelectedButNoStartDatesProvided_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("").end("2023-01-01").build()).build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start(null).end("2023-02-01").build()).build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build())
            .build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));

        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(appeal).otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getErrors().size()).isEqualTo(1);

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        assertThat(error1).isEqualTo("Add a start date for unavailable dates");
    }

    @Test
    void givenAnyCaseUnavailableDatesSelectedButNoEndDatesProvided_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("").build()).build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start("2023-02-01").end(null).build()).build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build())
            .build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));

        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(appeal).otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getErrors().size()).isEqualTo(1);

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        assertThat(error1).isEqualTo("Add an end date for unavailable dates");
    }

    @Test
    void givenAnyCaseUnavailableDatesSelectedButStartDateIsAfterEndDate_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("2023-01-01").build())
            .build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-02").end("2023-01-01").build())
            .build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build())
            .build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));

        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(appeal).otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getErrors().size()).isEqualTo(1);

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        assertThat(error1).isEqualTo("Unavailability start date must be before end date");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("otherPartyConfidentialityCases")
    void confidentialityRequiredChangedDate_isHandledCorrectly(String scenario, SscsCaseData beforeCaseData,
        YesNo currentConfidentiality, boolean shouldUpdateDate, boolean featureFlag) {

        final LocalDateTime now = now().minusHours(1);
        if (beforeCaseData == null) {
            when(callback.getCaseDetailsBefore()).thenReturn(Optional.empty());
            return;
        }

        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        sscsCaseData.setOtherParties(singletonList(buildOtherPartyWithConfidentiality(ID_1, currentConfidentiality, now)));
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        final UpdateOtherPartyAboutToSubmitHandler updateOtherPartyAboutToSubmitHandler = new UpdateOtherPartyAboutToSubmitHandler(
            idamService, featureFlag);

        var response = updateOtherPartyAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if (featureFlag && shouldUpdateDate) {
            assertThat(getOtherParty(response).getValue().getConfidentialityRequiredChangedDate()).isAfter(now);
        } else {
            assertThat(getOtherParty(response).getValue().getConfidentialityRequiredChangedDate()).isEqualTo(now);
        }
    }

    private static Stream<Arguments> otherPartyConfidentialityCases() {
        return Stream.of(
            Arguments.of("no matching before party -> update date",
                SscsCaseData.builder().otherParties(singletonList(buildOtherPartyWithConfidentiality(ID_2, YES, now()))).build(), YES,
                true, true),
            Arguments.of("matching before party, same confidentiality -> do not update",
                SscsCaseData.builder().otherParties(singletonList(buildOtherPartyWithConfidentiality(ID_1, YES, now()))).build(), YES,
                false, true),
            Arguments.of("matching before party, different confidentiality -> update date",
                SscsCaseData.builder().otherParties(singletonList(buildOtherPartyWithConfidentiality(ID_1, NO, now()))).build(), YES,
                true, true),
            Arguments.of("null other parties in before case -> update date", SscsCaseData.builder().otherParties(null).build(),
                YES, true, true),
            Arguments.of("feature flag off and confidentiality changes -> do not update date",
                SscsCaseData.builder().otherParties(singletonList(buildOtherPartyWithConfidentiality(ID_1, NO, now()))).build(),
                YES, false, false));
    }

    private static CcdValue<OtherParty> getOtherParty(PreSubmitCallbackResponse<SscsCaseData> response) {
        return response.getData().getOtherParties().stream().filter(op -> Objects.equals(op.getValue().getId(), ID_1)).findFirst()
            .orElseThrow();
    }

    private static CcdValue<OtherParty> buildOtherPartyWithConfidentiality(String id, YesNo yesNo, LocalDateTime now) {
        return CcdValue.<OtherParty>builder().value(
            OtherParty.builder().id(id).unacceptableCustomerBehaviour(YesNo.YES).role(Role.builder().name("PayingParent").build())
                .confidentialityRequired(yesNo).confidentialityRequiredChangedDate(now).build()).build();
    }

    private CcdValue<OtherParty> buildConfidentialOtherParty(String id, YesNo confidentialityRequired) {
        return CcdValue.<OtherParty>builder().value(OtherParty.builder().id(id).confidentialityRequired(confidentialityRequired)
            .role(Role.builder().name("PayingParent").build()).build()).build();
    }

    private boolean isSscs5CaseValidated(List<CcdValue<OtherParty>> otherParties) {
        return emptyIfNull(otherParties).stream().filter(otherPartyCcdValue -> otherPartyCcdValue.getValue() != null)
            .map(otherPartyCcdValue -> otherPartyCcdValue.getValue())
            .allMatch(otherParty -> otherParty.getShowRole().equals(NO) && otherParty.getRole() == null);
    }

    private CcdValue<OtherParty> buildOtherParty(String wantsToAttend, YesNo confidentiality) {
        return CcdValue.<OtherParty>builder().value(
            OtherParty.builder().confidentialityRequired(confidentiality != null ? confidentiality : NO)
                .hearingOptions(HearingOptions.builder().wantsToAttend(wantsToAttend).build()).build()).build();
    }

    private CcdValue<OtherParty> buildOtherParty(String id) {
        return CcdValue.<OtherParty>builder().value(
            OtherParty.builder().id(id).unacceptableCustomerBehaviour(YesNo.YES).role(Role.builder().name("PayingParent").build())
                .build()).build();
    }

    private CcdValue<OtherParty> buildSscs5OtherParty(String id, String role) {
        return CcdValue.<OtherParty>builder().value(
            OtherParty.builder().id(id).unacceptableCustomerBehaviour(YesNo.NO).confidentialityRequired(NO)
                .role(Role.builder().name(role).build()).build()).build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder().value(
            OtherParty.builder().id(id).isAppointee(YES.getValue()).appointee(Appointee.builder().id(appointeeId).build())
                .rep(Representative.builder().id(repId).hasRepresentative(YES.getValue()).build())
                .role(Role.builder().name("ReceivingParent").build()).build()).build();
    }
}
