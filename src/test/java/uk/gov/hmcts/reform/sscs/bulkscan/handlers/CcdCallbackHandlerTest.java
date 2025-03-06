package uk.gov.hmcts.reform.sscs.bulkscan.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.EPIMMS_ID;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.PROCESSING_VENUE;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.REGION_ID;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.NONE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_SERVICE_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_USER_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_USER_ID;

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
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.domain.CaseResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.handlers.CcdCallbackHandler;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.transformers.CaseTransformer;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.validators.CaseValidator;
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
import uk.gov.hmcts.reform.sscs.bulkscan.domain.CaseEvent;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.transformation.SuccessfulTransformationResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.InvalidExceptionRecordException;
import uk.gov.hmcts.reform.sscs.bulkscan.helper.AppealPostcodeHelper;
import uk.gov.hmcts.reform.sscs.bulkscan.helper.SscsDataHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.bulkscan.service.CaseManagementLocationService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

@RunWith(JUnitParamsRunner.class)
public class CcdCallbackHandlerTest {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

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

    @Before
    public void setUp() {
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
        assertThat(ccdCallbackResponse.getWarnings()).isNull();
    }

    @Test(expected = InvalidExceptionRecordException.class)
    public void should_return_exception_record_and_errors_in_callback_response_when_transformation_fails() {
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().build();

        when(caseTransformer.transformExceptionRecord(exceptionRecord, false))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("Cannot transform Appellant Date of Birth. Please enter valid date"))
                .build());

        invokeCallbackHandler(exceptionRecord);
    }

    @Test(expected = InvalidExceptionRecordException.class)
    public void should_return_exc_data_and_errors_in_callback_when_transformation_success_and_validation_fails_with_errors() {
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().build();

        CaseResponse response = CaseResponse.builder().transformedCase(transformedCase).build();

        when(caseTransformer.transformExceptionRecord(exceptionRecord, false)).thenReturn(response);

        when(caseValidator.validateExceptionRecord(response, exceptionRecord, transformedCase, false))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("NI Number is invalid"))
                .build());
        try {
            invokeCallbackHandler(exceptionRecord);
        } catch (InvalidExceptionRecordException e) {
            assertLogContains("Errors found while validating exception record id null - NI Number is invalid");
            throw e;
        }
    }

    @Test(expected = InvalidExceptionRecordException.class)
    public void should_return_exc_data_and_warning_in_callback_when_is_automated_process_true_and_transformation_success_and_validation_fails_with_warning() {
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().isAutomatedProcess(true).build();
        ImmutableList<String> warningList = ImmutableList.of("office is missing");

        CaseResponse response = CaseResponse.builder().warnings(warningList).transformedCase(transformedCase).build();

        when(caseTransformer.transformExceptionRecord(exceptionRecord, false)).thenReturn(response);

        when(caseValidator.validateExceptionRecord(response, exceptionRecord, transformedCase, false))
            .thenReturn(CaseResponse.builder()
                .warnings(warningList)
                .build());

        invokeCallbackHandler(exceptionRecord);
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

        assertThat(warningCaptor.getAllValues().get(0).getWarnings()).hasSize(1);
        assertThat(warningCaptor.getAllValues().get(0).getWarnings().get(0)).isEqualTo("First warning");

        assertThat(ccdCallbackResponse.getWarnings()).hasSize(2);
    }

    @Test(expected = InvalidExceptionRecordException.class)
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

        invokeCallbackHandler(exceptionRecord);
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
        when(caseValidator.validateValidationRecord(any(), anyBoolean(), anyBoolean())).thenReturn(caseValidationResponse);
        when(appealPostcodeHelper.resolvePostCodeOrPort(appeal.getAppellant())).thenReturn("CV35 2TD");

        when(caseManagementLocationService.retrieveCaseManagementLocation(PROCESSING_VENUE, rpc)).thenReturn(
            Optional.of(CaseManagementLocation.builder().baseLocation("rpcEpimsId").region(REGION_ID).build()));

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        assertThat(ccdCallbackResponse.getData()).isNotNull();
        assertThat(ccdCallbackResponse.getErrors()).isEmpty();
        assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        assertThat(ccdCallbackResponse.getData().getInterlocReviewState()).isEqualTo(NONE);
        assertThat(ccdCallbackResponse.getData().getCreatedInGapsFrom()).isEqualTo("readyToList");
        assertThat(ccdCallbackResponse.getData().getEvidencePresent()).isEqualTo("No");
        assertThat(ccdCallbackResponse.getData().getBenefitCode()).isEqualTo("002");
        assertThat(ccdCallbackResponse.getData().getIssueCode()).isEqualTo("DD");
        assertThat(ccdCallbackResponse.getData().getCaseCode()).isEqualTo("002DD");
        assertThat(ccdCallbackResponse.getData().getDwpRegionalCentre()).isEqualTo("Springburn");
        assertThat(ccdCallbackResponse.getData().getProcessingVenue()).isEqualTo("Cardiff");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType()).isEqualTo("DWP");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory()).isEqualTo("personalIndependencePayment");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode()).isEqualTo("PIP");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel()).isEqualTo("Personal Independence Payment");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getCode()).isEqualTo("PIP");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getLabel()).isEqualTo("Personal Independence Payment");
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
        when(caseValidator.validateValidationRecord(any(), anyBoolean(), anyBoolean())).thenReturn(caseValidationResponse);

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        assertThat(ccdCallbackResponse.getData()).isNotNull();
        assertThat(ccdCallbackResponse.getErrors()).isEmpty();
        assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        assertThat(ccdCallbackResponse.getData().getInterlocReviewState()).isEqualTo(NONE);
        assertThat(ccdCallbackResponse.getData().getCreatedInGapsFrom()).isEqualTo("readyToList");
        assertThat(ccdCallbackResponse.getData().getEvidencePresent()).isEqualTo("No");
        assertThat(ccdCallbackResponse.getData().getBenefitCode()).isEqualTo("051");
        assertThat(ccdCallbackResponse.getData().getIssueCode()).isEqualTo("DD");
        assertThat(ccdCallbackResponse.getData().getCaseCode()).isEqualTo("051DD");
        assertThat(ccdCallbackResponse.getData().getDwpRegionalCentre()).isEqualTo("Balham");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType()).isEqualTo("DWP");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory()).isEqualTo("employmentAndSupportAllowance");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode()).isEqualTo("ESA");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel()).isEqualTo("Employment and Support Allowance");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getCode()).isEqualTo("ESA");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getLabel()).isEqualTo("Employment and Support Allowance");
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
        when(caseValidator.validateValidationRecord(any(), eq(true), anyBoolean())).thenReturn(caseValidationResponse);

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData(), EventType.DIRECTION_ISSUED);

        assertThat(ccdCallbackResponse.getData()).isNotNull();
        assertThat(ccdCallbackResponse.getErrors()).isEmpty();
        assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        assertThat(ccdCallbackResponse.getData().getInterlocReviewState()).isEqualTo(NONE);
        assertThat(ccdCallbackResponse.getData().getCreatedInGapsFrom()).isEqualTo("readyToList");
        assertThat(ccdCallbackResponse.getData().getEvidencePresent()).isEqualTo("No");
        assertThat(ccdCallbackResponse.getData().getBenefitCode()).isEqualTo("051");
        assertThat(ccdCallbackResponse.getData().getIssueCode()).isEqualTo("DD");
        assertThat(ccdCallbackResponse.getData().getCaseCode()).isEqualTo("051DD");
        assertThat(ccdCallbackResponse.getData().getDwpRegionalCentre()).isEqualTo("Balham");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType()).isEqualTo("DWP");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory()).isEqualTo("employmentAndSupportAllowance");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode()).isEqualTo("ESA");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel()).isEqualTo("Employment and Support Allowance");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getCode()).isEqualTo("ESA");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getLabel()).isEqualTo("Employment and Support Allowance");
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
        when(caseValidator.validateValidationRecord(any(), eq(true), anyBoolean())).thenReturn(caseValidationResponse);

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData(), EventType.DIRECTION_ISSUED_WELSH);

        assertThat(ccdCallbackResponse.getData()).isNotNull();
        assertThat(ccdCallbackResponse.getErrors()).isEmpty();
        assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        assertThat(ccdCallbackResponse.getData().getInterlocReviewState()).isEqualTo(NONE);
        assertThat(ccdCallbackResponse.getData().getCreatedInGapsFrom()).isEqualTo("readyToList");
        assertThat(ccdCallbackResponse.getData().getEvidencePresent()).isEqualTo("No");
        assertThat(ccdCallbackResponse.getData().getBenefitCode()).isEqualTo("051");
        assertThat(ccdCallbackResponse.getData().getIssueCode()).isEqualTo("DD");
        assertThat(ccdCallbackResponse.getData().getCaseCode()).isEqualTo("051DD");
        assertThat(ccdCallbackResponse.getData().getDwpRegionalCentre()).isEqualTo("Balham");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType()).isEqualTo("DWP");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory()).isEqualTo("employmentAndSupportAllowance");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode()).isEqualTo("ESA");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel()).isEqualTo("Employment and Support Allowance");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getCode()).isEqualTo("ESA");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getLabel()).isEqualTo("Employment and Support Allowance");
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
        when(caseValidator.validateValidationRecord(any(), eq(false), anyBoolean())).thenReturn(caseValidationResponse);

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        assertThat(ccdCallbackResponse.getData()).isNotNull();
        assertThat(ccdCallbackResponse.getErrors()).hasSize(1);
        assertThat(ccdCallbackResponse.getErrors()).contains("Mrn date is empty");
        assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        assertThat(ccdCallbackResponse.getData().getInterlocReviewState()).isEqualTo(NONE);
        assertThat(ccdCallbackResponse.getData().getCreatedInGapsFrom()).isEqualTo("readyToList");
        assertThat(ccdCallbackResponse.getData().getEvidencePresent()).isEqualTo("No");
        assertThat(ccdCallbackResponse.getData().getBenefitCode()).isEqualTo("051");
        assertThat(ccdCallbackResponse.getData().getIssueCode()).isEqualTo("DD");
        assertThat(ccdCallbackResponse.getData().getCaseCode()).isEqualTo("051DD");
        assertThat(ccdCallbackResponse.getData().getDwpRegionalCentre()).isEqualTo("Balham");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic()).isEqualTo("Fred Ward");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType()).isEqualTo("DWP");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory()).isEqualTo("employmentAndSupportAllowance");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode()).isEqualTo("ESA");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel()).isEqualTo("Employment and Support Allowance");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getCode()).isEqualTo("ESA");
        assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getLabel()).isEqualTo("Employment and Support Allowance");
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

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), anyBoolean()))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("NI Number is invalid"))
                .build());

        // when
        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        // then
        assertThat(ccdCallbackResponse.getErrors()).containsOnly("NI Number is invalid");
        assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
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

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), anyBoolean()))
            .thenReturn(CaseResponse.builder()
                .warnings(ImmutableList.of("Postcode is invalid"))
                .build());

        // when
        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        // then
        assertThat(ccdCallbackResponse.getErrors()).containsOnly("Postcode is invalid");
        assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        assertLogContains("Warnings found while validating exception record id 123 - Postcode is invalid");
    }

    @Test
    @Parameters({"", " ", "null", "Invalid"})
    public void should_return_error_for_invalid_benefitType(@Nullable String benefitType) {
        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitType).build()).build()).benefitCode(benefitType).ccdCaseId("123").build())
            .state("ScannedRecordReceived")
            .caseId("123")
            .build();

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), anyBoolean()))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("Benefit type is invalid"))
                .build());
        // when
        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        // then
        assertThat(ccdCallbackResponse.getErrors()).containsOnly("Benefit type is invalid");
        assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
    }

    @Test
    public void givenBlankBenefitTypeSscs1_shouldReturnUnknownCmc() {
        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().formType(FormType.SSCS1).appeal(Appeal.builder().benefitType(BenefitType.builder().code("").build()).build()).benefitCode("").ccdCaseId("123").build())
            .state("ScannedRecordReceived")
            .caseId("123")
            .build();

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), anyBoolean()))
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

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), anyBoolean()))
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

        when(caseValidator.validateValidationRecord(any(), anyBoolean(), anyBoolean()))
            .thenReturn(CaseResponse.builder()
                .errors(ImmutableList.of("Benefit type is invalid"))
                .build());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getCaseData());

        assertEquals("sscs5Unknown", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals("SSCS5 Unknown", ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
    }


    @Test
    public void should_pass_validateIbcRoleField_true_for_valid_appeal_event_sscs8_form() {
        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().formType(FormType.SSCS8).build())
            .build();

        CaseResponse caseValidationResponse = CaseResponse.builder().build();
        when(caseValidator.validateValidationRecord(any(), anyBoolean(), anyBoolean())).thenReturn(caseValidationResponse);

        invokeValidationCallbackHandler(caseDetails.getCaseData());

        verify(caseValidator).validateValidationRecord(any(), anyBoolean(), eq(true));
    }

    @Test
    public void should_pass_validateIbcRoleField_false_for_non_valid_appeal_event_sscs8_form() {
        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().formType(FormType.SSCS8).build())
            .build();

        CaseResponse caseValidationResponse = CaseResponse.builder().build();
        when(caseValidator.validateValidationRecord(any(), anyBoolean(), anyBoolean())).thenReturn(caseValidationResponse);

        invokeValidationCallbackHandler(caseDetails.getCaseData(), EventType.DIRECTION_ISSUED);

        verify(caseValidator).validateValidationRecord(any(), anyBoolean(), eq(false));
    }

    @Test
    public void should_pass_validateIbcRoleField_false_for_valid_appeal_event_non_sscs8_form() {
        SscsCaseDetails caseDetails = SscsCaseDetails
            .builder()
            .caseData(SscsCaseData.builder().formType(FormType.SSCS2).build())
            .build();

        CaseResponse caseValidationResponse = CaseResponse.builder().build();
        when(caseValidator.validateValidationRecord(any(), anyBoolean(), anyBoolean())).thenReturn(caseValidationResponse);

        invokeValidationCallbackHandler(caseDetails.getCaseData());

        verify(caseValidator).validateValidationRecord(any(), anyBoolean(), eq(false));
    }

    private void assertLogContains(final String logMessage) {
        assertThat(listAppender.list.stream().map(ILoggingEvent::getFormattedMessage)).contains(logMessage);
    }

    private void assertExceptionDataEntries(SuccessfulTransformationResponse successfulTransformationResponse) {
        assertThat(successfulTransformationResponse.getCaseCreationDetails().getCaseTypeId()).isEqualTo("Benefit");
        assertThat(successfulTransformationResponse.getCaseCreationDetails().getEventId()).isEqualTo("validAppealCreated");
        assertThat(successfulTransformationResponse.getCaseCreationDetails().getCaseData()).isEqualTo(transformedCase);
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
