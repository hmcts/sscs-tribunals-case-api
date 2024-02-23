package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.NamedParameters;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceTestBase;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.service.GenDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.GenDecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;

public class GenWriteFinalDecisionPreviewDecisionServiceTest extends WriteFinalDecisionPreviewDecisionServiceTestBase {

    protected GenDecisionNoticeOutcomeService genDecisionNoticeOutcomeService;
    protected GenDecisionNoticeQuestionService genDecisionNoticeQuestionService;
    protected VenueDataLoader venueDataLoader;

    public GenWriteFinalDecisionPreviewDecisionServiceTest() throws IOException {
        super("GEN");
        this.genDecisionNoticeQuestionService = new GenDecisionNoticeQuestionService();
        this.genDecisionNoticeOutcomeService = new GenDecisionNoticeOutcomeService(genDecisionNoticeQuestionService);
        this.venueDataLoader = new VenueDataLoader();
    }

    @Override
    protected WriteFinalDecisionPreviewDecisionServiceBase createPreviewDecisionService(GenerateFile generateFile, UserDetailsService userDetailsService,
        DocumentConfiguration documentConfiguration) {
        return new GenWriteFinalDecisionPreviewDecisionService(generateFile, userDetailsService, genDecisionNoticeQuestionService,
            genDecisionNoticeOutcomeService, documentConfiguration, venueDataLoader);
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
        // Note for GEN, we don't have the concept of "higher rate" so we just
        // set the common parameters instead
        setCommonNonDescriptorRoutePreviewParams(caseData);
        caseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        setDescriptorFlowIndicator("no", sscsCaseData);
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
        // N/A for GEN
    }

    @Override
    public void givenGeneratedDateIsAlreadySetNonGeneratedNonDescriptorFlow_thenDoSetNewGeneratedDate() {
        setDescriptorFlowIndicator("no", sscsCaseData);
        setCommonNonDescriptorRoutePreviewParams(sscsCaseData);
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
        return false;
    }

    @Test
    public void willSetPreviewFileWithReasons_whenAllowed() {

        setCommonNonDescriptorRoutePreviewParams(sscsCaseData);

        setDescriptorFlowIndicator("no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(null);

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
            appealAllowedExpectation, setAsideExpectation, true, false, true,
            documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonNonDescriptorFlowPreviewParams(body, false);
        assertNull(body.getMobilityAwardRate());
        assertEquals(false, body.isMobilityIsSeverelyLimited());
        assertEquals(false, body.isMobilityIsEntited());
        assertNull(body.getMobilityDescriptors());
        assertNull(body.getMobilityNumberOfPoints());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(body.getDailyLivingNumberOfPoints());
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingAwardRate());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    @Test
    public void willSetPreviewFileWithReasonsAndOtherPartiesAdded_whenAllowed() {

        setCommonNonDescriptorRoutePreviewParams(sscsCaseData);

        setDescriptorFlowIndicator("no", sscsCaseData);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childsupport").build());
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> otherPartyCcdValue1 = CcdValue.<OtherParty>builder().value(OtherParty.builder().name(Name.builder().firstName("otherPartyFirstName1").lastName("otherPartyLastName1").build()).build()).build();
        CcdValue<OtherParty> otherPartyCcdValue2 = CcdValue.<OtherParty>builder().value(OtherParty.builder().name(Name.builder().firstName("otherPartyFirstName2").lastName("otherPartyLastName2").build()).build()).build();
        otherPartyList.add(otherPartyCcdValue1);
        otherPartyList.add(otherPartyCcdValue2);
        sscsCaseData.setOtherParties(otherPartyList);
        OtherPartyAttendedQuestion otherPartyAttendedQuestion1 = OtherPartyAttendedQuestion.builder().value(OtherPartyAttendedQuestionDetails.builder().otherPartyName("otherPartyFirstName1 otherPartyLastName1").attendedOtherParty(YES).build()).build();
        OtherPartyAttendedQuestion otherPartyAttendedQuestion2 = OtherPartyAttendedQuestion.builder().value(OtherPartyAttendedQuestionDetails.builder().otherPartyName("otherPartyFirstName2 otherPartyLastName2").attendedOtherParty(NO).build()).build();
        List<OtherPartyAttendedQuestion> otherPartyAttendedQuestionList = new ArrayList<>();
        otherPartyAttendedQuestionList.add(otherPartyAttendedQuestion1);
        otherPartyAttendedQuestionList.add(otherPartyAttendedQuestion2);
        sscsCaseData.getSscsFinalDecisionCaseData().setOtherPartyAttendedQuestions(otherPartyAttendedQuestionList);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(null);

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
                appealAllowedExpectation, setAsideExpectation, true, false, true,
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertNull(body.getMobilityAwardRate());
        assertEquals(false, body.isMobilityIsSeverelyLimited());
        assertEquals(false, body.isMobilityIsEntited());
        assertNull(body.getMobilityDescriptors());
        assertNull(body.getMobilityNumberOfPoints());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(body.getDailyLivingNumberOfPoints());
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingAwardRate());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertEquals("otherPartyFirstName1 otherPartyLastName1", payload.getWriteFinalDecisionTemplateBody().getOtherPartyNamesAttendedHearing());
    }

    @Test
    public void willSetPreviewFileWithReasons_whenRefused() {

        setCommonNonDescriptorRoutePreviewParams(sscsCaseData);

        setDescriptorFlowIndicator("no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = false;

        boolean setAsideExpectation = false;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, false, true,
            documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonNonDescriptorFlowPreviewParams(body, false);
        assertNull(body.getMobilityAwardRate());
        assertEquals(false, body.isMobilityIsSeverelyLimited());
        assertEquals(false, body.isMobilityIsEntited());
        assertNull(body.getMobilityDescriptors());
        assertNull(body.getMobilityNumberOfPoints());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(body.getDailyLivingNumberOfPoints());
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingAwardRate());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    @Test
    public void willSetPreviewFileWithNullReasons_WhenReasonsListIsEmpty() {

        setCommonNonDescriptorRoutePreviewParams(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionReasons(new ArrayList<>());

        setDescriptorFlowIndicator("no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(null);

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
            appealAllowedExpectation, setAsideExpectation, true, false, true,
            documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonNonDescriptorFlowPreviewParams(body, true);
        assertNull(body.getMobilityAwardRate());
        assertEquals(false, body.isMobilityIsSeverelyLimited());
        assertEquals(false, body.isMobilityIsEntited());
        assertNull(body.getMobilityDescriptors());
        assertNull(body.getMobilityNumberOfPoints());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(body.getDailyLivingNumberOfPoints());
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingAwardRate());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }
}
