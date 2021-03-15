package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.WelshFooterService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;

@SpringBootTest
@AutoConfigureMockMvc
public class CreateWelshNoticeIt extends AbstractEventIt {

    @Autowired
    private WelshFooterService welshFooterService;

    @MockBean
    private DocmosisPdfService docmosisPdfService;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private IdamClient idamClient;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private EvidenceManagementService evidenceManagementService;

    @MockBean
    UploadResponse uploadResponse;

    @Before
    public void setup() throws IOException {
        setup("callback/createWelshNotice.json");
    }

    @Test
    public void callToAboutToStart_willThrowErrorForNonWelshCase() throws Exception {
        String json = getJson("callback/createWelshNoticeNonWelsh.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        if (result.getErrors().stream().findAny().isPresent()) {
            assertEquals("Error: This action is only available for Welsh cases.",
                    result.getErrors().stream().findAny().get());
        }
    }

    @Test
    public void callToAboutToStart_willProgressCreateWelshNoticeDirection() throws Exception {
        String json = getJson("callback/createWelshNoticeDirection.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        DynamicList expectedNoticeType = new DynamicList(
                new DynamicListItem("myfile2.jpg", "myfile2.jpg"),
                Collections.singletonList(new DynamicListItem("myfile2.jpg", "myfile2.jpg"))
        );

        DynamicList expectedDocumentType = new DynamicList(
                new DynamicListItem("Direction Notice", "Directions Notice"),
                Collections.singletonList(new DynamicListItem("Direction Notice", "Directions Notice"))
        );

        assertTrue(result.getData().isLanguagePreferenceWelsh());
        assertEquals(expectedNoticeType, result.getData().getOriginalNoticeDocuments());
        assertEquals(expectedDocumentType, result.getData().getDocumentTypes());
    }

    @Test
    public void callToAboutToStart_willProgressCreateWelshNoticeDecision() throws Exception {
        String json = getJson("callback/createWelshNoticeDecision.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        DynamicList expectedNoticeType = new DynamicList(
                new DynamicListItem("myfile1.jpg", "myfile1.jpg"),
                Collections.singletonList(new DynamicListItem("myfile1.jpg", "myfile1.jpg"))
        );

        DynamicList expectedDocumentType = new DynamicList(
                new DynamicListItem("Decision Notice", "Decision Notice"),
                Collections.singletonList(new DynamicListItem("Decision Notice", "Decision Notice"))
        );

        assertTrue(result.getData().isLanguagePreferenceWelsh());
        assertEquals(expectedNoticeType, result.getData().getOriginalNoticeDocuments());
        assertEquals(expectedDocumentType, result.getData().getDocumentTypes());
    }

    @Test
    public void shouldHandleCreateWelshNoticeAndGenerateBilingualDocumentEventCallback() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);

        when(docmosisPdfService.createPdf(any(),any())).thenReturn(pdfBytes);

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());
        List<SscsWelshDocument> documentList = result.getData().getSscsWelshDocuments();

        assertEquals(2, documentList.size());
        assertEquals("Direction Notice", documentList.get(0).getValue().getDocumentType());
        assertEquals("22222.pdf", documentList.get(0).getValue().getDocumentFileName());
        assertEquals("No", result.getData().getTranslationWorkOutstanding());
        assertEquals("directionIssuedWelsh", result.getData().getSscsWelshPreviewNextEvent());
        assertEquals("Direction Notice", documentList.get(1).getValue().getDocumentType());
        assertNotNull(documentList.get(1).getValue().getDocumentFileName());
    }

    @Test
    public void givenSubmittedCallbackForCreateWelshNotice_shouldUpdateCaseAndTriggerEvent() throws Exception {
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
                eq("directionIssuedWelsh")))
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

    private UploadResponse createUploadResponse() {
        UploadResponse response = mock(UploadResponse.class);
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(response.getEmbedded()).thenReturn(embedded);
        Document document = createDocument();
        when(embedded.getDocuments()).thenReturn(Collections.singletonList(document));
        return response;
    }

    private Document createDocument() {
        Document document = new Document();
        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = "www.logo.com";
        links.self = link;
        document.links = links;
        return document;
    }
}


