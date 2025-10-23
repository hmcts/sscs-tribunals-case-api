package uk.gov.hmcts.reform.sscs.bulkscan.transformers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.*;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.APPEAL_GROUNDS;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.APPEAL_GROUNDS_2;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.BENEFIT_TYPE_OTHER;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.DEFAULT_SIGN_LANGUAGE;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.EMAIL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_EXCLUDE_DATES_MISSING;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_OPTIONS_DIALECT_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_OPTIONS_LANGUAGE_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_OPTIONS_SIGN_LANGUAGE_INTERPRETER_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_OPTIONS_SIGN_LANGUAGE_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TELEPHONE_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TYPE_FACE_TO_FACE_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TYPE_ORAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TYPE_PAPER;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TYPE_TELEPHONE_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_TYPE_VIDEO_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_VIDEO_EMAIL_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IBC_ROLE_FOR_DECEASED;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IBC_ROLE_FOR_LACKING_CAPACITY;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IBC_ROLE_FOR_POA;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IBC_ROLE_FOR_SELF;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IBC_ROLE_FOR_U18;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.ISSUING_OFFICE;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IS_BENEFIT_TYPE_OTHER;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IS_BENEFIT_TYPE_TAX_CREDIT;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IS_HEARING_TYPE_ORAL_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IS_HEARING_TYPE_PAPER_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.MOBILE;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.MRN_DATE;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.NINO;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.NO_LITERAL;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.PERSON1_VALUE;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.PERSON_1_CHILD_MAINTENANCE_NUMBER;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.PHONE;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.YES_LITERAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.ESA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.FormType.SSCS1PEU;
import static uk.gov.hmcts.reform.sscs.ccd.domain.FormType.SSCS2;
import static uk.gov.hmcts.reform.sscs.ccd.domain.FormType.SSCS5;
import static uk.gov.hmcts.reform.sscs.ccd.domain.FormType.SSCS8;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService.normaliseNino;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.util.Assert;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.InputScannedDoc;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.OcrDataField;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ScannedData;
import uk.gov.hmcts.reform.sscs.bulkscan.constants.BenefitTypeIndicator;
import uk.gov.hmcts.reform.sscs.bulkscan.constants.BenefitTypeIndicatorSscs1U;
import uk.gov.hmcts.reform.sscs.bulkscan.constants.BenefitTypeIndicatorSscs5;
import uk.gov.hmcts.reform.sscs.bulkscan.helper.AppealPostcodeHelper;
import uk.gov.hmcts.reform.sscs.bulkscan.helper.SscsDataHelper;
import uk.gov.hmcts.reform.sscs.bulkscan.json.SscsJsonExtractor;
import uk.gov.hmcts.reform.sscs.bulkscan.service.CaseManagementLocationService;
import uk.gov.hmcts.reform.sscs.bulkscan.service.FuzzyMatcherService;
import uk.gov.hmcts.reform.sscs.bulkscan.validators.FormTypeValidator;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasonDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasons;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppellantRole;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.Role;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.CaseResponse;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

public class SscsCaseTransformerTest {

    private static final String UNIVERSAL_CREDIT = "Universal Credit";

    @Mock
    private SscsJsonExtractor sscsJsonExtractor;

    @Mock
    FormTypeValidator formTypeValidator;

    FormTypeValidator formTypeValidator2;

    @Mock
    private IdamService idamService;

    @Mock
    private CcdService ccdService;

    @Mock
    private AirLookupService airLookupService;

    @Mock
    private AppealPostcodeHelper appealPostcodeHelper;

    @Mock
    private CaseManagementLocationService caseManagementLocationService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;
    private SscsCaseTransformer transformer;
    private SscsCaseTransformer transformer2;

    private final List<OcrDataField> ocrList = new ArrayList<>();

    private Map<String, Object> pairs;

    private ExceptionRecord exceptionRecord;

    private ExceptionRecord sscs1UExceptionRecord;

    private ExceptionRecord nullFormExceptionRecord;

    private ExceptionRecord sscs2ExceptionRecord;
    private ExceptionRecord sscs5ExceptionRecord;

    @BeforeEach
    public void setup() {
        openMocks(this);
        pairs = new HashMap<>();
        DwpAddressLookupService dwpAddressLookupService = new DwpAddressLookupService();

        formTypeValidator2 = new FormTypeValidator(sscsJsonExtractor);

        SscsDataHelper sscsDataHelper = new SscsDataHelper(
            null,
            airLookupService,
            dwpAddressLookupService,
            true);

        transformer = new SscsCaseTransformer(
            ccdService,
            idamService,
            sscsDataHelper,
            sscsJsonExtractor,
            new FuzzyMatcherService(),
            appealPostcodeHelper,
            formTypeValidator,
            dwpAddressLookupService,
            caseManagementLocationService,
            regionalProcessingCenterService,
            false);

        transformer2 = new SscsCaseTransformer(
            ccdService,
            idamService,
            sscsDataHelper,
            sscsJsonExtractor,
            new FuzzyMatcherService(),
            appealPostcodeHelper,
            formTypeValidator2,
            dwpAddressLookupService,
            caseManagementLocationService,
            regionalProcessingCenterService,
            false);

        pairs.put("is_hearing_type_oral", IS_HEARING_TYPE_ORAL);
        pairs.put("is_hearing_type_paper", IS_HEARING_TYPE_PAPER);
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), true);

        exceptionRecord = ExceptionRecord.builder().ocrDataFields(ocrList).id(null).exceptionRecordId("123456").formType(
            SSCS1PEU.getId()).build();
        given(formTypeValidator.validate(exceptionRecord.getExceptionRecordId(), exceptionRecord)).willReturn(CaseResponse.builder().build());
        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).build());

        sscs1UExceptionRecord = ExceptionRecord.builder().ocrDataFields(ocrList).id(null).exceptionRecordId("123456").formType(FormType.SSCS1U.getId()).build();
        given(sscsJsonExtractor.extractJson(sscs1UExceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).build());
        given(formTypeValidator.validate(sscs1UExceptionRecord.getExceptionRecordId(), sscs1UExceptionRecord)).willReturn(CaseResponse.builder().build());
        nullFormExceptionRecord = ExceptionRecord.builder().ocrDataFields(ocrList).id(null).exceptionRecordId("123456").formType(FormType.SSCS1U.getId()).build();
        given(sscsJsonExtractor.extractJson(sscs1UExceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).build());

        given(formTypeValidator.validate(nullFormExceptionRecord.getExceptionRecordId(), nullFormExceptionRecord)).willReturn(CaseResponse.builder().build());
        sscs2ExceptionRecord = ExceptionRecord.builder().ocrDataFields(ocrList).id(null).exceptionRecordId("123456").formType(
            SSCS2.getId()).build();
        given(sscsJsonExtractor.extractJson(sscs2ExceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).build());

        given(formTypeValidator.validate(sscs2ExceptionRecord.getExceptionRecordId(), sscs2ExceptionRecord)).willReturn(CaseResponse.builder().build());
        sscs5ExceptionRecord = ExceptionRecord.builder().id("null").exceptionRecordId("123456").ocrDataFields(ocrList).id(null).exceptionRecordId("123456").formType(
            SSCS5.getId()).build();
        given(formTypeValidator.validate(sscs5ExceptionRecord.getExceptionRecordId(), sscs5ExceptionRecord)).willReturn(CaseResponse.builder().build());
        given(sscsJsonExtractor.extractJson(sscs5ExceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).build());

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @ParameterizedTest
    @CsvSource({"Bereavement Benefit, Pensions Dispute Resolution Team",
        "Carer's Allowance, Carer’s Allowance Dispute Resolution Team",
        "Maternity Allowance, Walsall Benefit Centre",
        "Bereavement Support Payment Scheme, Pensions Dispute Resolution Team"})
    public void givenBenefitTypeIsOtherBenefitAutoOfficeWithAnyOffice_thenCorrectOfficeIsReturned(String benefit, String office) {

        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.OTHER.getIndicatorString(), true);
        pairs.put(BENEFIT_TYPE_OTHER, benefit);
        pairs.put(ISSUING_OFFICE, "Anything");
        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertTrue(result.getErrors().isEmpty());
        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        assertEquals(office, appeal.getMrnDetails().getDwpIssuingOffice());
    }

    @Test
    public void givenInvalidBenefitTypePairings_thenReturnAnError() {
        pairs.put(BenefitTypeIndicator.ESA.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), true);
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertFalse(result.getErrors().isEmpty());
        assertEquals("is_benefit_type_pip and is_benefit_type_esa have contradicting values", result.getErrors().getFirst());
    }

    @Test
    public void givenInvalidBenefitTypePairingsForSscs5_thenReturnAnError() {
        pairs.put(BenefitTypeIndicatorSscs5.TAX_CREDIT.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicatorSscs5.GUARDIANS_ALLOWANCE.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicatorSscs5.TAX_FREE_CHILDCARE.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicatorSscs5.HOME_RESPONSIBILITIES_PROTECTION.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicatorSscs5.CHILD_BENEFIT.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicatorSscs5.THIRTY_HOURS_FREE_CHILDCARE.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicatorSscs5.GUARANTEED_MINIMUM_PENSION.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicatorSscs5.NATIONAL_INSURANCE_CREDITS.getIndicatorString(), true);
        CaseResponse result = transformer.transformExceptionRecord(sscs5ExceptionRecord, false);
        assertFalse(result.getErrors().isEmpty());
        assertEquals("is_benefit_type_tax_credit, is_benefit_type_guardians_allowance, is_benefit_type_tax_free_childcare, is_benefit_type_home_responsibilities_protection, is_benefit_type_child_benefit, is_benefit_type_30_hours_tax_free_childcare, is_benefit_type_guaranteed_minimum_pension and is_benefit_type_national_insurance_credits have contradicting values", result.getErrors().getFirst());
    }

    @Test
    public void givenTwoSscs5BenefitTypesAreTrue_thenReturnAnError() {
        pairs.put(BenefitTypeIndicatorSscs5.TAX_CREDIT.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicatorSscs5.GUARDIANS_ALLOWANCE.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicatorSscs5.TAX_FREE_CHILDCARE.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.HOME_RESPONSIBILITIES_PROTECTION.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.CHILD_BENEFIT.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.THIRTY_HOURS_FREE_CHILDCARE.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.GUARANTEED_MINIMUM_PENSION.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.NATIONAL_INSURANCE_CREDITS.getIndicatorString(), false);
        CaseResponse result = transformer.transformExceptionRecord(sscs5ExceptionRecord, false);
        assertEquals(1, result.getErrors().size());
        assertEquals("is_benefit_type_tax_credit and is_benefit_type_guardians_allowance have contradicting values", result.getErrors().getFirst());
    }

    @Test
    public void givenAllSscs5BenefitTypesAreFalse_thenReturnAnError() {
        pairs.put(BenefitTypeIndicatorSscs5.TAX_CREDIT.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.GUARDIANS_ALLOWANCE.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.TAX_FREE_CHILDCARE.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.HOME_RESPONSIBILITIES_PROTECTION.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.CHILD_BENEFIT.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.THIRTY_HOURS_FREE_CHILDCARE.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.GUARANTEED_MINIMUM_PENSION.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs5.NATIONAL_INSURANCE_CREDITS.getIndicatorString(), false);
        CaseResponse result = transformer.transformExceptionRecord(sscs5ExceptionRecord, false);
        assertEquals(1, result.getErrors().size());
        assertEquals("is_benefit_type_tax_credit, is_benefit_type_guardians_allowance, is_benefit_type_tax_free_childcare, is_benefit_type_home_responsibilities_protection, is_benefit_type_child_benefit, is_benefit_type_30_hours_tax_free_childcare, is_benefit_type_guaranteed_minimum_pension and is_benefit_type_national_insurance_credits fields are empty or false", result.getErrors().getFirst());
    }

    @Test
    public void givenAllSscs5BenefitTypesAreMissing_thenReturnAnError() {
        CaseResponse result = transformer.transformExceptionRecord(sscs5ExceptionRecord, false);
        assertEquals(1, result.getErrors().size());
        assertEquals("is_benefit_type_tax_credit, is_benefit_type_guardians_allowance, is_benefit_type_tax_free_childcare, is_benefit_type_home_responsibilities_protection, is_benefit_type_child_benefit, is_benefit_type_30_hours_tax_free_childcare, is_benefit_type_guaranteed_minimum_pension and is_benefit_type_national_insurance_credits fields are empty or false", result.getErrors().getFirst());
    }

    @Test
    public void givenNoTrueBenefitTypeIndicatorsOrOtherBenefitType_thenReturnAnError() {
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.OTHER.getIndicatorString(), false);
        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertEquals(1, result.getErrors().size());
        assertEquals("is_benefit_type_pip, is_benefit_type_esa, is_benefit_type_uc and benefit_type_other fields are empty", result.getErrors().getFirst());
    }

    @Test
    public void givenNoBenefitTypePairings_thenReturnAnError() {
        pairs.remove(BenefitTypeIndicator.PIP.getIndicatorString());
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.OTHER.getIndicatorString(), false);
        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertEquals(1, result.getErrors().size());
        assertEquals("is_benefit_type_pip, is_benefit_type_esa, is_benefit_type_uc and benefit_type_other fields are empty", result.getErrors().getFirst());
    }

    @Test
    public void givenNullFormType_thenNoError() {
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.OTHER.getIndicatorString(), false);
        CaseResponse result = transformer.transformExceptionRecord(nullFormExceptionRecord, false);
        assertEquals(0, result.getErrors().size());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    public void givenBenefitTypeIsDefinedWithTrueFalse_thenCheckCorrectCodeIsReturned(boolean isPip) {
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), isPip);
        pairs.put(BenefitTypeIndicator.ESA.getIndicatorString(), !isPip);
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertTrue(result.getErrors().isEmpty());
        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        Benefit expectedBenefit = isPip ? PIP : ESA;
        assertEquals(expectedBenefit.name(), appeal.getBenefitType().getCode());
    }

    @Test
    public void givenBenefitTypeIsOtherAttendanceAllowance_thenCorrectCodeIsReturned() {
        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.OTHER.getIndicatorString(), true);
        pairs.put(BENEFIT_TYPE_OTHER, "Attendance Allowance");
        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertTrue(result.getErrors().isEmpty());
        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        Benefit expectedBenefit = Benefit.ATTENDANCE_ALLOWANCE;
        assertEquals(expectedBenefit.getShortName(), appeal.getBenefitType().getCode());
    }

    @Test
    public void givenSscs2_thenBenefitTypeIsChildSupport() {
        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.OTHER.getIndicatorString(), true);
        CaseResponse result = transformer.transformExceptionRecord(sscs2ExceptionRecord, false);
        assertTrue(result.getErrors().isEmpty());
        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        Benefit expectedBenefit = Benefit.CHILD_SUPPORT;
        assertEquals(expectedBenefit.getShortName(), appeal.getBenefitType().getCode());
        assertEquals("Child Maintenance Service Group", appeal.getMrnDetails().getDwpIssuingOffice());
    }

    @ParameterizedTest
    @CsvSource({"Attendance Allowance, attendanceAllowance", "Bereavement Benefit, bereavementBenefit", "Carer's Allowance, carersAllowance", "Disability Living Allowance, DLA",
        "Income Support, incomeSupport", " Industrial Injuries Disablement Benefit, industrialInjuriesDisablement", "Job Seekers Allowance, JSA",
        "Maternity Allowance, maternityAllowance", "Social Fund, socialFund", "Bereavement Support Payment Scheme, bereavementSupportPaymentScheme",
        "Industrial Death Benefit, industrialDeathBenefit", "Pension Credit, pensionCredit", "Retirement Pension, retirementPension",})
    public void givenBenefitTypeIsOtherWithValidType_thenCorrectCodeIsReturned(String benefitDescription, String shortName) {
        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.OTHER.getIndicatorString(), true);
        pairs.put(BENEFIT_TYPE_OTHER, benefitDescription);
        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertTrue(result.getErrors().isEmpty());
        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        assertEquals(shortName, appeal.getBenefitType().getCode());
    }

    @ParameterizedTest
    @CsvSource({"Attendance Allowance, attendanceAllowance", "Bereavement Benefit, bereavementBenefit", "Carer's Allowance, carersAllowance", "Disability Living Allowance, DLA",
        "Income Support, incomeSupport", " Industrial Injuries Disablement Benefit, industrialInjuriesDisablement", "Job Seekers Allowance, JSA",
        "Maternity Allowance, maternityAllowance", "Social Fund, socialFund", "Bereavement Support Payment Scheme, bereavementSupportPaymentScheme",
        "Industrial Death Benefit, industrialDeathBenefit", "Pension Credit, pensionCredit", "Retirement Pension, retirementPension",})
    public void givenBenefitTypeIsOtherNotTickedWithValidType_thenCorrectCodeIsReturned(String benefitDescription, String shortName) {
        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.OTHER.getIndicatorString(), false);
        pairs.put(BENEFIT_TYPE_OTHER, benefitDescription);
        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertTrue(result.getErrors().isEmpty());
        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        assertEquals(shortName, appeal.getBenefitType().getCode());
    }

    @ParameterizedTest
    @CsvSource({"TAX_CREDIT, taxCredit, Tax Credit Office", "GUARDIANS_ALLOWANCE, guardiansAllowance, Child Benefit Office",
        "TAX_FREE_CHILDCARE, taxFreeChildcare, Childcare Service HMRC", "HOME_RESPONSIBILITIES_PROTECTION, homeResponsibilitiesProtection, PT Operations North East England",
        "CHILD_BENEFIT, childBenefit, Child Benefit Office", "THIRTY_HOURS_FREE_CHILDCARE, thirtyHoursFreeChildcare, Childcare Service HMRC",
        "GUARANTEED_MINIMUM_PENSION, guaranteedMinimumPension, PT Operations North East England", "NATIONAL_INSURANCE_CREDITS, nationalInsuranceCredits, PT Operations North East England"})
    public void givenBenefitTypeIsSscs5_thenCorrectCodeIsReturned(BenefitTypeIndicatorSscs5 benefitType, String expectedBenefitCode, String issuingOffice) {
        pairs.put(benefitType.getIndicatorString(), true);
        pairs.remove(BenefitTypeIndicator.PIP.getIndicatorString());
        CaseResponse result = transformer.transformExceptionRecord(sscs5ExceptionRecord, false);
        assertTrue(result.getErrors().isEmpty());
        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        assertEquals(expectedBenefitCode, appeal.getBenefitType().getCode());
        assertEquals(issuingOffice, appeal.getMrnDetails().getDwpIssuingOffice());
    }

    @Test
    public void givenBenefitTypeIsOtherWithInvalidType_thenErrorMessageReturned() {
        pairs.remove("is_benefit_type_pip");
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), false);

        pairs.put(BenefitTypeIndicatorSscs1U.OTHER.getIndicatorString(), true);
        pairs.put(BENEFIT_TYPE_OTHER, "Not a valid type");
        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertEquals(1, result.getErrors().size());
        assertEquals("benefit_type_other is invalid", result.getErrors().getFirst());
    }

    @Test
    public void givenBenefitTypeIsOther_thenNullCodeIsReturned() {
        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.OTHER.getIndicatorString(), true);
        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertEquals(1, result.getErrors().size());
        assertEquals("benefit_type_other is invalid", result.getErrors().getFirst());
    }

    @Test
    public void givenInvalidBenefitTypePipWithOtherBenefit_thenOneErrorMessage() {
        pairs.put(BENEFIT_TYPE_OTHER, "any value at all");
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.OTHER.getIndicatorString(), false);

        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertEquals(1, result.getErrors().size());
        assertEquals("is_benefit_type_pip and benefit_type_other have contradicting values", result.getErrors().getFirst());
    }

    @Test
    public void givenBenefitTypePipWithIsOtherBenefit_thenErrorMessage() {
        pairs.put(IS_BENEFIT_TYPE_OTHER, true);
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), false);
        pairs.put(BENEFIT_TYPE_OTHER, "Attendance Allowance");
        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertEquals(1, result.getErrors().size());
        assertEquals("is_benefit_type_pip and benefit_type_other have contradicting values", result.getErrors().getFirst());
    }

    @Test
    public void givenBenefitTypeEsaAndUcWithIsOtherBenefitYes_thenErrorMessage() {
        pairs.remove("is_benefit_type_pip");
        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), "No");
        pairs.put(IS_BENEFIT_TYPE_OTHER, "Yes");
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), "Yes");
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), "Yes");
        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertEquals(1, result.getErrors().size());
        assertEquals("is_benefit_type_esa, is_benefit_type_uc and is_benefit_type_other have contradicting values", result.getErrors().getFirst());
    }

    @Test
    public void givenBenefitTypeEsaAndUcWithIsOtherBenefitTrue_thenErrorMessage() {
        pairs.remove("is_benefit_type_pip");
        pairs.put(BenefitTypeIndicatorSscs1U.PIP.getIndicatorString(), false);
        pairs.put(IS_BENEFIT_TYPE_OTHER, true);
        pairs.put(BenefitTypeIndicatorSscs1U.ESA.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicatorSscs1U.UC.getIndicatorString(), true);
        CaseResponse result = transformer.transformExceptionRecord(sscs1UExceptionRecord, false);
        assertEquals(1, result.getErrors().size());
        assertEquals("is_benefit_type_esa, is_benefit_type_uc and is_benefit_type_other have contradicting values", result.getErrors().getFirst());
    }

    @ParameterizedTest
    @CsvSource({"Yes", "No"})
    public void givenBenefitTypeIsDefinedWithYesNo_thenCheckCorrectCodeIsReturned(String isPip) {
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), isPip);
        pairs.put(BenefitTypeIndicator.ESA.getIndicatorString(), isPip.equals("Yes") ? "No" : "Yes");
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertTrue(result.getErrors().isEmpty());
        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        Benefit expectedBenefit = isPip.equals("Yes") ? PIP : ESA;
        assertEquals(expectedBenefit.name(), appeal.getBenefitType().getCode());
    }

    @Test
    public void benefitTypeIsDefinedByDescriptionFieldWhenIsEsaOrIsPipIsNotSet() {
        pairs.put("benefit_type_description", BENEFIT_TYPE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().isEmpty());
        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        assertEquals(BENEFIT_TYPE, appeal.getBenefitType().getCode());
    }

    @Test
    public void givenBenefitTypeIsMisspelt_thenFuzzyMatchStillFindsCorrectType() {
        pairs.put("benefit_type_description", "Personal misspelt payment");
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertTrue(result.getErrors().isEmpty());
        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        assertEquals(BENEFIT_TYPE, appeal.getBenefitType().getCode());
    }

    @ParameterizedTest
    @CsvSource({"person1", "person2", "representative"})
    public void canHandleAddressWithoutAddressLine4(String personType) {
        Address expectedAddress = Address.builder()
            .line1("10 my street")
            .town("town")
            .county("county")
            .postcode(APPELLANT_POSTCODE)
            .build();
        for (String person : Arrays.asList("person1", personType)) {
            pairs.put(person + "_address_line1", expectedAddress.getLine1());
            pairs.put(person + "_address_line2", expectedAddress.getTown());
            pairs.put(person + "_address_line3", expectedAddress.getCounty());
            pairs.put(person + "_postcode", expectedAddress.getPostcode());
        }

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        Address actual = personType.equals("representative") ? appeal.getRep().getAddress() :
            personType.equals("person2") ? appeal.getAppellant().getAppointee().getAddress() : appeal.getAppellant().getAddress();
        assertEquals(expectedAddress, actual);
    }

    @ParameterizedTest
    @CsvSource({"person1", "person2", "representative"})
    public void givenAddressLine3IsBlankAndAddressLine4IsNotPresent_thenAddressLine3PopulatedWithDot(String personType) {
        Address expectedAddress = Address.builder()
            .line1("10 my street")
            .town("town")
            .county(".")
            .postcode(APPELLANT_POSTCODE)
            .build();
        for (String person : Arrays.asList("person1", personType)) {
            pairs.remove(person + "_address_line4");
            pairs.put(person + "_address_line1", expectedAddress.getLine1());
            pairs.put(person + "_address_line2", expectedAddress.getTown());
            pairs.put(person + "_address_line3", "");
            pairs.put(person + "_postcode", expectedAddress.getPostcode());
        }
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        Address actual = personType.equals("representative") ? appeal.getRep().getAddress() :
            personType.equals("person2") ? appeal.getAppellant().getAppointee().getAddress() : appeal.getAppellant().getAddress();
        assertEquals(expectedAddress, actual);
    }

    @ParameterizedTest
    @CsvSource({"person1", "person2", "representative"})
    public void givenAddressLine3IsNullAndAddressLine4IsNotPresent_thenAddressLine3PopulatedWithDot(String personType) {
        Address expectedAddress = Address.builder()
            .line1("10 my street")
            .town("town")
            .county(".")
            .postcode(APPELLANT_POSTCODE)
            .build();
        for (String person : Arrays.asList("person1", personType)) {
            pairs.remove(person + "_address_line4");
            pairs.put(person + "_address_line1", expectedAddress.getLine1());
            pairs.put(person + "_address_line2", expectedAddress.getTown());
            pairs.put(person + "_address_line3", null);
            pairs.put(person + "_postcode", expectedAddress.getPostcode());
        }
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        Address actual = personType.equals("representative") ? appeal.getRep().getAddress() :
            personType.equals("person2") ? appeal.getAppellant().getAppointee().getAddress() : appeal.getAppellant().getAddress();
        assertEquals(expectedAddress, actual);
    }

    @ParameterizedTest
    @CsvSource({"person1", "person2", "representative"})
    public void givenAddressLine2And3AreNullAndAddressLine4IsNotPresent_thenAddressLine3NotPopulatedWithDot(String personType) {
        Address expectedAddress = Address.builder()
            .line1("10 my street")
            .town(null)
            .county(null)
            .postcode(APPELLANT_POSTCODE)
            .build();
        for (String person : Arrays.asList("person1", personType)) {
            pairs.remove(person + "_address_line4");
            pairs.put(person + "_address_line1", expectedAddress.getLine1());
            pairs.put(person + "_address_line2", null);
            pairs.put(person + "_address_line3", null);
            pairs.put(person + "_postcode", expectedAddress.getPostcode());
        }
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        Address actual = personType.equals("representative") ? appeal.getRep().getAddress() :
            personType.equals("person2") ? appeal.getAppellant().getAppointee().getAddress() : appeal.getAppellant().getAddress();
        assertEquals(expectedAddress, actual);
    }

    @ParameterizedTest
    @CsvSource({"person1", "person2", "representative"})
    public void givenAddressLine3IsBlankAndAddressLine4IsPresent_thenAddressLine3NotPopulatedWithDot(String personType) {
        Address expectedAddress = Address.builder()
            .line1("10 my street")
            .line2("line2 address")
            .town(null)
            .county("county")
            .postcode(APPELLANT_POSTCODE)
            .build();
        for (String person : Arrays.asList("person1", personType)) {
            pairs.put(person + "_address_line1", expectedAddress.getLine1());
            pairs.put(person + "_address_line2", expectedAddress.getLine2());
            pairs.put(person + "_address_line3", "");
            pairs.put(person + "_address_line4", expectedAddress.getCounty());
            pairs.put(person + "_postcode", expectedAddress.getPostcode());
        }
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        Address actual = personType.equals("representative") ? appeal.getRep().getAddress() :
            personType.equals("person2") ? appeal.getAppellant().getAppointee().getAddress() : appeal.getAppellant().getAddress();
        assertEquals(expectedAddress, actual);
        assertEquals(0, result.getErrors().size());
        assertEquals(0, result.getWarnings().size());
    }

    @ParameterizedTest
    @CsvSource({"Yes", "No"})
    public void willGenerateSubscriptionsWithEmailAndPhoneAndSubscribeToEmail(String subscribeSms) {

        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);

        pairs.put("person1_want_sms_notifications", subscribeSms.equals("Yes"));
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        Subscriptions subscriptions = (Subscriptions) result.getTransformedCase().get("subscriptions");

        Subscription expectedSubscription = Subscription.builder()
            .wantSmsNotifications(subscribeSms)
            .subscribeSms(subscribeSms)
            .mobile(APPELLANT_MOBILE)
            .email(APPELLANT_EMAIL)
            .subscribeEmail(YES_LITERAL)
            .tya(subscriptions.getAppellantSubscription().getTya())
            .build();

        assertEquals(expectedSubscription, subscriptions.getAppellantSubscription());
    }

    @ParameterizedTest
    @CsvSource({"Yes", "No"})
    public void willGenerateSubscriptionsWithEmailAndPhoneAndNotSubscribeToEmail(String subscribeSms) {

        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);

        pairs.put("person1_want_sms_notifications", subscribeSms.equals("Yes"));
        pairs.put("person1_mobile", APPELLANT_MOBILE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        Subscriptions subscriptions = (Subscriptions) result.getTransformedCase().get("subscriptions");

        Subscription expectedSubscription = Subscription.builder()
            .wantSmsNotifications(subscribeSms)
            .subscribeSms(subscribeSms)
            .mobile(APPELLANT_MOBILE)
            .subscribeEmail(NO_LITERAL)
            .tya(subscriptions.getAppellantSubscription().getTya())
            .build();

        assertEquals(expectedSubscription, subscriptions.getAppellantSubscription());
    }

    @Test
    public void givenKeyValuePairsWithPerson1AndPipBenefitType_thenBuildAnAppealWithAppellant() {

        pairs.put("benefit_type_description", BENEFIT_TYPE);
        pairs.put("mrn_date", MRN_DATE_VALUE);
        pairs.put("office", OFFICE);
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_phone", APPELLANT_PHONE);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("person1_dob", APPELLANT_DATE_OF_BIRTH);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put(APPEAL_GROUNDS, APPEAL_REASON);
        pairs.put("person1_nino", APPELLANT_NINO);
        pairs.put("representative_company", REPRESENTATIVE_NAME);
        pairs.put("representative_address_line1", REPRESENTATIVE_ADDRESS_LINE1);
        pairs.put("representative_address_line2", REPRESENTATIVE_ADDRESS_LINE2);
        pairs.put("representative_address_line3", REPRESENTATIVE_ADDRESS_LINE3);
        pairs.put("representative_address_line4", REPRESENTATIVE_ADDRESS_LINE4);
        pairs.put("representative_postcode", REPRESENTATIVE_POSTCODE);
        pairs.put("representative_phone", REPRESENTATIVE_PHONE_NUMBER);
        pairs.put("representative_email", REPRESENTATIVE_EMAIL);
        pairs.put("representative_title", REPRESENTATIVE_PERSON_TITLE);
        pairs.put("representative_first_name", REPRESENTATIVE_PERSON_FIRST_NAME);
        pairs.put("representative_last_name", REPRESENTATIVE_PERSON_LAST_NAME);
        pairs.put("appeal_late_reason", APPEAL_LATE_REASON);
        pairs.put("hearing_options_exclude_dates", HEARING_OPTIONS_EXCLUDE_DATES);
        pairs.put("hearing_options_hearing_loop", HEARING_LOOP);
        pairs.put("hearing_options_language_type", HEARING_OPTIONS_LANGUAGE_TYPE);
        pairs.put("agree_less_hearing_notice", AGREE_LESS_HEARING_NOTICE);
        pairs.put("signature_name", SIGNATURE_NAME);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertThat(result.getTransformedCase().get("appeal"))
            .usingRecursiveComparison()
            .ignoringFields("appellant.id", "rep.id")
            .isEqualTo(buildTestAppealData());
        assertEquals(BENEFIT_CODE, result.getTransformedCase().get("benefitCode"));
        assertEquals(ISSUE_CODE, result.getTransformedCase().get("issueCode"));
        assertEquals(CASE_CODE, result.getTransformedCase().get("caseCode"));
        assertEquals(DWP_REGIONAL_CENTRE, result.getTransformedCase().get("dwpRegionalCentre"));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenSscs8IbcInUk_thenBuildAnAppealWithIbcRoleAndIbcaReference() {
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_phone", APPELLANT_PHONE);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("person1_dob", APPELLANT_DATE_OF_BIRTH);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put(APPEAL_GROUNDS, APPEAL_REASON);
        pairs.put("person1_ibca_reference", APPELLANT_IBCA_REFERENCE);
        pairs.put(IBC_ROLE_FOR_SELF, true);
        pairs.put("form_type", SSCS8_FORM_TYPE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(SSCS8, result.getTransformedCase().get("formType"));
        Appeal transformedAppeal = (Appeal) result.getTransformedCase().get("appeal");
        assertNotNull(transformedAppeal);
        assertEquals(APPELLANT_IBC_ROLE_FOR_SELF, transformedAppeal.getAppellant().getIbcRole());
        assertEquals(APPELLANT_IBCA_REFERENCE, transformedAppeal.getAppellant().getIdentity().getIbcaReference());
        Address address = transformedAppeal.getAppellant().getAddress();
        assertNotNull(address);
        Address expectedAddress = Address.builder().line1(APPELLANT_ADDRESS_LINE1).line2(APPELLANT_ADDRESS_LINE2).town(APPELLANT_ADDRESS_LINE3).postcode(APPELLANT_POSTCODE).inMainlandUk(YesNo.YES).build();
        assertEquals(expectedAddress, address);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenPortOfEntry_thenBuildAnAppealWithAddressWithCountryPortOfEntry() {
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_Country", APPELLANT_ADDRESS_COUNTRY);
        pairs.put("person1_port_of_entry", APPELLANT_PORT_OF_ENTRY);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_ibca_reference", APPELLANT_IBCA_REFERENCE);
        pairs.put(IBC_ROLE_FOR_SELF, true);
        pairs.put("form_type", SSCS8_FORM_TYPE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(SSCS8, result.getTransformedCase().get("formType"));
        Appeal transformedAppeal = (Appeal) result.getTransformedCase().get("appeal");
        assertNotNull(transformedAppeal);
        assertEquals(APPELLANT_IBC_ROLE_FOR_SELF, transformedAppeal.getAppellant().getIbcRole());
        assertEquals(APPELLANT_IBCA_REFERENCE, transformedAppeal.getAppellant().getIdentity().getIbcaReference());
        Address address = transformedAppeal.getAppellant().getAddress();
        assertNotNull(address);
        Address expectedAddress = Address.builder().line1(APPELLANT_ADDRESS_LINE1).line2(APPELLANT_ADDRESS_LINE2).town(APPELLANT_ADDRESS_LINE3).country(APPELLANT_ADDRESS_COUNTRY).postcode(APPELLANT_POSTCODE).portOfEntry(APPELLANT_PORT_OF_ENTRY).inMainlandUk(YesNo.NO).build();
        assertEquals(expectedAddress, address);
        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        IBC_ROLE_FOR_SELF + "," + APPELLANT_IBC_ROLE_FOR_SELF,
        IBC_ROLE_FOR_U18 + "," + APPELLANT_IBC_ROLE_FOR_U18,
        IBC_ROLE_FOR_DECEASED + "," + APPELLANT_IBC_ROLE_FOR_DECEASED,
        IBC_ROLE_FOR_POA + "," + APPELLANT_IBC_ROLE_FOR_POA,
        IBC_ROLE_FOR_LACKING_CAPACITY + "," + APPELLANT_IBC_ROLE_FOR_LACKING_CAPACITY
    })
    public void givenChosenIbcRole_thenBuildAnAppealWithIbcRoleSet(String ibcRoleBoolean, String ibcRoleValue) {
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_Country", APPELLANT_ADDRESS_COUNTRY);
        pairs.put("person1_port_of_entry", APPELLANT_PORT_OF_ENTRY);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_ibca_reference", APPELLANT_IBCA_REFERENCE);
        pairs.put(ibcRoleBoolean, true);
        pairs.put("form_type", SSCS8_FORM_TYPE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(SSCS8, result.getTransformedCase().get("formType"));
        Appeal transformedAppeal = (Appeal) result.getTransformedCase().get("appeal");
        assertNotNull(transformedAppeal);
        assertEquals(ibcRoleValue, transformedAppeal.getAppellant().getIbcRole());
        assertEquals(APPELLANT_IBCA_REFERENCE, transformedAppeal.getAppellant().getIdentity().getIbcaReference());
        Address address = transformedAppeal.getAppellant().getAddress();
        assertNotNull(address);
        Address expectedAddress = Address.builder().line1(APPELLANT_ADDRESS_LINE1).line2(APPELLANT_ADDRESS_LINE2).town(APPELLANT_ADDRESS_LINE3).country(APPELLANT_ADDRESS_COUNTRY).postcode(APPELLANT_POSTCODE).portOfEntry(APPELLANT_PORT_OF_ENTRY).inMainlandUk(YesNo.NO).build();
        assertEquals(expectedAddress, address);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenKeyValuePairsWithEsaBenefitType_thenBuildAnAppealWithAppellant() {

        pairs.remove(BenefitTypeIndicator.PIP.getIndicatorString());
        pairs.put("is_benefit_type_esa", "true");
        pairs.put("office", "Balham DRT");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("Sheffield DRT", result.getTransformedCase().get("dwpRegionalCentre"));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenKeyValuePairsWithUcBenefitTypeAndWrongOfficePopulated_thenBuildAnAppealWithDefaultHandlingOffice() {
        transformer.setUcOfficeFeatureActive(true);
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), false);

        pairs.put("is_benefit_type_uc", "true");
        pairs.put("office", "Anything");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("Universal Credit", result.getTransformedCase().get("dwpRegionalCentre"));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenKeyValuePairsWithUcBenefitTypeAndRecoveryFromEstatesOffice_thenSetOfficesCorrectly() {
        transformer.setUcOfficeFeatureActive(true);
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), false);

        pairs.put("is_benefit_type_uc", "true");
        pairs.put("office", "Recovery from Estates");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("RfE", result.getTransformedCase().get("dwpRegionalCentre"));

        Appeal appeal = (Appeal) result.getTransformedCase().get("appeal");
        assertEquals("UC Recovery from Estates", appeal.getMrnDetails().getDwpIssuingOffice());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    //TODO: Remove when uc-office-feature switched on
    public void givenKeyValuePairsWithUcBenefitTypeAndWrongOfficePopulated_thenBuildAnAppealWithUcOffice() {
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), false);

        pairs.put("is_benefit_type_uc", "true");
        pairs.put("office", "Balham DRT");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(UNIVERSAL_CREDIT, result.getTransformedCase().get("dwpRegionalCentre"));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenKeyValuePairsWithUcBenefitTypeAndNoOfficePopulated_thenBuildAnAppealWithUcOffice() {
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), false);

        pairs.put("is_benefit_type_uc", "true");
        pairs.put("office", "");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(UNIVERSAL_CREDIT, result.getTransformedCase().get("dwpRegionalCentre"));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenKeyValuePairsWithPerson2AndPerson1_thenBuildAnAppealWithAppellantAndAppointee() {

        pairs.put("person1_title", APPOINTEE_TITLE);
        pairs.put("person1_first_name", APPOINTEE_FIRST_NAME);
        pairs.put("person1_last_name", APPOINTEE_LAST_NAME);
        pairs.put("person1_address_line1", APPOINTEE_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPOINTEE_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPOINTEE_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPOINTEE_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPOINTEE_POSTCODE);
        pairs.put("person1_phone", APPOINTEE_PHONE);
        pairs.put("person1_mobile", APPOINTEE_MOBILE);
        pairs.put("person1_dob", APPOINTEE_DATE_OF_BIRTH);
        pairs.put("person2_title", APPELLANT_TITLE);
        pairs.put("person2_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person2_last_name", APPELLANT_LAST_NAME);
        pairs.put("person2_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person2_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person2_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person2_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person2_postcode", APPELLANT_POSTCODE);
        pairs.put("person2_dob", APPELLANT_DATE_OF_BIRTH);
        pairs.put("person2_nino", APPELLANT_NINO);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Name appellantName = Name.builder().title(APPELLANT_TITLE).firstName(APPELLANT_FIRST_NAME).lastName(APPELLANT_LAST_NAME).build();
        Address appellantAddress = Address.builder().line1(APPELLANT_ADDRESS_LINE1).line2(APPELLANT_ADDRESS_LINE2).town(APPELLANT_ADDRESS_LINE3).county(APPELLANT_ADDRESS_LINE4).postcode(APPELLANT_POSTCODE).build();
        Identity appellantIdentity = Identity.builder().nino(normaliseNino(APPELLANT_NINO)).dob("1987-08-12").build();

        Name appointeeName = Name.builder().title(APPOINTEE_TITLE).firstName(APPOINTEE_FIRST_NAME).lastName(APPOINTEE_LAST_NAME).build();
        Address appointeeAddress = Address.builder().line1(APPOINTEE_ADDRESS_LINE1).line2(APPOINTEE_ADDRESS_LINE2).town(APPOINTEE_ADDRESS_LINE3).county(APPOINTEE_ADDRESS_LINE4).postcode(APPOINTEE_POSTCODE).build();
        Identity appointeeIdentity = Identity.builder().dob("1990-12-03").build();
        Contact appointeeContact = Contact.builder().phone(APPOINTEE_PHONE).mobile(APPOINTEE_MOBILE).build();
        Appointee appointee = Appointee.builder().name(appointeeName).address(appointeeAddress).contact(appointeeContact).identity(appointeeIdentity).build();

        Appellant expectedAppellant = Appellant.builder().name(appellantName).identity(appellantIdentity).isAppointee("Yes").address(appellantAddress).appointee(appointee).contact(Contact.builder().build()).build();

        Appellant appellantResult = ((Appeal) result.getTransformedCase().get("appeal")).getAppellant();
        assertThat(appellantResult)
            .usingRecursiveComparison()
            .ignoringFields("id", "appointee.id")
            .isEqualTo(expectedAppellant);

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenKeyValuePairsWithPerson2AndNoPerson1_thenBuildAnAppealWithAppellant() {

        pairs.put("person2_title", APPELLANT_TITLE);
        pairs.put("person2_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person2_last_name", APPELLANT_LAST_NAME);
        pairs.put("person2_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person2_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person2_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person2_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person2_postcode", APPELLANT_POSTCODE);
        pairs.put("person2_dob", APPELLANT_DATE_OF_BIRTH);
        pairs.put("person2_nino", APPELLANT_NINO);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Name appellantName = Name.builder().title(APPELLANT_TITLE).firstName(APPELLANT_FIRST_NAME).lastName(APPELLANT_LAST_NAME).build();
        Address appellantAddress = Address.builder().line1(APPELLANT_ADDRESS_LINE1).line2(APPELLANT_ADDRESS_LINE2).town(APPELLANT_ADDRESS_LINE3).county(APPELLANT_ADDRESS_LINE4).postcode(APPELLANT_POSTCODE).build();
        Identity appellantIdentity = Identity.builder().nino(normaliseNino(APPELLANT_NINO)).dob("1987-08-12").build();

        Appellant expectedAppellant = Appellant.builder().name(appellantName).identity(appellantIdentity).isAppointee("No").address(appellantAddress).contact(Contact.builder().build()).build();

        Appellant appellantResult = ((Appeal) result.getTransformedCase().get("appeal")).getAppellant();
        assertThat(appellantResult)
            .usingRecursiveComparison()
            .ignoringFields("id", "appointee.id")
            .isEqualTo(expectedAppellant);

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenAnAppellant_thenAddAppealNumberToAppellantSubscription() {
        pairs.put("person1_first_name", "Jeff");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Subscriptions subscriptions = ((Subscriptions) result.getTransformedCase().get("subscriptions"));
        assertNotNull(subscriptions.getAppellantSubscription().getTya());
        assertNull(subscriptions.getAppointeeSubscription());
    }

    @Test
    public void givenAnAppellantAndAppointee_thenOnlyAddAppealNumberToAppointeeSubscription() {
        pairs.put("person1_first_name", "Jeff");
        pairs.put("person2_first_name", "Terry");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Subscriptions subscriptions = ((Subscriptions) result.getTransformedCase().get("subscriptions"));
        assertNull(subscriptions.getAppellantSubscription());
        assertNotNull(subscriptions.getAppointeeSubscription().getTya());
    }

    @Test
    public void givenARepresentative_thenAddAppealNumberToRepresentativeSubscription() {
        pairs.put("representative_first_name", "Wendy");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Subscriptions subscriptions = ((Subscriptions) result.getTransformedCase().get("subscriptions"));
        assertNotNull(subscriptions.getRepresentativeSubscription().getTya());
    }

    @Test
    public void givenOralHearingType_thenBuildAnAppealWithWantsToAttendYes() {

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_TYPE_ORAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingType());
        assertEquals(YES_LITERAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getWantsToAttend());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenPaperHearingType_thenBuildAnAppealWithWantsToAttendNo() {

        pairs.put(IS_HEARING_TYPE_ORAL_LITERAL, false);
        pairs.put(IS_HEARING_TYPE_PAPER_LITERAL, true);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_TYPE_PAPER, ((Appeal) result.getTransformedCase().get("appeal")).getHearingType());
        assertEquals(NO_LITERAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getWantsToAttend());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"Yes", "No"})
    public void givenHearingTypeYesNo_thenCorrectlyBuildAnAppealWithWantsToAttendValue(String isOral) {

        pairs.put(IS_HEARING_TYPE_ORAL_LITERAL, isOral);
        pairs.put(IS_HEARING_TYPE_PAPER_LITERAL, isOral.equals("Yes") ? "No" : "Yes");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        String expectedHearingType = isOral.equals("Yes") ? HEARING_TYPE_ORAL : HEARING_TYPE_PAPER;
        String attendingHearing = isOral.equals("Yes") ? YES_LITERAL : NO_LITERAL;

        assertEquals(expectedHearingType, ((Appeal) result.getTransformedCase().get("appeal")).getHearingType());
        assertEquals(attendingHearing, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getWantsToAttend());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenContradictingPaperAndOralCaseValues_thenAddErrorToList() {
        Map<String, Object> contradictingPairs = ImmutableMap.<String, Object>builder()
            .put(IS_HEARING_TYPE_ORAL_LITERAL, "true")
            .put(IS_HEARING_TYPE_PAPER_LITERAL, "true").build();

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(contradictingPairs).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().contains("is_hearing_type_oral and is_hearing_type_paper have contradicting values"));
    }

    @Test
    public void givenHearingTypeOralIsTrueAndHearingTypePaperIsEmpty_thenSetHearingTypeToOral() {
        Map<String, Object> hearingTypePairs = new HashMap<>();
        hearingTypePairs.put("is_hearing_type_oral", true);
        hearingTypePairs.put("is_hearing_type_paper", "null");

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(hearingTypePairs).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_TYPE_ORAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingType());
    }

    @Test
    public void givenHearingTypePaperIsTrueAndHearingTypeOralIsEmpty_thenSetHearingTypeToPaper() {
        Map<String, Object> hearingTypePairs = new HashMap<>();
        hearingTypePairs.put("is_hearing_type_oral", "null");
        hearingTypePairs.put("is_hearing_type_paper", true);

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(hearingTypePairs).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_TYPE_PAPER, ((Appeal) result.getTransformedCase().get("appeal")).getHearingType());
    }

    @Test
    public void givenHearingTypeOralIsFalseAndHearingTypePaperIsEmpty_thenSetHearingTypeToPaper() {
        Map<String, Object> hearingTypePairs = new HashMap<>();
        hearingTypePairs.put("is_hearing_type_oral", false);
        hearingTypePairs.put("is_hearing_type_paper", "null");

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(hearingTypePairs).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_TYPE_PAPER, ((Appeal) result.getTransformedCase().get("appeal")).getHearingType());
    }

    @Test
    public void givenHearingTypePaperIsFalseAndHearingTypeOralIsEmpty_thenSetHearingTypeToOral() {
        Map<String, Object> hearingTypePairs = new HashMap<>();
        hearingTypePairs.put("is_hearing_type_oral", "null");
        hearingTypePairs.put("is_hearing_type_paper", false);

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(hearingTypePairs).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_TYPE_ORAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingType());
    }

    @Test
    public void givenBooleanValueIsRandomText_thenSetHearingTypeToNull() {
        Map<String, Object> textBooleanValueMap = ImmutableMap.<String, Object>builder()
            .put("is_hearing_type_oral", "I am a text value")
            .put("is_hearing_type_paper", "true").build();

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(textBooleanValueMap).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingType());
    }

    @Test
    public void givenAnInvalidDateOfBirth_thenAddErrorToList() {
        pairs.put("person1_dob", "12/99/1987");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().contains("person1_dob is an invalid date field. Needs to be a valid date and in the format dd/mm/yyyy"));
    }

    @Test
    public void givenAnInvalidMrnDate_thenAddErrorToList() {
        pairs.put("mrn_date", "12/99/1987");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().contains("mrn_date is an invalid date field. Needs to be a valid date and in the format dd/mm/yyyy"));
    }

    @Test
    public void givenANullMrnDate_thenAddErrorToList() {
        pairs.put("mrn_date", null);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenAnEmptyStringMrnDate_thenAddErrorToList() {
        pairs.put("mrn_date", "");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenCaseContainsHearingOptions_thenBuildAnAppealWithSupport() {

        pairs.put("hearing_options_hearing_loop", HEARING_LOOP);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("hearingLoop", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getArrangements().getFirst());
        assertEquals("Yes", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getWantsSupport());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenCaseContainsNoHearingOptions_thenBuildAnAppealWithNoSupport() {

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("No", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getWantsSupport());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"true", "Yes"})
    public void givenHearingLoopIsRequired_thenBuildAnAppealWithArrangementsWithHearingLoop(String hearingLoop) {

        pairs.put("hearing_options_hearing_loop", hearingLoop);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("hearingLoop", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getArrangements().getFirst());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"false", "No"})
    public void givenHearingLoopIsNotRequired_thenBuildAnAppealWithNoHearingLoop(String hearingLoop) {

        pairs.put("hearing_options_hearing_loop", hearingLoop);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(0, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getArrangements().size());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"true", "Yes"})
    public void givenDisabledAccessIsRequired_thenBuildAnAppealWithArrangementsWithDisabledAccess(String disabledAccess) {
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), true);

        pairs.put("hearing_options_accessible_hearing_rooms", disabledAccess);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("disabledAccess", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getArrangements().getFirst());
        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"false", "No"})
    public void givenDisabledAccessIsNotRequired_thenBuildAnAppealWithNoDisabledAccess(String disabledAccess) {

        pairs.put("hearing_options_accessible_hearing_rooms", disabledAccess);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(0, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getArrangements().size());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenSingleExcludedDate_thenBuildAnAppealWithExcludedStartDateAndScheduleHearingYes() {

        pairs.put("hearing_options_exclude_dates", HEARING_OPTIONS_EXCLUDE_DATES);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("2030-12-01", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates().getFirst().getValue().getStart());
        assertEquals(YES_LITERAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getScheduleHearing());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenSingleExcludedDate_thenBuildAnAppealWithExcludedStartDateAndWantToAttendYes() {

        pairs.put("hearing_options_exclude_dates", HEARING_OPTIONS_EXCLUDE_DATES);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("2030-12-01", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates().getFirst().getValue().getStart());
        assertEquals(YES_LITERAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getWantsToAttend());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenMultipleExcludedDates_thenBuildAnAppealWithExcludedDatesAndWantToAttendYes() {

        pairs.put("hearing_options_exclude_dates", "01/12/2030, 15/12/2030-31/12/2030");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        List<ExcludeDate> excludeDateList = ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates();

        assertEquals("2030-12-01", excludeDateList.getFirst().getValue().getStart());
        assertEquals("2030-12-15", excludeDateList.get(1).getValue().getStart());
        assertEquals("2030-12-31", excludeDateList.get(1).getValue().getEnd());
        assertEquals(YES_LITERAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getWantsToAttend());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenNoExcludedDate_thenBuildAnAppealWithExcludedStartDateAndScheduleHearingNo() {

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates());
        assertEquals(NO_LITERAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getScheduleHearing());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenAnExcludedDateAndDoesNotWantToAttendHearing_thenBuildAnAppealWithScheduleHearingNo() {

        pairs.put("hearing_options_exclude_dates", HEARING_OPTIONS_EXCLUDE_DATES);
        pairs.put("is_hearing_type_oral", false);
        pairs.put("is_hearing_type_paper", true);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("2030-12-01", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates().getFirst().getValue().getStart());
        assertEquals(NO_LITERAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getScheduleHearing());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenTwoSingleExcludedDatesWithSpace_thenBuildAnAppealWithTwoExcludedStartDates() {

        pairs.put("hearing_options_exclude_dates", "12/12/2018, 16/12/2018");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        List<ExcludeDate> excludeDates = ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates();
        assertEquals("2018-12-12", (excludeDates.getFirst().getValue().getStart()));
        assertEquals("2018-12-16", (excludeDates.get(1).getValue().getStart()));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenTwoSingleExcludedDatesWithNoSpace_thenBuildAnAppealWithTwoExcludedStartDates() {

        pairs.put("hearing_options_exclude_dates", "12/12/2018,16/12/2018");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        List<ExcludeDate> excludeDates = ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates();
        assertEquals("2018-12-12", (excludeDates.getFirst().getValue().getStart()));
        assertEquals("2018-12-16", (excludeDates.get(1).getValue().getStart()));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenExcludedDateRangeIsEmpty_thenBuildAnAppealWithEmptyExcludedDateRange() {

        pairs.put("hearing_options_exclude_dates", "");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        List<ExcludeDate> excludeDates = ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates();

        assertNull(excludeDates);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenExcludedDateRangeIsNull_thenBuildAnAppealWithEmptyExcludedDateRange() {

        pairs.put("hearing_options_exclude_dates", null);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        List<ExcludeDate> excludeDates = ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates();

        assertNull(excludeDates);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenSingleExcludedDateFollowedByRangeWithSpace_thenBuildAnAppealWithSingleExcludedStartDateAndADateRange() {

        pairs.put("hearing_options_exclude_dates", "12/12/2018, 16/12/2018 - 18/12/2018");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        List<ExcludeDate> excludeDates = ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates();
        assertEquals("2018-12-12", (excludeDates.getFirst().getValue().getStart()));
        assertEquals("2018-12-16", (excludeDates.get(1).getValue().getStart()));
        assertEquals("2018-12-18", (excludeDates.get(1).getValue().getEnd()));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenSingleExcludedDateFollowedByRangeWithNoSpace_thenBuildAnAppealWithSingleExcludedStartDateAndADateRange() {

        pairs.put("hearing_options_exclude_dates", "16/12/2018-18/12/2018");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        List<ExcludeDate> excludeDates = ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates();
        assertEquals("2018-12-16", (excludeDates.getFirst().getValue().getStart()));
        assertEquals("2018-12-18", (excludeDates.getFirst().getValue().getEnd()));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenMultipleExcludedDateFollowedByMultipleRange_thenBuildAnAppealWithMultipleExcludedStartDatesAndMultipleDateRanges() {

        pairs.put("hearing_options_exclude_dates", "12/12/2018, 14/12/2018, 16/12/2018 - 18/12/2018");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        List<ExcludeDate> excludeDates = ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates();
        assertEquals("2018-12-12", (excludeDates.getFirst().getValue().getStart()));
        assertEquals("2018-12-14", (excludeDates.get(1).getValue().getStart()));
        assertEquals("2018-12-16", (excludeDates.get(2).getValue().getStart()));
        assertEquals("2018-12-18", (excludeDates.get(2).getValue().getEnd()));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenIncorrectExcludedDateFormat_thenAddAnError() {

        pairs.put("hearing_options_exclude_dates", "16th December 2018");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().contains("hearing_options_exclude_dates contains an invalid date range. Should be single dates separated by commas and/or a date range e.g. 01/01/2020, 07/01/2020, 12/01/2020 - 15/01/2020"));
    }

    @Test
    public void givenIncorrectExcludedDateRangeFormat_thenAddAnError() {

        pairs.put("hearing_options_exclude_dates", "16/12/2018 - 18/12/2018 - 20/12/2018");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().contains("hearing_options_exclude_dates contains an invalid date range. Should be single dates separated by commas and/or a date range e.g. 01/01/2020, 07/01/2020, 12/01/2020 - 15/01/2020"));
    }

    @Test
    public void givenALanguageTypeIsEntered_thenBuildAnAppealWithArrangementsWithLanguageInterpreterAndTypeSet() {

        pairs.put(HEARING_OPTIONS_LANGUAGE_TYPE_LITERAL, HEARING_OPTIONS_LANGUAGE_TYPE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_OPTIONS_LANGUAGE_TYPE, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getLanguages());
        assertEquals(YES_LITERAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getLanguageInterpreter());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenALanguageTypeAndDialectIsEntered_thenBuildAnAppealWithArrangementsWithLanguageInterpreterAndDialectAppended() {

        pairs.put(HEARING_OPTIONS_LANGUAGE_TYPE_LITERAL, HEARING_OPTIONS_LANGUAGE_TYPE);
        pairs.put(HEARING_OPTIONS_DIALECT_LITERAL, HEARING_OPTIONS_DIALECT_TYPE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_OPTIONS_LANGUAGE_TYPE + " " + HEARING_OPTIONS_DIALECT_TYPE, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getLanguages());
        assertEquals(YES_LITERAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getLanguageInterpreter());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenADialectIsEntered_thenBuildAnAppealWithArrangementsWithLanguageTypeSetToDialect() {

        pairs.put(HEARING_OPTIONS_DIALECT_LITERAL, HEARING_OPTIONS_DIALECT_TYPE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_OPTIONS_DIALECT_TYPE, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getLanguages());
        assertEquals(YES_LITERAL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getLanguageInterpreter());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"Yes", "true"})
    public void givenASignLanguageInterpreterIsTrueAndTypeIsEntered_thenBuildAnAppealWithArrangementsWithSignLanguageInterpreterAndTypeSetToValueEntered(String signLanguageInterpreter) {

        pairs.put(HEARING_OPTIONS_SIGN_LANGUAGE_INTERPRETER_LITERAL, signLanguageInterpreter);
        pairs.put(HEARING_OPTIONS_SIGN_LANGUAGE_TYPE_LITERAL, SIGN_LANGUAGE_TYPE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("signLanguageInterpreter", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getArrangements().getFirst());
        assertEquals(SIGN_LANGUAGE_TYPE, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getSignLanguageType());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenASignLanguageInterpreterIsTrueAndTypeIsNotEntered_thenBuildAnAppealWithArrangementsWithSignLanguageInterpreterAndTypeSetToDefaultType() {

        pairs.put(HEARING_OPTIONS_SIGN_LANGUAGE_INTERPRETER_LITERAL, SIGN_LANGUAGE_REQUIRED);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("signLanguageInterpreter", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getArrangements().getFirst());
        assertEquals(DEFAULT_SIGN_LANGUAGE, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getSignLanguageType());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"No", "false"})
    public void givenASignLanguageInterpreterIsFalse_thenBuildAnAppealWithNoArrangements(String signLanguageInterpreter) {

        pairs.put(HEARING_OPTIONS_SIGN_LANGUAGE_INTERPRETER_LITERAL, signLanguageInterpreter);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(0, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getArrangements().size());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenASignLanguageInterpreterIsEntered_thenBuildAnAppealWithSignLanguageInterpreter() {

        pairs.put(HEARING_OPTIONS_SIGN_LANGUAGE_TYPE_LITERAL, SIGN_LANGUAGE_TYPE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("signLanguageInterpreter", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getArrangements().getFirst());
        assertEquals(SIGN_LANGUAGE_TYPE, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getSignLanguageType());
        assertEquals("No", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getLanguageInterpreter());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenASignLanguageAndLanguageIsEntered_thenBuildAnAppealWithSignLanguageAndLanguageRequirements() {
        pairs.put(HEARING_OPTIONS_SIGN_LANGUAGE_TYPE_LITERAL, SIGN_LANGUAGE_TYPE);
        pairs.put(HEARING_OPTIONS_LANGUAGE_TYPE_LITERAL, HEARING_OPTIONS_LANGUAGE_TYPE);
        pairs.put(HEARING_OPTIONS_DIALECT_LITERAL, HEARING_OPTIONS_DIALECT_TYPE);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertEquals("signLanguageInterpreter", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getArrangements().getFirst());
        assertEquals(SIGN_LANGUAGE_TYPE, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getSignLanguageType());
        assertEquals(HEARING_OPTIONS_LANGUAGE_TYPE + " " + HEARING_OPTIONS_DIALECT_TYPE, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getLanguages());
    }

    @Test
    public void givenASignLanguageInterpreterAndLanguageInterpreterIsEntered_thenBuildAnAppealWithSignLanguageAndLanguageInterpreter() {
        pairs.put(IS_HEARING_TYPE_ORAL_LITERAL, true);
        pairs.put(IS_HEARING_TYPE_PAPER_LITERAL, false);
        pairs.put(HEARING_OPTIONS_LANGUAGE_TYPE_LITERAL, HEARING_OPTIONS_LANGUAGE_TYPE);
        pairs.put(HEARING_OPTIONS_DIALECT_LITERAL, HEARING_OPTIONS_DIALECT_TYPE);
        pairs.put(HEARING_OPTIONS_SIGN_LANGUAGE_INTERPRETER_LITERAL, true);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertEquals("signLanguageInterpreter", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getArrangements().getFirst());
        assertEquals("British Sign Language", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getSignLanguageType());
        assertEquals(HEARING_OPTIONS_LANGUAGE_TYPE + " " + HEARING_OPTIONS_DIALECT_TYPE, ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getLanguages());
    }

    @Test
    public void givenACaseIsNotLinked_thenSetLinkedCaseToNo() {
        pairs.put(IS_HEARING_TYPE_ORAL_LITERAL, true);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("No", result.getTransformedCase().get("linkedCasesBoolean"));
    }

    @Test
    public void givenACaseIsLinked_thenSetLinkedCaseToYesAndPopulateAssociatedCases() {
        pairs.put("person1_nino", "JT0123456B");

        List<SscsCaseDetails> caseDetails = new ArrayList<>();
        caseDetails.add(SscsCaseDetails.builder().id(123L).build());

        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq("JT0123456B"), any())).willReturn(caseDetails);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("Yes", result.getTransformedCase().get("linkedCasesBoolean"));

        assertEquals("123", ((CaseLink)((List<?>) result.getTransformedCase().get("associatedCase")).getFirst()).getValue().getCaseReference());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    public void givenACaseAlreadyExistsWithSameNinoBenefitTypeAndMrnDate_thenReturnAWarningWhenWarningsCombined(boolean combineWarnings) {

        pairs.put(PERSON1_VALUE + NINO, APPELLANT_NINO);
        pairs.put(MRN_DATE, MRN_DATE_VALUE);
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), YES_LITERAL);

        SscsCaseDetails caseDetails = SscsCaseDetails.builder().id(123L).build();

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(eq(APPELLANT_NINO), eq("PIP"), eq("2048-11-01"), any())).willReturn(caseDetails);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, combineWarnings);

        String message = "Duplicate case already exists - please reject this exception record";
        if (combineWarnings) {
            assertEquals(message, result.getWarnings().getFirst());
        } else {
            assertEquals(message, result.getErrors().getFirst());
        }
    }

    @Test
    public void givenACaseWithNullOcrData_thenAddErrorToList() {

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(null).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().contains("No OCR data, case cannot be created"));
    }

    @Test
    public void givenACaseWithNoOcrData_thenAddErrorToList() {
        Map<String, Object> noPairs = ImmutableMap.<String, Object>builder().build();

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(noPairs).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().contains("No OCR data, case cannot be created"));
    }

    @Test
    public void givenACaseWithFailedSchemaValidation_thenAddErrorToList() {
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().id("123456").ocrDataFields(ocrList).formType(FormType.SSCS1PEU.toString()).build();

        given(formTypeValidator.validate("123456", exceptionRecord)).willReturn(CaseResponse.builder().errors(ImmutableList.of("NI Number is invalid")).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().contains("NI Number is invalid"));
    }

    @Test
    public void createCaseWithTodaysCaseCreationDate() {
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), true);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String nowDateFormatted = df.format(new Date());

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).records(null).openingDate(LocalDateTime.now().toLocalDate().toString()).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(nowDateFormatted, result.getTransformedCase().get("caseCreated"));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenOneDocument_thenBuildACase() {
        List<InputScannedDoc> records = new ArrayList<>();
        InputScannedDoc scannedRecord = buildTestScannedRecord(DocumentLink.builder().documentUrl("www.test.com").build(), "My subtype");
        records.add(scannedRecord);

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(
            ScannedData.builder().ocrCaseData(pairs).records(records).openingDate(LocalDateTime.now().toLocalDate().toString()).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Map<String, Object> transformedCase = result.getTransformedCase();
        @SuppressWarnings("unchecked")
        List<SscsDocument> docs = ((List<SscsDocument>) transformedCase.get("sscsDocument"));
        assertEquals(LocalDateTime.now().toLocalDate().toString(), docs.getFirst().getValue().getDocumentDateAdded());
        assertEquals(scannedRecord.getFileName(), docs.getFirst().getValue().getDocumentFileName());
        assertEquals(scannedRecord.getUrl().getDocumentUrl(), docs.getFirst().getValue().getDocumentLink().getDocumentUrl());
        assertEquals("appellantEvidence", docs.getFirst().getValue().getDocumentType());

        assertEquals(YES_LITERAL, transformedCase.get("evidencePresent"));

        DateTimeFormatter dtfOut = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String expectedCreatedDate = dtfOut.format(LocalDateTime.now());
        assertEquals(expectedCreatedDate, transformedCase.get("caseCreated"));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenOneDocumentWithAnOpeningDate_thenBuildACase() {
        List<InputScannedDoc> records = new ArrayList<>();
        InputScannedDoc scannedRecord = buildTestScannedRecord(DocumentLink.builder().documentUrl("www.test.com").build(), "My subtype");
        records.add(scannedRecord);

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).records(records).openingDate(LocalDateTime.now().minusYears(3).toLocalDate().toString()).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Map<String, Object> transformedCase = result.getTransformedCase();
        @SuppressWarnings("unchecked")
        List<SscsDocument> docs = ((List<SscsDocument>) transformedCase.get("sscsDocument"));
        assertEquals(LocalDateTime.now().toLocalDate().toString(), docs.getFirst().getValue().getDocumentDateAdded());
        assertEquals(scannedRecord.getFileName(), docs.getFirst().getValue().getDocumentFileName());
        assertEquals(scannedRecord.getUrl().getDocumentUrl(), docs.getFirst().getValue().getDocumentLink().getDocumentUrl());
        assertEquals("appellantEvidence", docs.getFirst().getValue().getDocumentType());

        assertEquals(YES_LITERAL, transformedCase.get("evidencePresent"));

        DateTimeFormatter dtfOut = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String expectedCreatedDate = dtfOut.format(LocalDateTime.now().minusYears(3));
        assertEquals(expectedCreatedDate, transformedCase.get("caseCreated"));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void should_handle_date_times_with_and_without_milliseconds() {
        // given
        List<InputScannedDoc> scannedRecords = Arrays.asList(
            InputScannedDoc.builder()
                .scannedDate(LocalDateTime.now().minusDays(1)) // no millis
                .controlNumber("123")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .fileName("mrn.jpg")
                .type("Testing")
                .subtype("My subtype").build(),
            InputScannedDoc.builder()
                .scannedDate(LocalDateTime.now()) // with millis
                .controlNumber("567")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .fileName("mrn.jpg")
                .type("Testing")
                .subtype("My subtype").build()
        );
        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).records(scannedRecords).build());

        // when
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        // then
        @SuppressWarnings("unchecked")
        List<SscsDocument> docs = ((List<SscsDocument>) result.getTransformedCase().get("sscsDocument"));

        assertThat(docs)
            .extracting(doc -> doc.getValue().getDocumentDateAdded())
            .containsExactlyInAnyOrder(
                LocalDateTime.now().minusDays(1).toLocalDate().toString(),
                LocalDateTime.now().toLocalDate().toString()
            );
    }

    @ParameterizedTest
    @CsvSource({"SSCS1, sscs1", "SSCS1PE, sscs1", "SSCS2, sscs2", "SSCS5, sscs5", "bla, appellantEvidence"})
    public void givenOneSscs1FormAndOneEvidence_thenBuildACaseWithCorrectDocumentTypes(String sscs1Type, String documentType) {
        List<InputScannedDoc> records = new ArrayList<>();
        InputScannedDoc scannedRecord1 = buildTestScannedRecord(DocumentLink.builder().documentUrl("http://www.test1.com").build(), sscs1Type);
        InputScannedDoc scannedRecord2 = buildTestScannedRecord(DocumentLink.builder().documentUrl("http://www.test2.com").build(), "My subtype");
        records.add(scannedRecord1);
        records.add(scannedRecord2);

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).records(records).openingDate(LocalDateTime.now().toLocalDate().toString()).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        @SuppressWarnings("unchecked")
        List<SscsDocument> docs = ((List<SscsDocument>) result.getTransformedCase().get("sscsDocument"));
        assertEquals(LocalDateTime.now().toLocalDate().toString(), docs.getFirst().getValue().getDocumentDateAdded());
        assertEquals(scannedRecord1.getFileName(), docs.getFirst().getValue().getDocumentFileName());
        assertEquals(scannedRecord1.getUrl().getDocumentUrl(), docs.getFirst().getValue().getDocumentLink().getDocumentUrl());
        assertEquals(documentType, docs.getFirst().getValue().getDocumentType());
        assertEquals(LocalDateTime.now().toLocalDate().toString(), docs.get(1).getValue().getDocumentDateAdded());
        assertEquals(scannedRecord2.getFileName(), docs.get(1).getValue().getDocumentFileName());
        assertEquals(scannedRecord2.getUrl().getDocumentUrl(), docs.get(1).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("appellantEvidence", docs.get(1).getValue().getDocumentType());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenOneDocumentWithNoDetails_thenShowAnError() {
        List<InputScannedDoc> records = new ArrayList<>();
        InputScannedDoc scannedRecord = InputScannedDoc.builder()
            .scannedDate(null)
            .controlNumber(null)
            .url(null)
            .fileName(null)
            .type(null)
            .subtype(null).build();

        records.add(scannedRecord);

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).records(records).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().contains("File name field must not be empty"));
    }

    @Test
    public void givenOneDocumentWithNoFileExtension_thenShowAnError() {
        List<InputScannedDoc> records = new ArrayList<>();

        InputScannedDoc scannedRecord = InputScannedDoc.builder()
            .scannedDate(LocalDateTime.now())
            .controlNumber("123")
            .url(DocumentLink.builder().documentUrl("www.test.com").build())
            .fileName("mrn details")
            .type("Testing")
            .subtype("My subtype").build();
        records.add(scannedRecord);

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).records(records).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().contains("Evidence file type 'mrn details' unknown"));
    }

    @Test
    public void givenOneDocumentWithInvalidFileExtension_thenShowAnError() {
        List<InputScannedDoc> records = new ArrayList<>();

        InputScannedDoc scannedRecord = InputScannedDoc.builder()
            .scannedDate(LocalDateTime.now())
            .controlNumber("123")
            .url(DocumentLink.builder().documentUrl("www.test.com").build())
            .fileName("mrn_details.xyz")
            .type("Testing")
            .subtype("My subtype").build();
        records.add(scannedRecord);

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).records(records).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertTrue(result.getErrors().contains("Evidence file type 'xyz' unknown"));
    }

    @Test
    public void givenACaseWithNoDocuments_thenBuildACaseWithNoEvidencePresent() {
        List<InputScannedDoc> records = new ArrayList<>();

        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).records(records).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        Map<String, Object> transformedCase = result.getTransformedCase();
        assertEquals(NO_LITERAL, transformedCase.get("evidencePresent"));

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenAPipCase_thenSetCreatedInGapsFromFieldToReadyToList() {
        pairs.put("office", "2");
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicator.ESA.getIndicatorString(), false);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        String createdInGapsFrom = ((String) result.getTransformedCase().get("createdInGapsFrom"));
        assertEquals(READY_TO_LIST.getId(), createdInGapsFrom);

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenAEsaCase_thenSetCreatedInGapsFromFieldToReadyToList() {
        pairs.put("office", "Chesterfield DRT");
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), false);
        pairs.put(BenefitTypeIndicator.ESA.getIndicatorString(), true);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        String createdInGapsFrom = ((String) result.getTransformedCase().get("createdInGapsFrom"));
        assertEquals(READY_TO_LIST.getId(), createdInGapsFrom);

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"(AE)", "AE", "PIP AE", "DWP PIP (AE)"})
    public void givenAPipAeCase_thenAcceptOfficeWithFuzzyMatching(String pipAe) {
        pairs.put("office", pipAe);
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicator.ESA.getIndicatorString(), false);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("AE", result.getTransformedCase().get("dwpRegionalCentre"));
        assertEquals("DWP PIP (AE)", ((Appeal) result.getTransformedCase().get("appeal")).getMrnDetails().getDwpIssuingOffice());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"PIP (3)", "  PIP 3  ", "PIP 3", "DWP PIP (3)"})
    public void givenAPipOffice3Case_thenAcceptOfficeWithFuzzyMatching(String pip3) {
        pairs.put("office", pip3);
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicator.ESA.getIndicatorString(), false);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("Bellevale", result.getTransformedCase().get("dwpRegionalCentre"));
        assertEquals("DWP PIP (3)", ((Appeal) result.getTransformedCase().get("appeal")).getMrnDetails().getDwpIssuingOffice());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"Recovery from Estates", "PIP Recovery from Estates"})
    public void givenAPipOfficeRecoveryEstatesCase_thenAcceptOfficeWithFuzzyMatching(String pipRecoveryEstates) {
        pairs.put("office", pipRecoveryEstates);
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), true);
        pairs.put(BenefitTypeIndicator.ESA.getIndicatorString(), false);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("RfE", result.getTransformedCase().get("dwpRegionalCentre"));
        assertEquals("PIP Recovery from Estates", ((Appeal) result.getTransformedCase().get("appeal")).getMrnDetails().getDwpIssuingOffice());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenACaseWithNoReadyToListOffice_thenSetCreatedInGapsFromFieldToReadyToList() {
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        String createdInGapsFrom = ((String) result.getTransformedCase().get("createdInGapsFrom"));
        assertEquals(READY_TO_LIST.getId(), createdInGapsFrom);

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"true", "Yes"})
    public void givenAgreeLessHearingNoticeIsRequired_thenBuildAnAppealWithAgreeLessHearingNotice(String agreeLessHearingNotice) {

        pairs.put("agree_less_hearing_notice", agreeLessHearingNotice);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("Yes", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getAgreeLessNotice());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"false", "No"})
    public void givenAgreeLessHearingNoticeIsNotRequired_thenBuildAnAppealWithNoAgreeLessHearingNotice(String agreeLessHearingNotice) {

        pairs.put("agree_less_hearing_notice", agreeLessHearingNotice);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("No", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getAgreeLessNotice());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"true", "Yes"})
    public void givenTellTribunalAboutDatesIsRequiredAndExcludedDatesProvided_thenBuildAnAppealWithExcludedDates(String tellTribunalAboutDates) {

        pairs.put("tell_tribunal_about_dates", tellTribunalAboutDates);
        pairs.put("hearing_options_exclude_dates", HEARING_OPTIONS_EXCLUDE_DATES);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals("2030-12-01", ((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates().getFirst().getValue().getStart());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"true", "Yes"})
    public void givenTellTribunalAboutDatesIsRequiredAndExcludedDatesIsEmpty_thenProvideWarningToCaseworker(String tellTribunalAboutDates) {

        pairs.put("tell_tribunal_about_dates", tellTribunalAboutDates);
        pairs.put("hearing_options_exclude_dates", "");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(1, result.getWarnings().size());
        assertEquals(HEARING_EXCLUDE_DATES_MISSING, result.getWarnings().getFirst());

        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"true", "Yes"})
    public void givenTellTribunalAboutDatesIsRequiredAndExcludedDatesIsNotPresent_thenProvideWarningToCaseworker(String tellTribunalAboutDates) {

        pairs.put("tell_tribunal_about_dates", tellTribunalAboutDates);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(1, result.getWarnings().size());
        assertEquals(HEARING_EXCLUDE_DATES_MISSING, result.getWarnings().getFirst());

        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"false", "No"})
    public void givenTellTribunalAboutDatesIsNotRequired_thenBuildAnAppealWithNoExcludedDates(String tellTribunalAboutDates) {

        pairs.put("tell_tribunal_about_dates", tellTribunalAboutDates);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingOptions().getExcludeDates());

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"Doctor, Dr", "Reverend, Rev"})
    public void givenTitleIsLong_thenConvertToShortenedVersion(String ocrTitle, String outputTitle) {

        pairs.put("person1_title", ocrTitle);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(outputTitle, ((Appeal) result.getTransformedCase().get("appeal")).getAppellant().getName().getTitle());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void givenAnAppealWithAnErrorAndCombineWarningsTrue_thenMoveErrorsToWarnings() {
        pairs.put("person1_dob", "12/99/1987");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, true);

        assertTrue(result.getWarnings().contains("person1_dob is an invalid date field. Needs to be a valid date and in the format dd/mm/yyyy"));
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void givenATransformForValidationRequestFailsSchemaValidation_thenReturnErrors() {
        pairs.put("bla", "12/99/1987");

        ExceptionRecord exceptionRecord = ExceptionRecord.builder().id("123456").ocrDataFields(ocrList).formType(FormType.SSCS1PEU.toString()).build();

        given(formTypeValidator.validate("123456", exceptionRecord)).willReturn(CaseResponse.builder().errors(ImmutableList.of("NI Number is invalid")).build());

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, true);

        assertEquals("NI Number is invalid", result.getErrors().getFirst());
    }

    @ParameterizedTest
    @CsvSource({"true", "Yes", "false", "No"})
    public void givenHearingSubtypeDetailsAreProvided_thenBuildAnAppealHearingSubtypeDetails(String hearingSubtypeFlag) {

        pairs.put(HEARING_TYPE_TELEPHONE_LITERAL, hearingSubtypeFlag);
        pairs.put(HEARING_TELEPHONE_LITERAL, HEARING_TELEPHONE_NUMBER);
        pairs.put(HEARING_TYPE_VIDEO_LITERAL, hearingSubtypeFlag);
        pairs.put(HEARING_VIDEO_EMAIL_LITERAL, HEARING_VIDEO_EMAIL);
        pairs.put(HEARING_TYPE_FACE_TO_FACE_LITERAL, hearingSubtypeFlag);
        String expectedResult = hearingSubtypeFlag.equals("true") || hearingSubtypeFlag.equals("Yes") ? "Yes" : "No";

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertEquals(expectedResult, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeTelephone());
        assertEquals(HEARING_TELEPHONE_NUMBER, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingTelephoneNumber());
        assertEquals(expectedResult, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeVideo());
        assertEquals(HEARING_VIDEO_EMAIL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingVideoEmail());
        assertEquals(expectedResult, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeFaceToFace());
    }

    @ParameterizedTest
    @CsvSource({"true", "Yes", "false", "No"})
    public void givenHearingSubtypeDetailsAreProvided_WithoutHearingTypeTelephone_thenBuildAnAppealHearingSubtypeDetails(String hearingSubtypeFlag) {

        pairs.put(HEARING_TELEPHONE_LITERAL, HEARING_TELEPHONE_NUMBER);
        pairs.put(HEARING_TYPE_VIDEO_LITERAL, hearingSubtypeFlag);
        pairs.put(HEARING_VIDEO_EMAIL_LITERAL, HEARING_VIDEO_EMAIL);
        pairs.put(HEARING_TYPE_FACE_TO_FACE_LITERAL, hearingSubtypeFlag);
        String expectedResult = hearingSubtypeFlag.equals("true") || hearingSubtypeFlag.equals("Yes") ? "Yes" : "No";

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeTelephone());
        assertEquals(HEARING_TELEPHONE_NUMBER, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingTelephoneNumber());
        assertEquals(expectedResult, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeVideo());
        assertEquals(HEARING_VIDEO_EMAIL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingVideoEmail());
        assertEquals(expectedResult, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeFaceToFace());
    }

    @ParameterizedTest
    @CsvSource({"true", "Yes", "false", "No"})
    public void givenHearingSubtypeDetailsAreProvided_WithoutHearingTypeTelephoneOrHearingTelephone_thenBuildAnAppealHearingSubtypeDetails(String hearingSubtypeFlag) {

        pairs.put(HEARING_TYPE_VIDEO_LITERAL, hearingSubtypeFlag);
        pairs.put(HEARING_VIDEO_EMAIL_LITERAL, HEARING_VIDEO_EMAIL);
        pairs.put(HEARING_TYPE_FACE_TO_FACE_LITERAL, hearingSubtypeFlag);
        String expectedResult = hearingSubtypeFlag.equals("true") || hearingSubtypeFlag.equals("Yes") ? "Yes" : "No";

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeTelephone());
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingTelephoneNumber());
        assertEquals(expectedResult, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeVideo());
        assertEquals(HEARING_VIDEO_EMAIL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingVideoEmail());
        assertEquals(expectedResult, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeFaceToFace());
    }

    @ParameterizedTest
    @CsvSource({"true, Yes", "Yes, Yes", "false, No", "No, No"})
    public void givenHearingSubtypeDetailsAreProvided_WithoutHearingTypeTelephoneOrVideoOrHearingTelephone_thenBuildAnAppealHearingSubtypeDetails(String hearingSubtypeFlag, String expectedResult) {

        pairs.put(HEARING_VIDEO_EMAIL_LITERAL, HEARING_VIDEO_EMAIL);
        pairs.put(HEARING_TYPE_FACE_TO_FACE_LITERAL, hearingSubtypeFlag);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeTelephone());
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingTelephoneNumber());
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeVideo());
        assertEquals(HEARING_VIDEO_EMAIL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingVideoEmail());
        assertEquals(expectedResult, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeFaceToFace());
    }

    @ParameterizedTest
    @CsvSource({"true", "Yes", "false", "No"})
    public void givenHearingSubtypeDetailsAreProvided_WithOnlyHearingTypeFaceToFace_thenBuildAnAppealHearingSubtypeDetails(String hearingSubtypeFlag) {

        pairs.put(HEARING_TYPE_FACE_TO_FACE_LITERAL, hearingSubtypeFlag);
        final String expectedResult = hearingSubtypeFlag.equals("true") || hearingSubtypeFlag.equals("Yes") ? "Yes" : "No";

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeTelephone());
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingTelephoneNumber());
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeVideo());
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingVideoEmail());
        assertEquals(expectedResult, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeFaceToFace());

    }

    @Test
    public void givenHearingSubtypeDetailsAreProvided_WithNoPairs_thenBuildAnAppealHearingSubtypeDetails() {
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeTelephone());
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingTelephoneNumber());
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeVideo());
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingVideoEmail());
        assertNull(((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getWantsHearingTypeFaceToFace());
    }

    @Test
    public void givenInvalidHearingSubtypeDetailsAreProvided_thenShowWarnings() {
        pairs.put(HEARING_TYPE_TELEPHONE_LITERAL, "test");
        pairs.put(HEARING_TYPE_VIDEO_LITERAL, "test");
        pairs.put(HEARING_TYPE_FACE_TO_FACE_LITERAL, "test");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertEquals(3, result.getErrors().size());
        assertEquals(HEARING_TYPE_TELEPHONE_LITERAL + " has an invalid value. Should be Yes/No or True/False", result.getErrors().getFirst());
        assertEquals(HEARING_TYPE_FACE_TO_FACE_LITERAL + " has an invalid value. Should be Yes/No or True/False", result.getErrors().get(1));
        assertEquals(HEARING_TYPE_VIDEO_LITERAL + " has an invalid value. Should be Yes/No or True/False", result.getErrors().get(2));
    }

    @Test
    public void givenHearingSubtypeDetailsAreProvidedWithNoHearingTelephoneNumberButWithPerson1Mobile_thenPopulateHearingTelephoneNumberWithPerson1Mobile() {

        pairs.put(HEARING_TYPE_TELEPHONE_LITERAL, "Yes");
        pairs.put(PERSON1_VALUE + MOBILE, HEARING_TELEPHONE_NUMBER);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_TELEPHONE_NUMBER, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingTelephoneNumber());
    }

    @Test
    public void givenHearingSubtypeDetailsAreProvidedWithNoHearingTelephoneNumberButWithPerson1Phone_thenPopulateHearingTelephoneNumberWithPerson1Phone() {

        pairs.put(HEARING_TYPE_TELEPHONE_LITERAL, "Yes");
        pairs.put(PERSON1_VALUE + PHONE, HEARING_TELEPHONE_NUMBER);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_TELEPHONE_NUMBER, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingTelephoneNumber());
    }

    @Test
    public void givenHearingSubtypeDetailsAreProvidedWithNoHearingTelephoneNumberButWithPerson1PhoneAndPerson1Mobile_thenPopulateHearingTelephoneNumberWithPerson1Mobile() {

        pairs.put(HEARING_TYPE_TELEPHONE_LITERAL, "Yes");
        pairs.put(PERSON1_VALUE + MOBILE, HEARING_TELEPHONE_NUMBER);
        pairs.put(PERSON1_VALUE + PHONE, "07999888777");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_TELEPHONE_NUMBER, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingTelephoneNumber());
    }

    @Test
    public void givenHearingSubtypeDetailsAreProvidedWithNoHearingVideoEmailButWithPerson1Email_thenPopulateHearingVideoEmailWithPerson1Email() {

        pairs.put(HEARING_TYPE_VIDEO_LITERAL, "Yes");
        pairs.put(PERSON1_VALUE + EMAIL, HEARING_VIDEO_EMAIL);

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(HEARING_VIDEO_EMAIL, ((Appeal) result.getTransformedCase().get("appeal")).getHearingSubtype().getHearingVideoEmail());
    }

    @Test
    public void givenAppealGrounds2Provided_thenBuildAnAppealWithAppealReasons() {

        pairs.put(APPEAL_GROUNDS, null);
        pairs.put(APPEAL_GROUNDS_2, "My appeal grounds");

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertEquals("My appeal grounds", ((Appeal) result.getTransformedCase().get("appeal")).getAppealReasons().getReasons().getFirst().getValue().getDescription());
    }

    @Test
    public void setProcessingVenue_withGivingPriorityToAppointeeOverAppellant() {
        pairs.put("benefit_type_description", BENEFIT_TYPE);
        for (String person : Arrays.asList("person1", "person2")) {
            pairs.put(person + "_address_line1", "10 my street");
            pairs.put(person + "_address_line2", "line2 address");
            pairs.put(person + "_address_line3", "London");
            pairs.put(person + "_address_line4", "county");
        }
        pairs.put("person1_postcode", APPOINTEE_POSTCODE);
        pairs.put("person2_postcode", APPELLANT_POSTCODE);

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .epimsId("rpcEpimsId").build();

        when(regionalProcessingCenterService.getByPostcode(eq(APPOINTEE_POSTCODE), anyBoolean())).thenReturn(rpc);
        when(appealPostcodeHelper.resolvePostCodeOrPort(any())).thenReturn(APPOINTEE_POSTCODE);
        when(airLookupService.lookupAirVenueNameByPostCode(eq(APPOINTEE_POSTCODE), any(BenefitType.class))).thenReturn(PROCESSING_VENUE);
        when(caseManagementLocationService.retrieveCaseManagementLocation(PROCESSING_VENUE, rpc)).thenReturn(
            Optional.of(CaseManagementLocation.builder().baseLocation("rpcEpimsId").region(REGION_ID).build()));

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(PROCESSING_VENUE, result.getTransformedCase().get("processingVenue"));

        CaseManagementLocation caseManagementLocation = (CaseManagementLocation) result.getTransformedCase().get("caseManagementLocation");
        assertNotNull(caseManagementLocation);
        assertEquals("rpcEpimsId", caseManagementLocation.getBaseLocation());
        assertEquals(REGION_ID, caseManagementLocation.getRegion());
    }

    @Test
    public void setProcessingVenue_fromAppellantAddress() {
        pairs.put("benefit_type_description", BENEFIT_TYPE);
        pairs.put("person1_address_line1", "10 my street");
        pairs.put("person1_address_line2", "line2 address");
        pairs.put("person1_address_line3", "London");
        pairs.put("person1_address_line4", "county");
        pairs.put("person1_postcode", APPELLANT_POSTCODE);

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .epimsId("rpcEpimsId").build();

        when(regionalProcessingCenterService.getByPostcode(APPELLANT_POSTCODE, false)).thenReturn(rpc);

        when(appealPostcodeHelper.resolvePostCodeOrPort(any())).thenReturn(APPELLANT_POSTCODE);
        when(airLookupService.lookupAirVenueNameByPostCode(eq(APPELLANT_POSTCODE), any(BenefitType.class))).thenReturn(PROCESSING_VENUE);
        when(caseManagementLocationService.retrieveCaseManagementLocation(PROCESSING_VENUE, rpc)).thenReturn(
            Optional.of(CaseManagementLocation.builder().baseLocation("rpcEpimsId").region(REGION_ID).build()));

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(PROCESSING_VENUE, result.getTransformedCase().get("processingVenue"));

        CaseManagementLocation caseManagementLocation = (CaseManagementLocation) result.getTransformedCase().get("caseManagementLocation");
        assertNotNull(caseManagementLocation);
        assertEquals("rpcEpimsId", caseManagementLocation.getBaseLocation());
        assertEquals(REGION_ID, caseManagementLocation.getRegion());
    }

    @Test
    public void setProcessingVenue_isIbcCase() {
        pairs.put(BenefitTypeIndicator.PIP.getIndicatorString(), false);
        pairs.put("benefit_type_description", Benefit.INFECTED_BLOOD_COMPENSATION.getDescription());
        pairs.put("person1_address_line1", "10 my street");
        pairs.put("person1_address_line2", "line2 address");
        pairs.put("person1_address_line3", "London");
        pairs.put("person1_address_line4", "county");
        pairs.put("person1_postcode", APPELLANT_POSTCODE);

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .epimsId("rpcEpimsId").build();

        when(regionalProcessingCenterService.getByPostcode(APPELLANT_POSTCODE, true)).thenReturn(rpc);

        when(appealPostcodeHelper.resolvePostCodeOrPort(any())).thenReturn(APPELLANT_POSTCODE);
        when(airLookupService.lookupAirVenueNameByPostCode(eq(APPELLANT_POSTCODE), any(BenefitType.class))).thenReturn(PROCESSING_VENUE);
        when(caseManagementLocationService.retrieveCaseManagementLocation(PROCESSING_VENUE, rpc)).thenReturn(
            Optional.of(CaseManagementLocation.builder().baseLocation("rpcEpimsId").region(REGION_ID).build()));

        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);

        assertEquals(PROCESSING_VENUE, result.getTransformedCase().get("processingVenue"));

        CaseManagementLocation caseManagementLocation = (CaseManagementLocation) result.getTransformedCase().get("caseManagementLocation");
        assertNotNull(caseManagementLocation);
        assertEquals("rpcEpimsId", caseManagementLocation.getBaseLocation());
        assertEquals(REGION_ID, caseManagementLocation.getRegion());
    }

    @ParameterizedTest
    @CsvSource({CHILD_MAINTENANCE_NUMBER, "001"})
    public void givenSscs2FormWithChildMaintenanceNumber_thenCaseDataValueIsSet(String childMaintenance) {
        pairs.put(BENEFIT_TYPE_OTHER, "Child support");
        pairs.put(PERSON_1_CHILD_MAINTENANCE_NUMBER, childMaintenance);
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("is_paying_parent", "true");
        CaseResponse result = transformer.transformExceptionRecord(sscs2ExceptionRecord, false);
        assertNoErrorsOrWarnings(result);
        assertEquals(childMaintenance, result.getTransformedCase().get("childMaintenanceNumber"));
    }

    @Test
    public void givenSscs2FormWithOtherPartyNameAndAddressSet_thenCaseDataValueIsSet() {
        pairs.put(BENEFIT_TYPE_OTHER, "Child support");
        pairs.put("other_party_title", OTHER_PARTY_TITLE);
        pairs.put("other_party_first_name", OTHER_PARTY_FIRST_NAME);
        pairs.put("other_party_last_name", OTHER_PARTY_LAST_NAME);
        pairs.put("is_other_party_address_known", "true");
        pairs.put("other_party_address_line1", OTHER_PARTY_ADDRESS_LINE1);
        pairs.put("other_party_address_line2", OTHER_PARTY_ADDRESS_LINE2);
        pairs.put("other_party_address_line3", OTHER_PARTY_ADDRESS_LINE3);
        pairs.put("other_party_postcode", OTHER_PARTY_POSTCODE);
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("is_paying_parent", "true");
        CaseResponse result = transformer.transformExceptionRecord(sscs2ExceptionRecord, false);
        assertNoErrorsOrWarnings(result);

        @SuppressWarnings("unchecked")
        OtherParty otherParty = ((List<CcdValue<OtherParty>>) result.getTransformedCase().get("otherParties")).getFirst().getValue();
        assertEquals(OTHER_PARTY_ID_ONE, otherParty.getId());
        Name otherPartyName = otherParty.getName();
        assertEquals(OTHER_PARTY_TITLE, otherPartyName.getTitle());
        assertEquals(OTHER_PARTY_FIRST_NAME, otherPartyName.getFirstName());
        assertEquals(OTHER_PARTY_LAST_NAME, otherPartyName.getLastName());
        Address otherPartyAddress = otherParty.getAddress();
        assertEquals(OTHER_PARTY_ADDRESS_LINE1, otherPartyAddress.getLine1());
        assertEquals(OTHER_PARTY_ADDRESS_LINE2, otherPartyAddress.getTown());
        assertEquals(OTHER_PARTY_ADDRESS_LINE3, otherPartyAddress.getCounty());
        assertEquals(OTHER_PARTY_POSTCODE, otherPartyAddress.getPostcode());
    }

    @Test
    public void givenSscs2FormWithoutChildMaintenanceNumberOrOtherPartyNameAndAddress_thenCaseDataValueIsNotSet() {
        pairs.put(BENEFIT_TYPE_OTHER, "Child support");
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("is_paying_parent", "true");
        CaseResponse result = transformer.transformExceptionRecord(sscs2ExceptionRecord, false);
        assertNoErrorsOrWarnings(result);
        assertNull(result.getTransformedCase().get("childMaintenanceNumber"));
        @SuppressWarnings("unchecked")
        List<CcdValue<OtherParty>> otherParties = ((List<CcdValue<OtherParty>>) result.getTransformedCase().get("otherParties"));
        assertNull(otherParties);
    }

    @Test
    public void givenSscs2FormWithIncorrectOtherPartyAddressSelection_thenErrorIsThrown() {
        pairs.put(BENEFIT_TYPE_OTHER, "Child support");
        pairs.put("other_party_title", OTHER_PARTY_TITLE);
        pairs.put("other_party_first_name", OTHER_PARTY_FIRST_NAME);
        pairs.put("other_party_last_name", OTHER_PARTY_LAST_NAME);
        pairs.put("is_other_party_address_known", "Invalid");
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("is_paying_parent", "true");
        CaseResponse result = transformer.transformExceptionRecord(sscs2ExceptionRecord, false);
        assertFalse(result.getErrors().isEmpty());
        assertEquals("is_other_party_address_known has an invalid value. Should be Yes/No or True/False", result.getErrors().getFirst());
        assertTrue(result.getWarnings().isEmpty());
        @SuppressWarnings("unchecked")
        List<CcdValue<OtherParty>> otherParties = ((List<CcdValue<OtherParty>>) result.getTransformedCase().get("otherParties"));
        assertNull(otherParties.getFirst().getValue().getAddress());
    }

    @Test
    public void givenSscs2FormWithPartyAddressNotSelectedButAddressEntered_thenAddressIsSet() {
        pairs.put(BENEFIT_TYPE_OTHER, "Child support");
        pairs.put("other_party_title", OTHER_PARTY_TITLE);
        pairs.put("other_party_first_name", OTHER_PARTY_FIRST_NAME);
        pairs.put("other_party_last_name", OTHER_PARTY_LAST_NAME);
        pairs.put("is_other_party_address_known", null);
        pairs.put("other_party_address_line1", OTHER_PARTY_ADDRESS_LINE1);
        pairs.put("other_party_address_line2", OTHER_PARTY_ADDRESS_LINE2);
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("is_paying_parent", "true");
        CaseResponse result = transformer.transformExceptionRecord(sscs2ExceptionRecord, false);
        assertNoErrorsOrWarnings(result);
        @SuppressWarnings("unchecked")
        List<CcdValue<OtherParty>> otherParties = ((List<CcdValue<OtherParty>>) result.getTransformedCase().get("otherParties"));
        assertNotNull(otherParties.getFirst().getValue().getAddress());
    }

    @ParameterizedTest
    @CsvSource({
        "true,false,false,,PAYING_PARENT",
        "false,true,false,,RECEIVING_PARENT",
        "false,false,true,Guardian,OTHER"
    })
    public void givenKeyValuePairsWithPerson1_thenBuildAnAppealWithAppellantAndRole(String payingParent, String receivingParent, String other, String description, AppellantRole appellantRole) {
        pairs.put(BENEFIT_TYPE_OTHER, "Child support");
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_dob", APPELLANT_DATE_OF_BIRTH);
        pairs.put("person1_nino", APPELLANT_NINO);
        pairs.put("is_paying_parent", payingParent);
        pairs.put("is_receiving_parent", receivingParent);
        pairs.put("is_another_party", other);
        pairs.put("other_party_details", description);

        CaseResponse result = transformer.transformExceptionRecord(sscs2ExceptionRecord, false);

        Name appellantName = Name.builder().title(APPELLANT_TITLE).firstName(APPELLANT_FIRST_NAME).lastName(APPELLANT_LAST_NAME).build();
        Address appellantAddress = Address.builder().line1(APPELLANT_ADDRESS_LINE1).line2(APPELLANT_ADDRESS_LINE2).town(APPELLANT_ADDRESS_LINE3).county(APPELLANT_ADDRESS_LINE4).postcode(APPELLANT_POSTCODE).build();
        Identity appellantIdentity = Identity.builder().nino(normaliseNino(APPELLANT_NINO)).dob("1987-08-12").build();
        Role role = Role.builder().name(appellantRole.getName()).description(description).build();
        Appellant expectedAppellant = Appellant.builder().name(appellantName).identity(appellantIdentity).isAppointee("No").role(role).address(appellantAddress).contact(Contact.builder().build()).build();

        Appellant appellantResult = ((Appeal) result.getTransformedCase().get("appeal")).getAppellant();
        assertThat(appellantResult)
            .usingRecursiveComparison()
            .ignoringFields("id", "appointee.id")
            .isEqualTo(expectedAppellant);

        assertTrue(result.getErrors().isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true;true;true;any;is_paying_parent, is_receiving_parent, is_another_party and other_party_details have conflicting values",
        "true;false;false;any;is_paying_parent and other_party_details have conflicting values",
        "true;false;true;any;is_paying_parent, is_another_party and other_party_details have conflicting values",
    }, delimiter = ';')
    public void givenKeyValuePairsWithPerson1AndInvalidAppellantRole_thenReturnAnWarnings(String payingParent, String receivingParent, String other, String description, String errorMessage) {
        pairs.put(BENEFIT_TYPE_OTHER, "Child support");
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_dob", APPELLANT_DATE_OF_BIRTH);
        pairs.put("person1_nino", APPELLANT_NINO);
        pairs.put("is_paying_parent", payingParent);
        pairs.put("is_receiving_parent", receivingParent);
        pairs.put("is_another_party", other);
        pairs.put("other_party_details", description);

        CaseResponse result = transformer.transformExceptionRecord(sscs2ExceptionRecord, false);
        assertFalse(result.getWarnings().isEmpty());
        assertEquals(errorMessage, result.getWarnings().getFirst());
    }

    @ParameterizedTest
    @CsvSource({"Yes, Yes, SSCS2", "No, No, SSCS2", "true, Yes, SSCS2", "false, No, SSCS2", "Yes, Yes, SSCS5", "No, No, SSCS5"})
    public void givenSscs2Or5FormAndConfidentialityRequired_thenCaseDataValueIsSet(String keepHomeAddressConfidential, String expected, FormType formType) {
        if (formType.equals(SSCS2)) {
            pairs.put(BENEFIT_TYPE_OTHER, "Child support");
        } else if (formType.equals(SSCS5)) {
            pairs.put(IS_BENEFIT_TYPE_TAX_CREDIT, true);
        }
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("keep_home_address_confidential", keepHomeAddressConfidential);
        pairs.put("is_paying_parent", "true");

        ExceptionRecord exceptionRecord = formType.equals(SSCS2) ? sscs2ExceptionRecord : sscs5ExceptionRecord;
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertNoErrorsOrWarnings(result);

        YesNo appellantConfidentialityRequired = ((Appeal) result.getTransformedCase().get("appeal")).getAppellant().getConfidentialityRequired();
        assertEquals(expected, appellantConfidentialityRequired.toString());
    }

    @ParameterizedTest
    @CsvSource({"SSCS2", "SSCS5"})
    public void givenSscs2Or5FormAndConfidentialityRequiredEmpty_thenCaseDataValueIsNull(FormType formType) {
        if (formType.equals(SSCS2)) {
            pairs.put(BENEFIT_TYPE_OTHER, "Child support");
        } else if (formType.equals(SSCS5)) {
            pairs.put(IS_BENEFIT_TYPE_TAX_CREDIT, true);
        }
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("keep_home_address_confidential", "");
        pairs.put("is_paying_parent", "true");

        ExceptionRecord exceptionRecord = formType.equals(SSCS2) ? sscs2ExceptionRecord : sscs5ExceptionRecord;
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertNoErrorsOrWarnings(result);

        YesNo appellantConfidentialityRequired = ((Appeal) result.getTransformedCase().get("appeal")).getAppellant().getConfidentialityRequired();
        assertNull(appellantConfidentialityRequired);
    }

    @ParameterizedTest
    @CsvSource({"SSCS2", "SSCS5"})
    public void givenSscs2Or5FormAndNoConfidentiality_thenCaseDataValueIsNull(FormType formType) {
        if (formType.equals(SSCS2)) {
            pairs.put(BENEFIT_TYPE_OTHER, "Child support");
        } else if (formType.equals(SSCS5)) {
            pairs.put(IS_BENEFIT_TYPE_TAX_CREDIT, true);
        }
        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("is_paying_parent", "true");

        ExceptionRecord exceptionRecord = formType.equals(SSCS2) ? sscs2ExceptionRecord : sscs5ExceptionRecord;
        CaseResponse result = transformer.transformExceptionRecord(exceptionRecord, false);
        assertNoErrorsOrWarnings(result);

        YesNo appellantConfidentialityRequired = ((Appeal) result.getTransformedCase().get("appeal")).getAppellant().getConfidentialityRequired();
        assertNull(appellantConfidentialityRequired);
    }

    @ParameterizedTest
    @CsvSource({"SSCS2", "SSCS5", "SSCS1", "SSCS1U", "SSCS1PE", "SSCS1PEU"})
    public void notAValidFormFalse(String formType) {
        assertFalse(formTypeValidator2.notAValidFormType(formType));
    }

    @ParameterizedTest
    @CsvSource({"SSCS", "SSCS55", "SSCS11", "UNKNOWN"})
    public void notAValidFormTrue(String formType) {
        assertTrue(formTypeValidator2.notAValidFormType(formType));
    }

    @Test
    public void givenNullForm_thenThrowError() {
        pairs.put(IS_BENEFIT_TYPE_TAX_CREDIT, true);

        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("is_paying_parent", "true");

        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(ocrList).id(null).exceptionRecordId("123456").formType(
            null).build();
        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).build());
        CaseResponse result = transformer2.transformExceptionRecord(exceptionRecord, false);
        assertOneError(result);
    }

    @Test
    public void givenNullFormAndInvalid_thenThrowError() {
        prepareData("SSCS");

        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(ocrList).id(null).exceptionRecordId("123456").formType(
            null).build();
        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).build());

        CaseResponse result = transformer2.transformExceptionRecord(exceptionRecord, false);

        assertOneError(result);
    }

    @Test
    public void givenNullFormWithOcrFormType_thenThrowNoError() {
        pairs.put(IS_BENEFIT_TYPE_TAX_CREDIT, true);

        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("is_paying_parent", "true");
        pairs.put("form_type", "SSCS5");

        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(ocrList).id(null).exceptionRecordId("123456").formType(
            null).build();
        given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).build());
        CaseResponse result = transformer2.transformExceptionRecord(exceptionRecord, false);
        assertNoErrorsOrWarnings(result);
    }

    private void prepareData(String formType) {
        pairs.put(IS_BENEFIT_TYPE_TAX_CREDIT, true);

        pairs.put("person1_title", APPELLANT_TITLE);
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        pairs.put("person1_email", APPELLANT_EMAIL);
        pairs.put("person1_mobile", APPELLANT_MOBILE);
        pairs.put("is_paying_parent", "true");
        pairs.put("form_type", formType);
    }

    private void assertOneError(CaseResponse result) {
        assertError(result, 1);
    }

    private void assertError(CaseResponse result, int errorCount) {
        assertEquals(errorCount, result.getErrors().size());
        assertTrue(result.getWarnings().isEmpty());
    }

    private void assertNoErrorsOrWarnings(CaseResponse result) {
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private void assertSscsDocumentFormType(CaseResponse result, String formType) {
        Object resultObject = result.getTransformedCase().get("sscsDocument");
        Assert.isInstanceOf(List.class, resultObject);

        assertThat((List<SscsDocument>)resultObject)
            .extracting(SscsDocument::getValue)
            .extracting(SscsDocumentDetails::getDocumentType)
            .contains(formType);
    }


    private CaseResponse preparingFormTypeCheckingDataAndImplementTransformExceptionRecord(String given, String input, boolean isIncludeDocument) {
        prepareData(input);

        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(ocrList).id(null).exceptionRecordId("123456").formType(
            given).build();

        if (isIncludeDocument) {
            List<InputScannedDoc> records = new ArrayList<>();
            InputScannedDoc scannedRecord = buildTestScannedRecord(DocumentLink.builder().documentUrl("www.test.com").build(), given);
            records.add(scannedRecord);

            given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(
                ScannedData.builder().ocrCaseData(pairs).records(records).openingDate(LocalDateTime.now().toLocalDate().toString()).build());
        } else {
            given(sscsJsonExtractor.extractJson(exceptionRecord)).willReturn(ScannedData.builder().ocrCaseData(pairs).build());
        }


        return transformer2.transformExceptionRecord(exceptionRecord, false);
    }

    private void checkFormTypeNoThrowError(String given, String input) {
        CaseResponse result = preparingFormTypeCheckingDataAndImplementTransformExceptionRecord(given, input, false);

        assertNoErrorsOrWarnings(result);
    }

    private void checkFormTypeWithOneError(String given, String input) {
        checkFormTypeWithError(given, input, 1);
    }

    private void checkFormTypeWithError(String given, String input, int errorCount) {
        CaseResponse result = preparingFormTypeCheckingDataAndImplementTransformExceptionRecord(given, input, false);

        assertError(result, errorCount);
    }

    private void checkFormTypeAndDocumentUpdated(String given, String input, String expected) {
        CaseResponse result = preparingFormTypeCheckingDataAndImplementTransformExceptionRecord(given, input, true);

        assertSscsDocumentFormType(result, expected);
    }

    @Test
    public void givenOtherFormTypeWithInputValidFormType_thenThrowNoError() {
        checkFormTypeNoThrowError("Other", "SSCS5");
    }

    @Test
    public void givenNullFormTypeWithInputValidFormType_thenThrowNoError() {
        checkFormTypeNoThrowError(null, "SSCS5");
    }

    @Test
    public void givenNullFormAndWithNullFormTypeInput_thenThrowError() {
        checkFormTypeWithOneError(null, null);
    }

    @Test
    public void givenValidFormAndWithNullFormTypeInput_thenThrowNoError() {
        checkFormTypeNoThrowError("sscs5", null);
    }

    @Test
    public void givenOtherFormAndWithNullFormTypeInput_thenThrowError() {
        checkFormTypeWithOneError("Other", null);
    }

    @Test
    public void givenOtherFormAndWithSscs2FormTypeInput_thenDocumentUpdated() {
        checkFormTypeAndDocumentUpdated("Other", "SSCS2", "sscs2");
    }


    @Test
    public void givenSscs2FormAndWithSscs5FormTypeInput_thenDocumentUpdated() {
        checkFormTypeAndDocumentUpdated("sscs2", "sscs5", "sscs5");
    }

    @Test
    public void givenNullFormAndWithSscs5FormTypeInput_thenDocumentUpdated() {
        checkFormTypeAndDocumentUpdated(null, "sscs5", "sscs5");
    }

    private Appeal buildTestAppealData() {
        Name appellantName = Name.builder().title(APPELLANT_TITLE).firstName(APPELLANT_FIRST_NAME).lastName(APPELLANT_LAST_NAME).build();
        Address appellantAddress = Address.builder().line1(APPELLANT_ADDRESS_LINE1).line2(APPELLANT_ADDRESS_LINE2).town(APPELLANT_ADDRESS_LINE3).county(APPELLANT_ADDRESS_LINE4).postcode(APPELLANT_POSTCODE).build();
        Identity appellantIdentity = Identity.builder().nino(normaliseNino(APPELLANT_NINO)).dob(formatDate(APPELLANT_DATE_OF_BIRTH)).build();
        Contact appellantContact = Contact.builder().phone(APPELLANT_PHONE).mobile(APPELLANT_MOBILE).email(APPELLANT_EMAIL).build();
        Appellant appellant = Appellant.builder().name(appellantName).identity(appellantIdentity).isAppointee("No").address(appellantAddress).contact(appellantContact).build();
        HearingSubtype hearingSubtype = HearingSubtype.builder().build();

        Name repName = Name.builder().title(REPRESENTATIVE_PERSON_TITLE).firstName(REPRESENTATIVE_PERSON_FIRST_NAME).lastName(REPRESENTATIVE_PERSON_LAST_NAME).build();
        Address repAddress = Address.builder().line1(REPRESENTATIVE_ADDRESS_LINE1).line2(REPRESENTATIVE_ADDRESS_LINE2).town(REPRESENTATIVE_ADDRESS_LINE3).county(REPRESENTATIVE_ADDRESS_LINE4).postcode(REPRESENTATIVE_POSTCODE).build();
        Contact repContact = Contact.builder().phone(REPRESENTATIVE_PHONE_NUMBER).email(REPRESENTATIVE_EMAIL).build();

        ExcludeDate excludeDate = ExcludeDate.builder().value(DateRange.builder().start(formatDate(HEARING_OPTIONS_EXCLUDE_DATES)).build()).build();
        List<ExcludeDate> excludedDates = new ArrayList<>();
        excludedDates.add(excludeDate);

        List<String> hearingSupportArrangements = new ArrayList<>();
        hearingSupportArrangements.add("hearingLoop");

        return Appeal.builder()
            .benefitType(BenefitType.builder().code(BENEFIT_TYPE).description(BENEFIT_TYPE_DESCRIPTION).build())
            .appellant(appellant)
            .appealReasons(AppealReasons.builder().reasons(Collections.singletonList(AppealReason.builder().value(AppealReasonDetails.builder().description(APPEAL_REASON).build()).build())).build())
            .rep(Representative.builder().hasRepresentative(YES_LITERAL).name(repName).address(repAddress).contact(repContact).organisation(REPRESENTATIVE_NAME).build())
            .mrnDetails(MrnDetails.builder().mrnDate(formatDate(MRN_DATE_VALUE)).dwpIssuingOffice("DWP PIP (5)").mrnLateReason(APPEAL_LATE_REASON).build())
            .hearingType(HEARING_TYPE_ORAL)
            .hearingSubtype(hearingSubtype)
            .hearingOptions(HearingOptions.builder()
                .scheduleHearing(YES_LITERAL)
                .excludeDates(excludedDates)
                .agreeLessNotice(YES_LITERAL)
                .arrangements(hearingSupportArrangements)
                .languageInterpreter(YES_LITERAL)
                .languages(HEARING_OPTIONS_LANGUAGE_TYPE)
                .wantsToAttend(YES_LITERAL)
                .wantsSupport(YES_LITERAL).build())
            .signer(SIGNATURE_NAME)
            .receivedVia("Paper")
            .build();
    }

    private InputScannedDoc buildTestScannedRecord(DocumentLink link, String subType) {
        return InputScannedDoc.builder()
            .scannedDate(LocalDateTime.now())
            .controlNumber("123")
            .url(link)
            .fileName("mrn.jpg")
            .type("Form")
            .subtype(subType).build();
    }

    private String formatDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return LocalDate.parse(date, formatter).toString();
    }

}
