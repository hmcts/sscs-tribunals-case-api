package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler.getJsonCallbackForTestAndReplace;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.functional.mya.BaseFunctionTest;

@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
public class PipDecisionNoticeFunctionalTest extends BaseFunctionTest {

    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();

    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Autowired
    protected ObjectMapper objectMapper;


    // The Scenarios are defined https://tools.hmcts.net/confluence/display/SSCS/ESA+DN+template+content+-+judges+input
    @Test
    public void scenario1_shouldGeneratePdfWithExpectedText() throws IOException {
        String json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList("same", "standardRate", "preparingFood1e", "takingNutrition2d", "same", "standardRate", "movingAround", "movingAround12a"));
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the standard rate from 17/12/2020 to 17/12/2021."));
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs has limited ability to carry out the activities of daily living set out below. They score 8 points. They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food e. Needs supervision or assistance to either prepare or cook a simple meal. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is entitled to the mobility component at the standard rate from 17/12/2020 for an indefinite period."));
            assertThat(pdfTextWithoutNewLines, containsString("6. Joe Bloggs is limited in their ability to mobilise. They score 0 points."));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("9. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("10. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("11.")));
        }
    }

    @NamedParameters("noAwardComparisons")
    @SuppressWarnings("unused")
    private Object[] noAwardComparisons() {
        return new Object[]{
            new Object[]{"lower", false, true, false},
            new Object[]{"same", false, false, false},
            new Object[]{"lower", false, true, true},
            new Object[]{"same", false, false, true}
        };
    }

    @NamedParameters("noAwardNoAwardComparisons")
    @SuppressWarnings("unused")
    private Object[] noAwardNoAwardComparisons() {
        return new Object[]{
            new Object[]{"lower", "lower", false, true, false},
            new Object[]{"lower", "same", false, true, false},
            new Object[]{"same", "lower", false, true, false},
            new Object[]{"same", "same", false, false, false},
            new Object[]{"lower", "lower", false, true, true},
            new Object[]{"lower", "same", false, true, true},
            new Object[]{"same", "lower", false, true, true},
            new Object[]{"same", "same", false, false, true}
        };
    }

    @NamedParameters("noAwardStandardRateComparisons")
    @SuppressWarnings("unused")
    private Object[] noAwardStandardRateComparisons() {
        return new Object[]{
            new Object[]{"lower", "lower", false, true, false},
            new Object[]{"lower", "same", false, true, false},
            new Object[]{"lower", "higher", true, true, false},
            new Object[]{"same", "lower", false, true, false},
            new Object[]{"same", "same", false, false, false},
            new Object[]{"same", "higher", true, true, false},
            new Object[]{"lower", "lower", false, true, true},
            new Object[]{"lower", "same", false, true, true},
            new Object[]{"lower", "higher", true, true, true},
            new Object[]{"same", "lower", false, true, true},
            new Object[]{"same", "same", false, false, true},
            new Object[]{"same", "higher", true, true, true}
        };
    }

    @NamedParameters("noAwardEnhancedRateComparisons")
    @SuppressWarnings("unused")
    private Object[] noAwardEnhancedRateComparisons() {
        return new Object[]{
            new Object[]{"lower", "same", false, true, false},
            new Object[]{"lower", "higher", true, true, false},
            new Object[]{"same", "same", false, false, false},
            new Object[]{"same", "higher", true, true, false},
            new Object[]{"lower", "same", false, true, true},
            new Object[]{"lower", "higher", true, true, true},
            new Object[]{"same", "same", false, false, true},
            new Object[]{"same", "higher", true, true, true}
        };
    }

    @NamedParameters("standardRateComparisons")
    @SuppressWarnings("unused")
    private Object[] standardRateComparisons() {
        return new Object[]{
            new Object[]{"lower", false, true, false},
            new Object[]{"same", false, false, false},
            new Object[]{"higher", true, true, false},
            new Object[]{"lower", false, true, true},
            new Object[]{"same", false, false, true},
            new Object[]{"higher", true, true, true}
        };
    }

    @NamedParameters("standardRateStandardRateComparisons")
    @SuppressWarnings("unused")
    private Object[] standardRateStandardRateComparisons() {
        return new Object[]{
            new Object[]{"lower", "lower", false, true, false},
            new Object[]{"same", "lower", false, true, false},
            new Object[]{"higher", "lower", true, true, false},
            new Object[]{"lower", "same", false, true, false},
            new Object[]{"same", "same", false, false, false},
            new Object[]{"higher", "same", true, true, false},
            new Object[]{"lower", "higher", true, true, false},
            new Object[]{"same", "higher", true, true, false},
            new Object[]{"higher", "higher", true, true, false},
            new Object[]{"lower", "lower", false, true, true},
            new Object[]{"same", "lower", false, true, true},
            new Object[]{"higher", "lower", true, true, true},
            new Object[]{"lower", "same", false, true, true},
            new Object[]{"same", "same", false, false, true},
            new Object[]{"higher", "same", true, true, true},
            new Object[]{"lower", "higher", true, true, true},
            new Object[]{"same", "higher", true, true, true},
            new Object[]{"higher", "higher", true, true, true},
        };
    }

    @NamedParameters("standardRateEnhancedRateComparisons")
    @SuppressWarnings("unused")
    private Object[] standardRateEnhancedRateComparisons() {
        return new Object[]{
            new Object[]{"lower", "same", false, true, false},
            new Object[]{"same", "same", false, false, false},
            new Object[]{"higher", "same", true, true, false},
            new Object[]{"lower", "higher", true, true, false},
            new Object[]{"same", "higher", true, true, false},
            new Object[]{"higher", "higher", true, true, false},
            new Object[]{"lower", "same", false, true, true},
            new Object[]{"same", "same", false, false, true},
            new Object[]{"higher", "same", true, true, true},
            new Object[]{"lower", "higher", true, true, true},
            new Object[]{"same", "higher", true, true, true},
            new Object[]{"higher", "higher", true, true, true}
        };
    }

    @NamedParameters("enhancedRateEnhancedRateComparisons")
    @SuppressWarnings("unused")
    private Object[] enhancedRateEnhancedRateComparisons() {
        return new Object[]{
            new Object[]{"same", "same", false, false, false},
            new Object[]{"higher", "same", true, true, false},
            new Object[]{"same", "higher", true, true, false},
            new Object[]{"higher", "higher", true, true, false},
            new Object[]{"same", "same", false, false, true},
            new Object[]{"higher", "same", true, true, true},
            new Object[]{"same", "higher", true, true, true},
            new Object[]{"higher", "higher", true, true, true}
        };
    }

    @NamedParameters("enhancedRateComparisons")
    @SuppressWarnings("unused")
    private Object[] enhancedRateComparisons() {
        return new Object[]{
            new Object[]{"same", false, false, false},
            new Object[]{"higher", true, true, false},
            new Object[]{"same", false, false, true},
            new Object[]{"higher", true, true, true}
        };
    }

    @NamedParameters("hearingTypeCombinations")
    @SuppressWarnings("unused")
    private Object[] hearingTypeCombinations() {
        return new Object[]{
            new Object[]{"video", false, false},
            new Object[]{"video", false, true},
            new Object[]{"video", true, false},
            new Object[]{"video", true, true},
            new Object[]{"paper", false, false},
            new Object[]{"paper", false, true},
            new Object[]{"paper", true, false},
            new Object[]{"paper", true, true},
            new Object[]{"telephone", false, false},
            new Object[]{"telephone", false, true},
            new Object[]{"telephone", true, false},
            new Object[]{"telephone", true, true},
            new Object[]{"triage", false, false},
            new Object[]{"triage", false, true},
            new Object[]{"triage", true, false},
            new Object[]{"triage", true, true},
            new Object[]{"faceToFace", false, false},
            new Object[]{"faceToFace", false, true},
            new Object[]{"faceToFace", true, false},
            new Object[]{"faceToFace", true, true},
        };
    }

    @Test
    @Parameters(named = "hearingTypeCombinations")
    public void notConsideredNoAward_shouldGeneratePdfWithExpectedTextForHearingType(String hearingType, boolean appellantAttended, boolean presentingOfficerAttended) throws IOException {

        String comparedToDwpMobility = "lower";
        boolean allowed = false;
        boolean setAside = true;
        boolean indefinite = false;

        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallbackHearingType.json", Arrays.asList("\"pipWriteFinalDecisionComparedToDWPDailyLivingQuestion\": \"COMPARED_TO_DWP_DAILY_LIVING\",", "DAILY_LIVING_RATE", "\"pipWriteFinalDecisionDailyLivingActivitiesQuestion\" : [\"preparingFood\", \"takingNutrition\"],", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\",", "TYPE_OF_HEARING", "APPELLANT_ATTENDED", "PRESENTING_OFFICE_ATTENDED"), Arrays.asList("", "notConsidered", "", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a", "", hearingType, appellantAttended ? "Yes" : "No", presentingOfficerAttended ? "Yes" : "No"));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallbackHearingType.json", Arrays.asList("\"pipWriteFinalDecisionComparedToDWPDailyLivingQuestion\": \"COMPARED_TO_DWP_DAILY_LIVING\",", "DAILY_LIVING_RATE", "\"pipWriteFinalDecisionDailyLivingActivitiesQuestion\" : [\"preparingFood\", \"takingNutrition\"],", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "TYPE_OF_HEARING", "APPELLANT_ATTENDED", "PRESENTING_OFFICE_ATTENDED"), Arrays.asList("", "notConsidered", "", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a", hearingType, appellantAttended ? "Yes" : "No", presentingOfficerAttended ? "Yes" : "No"));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("3. Only the mobility component was in issue on this appeal and the daily living component was not considered. "));
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs does not qualify for an award of the mobility component from 17/12/2020. They score 0 points. This is insufficient to meet the threshold for the test."));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around a."));
            assertThat(pdfTextWithoutNewLines, containsString("0 points"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Anything else."));
            boolean additionalParagraph = false;
            if ("video".equals(hearingType)) {
                if (appellantAttended && presentingOfficerAttended) {
                    assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
                } else if (appellantAttended && !presentingOfficerAttended) {
                    assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. No Presenting Officer attended on behalf of the Respondent."));
                } else if (!appellantAttended && presentingOfficerAttended) {
                    assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Felix Sydney did not attend the hearing today. A Presenting Officer attended on behalf of the Respondent.\n"));
                } else if (!appellantAttended && !presentingOfficerAttended) {
                    assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Felix Sydney did not attend the hearing today. No Presenting Officer attended on behalf of the Respondent.\n"));
                }
                assertThat(pdfTextWithoutNewLines, containsString("Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today.\n"));
            } else {
                if (appellantAttended && presentingOfficerAttended) {
                    assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
                } else if (appellantAttended && !presentingOfficerAttended) {
                    assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. No Presenting Officer attended on behalf of the Respondent."));
                } else if (!appellantAttended && presentingOfficerAttended) {
                    assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Felix Sydney did not attend the hearing today. A Presenting Officer attended on behalf of the Respondent.\n"));
                } else if (!appellantAttended && !presentingOfficerAttended) {
                    assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Felix Sydney did not attend the hearing today. No Presenting Officer attended on behalf of the Respondent.\n"));
                }
                assertThat(pdfTextWithoutNewLines, containsString("Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today.\n"));
            }
            if (additionalParagraph) {
                assertThat(pdfTextWithoutNewLines, not(containsString("10.")));
            } else {
                assertThat(pdfTextWithoutNewLines, not(containsString("9.")));
            }
        }
    }

    @Test
    @Parameters(named = "noAwardComparisons")
    public void notConsideredNoAward_shouldGeneratePdfWithExpectedText(String comparedToDwpMobility, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("\"pipWriteFinalDecisionComparedToDWPDailyLivingQuestion\": \"COMPARED_TO_DWP_DAILY_LIVING\",", "DAILY_LIVING_RATE", "\"pipWriteFinalDecisionDailyLivingActivitiesQuestion\" : [\"preparingFood\", \"takingNutrition\"],", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList("", "notConsidered", "", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("\"pipWriteFinalDecisionComparedToDWPDailyLivingQuestion\": \"COMPARED_TO_DWP_DAILY_LIVING\",", "DAILY_LIVING_RATE", "\"pipWriteFinalDecisionDailyLivingActivitiesQuestion\" : [\"preparingFood\", \"takingNutrition\"],", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList("", "notConsidered", "", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a"));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("3. Only the mobility component was in issue on this appeal and the daily living component was not considered. "));
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs does not qualify for an award of the mobility component from 17/12/2020. They score 0 points. This is insufficient to meet the threshold for the test."));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around a."));
            assertThat(pdfTextWithoutNewLines, containsString("0 points"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("9.")));
        }
    }

    @Parameters(named = "standardRateComparisons")
    public void notConsideredStandardRate_shouldGeneratePdfWithExpectedText(String comparedToDwpMobility, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("\"pipWriteFinalDecisionComparedToDWPDailyLivingQuestion\": \"COMPARED_TO_DWP_DAILY_LIVING\",", "DAILY_LIVING_RATE", "\"pipWriteFinalDecisionDailyLivingActivitiesQuestion\" : [\"preparingFood\", \"takingNutrition\"],", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList("", "notConsidered", "", comparedToDwpMobility, "standardRate", "movingAround", "movingAround12c", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("\"pipWriteFinalDecisionComparedToDWPDailyLivingQuestion\": \"COMPARED_TO_DWP_DAILY_LIVING\",", "DAILY_LIVING_RATE", "\"pipWriteFinalDecisionDailyLivingActivitiesQuestion\" : [\"preparingFood\", \"takingNutrition\"],", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList("", "notConsidered", "", comparedToDwpMobility, "standardRate", "movingAround", "movingAround12c"));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("3. Only the mobility component was in issue on this appeal and the daily living component was not considered. "));
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs is entitled to the mobility component at the standard rate from 17/12/2020 for an indefinite period."));
            assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is limited in their ability to mobilise. They score 8 points.They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around c."));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("10.")));
        }
    }

    @Parameters(named = "enhancedRateComparisons")
    public void notConsideredEnhancedRate_shouldGeneratePdfWithExpectedText(String comparedToDwpMobility, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("\"pipWriteFinalDecisionComparedToDWPDailyLivingQuestion\": \"COMPARED_TO_DWP_DAILY_LIVING\",", "DAILY_LIVING_RATE", "\"pipWriteFinalDecisionDailyLivingActivitiesQuestion\" : [\"preparingFood\", \"takingNutrition\"],", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList("", "notConsidered", "", comparedToDwpMobility, "enhancedRate", "movingAround", "movingAround12e", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("\"pipWriteFinalDecisionComparedToDWPDailyLivingQuestion\": \"COMPARED_TO_DWP_DAILY_LIVING\",", "DAILY_LIVING_RATE", "\"pipWriteFinalDecisionDailyLivingActivitiesQuestion\" : [\"preparingFood\", \"takingNutrition\"],", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList("", "notConsidered", "", comparedToDwpMobility, "enhancedRate", "movingAround", "movingAround12e"));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("3. Only the mobility component was in issue on this appeal and the daily living component was not considered. "));
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs is entitled to the mobility component at the enhanced rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs is entitled to the mobility component at the enhanced rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is severely limited in their ability to mobilise. They score 12 points.They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around e."));
            assertThat(pdfTextWithoutNewLines, containsString("12 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("10.")));
        }
    }

    @Test
    @Parameters(named = "noAwardComparisons")
    public void noAwardNotConsidered_shouldGeneratePdfWithExpectedText(String comparedToDwpDailyLiving, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "\"pipWriteFinalDecisionComparedToDWPMobilityQuestion\": \"COMPARED_TO_DWP_MOBILITY\",", "MOBILITY_RATE", "\"pipWriteFinalDecisionMobilityActivitiesQuestion\" : [\"MOBILITY_ACTIVITIES_ANSWER\"],", "\"pipWriteFinalDecisionMovingAroundQuestion\" : \"MOVING_AROUND_ANSWER\",", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "noAward", "preparingFood1d", "takingNutrition2d", "", "notConsidered", "", "", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "\"pipWriteFinalDecisionComparedToDWPMobilityQuestion\": \"COMPARED_TO_DWP_MOBILITY\",", "MOBILITY_RATE", "\"pipWriteFinalDecisionMobilityActivitiesQuestion\" : [\"MOBILITY_ACTIVITIES_ANSWER\"],", "\"pipWriteFinalDecisionMovingAroundQuestion\" : \"MOVING_AROUND_ANSWER\","), Arrays.asList(comparedToDwpDailyLiving, "noAward", "preparingFood1d", "takingNutrition2d", "", "notConsidered", "", ""));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is not entitled to the daily living component from 17/12/2020. They score 6 points. This is insufficient to meet the threshold for the test."));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food d. Needs prompting to be able to either prepare or cook a simple meal. 2 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6 points"));
            assertThat(pdfTextWithoutNewLines, containsString("4. Only the daily living component was in issue on this appeal and the mobility component was not considered. "));
            assertThat(pdfTextWithoutNewLines, containsString("5. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("9.")));
        }
    }

    @Test
    @Parameters(named = "noAwardNoAwardComparisons")
    public void noAwardNoAward_shouldGeneratePdfWithExpectedText(String comparedToDwpDailyLiving, String comparedToDwpMobility, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "noAward", "preparingFood1d", "takingNutrition2d", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList(comparedToDwpDailyLiving, "noAward", "preparingFood1d", "takingNutrition2d", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a"));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is not entitled to the daily living component from 17/12/2020. They score 6 points. This is insufficient to meet the threshold for the test."));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food d. Needs prompting to be able to either prepare or cook a simple meal. 2 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6 points"));
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs does not qualify for an award of the mobility component from 17/12/2020. They score 0 points. This is insufficient to meet the threshold for the test."));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around a."));
            assertThat(pdfTextWithoutNewLines, containsString("0 points"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("9.")));
        }
    }

    @Test
    @Parameters(named = "noAwardStandardRateComparisons")
    public void noAwardStandardRate_shouldGeneratePdfWithExpectedText(String comparedToDwpDailyLiving, String comparedToDwpMobility, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "noAward", "preparingFood1d", "takingNutrition2d", comparedToDwpMobility, "standardRate", "movingAround", "movingAround12c", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList(comparedToDwpDailyLiving, "noAward", "preparingFood1d", "takingNutrition2d", comparedToDwpMobility, "standardRate", "movingAround", "movingAround12c"));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is not entitled to the daily living component from 17/12/2020. They score 6 points. This is insufficient to meet the threshold for the test."));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food d. Needs prompting to be able to either prepare or cook a simple meal. 2 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6 points"));
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs is entitled to the mobility component at the standard rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs is entitled to the mobility component at the standard rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is limited in their ability to mobilise. They score 8 points.They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around c."));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("10.")));
        }
    }

    @Test
    @Parameters(named = "noAwardEnhancedRateComparisons")
    public void noAwardEnhancedRate_shouldGeneratePdfWithExpectedText(String comparedToDwpDailyLiving, String comparedToDwpMobility, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "noAward", "preparingFood1d", "takingNutrition2d", comparedToDwpMobility, "enhancedRate", "movingAround", "movingAround12e", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList(comparedToDwpDailyLiving, "noAward", "preparingFood1d", "takingNutrition2d", comparedToDwpMobility, "enhancedRate", "movingAround", "movingAround12e"));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is not entitled to the daily living component from 17/12/2020. They score 6 points. This is insufficient to meet the threshold for the test."));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food d. Needs prompting to be able to either prepare or cook a simple meal. 2 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6 points"));
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs is entitled to the mobility component at the enhanced rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs is entitled to the mobility component at the enhanced rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is severely limited in their ability to mobilise. They score 12 points.They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around e."));
            assertThat(pdfTextWithoutNewLines, containsString("12 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("10.")));
        }
    }


    @Test
    @Parameters(named = "standardRateComparisons")
    public void standardRateNotConsidered_shouldGeneratePdfWithExpectedText(String comparedToDwpDailyLiving, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "\"pipWriteFinalDecisionComparedToDWPMobilityQuestion\": \"COMPARED_TO_DWP_MOBILITY\",", "MOBILITY_RATE", "\"pipWriteFinalDecisionMobilityActivitiesQuestion\" : [\"MOBILITY_ACTIVITIES_ANSWER\"],", "\"pipWriteFinalDecisionMovingAroundQuestion\" : \"MOVING_AROUND_ANSWER\",", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "standardRate", "preparingFood1e", "takingNutrition2d", "", "notConsidered", "", "", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "\"pipWriteFinalDecisionComparedToDWPMobilityQuestion\": \"COMPARED_TO_DWP_MOBILITY\",", "MOBILITY_RATE", "\"pipWriteFinalDecisionMobilityActivitiesQuestion\" : [\"MOBILITY_ACTIVITIES_ANSWER\"],", "\"pipWriteFinalDecisionMovingAroundQuestion\" : \"MOVING_AROUND_ANSWER\","), Arrays.asList(comparedToDwpDailyLiving, "standardRate", "preparingFood1e", "takingNutrition2d", "", "notConsidered", "", ""));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the standard rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the standard rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs has limited ability to carry out the activities of daily living set out below. They score 8 points. They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food e. Needs supervision or assistance to either prepare or cook a simple meal. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Only the daily living component was in issue on this appeal and the mobility component was not considered. "));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("10.")));
        }
    }

    @Test
    @Parameters(named = "noAwardStandardRateComparisons")
    public void standardRateNoAward_shouldGeneratePdfWithExpectedText(String comparedToDwpMobility, String comparedToDwpDailyLiving, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "standardRate", "preparingFood1e", "takingNutrition2d", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList(comparedToDwpDailyLiving, "standardRate", "preparingFood1e", "takingNutrition2d", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a"));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the standard rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the standard rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs has limited ability to carry out the activities of daily living set out below. They score 8 points. They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food e. Needs supervision or assistance to either prepare or cook a simple meal. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs does not qualify for an award of the mobility component from 17/12/2020. They score 0 points. This is insufficient to meet the threshold for the test."));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around a."));
            assertThat(pdfTextWithoutNewLines, containsString("0 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("10.")));
        }
    }

    @Test
    @Parameters(named = "standardRateStandardRateComparisons")
    public void standardRateStandardRate_shouldGeneratePdfWithExpectedText(String comparedToDwpDailyLiving, String comparedToDwpMobility, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "standardRate", "preparingFood1e", "takingNutrition2d", comparedToDwpMobility, "standardRate", "movingAround", "movingAround12c", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList(comparedToDwpDailyLiving, "standardRate", "preparingFood1e", "takingNutrition2d", comparedToDwpMobility, "standardRate", "movingAround", "movingAround12c"));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the standard rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the standard rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs has limited ability to carry out the activities of daily living set out below. They score 8 points. They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food e. Needs supervision or assistance to either prepare or cook a simple meal. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is entitled to the mobility component at the standard rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is entitled to the mobility component at the standard rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("6. Joe Bloggs is limited in their ability to mobilise. They score 8 points.They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around c."));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("9. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("10. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("11.")));
        }
    }

    @Test
    @Parameters(named = "standardRateEnhancedRateComparisons")
    public void standardRateEnhancedRate_shouldGeneratePdfWithExpectedText(String comparedToDwpDailyLiving, String comparedToDwpMobility, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "standardRate", "preparingFood1e", "takingNutrition2d", comparedToDwpMobility, "enhancedRate", "movingAround", "movingAround12e", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList(comparedToDwpDailyLiving, "standardRate", "preparingFood1e", "takingNutrition2d", comparedToDwpMobility, "enhancedRate", "movingAround", "movingAround12e"));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the standard rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the standard rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs has limited ability to carry out the activities of daily living set out below. They score 8 points. They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food e. Needs supervision or assistance to either prepare or cook a simple meal. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is entitled to the mobility component at the enhanced rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is entitled to the mobility component at the enhanced rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("6. Joe Bloggs is severely limited in their ability to mobilise. They score 12 points.They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around e."));
            assertThat(pdfTextWithoutNewLines, containsString("12 points"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("9. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("10. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("11.")));
        }
    }

    @Test
    @Parameters(named = "enhancedRateComparisons")
    public void enhancedRateNotConsidered_shouldGeneratePdfWithExpectedText(String comparedToDwpDailyLiving, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "\"pipWriteFinalDecisionComparedToDWPMobilityQuestion\": \"COMPARED_TO_DWP_MOBILITY\",", "MOBILITY_RATE", "\"pipWriteFinalDecisionMobilityActivitiesQuestion\" : [\"MOBILITY_ACTIVITIES_ANSWER\"],", "\"pipWriteFinalDecisionMovingAroundQuestion\" : \"MOVING_AROUND_ANSWER\",", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "enhancedRate", "preparingFood1f", "takingNutrition2d", "", "notConsidered", "", "", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "\"pipWriteFinalDecisionComparedToDWPMobilityQuestion\": \"COMPARED_TO_DWP_MOBILITY\",", "MOBILITY_RATE", "\"pipWriteFinalDecisionMobilityActivitiesQuestion\" : [\"MOBILITY_ACTIVITIES_ANSWER\"],", "\"pipWriteFinalDecisionMovingAroundQuestion\" : \"MOVING_AROUND_ANSWER\","), Arrays.asList(comparedToDwpDailyLiving, "enhancedRate", "preparingFood1f", "takingNutrition2d", "", "notConsidered", "", ""));
        }
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the enhanced rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the enhanced rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs has severely limited ability to carry out the activities of daily living set out below. They score 12 points. They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food f. Cannot prepare and cook food. 8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("12 points"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Only the daily living component was in issue on this appeal and the mobility component was not considered. "));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("10.")));
        }
    }

    @Test
    @Parameters(named = "noAwardEnhancedRateComparisons")
    public void enhancedRateNoAward_shouldGeneratePdfWithExpectedText(String comparedToDwpMobility, String comparedToDwpDailyLiving, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "enhancedRate", "preparingFood1f", "takingNutrition2d", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList(comparedToDwpDailyLiving, "enhancedRate", "preparingFood1f", "takingNutrition2d", comparedToDwpMobility, "noAward", "movingAround", "movingAround12a"));
        }
        System.out.println(json);
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the enhanced rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the enhanced rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs has severely limited ability to carry out the activities of daily living set out below. They score 12 points. They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food f. Cannot prepare and cook food. 8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs does not qualify for an award of the mobility component from 17/12/2020. They score 0 points. This is insufficient to meet the threshold for the test."));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around a."));
            assertThat(pdfTextWithoutNewLines, containsString("0 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("10.")));
        }
    }

    @Test
    @Parameters(named = "standardRateEnhancedRateComparisons")
    public void enhancedRateStandardRate_shouldGeneratePdfWithExpectedText(String comparedToDwpMobility, String comparedToDwpDailyLiving, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "enhancedRate", "preparingFood1f", "takingNutrition2d", comparedToDwpMobility, "standardRate", "movingAround", "movingAround12c", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList(comparedToDwpDailyLiving, "enhancedRate", "preparingFood1f", "takingNutrition2d", comparedToDwpMobility, "standardRate", "movingAround", "movingAround12c"));
        }
        System.out.println(json);
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the enhanced rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the enhanced rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs has severely limited ability to carry out the activities of daily living set out below. They score 12 points. They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food f. Cannot prepare and cook food. 8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is entitled to the mobility component at the standard rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is entitled to the mobility component at the standard rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("6. Joe Bloggs is limited in their ability to mobilise. They score 8 points.They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around c."));
            assertThat(pdfTextWithoutNewLines, containsString("8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("9. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("10. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("11.")));
        }
    }

    @Test
    @Parameters(named = "enhancedRateEnhancedRateComparisons")
    public void enhancedRateEnhancedRate_shouldGeneratePdfWithExpectedText(String comparedToDwpDailyLiving, String comparedToDwpMobility, boolean allowed, boolean setAside, boolean indefinite) throws IOException {
        String json;
        if (indefinite) {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER", "\"writeFinalDecisionEndDate\": \"2021-12-17\","), Arrays.asList(comparedToDwpDailyLiving, "enhancedRate", "preparingFood1f", "takingNutrition2d", comparedToDwpMobility, "enhancedRate", "movingAround", "movingAround12e", ""));
        } else {
            json = getJsonCallbackForTestAndReplace("handlers/writefinaldecision/pipScenarioCallback.json", Arrays.asList("COMPARED_TO_DWP_DAILY_LIVING", "DAILY_LIVING_RATE", "PREPARING_FOOD_ANSWER", "TAKING_NUTRITION_ANSWER", "COMPARED_TO_DWP_MOBILITY", "MOBILITY_RATE", "MOBILITY_ACTIVITIES_ANSWER", "MOVING_AROUND_ANSWER"), Arrays.asList(comparedToDwpDailyLiving, "enhancedRate", "preparingFood1f", "takingNutrition2d", comparedToDwpMobility, "enhancedRate", "movingAround", "movingAround12e"));
        }
        System.out.println(json);
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            if (allowed) {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            }
            if (setAside) {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is set aside."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 in respect of Personal Independence Payment is confirmed."));
            }
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the enhanced rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is entitled to the daily living component at the enhanced rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("4. Joe Bloggs has severely limited ability to carry out the activities of daily living set out below. They score 12 points. They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Preparing food f. Cannot prepare and cook food. 8 points"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Taking nutrition d. Needs prompting to be able to take nutrition. 4 points"));
            assertThat(pdfTextWithoutNewLines, containsString("12 points"));
            if (indefinite) {
                assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is entitled to the mobility component at the enhanced rate from 17/12/2020 for an indefinite period."));
            } else {
                assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs is entitled to the mobility component at the enhanced rate from 17/12/2020 to 17/12/2021."));
            }
            assertThat(pdfTextWithoutNewLines, containsString("6. Joe Bloggs is severely limited in their ability to mobilise. They score 12 points.They satisfy the following descriptors:"));
            assertThat(pdfTextWithoutNewLines, containsString("12. Moving around e."));
            assertThat(pdfTextWithoutNewLines, containsString("12 points"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("9. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("10. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended the hearing today and the tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("11.")));
        }
    }

    private String replaceNewLines(String pdfText) {
        return pdfText.replaceAll("-\n", "-").replaceAll("[\\n\\t]", " ").replaceAll("\\s{2,}", " ");
    }

    private byte[] callPreviewFinalDecision(String json) throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(json), "PreviewFinalDecision");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(ccdEventResponse.getData().getWriteFinalDecisionPreviewDocument(), is(not(nullValue())));
        return sscsMyaBackendRequests.toBytes(ccdEventResponse.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
    }

    private CcdEventResponse getCcdEventResponse(HttpResponse httpResponse) throws IOException {
        String response = EntityUtils.toString(httpResponse.getEntity());
        return objectMapper.readValue(response, CcdEventResponse.class);
    }
}
