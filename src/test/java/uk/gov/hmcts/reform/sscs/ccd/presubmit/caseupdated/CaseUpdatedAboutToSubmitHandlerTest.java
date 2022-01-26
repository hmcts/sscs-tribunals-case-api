package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SUPER_USER;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.AssociatedCaseLinkHelper;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@RunWith(JUnitParamsRunner.class)
public class CaseUpdatedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private CaseUpdatedAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;

    private SscsCaseData sscsCaseData;

    private SscsCaseData sscsCaseDataBefore;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private AirLookupService airLookupService;

    private AssociatedCaseLinkHelper associatedCaseLinkHelper;

    @Before
    public void setUp() {
        openMocks(this);
        associatedCaseLinkHelper = new AssociatedCaseLinkHelper(ccdService, idamService);
        handler = new CaseUpdatedAboutToSubmitHandler(regionalProcessingCenterService, associatedCaseLinkHelper, airLookupService, new DwpAddressLookupService(), idamService, true);

        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().address(Address.builder().postcode("CM120NS").build()).build()).build())
                .benefitCode("002")
                .issueCode("DD")
                .build();

        sscsCaseDataBefore = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .appellant(Appellant.builder().address(Address.builder().postcode("CM120NS").build()).build()).build())
                .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);
        when(idamService.getUserDetails(anyString())).thenReturn(UserDetails.builder().roles(List.of(SUPER_USER.getValue())).build());
    }

    @Test
    public void givenANonCaseUpdatedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenACaseUpdatedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCaseUpdatedCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenACaseUpdatedCallbackType_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenACaseUpdatedEvent_thenSetCaseCode() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002DD", response.getData().getCaseCode());
    }

    @Test
    public void givenACaseUpdatedEventWithUcCase_thenSetCaseCode() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        sscsCaseData.setElementsDisputedList(elementList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("uc").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002DD", response.getData().getCaseCode());
    }

    @Test
    public void givenACaseUpdatedEventWithEmptyBenefitCodeAndCaseCode_thenDoNotOverrideCaseCode() {
        callback.getCaseDetails().getCaseData().setBenefitCode(null);
        callback.getCaseDetails().getCaseData().setIssueCode(null);
        callback.getCaseDetails().getCaseData().setCaseCode("002DD");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002DD", response.getData().getCaseCode());
    }

    @Test
    public void givenAnAppealWithPostcode_updateRpc() {
        when(regionalProcessingCenterService.getByPostcode("CM120NS")).thenReturn(RegionalProcessingCenter.builder().name("Region1").address1("Line1").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Line1", response.getData().getRegionalProcessingCenter().getAddress1());
        assertEquals("Region1", response.getData().getRegion());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenMultipleAssociatedCases_thenAddAllAssociatedCaseLinksToCase() {
        Appellant appellant = Appellant.builder().identity(Identity.builder().nino("AB223344B").build()).build();
        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().ccdCaseId("12345678").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().ccdCaseId("56765676").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);

        when(ccdService.findCaseBy(anyString(), anyString(), any())).thenReturn(matchedByNinoCases);
        callback.getCaseDetails().getCaseData().setBenefitCode(null);
        callback.getCaseDetails().getCaseData().setIssueCode(null);
        callback.getCaseDetails().getCaseData().setCaseCode("002DD");
        callback.getCaseDetails().getCaseData().getAppeal().setAppellant(appellant);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(2, response.getData().getAssociatedCase().size());
        assertEquals("Yes", response.getData().getLinkedCasesBoolean());
        assertEquals("56765676", response.getData().getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("12345678", response.getData().getAssociatedCase().get(1).getValue().getCaseReference());

        assertEquals("Yes", matchingCase1.getData().getLinkedCasesBoolean());
        assertEquals("ccdId", matchingCase1.getData().getAssociatedCase().get(0).getValue().getCaseReference());

        assertEquals("Yes", matchingCase2.getData().getLinkedCasesBoolean());
        assertEquals("ccdId", matchingCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    @Parameters({"Birmingham,Glasgow,Yes", "Glasgow,Birmingham,No"})
    public void givenChangeInRpcChangeIsScottish(String oldRpcName, String newRpcName, String expected) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setIsScottishCase("No");
        RegionalProcessingCenter oldRpc = RegionalProcessingCenter.builder().name(oldRpcName).build();
        RegionalProcessingCenter newRpc = RegionalProcessingCenter.builder().name(newRpcName).build();

        handler.maybeChangeIsScottish(oldRpc, newRpc, caseData);

        assertEquals(expected, caseData.getIsScottishCase());
    }

    @Test
    @Parameters({"Birmingham,No", "Glasgow,Yes"})
    public void givenChangeInNullRpcChangeIsScottish(String newRpcName, String expected) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setIsScottishCase("No");
        RegionalProcessingCenter oldRpc = null;
        RegionalProcessingCenter newRpc = RegionalProcessingCenter.builder().name(newRpcName).build();

        handler.maybeChangeIsScottish(oldRpc, newRpc, caseData);

        assertEquals(expected, caseData.getIsScottishCase());
    }

    @Test
    @Parameters({"Birmingham,No", "Glasgow,Yes"})
    public void givenAnAppealWithPostcode_updateRpcToScottish(String newRpcName, String expectedIsScottish) {
        when(regionalProcessingCenterService.getByPostcode("CM120NS")).thenReturn(RegionalProcessingCenter.builder().name(newRpcName).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(newRpcName, response.getData().getRegionalProcessingCenter().getName());
        assertEquals(expectedIsScottish, response.getData().getIsScottishCase());
    }

    @Test
    public void givenAnAppealWithNewAppellantPostcodeAndNoAppointee_thenUpdateProcessingVenue() {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setPostcode("AB1200B");

        when(airLookupService.lookupAirVenueNameByPostCode("AB1200B", sscsCaseData.getAppeal().getBenefitType())).thenReturn("VenueB");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("VenueB", response.getData().getProcessingVenue());
    }

    @Test
    public void givenAnAppealWithNewAppointeePostcode_thenUpdateProcessingVenueWithAppointeeVenue() {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().address(Address.builder().postcode("AB1200B").build()).build());

        when(airLookupService.lookupAirVenueNameByPostCode("AB1200B", sscsCaseData.getAppeal().getBenefitType())).thenReturn("VenueB");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("VenueB", response.getData().getProcessingVenue());
    }

    @Test
    @Parameters({"null", " ", ""})
    public void givenAnAppealWithNullOrEmptyPostcode_thenDoNotUpdateProcessingVenue(@Nullable String postcode) {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setPostcode("AB1200B");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setPostcode(postcode);
        callback.getCaseDetails().getCaseData().setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("rpc1").build());
        callback.getCaseDetails().getCaseData().setProcessingVenue("VenueA");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(airLookupService);
        assertEquals("VenueA", response.getData().getProcessingVenue());
        assertEquals("rpc1", response.getData().getRegionalProcessingCenter().getName());
    }

    @Test
    public void givenAnAppealWithNewAppointeeButEmptyPostcode_thenUpdateProcessingVenueWithAppellantVenue() {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setPostcode("AB1200B");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().address(Address.builder().postcode(null).build()).build());

        when(airLookupService.lookupAirVenueNameByPostCode("AB1200B", sscsCaseData.getAppeal().getBenefitType())).thenReturn("VenueB");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("VenueB", response.getData().getProcessingVenue());
    }

    @Ignore("commented out as case loader is failing on this validation checks, we need to do another data exercise to clean the data")
    @Test
    @Parameters({"!. House, House, House, House",
            "~., 101 House, House, House",
            " Ho.use, ., \"101 House, House",
            " ., ãHouse, âHouse, &101 House"})
    public void givenACaseUpdateEventWithInvalidAppellantAddressDetails_thenReturnError(String line1, String line2, String town, String county) {
        Address appellantAddress = callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress();
        appellantAddress.setLine1(line1);
        appellantAddress.setLine2(line2);
        appellantAddress.setCounty(county);
        appellantAddress.setTown(town);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        long numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(1, numberOfExpectedError);
    }

    @Ignore("commented out as case loader is failing on this validation checks, we need to do another data exercise to clean the data")
    @Test
    @Parameters({"!. House, House, House, House",
            "~., 101 House, House, House",
            " Ho.use, ., \"101 House, House",
            " ., ãHouse, âHouse, &101 House"})
    public void givenACaseUpdateEventWithInvalidRepresentativeAddressDetails_thenReturnError(String line1, String line2, String town, String county) {
        Representative representative = Representative.builder().address(buildAddress(line1, line2, county, town)).build();
        callback.getCaseDetails().getCaseData().getAppeal().setRep(representative);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        long numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(1, numberOfExpectedError);
    }

    @Ignore("commented out as case loader is failing on this validation checks, we need to do another data exercise to clean the data")
    @Test
    @Parameters({"!. House, House, House, House",
            "~., 101 House, House, House",
            " Ho.use, ., \"101 House, House",
            " ., ãHouse, âHouse, &101 House"})
    public void givenACaseUpdateEventWithInvalidAppointeeAddressDetails_thenReturnError(String line1, String line2, String town, String county) {
        Appointee appointee = Appointee.builder().address(buildAddress(line1, line2, county, town)).build();
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setAppointee(appointee);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        long numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(1, numberOfExpectedError);
    }

    @Ignore("commented out as case loader is failing on this validation checks, we need to do another data exercise to clean the data")
    @Test
    @Parameters({"!. House, House, House, House",
            "~., 101 House, House, House",
            " Ho.use, ., \"101 House, House",
            " ., ãHouse, âHouse, &101 House"})
    public void givenACaseUpdateEventWithInvalidJointPartyAddressDetails_thenReturnError(String line1, String line2, String town, String county) {
        callback.getCaseDetails().getCaseData().setJointPartyAddress(buildAddress(line1, line2, county, town));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        long numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(1, numberOfExpectedError);
    }

    @Test
    @Parameters({"  ,   ,   ,   ",
             "Ts. Test's Ltd, Ts. Test's Ltd, Ts. Test's Ltd, Ts. Test's Ltd",
            "A“”\"’'?![]()/£:_+-%&, A“”\"’'?![]()/£:_+-%&, A“”\"’'?![]()/£:_+-%&, A“”\"’'?![]()/£:_+-%&",
            "\\,Test Street,\\,Test Street,\\,Test Street,\\,Test Street",
            ".dot Street,.dot Street,.dot Street,.dot Street"})
    public void givenACaseUpdateEventWithAddressDetails_thenShouldNotReturnError(String line1, String line2, String town, String county) {
        Address address = buildAddress(line1, line2, county, town);
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setAddress(address);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        long numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(0, numberOfExpectedError);

        Representative representative = Representative.builder().address(address).build();
        callback.getCaseDetails().getCaseData().getAppeal().toBuilder().rep(representative).build();
        response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(0, numberOfExpectedError);

        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().toBuilder().appointee(Appointee.builder().address(address).build()).build();
        response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(0, numberOfExpectedError);

        callback.getCaseDetails().getCaseData().setJointPartyAddress(address);
        response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(0, numberOfExpectedError);
    }

    @Test
    public void givenChildSupportCaseAndCaseCodeIsChangedToNonChildSupportCodeAndCaseHasOtherParty_thenShowError() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("022");

        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().build()).build();
        otherPartyList.add(ccdValue);
        sscsCaseData.setOtherParties(otherPartyList);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getErrors().iterator().next(), is("Benefit code cannot be changed on cases with registered 'Other Party'"));
    }

    @Test
    public void givenChildSupportCaseAndCaseCodeIsChangedToNonChildSupportCodeAndCaseHasNoOtherParty_thenShowWarning() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(), is("The benefit code will be changed to a non-child support benefit code"));
    }

    @Test
    @Parameters({"022", "023", "024", "025", "026", "028"})
    public void givenChildSupportCaseAndCaseCodeIsSetToChildSupportCode_thenNoWarningOrErrorIsShown(String childSupportBenefitCode) {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode(childSupportBenefitCode);
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenChildSupportCaseAndCaseCodeIsAlreadyANonChildSupportCase_thenShowErrorOrWarning() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("001");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenBenefitTypeAndDwpIssuingOfficeEmpty_thenAddWarningMessages() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(null);
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(2, response.getWarnings().size());
        assertThat(response.getWarnings(), hasItems("Benefit type code is empty", "FTA issuing office is empty"));
    }

    @Test
    public void givenInvalidBenefitTypeAndDwpIssuingOfficeEmpty_thenAddWarningMessages() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("INVALID").build());
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getWarnings().size());
        assertEquals(1, response.getErrors().size());

        assertThat(response.getWarnings(), hasItems("DWP issuing office is empty"));
        assertThat(response.getErrors(), hasItems("Benefit type code is invalid, should be one of: ESA, JSA, PIP, DLA, UC, carersAllowance, attendanceAllowance, "
                        + "bereavementBenefit, industrialInjuriesDisablement, maternityAllowance, socialFund, incomeSupport, bereavementSupportPaymentScheme, "
                        + "industrialDeathBenefit, pensionCredit, retirementPension, childSupport, taxCredit, guardiansAllowance, taxFreeChildcare, "
                        + "homeResponsibilitiesProtection, childBenefit, thirtyHoursFreeChildcare, guaranteedMinimumPension, nationalInsuranceCredits"));
    }

    @Test
    public void givenInvalidBenefitTypeAndInvalidDwpIssuingOffice_thenAddWarningMessages() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("INVALID").build());
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice("Test").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertThat(response.getErrors(), hasItems("Benefit type code is invalid, should be one of: ESA, JSA, PIP, DLA, UC, carersAllowance, attendanceAllowance, "
                + "bereavementBenefit, industrialInjuriesDisablement, maternityAllowance, socialFund, incomeSupport, bereavementSupportPaymentScheme, "
                + "industrialDeathBenefit, pensionCredit, retirementPension, childSupport, taxCredit, guardiansAllowance, taxFreeChildcare, "
                + "homeResponsibilitiesProtection, childBenefit, thirtyHoursFreeChildcare, guaranteedMinimumPension, nationalInsuranceCredits"));
    }

    @Test
    @Parameters({
            "ESA,DWP issuing office is invalid\\, should one of: Balham DRT\\, Birkenhead LM DRT\\, Chesterfield DRT\\, Coatbridge Benefit Centre\\, Inverness DRT\\, Lowestoft DRT\\, Milton Keynes DRT\\, Norwich DRT\\, Sheffield DRT\\, Springburn DRT\\, Watford DRT\\, Wellingborough DRT\\, Worthing DRT\\, Recovery from Estates",
            "PIP,DWP issuing office is invalid\\, should one of: 1\\, 2\\, 3\\, 4\\, 5\\, 6\\, 7\\, 8\\, 9\\, AE\\, Recovery from Estates",
            "DLA,DWP issuing office is invalid\\, should one of: Disability Benefit Centre 4\\, The Pension Service 11\\, Recovery from Estates",
            "UC,DWP issuing office is invalid\\, should one of: Universal Credit\\, Recovery from Estates",
            "carersAllowance,DWP issuing office is invalid\\, should one of: Carer’s Allowance Dispute Resolution Team",
            "bereavementBenefit,DWP issuing office is invalid\\, should one of: Pensions Dispute Resolution Team",
            "attendanceAllowance,DWP issuing office is invalid\\, should one of: The Pension Service 11\\, Recovery from Estates",
            "industrialInjuriesDisablement,DWP issuing office is invalid\\, should one of: Barrow IIDB Centre\\, Barnsley Benefit Centre",
            "maternityAllowance,DWP issuing office is invalid\\, should one of: Walsall Benefit Centre",
            "JSA,DWP issuing office is invalid\\, should one of: Worthing DRT\\, Birkenhead DRT\\, Inverness DRT\\, Recovery from Estates",
            "socialFund,DWP issuing office is invalid\\, should one of: St Helens Sure Start Maternity Grant\\, Funeral Payment Dispute Resolution Team\\, Pensions Dispute Resolution Team",
            "incomeSupport,DWP issuing office is invalid\\, should one of: Worthing DRT\\, Birkenhead DRT\\, Inverness DRT\\, Recovery from Estates",
            "bereavementSupportPaymentScheme,DWP issuing office is invalid\\, should one of: Pensions Dispute Resolution Team",
            "industrialDeathBenefit,DWP issuing office is invalid\\, should one of: Barrow IIDB Centre\\, Barnsley Benefit Centre",
            "pensionCredit,DWP issuing office is invalid\\, should one of: Pensions Dispute Resolution Team\\, Recovery from Estates",
            "retirementPension,DWP issuing office is invalid\\, should one of: Pensions Dispute Resolution Team\\, Recovery from Estates",
            "childSupport,DWP issuing office is invalid\\, should one of: Child Maintenance Service Group",
    })
    public void givenValidBenefitTypeAndInvalidDwpIssuingOffice_thenAddWarningMessages(String benefitCode, String warning) {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitCode).build());
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice("Test").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getWarnings().size());
        assertThat(response.getWarnings(), hasItems(warning));
    }

    @Test
    @Parameters({"PIP,1,Newcastle", "ESA,Balham DRT,Balham DRT", "DLA,Disability Benefit Centre 4,DLA Child/Adult", "UC,Universal Credit,Universal Credit",
            "carersAllowance, Carer’s Allowance Dispute Resolution Team,Carers Allowance", "bereavementBenefit,Pensions Dispute Resolution Team,Bereavement Benefit",
            "attendanceAllowance,The Pension Service 11,Attendance Allowance", "industrialInjuriesDisablement,Barrow IIDB Centre,IIDB Barrow", "maternityAllowance,Walsall Benefit Centre,Maternity Allowance",
            "JSA,Worthing DRT,JSA Worthing", "socialFund,St Helens Sure Start Maternity Grant,SSMG","incomeSupport,Worthing DRT,IS Worthing",
            "bereavementSupportPaymentScheme,Pensions Dispute Resolution Team,Bereavement Support Payment", "industrialDeathBenefit,Barrow IIDB Centre,IDB Barrow",
            "pensionCredit,Pensions Dispute Resolution Team,Pension Credit", "retirementPension,Pensions Dispute Resolution Team,Retirement Pension",
            "childSupport,Child Maintenance Service Group,Child Support"
    })
    public void givenValidBenefitTypeAndValidDwpIssuingOffice_thenThereIsNoWarningMessagesAndSetRegionalCenter(String benefitCode, String dwpIssuingOffice, String regionalCenter) {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitCode).build());
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice(dwpIssuingOffice).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(regionalCenter, response.getData().getDwpRegionalCentre());
    }

    @Test
    @Parameters({
        "caseworker-sscs-superuser,1", "caseworker-sscs-systemupdate,0", "caseworker-sscs-clerk,1"
    })
    public void givenHearingTypeOralAndWantsToAttendHearingNo_thenAddWarningMessage(String idamUserRole, int warnings) {
        callback.getCaseDetails().getCaseData().getAppeal().setHearingType(HearingType.ORAL.getValue());
        callback.getCaseDetails().getCaseData().getAppeal().setHearingOptions(HearingOptions.builder().wantsToAttend("No").build());
        when(idamService.getUserDetails(anyString())).thenReturn(UserDetails.builder().roles(List.of(idamUserRole)).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(warnings, response.getWarnings().size());
        if (warnings > 0) {
            assertThat(response.getWarnings(), hasItems("There is a mismatch between the hearing type and the wants to attend field, "
                + "all hearing options will be cleared please check if this is correct"));
        }
    }

    private long getNumberOfExpectedError(PreSubmitCallbackResponse<SscsCaseData> response) {
        return response.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("Invalid characters are being used at the beginning of address fields, please correct"))
                .count();
    }

    private Address buildAddress(String line1, String line2, String county, String town) {
        return Address.builder().line1(line1).line2(line2).county(county).town(town).build();
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
            "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenACaseAppellantConfidentialityYes_thenCaseConfidentialYes(String shortName) {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setConfidentialityRequired(YES);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YES, response.getData().getIsConfidentialCase());
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
            "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenACaseAppellantConfidentialityNo_thenCaseConfidentialNull(String shortName) {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setConfidentialityRequired(NO);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(null, response.getData().getIsConfidentialCase());
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
            "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenACaseAppellantConfidentialityNoOtherPartyYes_thenCaseConfidentialYes(String shortName) {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setConfidentialityRequired(NO);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().confidentialityRequired(YES).build()).build();
        otherPartyList.add(ccdValue);
        callback.getCaseDetails().getCaseData().setOtherParties(otherPartyList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YES, response.getData().getIsConfidentialCase());
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
            "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenACaseOtherPartyConfidentialityYes_thenCaseConfidentialYes(String shortName) {
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().confidentialityRequired(YES).build()).build();
        otherPartyList.add(ccdValue);
        callback.getCaseDetails().getCaseData().setOtherParties(otherPartyList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YES, response.getData().getIsConfidentialCase());
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
            "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenACaseOtherPartyConfidentialityNo_thenCaseConfidentialNull(String shortName) {
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().confidentialityRequired(NO).build()).build();
        otherPartyList.add(ccdValue);
        callback.getCaseDetails().getCaseData().setOtherParties(otherPartyList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(null, response.getData().getIsConfidentialCase());
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
            "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenACaseOtherPartyConfidentialityNoAndYes_thenCaseConfidentialYes(String shortName) {
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().confidentialityRequired(NO).build()).build();
        otherPartyList.add(ccdValue);
        CcdValue<OtherParty> ccdValue1 = CcdValue.<OtherParty>builder().value(OtherParty.builder().confidentialityRequired(YES).build()).build();
        otherPartyList.add(ccdValue1);
        callback.getCaseDetails().getCaseData().setOtherParties(otherPartyList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YES, response.getData().getIsConfidentialCase());
    }

    @Test
    public void givenNewAppellantName_thenSetCaseName() {
        sscsCaseDataBefore.getWorkAllocationFields().setCaseNameHmctsInternal("Old Name");
        sscsCaseData.getWorkAllocationFields().setCaseNameHmctsInternal("Old Name");
        sscsCaseData.setAppeal(Appeal.builder()
                        .benefitType(new BenefitType("UC", "Universal credit"))
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("New").lastName("Name").build())
                                .build())
                        .build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("New Name", response.getData().getWorkAllocationFields().getCaseNameHmctsInternal());
        assertEquals("New Name", response.getData().getWorkAllocationFields().getCaseNameHmctsRestricted());
        assertEquals("New Name", response.getData().getWorkAllocationFields().getCaseNamePublic());
    }

    @Test
    public void givenAppellantNameAdded_thenSetCaseName() {
        sscsCaseData.setAppeal(Appeal.builder()
                        .benefitType(new BenefitType("UC", "Universal credit"))
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("New").lastName("Name").build())
                                .build())
                        .build());

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("New Name", response.getData().getWorkAllocationFields().getCaseNameHmctsInternal());
        assertEquals("New Name", response.getData().getWorkAllocationFields().getCaseNameHmctsRestricted());
        assertEquals("New Name", response.getData().getWorkAllocationFields().getCaseNamePublic());
    }

    @Test
    public void givenAppellantNameDeleted_thenUnsetCaseName() {
        sscsCaseDataBefore.getWorkAllocationFields().setCaseNameHmctsInternal("Old Name");
        sscsCaseData.getWorkAllocationFields().setCaseNameHmctsInternal("Old Name");
        sscsCaseData.setAppeal(Appeal.builder()
                .benefitType(new BenefitType("UC", "Universal credit"))
                .appellant(Appellant.builder().build())
                .build());

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getWorkAllocationFields().getCaseNameHmctsInternal());
        assertNull(response.getData().getWorkAllocationFields().getCaseNameHmctsRestricted());
        assertNull(response.getData().getWorkAllocationFields().getCaseNamePublic());
    }

    @Test
    public void givenBenefitTypeChanged_thenSetCaseCategories() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        sscsCaseDataBefore.getWorkAllocationFields().setCategories(Benefit.ESA);
        sscsCaseData.getWorkAllocationFields().setCategories(Benefit.ESA);
        sscsCaseDataBefore.getAppeal().setBenefitType(new BenefitType("ESA", "Employment and Support Allowance"));
        sscsCaseData.setAppeal(Appeal.builder()
                .benefitType(new BenefitType("UC", "Universal Credit"))
                .appellant(Appellant.builder()
                        .name(Name.builder().firstName("New").lastName("Name").build())
                        .build())
                .build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("Universal Credit", response.getData().getWorkAllocationFields().getCaseAccessCategory());
        assertEquals("Universal Credit", response.getData().getWorkAllocationFields().getCaseManagementCategory().getValue().getLabel());
        assertEquals("UC", response.getData().getWorkAllocationFields().getCaseManagementCategory().getValue().getCode());
    }

    @Test
    public void givenDeleteBenefitType_thenAddError() {
        sscsCaseDataBefore.getWorkAllocationFields().setCategories(Benefit.ESA);
        sscsCaseDataBefore = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().address(Address.builder().postcode("CM120NS").build()).build()).build())
                .build();

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);

        sscsCaseData.getWorkAllocationFields().setCategories(Benefit.ESA);
        sscsCaseData.setAppeal(Appeal.builder()
                .benefitType(new BenefitType("", ""))
                .appellant(Appellant.builder()
                        .name(Name.builder().firstName("New").lastName("Name").build())
                        .build())
                .build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
    }

    @Test
    public void givenNoBenefitType_thenAddWarning() {
        sscsCaseDataBefore = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .appellant(Appellant.builder().address(Address.builder().postcode("CM120NS").build()).build()).build())
                .build();

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);

        sscsCaseData.setAppeal(Appeal.builder()
                .benefitType(new BenefitType("", ""))
                .appellant(Appellant.builder()
                        .name(Name.builder().firstName("New").lastName("Name").build())
                        .build())
                .build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getWarnings().size());
    }

    @Test
    public void givenNoBenefitTypeBeforeAddCode_thenSetCategories() {
        sscsCaseDataBefore = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .appellant(Appellant.builder().address(Address.builder().postcode("CM120NS").build()).build()).build())
                .build();

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);

        sscsCaseData.setAppeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder()
                        .name(Name.builder().firstName("New").lastName("Name").build())
                        .build())
                .build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Personal Independence Payment", response.getData().getWorkAllocationFields().getCaseAccessCategory());
        assertEquals("Personal Independence Payment", response.getData().getWorkAllocationFields().getCaseManagementCategory().getValue().getLabel());
        assertEquals("PIP", response.getData().getWorkAllocationFields().getCaseManagementCategory().getValue().getCode());
    }

    @Test
    public void givenInvalidBenefitType_thenAddError() {
        sscsCaseDataBefore = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .appellant(Appellant.builder().address(Address.builder().postcode("CM120NS").build()).build()).build())
                .build();

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);

        sscsCaseData.setAppeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("turnip").build())
                .appellant(Appellant.builder()
                        .name(Name.builder().firstName("New").lastName("Name").build())
                        .build())
                .build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
    }
}
