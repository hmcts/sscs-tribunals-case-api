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
import lombok.SneakyThrows;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.functional.mya.BaseFunctionTest;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
public class PipDecisionNoticeFunctionalTest extends BaseFunctionTest {

    @Autowired
    protected ObjectMapper objectMapper;


    @SuppressWarnings("unused")
    private static Boolean[] allowed() {
        return new Boolean[]{false, true};
    }

    @ParameterizedTest
    @MethodSource("allowed")
    public void nonDescriptorFlow_shouldGeneratePdfWithExpectedText(boolean allowed) throws IOException {

        String json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallbackNonDescriptorFlow.json", Arrays.asList("ALLOWED_OR_REFUSED"),
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
                    "7. This has been a remote hearing in the form of a video hearing. Joe Bloggs the appellant attended and the Tribunal considered the appeal bundle to page B7. First Tier Agency representative attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("8.")));
        }
    }

    private String replaceNewLines(String pdfText) {
        return pdfText.replaceAll("-\n", "-").replaceAll("[\\n\\t]", " ").replaceAll("\\s{2,}", " ");
    }

    private byte[] callPreviewFinalDecision(String json) throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(json), "PreviewFinalDecision");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getCode(), is(200));
        assertThat(ccdEventResponse.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument(), is(not(nullValue())));
        return sscsMyaBackendRequests.toBytes(ccdEventResponse.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
    }

    @SneakyThrows
    private CcdEventResponse getCcdEventResponse(HttpResponse httpResponse) {
        String response = EntityUtils.toString(((ClassicHttpResponse)httpResponse).getEntity());
        return objectMapper.readValue(response, CcdEventResponse.class);
    }
}
