package uk.gov.hmcts.reform.sscs.functional.handlers.timeextension;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class DwpRequestTimeExtensionAboutToSubmitHandlerTest extends BaseHandler {

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFieldsAndTl1Form() throws Exception {
        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest())
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .rootPath("data")
            .assertThat().body("interlocReviewState", equalTo("reviewByTcw"))
            .assertThat().body("dwpState", equalTo("extensionRequested"))
            .assertThat().body("interlocReferralReason", equalTo("timeExtension"))
            .assertThat().body("sscsDocument.value", hasItem(hasEntry("documentType", "tl1Form")));
    }

    private String getJsonCallbackForTest() throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource("handlers/timeextension/dwpRequestTimeExtensionAboutToSubmitCallback.json")).getFile();
        return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
    }
}
