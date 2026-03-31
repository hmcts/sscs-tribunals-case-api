package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler.getJsonCallbackForTestAndReplace;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.TribunalsCaseApiApplication;
import uk.gov.hmcts.reform.sscs.functional.mya.BaseFunctionTest;
import uk.gov.hmcts.reform.sscs.functional.mya.CitizenIdamService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TribunalsCaseApiApplication.class, CitizenIdamService.class})
@TestPropertySource(locations = "classpath:config/application_functional.properties")
public class GenDecisionNoticeFunctionalTest extends BaseFunctionTest {

    @Autowired
    protected ObjectMapper objectMapper;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Disabled //Ignore this test until we support additional benefit types
    public void nonDescriptorFlow_shouldGeneratePdfWithExpectedText(boolean allowed) throws IOException {

        String json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/dlaScenarioCallbackNonDescriptorFlow.json",
            List.of("ALLOWED_OR_REFUSED"),
            List.of(allowed ? "allowed" : "refused"));

        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = Loader.loadPDF(bytes)) {
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

    @ParameterizedTest
    @CsvSource({"Yes", "No"})
    public void shouldGenerateExpectedDecisionTextWithOtherPartiesIncluded(String isAppointeeOnCase) throws IOException {

        String json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/writeFinalDecisionWithOtherParties.json",
            List.of("IS_APPOINTEE_ON_CASE"),
            List.of(isAppointeeOnCase));

        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = Loader.loadPDF(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);

            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 01/01/2026 is set aside."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Decision"));
            assertThat(pdfTextWithoutNewLines, containsString("4. Reason"));

            if ("Yes".equalsIgnoreCase(isAppointeeOnCase)) {
                assertThat(pdfTextWithoutNewLines, containsString("5. This has been an oral (face to face) hearing. "
                    + "The following people attended: John Smith the second respondent, "
                    + "Jane Smith the third respondent and a representative from the First Tier Agency. "
                    + "Mary Bloggs the appointee, David Jones the fourth respondent and Sarah Jones the fifth respondent did not attend. "));

                assertThat(pdfTextWithoutNewLines, containsString("6. Having considered the appeal bundle to page B7 "
                    + "and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) "
                    + "Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Mary Bloggs of the hearing "
                    + "and that it is in the interests of justice to proceed today. "
                    + "Tribunal Judge: A User Date: 20/02/2026 Issued to the parties on: 20/02/2026 "
                    + "Corrected decision signed by: Service Account Date of correction: 10/03/2026 "
                    + "Corrected notice issued to parties on: "));

                assertThat(pdfTextWithoutNewLines, not(containsString("7.")));
            } else {

                assertThat(pdfTextWithoutNewLines, containsString("5. This has been an oral (face to face) hearing. "
                    + "The following people attended: Joe Bloggs the appellant, John Smith the second respondent, "
                    + "Jane Smith the third respondent and a representative from the First Tier Agency. "
                    + "David Jones the fourth respondent and Sarah Jones the fifth respondent did not attend. "
                    + "The Tribunal considered the appeal bundle to page B7."));

                assertThat(pdfTextWithoutNewLines, not(containsString("6.")));
            }
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
