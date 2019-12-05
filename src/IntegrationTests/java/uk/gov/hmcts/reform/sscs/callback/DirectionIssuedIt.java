package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.DwpState;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.idam.IdamApiClient;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@SpringBootTest
@AutoConfigureMockMvc
public class DirectionIssuedIt extends AbstractEventIt {

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

    @Before
    public void setup() throws IOException {
        setup("callback/directionIssuedForPreview.json");
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
    public void callToAboutToSubmitEventHandler_willSaveTheInterlocDirectionDocumentFromPreview() throws Exception {
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
        assertEquals(DocumentType.DIRECTION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals("some location", result.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("Addition B - Directions notice issued on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("B", result.getData().getSscsDocument().get(0).getValue().getBundleAddition());
        assertEquals("some location", result.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals(DwpState.DIRECTION_ACTION_REQUIRED.getId(), result.getData().getDwpState());
        assertEquals("awaitingInformation", result.getData().getInterlocReviewState());
    }

}
