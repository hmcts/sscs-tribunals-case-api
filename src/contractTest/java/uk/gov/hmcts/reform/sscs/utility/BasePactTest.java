package uk.gov.hmcts.reform.sscs.utility;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;

public class BasePactTest {

    public  PactDslJsonBody generateHearingsJsonBody(String statusMessage, HmcStatus responseStatus) {
        PactDslJsonBody pactDslJsonBody = new PactDslJsonBody();

        addStatusMessage(pactDslJsonBody, statusMessage);

        pactDslJsonBody
            .numberType("versionNumber", 123L)
            .stringType("hearingRequestId", "1880163574")
            .stringType("status", responseStatus.toString())
            .stringMatcher("timeStamp", "2030-08-20T12:40:00")
            .asBody();

        return pactDslJsonBody;
    }

    protected  void addStatusMessage(PactDslJsonBody pactDslJsonBody, String statusMessage) {
        pactDslJsonBody
            .stringType("status", statusMessage);
    }
}
