package uk.gov.hmcts.reform.sscs.bulkscan.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.EPIMMS_ID;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.PROCESSING_VENUE;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.REGION_ID;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_SERVICE_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_USER_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_USER_ID;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.NONE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.handlers.CcdCallbackHandler;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.transformers.CaseTransformer;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.validators.CaseValidator;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.CaseEvent;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.transformation.SuccessfulTransformationResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.InvalidExceptionRecordException;
import uk.gov.hmcts.reform.sscs.bulkscan.helper.AppealPostcodeHelper;
import uk.gov.hmcts.reform.sscs.bulkscan.helper.SscsDataHelper;
import uk.gov.hmcts.reform.sscs.bulkscan.service.CaseManagementLocationService;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.domain.CaseResponse;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

public class CcdCallbackHandlerTest {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Mock
    private CaseTransformer caseTransformer;

    @Mock
    private CaseValidator caseValidator;

    @Mock
    private DwpAddressLookupService dwpAddressLookupService;

    @Mock
    private AirLookupService airLookupService;

    @Mock
    private AppealPostcodeHelper appealPostcodeHelper;

    @Mock
    private CaseManagementLocationService caseManagementLocationService;

    private CcdCallbackHandler ccdCallbackHandler;

    @Captor
    private ArgumentCaptor<CaseResponse> warningCaptor;

    private Map<String, Object> transformedCase;

    private IdamTokens idamTokens;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        Logger fooLogger = (Logger) LoggerFactory.getLogger(CcdCallbackHandler.class);

        listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);

        SscsDataHelper sscsDataHelper =
            new SscsDataHelper(
                new CaseEvent(null, "validAppealCreated", null, null),
                airLookupService,
                dwpAddressLookupService,
                true);

        ccdCallbackHandler =
            new CcdCallbackHandler(
                caseValidator,
                sscsDataHelper,
                caseTransformer,
                appealPostcodeHelper,
                dwpAddressLookupService,
                caseManagementLocationService,
                true);

        idamTokens = IdamTokens.builder().idamOauth2Token(TEST_USER_AUTH_TOKEN).serviceAuthorization(TEST_SERVICE_AUTH_TOKEN).userId(TEST_USER_ID).build();

        given(dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice("PIP", "3")).willReturn("Springburn");
        given(dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice("ESA", "Balham DRT")).willReturn("Balham");

        given(airLookupService.lookupAirVenueNameByPostCode(anyString(), any(BenefitType.class))).willReturn(PROCESSING_VENUE);
        given(caseManagementLocationService.retrieveCaseManagementLocation(eq(PROCESSING_VENUE), any())).willReturn(
            Optional.of(CaseManagementLocation.builder().baseLocation(EPIMMS_ID).region(REGION_ID).build()));

        LocalDate localDate = LocalDate.now();

        Appeal appeal = Appeal.builder().mrnDetails(MrnDetails.builder().mrnDate(localDate.format(formatter)).build()).build();
        transformedCase = new HashMap<>();
        transformedCase.put("appeal", appeal);
    }

    @Test
    public void should_return_exception_data_with_case_id_and_state_when_transformation_and_validation_are_successful() {
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().build();

        CaseResponse response = CaseResponse.builder().transformedCase(transformedCase).build();

        when(caseTransformer.transformExceptionRecord(exceptionRecord, false)).thenReturn(response);

        // No errors and warnings are populated hence validation would be successful
        CaseResponse caseValidationResponse = CaseResponse.builder().transformedCase(transformedCase).build();
        when(caseValidator.validateExceptionRecord(response, exceptionRecord, transformedCase, false))
            .thenReturn(caseValidationResponse);

        SuccessfulTransformationResponse ccdCallbackResponse = invokeCallbackHandler(exceptionRecord);

        assertExceptionDataEntries(ccdCallbackResponse);
        assertNull(ccdCallbackResponse.getWarnings());
    }

    @Test
    public void should_return_exception_record_and_errors_in_callback_response_when_transformation_fails() {
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().build();

        when(caseTransformer.transformExceptionRecord(exceptionRecord, false))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("Cannot transform Appellant Date of Birth. Please enter valid date"))
                .build());
        assertThrows(InvalidExceptionRecordException.class, () -> invokeCallbackHandler(exceptionRecord));
    }

    @Test
    public void should_return_exc_data_and_errors_in_callback_when_transformation_success_and_validation_fails_with_errors() {
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().build();

        CaseResponse response = CaseResponse.builder().transformedCase(transformedCase).build();

        when(caseTransformer.transformExceptionRecord(exceptionRecord, false)).thenReturn(response);

        when(caseValidator.validateExceptionRecord(response, exceptionRecord, transformedCase, false))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("NI Number is invalid"))
                .build());
        assertThrows(InvalidExceptionRecordException.class, () -> invokeCallbackHandler(exceptionRecord));
        assertLogContains("Errors found while validating exception record id null - NI Number is invalid");
    }

    @Test
    public void should_return_exc_data_and_warning_in_callback_when_is_automated_process_true_and_transformation_success_and_validation_fails_with_warning() {
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().isAutomatedProcess(true).build();
        ImmutableList<String> warningList = ImmutableList.of("office is missing");

        CaseResponse response = CaseResponse.builder().warnings(warningList).transformedCase(transformedCase).build();

        when(caseTransformer.transformExceptionRecord(exceptionRecord, false)).thenReturn(response);

        when(caseValidator.validateExceptionRecord(response, exceptionRecord, transformedCase, false))
            .thenReturn(CaseResponse.builder()
                .warnings(warningList)
                .build());

        assertThrows(InvalidExceptionRecordException.class, () -> invokeCallbackHandler(exceptionRecord));
    }

    @Test
    public void givenAWarningInTransformationServiceAndAnotherWarningInValidationService_thenShowBothWarnings() {
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().build();

        List<String> warnings = new ArrayList<>();
        warnings.add("First warning");

        when(caseTransformer.transformExceptionRecord(exceptionRecord, false))
            .thenReturn(CaseResponse.builder()
                .transformedCase(transformedCase)
                .warnings(warnings)
                .build());

        List<String> warnings2 = new ArrayList<>();
        warnings2.add("First warning");
        warnings2.add("Second warning");

        CaseResponse caseValidationResponse = CaseResponse.builder().warnings(warnings2).transformedCase(transformedCase).build();

        when(caseValidator.validateExceptionRecord(any(), eq(exceptionRecord), eq(transformedCase), eq(false)))
            .thenReturn(caseValidationResponse);

        SuccessfulTransformationResponse ccdCallbackResponse = invokeCallbackHandler(exceptionRecord);

        verify(caseValidator).validateExceptionRecord(warningCaptor.capture(), eq(exceptionRecord), eq(transformedCase), eq(false));
        assertEquals(2, ccdCallbackResponse.getWarnings().size());
        assertEquals(1, warningCaptor.getAllValues().getFirst().getWarnings().size());
        assertEquals("First warning", warningCaptor.getAllValues().getFirst().getWarnings().getFirst());
    }

    @Test
    public void givenAWarningInValidationServiceWhenIsAutomatedProcessIsTrue_thenShowWarnings() {
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().isAutomatedProcess(true).build();

        List<String> warnings = new ArrayList<>();

        when(caseTransformer.transformExceptionRecord(exceptionRecord, false))
            .thenReturn(CaseResponse.builder()
                .transformedCase(transformedCase)
                .warnings(warnings)
                .build());

        List<String> warnings2 = new ArrayList<>();
        warnings2.add("Second warning");

        CaseResponse caseValidationResponse = CaseResponse.builder().warnings(warnings2).transformedCase(transformedCase).build();

        when(caseValidator.validateExceptionRecord(any(), eq(exceptionRecord), eq(transformedCase), eq(false)))
            .thenReturn(caseValidationResponse);

        assertThrows(InvalidExceptionRecordException.class, () -> invokeCallbackHandler(exceptionRecord));
    }

    @Test
    public void should_return_no_warnings_or_errors_or_data_when_validation_endpoint_is_successful_for_pip_case() {
        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .epimsId("rpcEpimsId").build();
        Appeal appeal = Appeal.builder()
            .appellant(Appellant.builder()
                .name(Name.builder().firstName("Fred").lastName("Ward").build())
                .identity(Identity.builder().nino("JT123456N").dob("12/08/1990").build())
                .address(Address.builder().postcode("CV35 2TD").build())
                .build())
            .mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build())
            .benefitType(BenefitType.builder().code("PIP").build())
            .build();

        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().regionalProcessingCenter(rpc)
                .appeal(appeal).interlocReviewState(REVIEW_BY_TCW).formType(FormType.SSCS1PEU).build())
            .state("ScannedRecordReceived")
            .caseId("1234")
            .build();

        CaseResponse caseValidationResponse = CaseResponse.builder().build();
        when(caseValidator.validateValidationRecord(any(), anyBoolean(), any())).thenReturn(caseValidationResponse);
        when(appealPostcodeHelper.resolvePostCodeOrPort(appeal.getAppellant())).thenReturn("CV35 2TD");

        when(caseManagementLocationService.retrieveCaseManagementLocation(PROCESSING_VENUE, rpc)).thenReturn(
            Optional.of(CaseManagementLocation.builder().baseLocation("rpcEpimsId").region(REGION_ID).build()));

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        assertNotNull(ccdCallbackResponse.getData());
        assertTrue(ccdCallbackResponse.getErrors().isEmpty());
        assertTrue(ccdCallbackResponse.getWarnings().isEmpty());
        assertEquals(NONE, ccdCallbackResponse.getData().getInterlocReviewState());
        assertEquals("readyToList", ccdCallbackResponse.getData().getCreatedInGapsFrom());
        assertEquals("No", ccdCallbackResponse.getData().getEvidencePresent());
        assertEquals("002", ccdCallbackResponse.getData().getBenefitCode());
        assertEquals("DD", ccdCallbackResponse.getData().getIssueCode());
        assertEquals("002DD", ccdCallbackResponse.getData().getCaseCode());
        assertEquals("Springburn", ccdCallbackResponse.getData().getDwpRegionalCentre());
        assertEquals("Cardiff", ccdCallbackResponse.getData().getProcessingVenue());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic());
        assertEquals("DWP", ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType());
        assertEquals("personalIndependencePayment", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory());
        assertEquals("PIP", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals("Personal Independence Payment", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
        assertEquals("PIP", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().getFirst().getCode());
        assertEquals("Personal Independence Payment", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().getFirst().getLabel());
        assertEquals("rpcEpimsId", ccdCallbackResponse.getData().getCaseManagementLocation().getBaseLocation());
        assertEquals(REGION_ID, ccdCallbackResponse.getData().getCaseManagementLocation().getRegion());
    }

    @Test
    public void should_return_no_warnings_or_errors_or_data_when_validation_endpoint_is_successful_for_esa_case() {

        Appeal appeal = Appeal.builder()
            .appellant(Appellant.builder()
                .name(Name.builder().firstName("Fred").lastName("Ward").build())
                .identity(Identity.builder().nino("JT123456N").dob("12/08/1990").build())
                .build())
            .mrnDetails(MrnDetails.builder().dwpIssuingOffice("Balham DRT").build())
            .benefitType(BenefitType.builder().code("ESA").build())
            .build();

        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().appeal(appeal).interlocReviewState(REVIEW_BY_TCW).formType(FormType.SSCS1PEU).build())
            .state("ScannedRecordReceived")
            .caseId("1234")
            .build();

        CaseResponse caseValidationResponse = CaseResponse.builder().build();
        when(caseValidator.validateValidationRecord(any(), anyBoolean(), any())).thenReturn(caseValidationResponse);

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        assertNotNull(ccdCallbackResponse.getData());
        assertTrue(ccdCallbackResponse.getErrors().isEmpty());
        assertTrue(ccdCallbackResponse.getWarnings().isEmpty());
        assertEquals(NONE, ccdCallbackResponse.getData().getInterlocReviewState());
        assertEquals("readyToList", ccdCallbackResponse.getData().getCreatedInGapsFrom());
        assertEquals("No", ccdCallbackResponse.getData().getEvidencePresent());
        assertEquals("051", ccdCallbackResponse.getData().getBenefitCode());
        assertEquals("DD", ccdCallbackResponse.getData().getIssueCode());
        assertEquals("051DD", ccdCallbackResponse.getData().getCaseCode());
        assertEquals("Balham", ccdCallbackResponse.getData().getDwpRegionalCentre());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic());
        assertEquals("DWP", ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType());
        assertEquals("employmentAndSupportAllowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory());
        assertEquals("ESA", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals("Employment and Support Allowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
        assertEquals("ESA", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().getFirst().getCode());
        assertEquals("Employment and Support Allowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().getFirst().getLabel());
    }

    @Test
    public void should_return_warnings_or_error_on_data_when_direction_issued_and_mrn_date_is_empty_for_esa_case() {

        DynamicList appealToProccedDynamicList = new DynamicList(new DynamicListItem("appealToProceed", "appealToProceed"), new ArrayList<>());
        Appeal appeal = Appeal.builder()
            .appellant(Appellant.builder()
                .name(Name.builder().firstName("Fred").lastName("Ward").build())
                .identity(Identity.builder().nino("JT123456N").dob("12/08/1990").build())
                .build())
            .mrnDetails(MrnDetails.builder().mrnDate("").dwpIssuingOffice("Balham DRT").build())
            .benefitType(BenefitType.builder().code("ESA").build())
            .build();

        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().appeal(appeal).interlocReviewState(REVIEW_BY_TCW).formType(FormType.SSCS1PEU).build())
            .state("ScannedRecordReceived")
            .caseId("1234")
            .build();

        caseDetails.getCaseData().setDirectionTypeDl(appealToProccedDynamicList);
        CaseResponse caseValidationResponse = CaseResponse.builder().build();
        when(caseValidator.validateValidationRecord(any(), eq(true), any())).thenReturn(caseValidationResponse);

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData(), EventType.DIRECTION_ISSUED);

        assertNotNull(ccdCallbackResponse.getData());
        assertTrue(ccdCallbackResponse.getErrors().isEmpty());
        assertTrue(ccdCallbackResponse.getWarnings().isEmpty());
        assertEquals(NONE, ccdCallbackResponse.getData().getInterlocReviewState());
        assertEquals("readyToList", ccdCallbackResponse.getData().getCreatedInGapsFrom());
        assertEquals("No", ccdCallbackResponse.getData().getEvidencePresent());
        assertEquals("051", ccdCallbackResponse.getData().getBenefitCode());
        assertEquals("DD", ccdCallbackResponse.getData().getIssueCode());
        assertEquals("051DD", ccdCallbackResponse.getData().getCaseCode());
        assertEquals("Balham", ccdCallbackResponse.getData().getDwpRegionalCentre());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic());
        assertEquals("DWP", ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType());
        assertEquals("employmentAndSupportAllowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory());
        assertEquals("ESA", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals("Employment and Support Allowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
        assertEquals("ESA", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().getFirst().getCode());
        assertEquals("Employment and Support Allowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().getFirst().getLabel());
    }

    @Test
    public void should_return_warnings_or_error_on_data_when_direction_issued_welsh_and_mrn_date_is_empty_for_esa_case() {

        DynamicList appealToProccedDynamicList = new DynamicList(new DynamicListItem("appealToProceed", "appealToProceed"), new ArrayList<>());
        Appeal appeal = Appeal.builder()
            .appellant(Appellant.builder()
                .name(Name.builder().firstName("Fred").lastName("Ward").build())
                .identity(Identity.builder().nino("JT123456N").dob("12/08/1990").build())
                .build())
            .mrnDetails(MrnDetails.builder().mrnDate("").dwpIssuingOffice("Balham DRT").build())
            .benefitType(BenefitType.builder().code("ESA").build())
            .build();

        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().appeal(appeal).interlocReviewState(REVIEW_BY_TCW).formType(FormType.SSCS1PEU).build())
            .state("ScannedRecordReceived")
            .caseId("1234")
            .build();

        caseDetails.getCaseData().setDirectionTypeDl(appealToProccedDynamicList);
        CaseResponse caseValidationResponse = CaseResponse.builder().build();
        when(caseValidator.validateValidationRecord(any(), eq(true), any())).thenReturn(caseValidationResponse);

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData(), EventType.DIRECTION_ISSUED_WELSH);

        assertNotNull(ccdCallbackResponse.getData());
        assertTrue(ccdCallbackResponse.getErrors().isEmpty());
        assertTrue(ccdCallbackResponse.getWarnings().isEmpty());
        assertEquals(NONE, ccdCallbackResponse.getData().getInterlocReviewState());
        assertEquals("readyToList", ccdCallbackResponse.getData().getCreatedInGapsFrom());
        assertEquals("No", ccdCallbackResponse.getData().getEvidencePresent());
        assertEquals("051", ccdCallbackResponse.getData().getBenefitCode());
        assertEquals("DD", ccdCallbackResponse.getData().getIssueCode());
        assertEquals("051DD", ccdCallbackResponse.getData().getCaseCode());
        assertEquals("Balham", ccdCallbackResponse.getData().getDwpRegionalCentre());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic());
        assertEquals("DWP", ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType());
        assertEquals("employmentAndSupportAllowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory());
        assertEquals("ESA", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals("Employment and Support Allowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
        assertEquals("ESA", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().getFirst().getCode());
        assertEquals("Employment and Support Allowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().getFirst().getLabel());
    }

    @Test
    public void should_return_warnings_on_data_when_other_event_and_mrn_date_is_empty_for_esa_case() {

        Appeal appeal = Appeal.builder()
            .appellant(Appellant.builder()
                .name(Name.builder().firstName("Fred").lastName("Ward").build())
                .identity(Identity.builder().nino("JT123456N").dob("12/08/1990").build())
                .build())
            .mrnDetails(MrnDetails.builder().mrnDate("").dwpIssuingOffice("Balham DRT").build())
            .benefitType(BenefitType.builder().code("ESA").build())
            .build();

        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().ccdCaseId("123").appeal(appeal).interlocReviewState(REVIEW_BY_TCW).formType(FormType.SSCS1PEU).build())
            .state("ScannedRecordReceived")
            .caseId("123")
            .build();

        CaseResponse caseValidationResponse = CaseResponse.builder().warnings(Lists.list("Mrn date is empty")).build();
        when(caseValidator.validateValidationRecord(any(), eq(false), any())).thenReturn(caseValidationResponse);

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        assertNotNull(ccdCallbackResponse.getData());
        assertEquals(1, ccdCallbackResponse.getErrors().size());
        assertTrue(ccdCallbackResponse.getErrors().contains("Mrn date is empty"));
        assertTrue(ccdCallbackResponse.getWarnings().isEmpty());
        assertEquals(NONE, ccdCallbackResponse.getData().getInterlocReviewState());
        assertEquals("readyToList", ccdCallbackResponse.getData().getCreatedInGapsFrom());
        assertEquals("No", ccdCallbackResponse.getData().getEvidencePresent());
        assertEquals("051", ccdCallbackResponse.getData().getBenefitCode());
        assertEquals("DD", ccdCallbackResponse.getData().getIssueCode());
        assertEquals("051DD", ccdCallbackResponse.getData().getCaseCode());
        assertEquals("Balham", ccdCallbackResponse.getData().getDwpRegionalCentre());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertEquals("Fred Ward", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic());
        assertEquals("DWP", ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType());
        assertEquals("employmentAndSupportAllowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory());
        assertEquals("ESA", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals("Employment and Support Allowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
        assertEquals("ESA", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().getFirst().getCode());
        assertEquals("Employment and Support Allowance", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().getFirst().getLabel());
        assertLogContains("Warnings found while validating exception record id 123 - Mrn date is empty");
    }

    @Test
    public void should_return_exc_data_and_errors_in_callback_when_validation_endpoint_fails_with_errors() {
        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().ccdCaseId("123").build())
            .state("ScannedRecordReceived")
            .caseId("123")
            .build();

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), any()))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("NI Number is invalid"))
                .build());

        // when
        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        // then
        assertEquals(1, ccdCallbackResponse.getErrors().size());
        assertTrue(ccdCallbackResponse.getErrors().contains("NI Number is invalid"));
        assertTrue(ccdCallbackResponse.getWarnings().isEmpty());
        assertLogContains("Errors found while validating exception record id 123 - NI Number is invalid");
    }

    @Test
    public void should_return_exc_data_and_errors_in_callback_when_validation_endpoint_fails_with_warnings() {
        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().ccdCaseId("123").build())
            .state("ScannedRecordReceived")
            .caseId("123")
            .build();

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), any()))
            .thenReturn(CaseResponse.builder()
                .warnings(ImmutableList.of("Postcode is invalid"))
                .build());

        // when
        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        // then
        assertEquals(1, ccdCallbackResponse.getErrors().size());
        assertTrue(ccdCallbackResponse.getErrors().contains("Postcode is invalid"));
        assertTrue(ccdCallbackResponse.getWarnings().isEmpty());
        assertLogContains("Warnings found while validating exception record id 123 - Postcode is invalid");
    }

    @ParameterizedTest
    @CsvSource(value = {"''", "' '", "null", "Invalid"}, nullValues = "null")
    public void should_return_error_for_invalid_benefitType(String benefitType) {
        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitType).build()).build()).benefitCode(benefitType).ccdCaseId("123").build())
            .state("ScannedRecordReceived")
            .caseId("123")
            .build();

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), any()))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("Benefit type is invalid"))
                .build());
        // when
        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        // then
        assertEquals(1, ccdCallbackResponse.getErrors().size());
        assertTrue(ccdCallbackResponse.getErrors().contains("Benefit type is invalid"));
        assertTrue(ccdCallbackResponse.getWarnings().isEmpty());
    }

    @Test
    public void givenBlankBenefitTypeSscs1_shouldReturnUnknownCmc() {
        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().formType(FormType.SSCS1).appeal(Appeal.builder().benefitType(BenefitType.builder().code("").build()).build()).benefitCode("").ccdCaseId("123").build())
            .state("ScannedRecordReceived")
            .caseId("123")
            .build();

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), any()))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("Benefit type is invalid"))
                .build());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        assertEquals("sscs12Unknown", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals("SSCS1/2 Unknown", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
    }

    @Test
    public void givenBlankBenefitTypeSscs2_shouldReturnUnknownCmc() {
        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().formType(FormType.SSCS2).appeal(Appeal.builder().benefitType(BenefitType.builder().code("").build()).build()).benefitCode("").ccdCaseId("123").build())
            .state("ScannedRecordReceived")
            .caseId("123")
            .build();

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), any()))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("Benefit type is invalid"))
                .build());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        assertEquals("sscs12Unknown", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals("SSCS1/2 Unknown", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
    }

    @Test
    public void givenBlankBenefitTypeSscs5_shouldReturnUnknownCmc() {
        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().formType(FormType.SSCS5).appeal(Appeal.builder().benefitType(BenefitType.builder().code("").build()).build()).benefitCode("").ccdCaseId("123").build())
            .state("ScannedRecordReceived")
            .caseId("123")
            .build();

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), any()))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("Benefit type is invalid"))
                .build());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        assertEquals("sscs5Unknown", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals("SSCS5 Unknown", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
    }

    private void assertLogContains(final String logMessage) {
        assertTrue(listAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList().contains(logMessage));
    }

    private void assertExceptionDataEntries(SuccessfulTransformationResponse successfulTransformationResponse) {
        assertEquals("Benefit", successfulTransformationResponse.getCaseCreationDetails().getCaseTypeId());
        assertEquals("validAppealCreated", successfulTransformationResponse.getCaseCreationDetails().getEventId());
        assertEquals(transformedCase, successfulTransformationResponse.getCaseCreationDetails().getCaseData());
    }

    private SuccessfulTransformationResponse invokeCallbackHandler(ExceptionRecord exceptionRecord) {
        return ccdCallbackHandler.handle(exceptionRecord);
    }

    private PreSubmitCallbackResponse<SscsCaseData> invokeValidationCallbackHandler(SscsCaseData caseDetails) {
        return invokeValidationCallbackHandler(caseDetails, EventType.VALID_APPEAL);
    }

    private PreSubmitCallbackResponse<SscsCaseData> invokeValidationCallbackHandler(SscsCaseData caseDetails, EventType eventType) {
        uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails<SscsCaseData> c = new uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails<>(123L, "sscs",
            State.INTERLOCUTORY_REVIEW_STATE, caseDetails, LocalDateTime.now(), "Benefit");

        return ccdCallbackHandler.handleValidationAndUpdate(
            new Callback<>(c, Optional.empty(), eventType, false), idamTokens);
    }
}
