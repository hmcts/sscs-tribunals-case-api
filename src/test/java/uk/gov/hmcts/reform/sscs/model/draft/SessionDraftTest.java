package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;

public class SessionDraftTest {

    @Test
    public void shouldDeserializeSessionDraftAsExpected() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        MrnDetails mrnDetails = TEST_MRN_DETAILS_WITH_DATE;

        SessionDraft sessionDraft = new SessionDraft(
            new SessionBenefitType(TEST_BENEFIT_TYPE),
            new SessionPostcodeChecker(TEST_APPELLANT_ADDRESS),
            new SessionCreateAccount(),
            new SessionHaveAMrn(mrnDetails),
            new SessionMrnDate(mrnDetails),
            new SessionCheckMrn(mrnDetails),
            new SessionMrnOverThirteenMonthsLate(mrnDetails)
        );

        String actual = objectMapper.writeValueAsString(sessionDraft);
        System.out.println(actual);
    }

    public static BenefitType TEST_BENEFIT_TYPE =
        BenefitType.builder()
            .description("Personal Independence Payment")
            .code("PIP")
            .build();

    public static Address TEST_APPELLANT_ADDRESS =
        Address.builder()
            .postcode("AP1 4NT")
            .build();

    public static MrnDetails TEST_MRN_DETAILS_WITH_DATE =
        MrnDetails.builder()
            .mrnDate("01-02-2017")
            .mrnLateReason("Just forgot to do it")
            .build();

    public static MrnDetails TEST_MRN_DETAILS_WITHOUT_DATE =
        MrnDetails.builder()
            .build();
}