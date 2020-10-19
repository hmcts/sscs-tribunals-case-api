package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

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

@RunWith(JUnitParamsRunner.class)
public class EsaActivityTypeTest {

    @Mock
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testPhysicalDisablitiesName() {
        Assert.assertEquals("Physical Disabilities", EsaActivityType.PHYSICAL_DISABLITIES.getName());
    }

    @Test
    public void testMentalAssessmentGetName() {
        Assert.assertEquals("Mental, cognitive and intellectual function assessment", EsaActivityType.MENTAL_ASSESSMENT.getName());
    }

    @Test
    public void testPhysicalDisabilitiesAnswersExtractor() {

        Function<SscsCaseData, List<String>> answersExtractor = EsaActivityType.PHYSICAL_DISABLITIES.getAnswersExtractor();
        Assert.assertNotNull(answersExtractor);

        Mockito.when(sscsCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion()).thenReturn(Arrays.asList("physicalDisabilitiesAnswer1", "physicalDisabilitiesAnswer2"));
        List<String> answers = answersExtractor.apply(sscsCaseData);
        Assert.assertEquals(Arrays.asList("physicalDisabilitiesAnswer1", "physicalDisabilitiesAnswer2"), answers);
    }

    @Test
    public void testMentalAssessmentAnswersExtractor() {

        Function<SscsCaseData, List<String>> answersExtractor = EsaActivityType.MENTAL_ASSESSMENT.getAnswersExtractor();
        Assert.assertNotNull(answersExtractor);

        Mockito.when(sscsCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion()).thenReturn(Arrays.asList("mentalAssessmentAnswer1", "mentalAssessmentAnswer2"));
        List<String> answers = answersExtractor.apply(sscsCaseData);
        Assert.assertEquals(Arrays.asList("mentalAssessmentAnswer1", "mentalAssessmentAnswer2"), answers);
    }

}
