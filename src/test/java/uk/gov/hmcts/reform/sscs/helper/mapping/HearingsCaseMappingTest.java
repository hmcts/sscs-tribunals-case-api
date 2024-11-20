package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.CaseCategoryType.CASE_SUBTYPE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.CaseCategoryType.CASE_TYPE;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseCategory;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;
import uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil;

class HearingsCaseMappingTest extends HearingsMappingBase {
    @Mock
    private SessionCategoryMapService sessionCategoryMaps;

    @Mock
    private ReferenceDataServiceHolder refData;

    @DisplayName("When a valid hearing wrapper is given buildHearingCaseDetails returns the correct Hearing Case Details")
    @Test
    void buildHearingCaseDetails() throws ListingException {
        // TODO Finish Test when method done

        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE,ISSUE_CODE,false,false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                    false,false,SessionCategory.CATEGORY_03,null));

        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);

        List<CcdValue<OtherParty>> otherParties = new ArrayList<>();
        otherParties.add(new CcdValue<>(OtherParty.builder()
            .hearingOptions(HearingOptions.builder().build())
            .appointee(Appointee.builder().build())
            .rep(Representative.builder().build())
            .build()));
        otherParties.add(new CcdValue<>(OtherParty.builder()
            .hearingOptions(HearingOptions.builder().build())
            .appointee(Appointee.builder().build())
            .rep(Representative.builder().build())
            .build()));
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .caseCreated(CASE_CREATED)
            .caseAccessManagementFields(CaseAccessManagementFields.builder()
                .caseNameHmctsInternal(CASE_NAME_INTERNAL)
                .caseNamePublic(CASE_NAME_PUBLIC)
                .build())
            .appeal(Appeal.builder()
                .hearingOptions(HearingOptions.builder().build())
                .appellant(Appellant.builder()
                    .appointee(Appointee.builder().build())
                    .build())
                .rep(Representative.builder().build())
                .build())
            .otherParties(otherParties)
            .caseManagementLocation(CaseManagementLocation.builder()
                .baseLocation(EPIMS_ID)
                .region(REGION)
                .build())
            .build();
        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(caseData)
            .caseData(caseData)
            .build();

        CaseDetails caseDetails = HearingsCaseMapping.buildHearingCaseDetails(wrapper, refData);

        assertNotNull(caseDetails.getCaseId());
        assertNotNull(caseDetails.getCaseDeepLink());
        assertNotNull(caseDetails.getHmctsInternalCaseName());
        assertNotNull(caseDetails.getPublicCaseName());
        assertNotNull(caseDetails.getCaseCategories());
        assertNotNull(caseDetails.getCaseManagementLocationCode());
        assertNotNull(caseDetails.getCaseSlaStartDate());
    }

    @DisplayName("getCaseID Test")
    @Test
    void getCaseID() {
        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId(String.valueOf(CASE_ID)).build();

        String result = HearingsCaseMapping.getCaseID(caseData);
        String expected = String.valueOf(CASE_ID);

        assertEquals(expected, result);
    }

    @DisplayName("When case ID is given getCaseDeepLink returns the correct link")
    @Test
    void getCaseDeepLink() {
        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(SscsCaseData.builder()
                .ccdCaseId(String.valueOf(CASE_ID))
                .build())
            .build();
        Mockito.when(refData.getExUiUrl()).thenReturn(EX_UI_URL);
        String result = HearingsCaseMapping.getCaseDeepLink(wrapper.getCaseData(), refData);
        String expected = HearingsCaseMapping.CASE_DETAILS_URL.formatted(EX_UI_URL, CASE_ID);

        assertEquals(expected, result);
    }

    @DisplayName("getInternalCaseName Test")
    @Test
    void getInternalCaseName() {
        String caseNameInternal = CASE_NAME_INTERNAL;
        SscsCaseData caseData = SscsCaseData.builder()
            .caseAccessManagementFields(CaseAccessManagementFields.builder()
                .caseNameHmctsInternal(caseNameInternal)
                .build())
            .build();

        String result = HearingsCaseMapping.getInternalCaseName(caseData);

        assertEquals(caseNameInternal, result);
    }

    @DisplayName("getPublicCaseName Test")
    @Test
    void getPublicCaseName() {
        String caseNamePublic = CASE_NAME_PUBLIC;
        SscsCaseData caseData = SscsCaseData.builder()
            .caseAccessManagementFields(CaseAccessManagementFields.builder()
                .caseNamePublic(caseNamePublic)
                .build())
            .build();

        String result = HearingsCaseMapping.getPublicCaseName(caseData);

        assertEquals(caseNamePublic, result);
    }

    @DisplayName("shouldBeAdditionalSecurityFlag Parameterized Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "Yes,YES,true",
        "Yes,NO,true",
        "No,YES,true",
        "No,NO,false",
        "null,YES,true",
        "null,NO,false",
    }, nullValues = {"null"})
    void shouldBeAdditionalSecurityFlag(String dwpUcbFlag, YesNo otherPartiesUcb, boolean expected) {
        List<CcdValue<OtherParty>> otherParties = new ArrayList<>();
        otherParties.add(CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .unacceptableCustomerBehaviour(otherPartiesUcb)
                .build())
            .build());

        SscsCaseData caseData = SscsCaseData.builder()
            .dwpUcb(dwpUcbFlag)
            .appeal(Appeal.builder()
                .build())
            .otherParties(otherParties)
            .build();
        boolean result = HearingsCaseMapping.shouldBeAdditionalSecurityFlag(caseData);

        assertEquals(expected, result);
    }

    @DisplayName("shouldBeAdditionalSecurityOtherParties when otherParties are not null Parameterized Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "YES,true",
        "NO,false",
        "null,false",
    }, nullValues = {"null"})
    void shouldBeAdditionalSecurityOtherParties(YesNo ucb, boolean expected) {
        List<CcdValue<OtherParty>> otherParties = new ArrayList<>();

        otherParties.add(CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .unacceptableCustomerBehaviour(ucb)
                .build())
            .build());
        boolean result = HearingsCaseMapping.shouldBeAdditionalSecurityOtherParties(otherParties);

        assertEquals(expected, result);
    }

    @DisplayName("shouldBeAdditionalSecurityOtherParties when otherParties are null Test")
    @Test
    void shouldBeAdditionalSecurityOtherParties() {
        boolean result = HearingsCaseMapping.shouldBeAdditionalSecurityOtherParties(null);

        assertThat(result).isFalse();
    }

    @DisplayName("shouldBeAdditionalSecurityFlag Parameterized Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "YES,Yes,Yes,true",
        "YES,Yes,No,true",
        "YES,No,Yes,true",
        "YES,No,No,true",
        "NO,Yes,Yes,true",
        "NO,Yes,No,true",
        "NO,No,Yes,true",
        "NO,No,No,false",
        "null,Yes,Yes,true",
        "null,Yes,No,true",
        "null,No,Yes,true",
        "null,No,No,false"
    }, nullValues = {"null"})
    void isInterpreterRequired(YesNo adjournCaseInterpreter, String appellantInterpreter, String otherPartyInterpreter, boolean expected) {
        List<CcdValue<OtherParty>> otherParties = new ArrayList<>();
        otherParties.add(CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .hearingOptions(HearingOptions.builder()
                    .languageInterpreter(otherPartyInterpreter)
                    .build())
                .build())
            .build());

        SscsCaseData caseData = SscsCaseData.builder()
            .adjournment(Adjournment.builder()
                .interpreterRequired(adjournCaseInterpreter)
                .build())
            .appeal(Appeal.builder()
                .hearingOptions(HearingOptions.builder()
                    .languageInterpreter(appellantInterpreter)
                    .build())
                .build())
            .otherParties(otherParties)
            .build();
        boolean result = HearingChannelUtil.isInterpreterRequired(caseData);

        assertEquals(expected, result);
    }

    @DisplayName("isInterpreterRequiredOtherParties when otherParties are not null Parameterized Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "Yes,true,true",
        "Yes,false,true",
        "No,true,true",
        "No,false,false",
    }, nullValues = {"null"})
    void isInterpreterRequiredOtherParties(String interpreter, boolean signLanguage, boolean expected) {
        List<CcdValue<OtherParty>> otherParties = new ArrayList<>();
        otherParties.add(CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .hearingOptions(HearingOptions.builder()
                    .languageInterpreter(interpreter)
                    .arrangements(signLanguage ?  List.of("signLanguageInterpreter") : null)
                    .build())
                .build())
            .build());

        boolean result = HearingChannelUtil.isInterpreterRequiredOtherParties(otherParties);

        assertEquals(expected, result);
    }

    @DisplayName("isInterpreterRequiredOtherParties when otherParties are null Test")
    @Test
    void isInterpreterRequiredOtherParties() {
        boolean result = HearingChannelUtil.isInterpreterRequiredOtherParties(null);

        assertThat(result).isFalse();
    }

    @DisplayName("isInterpreterRequiredHearingOptions Parameterized Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "Yes,signLanguageInterpreter|somethingElse,true",
        "Yes,signLanguageInterpreter,true",
        "Yes,somethingElse,true",
        "Yes,null,true",
        "Yes,,true",
        "No,signLanguageInterpreter|somethingElse,true",
        "No,signLanguageInterpreter,true",
        "No,somethingElse,false",
        "No,null,false",
        "No,,false",
        "null,signLanguageInterpreter|somethingElse,true",
        "null,signLanguageInterpreter,true",
        "null,somethingElse,false",
        "null,null,false",
        "null,,false",
        ",signLanguageInterpreter|somethingElse,true",
        ",signLanguageInterpreter,true",
        ",somethingElse,false",
        ",null,false",
        ",,false",
    }, nullValues = {"null"})
    void isInterpreterRequiredHearingOptions(String interpreter, String arrangements, boolean expected) {

        HearingOptions hearingOptions = HearingOptions.builder()
            .languageInterpreter(interpreter)
            .arrangements(nonNull(arrangements) ?  splitCsvParamArray(arrangements) : null)
            .build();
        boolean result = HearingChannelUtil.isInterpreterRequiredHearingOptions(hearingOptions);

        assertEquals(expected, result);
    }


    @DisplayName("When give a valid benefit code and issue code, buildCaseCategories returns a valid case Category and  case subcategory")
    @Test
    void buildCaseCategories() throws ListingException {
        String parentValue = "BBA3-002";
        String subTypeValue = "BBA3-002-DD";

        SessionCategoryMap sessionCategoryMap = new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                false, false, SessionCategory.CATEGORY_06, null);

        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE, ISSUE_CODE,false,false))
                .willReturn(sessionCategoryMap);
        given(sessionCategoryMaps.getCategoryTypeValue(sessionCategoryMap))
                .willReturn(parentValue);
        given(sessionCategoryMaps.getCategorySubTypeValue(sessionCategoryMap))
                .willReturn(subTypeValue);

        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(BENEFIT_CODE)
                .issueCode(ISSUE_CODE)
                .build();

        List<CaseCategory> result = HearingsCaseMapping.buildCaseCategories(caseData, refData);

        assertThat(result)
                .extracting("categoryType", "categoryValue", "categoryParent")
                .as("Case sub type categories should have a parent set.")
                .contains(tuple(CASE_TYPE, parentValue, null), tuple(CASE_SUBTYPE, subTypeValue, parentValue));
    }


    @DisplayName("When a case with a valid CaseManagementLocation is given getCaseManagementLocationCode returns the correct EPIMS ID")
    @Test
    void getCaseManagementLocationCode() {
        SscsCaseData caseData = SscsCaseData.builder()
                .caseManagementLocation(CaseManagementLocation.builder()
                .baseLocation(EPIMS_ID)
                .region(REGION)
                .build())
            .build();
        String result = HearingsCaseMapping.getCaseManagementLocationCode(caseData);

        assertEquals(EPIMS_ID, result);
    }

    @DisplayName("When a case without a CaseManagementLocation is given getCaseManagementLocationCode returns null")
    @Test
    void getCaseManagementLocationCodeWhenNull() {
        SscsCaseData caseData = SscsCaseData.builder()
            .build();
        String result = HearingsCaseMapping.getCaseManagementLocationCode(caseData);

        assertNull(result);
    }

    @DisplayName("shouldBeSensitiveFlag Test")
    @Test
    void shouldBeSensitiveFlag() {
        boolean result = HearingsCaseMapping.shouldBeSensitiveFlag();

        assertFalse(result);
    }

    @DisplayName("getCaseCreated Test")
    @Test
    void getCaseCreated() {
        String caseCreatedDate = "2022-04-01";
        SscsCaseData caseData = SscsCaseData.builder()
            .caseCreated(caseCreatedDate)
            .build();

        String result = HearingsCaseMapping.getCaseCreated(caseData);

        assertEquals(caseCreatedDate, result);
    }
}
