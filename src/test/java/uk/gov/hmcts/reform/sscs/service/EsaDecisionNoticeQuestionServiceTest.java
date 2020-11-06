package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityQuestionKey;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityType;

public class EsaDecisionNoticeQuestionServiceTest extends DecisionNoticeQuestionServiceTestBase<EsaDecisionNoticeQuestionService> {

    @Override
    protected EsaDecisionNoticeQuestionService createDecisionNoticeQuestionService() throws IOException {
        return new EsaDecisionNoticeQuestionService();
    }

    @Test
    public void givenASelectedQuestionKey_thenExtractTheQuestionFromTheText() {
        EsaActivityQuestion question = service.extractQuestionFromKey(EsaActivityQuestionKey.MOBILISING_UNAIDED);
        Assert.assertNotNull(question);
        Assert.assertEquals("mobilisingUnaided", question.getKey());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", question.getValue());
        assertEquals(EsaActivityType.PHYSICAL_DISABILITIES, question.getActivityType());
    }
}