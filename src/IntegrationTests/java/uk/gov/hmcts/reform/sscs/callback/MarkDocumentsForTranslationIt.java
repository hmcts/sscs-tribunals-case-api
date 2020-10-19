package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;

@SpringBootTest
@AutoConfigureMockMvc
public class MarkDocumentsForTranslationIt extends AbstractEventIt {

    @Before
    public void setup() throws IOException {
        setup("callback/markDocumentsForTranslation.json");
    }

    @Test
    public void callToAboutToStart_willCheckIfCaseIsWelshElseThrowErrorMessage() throws Exception {
        String json = getJson("callback/markDocumentsForTranslation-nonWelsh.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        if (result.getErrors().stream().findAny().isPresent()) {
            assertEquals("Error: This action is only available for Welsh cases.",
                    result.getErrors().stream().findAny().get());
        }
    }

    @Test
    public void callToAboutToStart_willProgressCaseWelshCase() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertTrue(result.getData().isLanguagePreferenceWelsh());
    }

    @Test
    public void callToAboutToSubmit_willUpdateTranslationStatusOfSscsDocumentsAndTranslationOutStandingFlag() throws Exception {

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        result.getData().getSscsDocument().stream()
                .filter(data -> data.getValue().getDocumentType()
                        .equalsIgnoreCase("appellantEvidence") || data.getValue().getDocumentType().equalsIgnoreCase("sscs1"))
                .forEach(data -> assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED, data.getValue().getDocumentTranslationStatus()));
        assertEquals("Yes", result.getData().getTranslationWorkOutstanding());
    }
}


