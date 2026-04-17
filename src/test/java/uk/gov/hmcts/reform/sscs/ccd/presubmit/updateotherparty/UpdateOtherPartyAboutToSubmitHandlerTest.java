package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static java.time.LocalDate.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.issuehearingenquiryform.IssueHearingEnquiryFormAboutToSubmit.getHearingResponseExpectedByDays;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_1;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_2;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_3;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_4;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.Role;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;

@ExtendWith(MockitoExtension.class)
class UpdateOtherPartyAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String SSCS5_ROLE_WARNING = "You have entered a role for the Other Party which is not valid "
        + "for an SSCS5 case. This role will be ignored when the event completes.";
    private static final String SSCS2_ROLE_ERROR = "Role is required for the selected case";
    private static final int UUID_SIZE = 36;
    private UpdateOtherPartyAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    private SscsCaseData sscsCaseData;

    private final Appeal appeal = Appeal.builder()
            .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
            .build();

    @BeforeEach
    void setUp() {
        handler = new UpdateOtherPartyAboutToSubmitHandler(idamService, false);
        lenient().when(idamService.getUserDetails(any())).thenReturn(UserDetails.builder().roles(
            singletonList(UserRole.SUPER_USER.getValue())).build());

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        lenient().when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
        lenient().when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setAppeal(Appeal.builder()
                                      .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build());
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class);
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
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonAboutToSubmitEvent_thenReturnFalse(final CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void givenOtherPartiesUcbIsYes_thenUpdateCaseDataOtherPartyUcb() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty(ID_2), buildOtherParty(ID_1)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getOtherPartyUcb()).isEqualTo(YesNo.YES.getValue());
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenNewOtherPartyAdded_thenAssignAnId() {
        sscsCaseData.setOtherParties(singletonList(
            buildOtherPartyWithAppointeeAndRep(null, null, null)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOtherParties())
            .hasSize(1)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).hasSize(UUID_SIZE);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenExistingOtherParties_thenNewOtherPartyAssignedNewId() {
        sscsCaseData.setOtherParties(Arrays.asList(
            buildOtherParty(ID_2),
            buildOtherParty(ID_1),
            buildOtherPartyWithAppointeeAndRep(null, null, null)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOtherParties())
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_1);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            })
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_2);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            })
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId())
                    .isNotEqualTo(ID_1)
                    .isNotEqualTo(ID_2)
                    .hasSize(UUID_SIZE);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenExistingOtherPartiesWithAppointeeAndRep_thenNewOtherPartyAssignedNewId() {
        sscsCaseData.setOtherParties(Arrays.asList(
            buildOtherParty(ID_2),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4),
            buildOtherPartyWithAppointeeAndRep(null, null, null)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOtherParties())
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_1);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
                assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
            })
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_2);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            })
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId())
                    .isNotEqualTo(ID_1)
                    .isNotEqualTo(ID_2)
                    .hasSize(UUID_SIZE);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenExistingOtherParties_thenNewOtherPartyAppointeeAndRepAssignedNewId() {
        sscsCaseData.setOtherParties(Arrays.asList(
            buildOtherPartyWithAppointeeAndRep(ID_2, null, null),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4),
            buildOtherParty(null)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOtherParties())
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_1);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
                assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
            })
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(ID_2);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            })
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId())
                    .isNotEqualTo(ID_1)
                    .isNotEqualTo(ID_2)
                    .hasSize(UUID_SIZE);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            });

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenEmptyOtherParties_thenSetToNullRatherThanEmpty() {
        sscsCaseData.setOtherParties(emptyList());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getOtherParties()).isNull();
        assertThat(response.getErrors()).isEmpty();
    }

    private CcdValue<OtherParty> buildConfidentialOtherParty(final String id, final YesNo confidentialityRequired) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .confidentialityRequired(confidentialityRequired)
                .role(Role.builder().name("PayingParent").build())
                .build())
            .build();
    }

    @Test
    void givenOtherPartyWantsConfidentiality_thenCaseIsConfidential() {
        final SscsCaseData caseData = SscsCaseData.builder()
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build())
                .otherParties(Arrays.asList(buildConfidentialOtherParty(ID_2, YES), buildConfidentialOtherParty(ID_1, NO))).build();
        when(caseDetails.getCaseData()).thenReturn(caseData);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getIsConfidentialCase()).isEqualTo(YES);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenNoOtherPartyWantsConfidentiality_thenCaseIsNotConfidential() {
        final SscsCaseData caseData = SscsCaseData.builder()
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build())
                .otherParties(Arrays.asList(buildConfidentialOtherParty(ID_2, NO), buildConfidentialOtherParty(ID_1, NO))).build();
        when(caseDetails.getCaseData()).thenReturn(caseData);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getIsConfidentialCase()).isNull();
        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit", "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    void givenSscs5CaseOtherPartyWithRole_thenWarningIsReturned(final String benefit) {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, null)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);

        final PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getWarnings()).containsExactly(SSCS5_ROLE_WARNING);
        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit", "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    void givenSscs5CaseOtherPartyWithNoRole_thenNoErrorsOrWarningIsReturned(final String benefit) {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, null), buildSscs5OtherParty(ID_1, null))).build();
        when(caseDetails.getCaseData()).thenReturn(caseData);

        final PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getOtherParties()).hasSize(2);
        assertThat(isSscs5CaseValidated(response.getData().getOtherParties())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit", "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    void givenSscs5CaseOtherPartyWithRoleWarningIgnored_thenCaseIsUpdated(final String benefit) {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, null)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.isIgnoreWarnings()).thenReturn(true);

        final PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getOtherParties()).hasSize(2);
        assertThat(isSscs5CaseValidated(response.getData().getOtherParties())).isTrue();
    }

    @Test
    void givenSscs2CaseOtherPartyWithRoleMissing_thenErrorReturned() {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(
                Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                    .build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, null)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);

        final PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrors()).containsExactly(SSCS2_ROLE_ERROR);
    }

    @Test
    void givenSscs2CaseOtherPartyWithRoleEntered_thenNoError() {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(
                Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                    .build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, "")))
            .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);

        final PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenSscsIbaCaseOtherPartyWithRoleEntered_thenNoRoleCheck() {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(
                Appeal.builder().benefitType(BenefitType.builder().code(Benefit.INFECTED_BLOOD_COMPENSATION.getShortName()).build())
                    .build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, null), buildSscs5OtherParty(ID_1, null)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);

        final PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare",
        "homeResponsibilitiesProtection", "childBenefit", "thirtyHoursFreeChildcare",
        "guaranteedMinimumPension", "nationalInsuranceCredits"})
    void givenNonSscs1PaperCaseOtherPartyWantsToAttendYes_thenCaseIsOralAndWarningShown(final String shortName) {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(shortName).build())
                .hearingType(HearingType.PAPER.getValue())
                .build())
            .otherParties(Arrays.asList(buildOtherParty("No", null), buildOtherParty("Yes", NO)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).hasSize(1);
        assertThat(response.getWarnings()).anyMatch(m -> m.contains(
            "The hearing type will be changed from Paper to Oral as at least one of the"
                + " parties to the case would like to attend the hearing"));
        assertThat(response.getData().getAppeal().getHearingType()).isEqualTo(HearingType.ORAL.getValue());
    }

    @Test
    void givenNonSscs1PaperCaseOtherPartyWantsToAttendYesCaseLoader_thenCaseIsOralAndNoWarningShown() {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("taxCredit").build())
                .hearingType(HearingType.PAPER.getValue())
                .build())
            .otherParties(Arrays.asList(buildOtherParty("No", null), buildOtherParty("Yes", NO)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(idamService.getUserDetails(any())).thenReturn(UserDetails.builder().roles(List.of("caseworker-sscs-systemupdate")).build());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getAppeal().getHearingType()).isEqualTo(HearingType.ORAL.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare",
        "homeResponsibilitiesProtection", "childBenefit", "thirtyHoursFreeChildcare",
        "guaranteedMinimumPension", "nationalInsuranceCredits"})
    void givenNonSscs1PaperCaseOtherPartyWantsToAttendNo_thenCaseIsNotChangedAndNoWarningShown(final String shortName) {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(shortName).build())
                .hearingType(HearingType.PAPER.getValue())
                .build())
            .otherParties(Arrays.asList(buildOtherParty("No", null), buildOtherParty("No", NO)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getAppeal().getHearingType()).isEqualTo(HearingType.PAPER.getValue());
    }

    @ParameterizedTest
    @CsvSource({"paper,No,Yes", "oral,No,No", "online,Yes,Yes"})
    void givenSscs1CaseOtherPartyWantsToAttendYes_thenHearingTypeNotChangedAndNoWarningShown(
        final String hearingType, final String wantsToAttend1, final String wantsToAttend2) {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .hearingType(hearingType)
                .build())
            .otherParties(Arrays.asList(buildOtherParty(wantsToAttend1, null), buildOtherParty(wantsToAttend2, NO)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getAppeal().getHearingType()).isEqualTo(hearingType);
    }

    @Test
    void givenAnyCaseUnavailableDatesSelectedButEmptyExcludedDates_thenThrowError() {
        final CcdValue<OtherParty> otherParty1 = buildOtherParty("Yes", NO);
        final CcdValue<OtherParty> otherParty2 = buildOtherParty("Yes", NO);
        final CcdValue<OtherParty> otherParty3 = buildOtherParty("Yes", NO);

        otherParty1.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty2.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty3.getValue().getHearingOptions().setScheduleHearing("Yes");

        otherParty1.getValue().setRole(Role.builder().name("ReceivingParent").build());
        otherParty2.getValue().setRole(Role.builder().name("PayingParent").build());
        otherParty3.getValue().setRole(Role.builder().name("Other").build());

        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(Arrays.asList(otherParty1, otherParty2, otherParty3)).build();

        when(caseDetails.getCaseData()).thenReturn(caseData);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrors()).containsExactly(
            "Add a start date for unavailable dates",
            "Add an end date for unavailable dates"
        );
    }

    @Test
    void givenAnyCaseUnavailableDatesSelectedButNoDatesProvided_thenThrowError() {
        final CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        final ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("").end("").build()).build();
        final ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start(null).end(null).build()).build();
        final ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("2023-01-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));

        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(caseData);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrors()).containsExactly(
            "Add a start date for unavailable dates",
            "Add an end date for unavailable dates"
        );
    }

    @Test
    void givenAnyCaseUnavailableDatesSelectedButNoStartDatesProvided_thenThrowError() {
        final CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        final ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("").end("2023-01-01").build()).build();
        final ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start(null).end("2023-02-01").build()).build();
        final ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));

        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(caseData);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrors()).containsExactly("Add a start date for unavailable dates");
    }

    @Test
    void givenAnyCaseUnavailableDatesSelectedButNoEndDatesProvided_thenThrowError() {
        final CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        final ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("").build()).build();
        final ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start("2023-02-01").end(null).build()).build();
        final ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));

        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(caseData);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrors()).containsExactly("Add an end date for unavailable dates");
    }

    @Test
    void givenAnyCaseUnavailableDatesSelectedButStartDateIsAfterEndDate_thenThrowError() {
        final CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        final ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("2023-01-01").build()).build();
        final ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-02").end("2023-01-01").build()).build();
        final ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));

        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(caseData);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrors()).containsExactly("Unavailability start date must be before end date");
    }


    @Test
    void givenCmConfidentialityEnabledAndChildSupportBenefit_thenDirectionDueDateAndInterlocSet() {
        final UpdateOtherPartyAboutToSubmitHandler handlerWithFlag = new UpdateOtherPartyAboutToSubmitHandler(idamService, true);
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build())
            .otherParties(singletonList(buildSscs5OtherParty(ID_1, "PayingParent")))
            .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);

        final PreSubmitCallbackResponse<SscsCaseData> response =
            handlerWithFlag.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDirectionDueDate())
            .isEqualTo(now().plusDays(getHearingResponseExpectedByDays()).toString());
        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.HEF_ISSUED);
    }

    @ParameterizedTest
    @CsvSource({
        "false,CHILD_SUPPORT",
        "true,INFECTED_BLOOD_COMPENSATION"
    })
    void givenNonCmConfidentialityOrCmBenefit_thenDirectionDueDateAndInterlocNotSet(
        boolean cmConfidentialityEnabled, Benefit benefit) {
        final UpdateOtherPartyAboutToSubmitHandler handlerWithFlag =
            new UpdateOtherPartyAboutToSubmitHandler(idamService, cmConfidentialityEnabled);
        final SscsCaseData caseData = SscsCaseData.builder()
                                                  .appeal(Appeal.builder().benefitType(
                                                      BenefitType
                                                          .builder()
                                                          .code(benefit.getShortName())
                                                          .build()).build())
                                                  .otherParties(singletonList(buildSscs5OtherParty(ID_1, "PayingParent")))
                                                  .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);

        final PreSubmitCallbackResponse<SscsCaseData> response =
            handlerWithFlag.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if (benefit == Benefit.CHILD_SUPPORT) {
            assertThat(response.getData().getDirectionDueDate()).isEqualTo(now().plusDays(14).toString());
        } else {
            assertThat(response.getData().getDirectionDueDate()).isNull();
        }
        assertThat(response.getData().getInterlocReviewState()).isNull();
    }

    private boolean isSscs5CaseValidated(final List<CcdValue<OtherParty>> otherParties) {
        return emptyIfNull(otherParties).stream()
            .filter(otherPartyCcdValue -> otherPartyCcdValue.getValue() != null)
            .map(CcdValue::getValue)
            .allMatch(otherParty -> otherParty.getShowRole().equals(NO) && otherParty.getRole() == null);
    }

    private CcdValue<OtherParty> buildOtherParty(final String wantsToAttend, final YesNo confidentiality) {
        return CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .confidentialityRequired(confidentiality != null ? confidentiality : NO)
            .hearingOptions(HearingOptions.builder().wantsToAttend(wantsToAttend).build())
            .build()).build();
    }

    private CcdValue<OtherParty> buildOtherParty(final String id) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .unacceptableCustomerBehaviour(YesNo.YES)
                .role(Role.builder().name("PayingParent").build())
                .build())
            .build();
    }

    private CcdValue<OtherParty> buildSscs5OtherParty(final String id, final String role) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .unacceptableCustomerBehaviour(YesNo.NO)
                .confidentialityRequired(NO)
                .role(Role.builder().name(role).build())
                .build())
            .build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(final String id, final String appointeeId, final String repId) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .isAppointee(YES.getValue())
                .appointee(Appointee.builder().id(appointeeId).build())
                .rep(Representative.builder().id(repId).hasRepresentative(YES.getValue()).build())
                .role(Role.builder().name("ReceivingParent").build())
                .build())
            .build();
    }
}
