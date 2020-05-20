package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class DecisionNoticeQuestionServiceTest {

    private DecisionNoticeQuestionService service;

    @Before
    public void setup() throws IOException {
        service = new DecisionNoticeQuestionService();
    }

    @Test
    public void givenASelectedAnswerForADecisionNoticeQuestion_thenExtractThePointsFromTheText() {
        int points = service.extractPointsFromSelectedValue("preparingFood1f");

        assertEquals(8, points);
    }

    @Test
    public void givenANonMatchedNumber_thenReturn0Points() {
        int points = service.extractPointsFromSelectedValue("random");

        assertEquals(0, points);
    }

    @Test
    public void givenANullNumber_thenReturn0Points() {
        int points = service.extractPointsFromSelectedValue(null);

        assertEquals(0, points);
    }
}