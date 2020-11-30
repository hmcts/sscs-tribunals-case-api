package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import junitparams.JUnitParamsRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsUcCaseData;

@RunWith(JUnitParamsRunner.class)
public class UcActivityTypeTest {

    @Mock
    private SscsUcCaseData sscsUcCaseData;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testPhysicalDisablitiesName() {
        Assert.assertEquals("Physical Disabilities", UcActivityType.PHYSICAL_DISABILITIES.getName());
    }

    @Test
    public void testMentalAssessmentGetName() {
        Assert.assertEquals("Mental, cognitive and intellectual function assessment", UcActivityType.MENTAL_ASSESSMENT.getName());
    }

    @Test
    public void testPhysicalDisabilitiesAnswersExtractor() {

        Function<SscsCaseData, List<String>> answersExtractor = UcActivityType.PHYSICAL_DISABILITIES.getAnswersExtractor();
        Assert.assertNotNull(answersExtractor);
        Mockito.when(sscsUcCaseData.getUcWriteFinalDecisionPhysicalDisabilitiesQuestion()).thenReturn(Arrays.asList("physicalDisabilitiesAnswer1", "physicalDisabilitiesAnswer2"));
        SscsCaseData sscsCaseData = SscsCaseData.builder().sscsUcCaseData(sscsUcCaseData).build();
        List<String> answers = answersExtractor.apply(sscsCaseData);
        Assert.assertEquals(Arrays.asList("physicalDisabilitiesAnswer1", "physicalDisabilitiesAnswer2"), answers);
    }

    @Test
    public void testMentalAssessmentAnswersExtractor() {

        Function<SscsCaseData, List<String>> answersExtractor = UcActivityType.MENTAL_ASSESSMENT.getAnswersExtractor();
        Assert.assertNotNull(answersExtractor);
        SscsCaseData sscsCaseData = SscsCaseData.builder().sscsUcCaseData(sscsUcCaseData).build();
        Mockito.when(sscsUcCaseData.getUcWriteFinalDecisionMentalAssessmentQuestion()).thenReturn(Arrays.asList("mentalAssessmentAnswer1", "mentalAssessmentAnswer2"));
        List<String> answers = answersExtractor.apply(sscsCaseData);
        Assert.assertEquals(Arrays.asList("mentalAssessmentAnswer1", "mentalAssessmentAnswer2"), answers);
    }

}
