package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import org.junit.Assert;
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
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
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

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForDescriptorRoute() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptor.json", "START_DATE_PLACEHOLDER", "2018-10-10");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals("An Test", parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
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
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());

    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForNotConsideredDescriptorRoute() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorNotConsidered.json", "START_DATE_PLACEHOLDER", "2018-10-10");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals("An Test", parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
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
        Assert.assertNotNull(payload.getMobilityAwardRate());
        assertEquals("not considered", payload.getMobilityAwardRate());
        Assert.assertNull(payload.getMobilityDescriptors());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());

    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForNonDescriptorRoute() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionNonDescriptor.json", "START_DATE_PLACEHOLDER", "2018-10-10");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals("An Test", parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
        assertEquals(true, payload.isAllowed());
        assertEquals(true, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals("An Test",payload.getAppellantName());
        assertNull(payload.getStartDate());
        assertNull(payload.getEndDate());
        assertEquals(true, payload.isIndefinite());
        assertEquals(false, payload.isDailyLivingIsEntited());
        assertEquals(false, payload.isDailyLivingIsSeverelyLimited());
        assertNull(payload.getDailyLivingAwardRate());
        assertNull(payload.getDailyLivingDescriptors());
        Assert.assertNull(payload.getDailyLivingNumberOfPoints());
        assertEquals(false, payload.isMobilityIsEntited());
        assertEquals(false, payload.isMobilityIsSeverelyLimited());
        assertNull(payload.getMobilityAwardRate());
        assertNull(payload.getMobilityDescriptors());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertNotNull(payload.getDetailsOfDecision());
        Assert.assertEquals("The details of the decision", payload.getDetailsOfDecision());
        assertEquals("Something else.", payload.getAnythingElse());
    }

    @Test
    public void callToMidEventCallback_willValidateTheDatePip() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptor.json", "START_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Decision notice end date must be after decision notice start date", result.getErrors().toArray()[0]);

    }

    @Test
    public void callToMidEventCallback_willValidateTheDateEsa() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESA.json", "START_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Decision notice end date must be after decision notice start date", result.getErrors().toArray()[0]);

    }

    @Test
    public void callToAboutToSubmitHandler_willWriteDraftFinalDecisionToCase() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptor.json", "START_DATE_PLACEHOLDER", "2018-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());

        assertEquals(DRAFT_DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Decision Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    /**
     * Due to a CCD bug ( https://tools.hmcts.net/jira/browse/RDM-8200 ) we have had
     * to implement a workaround in WriteFinalDecisionAboutToSubmitHandler to set
     * the generated date to now, even though it is already being determined by the
     * preview document handler.  This is because on submission, the correct generated date
     * (the one referenced in the preview document) is being overwritten to a null value.
     * Once RDM-8200 is fixed and we remove the workaround, this test should be changed
     * to assert that a "something has gone wrong" error is displayed instead of
     * previewing the document, as a null generated date would indicate that the
     * date in the preview document hasn't been set.
     *
     */
    @Test
    public void callToAboutToSubmitHandler_willWriteDraftFinalDecisionToCaseWithGeneratedDateAsNowWhenSubmittedGeneratedDateIsNull() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorNullGeneratedDate.json", "START_DATE_PLACEHOLDER", "2018-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());


        assertNotNull(result.getData().getWriteFinalDecisionGeneratedDate());
        assertEquals(LocalDate.now().toString(), result.getData().getWriteFinalDecisionGeneratedDate());

        assertEquals(DRAFT_DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Decision Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    /**
     * This test asserts that whatever the value of the existing generated date from CCD
     * submitted as part of the payload to the WriterFinalSubmissionAboutToSubmitHandler,
     * then that date is updated to now() after the WriterFinalSubmissionAboutToSubmitHandler is called.
     * This is due to a workaround we have implemented in the WriterFinalSubmissionAboutToSubmitHandler
     *
     */
    @Test
    public void callToAboutToSubmitHandler_willWriteDraftFinalDecisionToCaseWithGeneratedDateAsNowWhenSubmittedGeneratedDateIsSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorSetGeneratedDate.json", "START_DATE_PLACEHOLDER", "2018-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());


        assertNotNull(result.getData().getWriteFinalDecisionGeneratedDate());

        assertEquals(LocalDate.now().toString(), result.getData().getWriteFinalDecisionGeneratedDate());

        assertEquals(DRAFT_DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Decision Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void callToAboutToSubmitHandler_willWriteDraftFinalDecisionToCaseForNonDescriptorRoute() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionNonDescriptor.json", "START_DATE_PLACEHOLDER", "2018-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());

        assertEquals(DRAFT_DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Decision Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void callToAboutToSubmitHandler_willWriteManuallyUploadedFinalDecisionToCase() throws Exception {
        setup("callback/writeFinalDecisionManualUploadDescriptor.json");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());

        assertEquals(DRAFT_DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Decision Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void callToAboutToSubmitHandler_willWriteManuallyUploadedFinalDecisionToCaseForNonDescriptorRoute() throws Exception {
        setup("callback/writeFinalDecisionManualUploadNonDescriptor.json");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());

        assertEquals(DRAFT_DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Decision Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }
}
