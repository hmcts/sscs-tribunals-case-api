package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
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
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@SpringBootTest
@AutoConfigureMockMvc
public class FinalDecisionIssuedForPreviewIt extends AbstractEventIt {

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private IdamClient idamClient;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private EvidenceManagementService evidenceManagementService;

    @MockBean
    private GenerateFile generateFile;

    @MockBean
    private UserDetails userDetails;

    @Before
    public void setup() throws IOException {
        setup("callback/finalDecisionIssuedForPreview.json");
    }

    @Test
    public void callToMidEventHandler_willPreviewTheDocument() throws Exception {
        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        DirectionOrDecisionIssuedTemplateBody payload = (DirectionOrDecisionIssuedTemplateBody) capture.getValue().getFormPayload();
        assertEquals("An Test", payload.getAppellantFullName());
        assertEquals("12345656789", payload.getCaseId());
        assertEquals("JT 12 34 56 D", payload.getNino());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
    }
}
