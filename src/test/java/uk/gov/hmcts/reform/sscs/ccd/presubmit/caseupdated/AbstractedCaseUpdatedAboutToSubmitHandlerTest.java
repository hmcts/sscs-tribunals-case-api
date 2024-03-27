package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.BenefitCode.PIP_NEW_CLAIM;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Issue.AT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.CTSC_CLERK;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SUPER_USER;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.AssociatedCaseLinkHelper;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.*;


@Slf4j
@ExtendWith(MockitoExtension.class)
public abstract class AbstractedCaseUpdatedAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    @Mock
    private CcdService ccdService;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private AirLookupService airLookupService;

    @Mock
    private IdamService idamService;

    @Mock
    private RefDataService refDataService;

    @Mock
    private VenueService venueService;

    @Mock
    private SessionCategoryMapService categoryMapService;

    private CaseUpdatedAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;

    private SscsCaseData sscsCaseDataBefore;

    private Appeal appeal;

    abstract boolean getAddLinkToOtherAssociatedCasesV2();

    @BeforeEach
    void setUp() {
        Boolean addLinkToOtherAssociatedCasesV2Boolean = getAddLinkToOtherAssociatedCasesV2();
        AssociatedCaseLinkHelper associatedCaseLinkHelper = new AssociatedCaseLinkHelper(ccdService, idamService, updateCcdCaseService);
        ReflectionTestUtils.setField(associatedCaseLinkHelper, "addLinkToOtherAssociatedCasesV2Enabled", addLinkToOtherAssociatedCasesV2Boolean);

        handler = new CaseUpdatedAboutToSubmitHandler(
            regionalProcessingCenterService,
            associatedCaseLinkHelper,
            airLookupService,
            new DwpAddressLookupService(),
            idamService,
            refDataService,
            venueService,
            categoryMapService,
            true);

        lenient().when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .build())
                .appellant(Appellant.builder()
                        .name(Name.builder().firstName("First").lastName("Last").build())
                        .address(Address.builder().line1("Line1").line2("Line2").postcode("CM120NS").build())
                        .identity(Identity.builder().nino("AB223344B").dob("1995-12-20").build())
                        .isAppointee("Yes")
                        .appointee(
                                Appointee.builder()
                                        .address(Address.builder()
                                                .line1("123 the Street")
                                                .postcode("CM120NS")
                                                .build()
                                        )
                                        .build()
                        )
                    .build())
                    .rep(Representative.builder().address(Address.builder()
                            .line1("123 the Street")
                            .postcode("CM120NS")
                            .build()
                    )
                            .build()
                    )
                .build())
                .jointParty(JointParty.builder()
                        .jointPartyAddressSameAsAppellant(NO)
                        .address(Address.builder()
                                .line1("123 the street")
                                .postcode("CM120NS")
                                .build()
                        )
                        .build()
                )
            .benefitCode("002")
            .issueCode("DD")
            .isFqpmRequired(NO)
            .build();


        sscsCaseDataBefore = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .address(Address.builder()
                            .line1("123 the Street")
                        .postcode("CM120NS")
                        .build())
                        .appointee(
                                Appointee.builder()
                                        .address(Address.builder()
                                                .line1("123 the Street")
                                                .postcode("CM120NS")
                                                .build()
                                        )
                                        .build()

                        )
                    .build()
                )
                    .rep(Representative.builder().address(Address.builder()
                            .line1("123 the Street")
                            .postcode("CM120NS")
                            .build()
                    )
                                    .build()
                    )
                .build())
                .jointParty(JointParty.builder()
                        .address(Address.builder()
                                .postcode("CM120NS")
                                .build()
                        )
                                .build()
                )
                        .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        lenient().when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        lenient().when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);
        lenient().when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        lenient().when(idamService.getUserDetails(anyString())).thenReturn(UserDetails.builder()
            .roles(List.of(SUPER_USER.getValue()))
            .build());
        lenient().when(categoryMapService.getSessionCategory(any(String.class), any(String.class), any(boolean.class),
                any(boolean.class))).thenReturn(new SessionCategoryMap(PIP_NEW_CLAIM, AT, true, true));
        appeal = callback.getCaseDetails().getCaseData().getAppeal();
    }

    @Test
    void givenANonCaseUpdatedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    void givenACaseUpdatedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @EnumSource(names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonCaseUpdatedCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    void givenACaseUpdatedCallbackType_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    void givenACaseUpdatedEvent_thenSetCaseCode() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002DD", response.getData().getCaseCode());
    }

    @Test
    void givenACaseUpdatedEventWithUcCase_thenSetCaseCode() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        sscsCaseData.setElementsDisputedList(elementList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("uc").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002DD", response.getData().getCaseCode());
    }

    @Test
    void givenACaseUpdatedEventWithEmptyBenefitCodeAndCaseCode_thenDoNotOverrideCaseCode() {
        callback.getCaseDetails().getCaseData().setBenefitCode(null);
        callback.getCaseDetails().getCaseData().setIssueCode(null);
        callback.getCaseDetails().getCaseData().setCaseCode("002DD");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002DD", response.getData().getCaseCode());
    }

    @Test
    void givenACaseUpdatedEventWithEmptyAppellantDetails_thenProvideAnError() {
        Representative representative = Representative.builder()
                .name(Name.builder().firstName("Test").lastName("Test").build())
                .address(Address.builder().line1("123 Lane").postcode(null).build())
                .hasRepresentative(YES.getValue())
                .build();

        JointParty jointParty = JointParty.builder()
                .name(Name.builder().firstName("Test").lastName("Test").build())
                .jointPartyAddressSameAsAppellant(NO)
                .address(Address.builder().line1("123 Lane").postcode(null).build())
                .hasJointParty(YES)
                .build();

        callback.getCaseDetails().getCaseData().getAppeal().setRep(representative);
        callback.getCaseDetails().getCaseData().setJointParty(jointParty);

        var caseData = callback.getCaseDetails().getCaseData();

        caseData.getAppeal().getAppellant().getAddress().setPostcode(null);
        caseData.getAppeal().getAppellant().getAppointee().getAddress().setPostcode(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Set<String> expectedErrorMessages = Set.of("You must enter a valid UK postcode for the appellant",
                "You must enter a valid UK postcode for the representative",
                "You must enter a valid UK postcode for the joint party",
                "You must enter a valid UK postcode for the appointee");

        assertThat(response.getErrors(), is(expectedErrorMessages));
    }

    @Test
    public void givenPartyTypeHasFirstLineOfAddressAndInvalidPostcode_thenProvideAnError() {
        Representative representative = Representative.builder()
                .name(Name.builder().firstName("Test").lastName("Test").build())
                .address(Address.builder().line1("123 Lane").postcode("73GH Y7U").build())
                .hasRepresentative(YES.getValue())
                .build();

        JointParty jointParty = JointParty.builder()
                .name(Name.builder().firstName("Test").lastName("Test").build())
                .jointPartyAddressSameAsAppellant(NO)
                .address(Address.builder().line1("123 Lane").postcode("73GH Y7U").build())
                .hasJointParty(YES)
                .build();

        callback.getCaseDetails().getCaseData().getAppeal().setRep(representative);
        callback.getCaseDetails().getCaseData().setJointParty(jointParty);

        var caseData = callback.getCaseDetails().getCaseData();

        caseData.getAppeal().getAppellant().getAddress().setPostcode("73GH Y7U");
        caseData.getAppeal().getAppellant().getAppointee().getAddress().setPostcode("73GH Y7U");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Set<String> expectedErrorMessages = Set.of("You must enter a valid UK postcode for the appellant",
                "You must enter a valid UK postcode for the representative",
                "You must enter a valid UK postcode for the joint party",
                "You must enter a valid UK postcode for the appointee");

        assertThat(response.getErrors(), is(expectedErrorMessages));
    }

    @Test
    public void givenPartyTypeHasNoFirstLineOfAddressAndValidPostcode_thenProvideAnError() {
        Representative representative = Representative.builder()
                .name(Name.builder().firstName("Test").lastName("Test").build())
                .address(Address.builder().line1(null).postcode("CM120NS").build())
                .hasRepresentative(YES.getValue())
                .build();

        JointParty jointParty = JointParty.builder()
                .name(Name.builder().firstName("Test").lastName("Test").build())
                .jointPartyAddressSameAsAppellant(NO)
                .address(Address.builder().line1(null).postcode("CM120NS").build())
                .hasJointParty(YES)
                .build();

        callback.getCaseDetails().getCaseData().getAppeal().setRep(representative);
        callback.getCaseDetails().getCaseData().setJointParty(jointParty);

        var caseData = callback.getCaseDetails().getCaseData();

        caseData.getAppeal().getAppellant().getAddress().setLine1(null);
        caseData.getAppeal().getAppellant().getAppointee().getAddress().setLine1(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Set<String> expectedErrorMessages = Set.of("You must enter address line 1 for the appellant",
                "You must enter address line 1 for the representative",
                "You must enter address line 1 for the joint party",
                "You must enter address line 1 for the appointee");

        assertThat(response.getErrors(), is(expectedErrorMessages));
    }

    @Test
    public void givenPartyTypeIsSetToFalse_thenGiveNoValidation() {
        Representative representative = Representative.builder()
                .name(Name.builder().firstName("").lastName("").build())
                .address(Address.builder().line1(null).postcode(null).build())
                .hasRepresentative(NO.getValue())
                .build();

        JointParty jointParty = JointParty.builder()
                .name(Name.builder().firstName("").lastName("").build())
                .jointPartyAddressSameAsAppellant(NO)
                .address(Address.builder().line1(null).postcode(null).build())
                .hasJointParty(NO)
                .build();

        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setIsAppointee("No");
        callback.getCaseDetails().getCaseData().getAppeal().setRep(representative);
        callback.getCaseDetails().getCaseData().setJointParty(jointParty);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));

    }

    @Test
    public void givenPartyTypeIsSetToTrueAndAddressIsEmpty_thenGiveValidation() {
        Representative representative = Representative.builder()
                .name(Name.builder().firstName("").lastName("").build())
                .address(Address.builder().line1(null).postcode(null).build())
                .hasRepresentative(YES.getValue())
                .build();

        JointParty jointParty = JointParty.builder()
                .name(Name.builder().firstName("").lastName("").build())
                .jointPartyAddressSameAsAppellant(NO)
                .address(Address.builder().line1(null).postcode(null).build())
                .hasJointParty(YES)
                .build();

        var caseData = callback.getCaseDetails().getCaseData();

        caseData.setJointParty(jointParty);
        caseData.getAppeal().setRep(representative);
        caseData.getAppeal().getAppellant().setIsAppointee("Yes");
        caseData.getAppeal().getAppellant().getAppointee().getAddress().setLine1(null);
        caseData.getAppeal().getAppellant().getAppointee().getAddress().setPostcode(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(10));

    }

    @Test
    public void givenJointPartySameAddressAsAppeallantIsSetToYes_validateAppeallantAddressNotJointParty() {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setLine1(null);
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setPostcode("73GH Y7U");

        JointParty jointParty = JointParty.builder()
                .name(Name.builder().firstName("Test").lastName("Test").build())
                .jointPartyAddressSameAsAppellant(YES)
                .address(Address.builder().line1("123 The Street").postcode("CM120NS").build())
                .hasJointParty(YES)
                .build();

        callback.getCaseDetails().getCaseData().setJointParty(jointParty);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Set<String> expectedErrorMessages = Set.of("You must enter address line 1 for the appellant",
                "You must enter a valid UK postcode for the appellant");

        assertThat(response.getErrors(), is(expectedErrorMessages));
    }

    @Test
    public void givenJointSameAddressAsAppeallantIsSetToNo_validateJointPartyAddress() {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setLine1("123 The Street");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setPostcode("CM120NS");

        JointParty jointParty = JointParty.builder()
                .name(Name.builder().firstName("Test").lastName("Test").build())
                .jointPartyAddressSameAsAppellant(YES)
                .address(Address.builder().line1(null).postcode(null).build())
                .hasJointParty(YES)
                .build();

        callback.getCaseDetails().getCaseData().setJointParty(jointParty);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));

    }

    @Test
    void givenACaseUpdatedEventWithEmptyAppointeeDetails_thenProvideAnError() {
        sscsCaseData.getAppeal().getAppellant().setIsAppointee("Yes");
        Appointee appointee = Appointee.builder()
                .name(Name.builder().firstName("").lastName("").build())
                .address(Address.builder().line1("").line2("").postcode("").build())
                .identity(Identity.builder().nino("").dob("").build())
                .build();

        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setAppointee(appointee);
        callback.getCaseDetails().getCaseData().setHasOtherPartyAppointee(YES);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size(), is(4));
    }

    @Test
    void givenACaseUpdatedEventWithEmptyRepresentativeDetails_thenProvideAnError() {
        Representative representative = Representative.builder()
                .name(Name.builder().firstName("").lastName("").build())
                .address(Address.builder().line1("123 Lane").postcode("CM120NS").build())
                .hasRepresentative(YES.getValue())
                .build();
        callback.getCaseDetails().getCaseData().getAppeal().setRep(representative);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(2));
    }

    @Test
    void givenACaseUpdatedEventWithNullRepresentativeAddress_thenProvideAnError() {
        Representative representative = Representative.builder()
                .name(Name.builder().firstName("Test").lastName("Test").build())
                .address(null)
                .hasRepresentative(YES.getValue())
                .build();
        callback.getCaseDetails().getCaseData().getAppeal().setRep(representative);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(2));
    }

    @Test
    void givenACaseUpdatedEventWithNullRepresentativeDetails_thenProvideAnError() {
        Representative representative = Representative.builder()
                .name(null)
                .address(Address.builder().line1("123 Lane").postcode("CM120NS").build())
                .hasRepresentative(YES.getValue())
                .build();
        callback.getCaseDetails().getCaseData().getAppeal().setRep(representative);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
    }

    @Test
    void givenACaseUpdatedEventWithEmptyRepresentativeNameButOrganisation_ThenReturnNoWarnings() {
        Representative representative = Representative.builder()
                .hasRepresentative(YES.getValue())
                .address(Address.builder().line1("123 Lane").postcode("CM120NS").build())
                .organisation("Test Organisation")
                .build();
        callback.getCaseDetails().getCaseData().getAppeal().setRep(representative);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    void givenACaseUpdatedEventWithEmptyJointPartyDetails_thenProvideAnError() {
        JointParty jointParty = JointParty.builder()
                .name(Name.builder().firstName("").lastName("").build())
                .jointPartyAddressSameAsAppellant(NO)
                .address(Address.builder().line1("123 Lane").postcode("73GH Y7U").build())
                .hasJointParty(YES)
                .build();
        callback.getCaseDetails().getCaseData().setJointParty(jointParty);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(3));
    }


    @Test
    void givenAnAppealWithPostcode_updateRpc() {
        when(regionalProcessingCenterService.getByPostcode("CM120NS")).thenReturn(RegionalProcessingCenter.builder().name("Region1").address1("Line1").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Line1", response.getData().getRegionalProcessingCenter().getAddress1());
        assertEquals("Region1", response.getData().getRegion());
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () ->
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    abstract void verifyUpdateCcdCaseServiceIsCalledWithExpectedValues(UpdateCcdCaseService updateCcdCaseService, SscsCaseDetails matchingCase1, SscsCaseDetails matchingCase2);

    @Test
    void givenMultipleAssociatedCases_thenAddAllAssociatedCaseLinksToCase() {
        Appellant appellant = Appellant.builder()
                .name(Name.builder().firstName("First").lastName("Last").build())
                .address(Address.builder().line1("Line1").line2("Line2").postcode("CM120NS").build())
                .identity(Identity.builder().nino("AB223344B").dob("1995-12-20").build())
                .isAppointee("Yes")
                .appointee(
                        Appointee.builder()
                                .address(Address.builder()
                                        .line1("123 the Street")
                                        .postcode("CM120NS")
                                        .build()
                                )
                                .build()
                )
                .build();
        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().ccdCaseId("12345678").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().ccdCaseId("56765676").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);

        when(ccdService.findCaseBy(anyString(), anyString(), any())).thenReturn(matchedByNinoCases);
        callback.getCaseDetails().getCaseData().setCaseCode("002DD");
        callback.getCaseDetails().getCaseData().getAppeal().setAppellant(appellant);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyUpdateCcdCaseServiceIsCalledWithExpectedValues(updateCcdCaseService, matchingCase1, matchingCase2);

        assertEquals(2, response.getData().getAssociatedCase().size());
        assertEquals("Yes", response.getData().getLinkedCasesBoolean());
        assertEquals("56765676", response.getData().getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("12345678", response.getData().getAssociatedCase().get(1).getValue().getCaseReference());

        assertEquals("Yes", matchingCase1.getData().getLinkedCasesBoolean());
        assertEquals("ccdId", matchingCase1.getData().getAssociatedCase().get(0).getValue().getCaseReference());

        assertEquals("Yes", matchingCase2.getData().getLinkedCasesBoolean());
        assertEquals("ccdId", matchingCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @ParameterizedTest
    @CsvSource({"Birmingham,Glasgow,Yes", "Glasgow,Birmingham,No"})
    void givenChangeInRpcChangeIsScottish(String oldRpcName, String newRpcName, String expected) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setIsScottishCase("No");
        RegionalProcessingCenter oldRpc = RegionalProcessingCenter.builder().name(oldRpcName).build();
        RegionalProcessingCenter newRpc = RegionalProcessingCenter.builder().name(newRpcName).build();

        handler.maybeChangeIsScottish(oldRpc, newRpc, caseData);

        assertEquals(expected, caseData.getIsScottishCase());
    }

    @ParameterizedTest
    @CsvSource({"Birmingham,No", "Glasgow,Yes"})
    void givenChangeInNullRpcChangeIsScottish(String newRpcName, String expected) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setIsScottishCase("No");
        RegionalProcessingCenter oldRpc = null;
        RegionalProcessingCenter newRpc = RegionalProcessingCenter.builder().name(newRpcName).build();

        handler.maybeChangeIsScottish(oldRpc, newRpc, caseData);

        assertEquals(expected, caseData.getIsScottishCase());
    }

    @ParameterizedTest
    @CsvSource({"Birmingham,No", "Glasgow,Yes"})
    void givenAnAppealWithPostcode_updateRpcToScottish(String newRpcName, String expectedIsScottish) {
        when(regionalProcessingCenterService.getByPostcode("CM120NS")).thenReturn(RegionalProcessingCenter.builder().name(newRpcName).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(newRpcName, response.getData().getRegionalProcessingCenter().getName());
        assertEquals(expectedIsScottish, response.getData().getIsScottishCase());
    }

    @Test
    void givenAnAppealWithNewAppellantPostcodeAndNoAppointee_thenUpdateProcessingVenue() {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setIsAppointee("No");

        when(regionalProcessingCenterService.getByPostcode("AB12 00B")).thenReturn(
            RegionalProcessingCenter.builder()
                .name("rpcName")
            .postcode("rpcPostcode")
            .epimsId("rpcEpimsId")
                .build());
        String venueB = "VenueB";
        String venueEpimsId = "12345";
        when(venueService.getEpimsIdForVenue(venueB)).thenReturn(venueEpimsId);
        when(airLookupService.lookupAirVenueNameByPostCode("AB12 00B", sscsCaseData.getAppeal().getBenefitType())).thenReturn(
            venueB);

        when(refDataService.getCourtVenueRefDataByEpimsId(venueEpimsId)).thenReturn(CourtVenue.builder().courtStatus("Open").regionId("regionId").build());

        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setPostcode("AB12 00B");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(venueB, response.getData().getProcessingVenue());
        assertNotNull(response.getData().getCaseManagementLocation());
        assertEquals("rpcEpimsId", response.getData().getCaseManagementLocation().getBaseLocation());
        assertEquals("regionId", response.getData().getCaseManagementLocation().getRegion());
    }

    @Test
    void givenAnAppealWithNewAppointeePostcode_thenUpdateProcessingVenueWithAppointeeVenue() {
        when(regionalProcessingCenterService.getByPostcode("AB12 00B")).thenReturn(
            RegionalProcessingCenter.builder()
                .name("rpcName")
                .postcode("rpcPostcode")
                .epimsId("rpcEpimsId")
                .build());

        String venueB = "VenueB";
        String venueEpimsId = "12345";

        when(venueService.getEpimsIdForVenue(venueB)).thenReturn(venueEpimsId);
        when(airLookupService.lookupAirVenueNameByPostCode("AB12 00B", sscsCaseData.getAppeal().getBenefitType())).thenReturn(venueB);
        when(refDataService.getCourtVenueRefDataByEpimsId(venueEpimsId)).thenReturn(CourtVenue.builder().courtStatus("Open").regionId("regionId").build());

        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder()
                .name(Name.builder().firstName("First").lastName("Last").build())
                .identity(Identity.builder().nino("Nino").dob("Dob").build())
                .address(Address.builder().postcode("AB12 00B").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(venueB, response.getData().getProcessingVenue());
        assertNotNull(response.getData().getCaseManagementLocation());
        assertEquals("rpcEpimsId", response.getData().getCaseManagementLocation().getBaseLocation());
        assertEquals("regionId", response.getData().getCaseManagementLocation().getRegion());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void givenAnAppealWithNullOrEmptyPostcode_thenDoNotUpdateProcessingVenue(String postcode) {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setPostcode("AB1200B");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setPostcode(postcode);
        callback.getCaseDetails().getCaseData().setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("rpc1").build());
        callback.getCaseDetails().getCaseData().setProcessingVenue("VenueA");
        callback.getCaseDetails().getCaseData().setCaseManagementLocation(CaseManagementLocation.builder().baseLocation("base").region("region").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(airLookupService);
        assertEquals("VenueA", response.getData().getProcessingVenue());
        assertEquals("rpc1", response.getData().getRegionalProcessingCenter().getName());
        assertNotNull(response.getData().getCaseManagementLocation());
        assertEquals("base", response.getData().getCaseManagementLocation().getBaseLocation());
        assertEquals("region", response.getData().getCaseManagementLocation().getRegion());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void givenAnAppealWithNewAppointeeButEmptyPostcode_thenUpdateProcessingVenueWithAppellantVenue(String postCode) {
        when(regionalProcessingCenterService.getByPostcode("AB12 00B")).thenReturn(
            RegionalProcessingCenter.builder()
                .name("rpcName")
                .postcode("rpcPostcode")
                .epimsId("rpcEpimsId")
                .build());

        String venueB = "VenueB";
        String venueEpimsId = "12345";

        when(venueService.getEpimsIdForVenue(venueB)).thenReturn(venueEpimsId);
        when(airLookupService.lookupAirVenueNameByPostCode("AB12 00B", sscsCaseData.getAppeal().getBenefitType()))
            .thenReturn(venueB);
        when(refDataService.getCourtVenueRefDataByEpimsId(venueEpimsId)).thenReturn(CourtVenue.builder().courtStatus("Open").regionId("regionId").build());



        Appellant appellant = callback.getCaseDetails().getCaseData().getAppeal().getAppellant();
        appellant.getAddress().setPostcode("AB12 00B");
        appellant.setIsAppointee("Yes");
        appellant.setAppointee(Appointee.builder()
                .name(Name.builder().build())
                .address(Address.builder().postcode(postCode).build())
                .identity(Identity.builder().build())
                .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(venueB, response.getData().getProcessingVenue());
        assertNotNull(response.getData().getCaseManagementLocation());
        assertEquals("rpcEpimsId", response.getData().getCaseManagementLocation().getBaseLocation());
        assertEquals("regionId", response.getData().getCaseManagementLocation().getRegion());
    }

    @Test
    void givenAnAppealWithNewAppointee_thenUpdateRpcWithAppointeeVenue() {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setPostcode("AB1200B");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder()
                .name(Name.builder().firstName("New").lastName("Name").build())
                .identity(Identity.builder().nino("nino").dob("dob").build())
                .address(Address.builder().line1("Line 1").line2("Line 2").postcode("APP EEE").build())
                .build());

        when(regionalProcessingCenterService.getByPostcode("APP EEE")).thenReturn(RegionalProcessingCenter.builder()
            .name("AppointeeVenue")
            .address1("Line1")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("AppointeeVenue", response.getData().getRegionalProcessingCenter().getName());
    }

    @Test
    void givenAnAppealWithNoAppointee_thenUpdateRpcWithAppellantVenue() {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress().setPostcode("AB1200B");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setIsAppointee("No");
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder()
                .name(Name.builder().firstName("New").lastName("Name").build())
                .identity(Identity.builder().dob("Dob").nino("nino").build())
                .address(Address.builder().line1("Line 1").line2("Line 2").postcode("APP_EEE").build())
            .build());

        when(regionalProcessingCenterService.getByPostcode("AB1200B")).thenReturn(RegionalProcessingCenter.builder()
            .name("AppellantVenue")
            .address1("Line1")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("AppellantVenue", response.getData().getRegionalProcessingCenter().getName());
    }

    @Disabled("commented out as case loader is failing on this validation checks, we need to do another data exercise to clean the data")
    @ParameterizedTest
    @CsvSource({"!. House, House, House, House",
        "~., 101 House, House, House",
        " Ho.use, ., \"101 House, House",
        " ., ãHouse, âHouse, &101 House"})
    void givenACaseUpdateEventWithInvalidAppellantAddressDetails_thenReturnError(String line1, String line2, String town, String county) {
        Address appellantAddress = callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getAddress();
        appellantAddress.setLine1(line1);
        appellantAddress.setLine2(line2);
        appellantAddress.setCounty(county);
        appellantAddress.setTown(town);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        long numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(1, numberOfExpectedError);
    }

    @Disabled("commented out as case loader is failing on this validation checks, we need to do another data exercise to clean the data")
    @ParameterizedTest
    @CsvSource({"!. House, House, House, House",
        "~., 101 House, House, House",
        " Ho.use, ., \"101 House, House",
        " ., ãHouse, âHouse, &101 House"})
    void givenACaseUpdateEventWithInvalidRepresentativeAddressDetails_thenReturnError(String line1, String line2, String town, String county) {

        Representative representative = Representative.builder().address(buildAddress(line1, line2, county, town)).build();

        callback.getCaseDetails().getCaseData().getAppeal().setRep(representative);
        callback.getCaseDetails().getCaseData().setHasRepresentative(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        long numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(1, numberOfExpectedError);
    }

    @Disabled("commented out as case loader is failing on this validation checks, we need to do another data exercise to clean the data")
    @ParameterizedTest
    @CsvSource({"!. House, House, House, House",
        "~., 101 House, House, House",
        " Ho.use, ., \"101 House, House",
        " ., ãHouse, âHouse, &101 House"})
    void givenACaseUpdateEventWithInvalidAppointeeAddressDetails_thenReturnError(String line1, String line2, String town, String county) {
        Appointee appointee = Appointee.builder().address(buildAddress(line1, line2, county, town)).build();
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setAppointee(appointee);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        long numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(1, numberOfExpectedError);
    }

    @Disabled("commented out as case loader is failing on this validation checks, we need to do another data exercise to clean the data")
    @ParameterizedTest
    @CsvSource({"!. House, House, House, House",
        "~., 101 House, House, House",
        " Ho.use, ., \"101 House, House",
        " ., ãHouse, âHouse, &101 House"})
    void givenACaseUpdateEventWithInvalidJointPartyAddressDetails_thenReturnError(String line1, String line2, String town, String county) {
        callback.getCaseDetails().getCaseData().getJointParty().setAddress(buildAddress(line1, line2, county, town));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        long numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(1, numberOfExpectedError);
    }

    @ParameterizedTest
    @CsvSource({"  ,   ,   ,   ",
        "Ts. Test's Ltd, Ts. Test's Ltd, Ts. Test's Ltd, Ts. Test's Ltd",
        "A“”\"’'?![]()/£:_+-%&, A“”\"’'?![]()/£:_+-%&, A“”\"’'?![]()/£:_+-%&, A“”\"’'?![]()/£:_+-%&",
        ",Test Street,,Test Street,,Test Street,,Test Street",
        ".dot Street,.dot Street,.dot Street,.dot Street"})
    void givenACaseUpdateEventWithAddressDetails_thenShouldNotReturnError(String line1, String line2, String town, String county) {
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

        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder()
                .name(Name.builder().build())
                .identity(Identity.builder().build())
                .address(address).build());
        response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(0, numberOfExpectedError);

        callback.getCaseDetails().getCaseData().getJointParty().setAddress(address);
        response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        numberOfExpectedError = getNumberOfExpectedError(response);
        assertEquals(0, numberOfExpectedError);
    }

    @Test
    void givenChildSupportCaseAndCaseCodeIsChangedToNonChildSupportCodeAndCaseHasOtherParty_thenShowError() {
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
    void givenChildSupportCaseAndCaseCodeIsChangedToNonChildSupportCodeAndCaseHasNoOtherParty_thenShowWarning() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(), is("The benefit code will be changed to a non-child support benefit code"));
    }

    @ParameterizedTest
    @CsvSource({"022", "023", "024", "025", "026", "028"})
    void givenChildSupportCaseAndCaseCodeIsSetToChildSupportCode_thenNoWarningOrErrorIsShown(String childSupportBenefitCode) {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode(childSupportBenefitCode);
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    void givenChildSupportCaseAndCaseCodeIsAlreadyANonChildSupportCase_thenShowErrorOrWarning() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("001");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    void givenBenefitTypeAndDwpIssuingOfficeEmpty_thenAddWarningMessages() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(null);
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(2, response.getWarnings().size());
        assertThat(response.getWarnings(), hasItems("Benefit type code is empty", "FTA issuing office is empty"));
    }

    @Test
    void givenInvalidBenefitTypeAndDwpIssuingOfficeEmpty_thenAddWarningMessages() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("INVALID").build());
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getWarnings().size());
        assertEquals(1, response.getErrors().size());

        assertThat(response.getWarnings(), hasItems("FTA issuing office is empty"));
        assertThat(response.getErrors(), hasItems("Benefit type code is invalid, should be one of: ESA, JSA, PIP, DLA, UC, carersAllowance, attendanceAllowance, "
            + "bereavementBenefit, industrialInjuriesDisablement, maternityAllowance, socialFund, incomeSupport, bereavementSupportPaymentScheme, "
            + "industrialDeathBenefit, pensionCredit, retirementPension, childSupport, taxCredit, guardiansAllowance, taxFreeChildcare, "
            + "homeResponsibilitiesProtection, childBenefit, thirtyHoursFreeChildcare, guaranteedMinimumPension, nationalInsuranceCredits"));
    }

    @Test
    void givenInvalidBenefitTypeAndInvalidDwpIssuingOffice_thenAddWarningMessages() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("INVALID").build());
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice("Test").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertThat(response.getErrors(), hasItems("Benefit type code is invalid, should be one of: ESA, JSA, PIP, DLA, UC, carersAllowance, attendanceAllowance, "
            + "bereavementBenefit, industrialInjuriesDisablement, maternityAllowance, socialFund, incomeSupport, bereavementSupportPaymentScheme, "
            + "industrialDeathBenefit, pensionCredit, retirementPension, childSupport, taxCredit, guardiansAllowance, taxFreeChildcare, "
            + "homeResponsibilitiesProtection, childBenefit, thirtyHoursFreeChildcare, guaranteedMinimumPension, nationalInsuranceCredits"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ',', textBlock = """
        ESA,'DWP issuing office is invalid, should one of: Balham DRT, Birkenhead LM DRT, Chesterfield DRT, Coatbridge Benefit Centre, Inverness DRT, Lowestoft DRT, Milton Keynes DRT, Norwich DRT, Sheffield DRT, Springburn DRT, Watford DRT, Wellingborough DRT, Worthing DRT, Recovery from Estates'
        PIP,'DWP issuing office is invalid, should one of: 1, 2, 3, 4, 5, 6, 7, 8, 9, AE, Recovery from Estates'
        DLA,'DWP issuing office is invalid, should one of: Disability Benefit Centre 4, The Pension Service 11, Recovery from Estates'
        UC,'DWP issuing office is invalid, should one of: Universal Credit, Recovery from Estates'
        carersAllowance,'DWP issuing office is invalid, should one of: Carer’s Allowance Dispute Resolution Team'
        bereavementBenefit,'DWP issuing office is invalid, should one of: Pensions Dispute Resolution Team'
        attendanceAllowance,'DWP issuing office is invalid, should one of: The Pension Service 11, Recovery from Estates'
        industrialInjuriesDisablement,'DWP issuing office is invalid, should one of: Barrow IIDB Centre, Barnsley Benefit Centre'
        maternityAllowance,'DWP issuing office is invalid, should one of: Walsall Benefit Centre'
        JSA,'DWP issuing office is invalid, should one of: Worthing DRT, Birkenhead DRT, Inverness DRT, Recovery from Estates'
        socialFund,'DWP issuing office is invalid, should one of: St Helens Sure Start Maternity Grant, Funeral Payment Dispute Resolution Team, Pensions Dispute Resolution Team'
        incomeSupport,'DWP issuing office is invalid, should one of: Worthing DRT, Birkenhead DRT, Inverness DRT, Recovery from Estates'
        bereavementSupportPaymentScheme,'DWP issuing office is invalid, should one of: Pensions Dispute Resolution Team'
        industrialDeathBenefit,'DWP issuing office is invalid, should one of: Barrow IIDB Centre, Barnsley Benefit Centre'
        pensionCredit,'DWP issuing office is invalid, should one of: Pensions Dispute Resolution Team, Recovery from Estates'
        retirementPension,'DWP issuing office is invalid, should one of: Pensions Dispute Resolution Team, Recovery from Estates'
        childSupport,'DWP issuing office is invalid, should one of: Child Maintenance Service Group'
        """)
    void givenValidBenefitTypeAndInvalidDwpIssuingOffice_thenAddWarningMessages(String benefitCode, String warning) {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitCode).build());
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice("Test").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getWarnings().size());
        assertThat(response.getWarnings(), hasItems(warning));
    }

    @ParameterizedTest
    @CsvSource({"PIP,1,Newcastle", "ESA,Balham DRT,Balham DRT", "DLA,Disability Benefit Centre 4,DLA Child/Adult", "UC,Universal Credit,Universal Credit",
        "carersAllowance, Carer’s Allowance Dispute Resolution Team,Carers Allowance", "bereavementBenefit,Pensions Dispute Resolution Team,Bereavement Benefit",
        "attendanceAllowance,The Pension Service 11,Attendance Allowance", "industrialInjuriesDisablement,Barrow IIDB Centre,IIDB Barrow", "maternityAllowance,Walsall Benefit Centre,Maternity Allowance",
        "JSA,Worthing DRT,JSA Worthing", "socialFund,St Helens Sure Start Maternity Grant,SSMG","incomeSupport,Worthing DRT,IS Worthing",
        "bereavementSupportPaymentScheme,Pensions Dispute Resolution Team,Bereavement Support Payment", "industrialDeathBenefit,Barrow IIDB Centre,IDB Barrow",
        "pensionCredit,Pensions Dispute Resolution Team,Pension Credit", "retirementPension,Pensions Dispute Resolution Team,Retirement Pension",
        "childSupport,Child Maintenance Service Group,Child Support"
    })
    void givenValidBenefitTypeAndValidDwpIssuingOffice_thenThereIsNoWarningMessagesAndSetRegionalCenter(String benefitCode, String dwpIssuingOffice, String regionalCenter) {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitCode).build());
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice(dwpIssuingOffice).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(regionalCenter, response.getData().getDwpRegionalCentre());
    }

    @ParameterizedTest
    @CsvSource({
        "caseworker-sscs-superuser,1", "caseworker-sscs-systemupdate,0", "caseworker-sscs-clerk,1"
    })
    void givenHearingTypeOralAndWantsToAttendHearingNo_thenAddWarningMessage(String idamUserRole, int warnings) {
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

    @Test
    void givenAnAppealWithIncorrectExcludedDateStartDateAfterEndDate_thenProvideErrorMessage() {
        List<ExcludeDate> excludeDate = new ArrayList<>(List.of(
                ExcludeDate.builder().value(DateRange.builder()
                        .start("2023-06-17")
                        .end("2023-05-18")
                        .build()).build()));

        appeal.setHearingOptions(HearingOptions.builder().wantsToAttend("Yes").scheduleHearing("Yes").build());
        appeal.getHearingOptions().setExcludeDates(excludeDate);


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().iterator().next(), is("Unavailability start date must be before end date"));
    }

    @Test
    void givenAnAppealWithIncorrectExcludedDatesStartDateEmpty_thenProvideErrorMessage() {
        List<ExcludeDate> excludeDate = new ArrayList<>(List.of(
                ExcludeDate.builder().value(DateRange.builder()
                        .start(null)
                        .end("2023-05-17")
                        .build()).build()));

        appeal.setHearingOptions(HearingOptions.builder().wantsToAttend("Yes").scheduleHearing("Yes").build());
        appeal.getHearingOptions().setExcludeDates(excludeDate);


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().iterator().next(), is("Add a start date for unavailable dates"));
    }

    @Test
    void givenAnAppealWithIncorrectExcludedEndDate_thenProvideErrorMessage() {
        List<ExcludeDate> excludeDate;
        excludeDate = new ArrayList<>(List.of(
                ExcludeDate.builder().value(DateRange.builder()
                        .start("2023-06-17")
                        .end(null)
                        .build()).build()));

        appeal.setHearingOptions(HearingOptions.builder().wantsToAttend("Yes").scheduleHearing("Yes").build());
        appeal.getHearingOptions().setExcludeDates(excludeDate);


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().iterator().next(), is("Add an end date for unavailable dates"));
    }

    @Test
    void givenAnAppealWithEmptyExcludedDates_thenProvideErrorMessage() {
        List<ExcludeDate> excludeDate;
        excludeDate = new ArrayList<>(List.of(
                ExcludeDate.builder().value(DateRange.builder()
                        .start(null)
                        .end(null)
                        .build()).build()));

        appeal.setHearingOptions(HearingOptions.builder().wantsToAttend("Yes").scheduleHearing("Yes").build());
        appeal.getHearingOptions().setExcludeDates(excludeDate);


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().iterator().next(), is("Add a start date for unavailable dates"));
    }

    @Test
    void givenAnAppealWithCorrectExcludedDates_thenDontProvideError() {
        List<ExcludeDate> excludeDate;
        excludeDate = new ArrayList<>(List.of(
                ExcludeDate.builder().value(DateRange.builder()
                        .start("2023-06-17")
                        .end("2023-06-19")
                        .build()).build()));

        appeal.setHearingOptions(HearingOptions.builder().wantsToAttend("Yes").scheduleHearing("Yes").build());
        appeal.getHearingOptions().setExcludeDates(excludeDate);


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().isEmpty(), is(true));
    }

    @Test
    void givenAnAppealWithNoScheduledHearing_thenDontProvideError() {

        appeal.setHearingOptions(HearingOptions.builder().wantsToAttend("Yes").scheduleHearing("No").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().isEmpty(), is(true));
    }

    private long getNumberOfExpectedError(PreSubmitCallbackResponse<SscsCaseData> response) {
        return response.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("Invalid characters are being used at the beginning of address fields, please correct"))
                .count();
    }

    private Address buildAddress(String line1, String line2, String county, String town) {
        return Address.builder().line1(line1).line2(line2).county(county).town(town).build();
    }

    @ParameterizedTest
    @CsvSource({"childSupport,Child Support", "taxCredit,Tax Credit", "guardiansAllowance,Guardians Allowance",
        "taxFreeChildcare,Tax-Free Childcare", "homeResponsibilitiesProtection,Home Responsibilities Protection",
        "childBenefit,Child Benefit","thirtyHoursFreeChildcare,30 Hours Free Childcare",
        "guaranteedMinimumPension,Guaranteed Minimum Pension (COEG)","nationalInsuranceCredits,National Insurance Credits"})
    void givenACaseAppellantConfidentialityYes_thenCaseConfidentialYes(String shortName, String benefitDescription) {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setConfidentialityRequired(YES);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setDescription(benefitDescription);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YES, response.getData().getIsConfidentialCase());
    }

    @ParameterizedTest
    @CsvSource({"childSupport,Child Support", "taxCredit,Tax Credit", "guardiansAllowance,Guardians Allowance",
        "taxFreeChildcare,Tax-Free Childcare", "homeResponsibilitiesProtection,Home Responsibilities Protection",
        "childBenefit,Child Benefit","thirtyHoursFreeChildcare,30 Hours Free Childcare",
        "guaranteedMinimumPension,Guaranteed Minimum Pension (COEG)","nationalInsuranceCredits,National Insurance Credits"})
    void givenACaseAppellantConfidentialityNo_thenCaseConfidentialNull(String shortName, String benefitDescription) {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setConfidentialityRequired(NO);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setDescription(benefitDescription);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getIsConfidentialCase());
    }

    @ParameterizedTest
    @CsvSource({"childSupport,Child Support", "taxCredit,Tax Credit", "guardiansAllowance,Guardians Allowance",
        "taxFreeChildcare,Tax-Free Childcare", "homeResponsibilitiesProtection,Home Responsibilities Protection",
        "childBenefit,Child Benefit","thirtyHoursFreeChildcare,30 Hours Free Childcare",
        "guaranteedMinimumPension,Guaranteed Minimum Pension","nationalInsuranceCredits,National Insurance Credits"})
    void givenACaseAppellantConfidentialityNoOtherPartyYes_thenCaseConfidentialYes(String shortName, String benefitDescription) {
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setConfidentialityRequired(NO);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setDescription(benefitDescription);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().confidentialityRequired(YES).build()).build();
        otherPartyList.add(ccdValue);
        callback.getCaseDetails().getCaseData().setOtherParties(otherPartyList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YES, response.getData().getIsConfidentialCase());
    }

    @ParameterizedTest
    @CsvSource({"childSupport,Child Support", "taxCredit,Tax Credit", "guardiansAllowance,Guardians Allowance",
        "taxFreeChildcare,Tax-Free Childcare", "homeResponsibilitiesProtection,Home Responsibilities Protection",
        "childBenefit,Child Benefit","thirtyHoursFreeChildcare,30 Hours Free Childcare",
        "guaranteedMinimumPension,Guaranteed Minimum Pension","nationalInsuranceCredits,National Insurance Credits"})
    void givenACaseOtherPartyConfidentialityYes_thenCaseConfidentialYes(String shortName, String benefitDescription) {
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setDescription(benefitDescription);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().confidentialityRequired(YES).build()).build();
        otherPartyList.add(ccdValue);
        callback.getCaseDetails().getCaseData().setOtherParties(otherPartyList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YES, response.getData().getIsConfidentialCase());
    }

    @ParameterizedTest
    @CsvSource({"childSupport,Child Support", "taxCredit,Tax Credit", "guardiansAllowance,Guardians Allowance",
        "taxFreeChildcare,Tax-Free Childcare", "homeResponsibilitiesProtection,Home Responsibilities Protection",
        "childBenefit,Child Benefit","thirtyHoursFreeChildcare,30 Hours Free Childcare",
        "guaranteedMinimumPension,Guaranteed Minimum Pension","nationalInsuranceCredits,National Insurance Credits"})
    void givenACaseOtherPartyConfidentialityNo_thenCaseConfidentialNull(String shortName, String benefitDescription) {
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setDescription(benefitDescription);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().confidentialityRequired(NO).build()).build();
        otherPartyList.add(ccdValue);
        callback.getCaseDetails().getCaseData().setOtherParties(otherPartyList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getIsConfidentialCase());
    }

    @ParameterizedTest
    @CsvSource({"childSupport,Child Support", "taxCredit,Tax Credit", "guardiansAllowance,Guardians Allowance",
        "taxFreeChildcare,Tax-Free Childcare", "homeResponsibilitiesProtection,Home Responsibilities Protection",
        "childBenefit,Child Benefit","thirtyHoursFreeChildcare,30 Hours Free Childcare",
        "guaranteedMinimumPension,Guaranteed Minimum Pension","nationalInsuranceCredits,National Insurance Credits"})
    void givenACaseOtherPartyConfidentialityNoAndYes_thenCaseConfidentialYes(String shortName, String benefitDescription) {
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setDescription(benefitDescription);
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
    void givenNewAppellantName_thenSetCaseName() {
        sscsCaseDataBefore.getCaseAccessManagementFields().setCaseNameHmctsInternal("Old Name");
        sscsCaseData.getCaseAccessManagementFields().setCaseNameHmctsInternal("Old Name");

        BenefitType benefitType = BenefitType.builder().code("UC").description("Universal credit").build();
        var appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(benefitType);
        appeal.getAppellant().setName(new Name("", "New", "Name"));
        appeal.getAppellant().getAddress().setPostcode("Postcode");
        appeal.getAppellant().setIdentity(new Identity("1", "Nino"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("New Name", response.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertEquals("New Name", response.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertEquals("New Name", response.getData().getCaseAccessManagementFields().getCaseNamePublic());
    }

    @Test
    void givenAppellantNameAdded_thenSetCaseName() {
        BenefitType benefitType = BenefitType.builder().code("UC").description("Universal credit").build();
        var appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(benefitType);
        appeal.getAppellant().setName(new Name("", "New", "Name"));
        appeal.getAppellant().getAddress().setPostcode("Postcode");
        appeal.getAppellant().setIdentity(new Identity("1", "Nino"));

        sscsCaseData.setBenefitCode("001");
        sscsCaseData.setIssueCode("DD");

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("New Name", response.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertEquals("New Name", response.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertEquals("New Name", response.getData().getCaseAccessManagementFields().getCaseNamePublic());
    }

    @Test
    void givenAppellantNameDeleted_thenUnsetCaseName() {
        sscsCaseDataBefore.getCaseAccessManagementFields().setCaseNameHmctsInternal("Old Name");
        sscsCaseData.getCaseAccessManagementFields().setCaseNameHmctsInternal("Old Name");
        sscsCaseData.getAppeal().getAppellant().setName(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertNull(response.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertNull(response.getData().getCaseAccessManagementFields().getCaseNamePublic());
    }

    @Test
    void givenOldCaseNameExists_shouldStillSetNewCaseName() {
        sscsCaseDataBefore.getCaseAccessManagementFields().setCaseNameHmctsInternal("Harvey Specter");
        BenefitType benefitType = BenefitType.builder().code("UC").description("Universal credit").build();
        var appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(benefitType);
        appeal.getAppellant().setName(new Name("", "Louis", "Litt"));
        appeal.getAppellant().getAddress().setPostcode("Postcode");
        appeal.getAppellant().setIdentity(new Identity("1", "Nino"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData()
            .getCaseAccessManagementFields()
            .getCaseNameHmctsInternal(), is("Louis Litt"));
    }

    @Test
    void givenCaseAccessManagementFeatureDisabled_shouldNotSetCaseNames() {
        ReflectionTestUtils.setField(handler, "caseAccessManagementFeature", false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertNull(response.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertNull(response.getData().getCaseAccessManagementFields().getCaseNamePublic());
    }

    @Test
    void givenBenefitTypeChanged_thenSetCaseCategories() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        BenefitType benefitTypeBefore = BenefitType.builder().code("ESA").description("Employment and Support Allowance").build();
        sscsCaseDataBefore.getAppeal().setBenefitType(benefitTypeBefore);
        sscsCaseDataBefore.getCaseAccessManagementFields().setCategories(Benefit.ESA);

        BenefitType benefitType = BenefitType.builder().code("UC").description("Universal credit").build();
        var appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(benefitType);
        appeal.getAppellant().setName(new Name("", "New", "Name"));
        appeal.getAppellant().getAddress().setPostcode("Postcode");
        appeal.getAppellant().setIdentity(new Identity("1", "Nino"));
        sscsCaseData.getCaseAccessManagementFields().setCategories(Benefit.ESA);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("universalCredit", response.getData().getCaseAccessManagementFields().getCaseAccessCategory());
        assertEquals("Universal Credit", response.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
        assertEquals("UC", response.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
    }

    @Test
    void givenDeleteBenefitType_thenAddError() {
        sscsCaseDataBefore.getCaseAccessManagementFields().setCategories(Benefit.ESA);
        sscsCaseDataBefore = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().address(Address.builder().postcode("CM120NS").line1("123 Street").build()).build()).build())
                .build();

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);

        BenefitType benefitType = BenefitType.builder().code("").description("").build();
        sscsCaseData.setAppeal(Appeal.builder()
                .benefitType(benefitType)
                .appellant(Appellant.builder()
                        .name(Name.builder().firstName("New").lastName("Name").build())
                        .address(Address.builder().line1("Line 1").line2("Line 2").postcode("TS1 1ST").build())
                        .identity(Identity.builder().dob("1").nino("Nino").build())
                        .build())
                .build());
        sscsCaseData.getCaseAccessManagementFields().setCategories(Benefit.ESA);
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(new BenefitType("", "", null));
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setIdentity(new Identity("1", "Nino"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
    }

    @Test
    void givenNoBenefitType_thenAddWarning() {
        Address address = Address.builder().line1("123 Street").postcode("CM120NS").build();
        Appellant appellant = Appellant.builder().address(address).build();
        sscsCaseDataBefore = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .appellant(appellant).build())
                .build();

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);

        BenefitType benefitType = BenefitType.builder().code("").description("").build();
        var appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(benefitType);
        appeal.getAppellant().setName(new Name("", "New", "Name"));
        appeal.getAppellant().getAddress().setPostcode("Postcode");
        appeal.getAppellant().setIdentity(new Identity("1", "Nino"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getWarnings().size());
    }

    @Test
    void givenNoBenefitTypeBeforeAddCode_thenSetCategories() {
        sscsCaseDataBefore = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .appellant(Appellant.builder().address(Address.builder().postcode("CM120NS").build()).build()).build())
                .build();

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(new BenefitType("PIP", "", null));
        callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setIdentity(new Identity("1", "Nino"));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("personalIndependencePayment", response.getData().getCaseAccessManagementFields().getCaseAccessCategory());
        assertEquals("Personal Independence Payment", response.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
        assertEquals("PIP", response.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
    }

    @Test
    void givenInvalidBenefitType_thenAddError() {
        sscsCaseDataBefore = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .appellant(Appellant.builder().address(Address.builder().line1("123 Street").postcode("CM120NS").build()).build()).build())
                .build();

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);

        var appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(new BenefitType("turnip", null, null));
        appeal.getAppellant().setName(new Name("", "New", "Name"));
        appeal.getAppellant().getAddress().setPostcode("CM120NS");
        appeal.getAppellant().setIdentity(new Identity("1", "Nino"));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
    }

    @Test
    void givenCaseAccessManagementFeatureDisabled_shouldNotSetCaseCategories() {
        ReflectionTestUtils.setField(handler, "caseAccessManagementFeature", false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
        assertNull(response.getData().getCaseAccessManagementFields().getCaseAccessCategory());
        assertNull(response.getData().getCaseAccessManagementFields().getCaseManagementCategory());
    }

    @ParameterizedTest
    @CsvSource({"childSupport,Child Support", "taxCredit,Tax Credit", "guardiansAllowance,Guardians Allowance",
        "taxFreeChildcare,Tax-Free Childcare", "homeResponsibilitiesProtection,Home Responsibilities Protection",
        "childBenefit,Child Benefit","thirtyHoursFreeChildcare,30 Hours Free Childcare","guaranteedMinimumPension,Guaranteed Minimum Pension (COEG)",
        "nationalInsuranceCredits,National Insurance Credits"})
    void givenNonSscs1PaperCaseAppellantWantsToAttendYes_thenCaseIsOralAndWarningShown(String shortName, String description) {
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setDescription(description);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        otherPartyList.add(buildOtherParty("No",null));
        otherPartyList.add(buildOtherParty("No", NO));
        callback.getCaseDetails().getCaseData().setOtherParties(otherPartyList);
        callback.getCaseDetails().getCaseData().getAppeal().setHearingType(HearingType.PAPER.getValue());
        callback.getCaseDetails().getCaseData().getAppeal().setHearingOptions(
            HearingOptions.builder().wantsToAttend("Yes").build()
        );

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getWarnings().size());
        assertTrue(response.getWarnings().stream().anyMatch(m -> m.contains(
            "The hearing type will be changed from Paper to Oral as at least one of the"
            + " parties to the case would like to attend the hearing")));
        assertEquals(HearingType.ORAL.getValue(), response.getData().getAppeal().getHearingType());
    }

    @Test
    void givenNonSscs1PaperCaseAppellantWantsToAttendYesCaseLoader_thenCaseIsOralAndNoWarningShown() {
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode("childSupport");
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        otherPartyList.add(buildOtherParty("No",null));
        otherPartyList.add(buildOtherParty("No", NO));
        callback.getCaseDetails().getCaseData().setOtherParties(otherPartyList);
        callback.getCaseDetails().getCaseData().getAppeal().setHearingType(HearingType.PAPER.getValue());
        callback.getCaseDetails().getCaseData().getAppeal().setHearingOptions(
            HearingOptions.builder().wantsToAttend("Yes").build()
        );
        when(idamService.getUserDetails(any())).thenReturn(UserDetails.builder().roles(List.of("caseworker-sscs-systemupdate")).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(HearingType.ORAL.getValue(), response.getData().getAppeal().getHearingType());
    }

    @ParameterizedTest
    @CsvSource({"childSupport,Child Support", "taxCredit,Tax Credit", "guardiansAllowance,Guardians Allowance",
        "taxFreeChildcare,Tax-Free Childcare", "homeResponsibilitiesProtection,Home Responsibilities Protection",
        "childBenefit,Child Benefit","thirtyHoursFreeChildcare,30 Hours Free Childcare","guaranteedMinimumPension,Guaranteed Minimum Pension (COEG)",
        "nationalInsuranceCredits,National Insurance Credits"})
    void givenNonSscs1PaperCaseAppelllantWantsToAttendNo_thenCaseIsNotChangedAndNoWarningShown(String shortName, String description) {
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode(shortName);
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setDescription(description);
        callback.getCaseDetails().getCaseData().getAppeal().setHearingOptions(
            HearingOptions.builder().wantsToAttend("No").build()
        );
        callback.getCaseDetails().getCaseData().getAppeal().setHearingType(HearingType.PAPER.getValue());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(HearingType.PAPER.getValue(), response.getData().getAppeal().getHearingType());
    }

    @ParameterizedTest
    @CsvSource({"paper,Yes", "oral,Yes", "online,No"})
    void givenSscs1CaseOtherPartyWantsToAttendYes_thenHearingTypeNotChangedAndNoWarningShown(
        String hearingType, String appellantWantsToAttend) {
        callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().setCode("PIP");
        callback.getCaseDetails().getCaseData().getAppeal().setHearingType(hearingType);
        callback.getCaseDetails().getCaseData().getAppeal().setHearingOptions(
            HearingOptions.builder().wantsToAttend(appellantWantsToAttend).build()
        );

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(hearingType, response.getData().getAppeal().getHearingType());
    }

    private CcdValue<OtherParty> buildOtherParty(String wantsToAttend, YesNo confidentiality) {
        return CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .confidentialityRequired(confidentiality != null ? confidentiality : NO)
            .hearingOptions(HearingOptions.builder().wantsToAttend(wantsToAttend).build())
            .build()).build();
    }

    @ParameterizedTest
    @CsvSource({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    void givenSscs5CaseAndCaseCodeIsSetToSscs5Code_thenNoErrorIsShown(String sscs5BenefitCode) {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("taxFreeChildcare").description("Tax Credit").build());
        sscsCaseData.setBenefitCode(sscs5BenefitCode);
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @ParameterizedTest
    @CsvSource({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    void givenSscs5CaseAndCaseCodeIsChangedToNonSscs5_thenShowError(String sscs5BenefitCode) {
        when(idamService.getUserDetails(anyString())).thenReturn(UserDetails.builder().roles(List.of(CTSC_CLERK.getValue())).build());
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        sscsCaseData.getAppeal().setBenefitType(
            BenefitType.builder().code("homeResponsibilitiesProtection")
                .description("Home Responsibilities Protection").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode(sscs5BenefitCode);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertEquals("Benefit code cannot be changed to the selected code",
            response.getErrors().stream().findFirst().orElse(""));
    }

    @ParameterizedTest
    @CsvSource({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    void givenSscs5CaseAndCaseCodeIsChangedToNonSscs5SuperUser_thenShowError(String sscs5BenefitCode) {
        when(idamService.getUserDetails(anyString())).thenReturn(UserDetails.builder().roles(List.of(SUPER_USER.getValue())).build());
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        sscsCaseData.getAppeal().setBenefitType(
                BenefitType.builder().code("homeResponsibilitiesProtection")
                        .description("Home Responsibilities Protection").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode(sscs5BenefitCode);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertEquals("Benefit code cannot be changed to the selected code",
                response.getWarnings().stream().findFirst().orElse(""));
    }

    @ParameterizedTest
    @CsvSource({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    void givenNonSscs5CaseAndCaseCodeIsSetToSscs5Code_thenErrorIsShown(String sscs5BenefitCode) {
        when(idamService.getUserDetails(anyString())).thenReturn(UserDetails.builder().roles(List.of(CTSC_CLERK.getValue())).build());
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("PIP").description("test").build());
        sscsCaseData.setBenefitCode(sscs5BenefitCode);
        sscsCaseDataBefore.setBenefitCode("002");

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertEquals("Benefit code cannot be changed to the selected code",
            response.getErrors().stream().findFirst().orElse(""));
    }

    @ParameterizedTest
    @CsvSource({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    void givenNonSscs5CaseAndCaseCodeIsSetToSscs5CodeSuperUser_thenErrorIsShown(String sscs5BenefitCode) {
        when(idamService.getUserDetails(anyString())).thenReturn(UserDetails.builder().roles(List.of(SUPER_USER.getValue())).build());
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("PIP").description("test").build());
        sscsCaseData.setBenefitCode(sscs5BenefitCode);
        sscsCaseDataBefore.setBenefitCode("002");

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertEquals("Benefit code cannot be changed to the selected code",
                response.getWarnings().stream().findFirst().orElse(""));
    }

    @ParameterizedTest
    @CsvSource({
        "guaranteedMinimumPension,Guaranteed Minimum Pension (COEG),054,0",
        "nationalInsuranceCredits,Bereavement Benefit,test,2",
        "socialFund,30 Hours Free Childcare,002,1",
        "childSupport,Child Support,002,0"
    })
    void givenSscs5CaseBenefitCodeAndDescription_thenErrorIsShownForInvalidSet(String code, String description,
                                                                               String benefitCode, int error) {
        when(idamService.getUserDetails(anyString())).thenReturn(UserDetails.builder().roles(List.of(CTSC_CLERK.getValue())).build());
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(code).description(description).build());
        sscsCaseData.setBenefitCode(benefitCode);
        sscsCaseDataBefore.setBenefitCode(benefitCode);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(error));
        assertThat(response.getWarnings().size(), is(0));
        if (error > 0) {
            assertTrue(response.getErrors().stream().anyMatch(e -> e.equals("Benefit type cannot be changed to the selected type")));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "guaranteedMinimumPension,Guaranteed Minimum Pension (COEG),054,0,0",
        "nationalInsuranceCredits,Bereavement Benefit,test,0,2",
        "socialFund,30 Hours Free Childcare,002,0,1",
        "childSupport,Child Support,002,0,0"
    })
    void givenSscs5CaseBenefitCodeAndDescriptionSuperUser_thenErrorIsShownForInvalidSet(String code, String description,
                                                                                        String benefitCode, int error, int warnings) {
        when(idamService.getUserDetails(anyString())).thenReturn(UserDetails.builder().roles(List.of(SUPER_USER.getValue())).build());
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(code).description(description).build());
        sscsCaseData.setBenefitCode(benefitCode);
        sscsCaseDataBefore.setBenefitCode(benefitCode);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(error));
        assertThat(response.getWarnings().size(), is(warnings));
        if (warnings > 0) {
            assertTrue(response.getWarnings().stream().anyMatch(e -> e.equals("Benefit type cannot be changed to the selected type")));
        }
    }

    @Test
    public void givenAnyCaseAndLanguageIsNotSet_thenSetTheLanguageFieldToEmpty() {
        Appeal appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.getBenefitType().setCode("PIP");
        appeal.setHearingType("paper");
        appeal.setHearingOptions(HearingOptions.builder().wantsToAttend("Yes").languages(null).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertNull(appeal.getHearingOptions().getLanguages());
    }

    @Test
    void givenNewBenefitTypeAndCodeIsSelected_thenCaseCodeShouldChange() {
        DynamicListItem item = new DynamicListItem("088", "");
        DynamicList selection = new DynamicList(item, null);
        sscsCaseData.getAppeal().getBenefitType().setDescriptionSelection(selection);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertTrue(response.getErrors().isEmpty());
        SscsCaseData caseData = response.getData();
        assertEquals("088", caseData.getBenefitCode());
        assertEquals("DD", caseData.getIssueCode());
        assertEquals("088DD", caseData.getCaseCode());
    }

    @Test
    public void givenInvalidIssueBenefitCode_thenThrowError() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(categoryMapService.getSessionCategory(anyString(), anyString(), anyBoolean(), anyBoolean()))
            .thenReturn(null);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
    }

    @Test
    public void givenInvalidHearingVideoEmail_thenValidateAndReturnErrorMessage() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        HearingSubtype hearingSubType = HearingSubtype.builder()
            .hearingVideoEmail("12345")
            .wantsHearingTypeVideo(YES.getValue())
            .build();

        sscsCaseData.getAppeal().setHearingSubtype(hearingSubType);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().stream().allMatch(e -> e.equals("Hearing video email address must be valid email address")));
    }

    @Test
    public void givenInterpreterLanguage_thenSetInterpreterLanguageFromLanguageList() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        DynamicListItem item = new DynamicListItem("ENG", "english");
        DynamicList languagesList = new DynamicList(item, null);

        HearingOptions hearingOptions = HearingOptions.builder()
            .wantsToAttend(YES.getValue())
            .languageInterpreter(YES.getValue())
            .languagesList(languagesList)
            .build();

        sscsCaseData.getAppeal().setHearingOptions(hearingOptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String language = sscsCaseData.getAppeal().getHearingOptions().getLanguages();

        assertEquals(language, item.getLabel());
    }
}
