package uk.gov.hmcts.reform.sscs.bulkscan.controllers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Strings.concat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.JourneyClassification.NEW_APPLICATION;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.HEARING_EXCLUDE_DATES_MISSING;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.OcrDataBuilderTest.buildScannedValidationOcrData;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.SERVICE_AUTHORIZATION_HEADER_KEY;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.SERVICE_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.USER_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.USER_ID;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.USER_ID_HEADER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.FormType.SSCS2;
import static uk.gov.hmcts.reform.sscs.ccd.domain.FormType.SSCS5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.codec.Charsets;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.bulkscan.BaseTest;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ErrorResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.InputScannedDoc;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.OcrDataField;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.transformation.SuccessfulTransformationResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsQueryBuilder;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;

@RunWith(SpringRunner.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SscsBulkScanExceptionRecordCallback extends BaseTest {

    public static final String TRANSFORM_EXCEPTION_RECORD = "/transform-exception-record/";
    public static final String TRANSFORM_SCANNED_DATA = "/transform-scanned-data/";
    public static final String MRN_DATE_YESTERDAY_YYYY_MM_DD = LocalDate.now().minusDays(1).toString();
    public static final String MRN_DATE_YESTERDAY_DD_MM_YYYY =
        LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    public static final String COVENTRY_CMCB = "Coventry (CMCB)";
    public static final String COVENTRY_CMCB_EPIMS_ID = "1234";
    public static final String CHELMSFORD_EPIMS_ID = "555";
    public static final String CHELMSFORD = "Chelmsford";
    public static final String BASILDON_CC = "Basildon CC";
    public static final String BASILDON_CC_EPIMS_ID = "1";

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String loadJson(String fileName) throws IOException {
        URL url = getResource(fileName);
        return Resources.toString(url, Charsets.toCharset("UTF-8"));
    }

    @Before
    public void setup() {
        baseUrl = "http://localhost:" + randomServerPort;
    }

    //FIXME: delete after bulk scan auto case creation is switch on
    @Test
    public void should_handle_callback_and_return_caseid_and_state_case_created_in_exception_record_data()
        throws Exception {
        checkForLinkedCases();
        findCaseByForCaseworker(MRN_DATE_YESTERDAY_YYYY_MM_DD, "ESA");

        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithMrnDate(MRN_DATE_YESTERDAY_DD_MM_YYYY, this::addAppellant, "SSCS1")),
            httpHeaders());

        ResponseEntity<SuccessfulTransformationResponse> result =
            this.restTemplate
                .postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, SuccessfulTransformationResponse.class);

        verifyResultData(result, "mappings/exception/valid-appeal-response.json", this::getAppellantTya);
    }

    //FIXME: delete after bulk scan auto case creation is switch on
    @Test
    public void should_transform_incomplete_case_when_data_missing() throws Exception {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithMissingAppellantDetails()),
            httpHeaders()
        );

        ResponseEntity<SuccessfulTransformationResponse> result =
            this.restTemplate
                .postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, SuccessfulTransformationResponse.class);

        verifyResultData(result, "mappings/exception/case-incomplete-response.json", this::getAppellantTya);

    }

    //FIXME: delete after bulk scan auto case creation is switch on
    @Test
    public void should_create_non_compliant_case_when_mrn_date_greater_than_13_months() throws Exception {
        checkForLinkedCases();
        findCaseByForCaseworker("2017-01-01", "ESA");

        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithMrnDate("01/01/2017", this::addAppellant, "SSCS1")),
            httpHeaders()
        );

        // When
        ResponseEntity<SuccessfulTransformationResponse> result =
            this.restTemplate
                .postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, SuccessfulTransformationResponse.class);

        verifyResultData(result, "mappings/exception/case-non-compliant-response.json", this::getAppellantTya);
    }

    //FIXME: delete after bulk scan auto case creation is switch on
    @Test
    public void should_return_error_list_populated_when_exception_record_transformation_fails() {
        // Given
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithContradictingValues()),
            httpHeaders()
        );

        // When
        ResponseEntity<ErrorResponse> result =
            this.restTemplate.postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, ErrorResponse.class);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(result.getBody().errors)
            .containsOnly("is_hearing_type_oral and is_hearing_type_paper have contradicting values");

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    //FIXME: delete after bulk scan auto case creation is switch on
    @Test
    public void should_return_error_list_populated_when_key_value_pair_validation_fails() {
        // Given
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithInvalidKey()),
            httpHeaders()
        );

        // When
        ResponseEntity<ErrorResponse> result =
            this.restTemplate.postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, ErrorResponse.class);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(result.getBody().errors)
            .containsOnly("$: property 'invalid_key' is not defined in the schema and the schema does not allow additional properties");

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    //FIXME: delete after bulk scan auto case creation is switch on
    @Test
    public void should_not_create_duplicate_non_compliant_case_when_mrndate_nino_benefit_code_case_exists() throws Exception {
        // Given
        checkForLinkedCases();
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithMrnDate("01/01/2017", this::addAppellant, "SSCS1")),
            httpHeaders()
        );

        findCaseByForCaseworkerReturnCaseDetails();

        // When
        ResponseEntity<ErrorResponse> result =
            this.restTemplate.postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, ErrorResponse.class);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(result.getBody().errors)
            .containsOnly("Duplicate case already exists - please reject this exception record");

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    //FIXME: delete after bulk scan auto case creation is switch on
    @Test
    public void should_return_warnings_when_tell_tribunal_about_dates_is_true_and_no_excluded_dates_provided() {
        // Given
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithNoExcludedHearingDates()),
            httpHeaders()
        );

        // When
        ResponseEntity<SuccessfulTransformationResponse> result =
            this.restTemplate
                .postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, SuccessfulTransformationResponse.class);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getWarnings())
            .contains(HEARING_EXCLUDE_DATES_MISSING);

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    @ParameterizedTest
    @MethodSource("endPoints")
    public void should_return_status_code_401_when_service_auth_token_is_missing(String url, boolean isAuto) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, USER_AUTH_TOKEN);
        headers.set(USER_ID_HEADER, USER_ID);

        ExceptionRecord exceptionRecord =
            (isAuto) ? autoExceptionCaseData(caseData(), "SSCS1PEU") : exceptionCaseData(caseData());
        HttpEntity<ExceptionRecord> request = new HttpEntity<>(exceptionRecord, headers);

        // When
        ResponseEntity<Void> result =
            this.restTemplate.postForEntity(url, request, Void.class);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @ParameterizedTest
    @MethodSource("endPoints")
    public void should_return_status_code_403_when_service_auth_token_is_missing(String url, boolean isAuto) {
        // Given
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("forbidden_service");

        ExceptionRecord exceptionRecord =
            (isAuto) ? autoExceptionCaseData(caseData(), "SSCS1PEU") : exceptionCaseData(caseData());
        HttpEntity<ExceptionRecord> request = new HttpEntity<>(exceptionRecord, httpHeaders());

        // When
        ResponseEntity<Void> result =
            this.restTemplate.postForEntity(url, request, Void.class);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    @Test
    public void auto_scan_should_handle_callback_and_return_caseid_and_state_case_created() throws Exception {
        checkForLinkedCases();
        findCaseByForCaseworker(MRN_DATE_YESTERDAY_YYYY_MM_DD, "ESA");

        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            autoExceptionCaseData(caseDataWithMrnDate(MRN_DATE_YESTERDAY_DD_MM_YYYY, this::addAppellant, "SSCS1PEU"),
                "SSCS1PEU"),
            httpHeaders());

        ResponseEntity<SuccessfulTransformationResponse> result =
            this.restTemplate
                .postForEntity(baseUrl + TRANSFORM_SCANNED_DATA, request, SuccessfulTransformationResponse.class);

        verifyResultData(result, "mappings/exception/auto-valid-appeal-response.json", this::getAppellantTya);
    }

    @Test
    public void auto_scan_should_handle_callback_and_return_caseid_and_state_case_created_Sscs1U() throws Exception {
        checkForLinkedCases();
        findCaseByForCaseworker(MRN_DATE_YESTERDAY_YYYY_MM_DD, "attendanceAllowance");

        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            autoExceptionCaseData(caseDataWithMrnDate(MRN_DATE_YESTERDAY_DD_MM_YYYY, this::addAppellant, "SSCS1U"),
                "SSCS1U"),
            httpHeaders());

        ResponseEntity<SuccessfulTransformationResponse> result =
            this.restTemplate
                .postForEntity(baseUrl + TRANSFORM_SCANNED_DATA, request, SuccessfulTransformationResponse.class);

        verifyResultData(result, "mappings/exception/auto-valid-appeal-response-attendance-allowance.json",
            this::getAppellantTya);
    }

    @Test
    public void auto_scan_with_appointee_should_handle_callback_and_return_caseid_and_state_case_created() throws Exception {
        checkForLinkedCases();
        findCaseByForCaseworker(MRN_DATE_YESTERDAY_YYYY_MM_DD, "ESA");

        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        when(venueService.getEpimsIdForVenue(COVENTRY_CMCB)).thenReturn(COVENTRY_CMCB_EPIMS_ID);
        when(refDataService.getCourtVenueRefDataByEpimsId(COVENTRY_CMCB_EPIMS_ID)).thenReturn(CourtVenue.builder().regionId(
            "1").courtStatus("Open").build());

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(autoExceptionCaseData(
            caseDataWithMrnDate(MRN_DATE_YESTERDAY_DD_MM_YYYY, this::addAppellantAndAppointee, "SSCS1PEU"), "SSCS1PEU"),
            httpHeaders());

        ResponseEntity<SuccessfulTransformationResponse> result =
            this.restTemplate
                .postForEntity(baseUrl + TRANSFORM_SCANNED_DATA, request, SuccessfulTransformationResponse.class);

        //Epims ID on this expectation is wrong due to a mismatch in how the rpc is resolved when an appointee exists.
        //Will be fixed in SSCS-10704
        verifyResultData(result, "mappings/exception/auto-valid-appeal-with-appointee-response.json",
            this::getAppointeeTya);
    }

    @Test
    public void auto_scan_should_not_transform_incomplete_case_when_data_missing() {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            autoExceptionCaseData(caseDataWithMissingAppellantAndHearingSubTypeDetails(), "SSCS1PEU"),
            httpHeaders()
        );

        ResponseEntity<ErrorResponse> result =
            this.restTemplate.postForEntity(baseUrl + TRANSFORM_SCANNED_DATA, request, ErrorResponse.class);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(result.getBody().errors)
            .contains("person1_last_name is empty",
                "person1_address_line1 is empty",
                "person1_address_line2 is empty",
                "person1_address_line3 is empty",
                "person1_postcode is empty",
                "person1_nino is empty",
                "hearing_type_telephone, hearing_type_video and hearing_type_face_to_face are empty. At least one must be populated");

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);

    }

    @Test
    public void auto_scan_should_not_transform_case_when_tell_tribunal_about_dates_is_true_and_no_excluded_dates_provided() {
        // Given
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithNoExcludedHearingDates()),
            httpHeaders()
        );

        // When
        ResponseEntity<ErrorResponse> result =
            this.restTemplate.postForEntity(baseUrl + TRANSFORM_SCANNED_DATA, request, ErrorResponse.class);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().warnings)
            .contains("Excluded dates have been provided which must be recorded on CCD");

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    @Test
    public void should_handle_sscs2_callback_and_return_caseid_and_state_case_created_in_exception_record_data()
        throws Exception {
        checkForLinkedCases();
        findCaseByForCaseworker(MRN_DATE_YESTERDAY_YYYY_MM_DD, "childSupport");

        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        when(venueService.getEpimsIdForVenue(CHELMSFORD)).thenReturn(CHELMSFORD_EPIMS_ID);
        when(refDataService.getCourtVenueRefDataByEpimsId(CHELMSFORD_EPIMS_ID)).thenReturn(CourtVenue.builder().regionId(
            "1").courtStatus("Open").build());

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithMrnDate(MRN_DATE_YESTERDAY_DD_MM_YYYY, this::addAppellant, "SSCS2"), "SSCS2", false),
            httpHeaders());

        ResponseEntity<SuccessfulTransformationResponse> result =
            this.restTemplate
                .postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, SuccessfulTransformationResponse.class);

        verifyResultData(result, "mappings/exception/sscs2-valid-appeal-response.json", this::getAppellantTya);
    }

    @Test
    public void should_handle_sscs5_callback_and_return_caseid_and_state_case_created_in_exception_record_data()
        throws Exception {
        checkForLinkedCases();
        findCaseByForCaseworker(MRN_DATE_YESTERDAY_YYYY_MM_DD, "taxFreeChildcare");

        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        when(venueService.getEpimsIdForVenue(BASILDON_CC)).thenReturn(BASILDON_CC_EPIMS_ID);
        when(refDataService.getCourtVenueRefDataByEpimsId(BASILDON_CC_EPIMS_ID)).thenReturn(CourtVenue.builder().regionId("1").epimsId(
            BASILDON_CC_EPIMS_ID).courtStatus("Open").build());

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithMrnDate(MRN_DATE_YESTERDAY_DD_MM_YYYY, this::addAppellant, "SSCS5"), "SSCS5", false),
            httpHeaders());

        ResponseEntity<SuccessfulTransformationResponse> result =
            this.restTemplate
                .postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, SuccessfulTransformationResponse.class);

        verifyResultData(result, "mappings/exception/sscs5-valid-appeal-response.json", this::getAppellantTya);
    }

    @Test
    public void should_return_error_list_populated_when_sscs2_key_value_pair_validation_fails() {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithInvalidKey(), "SSCS2", false),
            httpHeaders()
        );

        ResponseEntity<ErrorResponse> result =
            this.restTemplate.postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, ErrorResponse.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(result.getBody().errors)
            .containsOnly("$: property 'invalid_key' is not defined in the schema and the schema does not allow additional properties");

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    @Test
    public void should_return_warning_list_populated_when_sscs2_missing_data_validation_fails() {
        checkForLinkedCases();
        findCaseByForCaseworker(MRN_DATE_YESTERDAY_YYYY_MM_DD, "childSupport");
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithoutChildMaintenanceAndPartiallyMissingOtherPartyNameAddress(), "SSCS2", false),
            httpHeaders()
        );

        ResponseEntity<ErrorResponse> result =
            this.restTemplate.postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, ErrorResponse.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().warnings)
            .contains("person1_child_maintenance_number is empty",
                "other_party_last_name is empty",
                "other_party_address_line2 is empty",
                "other_party_postcode is empty");

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    @Test
    public void should_return_warning_list_populated_when_sscs2_appellant_role_empty() {
        checkForLinkedCases();
        findCaseByForCaseworker(MRN_DATE_YESTERDAY_YYYY_MM_DD, "childSupport");
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithoutAppellantRole(), "SSCS2", false),
            httpHeaders()
        );

        ResponseEntity<ErrorResponse> result =
            this.restTemplate.postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, ErrorResponse.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().warnings)
            .containsOnly("is_paying_parent, is_receiving_parent, is_another_party and other_party_details fields are empty");

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    @Test
    public void should_return_no_warning_when_sscs2_appellant_role_empty_ignore_warnings() {
        checkForLinkedCases();
        findCaseByForCaseworker(MRN_DATE_YESTERDAY_YYYY_MM_DD, "childSupport");
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithoutAppellantRole(), "SSCS2", true),
            httpHeaders()
        );

        ResponseEntity<ErrorResponse> result =
            this.restTemplate.postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, ErrorResponse.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().warnings).doesNotContain("is_paying_parent, is_receiving_parent, is_another_party and other_party_details fields are empty");

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    @Test
    public void should_return_warning_list_populated_when_sscs2_appellant_role_invalid() {
        checkForLinkedCases();
        findCaseByForCaseworker(MRN_DATE_YESTERDAY_YYYY_MM_DD, "childSupport");
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithInvalidAppellantRole(), "SSCS2", false),
            httpHeaders()
        );

        ResponseEntity<ErrorResponse> result =
            this.restTemplate.postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, ErrorResponse.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().warnings)
            .containsOnly("is_paying_parent, is_receiving_parent and is_another_party have conflicting values");

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    @Test
    public void should_return_no_warning_sscs2_appellant_role_invalid_ignore_warning() {
        checkForLinkedCases();
        findCaseByForCaseworker(MRN_DATE_YESTERDAY_YYYY_MM_DD, "childSupport");
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        HttpEntity<ExceptionRecord> request = new HttpEntity<>(
            exceptionCaseData(caseDataWithInvalidAppellantRole(), "SSCS2", true),
            httpHeaders()
        );

        ResponseEntity<ErrorResponse> result =
            this.restTemplate.postForEntity(baseUrl + TRANSFORM_EXCEPTION_RECORD, request, ErrorResponse.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().warnings)
            .doesNotContain("is_paying_parent, is_receiving_parent and is_another_party have conflicting values");

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    //FIXME: update after bulk scan auto case creation is switch on
    private Object[] endPoints() {
        return new Object[] {
            new Object[] {"http://localhost:" + randomServerPort + TRANSFORM_EXCEPTION_RECORD, false},
            new Object[] {"http://localhost:" + randomServerPort + TRANSFORM_SCANNED_DATA, true}
        };
    }

    private Map<String, Object> caseDataWithContradictingValues() {
        Map<String, Object> ocrList = new HashMap<>();

        ocrList.put("is_hearing_type_oral", true);
        ocrList.put("is_hearing_type_paper", true);

        return exceptionRecord(ocrList, null);
    }

    private Map<String, Object> caseDataWithNoExcludedHearingDates() {
        Map<String, Object> ocrList = new HashMap<>();

        ocrList.put("tell_tribunal_about_dates", true);
        ocrList.put("hearing_options_exclude_dates", "");

        return exceptionRecord(ocrList, null);
    }

    private Map<String, Object> caseDataWithInvalidKey() {
        Map<String, Object> ocrList = new HashMap<>();

        ocrList.put("invalid_key", true);

        return exceptionRecord(ocrList, null);
    }

    private Map<String, Object> caseDataWithMissingAppellantDetails() {
        Map<String, Object> ocrList = new HashMap<>();

        ocrList.put("mrn_date", "09/12/2018");
        ocrList.put("office", "Balham DRT");
        ocrList.put("contains_mrn", true);
        ocrList.put("benefit_type_description", "ESA");
        ocrList.put("person1_title", "Mr");
        ocrList.put("person1_first_name", "John");
        ocrList.put("is_hearing_type_oral", true);
        ocrList.put("is_hearing_type_paper", false);

        return exceptionRecord(ocrList, null);
    }

    private Map<String, Object> caseDataWithMissingAppellantAndHearingSubTypeDetails() {
        Map<String, Object> ocrList = new HashMap<>();

        ocrList.put("mrn_date", "09/12/2018");
        ocrList.put("office", "Balham DRT");
        ocrList.put("contains_mrn", true);
        ocrList.put("is_benefit_type_esa", "true");
        ocrList.put("person1_title", "Mr");
        ocrList.put("person1_first_name", "John");
        ocrList.put("is_hearing_type_oral", true);
        ocrList.put("is_hearing_type_paper", false);
        ocrList.put("hearing_type_telephone", "");
        ocrList.put("hearing_type_video", "");
        ocrList.put("hearing_type_face_to_face", "");


        return exceptionRecord(ocrList, null);
    }

    private Map<String, Object> exceptionRecord(Map<String, Object> ocrList, List<InputScannedDoc> docList) {
        Map<String, Object> exceptionRecord = new HashMap<>();
        exceptionRecord.put("scanOCRData", ocrList);
        exceptionRecord.put("scannedDocuments", docList);
        return exceptionRecord;
    }

    //FIXME: delete after bulk scan auto case creation is switch on
    private ExceptionRecord exceptionCaseData(Map<String, Object> caseData) {
        return exceptionCaseData(caseData, "SSCS1", false);
    }

    @SuppressWarnings("unchecked")
    private ExceptionRecord exceptionCaseData(Map<String, Object> caseData, String formType, boolean ignoreWarnings) {
        Map<String, Object> scannedData = (HashMap<String, Object>) caseData.get("scanOCRData");
        List<OcrDataField> scanOcrData = getOcrDataFields(scannedData);

        return ExceptionRecord.builder()
            .ocrDataFields(scanOcrData)
            .poBox("SSCSPO")
            .jurisdiction("SSCS")
            .formType(formType)
            .journeyClassification(NEW_APPLICATION)
            .ignoreWarnings(ignoreWarnings)
            .scannedDocuments((List<InputScannedDoc>) caseData.get("scannedDocuments"))
            .id("1234567890")
            .openingDate(LocalDateTime.parse("2018-01-11 12:00:00", formatter))
            .deliveryDate(LocalDateTime.parse("2018-01-11 12:00:00", formatter))
            .envelopeId("envelopeId")
            .isAutomatedProcess(false)
            .exceptionRecordId(null)
            .build();
    }

    @SuppressWarnings("unchecked")
    private ExceptionRecord autoExceptionCaseData(Map<String, Object> caseData, String formType) {
        Map<String, Object> scannedData = (HashMap<String, Object>) caseData.get("scanOCRData");
        List<OcrDataField> scanOcrData = getOcrDataFields(scannedData);

        return ExceptionRecord.builder()
            .ocrDataFields(scanOcrData)
            .poBox("SSCSPO")
            .jurisdiction("SSCS")
            .formType(formType)
            .journeyClassification(NEW_APPLICATION)
            .scannedDocuments((List<InputScannedDoc>) caseData.get("scannedDocuments"))
            .id(null)
            .openingDate(LocalDateTime.parse("2018-01-11 12:00:00", formatter))
            .deliveryDate(LocalDateTime.parse("2018-01-11 12:00:00", formatter))
            .envelopeId("envelopeId")
            .isAutomatedProcess(true)
            .exceptionRecordId("1234567891011")
            .build();
    }

    @SuppressWarnings("unchecked")
    private List<OcrDataField> getOcrDataFields(Map<String, Object> scannedData) {
        return (List<OcrDataField>) buildScannedValidationOcrData(scannedData.entrySet().stream().map(f -> {
            HashMap<String, Object> valueMap = new HashMap<>();
            valueMap.put("name", f.getKey());
            valueMap.put("value", f.getValue());
            return valueMap;
        }).toArray(HashMap[]::new));
    }

    private Map<String, Object> caseData() {
        return caseDataWithMrnDate("09/12/2018", this::addAppellant, "SSCS1PEU");
    }

    private Map<String, Object> caseDataWithMrnDate(String mrnDate, Consumer<Map<String, Object>> addPersonDetails,
                                                    String formType) {
        Map<String, Object> ocrList = new HashMap<>();
        addPersonDetails.accept(ocrList);

        List<InputScannedDoc> docList = new ArrayList<>();

        LocalDateTime dateTime = LocalDateTime.parse("2018-10-10 12:00:00", formatter);

        docList.add(InputScannedDoc.builder().scannedDate(dateTime)
                .controlNumber("11111")
                .url(DocumentLink.builder()
                    .documentUrl("http://www.bbc.com")
                    .documentBinaryUrl("http://www.bbc.com/binary")
                    .documentFilename("myfile.jpg").build())
                .type("other")
                .subtype("my subtype")
                .fileName("11111.pdf")
                .build());

        ocrList.put("mrn_date", mrnDate);
        if (formType.toLowerCase().equals(FormType.SSCS1U.toString())) {
            ocrList.put("office", "The Pension Service 11");
        } else if (!formType.toLowerCase().equals(SSCS2.toString()) && !formType.toLowerCase().equals(SSCS5.toString())) {
            ocrList.put("office", "Balham DRT");
        }
        ocrList.put("contains_mrn", true);

        if (formType.toLowerCase().equals(FormType.SSCS1.toString())) {
            ocrList.put("benefit_type_description", "ESA");
        } else if (formType.toLowerCase().equals(FormType.SSCS1U.toString())) {
            ocrList.put("is_benefit_type_other", false);
            ocrList.put("benefit_type_other", "Attendance Allowance");
        } else if (formType.toLowerCase().equals(SSCS5.toString())) {
            ocrList.put("is_benefit_type_tax_free_childcare", "true");
        } else if (!formType.toLowerCase().equals(SSCS2.toString())) {
            ocrList.put("is_benefit_type_esa", "true");
        }

        if (formType.toLowerCase().equals(SSCS2.toString())) {
            ocrList.put("person1_child_maintenance_number", "Test1234");
            ocrList.put("is_paying_parent", "true");
            addOtherParty(ocrList);
        }


        ocrList.put("is_hearing_type_oral", true);
        ocrList.put("is_hearing_type_paper", false);
        ocrList.put("hearing_options_exclude_dates", "01/12/2030");
        ocrList.put("hearing_type_telephone", "Yes");
        ocrList.put("hearing_telephone_number", "01234567890");
        ocrList.put("hearing_type_video", "Yes");
        ocrList.put("hearing_video_email", "my@email.com");
        ocrList.put("hearing_type_face_to_face", "No");

        return exceptionRecord(ocrList, docList);
    }

    private void addAppellant(Map<String, Object> ocrList) {
        ocrList.put("person1_title", "Mr");
        ocrList.put("person1_first_name", "John");
        ocrList.put("person1_last_name", "Smith");
        ocrList.put("person1_address_line1", "2 Drake Close");
        ocrList.put("person1_address_line2", "Hutton");
        ocrList.put("person1_address_line3", "Brentwood");
        ocrList.put("person1_address_line4", "Essex");
        ocrList.put("person1_postcode", "CM13 1AQ");
        ocrList.put("person1_phone", "01234567899");
        ocrList.put("person1_mobile", "07411222222");
        ocrList.put("person1_dob", "11/11/1976");
        ocrList.put("person1_nino", "BB000000B");
    }

    private void addAppellantAndAppointee(Map<String, Object> ocrList) {
        ocrList.put("person1_title", "Mr");
        ocrList.put("person1_first_name", "Tyrion");
        ocrList.put("person1_last_name", "Lannister");
        ocrList.put("person1_address_line1", "2 Casterly Rock");
        ocrList.put("person1_address_line2", "Benedictine");
        ocrList.put("person1_address_line3", "Coventry");
        ocrList.put("person1_address_line4", "Warwickshire");
        ocrList.put("person1_postcode", "CV3 6GU");
        ocrList.put("person1_phone", "01234567899");
        ocrList.put("person1_mobile", "07411222222");
        ocrList.put("person1_dob", "11/11/1976");
        ocrList.put("person1_nino", "BB000000B");
        ocrList.put("person2_title", "Mr");
        ocrList.put("person2_first_name", "John");
        ocrList.put("person2_last_name", "Smith");
        ocrList.put("person2_address_line1", "2 Drake Close");
        ocrList.put("person2_address_line2", "Hutton");
        ocrList.put("person2_address_line3", "Brentwood");
        ocrList.put("person2_address_line4", "Essex");
        ocrList.put("person2_postcode", "CM13 1AQ");
        ocrList.put("person2_dob", "11/11/1976");
        ocrList.put("person2_nino", "BB000000B");
    }

    private void addOtherParty(Map<String, Object> ocrList) {
        ocrList.put("other_party_title", "Mrs");
        ocrList.put("other_party_first_name", "Zoe");
        ocrList.put("other_party_last_name", "Butler");
        ocrList.put("is_other_party_address_known",null);
        ocrList.put("other_party_address_line1","299 Harrow");
        ocrList.put("other_party_address_line2","The Avenue");
        ocrList.put("other_party_address_line3","Hatch End");
        ocrList.put("other_party_postcode","HA5 4QT");

    }

    private Map<String, Object> caseDataWithoutChildMaintenanceAndPartiallyMissingOtherPartyNameAddress() {

        Map<String, Object> ocrList = new HashMap<>();
        ocrList.put("person1_child_maintenance_number", "");
        ocrList.put("other_party_title", "Mrs");
        ocrList.put("other_party_first_name", "Zoe");
        ocrList.put("is_other_party_address_known","true");
        ocrList.put("other_party_address_line1","299 Harrow");
        ocrList.put("other_party_address_line3","Hatch End");
        addAppellant(ocrList);
        ocrList.put("mrn_date", MRN_DATE_YESTERDAY_DD_MM_YYYY);
        ocrList.put("contains_mrn", true);
        ocrList.put("is_hearing_type_oral", true);
        ocrList.put("is_hearing_type_paper", false);
        ocrList.put("hearing_options_exclude_dates", "01/12/2030");
        ocrList.put("hearing_type_telephone", "Yes");
        ocrList.put("hearing_telephone_number", "01234567890");
        ocrList.put("hearing_type_video", "Yes");
        ocrList.put("hearing_video_email", "my@email.com");
        ocrList.put("hearing_type_face_to_face", "No");
        ocrList.put("is_paying_parent", "true");

        return exceptionRecord(ocrList, null);
    }

    private Map<String, Object> caseDataWithoutAppellantRole() {

        Map<String, Object> ocrList = new HashMap<>();
        ocrList.put("person1_child_maintenance_number", "12334");
        addAppellant(ocrList);
        ocrList.put("mrn_date", MRN_DATE_YESTERDAY_DD_MM_YYYY);
        ocrList.put("contains_mrn", true);
        ocrList.put("is_hearing_type_oral", true);
        ocrList.put("is_hearing_type_paper", false);
        ocrList.put("hearing_options_exclude_dates", "01/12/2030");
        ocrList.put("hearing_type_telephone", "Yes");
        ocrList.put("hearing_telephone_number", "01234567890");
        ocrList.put("hearing_type_video", "Yes");
        ocrList.put("hearing_video_email", "my@email.com");
        ocrList.put("hearing_type_face_to_face", "No");

        return exceptionRecord(ocrList, null);
    }

    private Map<String, Object> caseDataWithInvalidAppellantRole() {

        Map<String, Object> ocrList = new HashMap<>();
        ocrList.put("person1_child_maintenance_number", "123");
        addAppellant(ocrList);
        ocrList.put("mrn_date", MRN_DATE_YESTERDAY_DD_MM_YYYY);
        ocrList.put("contains_mrn", true);
        ocrList.put("is_hearing_type_oral", true);
        ocrList.put("is_hearing_type_paper", false);
        ocrList.put("hearing_options_exclude_dates", "01/12/2030");
        ocrList.put("hearing_type_telephone", "Yes");
        ocrList.put("hearing_telephone_number", "01234567890");
        ocrList.put("hearing_type_video", "Yes");
        ocrList.put("hearing_video_email", "my@email.com");
        ocrList.put("hearing_type_face_to_face", "No");
        ocrList.put("is_paying_parent", "true");
        ocrList.put("is_receiving_parent", "true");
        ocrList.put("is_another_party", "true");

        return exceptionRecord(ocrList, null);
    }

    private void findCaseByForCaseworkerReturnCaseDetails() throws Exception {
        SearchSourceBuilder query = SscsQueryBuilder.findCcdCaseByNinoAndBenefitTypeAndMrnDateQuery("BB000000B", "ESA", "2017-01-01");

        ccdServer.stubFor(post(concat(uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.FIND_CASE_EVENT_URL)).atPriority(1)
                .withHeader(AUTHORIZATION, equalTo(USER_AUTH_TOKEN))
                .withHeader(SERVICE_AUTHORIZATION_HEADER_KEY, equalTo(SERVICE_AUTH_TOKEN))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withRequestBody(containing(query.toString()))
                .willReturn(aResponse()
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(loadJson("mappings/existing-case-details-200-response.json"))));
    }

    private void verifyResultData(ResponseEntity<SuccessfulTransformationResponse> result,
                                  String expectedDataFileLocation,
                                  Function<SuccessfulTransformationResponse, String> getTya) throws Exception {
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

        SuccessfulTransformationResponse callbackResponse = result.getBody();

        String expected = loadJson(expectedDataFileLocation);
        String tya = getTya.apply(callbackResponse);

        expected = expected.replace("TYA_RANDOM_NUMBER", tya);
        expected = expected.replace("MRN_DATE", MRN_DATE_YESTERDAY_YYYY_MM_DD);

        ObjectMapper obj = new ObjectMapper();
        String jsonStr = obj.writeValueAsString(callbackResponse);

        JSONAssert.assertEquals(expected, jsonStr,
            new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("case_creation_details.case_data.appeal.appellant.id", (o1, o2) -> true),
                new Customization("case_creation_details.case_data.appeal.appellant.appointee.id", (o1, o2) -> true),
                new Customization("case_creation_details.case_data.appeal.rep.id", (o1, o2) -> true)
            ));

        verify(serviceAuthorisationApi).getServiceName(SERVICE_AUTH_TOKEN);
    }

    private String getAppellantTya(SuccessfulTransformationResponse callbackResponse) {
        return ((HashMap) ((HashMap) callbackResponse.getCaseCreationDetails().getCaseData().get("subscriptions"))
            .get("appellantSubscription")).get("tya").toString();
    }

    private String getAppointeeTya(SuccessfulTransformationResponse callbackResponse) {
        return ((HashMap) ((HashMap) callbackResponse.getCaseCreationDetails().getCaseData().get("subscriptions"))
            .get("appointeeSubscription")).get("tya").toString();
    }

    private HttpHeaders httpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, USER_AUTH_TOKEN);
        headers.set(SERVICE_AUTHORIZATION_HEADER_KEY, SERVICE_AUTH_TOKEN);
        headers.set(USER_ID_HEADER, USER_ID);
        return headers;
    }

    private String getParamsMatchCaseUrl() {
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("case.appeal.appellant.identity.nino", "BB000000B");

        return searchCriteria.entrySet().stream()
            .map(p -> p.getKey() + "=" + p.getValue())
            .reduce((p1, p2) -> p1 + "&" + p2)
            .map(s -> "?" + s)
            .orElse("");
    }
}
