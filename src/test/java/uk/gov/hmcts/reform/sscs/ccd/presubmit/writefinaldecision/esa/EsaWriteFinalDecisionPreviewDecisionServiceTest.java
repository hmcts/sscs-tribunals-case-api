package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.junit.Assert.*;
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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios.EsaScenario;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class EsaWriteFinalDecisionPreviewDecisionServiceTest extends WriteFinalDecisionPreviewDecisionServiceTestBase {

    public EsaWriteFinalDecisionPreviewDecisionServiceTest() {
        super("ESA");
    }

    @Override
    protected WriteFinalDecisionPreviewDecisionServiceBase createPreviewDecisionService(GenerateFile generateFile, IdamClient idamClient,
        DocumentConfiguration documentConfiguration) {
        return new EsaWriteFinalDecisionPreviewDecisionService(generateFile, idamClient, esaDecisionNoticeQuestionService, esaDecisionNoticeOutcomeService, documentConfiguration);
    }

    @Test
    public void willSetPreviewFile_WhenRefusedAndNoAward() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1c");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("no award", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertNull(body.getEsaSchedule3Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(9, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(9, body.getEsaNumberOfPoints().intValue());


        assertFalse(body.isEsaIsEntited());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_1, templateContent.getScenario());

        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenRefusedAndNoAward_WhenZeroPointsAndNoSchedule2Apply() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList());

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("no award", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertNull(body.getEsaSchedule3Descriptors());
        assertEquals(0, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(0, body.getEsaNumberOfPoints().intValue());


        assertFalse(body.isEsaIsEntited());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_1, templateContent.getScenario());

        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenRefusedSupportGroupOnlyAppealAndLowerRateAward_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1c");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("lower rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertNull(body.getEsaSchedule3Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(9, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(9, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_2, templateContent.getScenario());

        assertEquals(5, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenRefusedSupportGroupOnlyAppealAndLowerRateAward_WhenHighPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("lower rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertNull(body.getEsaSchedule3Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(15, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(15, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_2, templateContent.getScenario());

        assertEquals(5, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenRefusedAndNonWcaAppeal() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWcaAppeal("No");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsEsaCaseData().setDwpReassessTheAward("noRecommendation");

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
            appealAllowedExpectation, setAsideExpectation, true, false, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertFalse(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertNull(body.getEsaAwardRate());

        assertNull(body.getEsaSchedule2Descriptors());
        assertNull(body.getEsaSchedule3Descriptors());
        assertNull(body.getEsaNumberOfPoints());

        assertFalse(body.isEsaIsEntited());
        assertEquals("noRecommendation", body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_10, templateContent.getScenario());

        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndNonWcaAppeal() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWcaAppeal("No");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsEsaCaseData().setDwpReassessTheAward("noRecommendation");

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
            appealAllowedExpectation, setAsideExpectation, true, false, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertFalse(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertNull(body.getEsaAwardRate());

        assertNull(body.getEsaSchedule2Descriptors());
        assertNull(body.getEsaSchedule3Descriptors());
        assertNull(body.getEsaNumberOfPoints());

        assertFalse(body.isEsaIsEntited());
        assertEquals("noRecommendation", body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_10, templateContent.getScenario());

        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndLowerRateNoSchedule3() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("lower rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(15, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(15, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_5, templateContent.getScenario());
        assertEquals(9, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndHigherRateNoSchedule3() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(15, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(15, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_5, templateContent.getScenario());
        assertEquals(9, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndHigherRateSchedule3() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("schedule3MobilisingUnaided", "schedule3AppropriatenessOfBehaviour"));

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(15, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(15, body.getEsaNumberOfPoints().intValue());

        assertNotNull(body.getEsaSchedule3Descriptors());
        assertFalse(body.getEsaSchedule3Descriptors().isEmpty());
        assertEquals(2, body.getEsaSchedule3Descriptors().size());
        assertNotNull(body.getEsaSchedule3Descriptors().get(0));
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally "
            + "or could reasonably be worn or used. Cannot either: (a) mobilise more than 50 metres on level ground without stopping in order to avoid significant "
            + "discomfort or exhaustion; or (b) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.",
            body.getEsaSchedule3Descriptors().get(0).getActivityQuestionValue());

        assertEquals("1", body.getEsaSchedule3Descriptors().get(0).getActivityQuestionNumber());
        assertNull(body.getEsaSchedule3Descriptors().get(0).getActivityAnswerLetter());
        assertNull(body.getEsaSchedule3Descriptors().get(0).getActivityAnswerValue());
        assertEquals(0, body.getEsaSchedule3Descriptors().get(0).getActivityAnswerPoints());

        assertNotNull(body.getEsaSchedule3Descriptors().get(1));
        assertEquals("14. Appropriateness of behaviour with other people, due to cognitive impairment or mental disorder. Has, on a daily basis, uncontrollable episodes of aggressive or disinhibited behaviour that would be unreasonable in any workplace.",
            body.getEsaSchedule3Descriptors().get(1).getActivityQuestionValue());

        assertEquals("14", body.getEsaSchedule3Descriptors().get(1).getActivityQuestionNumber());
        assertNull(body.getEsaSchedule3Descriptors().get(1).getActivityAnswerLetter());
        assertNull(body.getEsaSchedule3Descriptors().get(1).getActivityAnswerValue());
        assertEquals(0, body.getEsaSchedule3Descriptors().get(1).getActivityAnswerPoints());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());


        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_6, templateContent.getScenario());
        assertEquals(10, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }



    @Override
    protected void setDescriptorFlowIndicator(String value, SscsCaseData sscsCaseData) {
        if ("no".equalsIgnoreCase(value)) {
            sscsCaseData.getSscsEsaCaseData().setDwpReassessTheAward("doNotReassess");
        }
        sscsCaseData.setWcaAppeal(value);
    }

    @Override
    protected boolean getDescriptorFlowIndicator(WriteFinalDecisionTemplateBody body) {
        return body.isWcaAppeal();
    }

    @Override
    protected void setHigherRateScenarioFields(SscsCaseData caseData) {
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");
        caseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>());
        caseData.setDoesRegulation35Apply(YesNo.YES);
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
    public void willSetPreviewFile_WhenAllowedAndSupportGroupOnlyNoSchedule3_WhenHighPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(15, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(15, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_3, templateContent.getScenario());
        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndSupportGroupOnlySchedule3_WhenHighPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("schedule3MobilisingUnaided", "schedule3AppropriatenessOfBehaviour"));

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means regulation 29 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(15, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(15, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_4, templateContent.getScenario());
        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndSupportGroupOnlyNoSchedule3_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must apply
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(0, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("e", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("None of the above applies.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(0, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_3, templateContent.getScenario());
        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedAndSupportGroupOnlySchedule3_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("schedule3MobilisingUnaided", "schedule3AppropriatenessOfBehaviour"));
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must apply
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(0, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("e", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("None of the above applies.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(0, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_4, templateContent.getScenario());
        assertEquals(8, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedNotSupportGroupOnlyNoSchedule3NoReg35_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must apply
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("lower rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(0, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("e", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("None of the above applies.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(0, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_7, templateContent.getScenario());
        assertEquals(11, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedNotSupportGroupOnlyNoSchedule3NoReg35_WhenZeroPointsAndNoSchedule2() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setDoesRegulation35Apply(YesNo.NO);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList());

        // 0 points awarded as no schedule 2 apply - low, which means regulation 29 must apply
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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("lower rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(0, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(0, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_7, templateContent.getScenario());
        assertEquals(11, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedNotSupportGroupOnlyNoSchedule3WithReg35_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must apply
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(0, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("e", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("None of the above applies.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(0, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_8, templateContent.getScenario());
        assertEquals(10, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedNotSupportGroupOnlyNoSchedule3WithReg35_WhenZeroPointsAndNoSchedule2() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("No");
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setDoesRegulation35Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList());

        // 0 points awarded as no schedule 2- low, which means regulation 29 must apply

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(0, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(0, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_8, templateContent.getScenario());
        assertEquals(10, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    public void willSetPreviewFile_WhenAllowedNotSupportGroupOnlyWithSchedule3_WhenLowPoints() {

        String endDate = "2018-11-10";
        setCommonPreviewParams(sscsCaseData, endDate);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList("schedule3MobilisingUnaided", "schedule3AppropriatenessOfBehaviour"));
        sscsCaseData.setDoesRegulation29Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means regulation 29 must apply
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

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
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get("esa").get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertTrue(payload.getWriteFinalDecisionTemplateBody().isWcaAppeal());

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertEquals("higher rate", body.getEsaAwardRate());

        assertNotNull(body.getEsaSchedule2Descriptors());
        assertEquals(1, body.getEsaSchedule2Descriptors().size());
        assertNotNull(body.getEsaSchedule2Descriptors().get(0));
        assertEquals(0, body.getEsaSchedule2Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("e", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("None of the above applies.", body.getEsaSchedule2Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", body.getEsaSchedule2Descriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getEsaNumberOfPoints());
        assertEquals(0, body.getEsaNumberOfPoints().intValue());

        assertTrue(body.isEsaIsEntited());
        assertNull(body.getDwpReassessTheAward());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

        assertNotNull(payload.getWriteFinalDecisionTemplateContent());
        assertTrue(payload.getWriteFinalDecisionTemplateContent() instanceof EsaTemplateContent);
        EsaTemplateContent templateContent = (EsaTemplateContent)payload.getWriteFinalDecisionTemplateContent();
        assertEquals(EsaScenario.SCENARIO_9, templateContent.getScenario());
        assertEquals(11, payload.getWriteFinalDecisionTemplateContent().getComponents().size());
    }

    @Test
    @Ignore
    public void givenWelsh_GeneratedDateIsAlreadySet_thenDoNotSetNewGeneratedDate() {
        // Ignoring this test until Welsh is implemented
    }
}
