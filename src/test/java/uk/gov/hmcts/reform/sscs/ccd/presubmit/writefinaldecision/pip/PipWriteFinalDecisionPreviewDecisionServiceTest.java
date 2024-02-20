package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceTestBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios.PipScenario;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;

public class PipWriteFinalDecisionPreviewDecisionServiceTest extends WriteFinalDecisionPreviewDecisionServiceTestBase {

    protected PipDecisionNoticeOutcomeService pipDecisionNoticeOutcomeService;
    protected PipDecisionNoticeQuestionService pipDecisionNoticeQuestionService;
    protected VenueDataLoader venueDataLoader;

    public PipWriteFinalDecisionPreviewDecisionServiceTest() throws IOException {
        super("PIP");
        this.pipDecisionNoticeQuestionService = new PipDecisionNoticeQuestionService();
        this.pipDecisionNoticeOutcomeService = new PipDecisionNoticeOutcomeService(pipDecisionNoticeQuestionService);
        this.venueDataLoader = new VenueDataLoader();
    }

    @Override
    protected WriteFinalDecisionPreviewDecisionServiceBase createPreviewDecisionService(GenerateFile generateFile, UserDetailsService userDetailsService,
        DocumentConfiguration documentConfiguration) {
        return new PipWriteFinalDecisionPreviewDecisionService(generateFile, userDetailsService, pipDecisionNoticeQuestionService,
            pipDecisionNoticeOutcomeService, documentConfiguration, venueDataLoader);
    }

    @NamedParameters("previewEndDateAndRateCombinations")
    @SuppressWarnings("unused")
    private Object[] previewEndDateAndRateCombinations() {
        return new Object[] {
            new Object[] {"2018-11-10", "standardRate", "lower", "lower"},
            new Object[] {"2018-11-10", "standardRate", "same", "lower"},
            new Object[] {"2018-11-10", "standardRate", "higher", "lower"},
            new Object[] {"2018-11-10", "standardRate", "lower", "same"},
            new Object[] {"2018-11-10", "standardRate", "same", "same"},
            new Object[] {"2018-11-10", "standardRate", "higher", "same"},
            new Object[] {"2018-11-10", "enhancedRate", "same", "lower"},
            new Object[] {"2018-11-10", "enhancedRate", "higher", "lower"},
            new Object[] {"2018-11-10", "enhancedRate", "same", "same"},
            new Object[] {"2018-11-10", "enhancedRate", "higher", "same"},
            new Object[] {"2018-11-10", "noAward", "lower", "lower"},
            new Object[] {"2018-11-10", "noAward", "same", "lower"},
            new Object[] {"2018-11-10", "noAward", "lower", "same"},
            new Object[] {"2018-11-10", "noAward", "same", "same"},
            new Object[] {"2018-11-10", "notConsidered", "lower", "lower"},
            new Object[] {"2018-11-10", "notConsidered", "same", "lower"},
            new Object[] {"2018-11-10", "notConsidered", "higher", "lower"},
            new Object[] {"2018-11-10", "notConsidered", "lower", "same"},
            new Object[] {"2018-11-10", "notConsidered", "same", "same"},
            new Object[] {"2018-11-10", "notConsidered", "higher", "same"},
            new Object[] {null, "standardRate", "lower", "lower"},
            new Object[] {null, "standardRate", "same", "lower"},
            new Object[] {null, "standardRate", "higher", "lower"},
            new Object[] {null, "standardRate", "lower", "same"},
            new Object[] {null, "standardRate", "same", "same"},
            new Object[] {null, "standardRate", "higher", "same"},
            new Object[] {null, "enhancedRate", "same", "lower"},
            new Object[] {null, "enhancedRate", "higher", "lower"},
            new Object[] {null, "enhancedRate", "same", "same"},
            new Object[] {null, "enhancedRate", "higher", "same"},
            new Object[] {null, "noAward", "lower", "lower"},
            new Object[] {null, "noAward", "same", "lower"},
            new Object[] {null, "noAward", "lower", "same"},
            new Object[] {null, "noAward", "same", "same"},
            new Object[] {null, "notConsidered", "lower", "lower"},
            new Object[] {null, "notConsidered", "same", "lower"},
            new Object[] {null, "notConsidered", "higher", "lower"},
            new Object[] {null, "notConsidered", "lower", "same"},
            new Object[] {null, "notConsidered", "same", "same"},
            new Object[] {null, "notConsidered", "higher", "same"},
        };
    }

    @Override
    protected Map<LanguagePreference, Map<EventType, String>> getBenefitSpecificDocuments() {
        final Map<EventType, String> englishEventTypeDocs = new HashMap<>();
        final Map<EventType, String> welshEventTypeDocs = new HashMap<>();
        welshEventTypeDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-ENG-00485");
        englishEventTypeDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-ENG-00483.docx");
        Map<LanguagePreference, Map<EventType, String>> docs = new HashMap<>();
        docs.put(LanguagePreference.ENGLISH, englishEventTypeDocs);
        docs.put(LanguagePreference.WELSH, welshEventTypeDocs);
        return docs;
    }

    @Override
    protected void setDescriptorFlowIndicator(String value, SscsCaseData sscsCaseData) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow(value);
    }

    @Override
    protected boolean getDescriptorFlowIndicator(WriteFinalDecisionTemplateBody body) {
        return body.isDescriptorFlow();
    }

    @Override
    protected void setHigherRateScenarioFields(SscsCaseData caseData) {
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("enhancedRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("enhancedRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionStartDate("2018-10-11");
    }

    @Override
    public void givenGeneratedDateIsAlreadySetGeneratedNonDescriptorFlow_thenSetNewGeneratedDate() {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGeneratedDate("2018-10-10");
        setDescriptorFlowIndicator("no", sscsCaseData);

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true, true, true, false, true,
            documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Override
    public void givenGeneratedDateIsAlreadySetNonGeneratedDescriptorFlow_thenDoSetNewGeneratedDate() {
        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(NO);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true, true, true,
            true, false, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Override
    public void givenGeneratedDateIsAlreadySetNonGeneratedNonDescriptorFlow_thenDoSetNewGeneratedDate() {
        setDescriptorFlowIndicator("no", sscsCaseData);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(NO);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true, true, true,
            false, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Override
    protected boolean isDescriptorFlowSupported() {
        return true;
    }

    @Test
    public void willSetPreviewFileForDailyLivingMobility_whenNotGeneratingNotice() {

        setCommonPreviewParams(sscsCaseData, null);

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(NO);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;
        boolean setAsideExpectation = appealAllowedExpectation;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, false, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(
                EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, null, false);

        assertNull(body.getMobilityAwardRate());
        assertFalse(body.isMobilityIsSeverelyLimited());
        assertFalse(body.isMobilityIsEntited());
        assertNull(body.getDailyLivingAwardRate());
        assertFalse(body.isDailyLivingIsSeverelyLimited());
        assertFalse(body.isDailyLivingIsEntited());
        assertNull(body.getMobilityDescriptors());
        assertNull(body.getMobilityNumberOfPoints());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(body.getDailyLivingNumberOfPoints());

        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    @Test
    public void willSetPreviewFileForNotDailyLivingMobility_whenNotGeneratingNotice() {

        setCommonPreviewParams(sscsCaseData, null);

        setDescriptorFlowIndicator("no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(NO);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;
        boolean setAsideExpectation = appealAllowedExpectation;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, false, false,
            documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, null, false);

        assertNull(body.getMobilityAwardRate());
        assertFalse(body.isMobilityIsSeverelyLimited());
        assertFalse(body.isMobilityIsEntited());
        assertNull(body.getDailyLivingAwardRate());
        assertFalse(body.isDailyLivingIsSeverelyLimited());
        assertFalse(body.isDailyLivingIsEntited());
        assertNull(body.getMobilityDescriptors());
        assertNull(body.getMobilityNumberOfPoints());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(body.getDailyLivingNumberOfPoints());

        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    @Test
    @Parameters(named = "previewEndDateAndRateCombinations")
    public void willSetPreviewFile_whenMobilityDescriptorsOnly_ForEndDateAndRate(String endDate, String rate, String descriptorsComparedToDwp,
        String nonDescriptorsComparedWithDwp) {

        setCommonPreviewParams(sscsCaseData, endDate);

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(descriptorsComparedToDwp);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(nonDescriptorsComparedWithDwp);

        if ("noAward".equals(rate) || "notConsidered".equals(rate)) {
            sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");
        }

        // Mobility specific parameters
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(rate);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(Arrays.asList("movingAround"));
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        if ("notConsidered".equals(rate)) {
            assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
            Assert.assertEquals(1, response.getErrors().size());
        } else {

            assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
            assertEquals(DocumentLink.builder()
                .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .documentBinaryUrl(URL + "/binary")
                .documentUrl(URL)
                .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

            boolean appealAllowedExpectation = !"notConsidered".equalsIgnoreCase(rate) && "higher".equals(descriptorsComparedToDwp);

            boolean setAsideExpectation = getConsideredComparissons(rate, "notConsidered", descriptorsComparedToDwp, nonDescriptorsComparedWithDwp).stream().anyMatch(comparission ->
                !"same".equalsIgnoreCase(comparission));

            NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10",
                appealAllowedExpectation, setAsideExpectation, true, true, true,
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

            assertEquals("Judge Full Name", payload.getUserName());
            assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

            WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

            assertNotNull(body);

            // Common assertions
            assertCommonPreviewParams(body, endDate, false);

            // Mobility specific assertions
            if ("standardRate".equals(rate)) {
                assertEquals("standard rate", body.getMobilityAwardRate());
                assertEquals(false, body.isMobilityIsSeverelyLimited());
                assertEquals(true, body.isMobilityIsEntited());
            } else if ("enhancedRate".equals(rate)) {
                assertEquals("enhanced rate", body.getMobilityAwardRate());
                assertEquals(true, body.isMobilityIsSeverelyLimited());
                assertEquals(true, body.isMobilityIsEntited());

            } else {
                assertEquals(false, body.isMobilityIsEntited());
            }

            assertNotNull(body.getMobilityDescriptors());
            assertEquals(1, body.getMobilityDescriptors().size());
            assertNotNull(body.getMobilityDescriptors().get(0));
            assertEquals(10, body.getMobilityDescriptors().get(0).getActivityAnswerPoints());
            assertEquals("d", body.getMobilityDescriptors().get(0).getActivityAnswerLetter());
            assertEquals("Can stand and then move using an aid or appliance more than 20 metres but no more than 50 metres.", body.getMobilityDescriptors().get(0).getActivityAnswerValue());
            assertEquals("12. Moving around", body.getMobilityDescriptors().get(0).getActivityQuestionValue());
            assertEquals("12", body.getMobilityDescriptors().get(0).getActivityQuestionNumber());
            assertNotNull(body.getMobilityNumberOfPoints());
            assertEquals(10, body.getMobilityNumberOfPoints().intValue());

            // Daily living specific assertions
            assertEquals(false, body.isDailyLivingIsEntited());
            assertEquals(false, body.isDailyLivingIsSeverelyLimited());
            assertNull(body.getDailyLivingDescriptors());
            assertNull(payload.getDateIssued());
            assertEquals(LocalDate.now(), payload.getGeneratedDate());
            assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());

        }
    }

    @Test
    @Parameters(named = "previewEndDateAndRateCombinations")
    public void willSetPreviewFile_whenDailyLivingDescriptorsOnly_ForEndDateAndRate(String endDate, String rate, String descriptorsComparedToDwp,
        String nonDescriptorsComparedWithDwp) {

        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(descriptorsComparedToDwp);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(nonDescriptorsComparedWithDwp);

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        // Daily living specific params
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(rate);

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("notConsidered");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(Arrays.asList("preparingFood"));
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        if ("notConsidered".equals(rate)) {
            assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
            Assert.assertEquals(1, response.getErrors().size());
        } else {

            assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
            assertEquals(DocumentLink.builder()
                .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .documentBinaryUrl(URL + "/binary")
                .documentUrl(URL)
                .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

            boolean appealAllowedExpectation = !"notConsidered".equalsIgnoreCase(rate) && "higher".equals(descriptorsComparedToDwp);

            boolean setAsideExpectation = getConsideredComparissons(rate, "notConsidered", descriptorsComparedToDwp, nonDescriptorsComparedWithDwp).stream().anyMatch(comparission ->
                !"same".equalsIgnoreCase(comparission));

            NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10",
                appealAllowedExpectation, setAsideExpectation, true, true, true,
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

            assertEquals("Judge Full Name", payload.getUserName());
            assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

            WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

            assertNotNull(body);

            // Common assertions
            assertCommonPreviewParams(body, endDate, false);

            // Daily living specific assertions
            if ("standardRate".equals(rate)) {
                assertEquals("standard rate", body.getDailyLivingAwardRate());
                assertEquals(false, body.isDailyLivingIsSeverelyLimited());
                assertEquals(true, body.isDailyLivingIsEntited());

            } else if ("enhancedRate".equals(rate)) {
                assertEquals("enhanced rate", body.getDailyLivingAwardRate());
                assertEquals(true, body.isDailyLivingIsSeverelyLimited());
                assertEquals(true, body.isDailyLivingIsEntited());

            } else {
                assertEquals(false, body.isDailyLivingIsEntited());
            }

            assertNotNull(body.getDailyLivingDescriptors());
            assertEquals(1, body.getDailyLivingDescriptors().size());
            assertNotNull(body.getDailyLivingDescriptors().get(0));
            assertEquals(8, body.getDailyLivingDescriptors().get(0).getActivityAnswerPoints());
            assertEquals("f", body.getDailyLivingDescriptors().get(0).getActivityAnswerLetter());
            assertEquals("Cannot prepare and cook food.", body.getDailyLivingDescriptors().get(0).getActivityAnswerValue());
            assertEquals("1. Preparing food", body.getDailyLivingDescriptors().get(0).getActivityQuestionValue());
            assertEquals("1", body.getDailyLivingDescriptors().get(0).getActivityQuestionNumber());
            assertNotNull(body.getDailyLivingNumberOfPoints());
            assertEquals(8, body.getDailyLivingNumberOfPoints().intValue());
            PipTemplateContent templateContent = (PipTemplateContent) payload.getWriteFinalDecisionTemplateContent();
            if ("noAward".equals(rate)) {
                assertEquals(PipScenario.SCENARIO_NO_AWARD_NOT_CONSIDERED, templateContent.getScenario());
            } else {
                assertEquals(PipScenario.SCENARIO_AWARD_NOT_CONSIDERED, templateContent.getScenario());
            }

            // Mobility specific assertions
            assertEquals(false, body.isMobilityIsEntited());
            assertEquals(false, body.isMobilityIsSeverelyLimited());
            assertNull(body.getMobilityDescriptors());
        }
    }

    @Test
    @Parameters(named = "previewEndDateAndRateCombinations")
    public void willSetPreviewFile_whenMobilityDescriptorsOnly_ForEndDateAndRateForIssueDecision(String endDate, String rate, String descriptorsComparedToDwp,
        String nonDescriptorsComparedWithDwp) {

        setCommonPreviewParams(sscsCaseData, endDate);

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(descriptorsComparedToDwp);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(nonDescriptorsComparedWithDwp);

        // Mobility specific parameters
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(rate);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(Arrays.asList("movingAround"));
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, false);

        if ("notConsidered".equals(rate)) {
            assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
            Assert.assertEquals(1, response.getErrors().size());
        } else {

            assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
            assertEquals(DocumentLink.builder()
                .documentFilename(String.format("Final Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .documentBinaryUrl(URL + "/binary")
                .documentUrl(URL)
                .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

            boolean appealAllowedExpectation = !"notConsidered".equalsIgnoreCase(rate) && "higher".equals(descriptorsComparedToDwp);

            boolean setAsideExpectation = getConsideredComparissons(rate, "notConsidered", descriptorsComparedToDwp, nonDescriptorsComparedWithDwp).stream().anyMatch(comparission ->
                !"same".equalsIgnoreCase(comparission));

            NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10",
                appealAllowedExpectation, setAsideExpectation, false,
                true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

            assertEquals("Judge Full Name", payload.getUserName());
            assertEquals("DECISION NOTICE", payload.getNoticeType());

            WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

            assertNotNull(body);

            // Common assertions
            assertCommonPreviewParams(body, endDate, false);

            // Mobility specific assertions
            if ("standardRate".equals(rate)) {
                assertEquals("standard rate", body.getMobilityAwardRate());
                assertEquals(false, body.isMobilityIsSeverelyLimited());
                assertEquals(true, body.isMobilityIsEntited());
            } else if ("enhancedRate".equals(rate)) {
                assertEquals("enhanced rate", body.getMobilityAwardRate());
                assertEquals(true, body.isMobilityIsSeverelyLimited());
                assertEquals(true, body.isMobilityIsEntited());

            } else {
                assertEquals(false, body.isMobilityIsEntited());
            }

            assertNotNull(body.getMobilityDescriptors());
            assertEquals(1, body.getMobilityDescriptors().size());
            assertNotNull(body.getMobilityDescriptors().get(0));
            assertEquals(10, body.getMobilityDescriptors().get(0).getActivityAnswerPoints());
            assertEquals("d", body.getMobilityDescriptors().get(0).getActivityAnswerLetter());
            assertEquals("Can stand and then move using an aid or appliance more than 20 metres but no more than 50 metres.", body.getMobilityDescriptors().get(0).getActivityAnswerValue());
            assertEquals("12. Moving around", body.getMobilityDescriptors().get(0).getActivityQuestionValue());
            assertEquals("12", body.getMobilityDescriptors().get(0).getActivityQuestionNumber());
            assertNotNull(body.getMobilityNumberOfPoints());
            assertEquals(10, body.getMobilityNumberOfPoints().intValue());

            // Daily living specific assertions
            assertEquals(false, body.isDailyLivingIsEntited());
            assertEquals(false, body.isDailyLivingIsSeverelyLimited());
            assertNull(body.getDailyLivingDescriptors());
            assertNull(payload.getDateIssued());
            assertEquals(LocalDate.now(), payload.getGeneratedDate());

            PipTemplateContent templateContent = (PipTemplateContent) payload.getWriteFinalDecisionTemplateContent();
            if ("noAward".equals(rate)) {
                assertEquals(PipScenario.SCENARIO_NOT_CONSIDERED_NO_AWARD, templateContent.getScenario());
            } else {
                assertEquals(PipScenario.SCENARIO_NOT_CONSIDERED_AWARD, templateContent.getScenario());
            }
        }
    }

    @Test
    public void willSetPreviewFileWithNullReasons_WhenReasonsListIsEmpty() {

        final String endDate = "2020-10-10";
        final String rate = "standardRate";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionReasons(new ArrayList<>());

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(null);

        // Mobility specific parameters
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(rate);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("same");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(Arrays.asList("movingAround"));
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true,
            documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, true);

        assertEquals("standard rate", body.getMobilityAwardRate());
        assertEquals(false, body.isMobilityIsSeverelyLimited());
        assertEquals(true, body.isMobilityIsEntited());

        assertNotNull(body.getMobilityDescriptors());
        assertEquals(1, body.getMobilityDescriptors().size());
        assertNotNull(body.getMobilityDescriptors().get(0));
        assertEquals(10, body.getMobilityDescriptors().get(0).getActivityAnswerPoints());
        assertEquals("d", body.getMobilityDescriptors().get(0).getActivityAnswerLetter());
        assertEquals("Can stand and then move using an aid or appliance more than 20 metres but no more than 50 metres.", body.getMobilityDescriptors().get(0).getActivityAnswerValue());
        assertEquals("12. Moving around", body.getMobilityDescriptors().get(0).getActivityQuestionValue());
        assertEquals("12", body.getMobilityDescriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getMobilityNumberOfPoints());
        assertEquals(10, body.getMobilityNumberOfPoints().intValue());

        // Daily living specific assertions
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingAwardRate());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }
}
