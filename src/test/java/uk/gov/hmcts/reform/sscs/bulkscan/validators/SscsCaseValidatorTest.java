package uk.gov.hmcts.reform.sscs.bulkscan.validators;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.CHILD_MAINTENANCE_NUMBER;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.OTHER_PARTY_ADDRESS_LINE1;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.OTHER_PARTY_ADDRESS_LINE2;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.OTHER_PARTY_ADDRESS_LINE3;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.OTHER_PARTY_POSTCODE;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.APPEAL_GROUNDS;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.BENEFIT_TYPE_DESCRIPTION;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HAS_REPRESENTATIVE_FIELD_MISSING;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TYPE_FACE_TO_FACE_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TYPE_ORAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TYPE_PAPER;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TYPE_TELEPHONE_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TYPE_VIDEO_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IBC_ROLE_FOR_DECEASED;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IBC_ROLE_FOR_LACKING_CAPACITY;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IBC_ROLE_FOR_POA;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IBC_ROLE_FOR_SELF;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IBC_ROLE_FOR_U18;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IS_EMPTY;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IS_MISSING;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.NO_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.PORT_OF_ENTRY_INVALID_ERROR;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.ATTENDANCE_ALLOWANCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.BEREAVEMENT_BENEFIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.BEREAVEMENT_SUPPORT_PAYMENT_SCHEME;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CARERS_ALLOWANCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.DLA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.ESA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.IIDB;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.INCOME_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.INDUSTRIAL_DEATH_BENEFIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.INFECTED_BLOOD_COMPENSATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.JSA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.MATERNITY_ALLOWANCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PENSION_CREDIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.RETIREMENT_PENSION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.SOCIAL_FUND;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.OcrDataField;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ScannedData;
import uk.gov.hmcts.reform.sscs.bulkscan.json.SscsJsonExtractor;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasonDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasons;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.Role;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.domain.CaseResponse;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

public class SscsCaseValidatorTest {

    private static final String VALID_MOBILE = "07832882849";
    private static final String VALID_POSTCODE = "CM13 0GD";
    private static final String PORT_OF_NORWICH_A_FINE_CITY = "GBSTGTY00";
    private final List<String> titles = new ArrayList<>();
    private final Map<String, Object> ocrCaseData = new HashMap<>();
    private final List<OcrDataField> ocrList = new ArrayList<>();
    @Mock
    RegionalProcessingCenterService regionalProcessingCenterService;
    @Mock
    SscsJsonExtractor sscsJsonExtractor;
    DwpAddressLookupService dwpAddressLookupService;
    @Mock
    private PostcodeValidator postcodeValidator;
    private SscsCaseValidator validator;
    private MrnDetails defaultMrnDetails;
    private CaseResponse transformResponse;
    private CaseDetails caseDetails;
    private ScannedData scannedData;
    private ExceptionRecord exceptionRecord;

    private ExceptionRecord exceptionRecordSscs1U;
    private ExceptionRecord exceptionRecordSscs2;
    private ExceptionRecord exceptionRecordSscs5;

    @BeforeEach
    public void setup() {
        openMocks(this);
        dwpAddressLookupService = new DwpAddressLookupService();
        scannedData = mock(ScannedData.class);
        caseDetails = mock(CaseDetails.class);
        validator = new SscsCaseValidator(regionalProcessingCenterService, dwpAddressLookupService, postcodeValidator,
            sscsJsonExtractor, false);
        transformResponse = CaseResponse.builder().build();

        defaultMrnDetails = MrnDetails.builder().dwpIssuingOffice("2").mrnDate("2018-12-09").build();

        titles.add("Mr");
        titles.add("Mrs");
        ReflectionTestUtils.setField(validator, "titles", titles);
        ocrCaseData.put("person1_address_line4", "county");
        ocrCaseData.put("person2_address_line4", "county");
        ocrCaseData.put("representative_address_line4", "county");
        ocrCaseData.put("office", "2");
        ocrCaseData.put("appeal_grounds", "True");

        given(regionalProcessingCenterService.getByPostcode(eq(VALID_POSTCODE), anyBoolean()))
            .willReturn(RegionalProcessingCenter.builder().address1("Address 1").name("Liverpool").build());

        given(regionalProcessingCenterService.getByPostcode(eq(PORT_OF_NORWICH_A_FINE_CITY), anyBoolean()))
            .willReturn(RegionalProcessingCenter.builder().address1("Address 1").name("Bradford").build());

        exceptionRecord = ExceptionRecord.builder().ocrDataFields(ocrList).formType(FormType.SSCS1PE.getId()).build();
        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(scannedData);
        given(scannedData.getOcrCaseData()).willReturn(ocrCaseData);
        given(postcodeValidator.isValid(anyString())).willReturn(true);
        given(postcodeValidator.isValidPostcodeFormat(anyString())).willReturn(true);

        exceptionRecordSscs1U =
            ExceptionRecord.builder().ocrDataFields(ocrList).formType(FormType.SSCS1U.getId()).build();
        given(sscsJsonExtractor.extractJson(exceptionRecordSscs1U)).willReturn(scannedData);

        exceptionRecordSscs2 =
            ExceptionRecord.builder().ocrDataFields(ocrList).formType(FormType.SSCS2.getId()).build();
        given(sscsJsonExtractor.extractJson(exceptionRecordSscs2)).willReturn(scannedData);
        ocrCaseData.put("person1_child_maintenance_number", CHILD_MAINTENANCE_NUMBER);

        exceptionRecordSscs5 =
            ExceptionRecord.builder().ocrDataFields(ocrList).formType(FormType.SSCS5.getId()).build();
        given(sscsJsonExtractor.extractJson(exceptionRecordSscs5)).willReturn(scannedData);
    }

    @ParameterizedTest
    @CsvSource({"ESA", "JSA", "PIP", "DLA", "attendanceAllowance", "industrialInjuriesDisablement",
        "socialFund", "incomeSupport", "industrialDeathBenefit", "pensionCredit", "retirementPension"})
    public void givenAnAppealContainsAnInvalidOfficeForBenefitTypeOtherNotAutoOffice_thenAddAWarning(
        String benefitShortName) {
        defaultMrnDetails.setDwpIssuingOffice("Invalid Test Office");

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(benefitShortName, buildAppellant(false), true,
                FormType.SSCS1U),
            false);

        assertEquals("office is invalid", response.getWarnings().getFirst());
    }

    @ParameterizedTest
    @CsvSource({"carersAllowance", "bereavementBenefit", "maternityAllowance", "bereavementSupportPaymentScheme"})
    public void givenAnAppealContainsAnInvalidOfficeForBenefitTypeOtherAutoOffice_thenDoNotAddAWarning(
        String benefitShortName) {
        defaultMrnDetails.setDwpIssuingOffice("Invalid Test Office");

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(benefitShortName, buildAppellant(false), true,
                FormType.SSCS1U),
            false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppealContainsAnInvalidOfficeForBenefitTypeUC_ucOfficeFeatureActive_thenAddAWarning() {
        defaultMrnDetails.setDwpIssuingOffice("Invalid Test Office");
        validator.setUcOfficeFeatureActive(true);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(UC.getShortName(), buildAppellant(false), true,
                FormType.SSCS1U),
            false);

        assertEquals("office is invalid", response.getWarnings().getFirst());
    }

    @Test
    //TODO: Invalid when ucOfficeFeatureActive fully enabled, to be removed then.
    public void givenAnAppealContainsAnInvalidOfficeForBenefitTypeUC_thenDoNotAddAWarning() {
        defaultMrnDetails.setDwpIssuingOffice("Invalid Test Office");
        validator.setUcOfficeFeatureActive(false);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(UC.getShortName(), buildAppellant(false), true,
                FormType.SSCS1U),
            false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @ParameterizedTest
    @CsvSource({"The Pension Service 11", "Recovery from Estates"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeAttendanceAllowance_thenDoNotAddAWarning(
        String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse,
                exceptionRecord,
                buildMinimumAppealDataWithBenefitTypeAndFormType(ATTENDANCE_ALLOWANCE.getShortName(),
                    buildAppellant(false),
                    true, FormType.SSCS1U),
                false);

        String assertionMessage = "Asserting Benefit: Attendance Allowance with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"Disability Benefit Centre 4", "The Pension Service 11", "Recovery from Estates"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeDla_thenDoNotAddAWarning(String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(DLA.getShortName(), buildAppellant(false), true,
                FormType.SSCS1U),
            false);

        String assertionMessage = "Asserting Benefit: DLA with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"Worthing DRT", "Birkenhead DRT", "Recovery from Estates", "Inverness DRT"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeIncomeSupport_thenDoNotAddAWarning(
        String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(INCOME_SUPPORT.getShortName(), buildAppellant(false), true,
                FormType.SSCS1U),
            false);

        String assertionMessage = "Asserting Benefit: Income Support with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"Barrow IIDB Centre", "Barnsley Benefit Centre"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeIidb_thenDoNotAddAWarning(String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(IIDB.getShortName(), buildAppellant(false), true,
                FormType.SSCS1U),
            false);

        String assertionMessage = "Asserting Benefit: IIDB with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"Worthing DRT", "Birkenhead DRT", "Recovery from Estates", "Inverness DRT"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeJsa_thenDoNotAddAWarning(String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(JSA.getShortName(), buildAppellant(false), true,
                FormType.SSCS1U),
            false);

        String assertionMessage = "Asserting Benefit: JSA with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"St Helens Sure Start Maternity Grant", "Funeral Payment Dispute Resolution Team",
        "Pensions Dispute Resolution Team"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeSocialFund_thenDoNotAddAWarning(
        String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(SOCIAL_FUND.getShortName(), buildAppellant(false), true,
                FormType.SSCS1U),
            false);

        String assertionMessage = "Asserting Benefit: Social Fund with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"Barrow IIDB Centre", "Barnsley Benefit Centre"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeIndustrialDeathBenefit_thenDoNotAddAWarning(
        String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(INDUSTRIAL_DEATH_BENEFIT.getShortName(),
                buildAppellant(false), true, FormType.SSCS1U),
            false);

        String assertionMessage = "Asserting Benefit: Industrial Death Benefit with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"Pensions Dispute Resolution Team", "Recovery from Estates"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypePensionCredit_thenDoNotAddAWarning(
        String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(PENSION_CREDIT.getShortName(), buildAppellant(false), true,
                FormType.SSCS1U),
            false);

        String assertionMessage = "Asserting Benefit: Pension Credit with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"Pensions Dispute Resolution Team", "Recovery from Estates"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeRetirementPension_thenDoNotAddAWarning(
        String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(RETIREMENT_PENSION.getShortName(), buildAppellant(false),
                true, FormType.SSCS1U),
            false);

        String assertionMessage = "Asserting Benefit: Retirement Pension with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"Pensions Dispute Resolution Team"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeBereavementBenefit_thenDoNotAddAWarning(
        String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(BEREAVEMENT_BENEFIT.getShortName(), buildAppellant(false),
                true, FormType.SSCS1U),
            false);

        String assertionMessage = "Asserting Benefit: Bereavement Benefit with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"Carer’s Allowance Dispute Resolution Team"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeCarersAllowance_thenDoNotAddAWarning(
        String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(CARERS_ALLOWANCE.getShortName(), buildAppellant(false),
                true, FormType.SSCS1U),
            false);

        String assertionMessage = "Asserting Benefit: Carers Allowance with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"Walsall Benefit Centre"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeMaternityAllowance_thenDoNotAddAWarning(
        String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(MATERNITY_ALLOWANCE.getShortName(), buildAppellant(false),
                true, FormType.SSCS1U),
            false);

        String assertionMessage = "Asserting Benefit: Maternity Allowance with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @ParameterizedTest
    @CsvSource({"Pensions Dispute Resolution Team"})
    public void givenAnAppealContainsAValidOfficeForBenefitTypeBsps_thenDoNotAddAWarning(String dwpIssuingOffice) {
        defaultMrnDetails.setDwpIssuingOffice(dwpIssuingOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildMinimumAppealDataWithBenefitTypeAndFormType(BEREAVEMENT_SUPPORT_PAYMENT_SCHEME.getShortName(),
                buildAppellant(false),
                true, FormType.SSCS1U),
            false);

        String assertionMessage =
            "Asserting Benefit: Bereavement Support Payment Scheme with Office: " + dwpIssuingOffice;
        assertEquals(0, response.getWarnings().size(), assertionMessage);
        assertEquals(0, response.getErrors().size(), assertionMessage);
    }

    @Test
    public void givenAnAppellantIsEmpty_thenAddAWarning() {
        Map<String, Object> ocrCaseDataEmptyOffice = new HashMap<>();
        ocrCaseDataEmptyOffice.put("person1_address_line4", "county");
        ocrCaseDataEmptyOffice.put("person2_address_line4", "county");
        ocrCaseDataEmptyOffice.put("representative_address_line4", "county");
        given(scannedData.getOcrCaseData()).willReturn(ocrCaseDataEmptyOffice);
        Map<String, Object> pairs = new HashMap<>();
        pairs.put("appeal", Appeal.builder().hearingType(HEARING_TYPE_ORAL).build());
        pairs.put("bulkScanCaseReference", 123);
        pairs.put("formType", FormType.SSCS1);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, pairs, false);

        assertThat(response.getWarnings())
            .containsOnly(
                "person1_title is empty",
                "person1_first_name is empty",
                "person1_last_name is empty",
                "person1_address_line1 is empty",
                "person1_address_line3 is empty",
                "person1_address_line4 is empty",
                "person1_postcode is empty",
                "person1_nino is empty",
                "mrn_date is empty",
                "office is empty",
                "benefit_type_description is empty");
    }

    @Test
    public void givenAnAppellantIsEmptySscs8_thenAddAWarningForIbcaReference() {
        Map<String, Object> ocrCaseDataEmptyOffice = new HashMap<>();
        ocrCaseDataEmptyOffice.put("person1_address_line4", "county");
        ocrCaseDataEmptyOffice.put("person2_address_line4", "county");
        ocrCaseDataEmptyOffice.put("representative_address_line4", "county");
        given(scannedData.getOcrCaseData()).willReturn(ocrCaseDataEmptyOffice);
        Map<String, Object> pairs = new HashMap<>();
        BenefitType benefitType = BenefitType.builder().code(INFECTED_BLOOD_COMPENSATION.getShortName()).build();
        AppealReasons appealReasons = AppealReasons.builder().reasons(List.of(AppealReason.builder().value(AppealReasonDetails.builder().reason("some reason").description("some description").build()).build())).build();
        pairs.put("appeal", Appeal.builder()
            .hearingType(HEARING_TYPE_ORAL)
            .hearingSubtype(HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build())
            .benefitType(benefitType)
            .appealReasons(appealReasons).build());

        pairs.put("bulkScanCaseReference", 123);
        pairs.put("formType", FormType.SSCS8);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, pairs, false);

        assertThat(response.getWarnings())
            .containsOnly(
                "person1_title is empty",
                "person1_first_name is empty",
                "person1_last_name is empty",
                "person1_address_line1 is empty",
                "person1_address_line3 is empty",
                "person1_address_line4 is empty",
                "person1_postcode is empty",
                "person1_ibca_reference is empty",
                "mrn_date is empty",
                "appeal_grounds is missing");
    }

    @Test
    public void givenAnAppellantWithNoName_thenAddWarnings() {
        Map<String, Object> pairs = new HashMap<>();

        pairs.put("appeal", Appeal.builder().appellant(Appellant.builder().address(
                    Address.builder().line1("123 The Road").town("Harlow").county("Essex").postcode(VALID_POSTCODE).build())
                .identity(Identity.builder().nino("BB000000B").build()).build())
            .benefitType(BenefitType.builder().code(PIP.name()).build())
            .mrnDetails(defaultMrnDetails)
            .hearingType(HEARING_TYPE_ORAL).build());
        pairs.put("bulkScanCaseReference", 123);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, pairs, false);

        assertThat(response.getWarnings())
            .containsOnly("person1_title is empty",
                "person1_first_name is empty",
                "person1_last_name is empty");
    }

    @Test
    public void givenAnAppellantWithHearingTypeOralAndNoHearingSubType_thenAddWarnings() {
        Map<String, Object> pairs = new HashMap<>();

        pairs.put("appeal", Appeal.builder()
            .appellant(Appellant.builder().name(Name.builder().firstName("Harry").lastName("Kane").build())
                .address(Address.builder().line1("123 The Road").town("Harlow").county("Essex").postcode(VALID_POSTCODE)
                    .build())
                .identity(Identity.builder().nino("BB000000B").build()).build())
            .benefitType(BenefitType.builder().code(PIP.name()).build())
            .mrnDetails(defaultMrnDetails)
            .hearingType(HEARING_TYPE_ORAL).build());
        pairs.put("bulkScanCaseReference", 123);
        pairs.put("formType", FormType.SSCS1PEU);

        ocrCaseData.put(HEARING_TYPE_TELEPHONE_LITERAL, "");
        ocrCaseData.put(HEARING_TYPE_VIDEO_LITERAL, "");
        ocrCaseData.put(HEARING_TYPE_FACE_TO_FACE_LITERAL, "");

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, pairs, false);

        assertThat(response.getWarnings())
            .containsOnly("person1_title is empty",
                "hearing_type_telephone, hearing_type_video and hearing_type_face_to_face are empty. At least one must be populated");
    }

    @Test
    public void givenAnAppellantWithNoNameAndEmptyAppointeeDetails_thenAddWarnings() {
        Map<String, Object> pairs = new HashMap<>();

        Appointee appointee = Appointee.builder()
            .address(Address.builder().build())
            .name(Name.builder().build())
            .contact(Contact.builder().build())
            .identity(Identity.builder().build())
            .build();

        pairs.put("appeal", Appeal.builder().appellant(Appellant.builder()
                .appointee(appointee)
                .address(
                    Address.builder().line1("123 The Road").town("Harlow").county("Essex").postcode(VALID_POSTCODE).build())
                .identity(Identity.builder().nino("BB000000B").build()).build())
            .benefitType(BenefitType.builder().code(PIP.name()).build())
            .mrnDetails(defaultMrnDetails)
            .hearingType(HEARING_TYPE_ORAL).build());
        pairs.put("bulkScanCaseReference", 123);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, pairs, false);

        assertThat(response.getWarnings())
            .containsOnly(
                "person1_title is empty",
                "person1_first_name is empty",
                "person1_last_name is empty");
    }

    @Test
    public void givenAnAppellantWithNoAddress_thenAddWarnings() {
        Map<String, Object> pairs = new HashMap<>();

        pairs.put("appeal", Appeal.builder().appellant(Appellant.builder().name(
                    Name.builder().firstName("Harry").lastName("Kane").build())
                .identity(Identity.builder().nino("BB000000B").build()).build())
            .benefitType(BenefitType.builder().code(PIP.name()).build())
            .mrnDetails(defaultMrnDetails)
            .hearingType(HEARING_TYPE_ORAL)
            .hearingSubtype(HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build()).build());
        pairs.put("bulkScanCaseReference", 123);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, pairs, false);

        assertThat(response.getWarnings())
            .containsOnly(
                "person1_title is empty",
                "person1_address_line1 is empty",
                "person1_address_line3 is empty",
                "person1_address_line4 is empty",
                "person1_postcode is empty");
    }

    @Test
    public void givenIbcCaseAppellantWithNoAddress_thenAddWarnings() {
        Map<String, Object> pairs = new HashMap<>();
        AppealReasons appealReasons = AppealReasons.builder().reasons(List.of(AppealReason.builder().value(AppealReasonDetails.builder().reason("some reason").description("some description").build()).build())).build();
        pairs.put("appeal", Appeal.builder().appellant(Appellant.builder().name(
                    Name.builder().firstName("Harry").lastName("Kane").build())
                .identity(Identity.builder().ibcaReference("A12A21").build())
                .ibcRole("some-role").build())
            .appealReasons(appealReasons)
            .benefitType(BenefitType.builder().code(INFECTED_BLOOD_COMPENSATION.getShortName()).build())
            .mrnDetails(defaultMrnDetails)
            .hearingType(HEARING_TYPE_ORAL)
            .hearingSubtype(HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build()).build());
        pairs.put("bulkScanCaseReference", 123);
        pairs.put("formType", FormType.SSCS8);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, pairs, false);

        assertThat(response.getWarnings())
            .containsOnly(
                "person1_address_line1 is empty",
                "person1_address_line2 is empty",
                "person1_postcode is empty");
    }

    @Test
    public void givenIbcCaseAppellantWithPortOfEntry_thenNoWarningsForCountyPostcode() {
        Map<String, Object> pairs = new HashMap<>();
        AppealReasons appealReasons = AppealReasons.builder().reasons(List.of(AppealReason.builder().value(AppealReasonDetails.builder().reason("some reason").description("some description").build()).build())).build();
        pairs.put("appeal", Appeal.builder().appellant(Appellant.builder().name(
                    Name.builder().firstName("Harry").lastName("Kane").build())
                .identity(Identity.builder().ibcaReference("A12A21").build())
                .ibcRole("some-role").address(Address.builder().portOfEntry(PORT_OF_NORWICH_A_FINE_CITY)
                    .build()).build())
            .appealReasons(appealReasons)
            .benefitType(BenefitType.builder().code(INFECTED_BLOOD_COMPENSATION.getShortName()).build())
            .mrnDetails(defaultMrnDetails)
            .hearingType(HEARING_TYPE_ORAL)
            .hearingSubtype(HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build()).build());
        pairs.put("bulkScanCaseReference", 123);
        pairs.put("formType", FormType.SSCS8);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, pairs, false);

        assertThat(response.getWarnings())
            .containsOnly(
                "person1_address_line1 is empty",
                "person1_address_line2 is empty");
    }

    @Test
    public void givenAnAppellantDoesNotContainATitle_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getName().setTitle(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_title is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainAValidTitle_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getName().setTitle("Bla");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_title is invalid", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesContainAValidTitleWithFullStop_thenDoNotAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getName().setTitle("Mr.");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppellantDoesContainAValidTitleLowercase_thenDoNotAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getName().setTitle("mr");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppellantDoesContainAValidTitle_thenDoNotAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getName().setTitle("Mr");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppellantDoesNotContainAFirstName_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getName().setFirstName(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_first_name is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainALastName_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getName().setLastName(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_last_name is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainAnAddressLine1_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setLine1(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line1 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainValidAddressLine1_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setLine1("[my house");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line1 has invalid characters at the beginning", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainATownAndContainALine2_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setLine2("101 Street");
        appellant.getAddress().setTown(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line3 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainAValidTownAndContainALine2_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setLine2("101 Street");
        appellant.getAddress().setTown("@invalid");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line3 has invalid characters at the beginning", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainATownAndALine2_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        ocrCaseData.remove("person1_address_line4");
        ocrCaseData.remove("person2_address_line4");
        appellant.getAddress().setLine2(null);
        appellant.getAddress().setTown(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line2 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainAValidTownAndALine2_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        ocrCaseData.remove("person1_address_line4");
        ocrCaseData.remove("person2_address_line4");
        appellant.getAddress().setLine2(null);
        appellant.getAddress().setTown("@invalid");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line2 has invalid characters at the beginning", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainACountyAndContainALine2_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setLine2("101 Street");
        appellant.getAddress().setCounty(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line4 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainAValidCountyAndContainALine2_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setLine2("101 Street");
        appellant.getAddress().setCounty("(Bad County");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line4 has invalid characters at the beginning", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainACountyAndALine2_thenAddAWarning() {
        ocrCaseData.remove("person1_address_line4");
        ocrCaseData.remove("person2_address_line4");
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setLine2(null);
        appellant.getAddress().setCounty(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line3 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainAValidCountyAndALine2_thenAddAWarning() {
        ocrCaseData.remove("person1_address_line4");
        ocrCaseData.remove("person2_address_line4");
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setLine2(null);
        appellant.getAddress().setCounty("£bad County");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line3 has invalid characters at the beginning", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantContainsAPlaceholderCountyAndALine2_thenNoAWarning() {
        ocrCaseData.remove("person1_address_line4");
        ocrCaseData.remove("person2_address_line4");
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setLine2(null);
        appellant.getAddress().setCounty(".");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenAnAppellantDoesNotContainAPostcode_thenAddAWarningAndDoNotAddRegionalProcessingCenter() {
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setPostcode(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_postcode is empty", response.getWarnings().getFirst());
        verifyNoInteractions(regionalProcessingCenterService);
    }

    @Test
    public void givenAnIbcCase_warnsIfNoAppealReasons() {
        defaultMrnDetails.setDwpIssuingOffice("IBCA");
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setPostcode(null);
        appellant.getAddress().setInMainlandUk(YesNo.NO);
        appellant.getAddress().setPortOfEntry(PORT_OF_NORWICH_A_FINE_CITY);
        appellant.getIdentity().setIbcaReference("A12A12");
        appellant.setIbcRole("some-role");

        var data = buildMinimumAppealDataWithBenefitTypeWithAppealReasons(INFECTED_BLOOD_COMPENSATION.getShortName(), appellant, true, FormType.SSCS8, null);
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, data, false);

        assertEquals(1, response.getWarnings().size());
        assertTrue(response.getWarnings().contains(APPEAL_GROUNDS + " " + IS_EMPTY));
        verify(regionalProcessingCenterService).getByPostcode(PORT_OF_NORWICH_A_FINE_CITY, true);
    }

    @Test
    public void givenAnIbcCase_warnsIfAppealReasonsOcrIsFalse() {
        defaultMrnDetails.setDwpIssuingOffice("IBCA");
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setPostcode(null);
        appellant.getAddress().setInMainlandUk(YesNo.NO);
        appellant.getAddress().setPortOfEntry(PORT_OF_NORWICH_A_FINE_CITY);
        appellant.getIdentity().setIbcaReference("A12A12");
        appellant.setIbcRole("some-role");
        ocrCaseData.put(APPEAL_GROUNDS, "False");
        AppealReasons appealReasons = AppealReasons.builder().reasons(List.of(AppealReason.builder().value(AppealReasonDetails.builder().reason("some reason").description("some description").build()).build())).build();
        var data = buildMinimumAppealDataWithBenefitTypeWithAppealReasons(INFECTED_BLOOD_COMPENSATION.getShortName(), appellant, true, FormType.SSCS8, appealReasons);
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, data, false);

        assertEquals(1, response.getWarnings().size());
        assertTrue(response.getWarnings().contains(APPEAL_GROUNDS + " " + IS_MISSING));
        verify(regionalProcessingCenterService).getByPostcode(PORT_OF_NORWICH_A_FINE_CITY, true);
    }

    @ParameterizedTest
    @CsvSource(value = {"DIRECTION_ISSUED", "null"}, nullValues = {"null"})
    public void givenAnIbcCase_warnsIfNoAppealReasonsIfNotValidateAppealEvent(EventType eventType) {
        defaultMrnDetails.setDwpIssuingOffice("IBCA");
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setPostcode(null);
        appellant.getAddress().setInMainlandUk(YesNo.NO);
        appellant.getAddress().setPortOfEntry(PORT_OF_NORWICH_A_FINE_CITY);
        appellant.getIdentity().setIbcaReference("A12A12");
        appellant.setIbcRole("some-role");

        var data = buildMinimumAppealDataWithBenefitTypeWithAppealReasons(INFECTED_BLOOD_COMPENSATION.getShortName(), appellant, true, FormType.SSCS8, null);
        CaseResponse response = validator.validateValidationRecord(data, false, eventType);

        assertEquals(1, response.getWarnings().size());
        assertTrue(response.getWarnings().contains("Grounds for appeal " + IS_EMPTY));
        verify(regionalProcessingCenterService).getByPostcode(PORT_OF_NORWICH_A_FINE_CITY, true);
    }

    @Test
    public void givenAnIbcCase_doesNotWarnIfNoAppealReasonsIfValidateAppealEvent() {
        defaultMrnDetails.setDwpIssuingOffice("IBCA");
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setPostcode(null);
        appellant.getAddress().setInMainlandUk(YesNo.NO);
        appellant.getAddress().setPortOfEntry(PORT_OF_NORWICH_A_FINE_CITY);
        appellant.getIdentity().setIbcaReference("A12A12");
        appellant.setIbcRole("some-role");

        var data = buildMinimumAppealDataWithBenefitTypeWithAppealReasons(INFECTED_BLOOD_COMPENSATION.getShortName(), appellant, true, FormType.SSCS8, null);
        CaseResponse response = validator.validateValidationRecord(data, false, EventType.VALID_APPEAL);

        assertTrue(response.getWarnings().isEmpty());
        verify(regionalProcessingCenterService).getByPostcode(PORT_OF_NORWICH_A_FINE_CITY, true);
    }

    @Test
    public void givenAnIbcCase_doesNotWarnIfAppealReasons() {
        defaultMrnDetails.setDwpIssuingOffice("IBCA");
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setPostcode(null);
        appellant.getAddress().setInMainlandUk(YesNo.NO);
        appellant.getAddress().setPortOfEntry(PORT_OF_NORWICH_A_FINE_CITY);
        appellant.getIdentity().setIbcaReference("A12A12");
        appellant.setIbcRole("some-role");
        AppealReasons appealReasons = AppealReasons.builder().reasons(List.of(AppealReason.builder().value(AppealReasonDetails.builder().reason("some reason").description("some description").build()).build())).build();
        var data = buildMinimumAppealDataWithBenefitTypeWithAppealReasons(INFECTED_BLOOD_COMPENSATION.getShortName(), appellant, true, FormType.SSCS8, appealReasons);
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, data, false);

        assertEquals(0, response.getWarnings().size());
        verify(regionalProcessingCenterService).getByPostcode(PORT_OF_NORWICH_A_FINE_CITY, true);
    }

    @Test
    public void givenAnIbcCase_doesNotWarnPortOfEntryIfInUk() {
        defaultMrnDetails.setDwpIssuingOffice("IBCA");
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setPostcode(VALID_POSTCODE);
        appellant.getAddress().setInMainlandUk(YesNo.YES);
        appellant.getIdentity().setIbcaReference("A12A12");
        appellant.setIbcRole("some-role");
        AppealReasons appealReasons = AppealReasons.builder().reasons(List.of(AppealReason.builder().value(AppealReasonDetails.builder().reason("some reason").description("some description").build()).build())).build();
        var data = buildMinimumAppealDataWithBenefitTypeWithAppealReasons(INFECTED_BLOOD_COMPENSATION.getShortName(), appellant, true, FormType.SSCS8, appealReasons);
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, data, false);

        assertEquals(0, response.getWarnings().size());
        verify(regionalProcessingCenterService).getByPostcode(VALID_POSTCODE, true);
    }

    @Test
    public void givenAnIbcCase_errorsIfInvalidPortOfEntry() {
        defaultMrnDetails.setDwpIssuingOffice("IBCA");
        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setPostcode(null);
        appellant.getAddress().setInMainlandUk(YesNo.NO);
        appellant.getAddress().setPortOfEntry("some-invalid-port");
        appellant.getIdentity().setIbcaReference("A12A12");
        appellant.setIbcRole("some-role");
        AppealReasons appealReasons = AppealReasons.builder().reasons(List.of(AppealReason.builder().value(AppealReasonDetails.builder().reason("some reason").description("some description").build()).build())).build();
        var data = buildMinimumAppealDataWithBenefitTypeWithAppealReasons(INFECTED_BLOOD_COMPENSATION.getShortName(), appellant, true, FormType.SSCS8, appealReasons);
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, data, false);

        assertEquals(0, response.getWarnings().size());
        assertTrue(response.getErrors().contains(PORT_OF_ENTRY_INVALID_ERROR));
    }

    @Test
    public void givenAnAppellantContainsPostcodeWithNoRegionalProcessingCenter_thenDoNotAddRegionalProcessingCenter() {
        Appellant appellant = buildAppellant(false);
        given(regionalProcessingCenterService.getByPostcode(VALID_POSTCODE, false)).willReturn(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertNull(response.getTransformedCase().get("regionalProcessingCenter"));
        assertNull(response.getTransformedCase().get("region"));
        assertEquals("person1_postcode is not a postcode that maps to a regional processing center",
            response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppointee_thenRegionalProcessingCenterIsAlwaysFromTheAppellantsPostcode() {
        Appellant appellant = buildAppellant(true);
        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder().hearingRoute(HearingRoute.LIST_ASSIST)
            .name("Liverpool").address1("Address 1").build();
        given(regionalProcessingCenterService.getByPostcode(appellant.getAddress().getPostcode(), false)).willReturn(rpc);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS8),
                false);

        assertEquals(rpc, response.getTransformedCase().get("regionalProcessingCenter"));
        assertEquals(rpc.getName(), response.getTransformedCase().get("region"));
        assertTrue(response.getWarnings().contains("appeal_grounds is empty"));
    }

    @Test
    public void givenAnAppellantDoesNotContainANino_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setNino(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_nino is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesNotContainAValidNino_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setNino("Bla");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_nino is invalid", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppellantDoesContainANino_thenDoNotAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setNino("BB000000B");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSscs8DoesNotContainIbcaReference_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setIbcaReference(null);
        appellant.setIbcRole("some-role");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS8),
                false);

        assertEquals("person1_ibca_reference is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenIbcCaseDoesNotContainIbcaReference_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setIbcaReference(null);
        appellant.setIbcRole("some-role");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord,
                buildMinimumAppealDataWithBenefitType(INFECTED_BLOOD_COMPENSATION.getShortName(), appellant, true, FormType.SSCS8),
                false);

        assertEquals("person1_ibca_reference is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenSscs8DoesNotContainValidIbcaReference_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setIbcaReference("NOT VALID");
        appellant.setIbcRole("some-role");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS8),
                false);

        assertEquals("person1_ibca_reference is invalid", response.getWarnings().getFirst());
    }

    @Test
    public void givenSscs8HasNoAppellantIbcRole_thenAddAnError() {
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setIbcaReference("A12A12");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS8),
                false);

        assertEquals(1, response.getErrors().size());
        String actualError = response.getErrors().getFirst();
        assertTrue(actualError.startsWith("One of the following must be True: "));
        assertTrue(actualError.contains("person1_as_rep_of_deceased"));
        assertTrue(actualError.contains("person1_on_behalf_of_a_person_who_lacks_capacity"));
        assertTrue(actualError.contains("person1_as_poa"));
        assertTrue(actualError.contains("person1_for_self"));
        assertTrue(actualError.contains("person1_for_person_under_18"));
    }

    @Test
    public void givenSscs8HasOneAppellantIbcRole_thenDoNotError() {
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setIbcaReference("A12A12");
        ocrCaseData.put(IBC_ROLE_FOR_SELF, true);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS8),
                false);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSscs8HasAllAppellantIbcRoles_thenAddErrors() {
        ocrCaseData.put(IBC_ROLE_FOR_SELF, true);
        ocrCaseData.put(IBC_ROLE_FOR_U18, true);
        ocrCaseData.put(IBC_ROLE_FOR_DECEASED, true);
        ocrCaseData.put(IBC_ROLE_FOR_POA, true);
        ocrCaseData.put(IBC_ROLE_FOR_LACKING_CAPACITY, true);
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setIbcaReference("A12A12");
        appellant.setIbcRole(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS8),
                false);
        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().getFirst().replace(" cannot all be True", "");
        List<String> errors = Arrays.asList(error.split(", "));
        assertTrue(errors.contains(IBC_ROLE_FOR_SELF));
        assertTrue(errors.contains(IBC_ROLE_FOR_U18));
        assertTrue(errors.contains(IBC_ROLE_FOR_LACKING_CAPACITY));
        assertTrue(errors.contains(IBC_ROLE_FOR_POA));
        assertTrue(errors.contains(IBC_ROLE_FOR_DECEASED));
    }

    @Test
    public void givenSscs8HasMultipleAppellantIbcRoles_thenAddErrors() {
        ocrCaseData.put(IBC_ROLE_FOR_SELF, true);
        ocrCaseData.put(IBC_ROLE_FOR_U18, true);
        ocrCaseData.put(IBC_ROLE_FOR_POA, true);
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setIbcaReference("A12A12");
        appellant.setIbcRole(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS8),
                false);

        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().getFirst().replace(" cannot all be True", "");
        List<String> errors = Arrays.asList(error.split(", "));
        assertTrue(errors.contains(IBC_ROLE_FOR_SELF));
        assertTrue(errors.contains(IBC_ROLE_FOR_U18));
        assertTrue(errors.contains(IBC_ROLE_FOR_POA));
    }

    @Test
    public void givenValidAppealEventSscs8HasIbcRoleSet_thenDoNotError() {
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setIbcaReference("A12A12");
        appellant.setIbcRole("myself");

        CaseResponse response = validator
            .validateValidationRecord(buildMinimumAppealData(appellant, true, FormType.SSCS8),
                false, EventType.VALID_APPEAL);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenValidAppealEventSscs8HasIbcRoleNotSet_thenError() {
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setIbcaReference("A12A12");
        appellant.setIbcRole(null);

        CaseResponse response = validator
            .validateValidationRecord(buildMinimumAppealData(appellant, true, FormType.SSCS8),
                false, EventType.VALID_APPEAL);

        assertEquals(Collections.singletonList("ibcRole is empty"), response.getErrors());
    }

    @Test
    public void givenAnAppointeeExistsAndAnAppellantDoesNotContainANino_thenAddAWarningAboutPerson2() {
        Appellant appellant = buildAppellant(true);
        appellant.getIdentity().setNino(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person2_nino is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppointeeWithEmptyDetailsAndAnAppellantDoesNotContainANino_thenAddAWarningAboutPerson1() {
        Appellant appellant = buildAppellant(true);
        appellant.getIdentity().setNino(null);
        appellant.getAppointee().setName(null);
        appellant.getAppointee().setAddress(null);
        appellant.getAppointee().setContact(null);
        appellant.getAppointee().setIdentity(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_nino is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppealDoesNotContainAnMrnDate_thenAddAWarning() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithMrn(MrnDetails.builder().dwpIssuingOffice("2").build(), buildAppellant(false),
                true, FormType.SSCS1PE), false);

        assertEquals("mrn_date is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppealContainsAnMrnDateInFuture_thenAddAWarning() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithMrn(MrnDetails.builder().dwpIssuingOffice("2").mrnDate("2148-10-10").build(),
                buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals("mrn_date is in future", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnMrnDoesNotContainADwpIssuingOfficeAndOcrDataIsEmpty_thenAddAWarning() {
        Map<String, Object> ocrCaseDataInvalidOffice = new HashMap<>();
        given(scannedData.getOcrCaseData()).willReturn(ocrCaseDataInvalidOffice);
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithMrn(MrnDetails.builder().mrnDate("2019-01-01").dwpIssuingOffice(null).build(),
                buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals("office is empty", response.getWarnings().getFirst());
        assertEquals(1, response.getWarnings().size());
    }

    @ParameterizedTest
    @CsvSource({"SSCS2", "SSCS5"})
    public void givenAnMrnDoesNotContainADwpIssuingOfficeAndFormTypeIsSscs2OrSscs5_thenDoNotAddAWarning(FormType formType) {
        Map<String, Object> ocrCaseDataInvalidOffice = new HashMap<>();
        given(scannedData.getOcrCaseData()).willReturn(ocrCaseDataInvalidOffice);
        ExceptionRecord exceptionRecord = formType.equals(FormType.SSCS2) ? exceptionRecordSscs2 : exceptionRecordSscs5;

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithMrn(MrnDetails.builder().mrnDate("2019-01-01").dwpIssuingOffice(null).build(),
                buildAppellant(false), true, formType), false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnMrnDoesNotContainAValidDwpIssuingOfficeAndOcrDataIsEmpty_thenAddAWarning() {
        Map<String, Object> ocrCaseDataInvalidOffice = new HashMap<>();
        given(scannedData.getOcrCaseData()).willReturn(ocrCaseDataInvalidOffice);
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithMrn(MrnDetails.builder().mrnDate("2019-01-01").dwpIssuingOffice("Bla").build(),
                buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals("office is invalid", response.getWarnings().getFirst());
        assertEquals(1, response.getWarnings().size());
    }

    @Test
    public void givenAnMrnDoesNotContainADwpIssuingOffice_thenAddAWarning() {
        given(scannedData.getOcrCaseData()).willReturn(emptyMap());
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithMrn(MrnDetails.builder().mrnDate("2019-01-01").dwpIssuingOffice(null).build(),
                buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals("office is empty", response.getWarnings().getFirst());
        assertEquals(1, response.getWarnings().size());
    }

    @Test
    public void givenAnMrnDoesNotContainAValidDwpIssuingOffice_thenAddAWarning() {
        Map<String, Object> ocrCaseDataInvalidOffice = new HashMap<>();
        ocrCaseDataInvalidOffice.put("office", "Bla");
        given(scannedData.getOcrCaseData()).willReturn(ocrCaseDataInvalidOffice);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithMrn(MrnDetails.builder().mrnDate("2019-01-01").dwpIssuingOffice(null).build(),
                buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals("office is invalid", response.getWarnings().getFirst());
        assertEquals(1, response.getWarnings().size());
    }

    @Test
    public void givenAnMrnDoesContainValidUpperCaseDwpIssuingOffice_thenNoWarning() {
        given(scannedData.getOcrCaseData()).willReturn(emptyMap());
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithMrn(
                MrnDetails.builder().mrnDate("2019-01-01").dwpIssuingOffice("BALHAM DRT").build(),
                buildAppellant(false), true, FormType.SSCS1PE), false);

        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    public void givenAnMrnDoesContainValidCapitaliseDwpIssuingOffice_thenNoWarning() {
        given(scannedData.getOcrCaseData()).willReturn(emptyMap());
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithMrn(
                MrnDetails.builder().mrnDate("2019-01-01").dwpIssuingOffice("Balham DRT").build(),
                buildAppellant(false), true, FormType.SSCS1PE), false);

        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    public void givenAnAppealContainsAnAppellantDateOfBirthInFuture_thenAddAWarning() {
        Appellant appellant = buildAppellant(false);
        appellant.getIdentity().setDob("2148-10-10");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_dob is in future", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppealContainsAnAppointeeDateOfBirthInFuture_thenAddAWarning() {
        Appellant appellant = buildAppellant(true);
        appellant.getAppointee().getIdentity().setDob("2148-10-10");

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_dob is in future", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppealContainsAHearingExcludedDateInPast_thenAddAWarning() {
        Appellant appellant = buildAppellant(true);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithExcludedDate("2018-10-10", appellant, true, FormType.SSCS1PE), false);

        assertEquals("hearing_options_exclude_dates is in past", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppealDoesNotContainABenefitTypeDescription_thenAddAWarning() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType(null, buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals(BENEFIT_TYPE_DESCRIPTION + " is empty", response.getWarnings().getFirst());
    }

    @ParameterizedTest
    @CsvSource({"SSCS1U", "SSCS5"})
    public void givenAnAppealDoesNotContainABenefitTypeOtherForSscs1UOrSscs5Form_thenDoNotAddAWarning(FormType formType) {
        Map<String, Object> caseData = buildMinimumAppealDataWithBenefitType(null, buildAppellant(false), true, formType);
        caseData.put("formType", formType);
        ExceptionRecord exceptionRecord = formType.equals(FormType.SSCS1U) ? exceptionRecordSscs1U : exceptionRecordSscs5;
        CaseResponse response =
            validator.validateExceptionRecord(transformResponse, exceptionRecord, caseData, false);

        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    public void givenAnAppealContainsAnInvalidBenefitTypeDescription_thenAddAnError() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType("Bla", buildAppellant(false), true, FormType.SSCS1PE), false);

        List<String> benefitNameList = new ArrayList<>();
        for (Benefit be : Benefit.values()) {
            benefitNameList.add(be.getShortName());
        }

        assertEquals(BENEFIT_TYPE_DESCRIPTION + " invalid. Should be one of: " + String.join(", ", benefitNameList),
            response.getErrors().getFirst());
    }

    @Test
    public void givenAnAppealContainsAValidLowercaseBenefitTypeDescription_thenDoNotAddAWarning() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType(PIP.name().toLowerCase(), buildAppellant(false), true, FormType.SSCS1PE), false);

        List<String> benefitNameList = new ArrayList<>();
        for (Benefit be : Benefit.values()) {
            benefitNameList.add(be.name());
        }

        assertEquals("PIP", ((Appeal) response.getTransformedCase().get("appeal")).getBenefitType().getCode());
        assertEquals("Personal Independence Payment",
            ((Appeal) response.getTransformedCase().get("appeal")).getBenefitType().getDescription());
        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppealContainsAValidBenefitTypeDescription_thenDoNotAddAWarning() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType(PIP.name(), buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals("PIP", ((Appeal) response.getTransformedCase().get("appeal")).getBenefitType().getCode());
        assertEquals("Personal Independence Payment",
            ((Appeal) response.getTransformedCase().get("appeal")).getBenefitType().getDescription());
        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAPostcode_thenAddRegionalProcessingCenterToCase() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType(PIP.name(), buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals("Address 1",
            ((RegionalProcessingCenter) response.getTransformedCase().get("regionalProcessingCenter")).getAddress1());
        assertEquals("Liverpool", (response.getTransformedCase().get("region")));
    }

    @Test
    public void givenAnAppealContainsAnInvalidAppellantMobileNumberLessThan10Digits_thenAddAnError() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType("Bla", buildAppellantWithMobileNumber("07776156"), true, FormType.SSCS1PE), false);

        assertEquals("person1_mobile is invalid", response.getErrors().getFirst());
    }

    @Test
    public void givenAnAppealContainsAnInvalidRepresentativeMobileNumberLessThan10Digits_thenAddAnError() {
        Representative representative = buildRepresentative();
        representative.setContact(Contact.builder().build());
        representative.getContact().setMobile("0123456");

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals("representative_mobile is invalid", response.getErrors().getFirst());
    }

    @Test
    public void givenAnAppealContainsValidAppellantAnInvalidAppointeeMobileNumberLessThan10Digits_thenAddAnError() {
        Appellant appellant = buildAppellant(true);
        appellant.getContact().setMobile(VALID_MOBILE);
        appellant.setAppointee(buildAppointeeWithMobileNumber("07776156"));
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType("Bla", appellant, true, FormType.SSCS1PE), false);

        assertEquals("person1_mobile is invalid", response.getErrors().getFirst());
    }

    @Test
    public void givenAnAppealContainsAnInValidAppellantAnInvalidAppointeeMobileNumberLessThan10Digits_thenAddAnError() {
        Appellant appellant = buildAppellant(true);
        appellant.getContact().setMobile("07776157");
        appellant.setAppointee(buildAppointeeWithMobileNumber("07776156"));
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType("Bla", appellant, true, FormType.SSCS1PE), false);

        assertEquals("person1_mobile is invalid", response.getErrors().getFirst());
        assertEquals("person2_mobile is invalid", response.getErrors().get(1));
    }

    @Test
    public void givenAnAppealContainsAnInvalidAppellantMobileNumberGreaterThan11Digits_thenAddAnError() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType("Bla", buildAppellantWithMobileNumber("077761560000"), true, FormType.SSCS1PE), false);

        assertEquals("person1_mobile is invalid", response.getErrors().getFirst());
    }

    @Test
    public void givenAnAppealContainsAnInvalidRepresentativeMobileNumberGreaterThan11Digits_thenAddAnError() {
        Representative representative = buildRepresentative();
        representative.setContact(Contact.builder().build());
        representative.getContact().setMobile("0123456789000");

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals("representative_mobile is invalid", response.getErrors().getFirst());
    }

    @Test
    public void givenARepresentativeTitleIsInvalid_thenAddWarning() {
        Representative representative = buildRepresentative();
        representative.setContact(Contact.builder().build());
        representative.getName().setTitle("%54 3434 ^7*");

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals("representative_title is invalid", response.getWarnings().getFirst());
    }

    @ParameterizedTest
    @CsvSource(value = {"''", "null", "' '"}, nullValues = {"null"})
    public void givenARepresentativeTitleIsEmpty_thenDoNotAddAnyWarnings(String title) {
        Representative representative = buildRepresentative();
        representative.setContact(Contact.builder().build());
        representative.getName().setTitle(title);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppealContainsValidAppellantAnInvalidAppointeeMobileNumberGreaterThan11Digits_thenAddAnError() {
        Appellant appellant = buildAppellant(true);
        appellant.getContact().setMobile(VALID_MOBILE);
        appellant.setAppointee(buildAppointeeWithMobileNumber("077761560000"));
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType("Bla", appellant, true, FormType.SSCS1PE), false);

        assertEquals("person1_mobile is invalid", response.getErrors().getFirst());
    }

    @Test
    public void givenAnAppealContainsAnInvalidAppellantAnInvalidAppointeeMobileNumberGreaterThan11Digits_thenAddAnError() {
        Appellant appellant = buildAppellant(true);
        appellant.getContact().setMobile("077761560000");
        appellant.setAppointee(buildAppointeeWithMobileNumber("077761560000"));
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType("Bla", appellant, true, FormType.SSCS1PE), false);

        assertEquals("person1_mobile is invalid", response.getErrors().getFirst());
        assertEquals("person2_mobile is invalid", response.getErrors().get(1));
    }

    @Test
    public void givenAnAppealContainsAValidAppellantMobileNumber_thenDoNotAddAWarning() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType(PIP.name(), buildAppellantWithMobileNumber(VALID_MOBILE), true, FormType.SSCS1PE),
            false);

        assertEquals(VALID_MOBILE,
            ((Appeal) response.getTransformedCase().get("appeal")).getAppellant().getContact().getMobile());
        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppealContainsAnInvalidPostcode_thenAddAnError() {
        given(postcodeValidator.isValidPostcodeFormat(anyString())).willReturn(false);
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType("Bla", buildAppellantWithPostcode("Bla Bla"), true, FormType.SSCS1PE), false);

        assertEquals("person1_postcode is not in a valid format", response.getErrors().getFirst());
    }

    @Test
    public void givenAnAppealContainsAnValidPostcodeFormatButNotFound_thenAddWarning() {
        given(postcodeValidator.isValidPostcodeFormat(anyString())).willReturn(true);
        given(postcodeValidator.isValid(anyString())).willReturn(false);
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealDataWithBenefitType(PIP.name(), buildAppellantWithPostcode("W1 1LA"), true, FormType.SSCS1PE), false);

        assertEquals("person1_postcode is not a valid postcode", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppealContainsAnValidPostcodeFormatButFound_thenNoErrorOrWarnings() {
        given(postcodeValidator.isValidPostcodeFormat(anyString())).willReturn(true);
        given(postcodeValidator.isValid(anyString())).willReturn(true);
        given(regionalProcessingCenterService.getByPostcode(anyString(), eq(false))).willReturn(RegionalProcessingCenter.builder().address1("Address 1").name("Liverpool").build());
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealDataWithBenefitType(PIP.name(), buildAppellantWithPostcode("W1 1LA"), true, FormType.SSCS1PE), false);

        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getErrors().size()).isEqualTo(0);
    }

    @Test
    public void givenAnIbcAppealContainsAnValidPostcodeFormatButFound_thenNoErrorOrWarnings() {
        ocrCaseData.put(IBC_ROLE_FOR_SELF, true);
        given(postcodeValidator.isValidPostcodeFormat(anyString())).willReturn(true);
        given(postcodeValidator.isValid(anyString())).willReturn(true);
        AppealReasons appealReasons = AppealReasons.builder().reasons(List.of(AppealReason.builder().value(AppealReasonDetails.builder().reason("some reason").description("some description").build()).build())).build();
        given(regionalProcessingCenterService.getByPostcode(anyString(), eq(true))).willReturn(RegionalProcessingCenter.builder().address1("Address 1").name("Liverpool").build());
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealDataWithBenefitTypeWithAppealReasons(INFECTED_BLOOD_COMPENSATION.getShortName(), buildAppellantWithPostcode("W1 1LA"), true, FormType.SSCS8, appealReasons), false);

        assertThat(response.getWarnings().size()).isEqualTo(0);
        assertThat(response.getErrors().size()).isEqualTo(0);
        ocrCaseData.remove(IBC_ROLE_FOR_SELF);
    }

    @Test
    public void givenAnAppealContainsAValidPostcode_thenDoNotAddAWarning() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType(PIP.name(), buildAppellantWithPostcode(VALID_POSTCODE), true, FormType.SSCS1PE), false);

        assertEquals(VALID_POSTCODE,
            ((Appeal) response.getTransformedCase().get("appeal")).getAppellant().getAddress().getPostcode());
        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenARepresentativeDoesNotContainAFirstNameOrLastNameOrTitleOrCompany_thenAddAWarning() {
        Representative representative = buildRepresentative();
        representative.getName().setFirstName(null);
        representative.getName().setLastName(null);
        representative.getName().setTitle(null);
        representative.setOrganisation(null);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals(
            "representative_company, representative_first_name and representative_last_name are empty. At least one must be populated",
            response.getWarnings().getFirst());
    }

    @Test
    public void givenARepresentativeContainsAFirstNameButDoesNotContainALastNameOrTitleOrCompany_thenDoNotAddAWarning() {
        Representative representative = buildRepresentative();
        representative.getName().setLastName(null);
        representative.getName().setTitle(null);
        representative.setOrganisation(null);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenARepresentativeContainsALastNameButDoesNotContainAFirstNameOrTitleOrCompany_thenDoNotAddAWarning() {
        Representative representative = buildRepresentative();
        representative.getName().setFirstName(null);
        representative.getName().setTitle(null);
        representative.setOrganisation(null);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenARepresentativeContainsATitleButDoesNotContainAFirstNameOrLastNameOrCompany_thenAddAWarning() {
        Representative representative = buildRepresentative();
        representative.getName().setFirstName(null);
        representative.getName().setLastName(null);
        representative.setOrganisation(null);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals(
            "representative_company, representative_first_name and representative_last_name are empty. At least one must be populated",
            response.getWarnings().getFirst());
    }

    @Test
    public void givenARepresentativeContainsACompanyButDoesNotContainAFirstNameOrLastNameOrTitle_thenDoNotAddAWarning() {
        Representative representative = buildRepresentative();
        representative.getName().setFirstName(null);
        representative.getName().setLastName(null);
        representative.getName().setTitle(null);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenARepresentativeDoesNotContainAnAddressLine1_thenAddAWarning() {
        Representative representative = buildRepresentative();
        representative.getAddress().setLine1(null);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals("representative_address_line1 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenARepresentativeDoesNotContainATown_thenAddAWarning() {
        Representative representative = buildRepresentative();
        representative.getAddress().setLine2("101 Street");
        representative.getAddress().setTown(null);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals("representative_address_line3 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenARepresentativeDoesNotContainACountyAndContainAddressLine2_thenAddAWarning() {
        Representative representative = buildRepresentative();
        representative.getAddress().setLine2("101 Street");
        representative.getAddress().setCounty(null);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals("representative_address_line4 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenARepresentativeDoesNotContainACountyAndAddressLine2_thenAddAWarning() {
        ocrCaseData.remove("representative_address_line4");
        Representative representative = buildRepresentative();
        representative.getAddress().setLine2(null);
        representative.getAddress().setCounty(null);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals("representative_address_line3 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenARepresentativeDoesNotContainAPostcode_thenAddAWarningAndDoNotAddRegionalProcessingCenter() {
        Representative representative = buildRepresentative();
        representative.getAddress().setPostcode(null);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals("representative_postcode is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenANullRepresentative_thenAddAnError() {
        Representative representative = null;
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals(1, response.getErrors().size());
        assertEquals("The \"Has representative\" field is not selected, please select an option to proceed",
            response.getErrors().getFirst());
    }

    @Test
    public void givenARepresentativeWithHasRepresentativeFieldNotSet_thenAddAnError() {
        Representative representative = buildRepresentative();
        representative.setHasRepresentative(null);
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithRepresentative(buildAppellant(false), representative, true, FormType.SSCS1PE), false);

        assertEquals(1, response.getErrors().size());
        assertEquals(HAS_REPRESENTATIVE_FIELD_MISSING, response.getErrors().getFirst());
    }

    @Test
    public void givenAnAppointeeDoesNotContainATitle_thenAddAWarning() {
        Appellant appellant = buildAppellant(true);
        appellant.getAppointee().getName().setTitle(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_title is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppointeeDoesNotContainAFirstName_thenAddAWarning() {
        Appellant appellant = buildAppellant(true);
        appellant.getAppointee().getName().setFirstName(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_first_name is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppointeeDoesNotContainALastName_thenAddAWarning() {
        Appellant appellant = buildAppellant(true);
        appellant.getAppointee().getName().setLastName(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_last_name is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppointeeDoesNotContainAnAddressLine1_thenAddAWarning() {
        Appellant appellant = buildAppellant(true);
        appellant.getAppointee().getAddress().setLine1(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line1 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppointeeDoesNotContainAnAddressLine3AndContainAddressLine2_thenAddAWarning() {
        Appellant appellant = buildAppellant(true);
        appellant.getAppointee().getAddress().setLine2("101 Street");
        appellant.getAppointee().getAddress().setTown(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line3 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppointeeDoesNotContainAnAddressLine3And2_thenAddAWarning() {
        Appellant appellant = buildAppellant(true);
        ocrCaseData.remove("person1_address_line4");
        ocrCaseData.remove("person2_address_line4");
        appellant.getAppointee().getAddress().setLine2(null);
        appellant.getAppointee().getAddress().setTown(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line2 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppointeeDoesNotContainAnAddressLine4AndContainAddressLine2_thenAddAWarning() {
        Appellant appellant = buildAppellant(true);
        appellant.getAppointee().getAddress().setLine2("101 Street");
        appellant.getAppointee().getAddress().setCounty(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line4 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppointeeDoesNotContainAnAddressLine4And2_thenAddAWarning() {
        ocrCaseData.remove("person1_address_line4");
        ocrCaseData.remove("person2_address_line4");
        Appellant appellant = buildAppellant(true);
        appellant.getAppointee().getAddress().setLine2(null);
        appellant.getAppointee().getAddress().setCounty(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_address_line3 is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppointeeDoesNotContainAnAddressLinePostcode_thenAddAWarning() {
        Appellant appellant = buildAppellant(true);
        appellant.getAppointee().getAddress().setPostcode(null);

        CaseResponse response = validator
            .validateExceptionRecord(transformResponse, exceptionRecord, buildMinimumAppealData(appellant, true, FormType.SSCS1PE),
                false);

        assertEquals("person1_postcode is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppealWithNoHearingType_thenAddAWarning() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithHearingType(null, buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals("is_hearing_type_oral and/or is_hearing_type_paper is invalid", response.getWarnings().getFirst());
    }

    @Test
    public void givenAllMandatoryFieldsForAnAppellantExists_thenDoNotAddAWarning() {
        Map<String, Object> pairs = buildMinimumAppealData(buildAppellant(false), true, FormType.SSCS1PE);

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord, pairs, false);

        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenAllMandatoryFieldsAndValidDocumentDoNotAddAnError() {
        Map<String, Object> pairs = buildMinimumAppealData(buildAppellant(false), false, FormType.SSCS1PE);

        pairs.put("sscsDocument", buildDocument("myfile.pdf"));

        CaseResponse response = validator.validateValidationRecord(pairs, true);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAllMandatoryFieldsAndDocumentNameIsNullAddAnError() {
        Map<String, Object> pairs = buildMinimumAppealData(buildAppellant(false), false, FormType.SSCS1PE);

        pairs.put("sscsDocument", buildDocument(null));

        CaseResponse response = validator.validateValidationRecord(pairs, true);

        assertEquals(
            "There is a file attached to the case that does not have a filename, add a filename, e.g. filename.pdf",
            response.getErrors().getFirst());
    }

    @Test
    public void givenAllMandatoryFieldsAndDocumentNameNoExtensionAddAnError() {
        Map<String, Object> pairs = buildMinimumAppealData(buildAppellant(false), false, FormType.SSCS1PE);

        pairs.put("sscsDocument", buildDocument("Waiver"));

        CaseResponse response = validator.validateValidationRecord(pairs, true, null);

        assertEquals(
            "There is a file attached to the case called Waiver, filenames must have extension, e.g. filename.pdf",
            response.getErrors().getFirst());
    }

    @Test
    public void givenAValidationCallbackTypeWithIncompleteDetails_thenAddAWarningWithCorrectMessage() {

        Appellant appellant = buildAppellant(false);
        appellant.getAddress().setPostcode(null);

        CaseResponse response = validator.validateValidationRecord(buildMinimumAppealData(appellant, false, FormType.SSCS1PE), false, null);

        assertEquals("Appellant postcode is empty", response.getWarnings().getFirst());
        verifyNoInteractions(regionalProcessingCenterService);
    }

    @Test
    public void givenAValidationCallbackEventIsAppealToProceedAndMrnDateIsEmpty_thenNoWarningOrErrorMessage() {

        Map<String, Object> pairs =
            buildMinimumAppealDataWithMrn(MrnDetails.builder().dwpIssuingOffice("Sheffield DRT").build(),
                buildAppellant(false), false, FormType.SSCS1PE);

        CaseResponse response = validator.validateValidationRecord(pairs, true, null);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAValidationCallbackEventIsOtherAndMrnDateIsEmpty_thenNoWarningOrErrorMessage() {

        Map<String, Object> pairs =
            buildMinimumAppealDataWithMrn(MrnDetails.builder().dwpIssuingOffice("Sheffield DRT").build(),
                buildAppellant(false), false, FormType.SSCS1PE);

        CaseResponse response = validator.validateValidationRecord(pairs, false, null);

        assertEquals("Mrn date is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppealWithAnErrorAndCombineWarningsTrue_thenMoveErrorsToWarnings() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithBenefitType("Bla", buildAppellantWithMobileNumber("07776156"), true, FormType.SSCS1PE), true);

        assertEquals("person1_mobile is invalid", response.getWarnings().get(1));
        assertEquals(0, response.getErrors().size());
    }

    @ParameterizedTest
    @CsvSource({"07900123456", "01277323440", "01277323440 ext 123"})
    public void givenAnAppealWithValidHearingPhoneNumber_thenDoNotAddWarning(String number) {
        HearingSubtype hearingSubtype = HearingSubtype.builder().hearingTelephoneNumber(number).build();

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithHearingSubtype(hearingSubtype, buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenAnAppealWithAnInvalidHearingPhoneNumber_thenAddWarning() {
        HearingSubtype hearingSubtype = HearingSubtype.builder().hearingTelephoneNumber("01222").build();

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithHearingSubtype(hearingSubtype, buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals(
            "Telephone hearing selected but the number used is invalid. Please check either the hearing_telephone_number or person1_phone fields",
            response.getWarnings().getFirst());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppealWithAnInvalidHearingPhoneNumberForSscsCase_thenAddWarning() {
        Map<String, Object> pairs = buildMinimumAppealDataWithHearingSubtype(
            HearingSubtype.builder().wantsHearingTypeTelephone("Yes").hearingTelephoneNumber("01222").build(),
            buildAppellant(false), false, FormType.SSCS1PE);

        CaseResponse response = validator.validateValidationRecord(pairs, true, null);

        assertEquals(
            "Telephone hearing selected but the number used is invalid. Please check either the telephone or hearing telephone number fields",
            response.getWarnings().getFirst());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppealWithAHearingTypeTelephoneSelectedButNoTelephoneEntered_thenAddWarning() {
        HearingSubtype hearingSubtype =
            HearingSubtype.builder().wantsHearingTypeTelephone("Yes").hearingTelephoneNumber(null).build();

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithHearingSubtype(hearingSubtype, buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals("hearing_telephone_number has not been provided but data indicates hearing telephone is required",
            response.getWarnings().getFirst());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppealWithAHearingTypeTelephoneSelectedButNoTelephoneEnteredForSscsCase_thenAddWarning() {
        Map<String, Object> pairs = buildMinimumAppealDataWithHearingSubtype(
            HearingSubtype.builder().wantsHearingTypeTelephone("Yes").hearingTelephoneNumber(null).build(),
            buildAppellant(false), false, FormType.SSCS1PE);

        CaseResponse response = validator.validateValidationRecord(pairs, true, null);

        assertEquals("Hearing telephone number has not been provided but data indicates hearing telephone is required",
            response.getWarnings().getFirst());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppealWithAHearingTypeVideoSelectedButNoVideoEmailEntered_thenAddWarning() {
        HearingSubtype hearingSubtype =
            HearingSubtype.builder().wantsHearingTypeVideo("Yes").hearingVideoEmail(null).build();

        CaseResponse response = validator.validateExceptionRecord(transformResponse, exceptionRecord,
            buildMinimumAppealDataWithHearingSubtype(hearingSubtype, buildAppellant(false), true, FormType.SSCS1PE), false);

        assertEquals("hearing_video_email has not been provided but data indicates hearing video is required",
            response.getWarnings().getFirst());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppealWithAHearingTypeVideoSelectedButNoVideoEmailEnteredForSscsCase_thenAddWarning() {
        Map<String, Object> pairs = buildMinimumAppealDataWithHearingSubtype(
            HearingSubtype.builder().wantsHearingTypeVideo("Yes").hearingVideoEmail(null).build(),
            buildAppellant(false), false, FormType.SSCS1PE);

        CaseResponse response = validator.validateValidationRecord(pairs, true, null);

        assertEquals("Hearing video email address has not been provided but data indicates hearing video is required",
            response.getWarnings().getFirst());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnAppealWithAnHearingTypePaperAndEmptyHearingSubTypeForSscsCase_thenNoWarning() {
        Map<String, Object> pairs =
            buildMinimumAppealDataWithHearingType(HEARING_TYPE_PAPER, buildAppellant(false), false, FormType.SSCS1PE);
        CaseResponse response = validator.validateValidationRecord(pairs, true, null);
        assertEquals(0, response.getWarnings().size());
    }

    @ParameterizedTest
    @CsvSource({"SSCS1PEU", "SSCS2", "SSCS5"})
    public void givenAnAppealWithAnEmptyHearingSubTypeAndFormTypeIsSscs1peuForSscsCase_thenAddWarning(FormType formType) {
        Map<String, Object> pairs =
            buildMinimumAppealDataWithHearingSubtype(HearingSubtype.builder().build(), buildAppellant(false), false, formType);
        if (formType.equals(FormType.SSCS2)) {
            pairs.put("childMaintenanceNumber", "123456");
        }
        pairs.put("formType", formType);
        CaseResponse response = validator.validateValidationRecord(pairs, true, null);
        assertEquals(1, response.getWarnings().size());
        assertEquals("Hearing option telephone, video and face to face are empty. At least one must be populated",
            response.getWarnings().getFirst());
    }

    @Test
    public void givenAnAppealWithAnEmptyHearingSubTypeForSscsCase_thenNoWarning() {
        Map<String, Object> pairs = buildMinimumAppealDataWithHearingSubtype(HearingSubtype.builder().build(), buildAppellant(false), false, FormType.SSCS1PE);
        pairs.put("formType", FormType.SSCS1);
        CaseResponse response = validator.validateValidationRecord(pairs, true);
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenAnAppealWithAnEmptyHearingSubTypeAndFormTypIsNullForSscsCase_thenNoWarning() {
        Map<String, Object> pairs =
            buildMinimumAppealDataWithHearingSubtype(HearingSubtype.builder().build(), buildAppellant(false), false, FormType.SSCS1PE);
        pairs.put("formType", null);
        CaseResponse response = validator.validateValidationRecord(pairs, true, EventType.DIRECTION_ISSUED);
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenAnAppealWithAnHearingSubTypeVideoForSscsCase_thenNoWarning() {
        Map<String, Object> pairs = buildMinimumAppealDataWithHearingSubtype(
            HearingSubtype.builder().wantsHearingTypeVideo("Yes").hearingVideoEmail("m@m.com").build(),
            buildAppellant(false), false, FormType.SSCS1PE);
        CaseResponse response = validator.validateValidationRecord(pairs, true);
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenAnAppealWithAnHearingSubTypeFaceForSscsCase_thenNoWarning() {
        Map<String, Object> pairs =
            buildMinimumAppealDataWithHearingSubtype(HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build(),
                buildAppellant(false), false, FormType.SSCS1PE);
        CaseResponse response = validator.validateValidationRecord(pairs, true);
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenSscs2FormWithoutChildMaintenance_thenAddAWarning() {

        Map<String, Object> caseData = buildMinimumAppealDataWithBenefitTypeAndFormType(CHILD_SUPPORT.getShortName(), buildAppellant(false), true, FormType.SSCS2);
        caseData.remove("childMaintenanceNumber");

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2,
            caseData,
            false);

        assertEquals("person1_child_maintenance_number is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenSscs2FormWithChildMaintenance_thenAppellantShouldReturnValue() {

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2, buildCaseWithChildMaintenanceWithOtherPartyNameAddress(CHILD_MAINTENANCE_NUMBER, OTHER_PARTY_ADDRESS_LINE1, OTHER_PARTY_ADDRESS_LINE2, OTHER_PARTY_ADDRESS_LINE3, OTHER_PARTY_POSTCODE, buildOtherPartyName()), false);

        assertEquals(0, response.getWarnings().size());
        assertEquals(CHILD_MAINTENANCE_NUMBER, response.getTransformedCase().get("childMaintenanceNumber"));
    }

    @ParameterizedTest
    @CsvSource({", test2, test3, TS1 1ST, other_party_address_line1 is empty, 1",
        "test1, , , TS1 1ST, other_party_address_line2 is empty, 1",
        "test1, test2, , , other_party_postcode is empty, 1",
        "test1, , , , other_party_address_line2 is empty, 2",
        ", , , TS1 1ST, other_party_address_line1 is empty, 2",
    })
    public void givenSscs2FormWithoutOtherPartyAddressEntryAndIgnoreWarningsFalse_thenAddAWarning(String line1, String line2, String line3, String postcode, String warning, int size) {

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2,
            buildCaseWithChildMaintenanceWithOtherPartyNameAddress(CHILD_MAINTENANCE_NUMBER, line1, line2, line3, postcode, buildOtherPartyName()),
            false);

        assertFalse(response.getWarnings().isEmpty());
        assertEquals(size, response.getWarnings().size());
        assertEquals(warning, response.getWarnings().getFirst());
    }

    @Test
    public void givenOtherParty_WithFirstNameLastNamePopulated_WithNoAddress_noWarnings() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildCaseWithChildMaintenanceWithOtherPartyNameAddress(CHILD_MAINTENANCE_NUMBER, "", "", "", "", buildOtherPartyName()),
            false);
        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    public void givenOtherParty_WithNoName_WithNoAddress_noWarnings() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildCaseWithChildMaintenanceWithOtherPartyNameAddress(CHILD_MAINTENANCE_NUMBER, "", "", "", "", Name.builder().build()),
            false);
        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    public void givenOtherParty_NoName_WithAddressPresent_WarningSeen() {
        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildCaseWithChildMaintenanceWithOtherPartyNameAddress(CHILD_MAINTENANCE_NUMBER, "line1", "", "line3", "W1", Name.builder().build()),
            false);
        assertFalse(response.getWarnings().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    public void givenOtherParty_WithFirstNameOrLastNameMissing_WarningSeen(boolean isFirstnameBlank) {
        Name name = Name.builder().firstName(isFirstnameBlank ? "" : "fn").lastName(!isFirstnameBlank ? "" : "ln").build();
        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildCaseWithChildMaintenanceWithOtherPartyNameAddress(CHILD_MAINTENANCE_NUMBER, "line1", "line2", "line3", "W1", name),
            false);
        assertFalse(response.getWarnings().isEmpty());
        assertEquals(1, response.getWarnings().size());
    }

    @Test
    public void givenSscs2FormWithOtherPartyAddressEntry_thenValueIsSet() {

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecord,
            buildCaseWithChildMaintenanceWithOtherPartyNameAddress(
                CHILD_MAINTENANCE_NUMBER, OTHER_PARTY_ADDRESS_LINE1, OTHER_PARTY_ADDRESS_LINE2, OTHER_PARTY_ADDRESS_LINE3, OTHER_PARTY_POSTCODE, buildOtherPartyName()),
            false);

        assertTrue(response.getWarnings().isEmpty());
        assertTrue(response.getErrors().isEmpty());
    }


    @Test
    public void givenSscs2FormWithOtherPartyLastNameMissingAndIgnoreWarningsFalse_thenOtherPartyShouldReturnWarning() {
        Name otherPartyName = buildOtherPartyName();
        otherPartyName.setLastName(null);
        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2, buildCaseWithChildMaintenanceWithOtherPartyNameAddress(
                CHILD_MAINTENANCE_NUMBER, OTHER_PARTY_ADDRESS_LINE1, OTHER_PARTY_ADDRESS_LINE2, OTHER_PARTY_ADDRESS_LINE3, OTHER_PARTY_POSTCODE, otherPartyName),
            false);

        assertEquals(1, response.getWarnings().size());
        assertEquals("other_party_last_name is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenSscs2FormWithOtherPartyFirstNameMissingAndIgnoreWarningsFalse_thenOtherPartyShouldReturnWarning() {
        Name otherPartyName = buildOtherPartyName();
        otherPartyName.setFirstName(null);
        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2, buildCaseWithChildMaintenanceWithOtherPartyNameAddress(
                CHILD_MAINTENANCE_NUMBER, OTHER_PARTY_ADDRESS_LINE1, OTHER_PARTY_ADDRESS_LINE2, OTHER_PARTY_ADDRESS_LINE3, OTHER_PARTY_POSTCODE, otherPartyName),
            false);

        assertEquals(1, response.getWarnings().size());
        assertEquals("other_party_first_name is empty", response.getWarnings().getFirst());
    }

    @Test
    public void givenSscs2FormWithOtherPartyTitleInvalidAndIgnoreWarningsFalse_thenOtherPartyShouldReturnWarning() {
        Name otherPartyName = buildOtherPartyName();
        otherPartyName.setTitle("Random");
        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2, buildCaseWithChildMaintenanceWithOtherPartyNameAddress(
                CHILD_MAINTENANCE_NUMBER, OTHER_PARTY_ADDRESS_LINE1, OTHER_PARTY_ADDRESS_LINE2, OTHER_PARTY_ADDRESS_LINE3, OTHER_PARTY_POSTCODE, otherPartyName),
            false);

        assertEquals(1, response.getWarnings().size());
        assertEquals("other_party_title is invalid", response.getWarnings().getFirst());
    }

    @Test
    public void givenSscs2FormWithOtherPartyLastNameMissingAndIgnoreWarningsTrue_thenNoWarningsShownAndOtherPartyRemoved() {
        Name otherPartyName = buildOtherPartyName();
        otherPartyName.setLastName(null);

        exceptionRecordSscs2 =
            ExceptionRecord.builder().ocrDataFields(ocrList).formType(FormType.SSCS2.getId()).ignoreWarnings(true).build();
        given(sscsJsonExtractor.extractJson(exceptionRecordSscs2)).willReturn(scannedData);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2, buildCaseWithChildMaintenanceWithOtherPartyNameAddress(
                CHILD_MAINTENANCE_NUMBER, OTHER_PARTY_ADDRESS_LINE1, OTHER_PARTY_ADDRESS_LINE2, null, OTHER_PARTY_POSTCODE, otherPartyName),
            false);

        assertEquals(0, response.getWarnings().size());
        assertNull(response.getTransformedCase().get("otherParties"));
    }

    @Test
    public void givenSscs2FormWithChildMaintenanceNumberMissingAndIgnoreWarningsTrue_thenNoWarningsShownAndChildMaintenanceNumberRemoved() {

        exceptionRecordSscs2 = ExceptionRecord.builder().ocrDataFields(ocrList).formType(FormType.SSCS2.getId())
            .ignoreWarnings(true).build();
        given(sscsJsonExtractor.extractJson(exceptionRecordSscs2)).willReturn(scannedData);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2, buildCaseWithChildMaintenanceWithOtherPartyNameAddress(null,
                OTHER_PARTY_ADDRESS_LINE1, OTHER_PARTY_ADDRESS_LINE2, null, OTHER_PARTY_POSTCODE,
                buildOtherPartyName()), false);

        assertEquals(0, response.getWarnings().size());
        assertNull(response.getTransformedCase().get("childMaintenanceNumber"));
    }

    @Test
    public void givenSscs2FormWithChildMaintenanceNumberMissingAndIgnoreWarningsFalse_thenWarningShown() {

        exceptionRecordSscs2 = ExceptionRecord.builder().ocrDataFields(ocrList).formType(FormType.SSCS2.getId())
            .ignoreWarnings(false).build();
        given(sscsJsonExtractor.extractJson(exceptionRecordSscs2)).willReturn(scannedData);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2, buildCaseWithChildMaintenanceWithOtherPartyNameAddress(null,
                OTHER_PARTY_ADDRESS_LINE1, OTHER_PARTY_ADDRESS_LINE2, null, OTHER_PARTY_POSTCODE,
                buildOtherPartyName()), false);

        assertEquals(1, response.getWarnings().size());
        assertEquals("person1_child_maintenance_number is empty", response.getWarnings().getFirst());
    }


    @Test
    public void givenSscs2FormWithOtherPartyFirstNameMissingAndIgnoreWarningsTrue_thenNoWarningsShown() {
        Name otherPartyName = buildOtherPartyName();
        otherPartyName.setFirstName(null);

        exceptionRecordSscs2 =
            ExceptionRecord.builder().ocrDataFields(ocrList).formType(FormType.SSCS2.getId()).ignoreWarnings(true).build();
        given(sscsJsonExtractor.extractJson(exceptionRecordSscs2)).willReturn(scannedData);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2, buildCaseWithChildMaintenanceWithOtherPartyNameAddress(
                CHILD_MAINTENANCE_NUMBER, OTHER_PARTY_ADDRESS_LINE1, OTHER_PARTY_ADDRESS_LINE2, OTHER_PARTY_ADDRESS_LINE3, OTHER_PARTY_POSTCODE, otherPartyName),
            false);

        assertEquals(0, response.getWarnings().size());
        assertNull(response.getTransformedCase().get("otherParties"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void givenSscs2FormWithOtherPartyTitleInvalidAndIgnoreWarningsTrue_thenNoWarningsShown() {
        Name otherPartyName = buildOtherPartyName();
        otherPartyName.setTitle("Random");

        exceptionRecordSscs2 =
            ExceptionRecord.builder().ocrDataFields(ocrList).formType(FormType.SSCS2.getId()).ignoreWarnings(true).build();
        given(sscsJsonExtractor.extractJson(exceptionRecordSscs2)).willReturn(scannedData);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2, buildCaseWithChildMaintenanceWithOtherPartyNameAddress(
                CHILD_MAINTENANCE_NUMBER, OTHER_PARTY_ADDRESS_LINE1, OTHER_PARTY_ADDRESS_LINE2, OTHER_PARTY_ADDRESS_LINE3, OTHER_PARTY_POSTCODE, otherPartyName),
            false);

        assertEquals(0, response.getWarnings().size());
        assertEquals("Jerry Fisher", ((List<CcdValue<OtherParty>>) response.getTransformedCase().get("otherParties")).getFirst().getValue().getName().getFullNameNoTitle());
        @SuppressWarnings("unchecked")
        List<CcdValue<OtherParty>> otherParties = ((List<CcdValue<OtherParty>>) response.getTransformedCase().get("otherParties"));
        assertNotNull(otherParties.getFirst().getValue().getAddress());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void givenSscs2FormWithValidOtherPartyAndIgnoreWarningsTrue_thenNoWarningsShownAndOtherPartiesCreated() {
        Name otherPartyName = buildOtherPartyName();

        exceptionRecordSscs2 =
            ExceptionRecord.builder().ocrDataFields(ocrList).formType(FormType.SSCS2.getId()).ignoreWarnings(true).build();
        given(sscsJsonExtractor.extractJson(exceptionRecordSscs2)).willReturn(scannedData);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2, buildCaseWithChildMaintenanceWithOtherPartyNameAddress(
                CHILD_MAINTENANCE_NUMBER, OTHER_PARTY_ADDRESS_LINE1, OTHER_PARTY_ADDRESS_LINE2, OTHER_PARTY_ADDRESS_LINE3, OTHER_PARTY_POSTCODE, otherPartyName),
            false);

        assertEquals(0, response.getWarnings().size());
        assertEquals("Mr Jerry Fisher", ((List<CcdValue<OtherParty>>) response.getTransformedCase().get("otherParties")).getFirst().getValue().getName().getFullName());
    }

    @ParameterizedTest
    @CsvSource({", test2, test3, TS1 1ST",
        "test1, , , TS1 1ST",
        "test1, test2, test3,"
    })
    public void givenSscs2FormWithOtherPartyAddressFieldMissingAndIgnoreWarningsTrue_thenNoWarningsShownAddressNull(String line1, String line2, String line3, String postcode) {

        exceptionRecordSscs2 =
            ExceptionRecord.builder().ocrDataFields(ocrList).formType(FormType.SSCS2.getId()).ignoreWarnings(true).build();
        given(sscsJsonExtractor.extractJson(exceptionRecordSscs2)).willReturn(scannedData);

        CaseResponse response = validator.validateExceptionRecord(transformResponse,
            exceptionRecordSscs2, buildCaseWithChildMaintenanceWithOtherPartyNameAddress(
                CHILD_MAINTENANCE_NUMBER, line1, line2, line3, postcode, buildOtherPartyName()),
            false);

        assertEquals(0, response.getWarnings().size());
        @SuppressWarnings("unchecked")
        List<CcdValue<OtherParty>> otherParties = ((List<CcdValue<OtherParty>>) response.getTransformedCase().get("otherParties"));
        assertNotNull(otherParties);
        assertNotNull(otherParties.getFirst().getValue().getName());
        assertNull(otherParties.getFirst().getValue().getAddress());
    }

    private Object buildDocument(String filename) {
        List<SscsDocument> documentDetails = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder()
            .documentFileName(filename).documentLink(DocumentLink.builder().documentFilename(filename).build()).build();
        documentDetails.add(SscsDocument.builder().value(details).build());

        return documentDetails;
    }

    private Map<String, Object> buildMinimumAppealData(Appellant appellant, Boolean exceptionCaseType,
                                                       FormType formType) {
        return buildMinimumAppealDataWithMrnDateAndBenefitType(defaultMrnDetails, PIP.name(), appellant,
            buildMinimumRep(), null, exceptionCaseType, HEARING_TYPE_ORAL,
            HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build(), formType);
    }

    private Map<String, Object> buildMinimumAppealDataWithMrn(MrnDetails mrn, Appellant appellant,
                                                              Boolean exceptionCaseType,
                                                              FormType formType) {
        return buildMinimumAppealDataWithMrnDateAndBenefitType(mrn, ESA.name(), appellant, buildMinimumRep(), null,
            exceptionCaseType, HEARING_TYPE_PAPER, null, formType);
    }

    private Map<String, Object> buildMinimumAppealDataWithBenefitTypeWithAppealReasons(String benefitCode, Appellant appellant,
                                                                                       Boolean exceptionCaseType,
                                                                                       FormType formType,
                                                                                       AppealReasons appealReasons) {
        return buildMinimumAppealDataWithMrnDateFormTypeAndBenefitType(defaultMrnDetails, benefitCode, appellant,
            buildMinimumRep(), null, exceptionCaseType, HEARING_TYPE_ORAL,
            HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build(), formType, appealReasons);
    }

    private Map<String, Object> buildMinimumAppealDataWithBenefitType(String benefitCode, Appellant appellant,
                                                                      Boolean exceptionCaseType,
                                                                      FormType formType) {
        return buildMinimumAppealDataWithMrnDateAndBenefitType(defaultMrnDetails, benefitCode, appellant,
            buildMinimumRep(), null, exceptionCaseType, HEARING_TYPE_ORAL,
            HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build(), formType);
    }

    private Map<String, Object> buildMinimumAppealDataWithRepresentative(Appellant appellant,
                                                                         Representative representative,
                                                                         Boolean exceptionCaseType,
                                                                         FormType formType) {
        return buildMinimumAppealDataWithMrnDateAndBenefitType(defaultMrnDetails, PIP.name(), appellant, representative,
            null, exceptionCaseType, HEARING_TYPE_ORAL,
            HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build(), formType);
    }

    private Map<String, Object> buildMinimumAppealDataWithExcludedDate(String excludedDate, Appellant appellant,
                                                                       Boolean exceptionCaseType,
                                                                       FormType formType) {
        return buildMinimumAppealDataWithMrnDateAndBenefitType(defaultMrnDetails, PIP.name(), appellant,
            buildMinimumRep(), excludedDate, exceptionCaseType, HEARING_TYPE_ORAL, null, formType);
    }

    private Map<String, Object> buildMinimumAppealDataWithHearingType(String hearingType, Appellant appellant,
                                                                      Boolean exceptionCaseType,
                                                                      FormType formType) {
        return buildMinimumAppealDataWithMrnDateAndBenefitType(defaultMrnDetails, PIP.name(), appellant,
            buildMinimumRep(), null, exceptionCaseType, hearingType, null, formType);
    }

    private Map<String, Object> buildMinimumAppealDataWithHearingSubtype(HearingSubtype hearingSubtype,
                                                                         Appellant appellant,
                                                                         Boolean exceptionCaseType,
                                                                         FormType formType) {
        return buildMinimumAppealDataWithMrnDateAndBenefitType(defaultMrnDetails, PIP.name(), appellant,
            buildMinimumRep(), null, exceptionCaseType, HEARING_TYPE_ORAL, hearingSubtype, formType);
    }

    private Representative buildMinimumRep() {
        return Representative.builder().hasRepresentative(NO_LITERAL).build();
    }

    private Map<String, Object> buildMinimumAppealDataWithBenefitTypeAndFormType(String benefitCode,
                                                                                 Appellant appellant,
                                                                                 Boolean exceptionCaseType,
                                                                                 FormType formType) {
        return buildMinimumAppealDataWithMrnDateFormTypeAndBenefitType(defaultMrnDetails, benefitCode, appellant,
            buildMinimumRep(), null, exceptionCaseType, HEARING_TYPE_ORAL,
            HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build(), formType, null);
    }

    private Map<String, Object> buildMinimumAppealDataWithMrnDateAndBenefitType(MrnDetails mrn, String benefitCode,
                                                                                Appellant appellant,
                                                                                Representative representative,
                                                                                String excludeDates,
                                                                                Boolean exceptionCaseType,
                                                                                String hearingType,
                                                                                HearingSubtype hearingSubtype,
                                                                                FormType formType) {
        return buildMinimumAppealDataWithMrnDateFormTypeAndBenefitType(mrn, benefitCode, appellant, representative,
            excludeDates,
            exceptionCaseType, hearingType, hearingSubtype, formType, null);
    }

    private Map<String, Object> buildMinimumAppealDataWithMrnDateFormTypeAndBenefitType(MrnDetails mrn,
                                                                                        String benefitCode,
                                                                                        Appellant appellant,
                                                                                        Representative representative,
                                                                                        String excludeDates,
                                                                                        Boolean exceptionCaseType,
                                                                                        String hearingType,
                                                                                        HearingSubtype hearingSubtype,
                                                                                        FormType formType,
                                                                                        AppealReasons appealReasons) {
        Map<String, Object> dataMap = new HashMap<>();
        List<ExcludeDate> excludedDates = new ArrayList<>();
        excludedDates.add(ExcludeDate.builder().value(DateRange.builder().start(excludeDates).build()).build());
        dataMap.put("formType", formType);
        dataMap.put("appeal", Appeal.builder()
            .appealReasons(appealReasons)
            .mrnDetails(
                MrnDetails.builder().mrnDate(mrn.getMrnDate()).dwpIssuingOffice(mrn.getDwpIssuingOffice()).build())
            .benefitType(BenefitType.builder().code(benefitCode).build())
            .appellant(appellant)
            .rep(representative)
            .hearingOptions(HearingOptions.builder().excludeDates(excludedDates).build())
            .hearingType(hearingType)
            .hearingSubtype(hearingSubtype)
            .build());

        if (exceptionCaseType) {
            dataMap.put("bulkScanCaseReference", 123);
        }
        if (formType.equals(FormType.SSCS2)) {
            dataMap.put("childMaintenanceNumber", "123456");
        }
        return dataMap;
    }

    private Appellant buildAppellant(Boolean withAppointee) {
        return buildAppellantWithMobileNumberAndPostcode(withAppointee, VALID_MOBILE, VALID_POSTCODE);
    }

    private Appellant buildAppellantWithPostcode(String postcode) {
        return buildAppellantWithMobileNumberAndPostcode(false, VALID_MOBILE, postcode);
    }

    private Appellant buildAppellantWithMobileNumber(String mobileNumber) {
        return buildAppellantWithMobileNumberAndPostcode(false, mobileNumber, VALID_POSTCODE);
    }

    private Appellant buildAppellantWithMobileNumberAndPostcode(Boolean withAppointee, String mobileNumber,
                                                                String postcode) {
        Appointee appointee = withAppointee ? buildAppointee(VALID_MOBILE) : null;

        return Appellant.builder()
            .name(Name.builder().title("Mr").firstName("Bob").lastName("Smith").build())
            .address(
                Address.builder().line1("101 My Road").town("Brentwood").county("Essex").postcode(postcode).build())
            .identity(Identity.builder().nino("BB000000B").ibcaReference("A12A12").build())
            .contact(Contact.builder().mobile(mobileNumber).build())
            .appointee(appointee)
            .ibcRole("myself")
            .role(Role.builder().name("Paying parent").build()).build();
    }

    private Map<String, Object> buildCaseWithChildMaintenanceWithOtherPartyNameAddress(String childMaintenanceNumber, String line1, String line2, String line3, String postcode, Name otherPartyName) {
        Map<String, Object> datamap = buildMinimumAppealDataWithMrnDateFormTypeAndBenefitType(
            defaultMrnDetails,
            UC.getShortName(),
            buildAppellant(true),
            buildMinimumRep(),
            null,
            true,
            HEARING_TYPE_ORAL,
            HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build(),
            FormType.SSCS2,
            null);
        datamap.put("childMaintenanceNumber", childMaintenanceNumber);
        datamap.put("otherParties", Collections.singletonList(CcdValue.<OtherParty>builder().value(
                OtherParty.builder()
                    .name(otherPartyName)
                    .address(Address.builder()
                        .line1(line1)
                        .town(line2)
                        .county((line3 != null && !line3.equals("")) ? "." : line3)
                        .postcode(postcode)
                        .build())
                    .build())
            .build()));
        return datamap;
    }

    private Name buildOtherPartyName() {
        return Name.builder().title("Mr").firstName("Jerry").lastName("Fisher").build();
    }

    private Appointee buildAppointeeWithMobileNumber(String mobileNumber) {
        return buildAppointee(mobileNumber);
    }

    private Appointee buildAppointee(String mobileNumber) {

        return Appointee.builder()
            .name(Name.builder().title("Mr").firstName("Tim").lastName("Garwood").build())
            .address(Address.builder().line1("101 My Road").town("Gidea Park").county("Essex").postcode(VALID_POSTCODE)
                .build())
            .identity(Identity.builder().build())
            .contact(Contact.builder().mobile(mobileNumber).build())
            .build();
    }

    private Representative buildRepresentative() {

        return Representative.builder()
            .hasRepresentative("Yes")
            .organisation("Bob the builders Ltd")
            .name(Name.builder().title("Mr").firstName("Bob").lastName("Smith").build())
            .address(
                Address.builder().line1("101 My Road").town("Brentwood").county("Essex").postcode("CM13 1HG").build())
            .build();
    }
}