package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios.EsaScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class EsaWriteFinalDecisionIt extends WriteFinalDecisionItBase {

    @Test
    public void callToMidEventCallback_willValidateTheDate() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESA.json", "START_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Decision notice end date must be after decision notice start date", result.getErrors().toArray()[0]);

    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForDescriptorRoute() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESA.json", "START_DATE_PLACEHOLDER", "2018-10-10");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
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
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isEsaIsEntited());
        assertEquals("lower rate", payload.getEsaAwardRate());
        Assert.assertNotNull(payload.getEsaSchedule2Descriptors());
        assertEquals(1, payload.getEsaSchedule2Descriptors().size());
        assertEquals(15, payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getEsaNumberOfPoints());
        assertEquals(15, payload.getEsaNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertEquals("The appeal is allowed.\n"
                + "\n"
                + "The decision made by the Secretary of State on 01/09/2018 is set aside.\n"
                + "\n"
                + "AN Test has limited capability for work.\n"
                + "\n"
                + "In applying the Work Capability Assessment 15 points were scored from the activities and descriptors in Schedule 2 of the Employment and Support Allowance (ESA) Regulations 2008 made up as follows:\n"
                + "\n"
                + "1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.\ta.Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.\t15\n"
                + "\n"
                + "\n"
                + "AN Test does not have limited capability for work-related activity because no descriptor from Schedule 3 of the ESA Regulations applied. Regulation 35 did not apply.\n"
                + "\n"
                + "My reasons for decision\n"
                + "\n"
                + "Something else.\n"
                + "\n"
                + "This has been an oral (face to face) hearing. AN Test the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"
                + "\n", parentPayload.getWriteFinalDecisionTemplateContent().toString());
    }

    @Override
    protected String getBenefitType() {
        return "ESA";
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario1_ZeroPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "REGULATION_29", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\","), Arrays.asList("2018-10-10", "refused", "No", "1e", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());


        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
        assertEquals(false, payload.isAllowed());
        assertEquals(false, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(false, payload.isEsaIsEntited());
        assertEquals("no award", payload.getEsaAwardRate());
        Assert.assertNotNull(payload.getEsaSchedule2Descriptors());
        assertEquals(0, payload.getEsaSchedule2Descriptors().size());
        assertEquals(0, payload.getEsaNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_1, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario1_LowPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "REGULATION_29", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\","), Arrays.asList("2018-10-10", "refused", "No", "1c", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
        assertEquals(false, payload.isAllowed());
        assertEquals(false, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(false, payload.isEsaIsEntited());
        assertEquals("no award", payload.getEsaAwardRate());
        Assert.assertNotNull(payload.getEsaSchedule2Descriptors());
        assertEquals(1, payload.getEsaSchedule2Descriptors().size());
        assertEquals(9, payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getEsaNumberOfPoints());
        assertEquals(9, payload.getEsaNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_1, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_LowPoints_WhenIncorrectlyAllowed() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "REGULATION_29", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\","), Arrays.asList("2018-10-10", "allowed", "No", "1c", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        // Assert we get a user-friendly error message, as this combination is a possible (incorrect) combination selectable by the user
        assertEquals("You have awarded less than 15 points, specified that the appeal is allowed and specified that Support Group Only Appeal does not apply, "
                + "but have answered No for the Regulation 29 question. Please review your previous selection.", result.getErrors().iterator().next());
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_LowPoints_WhenRegulation29SetToYes() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "REGULATION_29", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\","), Arrays.asList("2018-10-10", "refused", "No", "1c", "Yes", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_LowPoints_WhenSupportGroupOnlySetToYes() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "REGULATION_29", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\","), Arrays.asList("2018-10-10", "refused", "Yes", "1c", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_LowPoints_WhenSupportGroupOnlyNotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "\"supportGroupOnlyAppeal\" : \"SUPPORT_GROUP_ONLY\",", "ANSWER", "REGULATION_29", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\","), Arrays.asList("2018-10-10", "refused", "", "1c", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        // Assert we get a user-friendly error message, as this combination is a possible (incorrect) combination selectable by the user
        assertEquals("You have specified that the appeal is refused, but have a missing answer for the Support Group Only Appeal question. Please review your previous selection.", result.getErrors().iterator().next());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_LowPoints_WhenRegulation29NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\","), Arrays.asList("2018-10-10", "refused", "No", "1c", "", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_WhenHighPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "REGULATION_29", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\","), Arrays.asList("2018-10-10", "refused", "No", "1a", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario2() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "REGULATION_35", "SCHEDULE_3"), Arrays.asList("2018-10-10", "refused", "Yes", "", "No", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
        assertEquals(false, payload.isAllowed());
        assertEquals(false, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isEsaIsEntited());
        assertEquals("lower rate", payload.getEsaAwardRate());
        Assert.assertNull(payload.getEsaSchedule2Descriptors());
        assertNull(payload.getEsaNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_2, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario2_WhenIncorrectlyAllowed() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "REGULATION_35", "SCHEDULE_3"), Arrays.asList("2018-10-10", "allowed", "Yes", "", "No", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is allowed, specified that Support Group Only Appeal applies and made no selections for the Schedule 3 Activities question, but have answered No for the Regulation 35 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario2_WhenRegulation35NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3"), Arrays.asList("2018-10-10", "refused", "Yes", "", "", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario2_WhenSchedule3NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "REGULATION_35", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\","), Arrays.asList("2018-10-10", "refused", "Yes", "", "No", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario3() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "REGULATION_35", "SCHEDULE_3"), Arrays.asList("2018-10-10", "allowed", "Yes", "", "Yes", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
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
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isEsaIsEntited());
        assertEquals("higher rate", payload.getEsaAwardRate());
        Assert.assertNull(payload.getEsaSchedule2Descriptors());
        assertNull(payload.getEsaNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_3, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario3_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "REGULATION_35", "SCHEDULE_3"), Arrays.asList("2018-10-10", "refused", "Yes", "", "Yes", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have awarded less than 15 points, specified that the appeal is refused and specified that Support Group Only Appeal applies, but have answered Yes for the Regulation 35 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario3_WhenRegulation35NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3"), Arrays.asList("2018-10-10", "allowed", "Yes", "", "", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario3_WhenSchedule3NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "REGULATION_35", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\","), Arrays.asList("2018-10-10", "refused", "Yes", "", "Yes", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario4() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3"), Arrays.asList("2018-10-10", "allowed", "Yes", "", "", "Yes"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
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
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isEsaIsEntited());
        assertEquals("higher rate", payload.getEsaAwardRate());
        Assert.assertNull(payload.getEsaSchedule2Descriptors());
        assertNull(payload.getEsaNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_4, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario4_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3"), Arrays.asList("2018-10-10", "refused", "Yes", "", "", "Yes"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have awarded less than 15 points, specified that the appeal is refused and specified that Support Group Only Appeal applies, but have made selections for the Schedule 3 Activities question and a missing answer for the Regulation 35 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario4_WhenNoSchedule3() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3"), Arrays.asList("2018-10-10", "allowed", "Yes", "", "", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario5() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "No", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
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
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isEsaIsEntited());
        assertEquals("lower rate", payload.getEsaAwardRate());
        Assert.assertNotNull(payload.getEsaSchedule2Descriptors());
        assertEquals(1, payload.getEsaSchedule2Descriptors().size());
        assertEquals(15, payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getEsaNumberOfPoints());
        assertEquals(15, payload.getEsaNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_5, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario5_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "", "No", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Regulation 29 question, submitted an unexpected answer for the Schedule 3 Activities question and submitted an unexpected answer for the Regulation 35 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario5_Regulation35NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario5_LowPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario6() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "Yes", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
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
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isEsaIsEntited());
        assertEquals("higher rate", payload.getEsaAwardRate());
        Assert.assertNotNull(payload.getEsaSchedule2Descriptors());
        assertEquals(1, payload.getEsaSchedule2Descriptors().size());
        assertEquals(15, payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getEsaNumberOfPoints());
        assertEquals(15, payload.getEsaNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_6, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario6_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "", "", "Yes", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Regulation 29 question and submitted an unexpected answer for the Schedule 3 Activities question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario6_WhenSchedule3NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\",", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario6_WhenNotSchedule3() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario6_WhenLowPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "Yes", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario7() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "REGULATION_29", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "Yes", "No", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
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
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isEsaIsEntited());
        assertEquals("lower rate", payload.getEsaAwardRate());
        Assert.assertNotNull(payload.getEsaSchedule2Descriptors());
        assertEquals(1, payload.getEsaSchedule2Descriptors().size());
        assertEquals(9, payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getEsaNumberOfPoints());
        assertEquals(9, payload.getEsaNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_7, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario7_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "REGULATION_29", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "Yes", "No", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have answered Yes for the Regulation 29 question, submitted an unexpected answer for the Schedule 3 Activities question and submitted an unexpected answer for the Regulation 35 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario7_WhenRegulation35NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "REGULATION_29", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "Yes", "", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario7_WhenSchedule3NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "REGULATION_29", "REGULATION_35", "\"esaWriteFinalDecisionSchedule3ActivitiesApply\" : \"SCHEDULE_3\",", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "Yes", "No", "", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario7_WhenRegulation29IncorrectlyDoesNotApply() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "REGULATION_29", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "No", "No", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have awarded less than 15 points, specified that the appeal is allowed and specified that Support Group Only Appeal does not apply, but have answered No for the Regulation 29 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario8() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "REGULATION_29", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "Yes", "Yes", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
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
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isEsaIsEntited());
        assertEquals("higher rate", payload.getEsaAwardRate());
        Assert.assertNotNull(payload.getEsaSchedule2Descriptors());
        assertEquals(1, payload.getEsaSchedule2Descriptors().size());
        assertEquals(9, payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getEsaNumberOfPoints());
        assertEquals(9, payload.getEsaNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_8, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario8_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "REGULATION_29", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "Yes", "Yes", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have answered Yes for the Regulation 29 question, submitted an unexpected answer for the Schedule 3 Activities question and submitted an unexpected answer for the Regulation 35 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario9() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "REGULATION_29", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "Yes", "No", "Yes", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
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
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isEsaIsEntited());
        assertEquals("higher rate", payload.getEsaAwardRate());
        Assert.assertNotNull(payload.getEsaSchedule2Descriptors());
        assertEquals(1, payload.getEsaSchedule2Descriptors().size());
        assertEquals(9, payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getEsaNumberOfPoints());
        assertEquals(9, payload.getEsaNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_9, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario9_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "REGULATION_29", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "Yes", "No", "Yes", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have answered Yes for the Regulation 29 question and submitted an unexpected answer for the Schedule 3 Activities question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForEsaNonWcaScenario10Refused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESANonWCA.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "ANSWERED_QUESTIONS"), Arrays.asList("2018-10-10", "refused", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
        assertEquals(false, payload.isAllowed());
        assertEquals(false, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(false, payload.isEsaIsEntited());
        assertNull(payload.getEsaAwardRate());
        Assert.assertNull(payload.getEsaSchedule2Descriptors());
        Assert.assertNull(payload.getEsaNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertEquals("The appeal is refused.\n"
                + "\n"
                + "The decision made by the Secretary of State on 01/09/2018 is confirmed.\n"
                + "\n"
                + "This is my summary.\n"
                + "\n"
                + "My reasons for decision\n"
                + "\n"
                + "Something else.\n"
                + "\n"
                + "This has been an oral (face to face) hearing. AN Test the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"
                + "\n", parentPayload.getWriteFinalDecisionTemplateContent().toString());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_10, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForEsaNonWcaScenario10Allowed() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESANonWCA.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "ANSWERED_QUESTIONS"), Arrays.asList("2018-10-10", "allowed", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
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
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(false, payload.isEsaIsEntited());
        assertNull(payload.getEsaAwardRate());
        Assert.assertNull(payload.getEsaSchedule2Descriptors());
        Assert.assertNull(payload.getEsaNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertEquals("The appeal is allowed.\n"
                + "\n"
                + "The decision made by the Secretary of State on 01/09/2018 is set aside.\n"
                + "\n"
                + "This is my summary.\n"
                + "\n"
                + "My reasons for decision\n"
                + "\n"
                + "Something else.\n"
                + "\n"
                + "This has been an oral (face to face) hearing. AN Test the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"
                + "\n", parentPayload.getWriteFinalDecisionTemplateContent().toString());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_10, esaTemplateContent.getScenario());
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForEsaNonWca_WhenPreviousQuestionsAnsweredAndOver15Points() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESANonWCA.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "ANSWERED_QUESTIONS"), Arrays.asList("2018-10-10", "allowed", "mobilisingUnaided\", \"standingAndSitting"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
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
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(false, payload.isEsaIsEntited());
        assertNull(payload.getEsaAwardRate());
        Assert.assertNull(payload.getEsaSchedule2Descriptors());
        Assert.assertNull(payload.getEsaNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertEquals("The appeal is allowed.\n"
                + "\n"
                + "The decision made by the Secretary of State on 01/09/2018 is set aside.\n"
                + "\n"
                + "This is my summary.\n"
                + "\n"
                + "My reasons for decision\n"
                + "\n"
                + "Something else.\n"
                + "\n"
                + "This has been an oral (face to face) hearing. AN Test the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"
                + "\n", parentPayload.getWriteFinalDecisionTemplateContent().toString());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario12() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "Yes", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals(AN_Test, parentPayload.getAppellantFullName());
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
        assertEquals(AN_Test, payload.getAppellantName());
        assertEquals("2018-10-10", payload.getStartDate());
        assertEquals("2018-11-10", payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isEsaIsEntited());
        assertEquals("higher rate", payload.getEsaAwardRate());
        Assert.assertNotNull(payload.getEsaSchedule2Descriptors());
        assertEquals(1, payload.getEsaSchedule2Descriptors().size());
        assertEquals(15, payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getEsaNumberOfPoints());
        assertEquals(15, payload.getEsaNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        EsaTemplateContent esaTemplateContent = (EsaTemplateContent) parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(EsaScenario.SCENARIO_12, esaTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario12_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "", "Yes", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Regulation 29 question, submitted an unexpected answer for the Schedule 3 Activities question and submitted an unexpected answer for the Regulation 35 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario12_WhenRegulation35NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "\"doesRegulation35Apply\" : \"REGULATION_35\",", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario12_WhenLowPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorESAScenario.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesRegulation29Apply\" : \"REGULATION_29\",", "REGULATION_35", "SCHEDULE_3", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "Yes", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userInfo.getGivenName()).thenReturn("Judge");
        when(userInfo.getFamilyName()).thenReturn("Full Name");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

}

