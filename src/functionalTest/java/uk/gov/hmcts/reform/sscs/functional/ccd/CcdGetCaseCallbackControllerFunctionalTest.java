package uk.gov.hmcts.reform.sscs.functional.ccd;

import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class CcdGetCaseCallbackControllerFunctionalTest {

    private static final String TEST_URL = System.getenv("TEST_URL");
    private static final String LOCAL_URL = "http://localhost:8008";
    private static final String GET_CASE_PATH = "/ccdGetCase";

    private final IdamService idamService;
    private final ObjectMapper objectMapper;
    private IdamTokens idamTokens;

    public CcdGetCaseCallbackControllerFunctionalTest(@Autowired IdamService idamService, @Autowired ObjectMapper objectMapper) {
        this.idamService = idamService;
        this.objectMapper = objectMapper;
    }

    @BeforeAll
    static void setUpBeforeAll() {
        RestAssured.baseURI = StringUtils.isNotBlank(TEST_URL) ? TEST_URL : LOCAL_URL;
    }

    @BeforeEach
    void setUp() {
        useRelaxedHTTPSValidation();
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    void shouldReturn200ForValidRequest() {
        RestAssured.given().contentType(ContentType.JSON)
            .header(SERVICE_AUTHORISATION_HEADER, idamTokens.getServiceAuthorization())
            .body(buildCallbackJson(PIP.getShortName())).when().post(GET_CASE_PATH).then().statusCode(HttpStatus.OK.value());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "UC_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
    void shouldReturn200WithMetadataFieldInjectedWhenCaseIsUcInWithDwpState() {
        RestAssured.given().contentType(ContentType.JSON)
            .header(SERVICE_AUTHORISATION_HEADER, idamTokens.getServiceAuthorization()).body(buildCallbackJson(UC.getShortName()))
            .when().post(GET_CASE_PATH).then().statusCode(HttpStatus.OK.value())
            .body(org.hamcrest.Matchers.containsString("[INJECTED_DATA.ENABLE_ADD_OTHER_PARTY_DATA]"))
            .body(org.hamcrest.Matchers.containsString("\"value\":\"Yes\""));
    }

    @Test
    void shouldReturn400WhenServiceAuthHeaderIsMissing() {
        RestAssured.given().contentType(ContentType.JSON).body(buildCallbackJson(PIP.getShortName())).when().post(GET_CASE_PATH).then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldReturn400WhenBodyIsMissing() {
        RestAssured.given().contentType(ContentType.JSON)
            .header(SERVICE_AUTHORISATION_HEADER, idamTokens.getServiceAuthorization()).when().post(GET_CASE_PATH).then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @SneakyThrows
    private String buildCallbackJson(final String benefitCode) {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitCode).build()).build()).build();
        final CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseData,
            LocalDateTime.now(), "Benefit");
        final Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.ACTION_FURTHER_EVIDENCE,
            false);
        return objectMapper.writeValueAsString(callback);
    }
}