package uk.gov.hmcts.reform.sscs.functional.evidence;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.artsok.RepeatedIfExceptionsTest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaEvidence;
import uk.gov.hmcts.reform.sscs.functional.sya.SubmitHelper;


@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class EvidenceDocumentUploadTest {

    @Value("${test-url}")
    private String testUrl;

    @Autowired
    private SubmitHelper submitHelper;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
    }

    @Test
    public void shouldStoreTheEvidenceDocumentInDocumentStore() {
        uploadAndVerifyEvidenceDocumentUpload();
    }

    @RepeatedIfExceptionsTest(repeats = 3, suspend = 5000L)
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
            .multiPart("file", new File(Objects.requireNonNull(resource).getPath()), MediaType.APPLICATION_PDF_VALUE)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .expect()
            .statusCode(200)
            .response().contentType(MediaType.APPLICATION_JSON_VALUE)
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
        // set random nino and mrnDate
        String nino = submitHelper.getRandomNino();
        syaJson = submitHelper.setNino(syaJson, nino);

        LocalDate mrnDate = LocalDate.now().minusMonths(12);
        syaJson = submitHelper.setLatestMrnDate(syaJson, mrnDate);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        return mapper.readValue(syaJson, SyaCaseWrapper.class);

    }
}
