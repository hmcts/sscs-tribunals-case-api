package uk.gov.hmcts.reform.sscs.ccd.presubmit.validappeal;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.ImmutableList;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseResponse;
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
import uk.gov.hmcts.reform.sscs.ccd.validation.sscscasedata.AppealPostcodeHelper;
import uk.gov.hmcts.reform.sscs.ccd.validation.sscscasedata.AppealValidator;
import uk.gov.hmcts.reform.sscs.exception.CaseManagementLocationService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.NONE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

@RunWith(JUnitParamsRunner.class)
public class ValidateAppealAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    public static final String PROCESSING_VENUE = "Cardiff";
    public static final String REGION_ID = "100";
    public static final String HEARING_TYPE_ORAL = "oral";
    public static final String NO_LITERAL = "No";
    private ValidateAppealAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CallbackType callbackType;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private ServiceRequestExecutor serviceRequestExecutor;

    @Mock
    private PreSubmitCallbackResponse<SscsCaseData> response;
    @Mock
    private CaseManagementLocationService caseManagementLocationService;

    @Mock
    private SscsCaseData bulkScanResponseData;

    private SscsCaseData sscsCaseData;

    private ValidateAppealAboutToSubmitHandler validateAppealAboutToSubmitHandler;

    @Mock
    private AppealValidator caseValidator;
    private MrnDetails defaultMrnDetails;
    @Mock
    private AppealPostcodeHelper appealPostcodeHelper;
    private ListAppender<ILoggingEvent> listAppender;

    @Before
    public void setUp() {
        openMocks(this);

        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonValidAppealCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenValidateAppealForPreValidCase_thenSetNonDigitalToDigitalCase() {
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        sscsCaseData.setCreatedInGapsFrom(null);
        when(response.getData()).thenReturn(bulkScanResponseData);
        when(bulkScanResponseData.getCreatedInGapsFrom()).thenReturn(READY_TO_LIST.getId());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCreatedInGapsFrom(), is(READY_TO_LIST.getId()));
        verify(serviceRequestExecutor).post(callback, "https://sscs-bulk-scan.net/validate");
    }

    @Test
    public void givenValidateAppealForPreValidCase_thenSetNoDigitalToDigitalCase() {
        when(caseDetails.getState()).thenReturn(State.INCOMPLETE_APPLICATION);
        sscsCaseData.setCreatedInGapsFrom(VALID_APPEAL.getId());
        when(response.getData()).thenReturn(bulkScanResponseData);
        when(bulkScanResponseData.getCreatedInGapsFrom()).thenReturn(READY_TO_LIST.getId());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCreatedInGapsFrom(), is(READY_TO_LIST.getId()));
        verify(serviceRequestExecutor).post(callback, "https://sscs-bulk-scan.net/validate");
    }

    @Test
    public void givenValidateAppealForPreValidCase_thenSetDwpRegionalCenterToCase() {
        when(caseDetails.getState()).thenReturn(State.INCOMPLETE_APPLICATION);
        when(response.getData()).thenReturn(bulkScanResponseData);
        when(bulkScanResponseData.getDwpRegionalCentre()).thenReturn("PIP Newcastle");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("PIP Newcastle", response.getData().getDwpRegionalCentre());
        verify(serviceRequestExecutor).post(callback, "https://sscs-bulk-scan.net/validate");
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
                .caseTypeId("1234")
                .build();

        CaseResponse caseValidationResponse = CaseResponse.builder().build(); // rename CaseResponse
        when(handler.validateValidationRecord(any(), anyBoolean())).thenReturn(caseValidationResponse);
        when(appealPostcodeHelper.resolvePostcode(appeal.getAppellant())).thenReturn("CV35 2TD");

        when(caseManagementLocationService.retrieveCaseManagementLocation(PROCESSING_VENUE, rpc)).thenReturn(
                Optional.of(CaseManagementLocation.builder().baseLocation("rpcEpimsId").region(REGION_ID).build()));

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

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
                .caseTypeId("1234")
                .build();

        CaseResponse caseValidationResponse = CaseResponse.builder().build();
        when(handler.validateValidationRecord(any(), anyBoolean())).thenReturn(caseValidationResponse);

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
                .caseTypeId("1234")
                .build();

        caseDetails.getData().setDirectionTypeDl(appealToProccedDynamicList);
        CaseResponse caseValidationResponse = CaseResponse.builder().build();
        when(handler.validateValidationRecord(any(), eq(true))).thenReturn(caseValidationResponse);

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData(), EventType.DIRECTION_ISSUED);

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
                .caseTypeId("1234")
                .build();

        caseDetails.getData().setDirectionTypeDl(appealToProccedDynamicList);
        CaseResponse caseValidationResponse = CaseResponse.builder().build();
        when(handler.validateValidationRecord(any(), eq(true))).thenReturn(caseValidationResponse);

        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData(), EventType.DIRECTION_ISSUED_WELSH);

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
                .caseTypeId("123")
                .build();

        CaseResponse caseValidationResponse = CaseResponse.builder().warnings(Lists.list("Mrn date is empty")).build();
        when(handler.validateValidationRecord(any(), eq(false))).thenReturn(caseValidationResponse);

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

    private void assertLogContains(final String logMessage) {
        Assertions.assertThat(listAppender.list.stream().map(ILoggingEvent::getFormattedMessage)).contains(logMessage);
    }

    @Test
    public void should_return_exc_data_and_errors_in_callback_when_validation_endpoint_fails_with_errors() {
        SscsCaseDetails caseDetails = SscsCaseDetails
                .builder()
                .data(SscsCaseData.builder().ccdCaseId("123").build())
                .state("ScannedRecordReceived")
                .caseTypeId("123")
                .build();

        when(handler.validateValidationRecord(any(), anyBoolean()))
                .thenReturn(CaseResponse.builder()
                        .errors(ImmutableList.of("NI Number is invalid"))
                        .build());

        // when
        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

        // then
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
                .caseTypeId("123")
                .build();

        when(handler.validateValidationRecord(any(), anyBoolean()))
                .thenReturn(CaseResponse.builder()
                        .warnings(ImmutableList.of("Postcode is invalid"))
                        .build());

        // when
        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

        // then
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
                .caseTypeId("123")
                .build();

        when(handler.validateValidationRecord(any(), anyBoolean()))
                .thenReturn(CaseResponse.builder()
                        .errors(ImmutableList.of("Benefit type is invalid"))
                        .build());
        // when
        PreSubmitCallbackResponse<SscsCaseData> ccdCallbackResponse = invokeValidationCallbackHandler(caseDetails.getData());

        // then
        Assertions.assertThat(ccdCallbackResponse.getErrors()).containsOnly("Benefit type is invalid");
        Assertions.assertThat(ccdCallbackResponse.getWarnings()).isEmpty();
    }

    private PreSubmitCallbackResponse<SscsCaseData> invokeValidationCallbackHandler(SscsCaseData caseDetails) {
        return invokeValidationCallbackHandler(caseDetails, EventType.VALID_APPEAL);
    }

    private PreSubmitCallbackResponse<SscsCaseData> invokeValidationCallbackHandler(SscsCaseData caseDetails, EventType eventType) {
        uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails<SscsCaseData> c = new uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails<>(123L, "sscs",
                State.INTERLOCUTORY_REVIEW_STATE, caseDetails, LocalDateTime.now(), "Benefit");

        return validateAppealAboutToSubmitHandler.handle(callbackType,
                new Callback<>(c, Optional.empty(), eventType, false), USER_AUTHORISATION);
    }
}
