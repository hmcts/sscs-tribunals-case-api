package uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CohActionDeserializerTest {
    @Test
    public void canDeserializePayload() {
        String onlineHearingId = UUID.randomUUID().toString();
        long caseId = 123L;
        String payload = "{\"case_id\": " + caseId + ", \"online_hearing_id\": \"" + onlineHearingId + "\"}";
        CohJobPayload deserialize = new CohActionDeserializer().deserialize(payload);

        assertThat(deserialize.getCaseId(), is(equalTo(caseId)));
        assertThat(deserialize.getOnlineHearingId(), is(equalTo(onlineHearingId)));
    }

    @Test
    public void canSerializeThenDeserializePayload() {
        String onlineHearingId = UUID.randomUUID().toString();
        long caseId = 123L;

        CohJobPayload expectedCohJobPayload = new CohJobPayload(caseId, onlineHearingId);

        String payload = new CohActionSerializer().serialize(expectedCohJobPayload);
        CohJobPayload cohJobPayload = new CohActionDeserializer().deserialize(payload);

        assertThat(cohJobPayload, is(equalTo(expectedCohJobPayload)));
    }
}
