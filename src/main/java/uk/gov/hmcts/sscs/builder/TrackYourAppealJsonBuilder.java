package uk.gov.hmcts.sscs.builder;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.sscs.domain.corecase.EventType.EVIDENCE_RECEIVED;
import static uk.gov.hmcts.sscs.model.AppConstants.*;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import uk.gov.hmcts.sscs.domain.corecase.EventType;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.Events;

public class TrackYourAppealJsonBuilder {

    private static final String NOT_KNOWN_IN_CCD = "NotKnownInCCD";

    private TrackYourAppealJsonBuilder() {

    }

    public static ObjectNode buildTrackYourAppealJson(CaseData caseData) {

        ObjectNode caseNode = JsonNodeFactory.instance.objectNode();
        caseNode.put("caseReference", caseData.getCaseReference());
        caseNode.put("appealNumber", NOT_KNOWN_IN_CCD);
        caseNode.put("status", getAppealStatus(caseData.getEvents()));
        caseNode.put("benefitType", caseData.getAppeal().getBenefitType().getCode().toLowerCase());

        if (caseData.getAppeal().getAppellant() != null) {
            caseNode.put("name", caseData.getAppeal().getAppellant().getName().getFullName());
            caseNode.put("surname", caseData.getAppeal().getAppellant().getName().getLastName());
        }

        List<Events> latestEvents = buildLatestEvents(caseData.getEvents());
        caseNode.set("latestEvents", buildEventArray(latestEvents));
        List<Events> historicalEvents = buildHistoricalEvents(caseData.getEvents(), latestEvents);
        if (historicalEvents.size() > 0) {
            caseNode.set("historicalEvents", buildEventArray(historicalEvents));
        }

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("appeal", caseNode);

        return root;
    }

    private static ArrayNode buildEventArray(List<Events> events) {

        ArrayNode latestEvents = JsonNodeFactory.instance.arrayNode();

        for (Events event: events) {
            ObjectNode eventNode = JsonNodeFactory.instance.objectNode();

            eventNode.put(DATE, getUtcDate((event)));
            eventNode.put(TYPE, getEventType(event).toString());
            eventNode.put(CONTENT_KEY,"status." + event.getValue().getType());

            buildEventNode(event, eventNode);

            latestEvents.add(eventNode);
        }

        return latestEvents;
    }

    private static List<Events> buildLatestEvents(List<Events> events) {
        List<Events> latestEvents = new ArrayList<>();

        for (Events event: events) {
            if (EVIDENCE_RECEIVED.equals(getEventType(event))) {
                latestEvents.add(event);
            } else {
                latestEvents.add(event);
                break;
            }
        }

        return latestEvents;
    }

    private static List<Events> buildHistoricalEvents(List<Events> events, List<Events> latestEvents) {

        List<Events> historicalEvents = events.stream().skip(latestEvents.size()).collect(toList());

        return historicalEvents;
    }

    private static String getAppealStatus(List<Events> events) {
        String appealStatus = "";

        if (null != events && !events.isEmpty()) {
            for (Events event : events) {
                if (getEventType(event).getOrder() > 0) {
                    appealStatus = getEventType(event).toString();
                    break;
                }
            }
        }
        return appealStatus;
    }

    private static void buildEventNode(Events event, ObjectNode eventNode) {

        switch (getEventType(event)) {
            case APPEAL_RECEIVED :
                eventNode.put(DWP_RESPONSE_DATE_LITERAL, getDwpResponseDate(event, MAX_DWP_RESPONSE_DAYS));
                break;
            case EVIDENCE_RECEIVED:
                eventNode.put(EVIDENCE_TYPE, event.getValue().getDescription());
                break;
            case DWP_RESPOND:
                eventNode.put(HEARING_CONTACT_DATE_LITERAL, getDwpResponseDate(event,
                        DAYS_FROM_DWP_RESPONSE_DATE_FOR_HEARING_CONTACT));
                break;
            default: break;
        }
    }

    private static EventType getEventType(Events event) {
        return EventType.getEventTypeByType(event.getValue().getType());
    }

    private static String getUtcDate(Events event) {
        return LocalDateTime.parse(event.getValue().getDate()).toLocalDate().toString() + "T00:00:00Z";
    }

    private static String getDwpResponseDate(Events event, int days) {
        return LocalDateTime.parse(event.getValue().getDate()).toLocalDate().plusDays(days).toString() + "T00:00:00Z";
    }
}
