package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler.getJsonCallbackForTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import junitparams.JUnitParamsRunner;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.functional.mya.BaseFunctionTest;

@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
public class PipDecisionNoticeFunctionalTest extends BaseFunctionTest {

    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();

    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Autowired
    protected ObjectMapper objectMapper;

    // The Scenarios are defined https://tools.hmcts.net/confluence/display/SSCS/ESA+DN+template+content+-+judges+input
    @Test
    public void scenario1_shouldGeneratePdfWithExpectedText() throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/pipScenario1Callback.json");
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the standard rate from 17/12/2020 for an indefinite period."));
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs has limited ability to carry out the activities of daily living set out below. They score 8 points. They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food e. Needs supervision or assistance to either prepare or cook a simple meal. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is entitled to the mobility component at the standard rate from 17/12/2020 for an indefinite period."));
            assertThat(pdfTextWithoutNewLines, containsString("6. Joe Bloggs is limited in their ability to mobilise. They score 0 points."));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("9. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("10. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("11.")));
        }
    }
    
    private String replaceNewLines(String pdfText) {
        return pdfText.replaceAll("-\n", "-").replaceAll("[\\n\\t]", " ").replaceAll("\\s{2,}", " ");
    }

    private byte[] callPreviewFinalDecision(String json) throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(json), "PreviewFinalDecision");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(ccdEventResponse.getData().getWriteFinalDecisionPreviewDocument(), is(not(nullValue())));
        return sscsMyaBackendRequests.toBytes(ccdEventResponse.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
    }

    private CcdEventResponse getCcdEventResponse(HttpResponse httpResponse) throws IOException {
        String response = EntityUtils.toString(httpResponse.getEntity());
        return objectMapper.readValue(response, CcdEventResponse.class);
    }
}
