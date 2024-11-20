package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import junitparams.JUnitParamsRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;

@RunWith(JUnitParamsRunner.class)
public class PipActivityTypeTest {

    @Mock
    private SscsPipCaseData sscsPipCaseData;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testDailyLivingGetName() {
        Assertions.assertEquals("Daily Living", PipActivityType.DAILY_LIVING.getName());
    }

    @Test
    public void testMobilityGetName() {
        Assertions.assertEquals("Mobility", PipActivityType.MOBILITY.getName());
    }

    @Test
    public void testDailyLivingAwardTypeExtractor() {

        Function<SscsCaseData, String> awardTypeExtractor = PipActivityType.DAILY_LIVING.getAwardTypeExtractor();
        Assertions.assertNotNull(awardTypeExtractor);
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("dailyLivingAnswer");
        String awardType = awardTypeExtractor.apply(sscsCaseData);
        Assertions.assertEquals("dailyLivingAnswer", awardType);
    }

    @Test
    public void testMobilityAwardTypeExtractor() {

        Function<SscsCaseData, String> awardTypeExtractor = PipActivityType.MOBILITY.getAwardTypeExtractor();
        Assertions.assertNotNull(awardTypeExtractor);
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("mobilityAnswer");
        String awardType = awardTypeExtractor.apply(sscsCaseData);
        Assertions.assertEquals("mobilityAnswer", awardType);
    }

    @Test
    public void testDailyLivingAnswersExtractor() {

        Function<SscsCaseData, List<String>> answersExtractor = PipActivityType.DAILY_LIVING.getAnswersExtractor();
        Assertions.assertNotNull(answersExtractor);
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDailyLivingActivitiesQuestion()).thenReturn(Arrays.asList("dailyLivingAnswer1", "dailyLivingAnswer2"));
        List<String> answers = answersExtractor.apply(sscsCaseData);
        Assertions.assertEquals(Arrays.asList("dailyLivingAnswer1", "dailyLivingAnswer2"), answers);
    }

    @Test
    public void testMobilityAnswersExtractor() {

        Function<SscsCaseData, List<String>> answersExtractor = PipActivityType.MOBILITY.getAnswersExtractor();
        Assertions.assertNotNull(answersExtractor);
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMobilityActivitiesQuestion()).thenReturn(Arrays.asList("mobilityAnswer1", "mobilityAnswer2"));
        List<String> answers = answersExtractor.apply(sscsCaseData);
        Assertions.assertEquals(Arrays.asList("mobilityAnswer1", "mobilityAnswer2"), answers);
    }

}
