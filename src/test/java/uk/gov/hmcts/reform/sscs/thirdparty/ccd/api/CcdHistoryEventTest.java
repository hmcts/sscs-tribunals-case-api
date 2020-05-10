package uk.gov.hmcts.reform.sscs.thirdparty.ccd.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

public class CcdHistoryEventTest {
    @Test
    public void canGetEventTypeFromCcdEventType() {
        CcdHistoryEvent ccdHistoryEvent = new CcdHistoryEvent(
                EventType.COH_ONLINE_HEARING_RELISTED.getCcdType()
        );

        assertThat(ccdHistoryEvent.getEventType(), is(EventType.COH_ONLINE_HEARING_RELISTED));
    }

}
