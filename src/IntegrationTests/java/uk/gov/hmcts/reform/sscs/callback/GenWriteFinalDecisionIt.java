package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.DescriptorTable;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.TemplateComponent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

@RunWith(JUnitParamsRunner.class)
public class GenWriteFinalDecisionIt extends WriteFinalDecisionItBase {

    @Test
    public void callToMidEventCallback_willValidateTheDate() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorDLA.json", "START_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Decision notice end date must be after decision notice start date", result.getErrors().toArray()[0]);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForDescriptorRoute() throws Exception {
        // N/A for GEN
    }

    @Test
    public void callToAboutToSubmitHandler_willWriteManuallyUploadedFinalDecisionToCase() throws Exception {
        setup("callback/writeFinalDecisionManualUploadDescriptorDLA.json");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());

        assertEquals(DRAFT_DECISION_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Decision Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @NamedParameters("noAwardComparisons")
    @SuppressWarnings("unused")
    private Object[] noAwardComparisons() {
        return new Object[] {
            new Object[] {"lower", false, true, false},
            new Object[] {"same", false, false, false},
            new Object[] {"lower", false, true, true},
            new Object[] {"same", false, false, true}
        };
    }

    @NamedParameters("noAwardNoAwardComparisons")
    @SuppressWarnings("unused")
    private Object[] noAwardNoAwardComparisons() {
        return new Object[] {
            new Object[] {"lower", "lower", false, true, false},
            new Object[] {"lower", "same", false, true, false},
            new Object[] {"same", "lower", false, true, false},
            new Object[] {"same", "same", false, false, false},
            new Object[] {"lower", "lower", false, true, true},
            new Object[] {"lower", "same", false, true, true},
            new Object[] {"same", "lower", false, true, true},
            new Object[] {"same", "same", false, false, true}
        };
    }

    @NamedParameters("noAwardStandardRateComparisons")
    @SuppressWarnings("unused")
    private Object[] noAwardStandardRateComparisons() {
        return new Object[] {
            new Object[] {"lower", "lower", false, true, false},
            new Object[] {"lower", "same", false, true, false},
            new Object[] {"lower", "higher", true, true, false},
            new Object[] {"same", "lower", false, true, false},
            new Object[] {"same", "same", false, false, false},
            new Object[] {"same", "higher", true, true, false},
            new Object[] {"lower", "lower", false, true, true},
            new Object[] {"lower", "same", false, true, true},
            new Object[] {"lower", "higher", true, true, true},
            new Object[] {"same", "lower", false, true, true},
            new Object[] {"same", "same", false, false, true},
            new Object[] {"same", "higher", true, true, true}
        };
    }

    @NamedParameters("noAwardEnhancedRateComparisons")
    @SuppressWarnings("unused")
    private Object[] noAwardEnhancedRateComparisons() {
        return new Object[] {
            new Object[] {"lower", "same", false, true, false},
            new Object[] {"lower", "higher", true, true, false},
            new Object[] {"same", "same", false, false, false},
            new Object[] {"same", "higher", true, true, false},
            new Object[] {"lower", "same", false, true, true},
            new Object[] {"lower", "higher", true, true, true},
            new Object[] {"same", "same", false, false, true},
            new Object[] {"same", "higher", true, true, true}
        };
    }

    @NamedParameters("standardRateComparisons")
    @SuppressWarnings("unused")
    private Object[] standardRateComparisons() {
        return new Object[] {
            new Object[] {"lower", false, true, false},
            new Object[] {"same", false, false, false},
            new Object[] {"higher", true, true, false},
            new Object[] {"lower", false, true, true},
            new Object[] {"same", false, false, true},
            new Object[] {"higher", true, true, true}
        };
    }

    @NamedParameters("allowed")
    @SuppressWarnings("unused")
    private Object[] allowed() {
        return new Object[] {
            new Object[] {false},
            new Object[] {true}
        };
    }

    @NamedParameters("standardRateStandardRateComparisons")
    @SuppressWarnings("unused")
    private Object[] standardRateStandardRateComparisons() {
        return new Object[] {
            new Object[] {"lower", "lower", false, true, false},
            new Object[] {"same", "lower", false, true, false},
            new Object[] {"higher", "lower", true, true, false},
            new Object[] {"lower", "same", false, true, false},
            new Object[] {"same", "same", false, false, false},
            new Object[] {"higher", "same", true, true, false},
            new Object[] {"lower", "higher", true, true, false},
            new Object[] {"same", "higher", true, true, false},
            new Object[] {"higher", "higher", true, true, false},
            new Object[] {"lower", "lower", false, true, true},
            new Object[] {"same", "lower", false, true, true},
            new Object[] {"higher", "lower", true, true, true},
            new Object[] {"lower", "same", false, true, true},
            new Object[] {"same", "same", false, false, true},
            new Object[] {"higher", "same", true, true, true},
            new Object[] {"lower", "higher", true, true, true},
            new Object[] {"same", "higher", true, true, true},
            new Object[] {"higher", "higher", true, true, true},
        };
    }

    @NamedParameters("standardRateEnhancedRateComparisons")
    @SuppressWarnings("unused")
    private Object[] standardRateEnhancedRateComparisons() {
        return new Object[] {
            new Object[] {"lower", "same", false, true, false},
            new Object[] {"same", "same", false, false, false},
            new Object[] {"higher", "same", true, true, false},
            new Object[] {"lower", "higher", true, true, false},
            new Object[] {"same", "higher", true, true, false},
            new Object[] {"higher", "higher", true, true, false},
            new Object[] {"lower", "same", false, true, true},
            new Object[] {"same", "same", false, false, true},
            new Object[] {"higher", "same", true, true, true},
            new Object[] {"lower", "higher", true, true, true},
            new Object[] {"same", "higher", true, true, true},
            new Object[] {"higher", "higher", true, true, true}
        };
    }

    @NamedParameters("enhancedRateEnhancedRateComparisons")
    @SuppressWarnings("unused")
    private Object[] enhancedRateEnhancedRateComparisons() {
        return new Object[] {
            new Object[] {"same", "same", false, false, false},
            new Object[] {"higher", "same", true, true, false},
            new Object[] {"same", "higher", true, true, false},
            new Object[] {"higher", "higher", true, true, false},
            new Object[] {"same", "same", false, false, true},
            new Object[] {"higher", "same", true, true, true},
            new Object[] {"same", "higher", true, true, true},
            new Object[] {"higher", "higher", true, true, true}
        };
    }

    @NamedParameters("enhancedRateComparisons")
    @SuppressWarnings("unused")
    private Object[] enhancedRateComparisons() {
        return new Object[] {
            new Object[] {"same", false, false, false},
            new Object[] {"higher", true, true, false},
            new Object[] {"same", false, false, true},
            new Object[] {"higher", true, true, true}
        };
    }

    @NamedParameters("hearingTypeCombinations")
    @SuppressWarnings("unused")
    private Object[] hearingTypeCombinations() {
        return new Object[] {
            new Object[] {"video", true, false},
            new Object[] {"video", true, true},
            new Object[] {"video", true, false},
            new Object[] {"video", true, true},
            new Object[] {"paper", false, false},
            new Object[] {"paper", false, true},
            new Object[] {"paper", true, false},
            new Object[] {"paper", true, true},
            new Object[] {"telephone", true, false},
            new Object[] {"telephone", true, true},
            new Object[] {"telephone", true, false},
            new Object[] {"telephone", true, true},
            new Object[] {"triage", false, false},
            new Object[] {"triage", false, true},
            new Object[] {"triage", true, false},
            new Object[] {"triage", true, true},
            new Object[] {"faceToFace", true, false},
            new Object[] {"faceToFace", true, true},
            new Object[] {"faceToFace", true, false},
            new Object[] {"faceToFace", true, true},
        };
    }

    protected String getJson(String fileLocation) throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource(fileLocation)).getFile();
        return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
    }

    public String getJsonCallbackForTestAndReplace(String fileLocation, List<String> replaceKeys, List<String> replaceValues) throws IOException {
        String result = getJson(fileLocation);
        for (int i = 0; i < replaceKeys.size(); i++) {
            result = result.replace(replaceKeys.get(i), replaceValues.get(i));
        }
        return result;
    }

    private String replaceNewLines(String pdfText) {
        return pdfText.replaceAll("-\n", "-").replaceAll("[\\n\\t]", " ").replaceAll("\\s{2,}", " ");
    }

    private void assertIsParagraphWithText(List<TemplateComponent<?>> components, int paragraphNumber, String text) {
        List<TemplateComponent<?>> filteredComponents = components.stream().filter(c -> c.isParagraph()).collect(Collectors.toList());
        Paragraph paragraph = Paragraph.class.cast(filteredComponents.get(paragraphNumber - 1));
        Assert.assertEquals(text, paragraph.getContent());
    }

    private void assertIsDescriptorTableWithDescriptors(List<TemplateComponent<?>> components, int componentIndex, Descriptor... descriptors) {
        DescriptorTable table = DescriptorTable.class.cast(components.get(componentIndex));
        List<Descriptor> tableDescriptors = table.getContent();
        tableDescriptors.forEach(c -> System.out.println(c.getActivityQuestionValue()));
        Assert.assertEquals(descriptors.length, tableDescriptors.size());
        for (int i = 0; i < descriptors.length; i++) {
            System.out.println(tableDescriptors.get(i));
            Assert.assertEquals(descriptors[i].getActivityAnswerPoints(), tableDescriptors.get(i).getActivityAnswerPoints());
            Assert.assertEquals(descriptors[i].getActivityAnswerValue(), tableDescriptors.get(i).getActivityAnswerValue());
            Assert.assertEquals(descriptors[i].getActivityQuestionValue(), tableDescriptors.get(i).getActivityQuestionValue());
            Assert.assertEquals(descriptors[i].getActivityQuestionNumber(), tableDescriptors.get(i).getActivityQuestionNumber());
            Assert.assertEquals(descriptors[i].getActivityAnswerLetter(), tableDescriptors.get(i).getActivityAnswerLetter());
        }
    }

    @Test
    @Parameters(named = "allowed")
    public void nonDescriptorFlow_shouldGeneratePdfWithExpectedText(boolean allowed) throws Exception {
        setup();
        String json = getJsonCallbackForTestAndReplace("callback/dlaScenarioCallbackNonDescriptorFlow.json", Arrays.asList("ALLOWED_OR_REFUSED"),
            Arrays.asList(allowed ? "allowed" : "refused"));

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
        final WriteFinalDecisionTemplateContent content = parentPayload.getWriteFinalDecisionTemplateContent();
        List<TemplateComponent<?>> components = content.getComponents();
        if (allowed) {
            assertIsParagraphWithText(components, 1, "The appeal is allowed.");
            assertIsParagraphWithText(components, 2, "The decision made by the Secretary of State on 17/11/2020 is set aside.");
        } else {
            assertIsParagraphWithText(components, 1, "The appeal is refused.");
            assertIsParagraphWithText(components, 2, "The decision made by the Secretary of State on 17/11/2020 is confirmed.");
        }
        assertIsParagraphWithText(components, 3, "My summary.");
        assertIsParagraphWithText(components, 4, "Reasons for decision 1");
        assertIsParagraphWithText(components, 5, "Reasons for decision 2");
        assertIsParagraphWithText(components, 6, "Anything else.");
        assertIsParagraphWithText(components, 7,
                "This has been a remote hearing in the form of a video hearing. Joe Bloggs the appellant attended and the Tribunal considered the appeal bundle to page B7. First Tier Agency representative attended on behalf of the Respondent.");
        Assert.assertEquals(7, components.size());
    }

    @Test
    @Parameters(named = "allowed")
    public void nonDescriptorFlowHmrc_shouldGeneratePdfWithExpectedText(boolean allowed) throws Exception {
        setup();
        String json = getJsonCallbackForTestAndReplace("callback/hmrcScenarioCallbackNonDescriptorFlow.json", Arrays.asList("ALLOWED_OR_REFUSED"),
                Arrays.asList(allowed ? "allowed" : "refused"));

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
        final WriteFinalDecisionTemplateContent content = parentPayload.getWriteFinalDecisionTemplateContent();
        List<TemplateComponent<?>> components = content.getComponents();
        if (allowed) {
            assertIsParagraphWithText(components, 1, "The appeal is allowed.");
            assertIsParagraphWithText(components, 2, "The decision made by the Respondent on 17/11/2020 is set aside.");
        } else {
            assertIsParagraphWithText(components, 1, "The appeal is refused.");
            assertIsParagraphWithText(components, 2, "The decision made by the Respondent on 17/11/2020 is confirmed.");
        }
        assertIsParagraphWithText(components, 3, "My summary.");
        assertIsParagraphWithText(components, 4, "Reasons for decision 1");
        assertIsParagraphWithText(components, 5, "Reasons for decision 2");
        assertIsParagraphWithText(components, 6, "Anything else.");
        assertIsParagraphWithText(components, 7,
                "This has been a remote hearing in the form of a video hearing. Joe Bloggs the appellant attended and the Tribunal considered the appeal bundle to page B7. First Tier Agency representative attended on behalf of the Respondent.");
        Assert.assertEquals(7, components.size());
    }

    @Test
    @Parameters(named = "hearingTypeCombinations")
    public void notConsideredNoAward_shouldGeneratePdfWithExpectedTextForHearingType(String hearingType, boolean appellantAttended, boolean presentingOfficerAttended) throws Exception {
        setup();
        String comparedToDwpMobility = "lower";

        boolean indefinite = false;

        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("callback/dlaScenarioCallbackHearingType.json", Arrays
                .asList("\"pipWriteFinalDecisionComparedToDWPDailyLivingQuestion\": \"COMPARED_TO_DWP_DAILY_LIVING\",", "DAILY_LIVING_RATE",
                    "\"pipWriteFinalDecisionDailyLivingActivitiesQuestion\" : [\"preparingFood\", \"takingNutrition\"],", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER",
                    "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\",", "TYPE_OF_HEARING", "APPELLANT_ATTENDED", "PRESENTING_OFFICER_ATTENDED"), Arrays
                .asList("", "notConsidered", "", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a", "", hearingType, appellantAttended ? "Yes" : "No",
                    presentingOfficerAttended ? "Yes" : "No"));
        } else {
            json = getJsonCallbackForTestAndReplace("callback/pipScenarioCallbackHearingType.json", Arrays
                .asList("\"pipWriteFinalDecisionComparedToDWPDailyLivingQuestion\": \"COMPARED_TO_DWP_DAILY_LIVING\",", "DAILY_LIVING_RATE",
                    "\"pipWriteFinalDecisionDailyLivingActivitiesQuestion\" : [\"preparingFood\", \"takingNutrition\"],", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER",
                    "MOVING_AROUND_ANSWER", "TYPE_OF_HEARING", "APPELLANT_ATTENDED", "PRESENTING_OFFICER_ATTENDED"), Arrays
                .asList("", "notConsidered", "", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a", hearingType, appellantAttended ? "Yes" : "No",
                    presentingOfficerAttended ? "Yes" : "No"));
        }


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
        final WriteFinalDecisionTemplateContent content = parentPayload.getWriteFinalDecisionTemplateContent();
        List<TemplateComponent<?>> components = content.getComponents();

        assertIsParagraphWithText(components, 1, "The appeal is refused.");
        assertIsParagraphWithText(components, 2, "The decision made by the Secretary of State on 17/11/2020 is set aside.");

        assertIsParagraphWithText(components, 3, "Only the mobility component was in issue on this appeal and the daily living component was not considered.");
        assertIsParagraphWithText(components, 4,
            "Joe Bloggs does not qualify for an award of the mobility component from 17/12/2020. They score 0 points. This is insufficient to meet the threshold for the test.");
        assertIsDescriptorTableWithDescriptors(components, 4, Descriptor.builder().activityQuestionNumber("12").activityAnswerLetter("a").activityQuestionValue("12. Moving around")
            .activityAnswerValue("Can stand and then move more than 200 metres, either aided or unaided.").activityAnswerPoints(0).build());
        assertIsParagraphWithText(components, 5, "Reasons for decision 1");
        assertIsParagraphWithText(components, 6, "Reasons for decision 2");
        assertIsParagraphWithText(components, 7, "Anything else.");

        boolean additionalParagraph = false;
        if ("video".equals(hearingType)) {
            if (appellantAttended && presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "This has been a remote hearing in the form of a video hearing. Joe Bloggs the appellant attended and the Tribunal considered the appeal bundle to page B7. First Tier Agency representative attended on behalf of the Respondent.");
            } else if (appellantAttended && !presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "This has been a remote hearing in the form of a video hearing. Joe Bloggs the appellant attended and the Tribunal considered the appeal bundle to page B7. First Tier Agency representative did not attend.");
            } else if (!appellantAttended && presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "This has been a remote hearing in the form of a video hearing. Joe Bloggs did not attend the hearing today. A Presenting Officer attended on behalf of the Respondent.");
                assertIsParagraphWithText(components, 9,
                    "Having considered the appeal bundle to page B7 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Joe Bloggs of the hearing and that it is in the interests of justice to proceed today. ");
                additionalParagraph = true;
            } else if (!appellantAttended && !presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "This has been a remote hearing in the form of a video hearing. Joe Bloggs did not attend the hearing today. First Tier Agency representative did not attend.");
                assertIsParagraphWithText(components, 9, (
                    "Having considered the appeal bundle to page B7 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Joe Bloggs of the hearing and that it is in the interests of justice to proceed today. "));
                additionalParagraph = true;
            }
        } else if ("telephone".equals(hearingType)) {
            if (appellantAttended && presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "This has been a remote hearing in the form of a telephone hearing. Joe Bloggs the appellant attended and the Tribunal considered the appeal bundle to page B7. First Tier Agency representative attended on behalf of the Respondent.");
            } else if (appellantAttended && !presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "This has been a remote hearing in the form of a telephone hearing. Joe Bloggs the appellant attended and the Tribunal considered the appeal bundle to page B7. First Tier Agency representative did not attend.");
            } else if (!appellantAttended && presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "This has been a remote hearing in the form of a telephone hearing. Joe Bloggs did not attend the hearing today. A Presenting Officer attended on behalf of the Respondent.");
                assertIsParagraphWithText(components, 9,
                    "Having considered the appeal bundle to page B7 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Joe Bloggs of the hearing and that it is in the interests of justice to proceed today. ");
                additionalParagraph = true;
            } else if (!appellantAttended && !presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "This has been a remote hearing in the form of a telephone hearing. Joe Bloggs did not attend the hearing today. First Tier Agency representative did not attend.");
                assertIsParagraphWithText(components, 9,
                    "Having considered the appeal bundle to page B7 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Joe Bloggs of the hearing and that it is in the interests of justice to proceed today. ");
                additionalParagraph = true;
            }
        } else if ("paper".equals(hearingType)) {
            assertIsParagraphWithText(components, 8, "No party has objected to the matter being decided without a hearing.");
            assertIsParagraphWithText(components, 9,
                "Having considered the appeal bundle to page B7 and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way.");
            additionalParagraph = true;
        } else if ("faceToFace".equals(hearingType)) {
            if (appellantAttended && presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "This has been an oral (face to face) hearing. Joe Bloggs the appellant attended the hearing today and the Tribunal considered the appeal bundle to page B7. First Tier Agency representative attended on behalf of the Respondent.");
            } else if (appellantAttended && !presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "This has been an oral (face to face) hearing. Joe Bloggs the appellant attended the hearing today and the Tribunal considered the appeal bundle to page B7. First Tier Agency representative did not attend.");
            } else if (!appellantAttended && presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "Joe Bloggs requested an oral hearing but did not attend today. A Presenting Officer attended on behalf of the Respondent.");
                assertIsParagraphWithText(components, 9,
                    "Having considered the appeal bundle to page B7 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Joe Bloggs of the hearing and that it is in the interests of justice to proceed today. ");
                additionalParagraph = true;
            } else if (!appellantAttended && !presentingOfficerAttended) {
                assertIsParagraphWithText(components, 8,
                    "Joe Bloggs requested an oral hearing but did not attend today. First Tier Agency representative did not attend.");
                assertIsParagraphWithText(components, 9,
                    "Having considered the appeal bundle to page B7 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Joe Bloggs of the hearing and that it is in the interests of justice to proceed today. ");
                additionalParagraph = true;
            }
        } else if ("triage".equals(hearingType)) {
            assertIsParagraphWithText(components, 8,
                "The tribunal considered the appeal bundle to page B7.");
        }

        if (additionalParagraph) {
            Assert.assertEquals(10, components.size());
        } else {
            Assert.assertEquals(9, components.size());
        }
    }


    @Override
    protected String getBenefitType() {
        return "DLA";
    }
}
