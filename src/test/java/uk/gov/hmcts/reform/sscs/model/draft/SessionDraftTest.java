package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class SessionDraftTest {

    @Test
    public void shouldSerializeSessionDraftAsExpected() throws JsonProcessingException {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
            .postcode(new SessionPostcodeChecker("AP1 4NT"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionMrnDateDetails("01", "02", "2017")))
            .checkMrn(new SessionCheckMrn("yes"))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("Just forgot to do it"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee("no"))
            .build();

        ObjectMapper objectMapper = new ObjectMapper();

        String actual = objectMapper.writeValueAsString(sessionDraft);
        System.out.println(actual);
    }

}