package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;

public abstract class DecisionNoticeQuestionServiceTestBase<S extends DecisionNoticeQuestionService> {

    protected S service;

    @Before
    public void setup() throws IOException {
        service = createDecisionNoticeQuestionService();
    }

    protected abstract S createDecisionNoticeQuestionService() throws IOException;

    @Test
    public void givenANonMatchedNumber_thenReturnEmptyAnswer() {
        Optional<ActivityAnswer> answer = service.extractAnswerFromSelectedValue("random");
        Assert.assertNotNull(answer);
        Assert.assertFalse(answer.isPresent());
    }

    @Test
    public void givenANullNumber_thenReturnEmptyAnswer() {
        Optional<ActivityAnswer> answer = service.extractAnswerFromSelectedValue(null);
        Assert.assertNotNull(answer);
        Assert.assertFalse(answer.isPresent());
    }
}