package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityQuestionKey;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaSchedule3QuestionKey;

public class EsaDecisionNoticeQuestionServiceTest extends DecisionNoticeQuestionServiceTestBase<EsaDecisionNoticeQuestionService> {

    @Override
    protected EsaDecisionNoticeQuestionService createDecisionNoticeQuestionService() throws IOException {
        return new EsaDecisionNoticeQuestionService();
    }

    @Test
    public void givenASelectedActivityQuestionKey_thenExtractTheQuestionFromTheText() {
        EsaActivityQuestion question = service.extractQuestionFromKey(EsaActivityQuestionKey.MOBILISING_UNAIDED);
        Assert.assertNotNull(question);
        Assert.assertEquals("mobilisingUnaided", question.getKey());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", question.getValue());
        assertEquals(EsaActivityType.PHYSICAL_DISABILITIES, question.getActivityType());
    }

    @Test
    public void givenASelectedSchedule3QuestionKey_thenExtractTheQuestionFromTheText() {
        EsaActivityQuestion question = service.extractQuestionFromKey(EsaSchedule3QuestionKey.MOBILISING_UNAIDED);
        Assert.assertNotNull(question);
        Assert.assertEquals("schedule3MobilisingUnaided", question.getKey());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used. "
            + "Cannot either: (a) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (b) repeatedly mobilise 50 metres within "
            + "a reasonable timescale because of significant discomfort or exhaustion.", question.getValue());
        assertEquals(EsaActivityType.PHYSICAL_DISABILITIES, question.getActivityType());
    }
}