package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_IN_FAVOUR_OF_APPELLANT;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@SpringBootTest
@AutoConfigureMockMvc
public class IssueFinalDecisionIt extends AbstractEventIt {

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private EvidenceManagementService evidenceManagementService;

    @Before
    public void setup() throws IOException {
        json = getJson("callback/issueFinalDecisionDescriptorCallback.json");
        super.setup();
    }

    @Test
    public void callToAboutToSubmitHandlerForDescriptorGenerateRoute_willMoveDraftDecisionToDecisionNotice() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());
        assertEquals(DECISION_IN_FAVOUR_OF_APPELLANT.getId(), result.getData().getOutcome());

        assertEquals(FINAL_DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(FINAL_DECISION_ISSUED, result.getData().getDwpState());
        assertEquals("some location", result.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("Addition B - Final Decision Notice issued on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("B", result.getData().getSscsDocument().get(0).getValue().getBundleAddition());
        assertEquals("some location", result.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());

    }

    @Test
    public void callToAboutToSubmitHandlerForNonDescriptorGenerateRoute_willMoveDraftDecisionToDecisionNotice() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());
        assertEquals(DECISION_IN_FAVOUR_OF_APPELLANT.getId(), result.getData().getOutcome());

        assertEquals(FINAL_DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(FINAL_DECISION_ISSUED, result.getData().getDwpState());
        assertEquals("some location", result.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("Addition B - Final Decision Notice issued on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("B", result.getData().getSscsDocument().get(0).getValue().getBundleAddition());
        assertEquals("some location", result.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());

    }

    @Test
    public void callToAboutToSubmitHandlerForNonDescriptorUploadRoute_willMoveDraftDecisionToDecisionNotice() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());
        assertEquals(DECISION_IN_FAVOUR_OF_APPELLANT.getId(), result.getData().getOutcome());

        assertEquals(FINAL_DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(FINAL_DECISION_ISSUED, result.getData().getDwpState());
        assertEquals("some location", result.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("Addition B - Final Decision Notice issued on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("B", result.getData().getSscsDocument().get(0).getValue().getBundleAddition());
        assertEquals("some location", result.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());

    }

    @Test
    public void callToAboutToSubmitHandlerForDescriptorUploadRoute_willMoveDraftDecisionToDecisionNotice() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());
        assertEquals(DECISION_IN_FAVOUR_OF_APPELLANT.getId(), result.getData().getOutcome());

        assertEquals(FINAL_DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(FINAL_DECISION_ISSUED, result.getData().getDwpState());
        assertEquals("some location", result.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("Addition B - Final Decision Notice issued on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("B", result.getData().getSscsDocument().get(0).getValue().getBundleAddition());
        assertEquals("some location", result.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());

    }


}
