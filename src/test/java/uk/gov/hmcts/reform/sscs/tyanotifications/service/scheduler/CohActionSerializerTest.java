package uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;
import org.junit.Test;

public class CohActionSerializerTest {

    @Test
    public void canSerialize() {
        int caseId = 123;
        String onlineHearingId = UUID.randomUUID().toString();
        String payload = new CohActionSerializer().serialize(new CohJobPayload(caseId, onlineHearingId));

        assertThat(payload, is(equalTo("{\"caseId\":" + caseId + ",\"onlineHearingId\":\"" + onlineHearingId + "\"}")));
    }
}
