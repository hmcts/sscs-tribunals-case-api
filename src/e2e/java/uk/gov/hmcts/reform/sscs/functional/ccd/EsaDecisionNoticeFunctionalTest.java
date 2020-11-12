package uk.gov.hmcts.reform.sscs.functional.ccd;

import static io.restassured.RestAssured.baseURI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler.getJsonCallbackForTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.functional.mya.BaseFunctionTest;

@RunWith(JUnitParamsRunner.class)
public class EsaDecisionNoticeFunctionalTest extends BaseFunctionTest {

    private static final String DM_URL_LOCAL = "http://dm-store:5005";
    private static final String DM_URL_AAT = "http://dm-store-aat.service.core-compute-aat.internal";
    private static final String DM_URL = System.getenv("TEST_URL") != null && System.getenv("TEST_URL").contains("aat") ? DM_URL_AAT : DM_URL_LOCAL;

    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();

    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Autowired
    protected ObjectMapper objectMapper;

    public EsaDecisionNoticeFunctionalTest() {
        baseURI = baseUrl;
    }

    @Test
    @Parameters({
            "noRecommendation, The Tribunal makes no recommendation as to when the Department should reassess Joe Bloggs.",
            "doNotReassess, In view of the degree of disability found by the Tribunal\\, and unless the regulations change\\, the Tribunal would recommend that the appellant is not re-assessed.",
            "reassess12, The Tribunal recommends that the Department reassesses Joe Bloggs within 12 months from today's date.",
            "reassess3, The Tribunal recommends that the Department reassesses Joe Bloggs within 3 months from today's date.",
            "doNotReassess3, The Tribunal recommends that the Department does not reassess Joe Bloggs within 3 months from today's date.",
            "doNotReassess18, The Tribunal recommends that the Department does not reassess Joe Bloggs within 18 months from today's date.",
    })
    public void dwpReassessTheAwardPreview_shouldGeneratePdfWithExpectedText(String code, String expectedText) throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/esaDwpReassessTheAwardCallback.json");
        json = json.replaceAll("DWP_REASSESS_THE_AWARD", code);
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(json), "PreviewFinalDecision");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(ccdEventResponse.getData().getWriteFinalDecisionPreviewDocument(), is(not(nullValue())));
        byte[] bytes = sscsMyaBackendRequests.toBytes(getDocumentUrl(ccdEventResponse));
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = pdfText.replaceAll("[\\n\\t]", "");
            assertThat(pdfTextWithoutNewLines, containsString("7. Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State."));
            assertThat(pdfTextWithoutNewLines, containsString(expectedText));
        }
    }

    private String getDocumentUrl(CcdEventResponse ccdEventResponse) {
        return ccdEventResponse.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl().replaceFirst(DM_URL_LOCAL, DM_URL);
    }

    private CcdEventResponse getCcdEventResponse(HttpResponse httpResponse) throws IOException {
        String response = EntityUtils.toString(httpResponse.getEntity());
        return objectMapper.readValue(response, CcdEventResponse.class);
    }
}
