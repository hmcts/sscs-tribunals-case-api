package uk.gov.hmcts.reform.sscs.functional.evidence;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaEvidence;


@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class EvidenceDocumentUploadTest {

    private static final String APPLICATION_PDF = "application/pdf";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json;charset=UTF-8";

    @Value("${test-url}")
    private String testUrl;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
    }

    @Test
    public void shouldStoreTheEvidenceDocumentInDocumentStore() {
        uploadAndVerifyEvidenceDocumentUpload();
    }


    @Test
    public void shouldCreateAppealCaseWithEvidenceDocumentLinkIntoCcd() throws IOException {
        Response response = uploadAndVerifyEvidenceDocumentUpload();

        String documentUrl = response.path("documents[0]._links.self.href");
        String fileName = response.path("documents[0].originalDocumentName");
        LocalDate localDate = LocalDate.now();

        String syaJsonWithEvidence = getSyaInputJsonWithEvidenceDocumentDetails(documentUrl, fileName, localDate);

        given()
            .contentType(ContentType.JSON)
            .body(syaJsonWithEvidence)
            .expect()
            .statusCode(201)
            .when()
            .post("/appeals");

    }

    private Response uploadAndVerifyEvidenceDocumentUpload() {
        URL resource = getClass().getClassLoader().getResource("evidence/evidence-document.pdf");

        return given()
            .multiPart("file", new File(Objects.requireNonNull(resource).getPath()), APPLICATION_PDF)
            .accept(APPLICATION_JSON_CHARSET_UTF_8)
            .contentType(MULTIPART_FORM_DATA)
            .expect()
            .statusCode(200)
            .response().contentType(APPLICATION_JSON_CHARSET_UTF_8)
            .when()
            .post("/evidence/upload");
    }

    private String getSyaInputJsonWithEvidenceDocumentDetails(String documentUrl, String fileName, LocalDate localDate) throws IOException {
        SyaCaseWrapper syaCaseWrapper = getDeserializeMessage();

        SyaEvidence syaEvidence = SyaEvidence.builder().url(documentUrl).fileName(fileName).uploadedDate(localDate).build();

        ArrayList<SyaEvidence> syaEvidences = new ArrayList<>();
        syaEvidences.add(syaEvidence);

        syaCaseWrapper.getReasonsForAppealing().setEvidences(syaEvidences);
        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(syaCaseWrapper);

    }


    private SyaCaseWrapper getDeserializeMessage() throws IOException {
        String syaJson;
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("evidence/appealCaseSyaDocument.json");
        syaJson = IOUtils.toString(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        return mapper.readValue(syaJson, SyaCaseWrapper.class);

    }
}
