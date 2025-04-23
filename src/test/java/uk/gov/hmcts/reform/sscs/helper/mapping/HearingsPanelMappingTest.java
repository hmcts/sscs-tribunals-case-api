package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitCode;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.Issue;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMember;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberExclusions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SessionCategory;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsIndustrialInjuriesData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.RequirementType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.MemberType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PanelPreference;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PanelRequirements;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCategoryService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

class HearingsPanelMappingTest extends HearingsMappingBase {

    public static final String IIDB_BENEFIT_CODE = "067";
    public static final String JUDGE_JOH_CODE = "84";
    @Mock
    private SessionCategoryMapService sessionCategoryMaps;

    @Mock
    private ReferenceDataServiceHolder refData;

    @Mock
    private PanelCategoryMapService panelCategoryMapService;
    @Mock
    private PanelCategoryService panelCategoryService;

    private HearingsPanelMapping hearingsPanelMapping;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        hearingsPanelMapping = new HearingsPanelMapping(panelCategoryService);
    }

    @DisplayName("When no data is given getPanelRequirements returns the valid but empty PanelRequirements")
    @Test
    void testGetPanelRequirements() {
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);

        SscsCaseData caseData = SscsCaseData.builder().build();

        PanelRequirements result = hearingsPanelMapping.getPanelRequirements(caseData, refData);

        assertThat(result).isNotNull();
        assertThat(result.getRoleTypes()).isEmpty();
        assertThat(result.getAuthorisationTypes()).isEmpty();
        assertThat(result.getAuthorisationSubTypes()).isEmpty();
        assertThat(result.getPanelPreferences()).isEmpty();
        assertThat(result.getPanelSpecialisms()).isEmpty();
    }

    @DisplayName("getRoleTypes returns TRIBUNALS_MEMBER_MEDICAL when benefit is IIDB and feature flag is disabled")
    @Test
    void shouldReturnTribunalsMemberMedicalWhenBenefitIsIndustrialInjuriesDisablementBenefit() {
        ReflectionTestUtils.setField(hearingsPanelMapping, "defaultPanelCompEnabled", false);
        caseData.setBenefitCode(IIDB_BENEFIT_CODE);
        when(refData.getSessionCategoryMaps()).thenReturn(sessionCategoryMaps);
        PanelRequirements result = hearingsPanelMapping.getPanelRequirements(caseData, refData);
        assertThat(result.getRoleTypes()).contains(PanelMemberType.TRIBUNALS_MEMBER_MEDICAL.getReference());
    }

    @DisplayName("HearingsPanelMapping should call panelCategoryService and return role types when feature flag is enabled")
    @Test
    void shouldPopulateRoleTypesFromPanelCategoryServiceWhenFeatureFlagIsEnabled() {
        ReflectionTestUtils.setField(hearingsPanelMapping, "defaultPanelCompEnabled", true);
        when(refData.getSessionCategoryMaps()).thenReturn(sessionCategoryMaps);
        when(panelCategoryService.getRoleTypes(caseData)).thenReturn(List.of(JUDGE_JOH_CODE));
        PanelRequirements result = hearingsPanelMapping.getPanelRequirements(caseData, refData);
        assertThat(result.getRoleTypes()).isNotEmpty();
        assertThat(result.getRoleTypes()).contains(JUDGE_JOH_CODE);
    }


    @DisplayName("getAuthorisationTypes returns an empty list")
    @Test
    void testGetAuthorisationTypes() {
        List<String> result = HearingsPanelMapping.getAuthorisationTypes();

        assertThat(result).isEmpty();
    }

    @DisplayName("getAuthorisationSubTypes returns an empty list")
    @Test
    void testGetAuthorisationSubTypes() {
        List<String> result = HearingsPanelMapping.getAuthorisationSubTypes();

        assertThat(result).isEmpty();
    }

    @DisplayName("When overrideFields are null getPanelPreferences returns an empty list")
    @Test
    void testGetPanelPreferencesOverrideFieldsNull() {
        List<PanelPreference> result = HearingsPanelMapping.getPanelPreferences(caseData);
        assertThat(result).isEmpty();
    }

    @DisplayName("When adjournment is enabled, "
        + " all the 3 panel members are provided "
        + " and panel member requirement type is null"
        + " then panel preferences should be not provided.")
    @Test
    void testPanelMembersExcludedIsNull() {
        caseData.setAdjournment(Adjournment.builder()
            .panelMembersExcluded(null)
            .panelMember1(JudicialUserBase.builder()
                .idamId("1")
                .personalCode("TOM")
                .build())
            .panelMember2(JudicialUserBase.builder()
                .idamId("2").personalCode("JERRY")
                .build())
            .panelMember3(JudicialUserBase.builder()
                .idamId("3").personalCode("Tyke")
                .build())
            .build());

        List<PanelPreference> result = HearingsPanelMapping.getPanelPreferences(caseData);
        assertThat(result).isNullOrEmpty();
    }

    @DisplayName("When panel members are provided"
        + " and panel member requirement type is RESERVED"
        + " then return panel member requirement type as MUST_INCLUDE.")
    @Test
    void testPanelMembersReserved() {
        CollectionItem<JudicialUserBase> reservedMember = CollectionItem.<JudicialUserBase>builder()
            .value(JudicialUserBase.builder().idamId("1").personalCode("TOM").build())
            .build();
        List<CollectionItem<JudicialUserBase>> reservedMembers = List.of(reservedMember);
        PanelMemberExclusions panelMembers = PanelMemberExclusions.builder()
            .arePanelMembersReserved(YesNo.YES)
            .reservedPanelMembers(reservedMembers)
            .build();
        caseData.getSchedulingAndListingFields().setPanelMemberExclusions(panelMembers);

        List<PanelPreference> result = HearingsPanelMapping.getPanelPreferences(caseData);
        assertThat(result).isNotEmpty().size().isEqualTo(1);
        assertThat(result.get(0).getRequirementType()).isEqualTo(RequirementType.MUST_INCLUDE);
    }

    @DisplayName("When panel members are provided "
        + " and panel member requirement type is YES"
        + " then return panel member requirement type as EXCLUDE.")
    @Test
    void testPanelMembersExcludedIsYes() {
        CollectionItem<JudicialUserBase> excludedMember = CollectionItem.<JudicialUserBase>builder()
            .value(JudicialUserBase.builder().idamId("1").personalCode("TOM").build())
            .build();
        List<CollectionItem<JudicialUserBase>> excludedMembers = List.of(excludedMember);
        PanelMemberExclusions panelMembers = PanelMemberExclusions.builder()
            .arePanelMembersExcluded(YesNo.YES)
            .excludedPanelMembers(excludedMembers)
            .build();
        caseData.getSchedulingAndListingFields().setPanelMemberExclusions(panelMembers);

        List<PanelPreference> result = HearingsPanelMapping.getPanelPreferences(caseData);
        assertThat(result).isNotEmpty().size().isEqualTo(1);
        assertThat(result.get(0).getRequirementType()).isEqualTo(RequirementType.EXCLUDE);
    }

    @DisplayName("Member type for hmc should be JOH")
    @Test
    void testMemberTypeShouldBeJoh() {
        JudicialUserBase judge = JudicialUserBase.builder().idamId("1").personalCode("TOM").build();
        CollectionItem<JudicialUserBase> excludedMember = CollectionItem.<JudicialUserBase>builder()
            .value(judge)
            .build();
        List<CollectionItem<JudicialUserBase>> excludedMembers = List.of(excludedMember);
        PanelMemberExclusions panelMembers = PanelMemberExclusions.builder()
            .arePanelMembersExcluded(YesNo.YES)
            .excludedPanelMembers(excludedMembers)
            .build();
        caseData.getSchedulingAndListingFields().setPanelMemberExclusions(panelMembers);

        List<PanelPreference> result = HearingsPanelMapping.getPanelPreferences(caseData);
        assertThat(result).isNotEmpty().size().isEqualTo(1);
        assertThat(result.get(0).getRequirementType()).isEqualTo(RequirementType.EXCLUDE);
        assertThat(result.get(0).getMemberType()).isEqualTo(MemberType.JOH);
    }

    @DisplayName("When no panel members details are provided "
        + " then return empty panel members detail.")
    @Test
    void testGetPanelPreferencesWhenPanelMemberNotProvide() {
        List<PanelPreference> result = HearingsPanelMapping.getPanelPreferences(caseData);
        assertThat(result).isEmpty();
    }

    @DisplayName("When a case is given with a second doctor getPanelRequirements returns the valid PanelRequirements")
    @ParameterizedTest
    @CsvSource(value = {
        "cardiologist,eyeSurgeon,1|3",
        "null,carer,2",
    }, nullValues = {"null"})
    void testGetPanelSpecialisms(String doctorSpecialism, String doctorSpecialismSecond, String expected) {
        SessionCategoryMap sessionCategoryMap = new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
            true, false, SessionCategory.CATEGORY_06, null
        );

        SscsCaseData caseData = SscsCaseData.builder()
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .sscsIndustrialInjuriesData(SscsIndustrialInjuriesData.builder()
                .panelDoctorSpecialism(doctorSpecialism)
                .secondPanelDoctorSpecialism(doctorSpecialismSecond)
                .build())
            .build();

        List<String> result = HearingsPanelMapping.getPanelSpecialisms(caseData, sessionCategoryMap);

        List<String> expectedList = splitCsvParamArray(expected);
        assertThat(result)
            .containsExactlyInAnyOrderElementsOf(expectedList);

    }

    @DisplayName("When a case is given with no second doctor getPanelRequirements returns the valid PanelRequirements")
    @ParameterizedTest
    @CsvSource(value = {
        "generalPractitioner,4",
    }, nullValues = {"null"})
    void testGetPanelSpecialisms(String doctorSpecialism, String expected) {

        SessionCategoryMap sessionCategoryMap = new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
            false, false, SessionCategory.CATEGORY_05, null
        );

        SscsCaseData caseData = SscsCaseData.builder()
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .sscsIndustrialInjuriesData(SscsIndustrialInjuriesData.builder()
                .panelDoctorSpecialism(doctorSpecialism)
                .build())
            .build();

        List<String> result = HearingsPanelMapping.getPanelSpecialisms(caseData, sessionCategoryMap);

        List<String> expectedList = splitCsvParamArray(expected);
        assertThat(result)
            .containsExactlyInAnyOrderElementsOf(expectedList);

    }

    @DisplayName("When an case has a null doctor specialism return an empty list.")
    @Test
    void testWhenAnCaseHasAnNullDoctorSpecialismReturnAnEmptyList() {

        SessionCategoryMap sessionCategoryMap = new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
            false, false, SessionCategory.CATEGORY_05, null
        );

        SscsCaseData caseData = SscsCaseData.builder()
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .sscsIndustrialInjuriesData(SscsIndustrialInjuriesData.builder()
                .panelDoctorSpecialism("doesntexist")
                .build())
            .build();

        List<String> result = HearingsPanelMapping.getPanelSpecialisms(caseData, sessionCategoryMap);

        List<String> expectedList = Collections.emptyList();
        assertThat(result)
            .containsExactlyInAnyOrderElementsOf(expectedList);

    }

    @DisplayName("When a case benefit is CHILD_SUPPORT then return empty list.")
    @Test
    void testWhenAnCaseBenefitChildSupportReturnAnEmptyList() {

        SessionCategoryMap sessionCategoryMap = new SessionCategoryMap(BenefitCode.CHILD_SUPPORT_ASSESSMENTS, Issue.DD,
                                                                       false, false, SessionCategory.CATEGORY_05, null
        );

        SscsCaseData caseData = SscsCaseData.builder()
            .benefitCode(CHILD_SUPPORT_BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .sscsIndustrialInjuriesData(SscsIndustrialInjuriesData.builder()
                                            .panelDoctorSpecialism("doesntexist")
                                            .build())
            .build();

        List<String> result = HearingsPanelMapping.getPanelSpecialisms(caseData, sessionCategoryMap);

        List<String> expectedList = Collections.emptyList();
        assertThat(result)
            .containsExactlyInAnyOrderElementsOf(expectedList);

    }


    @DisplayName("When a non doctor panel member is given getPanelMemberSpecialism returns the valid reference")
    @ParameterizedTest
    @EnumSource(
        value = PanelMember.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = {"FQPM", "MQPM1", "MQPM2"}
    )
    void testGetPanelMemberSpecialism(PanelMember value) {
        // added FQPM to the exclude list because LA is not currently set up to handle specialism for this type.
        String result = HearingsPanelMapping.getPanelMemberSpecialism(value, null, null);

        assertThat(result).isEqualTo(value.getReference());
    }

    @DisplayName("When the Panel Member is type FQPM, then return null for specialism")
    @Test
    void testGivenFqpmPanelMemberThenReturnNullForSpecialism() {
        // LA is not currently set up to handle specialism for FQPM panel members.
        String result = HearingsPanelMapping.getPanelMemberSpecialism(PanelMember.FQPM, null, null);
        assertThat(result).isNull();
    }

}
