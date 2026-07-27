package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTime;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.reference.data.model.DefaultPanelComposition;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;

@ExtendWith(MockitoExtension.class)
class HearingsAutoListMappingTest extends HearingsMappingBase {

    private SscsCaseData caseData;

    @Mock
    private PanelCompositionService panelCompositionService;

    private HearingsAutoListMapping hearingsAutoListMapping;

    @BeforeEach
    void setUp() {
        hearingsAutoListMapping = new HearingsAutoListMapping(panelCompositionService);
        caseData = SscsCaseData.builder()
            .benefitCode(BENEFIT_CODE).issueCode(ISSUE_CODE)
            .dwpResponseDate("2022-07-07")
            .appeal(Appeal.builder()
                .appellant(Appellant.builder().name(Name.builder().firstName("Appel").lastName("Lant").build()).build())
                .hearingOptions(HearingOptions.builder().wantsToAttend("Yes").build())
                .hearingSubtype(HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build())
                .build())
            .build();
    }


    @DisplayName("When there are no conditions that affect autolisting, shouldBeAutoListed returns true")
    @Test
    void testShouldBeAutoListed() throws ListingException {
        when(panelCompositionService.isBenefitIssueCodeValid(eq(BENEFIT_CODE), eq(ISSUE_CODE))).thenReturn(true);

        boolean result = hearingsAutoListMapping.shouldBeAutoListed(caseData);

        assertTrue(result);
    }

    @DisplayName("When there are no conditions that affect autolisting, shouldBeAutoListed returns true")
    @Test
    void testShouldBeAutoListedFalseWhenNullDwpResponseDate() throws ListingException {
        caseData.setDwpResponseDate(null);
        when(panelCompositionService.isBenefitIssueCodeValid(eq(BENEFIT_CODE), eq(ISSUE_CODE))).thenReturn(true);

        boolean result = hearingsAutoListMapping.shouldBeAutoListed(caseData);

        assertFalse(result);
    }

    @DisplayName("When there is a condition that affects autolisting, shouldBeAutoListed returns false")
    @Test
    void testShouldBeAutoListedFalse() throws ListingException {
        caseData.setLinkedCase(List.of(CaseLink.builder()
                .value(CaseLinkDetails.builder()
                        .caseReference("123456")
                        .build())
                .build()));

        boolean result = hearingsAutoListMapping.shouldBeAutoListed(caseData);

        assertFalse(result);
    }

    @DisplayName("When there is an additional adjournment hearing condition that affects autolisting, shouldBeAutoListed returns false")
    @Test
    void testShouldBeNotAutoListed() throws ListingException {
        Adjournment adjournment = caseData.getAdjournment();
        AdjournCaseTime adjournCaseTime = AdjournCaseTime.builder()
                .adjournCaseNextHearingSpecificTime("pm")
                .adjournCaseNextHearingFirstOnSession(List.of("firstOnSession"))
                .build();
        adjournment.setNextHearingDateType(AdjournCaseNextHearingDateType.DATE_TO_BE_FIXED);
        adjournment.setTime(adjournCaseTime);
        when(panelCompositionService.isBenefitIssueCodeValid(eq(BENEFIT_CODE), eq(ISSUE_CODE))).thenReturn(true);

        boolean result = hearingsAutoListMapping.shouldBeAutoListed(caseData);

        assertFalse(result);
    }

    @DisplayName("When override auto list is Yes, shouldBeAutoListed returns true")
    @Test
    void testShouldBeAutoListedOverride() throws ListingException {
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().autoList(YES).build());

        boolean result = hearingsAutoListMapping.shouldBeAutoListed(caseData);

        assertTrue(result);
    }

    @DisplayName("When override auto list is No, shouldBeAutoListed returns false")
    @Test
    void testShouldBeAutoListedOverrideNo() throws ListingException {
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder()
            .autoList(NO)
            .build());

        boolean result = hearingsAutoListMapping.shouldBeAutoListed(caseData);

        assertFalse(result);
    }

    @DisplayName("When appellant has a org as a representative, hasOrgRepresentative should return True")
    @Test
    void testHasOrgRepresentative() {
        caseData.getAppeal()
                .setRep(Representative.builder()
                        .hasRepresentative("Yes")
                        .organisation("test")
                        .build());

        boolean result = HearingsAutoListMapping.hasOrgRepresentative(caseData);

        assertTrue(result);
    }

    @DisplayName("When appellant and other parties dont have a org as a representative hasOrgRepresentative should return false")
    @Test
    void testHasOrgRepresentativeNoOrg() {
        boolean result = HearingsAutoListMapping.hasOrgRepresentative(caseData);

        assertFalse(result);
    }

    @DisplayName("When any other party has a rep with a name, hasOrgOtherParties should return true")
    @Test
    void testHasOrgOtherParties() {
        List<CcdValue<OtherParty>> otherParties = List.of(
                new CcdValue<OtherParty>(OtherParty.builder()
                        .rep(Representative.builder()
                                .hasRepresentative("Yes")
                                .organisation("Test")
                                .build())
                        .build()),
                new CcdValue<OtherParty>(OtherParty.builder()
                        .rep(Representative.builder()
                                .hasRepresentative("Yes")
                                .build())
                        .build()));

        boolean result = HearingsAutoListMapping.hasOrgOtherParties(otherParties);

        assertTrue(result);
    }

    @DisplayName("When no other party has a rep with a non blank name, hasOrgOtherParties should return false")
    @Test
    void testHasOrgOtherPartiesNone() {
        List<CcdValue<OtherParty>> otherParties = List.of(
                new CcdValue<OtherParty>(OtherParty.builder()
                        .rep(Representative.builder()
                                .hasRepresentative("Yes")
                                .organisation("")
                                .build())
                        .build()),
                new CcdValue<OtherParty>(OtherParty.builder()
                        .rep(Representative.builder()
                                .hasRepresentative("Yes")
                                .build())
                        .build()));

        boolean result = HearingsAutoListMapping.hasOrgOtherParties(otherParties);

        assertFalse(result);
    }

    @DisplayName("When no other party has a rep with a non blank name, hasOrgOtherParties should return false")
    @Test
    void testHasOrgOtherPartiesNull() {
        boolean result = HearingsAutoListMapping.hasOrgOtherParties(null);

        assertFalse(result);
    }

    @DisplayName("When hasRepresentative is Yes and organisation not blank isRepresentativeOrg should return True")
    @Test
    void testIsRepresentativeOrg() {
        Representative rep = Representative.builder()
                .hasRepresentative("Yes")
                .organisation("Test")
                .build();

        boolean result = HearingsAutoListMapping.isRepresentativeOrg(rep);

        assertTrue(result);
    }

    @DisplayName("When hasRepresentative is No, blank or null, isRepresentativeOrg should return False")
    @ParameterizedTest
    @ValueSource(strings = {"No"})
    @NullAndEmptySource
    void testIsRepresentativeOrgNull(String value) {
        Representative rep = Representative.builder()
                .hasRepresentative(value)
                .organisation("Test")
                .build();

        boolean result = HearingsAutoListMapping.isRepresentativeOrg(rep);

        assertFalse(result);
    }

    @DisplayName("When Organisation is null, isRepresentativeOrg should return False")
    @ParameterizedTest
    @NullAndEmptySource
    void testIsRepresentativeOrgBlankOrg(String value) {
        Representative rep = Representative.builder()
                .hasRepresentative("Yes")
                .organisation(value)
                .build();

        boolean result = HearingsAutoListMapping.isRepresentativeOrg(rep);

        assertFalse(result);
    }

    @DisplayName("When Representative is null, isRepresentativeOrg should return False")
    @Test
    void testIsRepresentativeOrgRepNull() {
        boolean result = HearingsAutoListMapping.isRepresentativeOrg(null);

        assertFalse(result);
    }

    @DisplayName("When wants to attend and PO is attending is no, isPaperCaseAndPoNotAttending return True")
    @Test
    void testIsPaperCaseAndPoNotAttending() {
        caseData.setDwpIsOfficerAttending("No");
        caseData.getAppeal().getHearingOptions().setWantsToAttend("No");

        boolean result = HearingsAutoListMapping.isPaperCase(caseData);

        assertTrue(result);
    }

    @DisplayName("When wants to attend is No and PO is attending is Yes, isPaperCaseAndPoNotAttending return True")
    @Test
    void testIsPaperCaseAndPoNotAttendingPoAttending() {
        caseData.setDwpIsOfficerAttending("Yes");
        caseData.getAppeal().getHearingOptions().setWantsToAttend("No");

        boolean result = HearingsAutoListMapping.isPaperCase(caseData);

        assertTrue(result);
    }

    @DisplayName("When wants to attend is Yes and PO is attending is No, isPaperCaseAndPoNotAttending return False")
    @Test
    void testIsPaperCaseAndPoNotAttendingNotPaper() {
        caseData.setDwpIsOfficerAttending("No");
        caseData.getAppeal().getHearingOptions().setWantsToAttend("Yes");

        boolean result = HearingsAutoListMapping.isPaperCase(caseData);

        assertFalse(result);
    }

    @DisplayName("When appellant wants to attend and PO is attending is Yes, isPaperCaseAndPoNotAttending return False")
    @Test
    void testIsPaperCaseAndPoNotAttendingPoAppellantAttending() {
        caseData.setDwpIsOfficerAttending("Yes");
        caseData.getAppeal().getHearingOptions().setWantsToAttend("Yes");

        boolean result = HearingsAutoListMapping.isPaperCase(caseData);

        assertFalse(result);
    }

    @DisplayName("When other in HearingOptions is not blank, isThereOtherComments return True")
    @Test
    void testIsThereOtherComments() {
        caseData.getAppeal().getHearingOptions().setOther("Test");

        boolean result = HearingsAutoListMapping.isThereOtherComments(caseData);

        assertTrue(result);
    }

    @DisplayName("When other in HearingOptions is blank, isThereOtherComments return False")
    @Test
    void testIsThereOtherCommentsNone() {
        boolean result = HearingsAutoListMapping.isThereOtherComments(caseData);

        assertFalse(result);
    }

    @DisplayName("hasMqpmOrFqpm should return true when default panel comp is enabled and case has medical member")
    @Test
    void hasMqpmOrFqpmWithDefaultPanelCompEnabledShouldReturnTrue() throws ListingException {
        DefaultPanelComposition panelComposition = new DefaultPanelComposition();
        panelComposition.setJohTiers(List.of("58"));
        when(panelCompositionService.getDefaultPanelComposition(any())).thenReturn(panelComposition);
        when(panelCompositionService.isBenefitIssueCodeValid(any(), any())).thenReturn(true);
        boolean result = hearingsAutoListMapping.hasMqpmOrFqpm(caseData);

        assertTrue(result);
    }

    @DisplayName("hasMqpmOrFqpm should return true when default panel comp is enabled and case has fqpm")
    @Test
    void hasMqpmOrFqpmOrFqpmWithDefaultPanelCompEnabledAndFqpmShouldReturnTrue() throws ListingException {
        DefaultPanelComposition panelComposition = new DefaultPanelComposition();
        panelComposition.setJohTiers(List.of("50"));
        when(panelCompositionService.getDefaultPanelComposition(any())).thenReturn(panelComposition);
        when(panelCompositionService.isBenefitIssueCodeValid(any(), any())).thenReturn(true);

        boolean result = hearingsAutoListMapping.hasMqpmOrFqpm(caseData);

        assertTrue(result);
    }

    @DisplayName("hasMqpmOrFqpm should return true when default panel comp is enabled and case has regional medical member")
    @Test
    void hasMqpmOrFqpmWithDefaultPanelCompEnabledAndRegionalMedicalMemberShouldReturnTrue() throws ListingException {
        DefaultPanelComposition panelComposition = new DefaultPanelComposition();
        panelComposition.setJohTiers(List.of("69"));
        when(panelCompositionService.getDefaultPanelComposition(any())).thenReturn(panelComposition);
        when(panelCompositionService.isBenefitIssueCodeValid(any(), any())).thenReturn(true);
        boolean result = hearingsAutoListMapping.hasMqpmOrFqpm(caseData);

        assertTrue(result);
    }

    @DisplayName("hasMqpmOrFqpm should return false when default panel comp is enabled and case has no roleTypes")
    @Test
    void hasMqpmOrFqpmDefaultPanelCompEnabledShouldReturnFalse() {
        when(panelCompositionService.getDefaultPanelComposition(any())).thenReturn(null);

        ListingException ex =
                assertThrows(ListingException.class,() -> hearingsAutoListMapping.hasMqpmOrFqpm(caseData));
        assertThat(ex).hasMessage("Incorrect benefit/issue code combination");
    }

    @Test
    void shouldNotAutoListIbcCase() throws ListingException {
        SscsCaseData caseData = SscsCaseData
            .builder()
            .benefitCode(Benefit.INFECTED_BLOOD_COMPENSATION.getBenefitCode())
            .build();

        assertFalse(hearingsAutoListMapping.shouldBeAutoListed(caseData));
    }
}
