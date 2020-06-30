package uk.gov.hmcts.reform.sscs.service.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

public class PaperCaseEventFilterUtilTest {

    private List<Event> eventsList;

    @Before
    public void setUp() throws Exception {
        eventsList = new ArrayList<>();
        eventsList.add(new Event(new EventDetails("2017-06-24T14:09:24.883", "appealReceived", "Appeal received")));
        eventsList.add(new Event(new EventDetails("2017-06-25T14:09:24.883", "responseReceived", "Dwp Respond")));
    }

    @Test
    public void shouldRemoveHearingBookedEventFromListOfEvents() {
        eventsList.add(new Event(new EventDetails("2017-06-26T14:11:34.013", "hearingBooked", "Hearing booked")));

        PaperCaseEventFilterUtil.removeNonPaperCaseEvents(eventsList);

        assertPaperCaseEventsAreReturned();
    }

    @Test
    public void shouldRemoveHearingEventFromListOfEvents() {
        eventsList.add(new Event(new EventDetails("2017-06-26T14:11:34.013", "hearing", "Hearing")));

        PaperCaseEventFilterUtil.removeNonPaperCaseEvents(eventsList);

        assertPaperCaseEventsAreReturned();
    }


    @Test
    public void shouldRemoveAdjournedEventFromListOfEvents() {
        eventsList.add(new Event(new EventDetails("2017-06-26T14:11:34.013", "hearingAdjourned", "Adjourned")));

        PaperCaseEventFilterUtil.removeNonPaperCaseEvents(eventsList);

        assertPaperCaseEventsAreReturned();
    }

    @Test
    public void shouldRemovePostponedEventFromListOfEvents() {
        eventsList.add(new Event(new EventDetails("2017-06-26T14:11:34.013", "hearingPostponed", "Postponed")));

        PaperCaseEventFilterUtil.removeNonPaperCaseEvents(eventsList);

        assertPaperCaseEventsAreReturned();
    }

    @Test
    public void shouldRemoveNewHearingBookedEventFromListOfEvents() {
        eventsList.add(new Event(new EventDetails("2017-06-26T14:11:34.013", "newHearingBooked", "New HearingBooked")));

        PaperCaseEventFilterUtil.removeNonPaperCaseEvents(eventsList);

        assertPaperCaseEventsAreReturned();
    }

    @Test
    public void shouldRemovePastHearingBookedEventFromListOfEvents() {
        eventsList.add(new Event(new EventDetails("2017-06-26T14:11:34.013", "pastHearingBooked", "Past HearingBooked")));

        PaperCaseEventFilterUtil.removeNonPaperCaseEvents(eventsList);

        assertPaperCaseEventsAreReturned();
    }

    private void assertPaperCaseEventsAreReturned() {
        assertThat(eventsList.size(), equalTo(2));
        assertThat(eventsList.get(0).getValue().getEventType(), equalTo(EventType.APPEAL_RECEIVED));
        assertThat(eventsList.get(1).getValue().getEventType(), equalTo(EventType.DWP_RESPOND));
    }

    @Test
    public void shouldNotRemovePaperCaseEventsFromListOfEvents() {
        eventsList.add(new Event(new EventDetails("2017-06-26T14:11:34.013", "appealDormant", "Appeal Dormant")));

        PaperCaseEventFilterUtil.removeNonPaperCaseEvents(eventsList);

        assertThat(eventsList.size(), equalTo(3));
        assertThat(eventsList.get(0).getValue().getEventType(), equalTo(EventType.APPEAL_RECEIVED));
        assertThat(eventsList.get(1).getValue().getEventType(), equalTo(EventType.DWP_RESPOND));
        assertThat(eventsList.get(2).getValue().getEventType(), equalTo(EventType.DORMANT));

    }
}