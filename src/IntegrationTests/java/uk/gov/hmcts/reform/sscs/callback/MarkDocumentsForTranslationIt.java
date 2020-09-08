package uk.gov.hmcts.reform.sscs.callback;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;


@SpringBootTest
@AutoConfigureMockMvc
public class MarkDocumentsForTranslationIt extends AbstractEventIt {

    @Before
    public void setup() throws IOException {
        setup("callback/markDocumentsForTranslation.json");
    }

    @Test
    public void callToAboutToStart_willPopulateWhoReviewsCase() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertTrue(result.getData().isLanguagePreferenceWelsh());
    }

    @Test
    public void callToAboutToSubmit_willPopulateWhoReviewsCase() throws Exception {
        String json = getJson("callback/markDocumentsForTranslation.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        final List<SscsDocument> appellantEvidence =
                result.getData().getSscsDocument().stream()
                        .filter(data -> data.getValue().getDocumentType()
                                .equalsIgnoreCase("appellantEvidence") || data.getValue().getDocumentType().equalsIgnoreCase("sscs1"))
                        .collect(Collectors.toList());


        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED, appellantEvidence.get(0).getValue().getDocumentTranslationStatus());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                appellantEvidence.get(1).getValue().getDocumentTranslationStatus());

    }


}


