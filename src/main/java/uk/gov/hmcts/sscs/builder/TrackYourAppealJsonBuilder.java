package uk.gov.hmcts.sscs.builder;

import static uk.gov.hmcts.sscs.model.AppConstants.*;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
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

        caseNode.set("latestEvents", buildEventArray(caseData.getEvents()));

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

    private static EventType getEventType(Events event) {
        return EventType.getEventTypeByType(event.getValue().getType());
    }

    private static void buildEventNode(Events event, ObjectNode eventNode) {

        switch (getEventType(event)) {
            case APPEAL_RECEIVED :
                eventNode.put(DWP_RESPONSE_DATE_LITERAL,getDwpResponseDate(event));
                break;
            case EVIDENCE_RECEIVED:
                eventNode.put(EVIDENCE_TYPE, event.getValue().getDescription());
                break;
            default: break;
        }
    }

    private static String getUtcDate(Events event) {
        return LocalDateTime.parse(event.getValue().getDate()).toLocalDate().toString() + "T00:00:00Z";
    }

    private static String getDwpResponseDate(Events event) {
        return LocalDateTime.parse(event.getValue().getDate()).toLocalDate().plusDays(MAX_DWP_RESPONSE_DAYS).toString() + "T00:00:00Z";
    }
}
