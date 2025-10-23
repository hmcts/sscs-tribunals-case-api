package uk.gov.hmcts.reform.sscs.bulkscan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application_e2e.yaml")
@RunWith(JUnitParamsRunner.class)
@Slf4j
public class TransformationFunctionalTest extends BaseFunctionalTest {
    public static final String MRN_DATE_YESTERDAY_YYYY_MM_DD = LocalDate.now().minusDays(1).toString();
    public static final String MRN_DATE_YESTERDAY_DD_MM_YYYY = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    @Test
    public void transform_appeal_created_case_when_all_fields_entered() throws IOException {
        String expectedJson = getJson("exception/output/expected_all_fields_entered.json");

        String person1Nino = generateRandomNino();
        String person2Nino = generateRandomNino();

        expectedJson = replaceNino(expectedJson, person1Nino, person2Nino);
        expectedJson = replaceMrnDate(expectedJson, MRN_DATE_YESTERDAY_YYYY_MM_DD);

        String jsonRequest = getJson("exception/all_fields_entered.json");
        jsonRequest = replaceNino(jsonRequest, person1Nino, person2Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }

    @Test
    public void transform_appeal_created_case_when_all_fields_entered_uc() throws IOException {
        String expectedJson = getJson("exception/output/expected_all_fields_entered_uc.json");

        String person1Nino = generateRandomNino();
        String person2Nino = generateRandomNino();

        expectedJson = replaceNino(expectedJson, person1Nino, person2Nino);
        expectedJson = replaceMrnDate(expectedJson, MRN_DATE_YESTERDAY_YYYY_MM_DD);

        String jsonRequest = getJson("exception/all_fields_entered_uc.json");
        jsonRequest = replaceNino(jsonRequest, person1Nino, person2Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }

    @Test
    public void transform_incomplete_case_when_missing_mandatory_fields() throws IOException {
        String expectedJson = getJson("exception/output/expected_some_mandatory_fields_missing.json");

        String jsonRequest = getJson("exception/some_mandatory_fields_missing.json");

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }

    @Test
    @Parameters({"see scanned SSCS1 form,mrn_date_greater_than_13_months.json", ",mrn_date_greater_than_13_months_grounds_missing.json"})
    public void transform_interlocutory_review_case_when_mrn_date_greater_than_13_months(String appealGrounds,
                                                                                         String path) throws IOException {
        String expectedJson = getJson("exception/output/expected_" + path);

        String person1Nino = generateRandomNino();
        String person2Nino = generateRandomNino();
        expectedJson = replaceNino(expectedJson, person1Nino, person2Nino);

        String jsonRequest = getJson("exception/mrn_date_greater_than_13_months.json");
        jsonRequest = jsonRequest.replace("APPEAL_GROUNDS", appealGrounds);
        jsonRequest = replaceNino(jsonRequest, person1Nino, person2Nino);

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }

    @Test
    public void should_not_transform_exception_record_when_schema_validation_fails_and_respond_with_422() throws IOException {
        String json = getJson("exception/invalid_name_key.json");

        Response response = transformExceptionRequest(json, UNPROCESSABLE_ENTITY.value());

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY.value());

        JsonPath errorResponse = response.getBody().jsonPath();

        assertThat(errorResponse.getList("errors"))
            .hasSize(1)
            .containsOnly("$: property 'first_name' is not defined in the schema and the schema does not allow additional properties");
        assertThat(errorResponse.getList("warnings")).isEmpty();
        assertThat(errorResponse.getMap("")).containsOnlyKeys("errors", "warnings");
    }

    @Test
    public void should_not_transform_exception_record_when_validation_error_and_respond_with_422() throws IOException {
        String json = getJson("exception/invalid_mobile_number.json");

        Response response = transformExceptionRequest(json, UNPROCESSABLE_ENTITY.value());

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY.value());

        JsonPath errorResponse = response.getBody().jsonPath();

        assertThat(errorResponse.getList("errors"))
            .hasSize(1)
            .containsOnly("person1_mobile is invalid");
        assertThat(errorResponse.getList("warnings")).isEmpty();
        assertThat(errorResponse.getMap("")).containsOnlyKeys("errors", "warnings");
    }

    @Test
    public void transform_appeal_created_case_when_all_fields_entered_attendance_allowance() throws IOException {
        String expectedJson = getJson("exception/output/expected_all_fields_entered_aa.json");

        String person1Nino = generateRandomNino();
        String person2Nino = generateRandomNino();

        expectedJson = replaceNino(expectedJson, person1Nino, person2Nino);
        expectedJson = replaceMrnDate(expectedJson, MRN_DATE_YESTERDAY_YYYY_MM_DD);

        String jsonRequest = getJson("exception/all_fields_entered_aa.json");
        jsonRequest = replaceNino(jsonRequest, person1Nino, person2Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }

    @Test
    public void transform_appeal_created_case_when_all_fields_entered_with_pip_office() throws IOException {
        String expectedJson = getJson("exception/output/expected_all_fields_pip_office_entered_aa.json");

        String person1Nino = generateRandomNino();
        String person2Nino = generateRandomNino();

        expectedJson = replaceNino(expectedJson, person1Nino, person2Nino);
        expectedJson = replaceMrnDate(expectedJson, MRN_DATE_YESTERDAY_YYYY_MM_DD);

        String jsonRequest = getJson("exception/all_fields_pip_office_entered_aa.json");
        jsonRequest = replaceNino(jsonRequest, person1Nino, person2Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }

    @Test
    public void transform_appeal_created_case_when_missing_office_maternity_allowance() throws IOException {
        String expectedJson = getJson("exception/output/expected_all_fields_auto_office_ma.json");

        String person1Nino = generateRandomNino();
        String person2Nino = generateRandomNino();

        expectedJson = replaceNino(expectedJson, person1Nino, person2Nino);
        expectedJson = replaceMrnDate(expectedJson, MRN_DATE_YESTERDAY_YYYY_MM_DD);

        String jsonRequest = getJson("exception/all_fields_auto_office_ma.json");
        jsonRequest = replaceNino(jsonRequest, person1Nino, person2Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }

    @Test
    public void sscs2_transform_appeal_created_case_when_all_fields_entered() throws IOException {
        String expectedJson = getJson("exception/output/sscs2_expected_all_fields_entered.json");
        String person1Nino = generateRandomNino();

        expectedJson = expectedJson.replace("{PERSON1_NINO}", person1Nino);
        expectedJson = replaceMrnDate(expectedJson, MRN_DATE_YESTERDAY_YYYY_MM_DD);

        String jsonRequest = getJson("exception/sscs2_all_fields_entered.json");
        jsonRequest = jsonRequest.replace("{PERSON1_NINO}", person1Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }

    @Test
    public void sscs5_transform_appeal_created_case_when_all_fields_entered() throws IOException {
        String expectedJson = getJson("exception/output/sscs5_expected_all_fields_entered.json");
        String person1Nino = generateRandomNino();

        expectedJson = expectedJson.replace("{PERSON1_NINO}", person1Nino);
        expectedJson = replaceMrnDate(expectedJson, MRN_DATE_YESTERDAY_YYYY_MM_DD);

        String jsonRequest = getJson("exception/sscs5_all_fields_entered.json");
        jsonRequest = jsonRequest.replace("{PERSON1_NINO}", person1Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }

    @Test
    public void transform_appeal_created_case_when_all_fields_entered_no_form_type() throws IOException {
        String person1Nino = generateRandomNino();
        String person2Nino = generateRandomNino();

        String jsonRequest = getJson("exception/all_fields_entered_no_form_type.json");
        jsonRequest = replaceNino(jsonRequest, person1Nino, person2Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        Response response = transformExceptionRequest(jsonRequest, UNPROCESSABLE_ENTITY.value());

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY.value());

        JsonPath errorResponse = response.getBody().jsonPath();

        assertThat(errorResponse.getList("errors"))
            .hasSize(1)
            .containsOnly("No valid form type was found. There needs to be a valid form_type on the OCR data or on the exception record.");
        assertThat(errorResponse.getList("warnings")).isEmpty();
        assertThat(errorResponse.getMap("")).containsOnlyKeys("errors", "warnings");
    }

    @Test
    public void transform_appeal_created_case_when_all_fields_entered_form_type_other() throws IOException {
        String person1Nino = generateRandomNino();
        String person2Nino = generateRandomNino();

        String jsonRequest = getJson("exception/all_fields_entered_form_type_other.json");
        jsonRequest = replaceNino(jsonRequest, person1Nino, person2Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        Response response = transformExceptionRequest(jsonRequest, UNPROCESSABLE_ENTITY.value());

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY.value());

        JsonPath errorResponse = response.getBody().jsonPath();

        assertThat(errorResponse.getList("errors"))
            .hasSize(1)
            .containsOnly("No valid form type was found. There needs to be a valid form_type on the OCR data or on the exception record.");
        assertThat(errorResponse.getList("warnings")).isEmpty();
        assertThat(errorResponse.getMap("")).containsOnlyKeys("errors", "warnings");
    }

    @Test
    public void transform_appeal_created_case_when_all_fields_entered_invalid_form_type() throws IOException {
        String person1Nino = generateRandomNino();
        String person2Nino = generateRandomNino();

        String jsonRequest = getJson("exception/all_fields_entered_invalid_form_type.json");
        jsonRequest = replaceNino(jsonRequest, person1Nino, person2Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        Response response = transformExceptionRequest(jsonRequest, UNPROCESSABLE_ENTITY.value());

        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY.value());

        JsonPath errorResponse = response.getBody().jsonPath();

        assertThat(errorResponse.getList("errors"))
            .hasSize(1)
            .containsOnly("No valid form type was found. There needs to be a valid form_type on the OCR data or on the exception record.");
        assertThat(errorResponse.getList("warnings")).isEmpty();
        assertThat(errorResponse.getMap("")).containsOnlyKeys("errors", "warnings");
    }

    @Test
    public void sscs1_transform_appeal_created_case_when_all_fields_entered_ocr_form_type() throws IOException {
        String person1Nino = generateRandomNino();
        String person2Nino = generateRandomNino();

        String expectedJson = getJson("exception/output/expected_all_fields_entered.json");

        expectedJson = replaceNino(expectedJson, person1Nino, person2Nino);
        expectedJson = replaceMrnDate(expectedJson, MRN_DATE_YESTERDAY_YYYY_MM_DD);

        String jsonRequest = getJson("exception/all_fields_entered_ocr_form_type.json");
        jsonRequest = replaceNino(jsonRequest, person1Nino, person2Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }

    @Test
    public void sscs2_transform_appeal_created_case_when_all_fields_entered_ocr_form_type() throws IOException {
        String expectedJson = getJson("exception/output/sscs2_expected_all_fields_entered.json");
        String person1Nino = generateRandomNino();

        expectedJson = expectedJson.replace("{PERSON1_NINO}", person1Nino);
        expectedJson = replaceMrnDate(expectedJson, MRN_DATE_YESTERDAY_YYYY_MM_DD);

        String jsonRequest = getJson("exception/sscs2_all_fields_entered.json");
        jsonRequest = jsonRequest.replace("{PERSON1_NINO}", person1Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }

    @Test
    public void sscs5_transform_appeal_created_case_when_all_fields_entered_ocr_form_type() throws IOException {
        String expectedJson = getJson("exception/output/sscs5_expected_all_fields_entered.json");
        String person1Nino = generateRandomNino();

        expectedJson = expectedJson.replace("{PERSON1_NINO}", person1Nino);
        expectedJson = replaceMrnDate(expectedJson, MRN_DATE_YESTERDAY_YYYY_MM_DD);

        String jsonRequest = getJson("exception/sscs5_all_fields_entered.json");
        jsonRequest = jsonRequest.replace("{PERSON1_NINO}", person1Nino);
        jsonRequest = replaceMrnDate(jsonRequest, MRN_DATE_YESTERDAY_DD_MM_YYYY);

        verifyResponseIsExpected(expectedJson, transformExceptionRequest(jsonRequest, OK.value()));
    }
}
