package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.Collections;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementSecureDocStoreService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class CreateBundleIt extends AbstractEventIt {

    @MockBean
    private CcdService ccdService;

    @MockBean
    private IdamService idamService;

    @MockBean
    private RestTemplate restTemplate;

    @Mock
    private ResponseEntity responseEntity;

    @MockBean
    private EvidenceManagementSecureDocStoreService evidenceManagementService;

    @MockBean
    private DocmosisPdfService docmosisPdfService;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Captor
    private ArgumentCaptor<HttpEntity<Callback<SscsCaseData>>> captor;

    @Before
    public void setup() throws IOException {
        CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper.registerModule(new JavaTimeModule());
        json = getJson("callback/createBundleCallback.json");

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    public void callToAboutToSubmitHandler_willCallExternalCreateBundleService() throws Exception {

        verifyBundlingServiceIsCalled();

        assertEquals(1,  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().size());
        assertEquals("sscs-bundle-config.yaml",  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().get(0).getValue());
    }

    @Test
    public void callToAboutToSubmitHandlerWithWelshCase_willCallExternalCreateBundleService() throws Exception {
        json = getJson("callback/createWelshBundleCallback.json");

        verifyBundlingServiceIsCalled();

        assertEquals(1,  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().size());
        assertEquals("sscs-bundle-welsh-config.yaml",  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().get(0).getValue());
    }

    @Test
    public void callToAboutToSubmitHandlerWithEditedDocuments_willCallExternalCreateBundleServiceWithMultiBundleConfig() throws Exception {
        json = getJson("callback/createBundleCallbackWithEditedDocuments.json");

        verifyBundlingServiceIsCalled();

        assertEquals(2,  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().size());
        assertEquals("sscs-bundle-edited-config.yaml",  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("sscs-bundle-config.yaml",  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().get(1).getValue());
    }

    @Test
    public void callToAboutToSubmitHandlerWithWelshCaseAndEditedDocuments_willCallExternalCreateBundleServiceWithMultiBundleConfig() throws Exception {
        json = getJson("callback/createWelshBundleCallbackWithEditedDocuments.json");

        verifyBundlingServiceIsCalled();

        assertEquals(2,  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().size());
        assertEquals("sscs-bundle-welsh-edited-config.yaml",  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("sscs-bundle-welsh-config.yaml",  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().get(1).getValue());
    }

    @Test
    public void callToAboutToSubmitHandlerWithAudioVideoEvidence_willCallExternalCreateBundleService() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), idamService.getIdamTokens())).thenReturn(uploadResponse);

        when(docmosisPdfService.createPdf(any(),any())).thenReturn(pdfBytes);

        json = getJson("callback/createBundleCallbackWithAudioVideoEvidence.json");

        verifyBundlingServiceIsCalled();

        assertEquals(2,  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().size());
        assertEquals("sscs-bundle-edited-config.yaml",  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("sscs-bundle-config.yaml",  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().get(1).getValue());
        assertEquals("Audio/video evidence document",  captor.getValue().getBody().getCaseDetails().getCaseData().getAudioVideoEvidenceBundleDocument().getDocumentFileName());

    }

    @Test
    public void callToAboutToSubmitHandlerWithEnhancedConfidentialityEditedDocuments_willCallExternalCreateBundleServiceWithMultiBundleConfig() throws Exception {
        json = getJson("callback/createBundleCallbackWithEnhancedConfidentialityEditedDocuments.json");

        verifyBundlingServiceIsCalled();

        assertEquals(2,  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().size());
        assertEquals("sscs-bundle-edited-config.yaml",  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("sscs-bundle-config.yaml",  captor.getValue().getBody().getCaseDetails().getCaseData().getMultiBundleConfiguration().get(1).getValue());
    }

    private void verifyBundlingServiceIsCalled() throws Exception {
        when(restTemplate.exchange(eq("/api/new-bundle"), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        SscsCaseData ingoreReturnedCaseData = SscsCaseData.builder().build();
        when(responseEntity.getBody()).thenReturn(new PreSubmitCallbackResponse<CaseData>(ingoreReturnedCaseData));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        verify(restTemplate).exchange(eq("/api/new-bundle"), eq(HttpMethod.POST), captor.capture(), any(ParameterizedTypeReference.class));
    }

    private UploadResponse createUploadResponse() {
        Document.Links links = new Document.Links();

        Document.Link self = new Document.Link();
        Document.Link binary = new Document.Link();
        self.href = "www.logo.com";
        binary.href = "www.logo.com";

        links.self = self;
        links.binary = binary;

        Document document = Document.builder().links(links).build();

        UploadResponse response = mock(UploadResponse.class);

        when(response.getDocuments()).thenReturn(Collections.singletonList(document));
        return response;
    }

    private Document createDocument() {
        Document document = Document.builder().build();

        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = "www.logo.com";
        links.self = link;
        document.links = links;
        return document;
    }
}
