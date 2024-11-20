package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;

@RunWith(JUnitParamsRunner.class)
public class EsaActivityTypeTest {

    @Mock
    private SscsEsaCaseData sscsEsaCaseData;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testPhysicalDisablitiesName() {
        Assertions.assertEquals("Physical Disabilities", EsaActivityType.PHYSICAL_DISABILITIES.getName());
    }

    @Test
    public void testMentalAssessmentGetName() {
        Assertions.assertEquals("Mental, cognitive and intellectual function assessment", EsaActivityType.MENTAL_ASSESSMENT.getName());
    }

    @Test
    public void testPhysicalDisabilitiesAnswersExtractor() {

        Function<SscsCaseData, List<String>> answersExtractor = EsaActivityType.PHYSICAL_DISABILITIES.getAnswersExtractor();
        Assertions.assertNotNull(answersExtractor);
        Mockito.when(sscsEsaCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion()).thenReturn(Arrays.asList("physicalDisabilitiesAnswer1", "physicalDisabilitiesAnswer2"));
        SscsCaseData sscsCaseData = SscsCaseData.builder().sscsEsaCaseData(sscsEsaCaseData).build();
        List<String> answers = answersExtractor.apply(sscsCaseData);
        Assertions.assertEquals(Arrays.asList("physicalDisabilitiesAnswer1", "physicalDisabilitiesAnswer2"), answers);
    }

    @Test
    public void testMentalAssessmentAnswersExtractor() {

        Function<SscsCaseData, List<String>> answersExtractor = EsaActivityType.MENTAL_ASSESSMENT.getAnswersExtractor();
        Assertions.assertNotNull(answersExtractor);
        SscsCaseData sscsCaseData = SscsCaseData.builder().sscsEsaCaseData(sscsEsaCaseData).build();
        Mockito.when(sscsEsaCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion()).thenReturn(Arrays.asList("mentalAssessmentAnswer1", "mentalAssessmentAnswer2"));
        List<String> answers = answersExtractor.apply(sscsCaseData);
        Assertions.assertEquals(Arrays.asList("mentalAssessmentAnswer1", "mentalAssessmentAnswer2"), answers);
    }

}
