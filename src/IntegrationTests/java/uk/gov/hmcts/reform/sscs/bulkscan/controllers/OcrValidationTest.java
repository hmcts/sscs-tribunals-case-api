package uk.gov.hmcts.reform.sscs.bulkscan.controllers;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.SERVICE_AUTH_TOKEN;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.RefDataService;

@AutoConfigureMockMvc
@ContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@TestPropertySource(locations = "classpath:config/application_it.properties")
public class OcrValidationTest  {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    protected ServiceAuthorisationApi serviceAuthorisationApi;

    @MockitoBean
    protected IdamService idamService;

    @MockitoBean
    protected CcdService ccdService;

    @MockitoBean
    private RefDataService refDataService;

    @Test
    public void should_return_200_when_ocr_form_validation_request_data_is_valid() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/valid-ocr-data.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.warnings", hasSize(0)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_when_ocr_form_validation_request_data_is_valid_Sscs2() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/valid-ocr-data.json");

        mvc.perform(
                post("/forms/SSCS1/validate-ocr")
                    .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.warnings", hasSize(0)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_when_ocr_form_for_uc_validation_request_data_is_valid() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/valid-ocr-data-for-uc.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.warnings", hasSize(0)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_when_ocr_form_with_address_line3_blank_validation_request_data_is_valid() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/valid-ocr-data-address-line3.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.warnings", hasSize(0)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_when_ocr_form_with_hearing_sub_type_validation_request_data_is_valid() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/valid-ocr-data-with-hearing-sub-type.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.warnings", hasSize(0)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_with_status_warnings_when_ocr_form_validation_request_data_is_incomplete() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/incomplete-ocr-data.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WARNINGS"))
            .andExpect(jsonPath("$.warnings", hasSize(7)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_when_ocr_form_with_form_type_sscs1u_and_hearing_sub_type_validation_request_data_are_empty() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/invalid-ocr-data-with-hearing-sub-type-sscs1u.json");

        mvc.perform(
            post("/forms/SSCS1U/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WARNINGS"))
            .andExpect(jsonPath("$.warnings", hasSize(1)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_when_ocr_form_with_form_type_sscs1peu_and_hearing_sub_type_validation_request_data_are_empty() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/invalid-ocr-data-with-hearing-sub-type-sscs1peu.json");

        mvc.perform(
            post("/forms/SSCS1PEU/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WARNINGS"))
            .andExpect(jsonPath("$.warnings", hasSize(1)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_when_ocr_form_with_form_type_sscs1_and_hearing_sub_type_validation_request_data_are_empty() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/invalid-ocr-data-with-hearing-sub-type.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.warnings", hasSize(0)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_with_status_errors_when_ocr_form_validation_request_fails_schema_validation() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/invalid-ocr-data-fails-schema.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERRORS"))
            .andExpect(jsonPath("$.warnings", hasSize(0)))
            .andExpect(jsonPath("$.errors", hasSize(1)));
    }

    @Test
    //Convert errors with transforming data to warnings during validation endpoint
    public void should_return_200_with_status_warnings_when_ocr_form_validation_request_has_errors_with_transformation() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/invalid-ocr-data-transformation.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WARNINGS"))
            .andExpect(jsonPath("$.warnings", hasSize(1)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    //Convert errors with the validating data to warnings during validation endpoint
    public void should_return_200_with_status_warnings_when_ocr_form_validation_request_has_errors_with_validation() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/invalid-ocr-data-validation.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WARNINGS"))
            .andExpect(jsonPath("$.warnings", hasSize(1)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_with_status_warnings_when_ocr_form_validation_request_has_duplicate_case_warnings() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        SscsCaseDetails caseDetails = SscsCaseDetails.builder().id(1L).build();

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(caseDetails);
        String content = readResource("mappings/ocr-validation/valid-ocr-data.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WARNINGS"))
            .andExpect(jsonPath("$.warnings", hasSize(1)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_401_when_service_auth_header_is_missing() throws Throwable {
        String content = readResource("mappings/ocr-validation/valid-ocr-data.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isUnauthorized())
            .andExpect(content().json("{\"error\":\"Missing ServiceAuthorization header\"}"));

    }

    @Test
    public void fuzzyMatchingMaternityAllowanceBenefitTypeForSscs1uForm() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/valid-ocr-data.json");
        content = content.replaceAll("benefit_type_description", "benefit_type_other");
        content = content.replaceAll("PIP", "Maternity something");

        mvc.perform(
            post("/forms/SSCS1U/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.warnings", hasSize(0)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void fuzzyMatchingInvalidNameBenefitTypeForSscs1uForm() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/valid-ocr-data.json");
        content = content.replaceAll("benefit_type_description", "benefit_type_other");
        content = content.replaceAll("PIP", "invalid name");

        mvc.perform(
            post("/forms/SSCS1U/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WARNINGS"))
            .andExpect(jsonPath("$.warnings", hasSize(1)))
            .andExpect(jsonPath("$.errors", hasSize(0)))
            .andExpect(content().json("{\"warnings\":[\"benefit_type_other is invalid\"],\"errors\":[],\"status\":\"WARNINGS\"}"));
    }

    @Test
    public void should_return_200_with_error_when_ocr_form_with_sscs2_data_is_used_for_sscs1_form() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/sscs2-valid-ocr-data.json");

        mvc.perform(
            post("/forms/SSCS1/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ERRORS"))
            .andExpect(jsonPath("$.warnings", hasSize(0)))
            .andExpect(jsonPath("$.errors", hasSize(9)))
            .andExpect(jsonPath("$.errors", containsInAnyOrder(
                "$: property 'person1_child_maintenance_number' is not defined in the schema and the schema does not allow additional properties",
                "$: property 'other_party_last_name' is not defined in the schema and the schema does not allow additional properties",
                "$: property 'other_party_first_name' is not defined in the schema and the schema does not allow additional properties",
                "$: property 'other_party_title' is not defined in the schema and the schema does not allow additional properties",
                "$: property 'is_paying_parent' is not defined in the schema and the schema does not allow additional properties",
                "$: property 'other_party_address_line1' is not defined in the schema and the schema does not allow additional properties",
                "$: property 'other_party_address_line2' is not defined in the schema and the schema does not allow additional properties",
                "$: property 'other_party_postcode' is not defined in the schema and the schema does not allow additional properties",
                "$: property 'is_other_party_address_known' is not defined in the schema and the schema does not allow additional properties")));
    }

    @Test
    public void should_return_200_when_ocr_form_with_sscs2_form_validation_request_data_is_valid() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/sscs2-valid-ocr-data.json");

        mvc.perform(
            post("/forms/SSCS2/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.warnings", hasSize(0)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_when_ocr_form_with_sscs5_form_validation_request_data_is_valid() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/sscs5-valid-ocr-data.json");

        mvc.perform(
            post("/forms/SSCS5/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.warnings", hasSize(0)))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    public void should_return_200_with_warning_when_ocr_form_with_sscs2_form_validation_request_data_is_invalid() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/sscs2-invalid-ocr-data.json");

        mvc.perform(
            post("/forms/SSCS2/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WARNINGS"))
            .andExpect(jsonPath("$.errors", hasSize(0)))
            .andExpect(jsonPath("$.warnings", hasSize(5)))
            .andExpect(jsonPath("$.warnings", containsInAnyOrder("person1_child_maintenance_number is empty",
                "is_paying_parent, is_receiving_parent, is_another_party and other_party_details fields are empty",
                "other_party_first_name is empty",
                "other_party_address_line1 is empty",
                "other_party_postcode is empty")));
    }

    @Test
    public void should_return_200_with_warning_when_ocr_form_with_sscs2_form_validation_request_appellant_role_invalid() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/sscs2-invalid-ocr-data-appellant-role.json");

        mvc.perform(
            post("/forms/SSCS2/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WARNINGS"))
            .andExpect(jsonPath("$.errors", hasSize(0)))
            .andExpect(jsonPath("$.warnings", hasSize(1)))
            .andExpect(jsonPath("$.warnings", contains("is_paying_parent, is_receiving_parent, is_another_party and other_party_details have conflicting values")));
    }

    @Test
    public void should_return_200_with_error_when_ocr_form_with_sscs5_form_validation_request_benefit_type_invalid() throws Throwable {
        when(serviceAuthorisationApi.getServiceName(SERVICE_AUTH_TOKEN)).thenReturn("test_service");

        String content = readResource("mappings/ocr-validation/sscs5-invalid-ocr-data-benefit-type.json");

        mvc.perform(
            post("/forms/SSCS5/validate-ocr")
                .header("ServiceAuthorization", SERVICE_AUTH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WARNINGS"))
            .andExpect(jsonPath("$.errors", hasSize(0)))
            .andExpect(jsonPath("$.warnings", hasSize(1)))
            .andExpect(jsonPath("$.warnings", containsInAnyOrder(
                "is_benefit_type_guardians_allowance and is_benefit_type_tax_free_childcare have contradicting values")));
    }


    private String readResource(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), StandardCharsets.UTF_8);
    }
}
