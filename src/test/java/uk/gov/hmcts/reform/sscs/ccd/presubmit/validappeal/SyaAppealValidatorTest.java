package uk.gov.hmcts.reform.sscs.ccd.presubmit.validappeal;

import static java.util.Optional.empty;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.NONE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseEvent;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.validation.address.AddressValidator;
import uk.gov.hmcts.reform.sscs.ccd.validation.helper.AppealPostcodeHelper;
import uk.gov.hmcts.reform.sscs.ccd.validation.helper.SscsDataHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.CaseManagementLocationService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

@RunWith(JUnitParamsRunner.class)
public class SyaAppealValidatorTest {

    public static final String PROCESSING_VENUE = "Cardiff";
    public static final String EPIMMS_ID = "1";
    public static final String REGION_ID = "100";

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private DwpAddressLookupService dwpAddressLookupService;
    @Mock
    private CaseManagementLocationService caseManagementLocationService;
    @Mock
    private AirLookupService airLookupService;
    @Mock
    private AppealPostcodeHelper appealPostcodeHelper;
    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;
    @Mock
    private AddressValidator addressValidator;

    private SyaAppealValidator appealValidator;
    private SyaAppealValidator appealValidatorSpy;
    private ValidateAppealAboutToSubmitHandler handler;
    private ListAppender<ILoggingEvent> listAppender;

    @Before
    public void setUp() {
        openMocks(this);

        Logger fooLogger = (Logger) LoggerFactory.getLogger(SyaAppealValidator.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);

        given(dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice("PIP", "3")).willReturn("Springburn");
        given(dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice("ESA", "Balham DRT")).willReturn("Balham");
        given(airLookupService.lookupAirVenueNameByPostCode(anyString(), any(BenefitType.class))).willReturn(PROCESSING_VENUE);
        given(caseManagementLocationService.retrieveCaseManagementLocation(eq(PROCESSING_VENUE), any())).willReturn(
                Optional.of(CaseManagementLocation.builder().baseLocation(EPIMMS_ID).region(REGION_ID).build()));
        appealValidator = new SyaAppealValidator(idamService, ccdService, dwpAddressLookupService, addressValidator, List.of("Mr", "Mrs"));
        appealValidatorSpy = spy(appealValidator);

        SscsDataHelper sscsDataHelper =
                new SscsDataHelper(
                        new CaseEvent(null, "validAppealCreated", null, null),
                        airLookupService,
                        dwpAddressLookupService);
        handler = new ValidateAppealAboutToSubmitHandler(appealValidatorSpy, appealPostcodeHelper, sscsDataHelper, dwpAddressLookupService, caseManagementLocationService);
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
                .data(SscsCaseData.builder().regionalProcessingCenter(rpc)
                        .appeal(appeal).interlocReviewState(REVIEW_BY_TCW).formType(FormType.SSCS1PEU).build())
                .state("ScannedRecordReceived")
                .id(123456789L)
                .build();

        doReturn(Map.of("errors", List.of(), "warnings", List.of()))
                .when(appealValidatorSpy).validateAppeal(anyMap(), anyMap(), anyBoolean(), anyBoolean(), anyBoolean());
        when(appealPostcodeHelper.resolvePostcode(appeal.getAppellant())).thenReturn("CV35 2TD");

        when(caseManagementLocationService.retrieveCaseManagementLocation(PROCESSING_VENUE, rpc)).thenReturn(
                Optional.of(CaseManagementLocation.builder().baseLocation("rpcEpimsId").region(REGION_ID).build()));

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse =
                invokeValidationCallbackHandler(caseDetails.getData());

        Assertions.assertThat(ccdCallbackResponse.getData()).isNotNull();
        Assertions.assertThat(ccdCallbackResponse.getErrors()).isEmpty();
        Assertions.assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        Assertions.assertThat(ccdCallbackResponse.getData().getInterlocReviewState()).isEqualTo(NONE);
        Assertions.assertThat(ccdCallbackResponse.getData().getCreatedInGapsFrom()).isEqualTo("readyToList");
        Assertions.assertThat(ccdCallbackResponse.getData().getEvidencePresent()).isEqualTo("No");
        Assertions.assertThat(ccdCallbackResponse.getData().getBenefitCode()).isEqualTo("002");
        Assertions.assertThat(ccdCallbackResponse.getData().getIssueCode()).isEqualTo("DD");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseCode()).isEqualTo("002DD");
        Assertions.assertThat(ccdCallbackResponse.getData().getDwpRegionalCentre()).isEqualTo("Springburn");
        Assertions.assertThat(ccdCallbackResponse.getData().getProcessingVenue()).isEqualTo("Cardiff");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType()).isEqualTo("DWP");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory()).isEqualTo("personalIndependencePayment");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode()).isEqualTo("PIP");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel()).isEqualTo("Personal Independence Payment");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getCode()).isEqualTo("PIP");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getLabel()).isEqualTo("Personal Independence Payment");
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
                .data(SscsCaseData.builder().appeal(appeal).interlocReviewState(REVIEW_BY_TCW).formType(FormType.SSCS1PEU).build())
                .state("ScannedRecordReceived")
                .id(1234L)
                .build();

        doReturn(Map.of("errors", List.of(), "warnings", List.of()))
                .when(appealValidatorSpy).validateAppeal(anyMap(), anyMap(), anyBoolean(), anyBoolean(), anyBoolean());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

        Assertions.assertThat(ccdCallbackResponse.getData()).isNotNull();
        Assertions.assertThat(ccdCallbackResponse.getErrors()).isEmpty();
        Assertions.assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        Assertions.assertThat(ccdCallbackResponse.getData().getInterlocReviewState()).isEqualTo(NONE);
        Assertions.assertThat(ccdCallbackResponse.getData().getCreatedInGapsFrom()).isEqualTo("readyToList");
        Assertions.assertThat(ccdCallbackResponse.getData().getEvidencePresent()).isEqualTo("No");
        Assertions.assertThat(ccdCallbackResponse.getData().getBenefitCode()).isEqualTo("051");
        Assertions.assertThat(ccdCallbackResponse.getData().getIssueCode()).isEqualTo("DD");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseCode()).isEqualTo("051DD");
        Assertions.assertThat(ccdCallbackResponse.getData().getDwpRegionalCentre()).isEqualTo("Balham");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType()).isEqualTo("DWP");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory()).isEqualTo("employmentAndSupportAllowance");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode()).isEqualTo("ESA");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel()).isEqualTo("Employment and Support Allowance");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getCode()).isEqualTo("ESA");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getLabel()).isEqualTo("Employment and Support Allowance");
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
                .data(SscsCaseData.builder().appeal(appeal).interlocReviewState(REVIEW_BY_TCW).formType(FormType.SSCS1PEU).build())
                .state("ScannedRecordReceived")
                .id(1234L)
                .build();

        caseDetails.getData().setDirectionTypeDl(appealToProccedDynamicList);
        doReturn(Map.of("errors", List.of(), "warnings", List.of()))
                .when(appealValidatorSpy).validateAppeal(anyMap(), anyMap(), anyBoolean(), anyBoolean(), anyBoolean());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

        Assertions.assertThat(ccdCallbackResponse.getData()).isNotNull();
        Assertions.assertThat(ccdCallbackResponse.getErrors()).isEmpty();
        Assertions.assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        Assertions.assertThat(ccdCallbackResponse.getData().getInterlocReviewState()).isEqualTo(NONE);
        Assertions.assertThat(ccdCallbackResponse.getData().getCreatedInGapsFrom()).isEqualTo("readyToList");
        Assertions.assertThat(ccdCallbackResponse.getData().getEvidencePresent()).isEqualTo("No");
        Assertions.assertThat(ccdCallbackResponse.getData().getBenefitCode()).isEqualTo("051");
        Assertions.assertThat(ccdCallbackResponse.getData().getIssueCode()).isEqualTo("DD");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseCode()).isEqualTo("051DD");
        Assertions.assertThat(ccdCallbackResponse.getData().getDwpRegionalCentre()).isEqualTo("Balham");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType()).isEqualTo("DWP");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory()).isEqualTo("employmentAndSupportAllowance");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode()).isEqualTo("ESA");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel()).isEqualTo("Employment and Support Allowance");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getCode()).isEqualTo("ESA");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getLabel()).isEqualTo("Employment and Support Allowance");
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
                .data(SscsCaseData.builder().appeal(appeal).interlocReviewState(REVIEW_BY_TCW).formType(FormType.SSCS1PEU).build())
                .state("ScannedRecordReceived")
                .id(1234L)
                .build();

        caseDetails.getData().setDirectionTypeDl(appealToProccedDynamicList);
        doReturn(Map.of("errors", List.of(), "warnings", List.of()))
                .when(appealValidatorSpy).validateAppeal(anyMap(), anyMap(), anyBoolean(), anyBoolean(), anyBoolean());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

        Assertions.assertThat(ccdCallbackResponse.getData()).isNotNull();
        Assertions.assertThat(ccdCallbackResponse.getErrors()).isEmpty();
        Assertions.assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        Assertions.assertThat(ccdCallbackResponse.getData().getInterlocReviewState()).isEqualTo(NONE);
        Assertions.assertThat(ccdCallbackResponse.getData().getCreatedInGapsFrom()).isEqualTo("readyToList");
        Assertions.assertThat(ccdCallbackResponse.getData().getEvidencePresent()).isEqualTo("No");
        Assertions.assertThat(ccdCallbackResponse.getData().getBenefitCode()).isEqualTo("051");
        Assertions.assertThat(ccdCallbackResponse.getData().getIssueCode()).isEqualTo("DD");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseCode()).isEqualTo("051DD");
        Assertions.assertThat(ccdCallbackResponse.getData().getDwpRegionalCentre()).isEqualTo("Balham");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType()).isEqualTo("DWP");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory()).isEqualTo("employmentAndSupportAllowance");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode()).isEqualTo("ESA");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel()).isEqualTo("Employment and Support Allowance");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getCode()).isEqualTo("ESA");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getLabel()).isEqualTo("Employment and Support Allowance");
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
                .data(SscsCaseData.builder().ccdCaseId("123").appeal(appeal).interlocReviewState(REVIEW_BY_TCW).formType(FormType.SSCS1PEU).build())
                .state("ScannedRecordReceived")
                .id(123L)
                .build();

        doReturn(Map.of("warnings",List.of("Mrn date is empty")))
                .when(appealValidatorSpy).validateAppeal(anyMap(), anyMap(), anyBoolean(), anyBoolean(), anyBoolean());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

        Assertions.assertThat(ccdCallbackResponse.getData()).isNotNull();
        Assertions.assertThat(ccdCallbackResponse.getErrors()).hasSize(1);
        Assertions.assertThat(ccdCallbackResponse.getErrors()).contains("Mrn date is empty");
        Assertions.assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        Assertions.assertThat(ccdCallbackResponse.getData().getInterlocReviewState()).isEqualTo(NONE);
        Assertions.assertThat(ccdCallbackResponse.getData().getCreatedInGapsFrom()).isEqualTo("readyToList");
        Assertions.assertThat(ccdCallbackResponse.getData().getEvidencePresent()).isEqualTo("No");
        Assertions.assertThat(ccdCallbackResponse.getData().getBenefitCode()).isEqualTo("051");
        Assertions.assertThat(ccdCallbackResponse.getData().getIssueCode()).isEqualTo("DD");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseCode()).isEqualTo("051DD");
        Assertions.assertThat(ccdCallbackResponse.getData().getDwpRegionalCentre()).isEqualTo("Balham");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseNamePublic()).isEqualTo("Fred Ward");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getOgdType()).isEqualTo("DWP");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseAccessCategory()).isEqualTo("employmentAndSupportAllowance");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode()).isEqualTo("ESA");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel()).isEqualTo("Employment and Support Allowance");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getCode()).isEqualTo("ESA");
        Assertions.assertThat(ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getListItems().get(0).getLabel()).isEqualTo("Employment and Support Allowance");
        assertLogContains("Warnings found while validating exception record id 123 - Mrn date is empty");
    }

    @Test
    public void should_return_exc_data_and_errors_in_callback_when_validation_endpoint_fails_with_errors() {
        SscsCaseDetails caseDetails = SscsCaseDetails
                .builder()
                .data(SscsCaseData.builder().ccdCaseId("123").build())
                .state("ScannedRecordReceived")
                .id(123L)
                .build();

        doReturn(Map.of("errors",List.of("NI Number is invalid")))
                .when(appealValidatorSpy).validateAppeal(anyMap(), anyMap(), anyBoolean(), anyBoolean(), anyBoolean());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

        Assertions.assertThat(ccdCallbackResponse.getErrors()).containsOnly("NI Number is invalid");
        Assertions.assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        assertLogContains("Errors found while validating exception record id 123 - NI Number is invalid");
    }

    @Test
    public void should_return_exc_data_and_errors_in_callback_when_validation_endpoint_fails_with_warnings() {
        SscsCaseDetails caseDetails = SscsCaseDetails
                .builder()
                .data(SscsCaseData.builder().ccdCaseId("123").build())
                .state("ScannedRecordReceived")
                .id(123L)
                .build();

        doReturn(Map.of("warnings",List.of("Postcode is invalid")))
                .when(appealValidatorSpy).validateAppeal(anyMap(), anyMap(), anyBoolean(), anyBoolean(), anyBoolean());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

        Assertions.assertThat(ccdCallbackResponse.getErrors()).containsOnly("Postcode is invalid");
        Assertions.assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
        assertLogContains("Warnings found while validating exception record id 123 - Postcode is invalid");
    }

    @Test
    @Parameters({"", " ", "null", "Invalid"})
    public void should_return_error_for_invalid_benefitType(@Nullable String benefitType) {
        SscsCaseDetails caseDetails = SscsCaseDetails
                .builder()
                .data(SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitType).build()).build()).benefitCode(benefitType).ccdCaseId("123").build())
                .state("ScannedRecordReceived")
                .id(123L)
                .build();

        doReturn(Map.of("errors",List.of("Benefit type is invalid")))
                .when(appealValidatorSpy).validateAppeal(anyMap(), anyMap(), anyBoolean(), anyBoolean(), anyBoolean());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

        Assertions.assertThat(ccdCallbackResponse.getErrors()).containsOnly("Benefit type is invalid");
        Assertions.assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
    }

    @Test
    @Parameters({"SSCS1, sscs12Unknown, SSCS1/2 Unknown", "SSCS2, sscs12Unknown, SSCS1/2 Unknown", "SSCS5, sscs5Unknown, SSCS5 Unknown"})
    public void givenBlankBenefitTypeSscs1_shouldReturnUnknownCmc(String formType, String expectedCode, String expectedLabel) {
        SscsCaseDetails caseDetails = SscsCaseDetails
                .builder()
                .data(SscsCaseData.builder()
                        .formType(FormType.valueOf(formType))
                        .appeal(Appeal.builder().benefitType(BenefitType.builder().code("").build()).build())
                        .benefitCode("")
                        .ccdCaseId("123")
                        .build())
                .state("ScannedRecordReceived")
                .id(123L)
                .build();

        doReturn(Map.of("errors",List.of("Benefit type is invalid")))
                .when(appealValidatorSpy).validateAppeal(anyMap(), anyMap(), anyBoolean(), anyBoolean(), anyBoolean());

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

        assertEquals(expectedCode, ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals(expectedLabel, ccdCallbackResponse.getData().getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
    }

    private PreSubmitCallbackResponse<SscsCaseData> invokeValidationCallbackHandler(SscsCaseData caseData) {
        var caseDetails = new CaseDetails<>(123L, "sscs",
                State.INTERLOCUTORY_REVIEW_STATE, caseData, LocalDateTime.now(), "Benefit");
        var callback = new Callback<>(caseDetails, empty(), EventType.VALID_APPEAL, false);

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    private void assertLogContains(final String logMessage) {
        Assertions.assertThat(listAppender.list.stream().map(ILoggingEvent::getFormattedMessage)).contains(logMessage);
    }
}
