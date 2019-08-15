package uk.gov.hmcts.reform.sscs.callback;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.*;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.idam.Authorize;
import uk.gov.hmcts.reform.sscs.idam.IdamApiClient;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@SpringBootTest
@AutoConfigureMockMvc
public class CcdCallbackEndpointIt {

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private MockMvc mockMvc;

    @MockBean
    private AuthorisationService authorisationService;

    @Autowired
    private SscsCaseCallbackDeserializer deserializer;

    private String json;

    @Autowired
    private PreSubmitCallbackDispatcher dispatcher;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;
    @MockBean
    private IdamApiClient idamApiClient;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Before
    public void setup() {
        CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void shouldHandleActionFurtherEvidenceEventCallback() throws Exception {
        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource("callback/actionFurtherEvidenceCallback.json")).getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        List<SscsDocument> documentList = result.getData().getSscsDocument();
        assertEquals(1, documentList.size());
        assertNull(result.getData().getScannedDocuments());
        assertEquals("appellantEvidence", documentList.get(0).getValue().getDocumentType());
        assertEquals("3", documentList.get(0).getValue().getControlNumber());
        assertEquals("scanned.pdf", documentList.get(0).getValue().getDocumentFileName());
        assertEquals("http://localhost:4603/documents/f812db06-fd5a-476d-a603-bee44b2ecd49", documentList.get(0).getValue().getDocumentLink().getDocumentUrl());
    }

    @Test
    public void actionFurtherEvidenceDropdownAboutToStartCallback() throws Exception {
        String path = getClass().getClassLoader().getResource("callback/actionFurtherEvidenceCallback.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertEquals(3, result.getData().getOriginalSender().getListItems().size());
        assertEquals(2, result.getData().getFurtherEvidenceAction().getListItems().size());
        assertEquals(ISSUE_FURTHER_EVIDENCE.getCode(), result.getData().getFurtherEvidenceAction().getListItems().get(0).getCode());
        assertEquals(OTHER_DOCUMENT_MANUAL.getCode(), result.getData().getFurtherEvidenceAction().getListItems().get(1).getCode());
    }

    @Test
    public void givenFurtherEvidenceIssueToAllParties_shouldUpdateDwpFurtherEvidenceState() throws Exception {
        String path = Objects.requireNonNull(getClass().getClassLoader()
                .getResource("callback/actionFurtherEvidenceWithInterlocOptionCallback.json")).getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replaceFirst("informationReceivedForInterloc", "issueFurtherEvidence");
        json = json.replaceFirst("Information received for interlocutory review", "Issue further evidence to all parties");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertNull(result.getData().getInterlocReviewState());
        assertEquals("furtherEvidenceReceived", result.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    public void givenFurtherEvidenceIssueToAllParties_onSubmitted_willStart_IssueFurtherEvidenceEvent() throws Exception {
        mockIdam();
        given(coreCaseDataApi.startEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
                eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
                eq("issueFurtherEvidence")))
                .willReturn(StartEventResponse.builder().build());

        given(coreCaseDataApi.submitEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
                eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
                eq(true), any(CaseDataContent.class)))
                .willReturn(CaseDetails.builder()
                        .id(123L)
                        .data(new HashMap<>())
                        .build());

        String path = Objects.requireNonNull(getClass().getClassLoader()
                .getResource("callback/actionFurtherEvidenceWithInterlocOptionCallback.json")).getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replaceFirst("informationReceivedForInterloc", "issueFurtherEvidence");
        json = json.replaceFirst("Information received for interlocutory review", "Issue further evidence to all parties");


        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdSubmittedEvent"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        verify(coreCaseDataApi).startEventForCaseWorker(any(), anyString(), anyString(), anyString(),
                anyString(), eq("12345656789"), eq("issueFurtherEvidence"));
    }

    @Test
    public void givenSubmittedCallbackForActionFurtherEvidence_shouldUpdateFieldAndTriggerEvent() throws Exception {
        mockIdam();
        mockCcd();

        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource("callback/actionFurtherEvidenceWithInterlocOptionCallback.json")).getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdSubmittedEvent"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertEquals("interlocutoryReview", result.getData().getInterlocReviewState());
        assertNull(result.getData().getDwpFurtherEvidenceStates());
    }

    private void mockCcd() {
        given(coreCaseDataApi.startEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
            eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
            eq("interlocInformationReceived")))
            .willReturn(StartEventResponse.builder().build());

        Map<String, Object> data = new HashMap<>();
        data.put("interlocReviewState", "interlocutoryReview");
        given(coreCaseDataApi.submitEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
            eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
            eq(true), any(CaseDataContent.class)))
            .willReturn(CaseDetails.builder()
                .id(123L)
                .data(data)
                .build());
    }

    private void mockIdam() {
        given(idamApiClient.authorizeCodeType(anyString(), eq("code"), eq("sscs"),
            eq("https://localhost:3000/authenticated"), eq(" ")))
            .willReturn(Authorize.builder().code("code").build());

        given(idamApiClient.authorizeToken(anyString(), eq("authorization_code"),
            eq("https://localhost:3000/authenticated"), eq("sscs"), anyString(), eq(" ")))
            .willReturn(Authorize.builder().accessToken("authToken").build());

        given(idamApiClient.getUserDetails("Bearer authToken"))
            .willReturn(UserDetails.builder().id("userId").build());

        given(authTokenGenerator.generate()).willReturn("s2s token");
    }

    @Test
    public void coversheetFurtherEvidence_shouldNotAddToDocuments() throws Exception {
        String path = getClass().getClassLoader().getResource("callback/actionFurtherEvidenceWithInterlocOptionCallback.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replaceFirst("informationReceivedForInterloc", "otherDocumentManual");
        json = json.replaceFirst("Information received for interlocutory review", "Other document type - action manually");
        json = json.replaceFirst("appellantEvidence", "coversheet");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertNull(result.getData().getInterlocReviewState());
        assertNull(result.getData().getScannedDocuments());
        assertNull(result.getData().getSscsDocument());
        assertEquals("Yes", result.getData().getEvidenceHandled());
    }

    @Test
    public void shouldHandleInterlocEventEventCallback() throws Exception {
        String path = getClass().getClassLoader().getResource("callback/interlocEventCallback.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertEquals("reviewByTcw", result.getData().getInterlocReviewState());
    }

    @Test
    public void shouldHandleSendToDwpOfflineEventCallback() throws Exception {
        String path = getClass().getClassLoader().getResource("callback/sendToDwpOfflineEventCallback.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertNull(result.getData().getHmctsDwpState());
    }

    @Test
    public void shouldHandleTcwDecisionAppealToProceedEventCallback() throws Exception {
        String path = getClass().getClassLoader().getResource("callback/tcwDecisionAppealToProceedEventCallback.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void shouldHandleJudgeDecisionAppealToProceedEventCallback() throws Exception {
        String path = getClass().getClassLoader().getResource("callback/judgeDecisionAppealToProceedEventCallback.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertTrue(result.getErrors().isEmpty());
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

    public PreSubmitCallbackResponse deserialize(String source) {
        try {
            PreSubmitCallbackResponse callback = mapper.readValue(
                source,
                new TypeReference<PreSubmitCallbackResponse<SscsCaseData>>() {
                }
            );

            return callback;

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not deserialize object", e);
        }
    }
}
