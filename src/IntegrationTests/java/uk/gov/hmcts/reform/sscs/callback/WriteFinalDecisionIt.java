package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_IN_FAVOUR_OF_APPELLANT;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.Assert;
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
public class WriteFinalDecisionIt extends AbstractEventIt {

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
        setup("callback/writeFinalDecision.json");
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
        assertEquals(true, payload.isAllowed());
        assertEquals(true, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals("An Test",payload.getAppellantName());
        assertEquals("2018-10-10",payload.getStartDate());
        assertEquals("2018-11-10",payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isDailyLivingIsEntited());
        assertEquals(true, payload.isDailyLivingIsSeverelyLimited());
        assertEquals("enhanced rate", payload.getDailyLivingAwardRate());
        Assert.assertNotNull(payload.getDailyLivingDescriptors());
        assertEquals(2, payload.getDailyLivingDescriptors().size());
        Assert.assertNotNull(payload.getDailyLivingDescriptors().get(0));
        assertEquals(8, payload.getDailyLivingDescriptors().get(0).getActivityAnswerPoints());
        assertEquals("f", payload.getDailyLivingDescriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot prepare and cook food.", payload.getDailyLivingDescriptors().get(0).getActivityAnswerValue());
        assertEquals("Preparing food", payload.getDailyLivingDescriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getDailyLivingDescriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getDailyLivingDescriptors().get(1));
        assertEquals(10, payload.getDailyLivingDescriptors().get(1).getActivityAnswerPoints());
        assertEquals("f", payload.getDailyLivingDescriptors().get(1).getActivityAnswerLetter());
        assertEquals("Cannot convey food and drink to their mouth and needs another person to do so.", payload.getDailyLivingDescriptors().get(1).getActivityAnswerValue());
        assertEquals("Taking nutrition", payload.getDailyLivingDescriptors().get(1).getActivityQuestionValue());
        assertEquals("2", payload.getDailyLivingDescriptors().get(1).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getDailyLivingNumberOfPoints());
        assertEquals(18, payload.getDailyLivingNumberOfPoints().intValue());
        assertEquals(false, payload.isMobilityIsEntited());
        assertEquals(false, payload.isMobilityIsSeverelyLimited());
        Assert.assertNull(payload.getMobilityAwardRate());
        Assert.assertNull(payload.getMobilityDescriptors());

    }

    @Test
    public void callToAboutToSubmitHandler_willWriteDraftFinalDecisionToCase() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(DECISION_IN_FAVOUR_OF_APPELLANT.getId(), result.getData().getOutcome());
    }
}
