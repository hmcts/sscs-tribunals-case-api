package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceTestBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcWriteFinalDecisionPreviewDecisionServiceTest extends WriteFinalDecisionPreviewDecisionServiceTestBase {

    public UcWriteFinalDecisionPreviewDecisionServiceTest() {
        super("UC");
    }

    @Override
    protected WriteFinalDecisionPreviewDecisionServiceBase createPreviewDecisionService(GenerateFile generateFile, IdamClient idamClient,
        DocumentConfiguration documentConfiguration) {
        return new UcWriteFinalDecisionPreviewDecisionService(generateFile, idamClient, ucDecisionNoticeQuestionService, ucDecisionNoticeOutcomeService, documentConfiguration);
    }

    @Test
    public void willSetPreviewFile_WhenRefusedAndNoAward() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1c");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = false;

        boolean setAsideExpectation = false;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("no award", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertNull(body.getUcSchedule7Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(9, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(9, body.getUcNumberOfPoints().intValue());


        assertFalse(body.isUcIsEntited());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_1, templateContent.getScenario());

        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenRefusedSupportGroupOnlyAppealAndLowerRateAward_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1c");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = false;

        boolean setAsideExpectation = false;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("lower rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertNull(body.getUcSchedule7Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(9, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(9, body.getUcNumberOfPoints().intValue());

        assertTrue(body.isUcIsEntited());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_2, templateContent.getScenario());

        assertEquals(5, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenRefusedSupportGroupOnlyAppealAndLowerRateAward_WhenHighPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = false;

        boolean setAsideExpectation = false;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("lower rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertNull(body.getUcSchedule7Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(15, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(15, body.getUcNumberOfPoints().intValue());

        assertTrue(body.isUcIsEntited());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_2, templateContent.getScenario());

        assertEquals(5, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenRefusedAndNonWcaAppeal() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWcaAppeal("No");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsUcCaseData().setDwpReassessTheAward("noRecommendation");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = false;

        boolean setAsideExpectation = false;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, false, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertFalse(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertNull(body.getUcAwardRate());

        assertNull(body.getUcSchedule6Descriptors());
        assertNull(body.getUcSchedule7Descriptors());
        assertNull(body.getUcNumberOfPoints());

        assertFalse(body.isUcIsEntited());
        assertEquals("noRecommendation", body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_10, templateContent.getScenario());

        assertEquals(7, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndNonWcaAppeal() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWcaAppeal("No");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsUcCaseData().setDwpReassessTheAward("noRecommendation");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, false, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertFalse(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertNull(body.getUcAwardRate());

        assertNull(body.getUcSchedule6Descriptors());
        assertNull(body.getUcSchedule7Descriptors());
        assertNull(body.getUcNumberOfPoints());

        assertFalse(body.isUcIsEntited());
        assertEquals("noRecommendation", body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_10, templateContent.getScenario());

        assertEquals(7, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndLowerRateNoSchedule7() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("lower rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(15, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(15, body.getUcNumberOfPoints().intValue());

        assertTrue(body.isUcIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_5, templateContent.getScenario());
        assertEquals(9, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndHigherRateNoSchedule7() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.YES);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(15, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(15, body.getUcNumberOfPoints().intValue());

        assertTrue(body.isUcIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_5, templateContent.getScenario());
        assertEquals(9, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndHigherRateSchedule7() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("schedule7MobilisingUnaided", "schedule7AppropriatenessOfBehaviour"));

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(15, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(15, body.getUcNumberOfPoints().intValue());

        assertNotNull(body.getUcSchedule7Descriptors());
        assertFalse(body.getUcSchedule7Descriptors().isEmpty());
        assertEquals(2, body.getUcSchedule7Descriptors().size());
        assertNotNull(body.getUcSchedule7Descriptors().get(0));
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally "
            + "or could reasonably be worn or used. Cannot either: (a) mobilise more than 50 metres on level ground without stopping in order to avoid significant "
            + "discomfort or exhaustion; or (b) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.",
            body.getUcSchedule7Descriptors().get(0).getActivityQuestionValue());

        assertEquals("1", body.getUcSchedule7Descriptors().get(0).getActivityQuestionNumber());
        assertNull(body.getUcSchedule7Descriptors().get(0).getActivityAnswerLetter());
        assertNull(body.getUcSchedule7Descriptors().get(0).getActivityAnswerValue());
        assertEquals(0, body.getUcSchedule7Descriptors().get(0).getActivityAnswerPoints());

        assertNotNull(body.getUcSchedule7Descriptors().get(1));
        assertEquals("14. Appropriateness of behaviour with other people, due to cognitive impairment or mental disorder. Has, on a daily basis, uncontrollable episodes of aggressive or disinhibited behaviour that would be unreasonable in any workplace.",
            body.getUcSchedule7Descriptors().get(1).getActivityQuestionValue());

        assertEquals("14", body.getUcSchedule7Descriptors().get(1).getActivityQuestionNumber());
        assertNull(body.getUcSchedule7Descriptors().get(1).getActivityAnswerLetter());
        assertNull(body.getUcSchedule7Descriptors().get(1).getActivityAnswerValue());
        assertEquals(0, body.getUcSchedule7Descriptors().get(1).getActivityAnswerPoints());

        assertTrue(body.isUcIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());


        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_6, templateContent.getScenario());
        assertEquals(10, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }



    @Override
    protected void setDescriptorFlowIndicator(String value, SscsCaseData sscsCaseData) {
        if ("no".equalsIgnoreCase(value)) {
            sscsCaseData.getSscsUcCaseData().setDwpReassessTheAward("doNotReassess");
        }
        sscsCaseData.setWcaAppeal(value);
    }

    @Override
    protected boolean getDescriptorFlowIndicator(WriteFinalDecisionTemplateBody body) {
        return body.isWcaAppeal();
    }

    @Override
    protected void setHigherRateScenarioFields(SscsCaseData caseData) {
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");
        caseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        caseData.setDoesSchedule9Paragraph4Apply(YesNo.YES);
    }

    @Override
    public void givenGeneratedDateIsAlreadySetGeneratedNonDescriptorFlow_thenSetNewGeneratedDate() {
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");
        setDescriptorFlowIndicator("no", sscsCaseData);

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true, true, true, false, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Override
    public void givenGeneratedDateIsAlreadySetNonGeneratedDescriptorFlow_thenDoSetNewGeneratedDate() {
        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("no");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true, true, true,
            true, false, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Override
    public void givenGeneratedDateIsAlreadySetNonGeneratedNonDescriptorFlow_thenDoSetNewGeneratedDate() {
        setDescriptorFlowIndicator("no", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("no");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",  true, true, true,
            false, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndSupportGroupOnlyNoSchedule7_WhenHighPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(15, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(15, body.getUcNumberOfPoints().intValue());

        assertTrue(body.isUcIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_3, templateContent.getScenario());
        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndSupportGroupOnlySchedule7_WhenHighPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("schedule7MobilisingUnaided", "schedule7AppropriatenessOfBehaviour"));

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(15, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(15, body.getUcNumberOfPoints().intValue());

        assertTrue(body.isUcIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_4, templateContent.getScenario());
        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndSupportGroupOnlyNoSchedule7_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.YES);
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must apply
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(0, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("e", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("None of the above applies.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(0, body.getUcNumberOfPoints().intValue());

        assertTrue(body.isUcIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_3, templateContent.getScenario());
        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndSupportGroupOnlySchedule7_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("schedule7MobilisingUnaided", "schedule7AppropriatenessOfBehaviour"));
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must apply
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(0, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("e", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("None of the above applies.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(0, body.getUcNumberOfPoints().intValue());

        assertTrue(body.isUcIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_4, templateContent.getScenario());
        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedNotSupportGroupOnlyNoSchedule7NoSch9Para4_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must apply
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("lower rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(0, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("e", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("None of the above applies.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(0, body.getUcNumberOfPoints().intValue());

        assertTrue(body.isUcIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_7, templateContent.getScenario());
        assertEquals(11, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedNotSupportGroupOnlyNoSchedule7WithSch9Para4_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must apply
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(0, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("e", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("None of the above applies.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(0, body.getUcNumberOfPoints().intValue());

        assertTrue(body.isUcIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_8, templateContent.getScenario());
        assertEquals(10, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedNotSupportGroupOnlyWithSchedule7_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("schedule7MobilisingUnaided", "schedule7AppropriatenessOfBehaviour"));
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must apply
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("uc").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getUcAwardRate());

        assertNotNull(body.getUcSchedule6Descriptors());
        assertEquals(1, body.getUcSchedule6Descriptors().size());
        assertNotNull(body.getUcSchedule6Descriptors().get(0));
        assertEquals(0, body.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("e", body.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("None of the above applies.", body.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getUcNumberOfPoints());
        assertEquals(0, body.getUcNumberOfPoints().intValue());

        assertTrue(body.isUcIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof UcTemplateContent);
        UcTemplateContent templateContent = (UcTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(UcScenario.SCENARIO_9, templateContent.getScenario());
        assertEquals(11, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    @Ignore
    public void givenWelsh_GeneratedDateIsAlreadySet_thenDoNotSetNewGeneratedDate() {
        // Ignoring this test until Welsh is implemented
    }
}
