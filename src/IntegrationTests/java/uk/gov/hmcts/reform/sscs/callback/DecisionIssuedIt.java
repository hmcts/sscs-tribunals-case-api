package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.idam.IdamApiClient;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@SpringBootTest
@AutoConfigureMockMvc
public class DecisionIssuedIt {

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private MockMvc mockMvc;

    @MockBean
    private AuthorisationService authorisationService;

    @Autowired
    private SscsCaseCallbackDeserializer deserializer;

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

    @MockBean
    private EvidenceManagementService evidenceManagementService;

    @MockBean
    private GenerateFile generateFile;

    private String json;

    @Before
    public void setup() throws IOException {
        CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper.registerModule(new JavaTimeModule());
        json = getJson("callback/decisionIssuedForPreview.json");

    }

    @Test
    public void callToMidEventHandler_willPreviewTheDocument() throws Exception {
        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        DirectionOrDecisionIssuedTemplateBody payload = (DirectionOrDecisionIssuedTemplateBody) capture.getValue().getFormPayload();
        assertEquals("Hello, you have been instructed to provide further information. Please do so at your earliest convenience.", payload.getNoticeBody());
        assertEquals("Maple Magoo", payload.getUserName());
        assertEquals("Proxy Judge", payload.getUserRole());
    }

    @Test
    public void callToAboutToSubmitEventHandler_willSaveTheInterlocDecisionDocumentFromPreview() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getPreviewDocument());
        assertNull(result.getData().getSignedRole());
        assertNull(result.getData().getSignedBy());
        assertNull(result.getData().getGenerateNotice());
        assertNull(result.getData().getDateAdded());
        assertEquals(4, result.getData().getSscsDocument().size());
        assertEquals(DocumentType.DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals("some location", result.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("Addition B - Decision notice issued on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")), result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("B", result.getData().getSscsDocument().get(0).getValue().getBundleAddition());
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

    private PreSubmitCallbackResponse<SscsCaseData> deserialize(String source) {
        try {
            return mapper.readValue(
                    source,
                    new TypeReference<PreSubmitCallbackResponse<SscsCaseData>>() {
                    }
            );

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not deserialize object", e);
        }
    }

    private String getJson(String fileLocation) throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
                .getResource(fileLocation)).getFile();
        return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
    }
}
