package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class SessionDraftTest {

    @Test
    public void shouldDeserializeSessionDraftAsExpected() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        SessionBenefitType benefitType = new SessionBenefitType("Personal Independence Payment (PIP)");
        SessionDraft sessionDraft = new SessionDraft(benefitType);
        String actual = objectMapper.writeValueAsString(sessionDraft);
        System.out.println(actual);
    }
}