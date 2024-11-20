package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;

public abstract class DecisionNoticeQuestionServiceTestBase<S extends DecisionNoticeQuestionService> {

    protected S service;

    @BeforeEach
    public void setup() throws IOException {
        service = createDecisionNoticeQuestionService();
    }

    protected abstract S createDecisionNoticeQuestionService() throws IOException;

    @Test
    public void givenANonMatchedNumber_thenReturnEmptyAnswer() {
        Optional<ActivityAnswer> answer = service.extractAnswerFromSelectedValue("random");
        Assertions.assertNotNull(answer);
        Assertions.assertFalse(answer.isPresent());
    }

    @Test
    public void givenANullNumber_thenReturnEmptyAnswer() {
        Optional<ActivityAnswer> answer = service.extractAnswerFromSelectedValue(null);
        Assertions.assertNotNull(answer);
        Assertions.assertFalse(answer.isPresent());
    }
}