package uk.gov.hmcts.reform.sscs.service.event;

import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

public class PaperCaseEventFilterUtil {

    private PaperCaseEventFilterUtil() {

    }

    public static void removeNonPaperCaseEvents(List<Event> eventsList) {
        NonPaperCaseEvents[] values = NonPaperCaseEvents.values();

        for (NonPaperCaseEvents nonPaperCaseEvents: values) {
            eventsList.removeIf(event ->
                    event.getValue().getEventType() == EventType.valueOf(nonPaperCaseEvents.name()));
        }
    }
}
