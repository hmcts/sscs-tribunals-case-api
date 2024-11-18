package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler.getJsonCallbackForTestAndReplace;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.ClassRule;
import org.junit.Ignore;
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
public class GenDecisionNoticeFunctionalTest extends BaseFunctionTest {

    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();

    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Autowired
    protected ObjectMapper objectMapper;

    @NamedParameters("allowed")
    @SuppressWarnings("unused")
    private Boolean[] allowed() {
        return new Boolean[] { false, true};
    }

    @Test
    @Parameters(named = "allowed")
    @Ignore //Ignore this test until we support additional benefit types
    public void nonDescriptorFlow_shouldGeneratePdfWithExpectedText(boolean allowed) throws IOException {

        String json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/dlaScenarioCallbackNonDescriptorFlow.json", Arrays.asList("ALLOWED_OR_REFUSED"),
            Arrays.asList(allowed ? "allowed" : "refused"));

        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 is confirmed."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("3. My summary."));
            assertThat(pdfTextWithoutNewLines, containsString("4. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString(
                "7. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended and the Tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("8.")));
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
