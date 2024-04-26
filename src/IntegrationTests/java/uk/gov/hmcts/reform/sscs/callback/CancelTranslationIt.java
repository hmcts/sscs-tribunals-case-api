package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;


@SpringBootTest
@AutoConfigureMockMvc
public class CancelTranslationIt extends AbstractEventIt {

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private IdamClient idamClient;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Before
    public void setup() throws IOException {
        setup("callback/cancelTranslation.json");
    }

    @Test
    public void callToAboutToStart_willThrowErrorForNonWelshCase() throws Exception {
        String json = getJson("callback/cancelTranslationNonWelsh.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        if (result.getErrors().stream().findAny().isPresent()) {
            assertEquals("Error: This action is only available for Welsh cases.",
                    result.getErrors().stream().findAny().get());
        }
    }

    @Test
    public void callToAboutToStart_willProgressWelshCase() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertTrue(result.getData().isLanguagePreferenceWelsh());
    }

    @Test
    public void callToAboutToSubmit_willClearTranslationStatusAndSetNextEvent() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        result.getData().getSscsDocument().stream()
                .filter(sd -> SscsDocumentTranslationStatus.TRANSLATION_REQUIRED
                        .equals(sd.getValue().getDocumentTranslationStatus()) || SscsDocumentTranslationStatus.TRANSLATION_REQUESTED
                        .equals(sd.getValue().getDocumentTranslationStatus()))
                .forEach(data -> assertNull(data.getValue().getDocumentTranslationStatus()));
        assertEquals("No", result.getData().getTranslationWorkOutstanding());
        assertEquals("sendToDwp", result.getData().getSscsWelshPreviewNextEvent());
    }

    @Test
    public void givenSubmittedCallbackForCancelTranslation_shouldUpdateFieldAndTriggerEvent() throws Exception {
        mockIdam();
        mockCcd();

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdSubmittedEvent"));
        assertHttpStatus(response, HttpStatus.OK);

        verify(coreCaseDataApi, atLeast(1)).startEventForCaseWorker(any(), anyString(), anyString(), eq("SSCS"),
                eq("Benefit"), eq("12345656789"), eq("updateCaseOnly"));
        verify(coreCaseDataApi, atLeast(1)).startEventForCaseWorker(any(), anyString(), anyString(), eq("SSCS"),
                eq("Benefit"), eq("12345656789"), eq("sendToDwp"));
        verify(coreCaseDataApi).submitEventForCaseWorker(anyString(), anyString(), anyString(), eq("SSCS"),
                eq("Benefit"), eq("12345656789"), eq(true), any(CaseDataContent.class));
    }


    private void mockCcd() {
        Map<String, Object> caseData = new HashMap<>();
        caseData.put("sscsWelshPreviewNextEvent", "sendToDwp");
        caseData.put("ccdCaseId", "12345656789");

        StartEventResponse startEventResponse = StartEventResponse.builder()
                .caseDetails(CaseDetails.builder()
                        .id(12345656789L)
                        .data(caseData)
                        .build())
                .build();
        given(coreCaseDataApi.startEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
                eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
                eq("updateCaseOnly")))
                .willReturn(startEventResponse);

        given(coreCaseDataApi.startEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
                eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
                eq("sendToDwp")))
                .willReturn(startEventResponse);

        Map<String, Object> data = new HashMap<>();
        data.put("sscsWelshPreviewNextEvent", null);
        given(coreCaseDataApi.submitEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
                eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
                eq(true), any(CaseDataContent.class)))
                .willReturn(CaseDetails.builder()
                        .id(12345656789L)
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


