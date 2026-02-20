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
import org.apache.pdfbox.Loader;
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
@TestPropertySource(locations = "classpath:config/application_functional.properties")
public class WriteFinalDecisionNoticeFunctionalTest extends BaseFunctionTest {

    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();

    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    public void shouldGenerateExpectedDecisionTextWithOtherPartiesIncluded() throws IOException {

        String json = getJsonCallbackForTest("handlers/writefinaldecision/writeFinalDecisionWithOtherParties.json");

        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = Loader.loadPDF(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);

            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 01/01/2026 is set aside."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Decision"));
            assertThat(pdfTextWithoutNewLines, containsString("4. Reason"));

            assertThat(pdfTextWithoutNewLines, containsString("5. This has been an oral (face to face) hearing. "
                + "The following people attended: Joe Bloggs the appellant, John Smith the second respondent, "
                + "Jane Smith the third respondent, and a representative from the First Tier Agency. "
                + "David Jones the fourth respondent, Sarah Jones the fifth respondent did not attend. "
                + "The Tribunal considered the appeal bundle to page B7."));

            assertThat(pdfTextWithoutNewLines, not(containsString("6.")));
        }
    }

    private String replaceNewLines(String pdfText) {
        return pdfText.replaceAll("-\n", "-").replaceAll("[\\n\\t]", " ").replaceAll("\\s{2,}", " ");
    }

    private byte[] callPreviewFinalDecision(String json) throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(json), "PreviewFinalDecision");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(ccdEventResponse.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument(), is(not(nullValue())));
        return sscsMyaBackendRequests.toBytes(ccdEventResponse.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
    }

    private CcdEventResponse getCcdEventResponse(HttpResponse httpResponse) throws IOException {
        String response = EntityUtils.toString(httpResponse.getEntity());
        return objectMapper.readValue(response, CcdEventResponse.class);
    }
}
