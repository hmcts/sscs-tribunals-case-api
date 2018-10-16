package uk.gov.hmcts.reform.sscs.functional.evidence;

import static io.restassured.RestAssured.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.domain.wrapper.SyaEvidence;


public class EvidenceDocumentUploadTest {

    public static final String APPLICATION_PDF = "application/pdf";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json;charset=UTF-8";
    public static final String UTF_8 = "UTF-8";

    private final String tcaInstance = System.getenv("TEST_URL");
    private final String localInstance = "http://localhost:8080";

    @Test
    public void shouldStoreTheEvidenceDocumentInDocumentStore() {
        baseURI = StringUtils.isNotBlank(tcaInstance) ? tcaInstance : localInstance;
        useRelaxedHTTPSValidation();
        uploadAndVerifyEvidenceDocumentUpload();
    }


    @Test
    public void shouldCreateAppealCaseWithEvidenceDocumentLinkIntoCcd() throws IOException {
        baseURI = StringUtils.isNotBlank(tcaInstance) ? tcaInstance : localInstance;
        useRelaxedHTTPSValidation();

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
                .multiPart("file", new File(resource.getPath()), APPLICATION_PDF)
                .accept(APPLICATION_JSON_CHARSET_UTF_8)
                .contentType(MULTIPART_FORM_DATA)
                .expect()
                .statusCode(200)
                .response().contentType(APPLICATION_JSON_CHARSET_UTF_8)
                .when()
                .post("/evidence/upload");
    }

    private String getSyaInputJsonWithEvidenceDocumentDetails(String documentUrl, String fileName, LocalDate localDate) throws IOException {
        SyaCaseWrapper syaCaseWrapper = getDeserializeMessage("evidence/appealCaseSyaDocument.json");

        SyaEvidence syaEvidence = SyaEvidence.builder().url(documentUrl).fileName(fileName).uploadedDate(localDate).build();

        ArrayList<SyaEvidence> syaEvidences = new ArrayList<>();
        syaEvidences.add(syaEvidence);

        syaCaseWrapper.getReasonsForAppealing().setEvidences(syaEvidences);
        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(syaCaseWrapper);

    }


    private SyaCaseWrapper getDeserializeMessage(String fileName) throws IOException {
        String syaJson = null;
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        syaJson = IOUtils.toString(inputStream, UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        return mapper.readValue(syaJson, SyaCaseWrapper.class);

    }
}
