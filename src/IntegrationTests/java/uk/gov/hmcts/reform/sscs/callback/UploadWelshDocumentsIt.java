package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;

@SpringBootTest
@AutoConfigureMockMvc
public class UploadWelshDocumentsIt extends AbstractEventIt {

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private IdamClient idamClient;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Before
    public void setup() throws IOException {
        setup("callback/uploadWelshDocuments.json");
    }

    @Test
    public void callToAboutToStart_willPopulateOriginalDocumentDropdown() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        DynamicList expected = new DynamicList(
                new DynamicListItem("appellantEvidence.pdf", "appellantEvidence.pdf"),
                Arrays.asList(new DynamicListItem("appellantEvidence.pdf", "appellantEvidence.pdf"),
                        new DynamicListItem("sscs1.pdf", "sscs1.pdf"))
        );
        assertEquals(expected, result.getData().getOriginalDocuments());
    }

    @Test
    public void callToAboutToStart_willProgressWelshCase() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertTrue(result.getData().isLanguagePreferenceWelsh());
    }

    @Test
    public void callToAboutToSubmit_willUpdateCaseAndSetNextEvent() throws Exception {
        String json = getJson("callback/uploadWelshDocumentsOriginalDocumentSelected.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        result.getData().getSscsDocument().stream()
                .filter(sd -> SscsDocumentTranslationStatus.TRANSLATION_REQUESTED
                        .equals(sd.getValue().getDocumentTranslationStatus()))
                .filter(sd -> result.getData().getOriginalDocuments().getValue().getCode()
                        .equals(sd.getValue().getDocumentLink().getDocumentFilename()))
                .forEach(data -> assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE,
                        data.getValue().getDocumentTranslationStatus()));
        // As only one document can be processed at a given time
        assertEquals("Yes", result.getData().getTranslationWorkOutstanding());
        assertEquals("sendToDwp", result.getData().getSscsWelshPreviewNextEvent());
    }

    @Test
    public void givenSubmittedCallbackForCancelTranslation_shouldUpdateCaseAndTriggerEvent() throws Exception {
        mockIdam();
        mockCcd();

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdSubmittedEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertNull(result.getData().getSscsWelshPreviewNextEvent());
    }


    private void mockCcd() {
        given(coreCaseDataApi.startEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
                eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
                eq("sendToDwp")))
                .willReturn(StartEventResponse.builder().build());

        Map<String, Object> data = new HashMap<>();
        data.put("sscsWelshPreviewNextEvent", null);
        given(coreCaseDataApi.submitEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
                eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
                eq(true), any(CaseDataContent.class)))
                .willReturn(CaseDetails.builder()
                        .id(123L)
                        .data(data)
                        .build());
    }

    private void mockIdam() {
        given(idamClient.getAccessToken(anyString(), anyString()))
                .willReturn("Bearer authToken");

        given(idamClient.getUserInfo(anyString()))
                .willReturn(new UserInfo("16", "userId", "", "", "", Arrays.asList("caseworker", "citizen")));

        given(authTokenGenerator.generate()).willReturn("s2s token");
    }
}
