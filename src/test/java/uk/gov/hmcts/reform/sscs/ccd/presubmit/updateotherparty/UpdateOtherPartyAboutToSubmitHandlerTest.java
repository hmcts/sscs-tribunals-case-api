package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static java.util.Collections.*;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_1;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_2;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_3;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_4;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;


@RunWith(JUnitParamsRunner.class)
public class UpdateOtherPartyAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String SSCS5_ROLE_WARNING = "You have entered a role for the Other Party which is not valid "
        + "for an SSCS5 case. This role will be ignored when the event completes.";
    private static final String SSCS2_ROLE_ERROR = "Role is required for the selected case";
    public static final int UUID_SIZE = 36;
    private UpdateOtherPartyAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    private SscsCaseData sscsCaseData;

    private Appeal appeal = Appeal.builder()
            .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
            .build();

    @Before
    public void setUp() {
        openMocks(this);
        handler = new UpdateOtherPartyAboutToSubmitHandler(idamService);
        when(idamService.getUserDetails(any())).thenReturn(UserDetails.builder().roles(Arrays.asList(UserRole.SUPER_USER.getValue())).build());

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        Appeal appeal = Appeal.builder()
                .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build();
        sscsCaseData.setAppeal(appeal);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenANonUpdateOtherPartyEvent_thenReturnFalse() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty(ID_2), buildOtherParty(ID_1)));
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnUpdateOtherPartyEvent_thenReturnTrue() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty(ID_2), buildOtherParty(ID_1)));
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAUpdateOtherPartyEventWithNullOtherPartiesList_thenReturnFalse() {
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonAboutToSubmitEvent_thenReturnFalse(CallbackType callbackType) {
        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenOtherPartiesUcbIsYes_thenUpdateCaseDataOtherPartyUcb() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty(ID_2), buildOtherParty(ID_1)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getOtherPartyUcb(), is(YesNo.YES.getValue()));
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenNewOtherPartyAdded_thenAssignAnId() {
        sscsCaseData.setOtherParties(Arrays.asList(
            buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertThat(response.getData().getOtherParties())
            .hasSize(1)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAssignedNewId() {
        sscsCaseData.setOtherParties(Arrays.asList(
            buildOtherParty(ID_2),
            buildOtherParty(ID_1),
            buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertThat(response.getData().getOtherParties())
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_1);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);;
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_2);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId())
                    .isNotEqualTo(ID_1)
                    .isNotEqualTo(ID_2)
                    .hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenExistingOtherPartiesWithAppointeeAndRep_thenNewOtherPartyAssignedNewId() {
        sscsCaseData.setOtherParties(Arrays.asList(
            buildOtherParty(ID_2),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4),
            buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<CcdValue<OtherParty>> otherParties = response.getData().getOtherParties();

        Assertions.assertThat(response.getData().getOtherParties())
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_1);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);;
                Assertions.assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
                Assertions.assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_2);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId())
                    .isNotEqualTo(ID_1)
                    .isNotEqualTo(ID_2)
                    .hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAppointeeAndRepAssignedNewId() {
        sscsCaseData.setOtherParties(Arrays.asList(
            buildOtherPartyWithAppointeeAndRep(ID_2, null, null),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4),
            buildOtherParty(null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertThat(response.getData().getOtherParties())
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_1);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);;
                Assertions.assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
                Assertions.assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_2);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId())
                    .isNotEqualTo(ID_1)
                    .isNotEqualTo(ID_2)
                    .hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            });

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenEmptyOtherParties_thenSetToNullRatherThanEmpty() {
        sscsCaseData.setOtherParties(emptyList());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getOtherParties(), is(nullValue()));
        assertEquals(0, response.getErrors().size());
    }

    private CcdValue<OtherParty> buildConfidentialOtherParty(String id, YesNo confidentialityRequired) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .confidentialityRequired(confidentialityRequired)
                .role(Role.builder().name("PayingParent").build())
                .build())
            .build();
    }

    @Test
    public void givenOtherPartyWantsConfidentiality_thenCaseIsConfidential() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build())
                .otherParties(Arrays.asList(buildConfidentialOtherParty(ID_2, YES), buildConfidentialOtherParty(ID_1, NO))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(YES, response.getData().getIsConfidentialCase());
        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenNoOtherPartyWantsConfidentiality_thenCaseIsNotConfidential() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build())
                .otherParties(Arrays.asList(buildConfidentialOtherParty(ID_2, NO), buildConfidentialOtherParty(ID_1, NO))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(null, response.getData().getIsConfidentialCase());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit", "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    public void givenSscs5CaseOtherPartyWithRole_thenWarningIsReturned(String benefit) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, null)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getWarnings().size());
        assertEquals(SSCS5_ROLE_WARNING, response.getWarnings().stream().findFirst().get());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit", "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    public void givenSscs5CaseOtherPartyWithNoRole_thenNoErrorsOrWarningIsReturned(String benefit) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, null), buildSscs5OtherParty(ID_1, null))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
        assertEquals(2, response.getData().getOtherParties().size());
        assertTrue(isSscs5CaseValidated(response.getData().getOtherParties()));
    }

    @Test
    @Parameters({"taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit", "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    public void givenSscs5CaseOtherPartyWithRoleWarningIgnored_thenCaseIsUpdated(String benefit) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, null)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.isIgnoreWarnings()).thenReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
        assertEquals(2, response.getData().getOtherParties().size());
        assertTrue(isSscs5CaseValidated(response.getData().getOtherParties()));
    }

    @Test
    public void givenSscs2CaseOtherPartyWithRoleMissing_thenErrorReturned() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(
                Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                    .build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, null)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getWarnings().size());
        assertEquals(1, response.getErrors().size());
        assertEquals(SSCS2_ROLE_ERROR, response.getErrors().stream().findFirst().get());
    }

    @Test
    public void givenSscs2CaseOtherPartyWithRoleEntered_thenNoError() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(
                Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                    .build())
            .otherParties(Arrays.asList(buildSscs5OtherParty(ID_2, "PayingParent"), buildSscs5OtherParty(ID_1, "")))
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenNonSscs1PaperCaseOtherPartyWantsToAttendYes_thenCaseIsOralAndWarningShown(String shortName) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(shortName).build())
                .hearingType(HearingType.PAPER.getValue())
                .build())
            .otherParties(Arrays.asList(buildOtherParty("No",null), buildOtherParty("Yes", NO)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getWarnings().size());
        assertTrue(response.getWarnings().stream().anyMatch(m -> m.contains(
            "The hearing type will be changed from Paper to Oral as at least one of the"
                + " parties to the case would like to attend the hearing")));
        assertEquals(HearingType.ORAL.getValue(), response.getData().getAppeal().getHearingType());
    }

    @Test
    public void givenNonSscs1PaperCaseOtherPartyWantsToAttendYesCaseLoader_thenCaseIsOralAndNoWarningShown() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("taxCredit").build())
                .hearingType(HearingType.PAPER.getValue())
                .build())
            .otherParties(Arrays.asList(buildOtherParty("No",null), buildOtherParty("Yes", NO)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getUserDetails(any())).thenReturn(UserDetails.builder().roles(List.of("caseworker-sscs-systemupdate")).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(HearingType.ORAL.getValue(), response.getData().getAppeal().getHearingType());
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenNonSscs1PaperCaseOtherPartyWantsToAttendNo_thenCaseIsNotChangedAndNoWarningShown(String shortName) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(shortName).build())
                .hearingType(HearingType.PAPER.getValue())
                .build())
            .otherParties(Arrays.asList(buildOtherParty("No",null), buildOtherParty("No", NO)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(HearingType.PAPER.getValue(), response.getData().getAppeal().getHearingType());
    }

    @Test
    @Parameters({"paper,No,Yes", "oral,No,No", "online,Yes,Yes"})
    public void givenSscs1CaseOtherPartyWantsToAttendYes_thenHearingTypeNotChangedAndNoWarningShown(
        String hearingType, String wantsToAttend1, String wantsToAttend2) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .hearingType(hearingType)
                .build())
            .otherParties(Arrays.asList(buildOtherParty(wantsToAttend1,null), buildOtherParty(wantsToAttend2, NO)))
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(hearingType, response.getData().getAppeal().getHearingType());
    }

    @Test
    public void givenAnyCaseUnavailableDatesSelectedButEmptyExcludedDates_thenThrowError() {
        CcdValue<OtherParty> otherParty1 = buildOtherParty("Yes", NO);
        CcdValue<OtherParty> otherParty2 = buildOtherParty("Yes", NO);
        CcdValue<OtherParty> otherParty3 = buildOtherParty("Yes", NO);

        otherParty1.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty2.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty3.getValue().getHearingOptions().setScheduleHearing("Yes");

        otherParty1.getValue().setRole(Role.builder().name("ReceivingParent").build());
        otherParty2.getValue().setRole(Role.builder().name("PayingParent").build());
        otherParty3.getValue().setRole(Role.builder().name("Other").build());

        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(Arrays.asList(otherParty1, otherParty2, otherParty3)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(2, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        String error2 = iterator.next();
        assertEquals("Add a start date for unavailable dates", error1);
        assertEquals("Add an end date for unavailable dates", error2);
    }

    @Test
    public void givenAnyCaseUnavailableDatesSelectedButNoDatesProvided_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("").end("").build()).build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start(null).end(null).build()).build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("2023-01-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));

        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(2, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        String error2 = iterator.next();
        assertEquals("Add a start date for unavailable dates", error1);
        assertEquals("Add an end date for unavailable dates", error2);
    }

    @Test
    public void givenAnyCaseUnavailableDatesSelectedButNoStartDatesProvided_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("").end("2023-01-01").build()).build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start(null).end("2023-02-01").build()).build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));

        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(1, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        assertEquals("Add a start date for unavailable dates", error1);
    }

    @Test
    public void givenAnyCaseUnavailableDatesSelectedButNoEndDatesProvided_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("").build()).build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start("2023-02-01").end(null).build()).build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));

        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(1, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        assertEquals("Add an end date for unavailable dates", error1);
    }

    @Test
    public void givenAnyCaseUnavailableDatesSelectedButStartDateIsAfterEndDate_thenThrowError() {
        CcdValue<OtherParty> otherParty = buildOtherParty("Yes", NO);
        otherParty.getValue().getHearingOptions().setScheduleHearing("Yes");
        otherParty.getValue().setRole(Role.builder().name("ReceivingParent").build());

        ExcludeDate excludeDate1 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-01").end("2023-01-01").build()).build();
        ExcludeDate excludeDate2 = ExcludeDate.builder().value(DateRange.builder().start("2023-01-02").end("2023-01-01").build()).build();
        ExcludeDate excludeDate3 = ExcludeDate.builder().value(DateRange.builder().start("2023-03-01").end("2023-04-02").build()).build();

        otherParty.getValue().getHearingOptions().setExcludeDates(List.of(excludeDate1, excludeDate2, excludeDate3));


        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(List.of(otherParty)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(1, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        assertEquals("Unavailability start date must be before end date", error1);
    }


    private boolean isSscs5CaseValidated(List<CcdValue<OtherParty>> otherParties) {
        return emptyIfNull(otherParties).stream()
            .filter(otherPartyCcdValue -> otherPartyCcdValue.getValue() != null)
            .map(otherPartyCcdValue -> otherPartyCcdValue.getValue())
            .allMatch(otherParty -> otherParty.getShowRole().equals(NO) && otherParty.getRole() == null);
    }

    private CcdValue<OtherParty> buildOtherParty(String wantsToAttend, YesNo confidentiality) {
        return CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .confidentialityRequired(confidentiality != null ? confidentiality : NO)
            .hearingOptions(HearingOptions.builder().wantsToAttend(wantsToAttend).build())
            .build()).build();
    }

    private CcdValue<OtherParty> buildOtherParty(String id) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .unacceptableCustomerBehaviour(YesNo.YES)
                .role(Role.builder().name("PayingParent").build())
                .build())
            .build();
    }

    private CcdValue<OtherParty> buildSscs5OtherParty(String id, String role) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .unacceptableCustomerBehaviour(YesNo.NO)
                .confidentialityRequired(NO)
                .role(Role.builder().name(role).build())
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
                .role(Role.builder().name("ReceivingParent").build())
                .build())
            .build();
    }
}
