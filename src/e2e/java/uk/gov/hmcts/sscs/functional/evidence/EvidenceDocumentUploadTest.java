package uk.gov.hmcts.sscs.functional.evidence;

import io.restassured.RestAssured;
import java.io.File;
import java.net.URL;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;


public class EvidenceDocumentUploadTest {

    public static final String APPLICATION_PDF = "application/pdf";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json;charset=UTF-8";

    private final String tcaInstance = System.getenv("TEST_URL");
    private final String localInstance = "http://localhost:8080";

    @Test
    public void shouldStoreTheEvidenceDocumentInDocumentStore() {
        RestAssured.baseURI = StringUtils.isNotBlank(tcaInstance) ? tcaInstance : localInstance;
        RestAssured.useRelaxedHTTPSValidation();

        URL resource = getClass().getClassLoader().getResource("evidence/evidence-document.pdf");

        RestAssured
                .given()
                    .multiPart("file", new File(resource.getPath()), APPLICATION_PDF)
                    .accept(APPLICATION_JSON_CHARSET_UTF_8)
                    .contentType(MULTIPART_FORM_DATA)
                .expect()
                    .statusCode(200)
                    .response().contentType(APPLICATION_JSON_CHARSET_UTF_8)
                .when()
                    .post("/evidence/upload");

    }
}
