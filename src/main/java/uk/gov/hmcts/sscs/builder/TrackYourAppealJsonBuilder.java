package uk.gov.hmcts.sscs.builder;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.json.JSONObject;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.corecase.Event;

import static uk.gov.hmcts.sscs.model.AppConstants.*;

public class TrackYourAppealJsonBuilder {

    private TrackYourAppealJsonBuilder() {

    }

    public static ObjectNode buildTrackYourAppealJson(CcdCase ccdCase) {

        ObjectNode caseNode = JsonNodeFactory.instance.objectNode();
        caseNode.put("caseReference", ccdCase.getCaseReference());
        caseNode.put("appealNumber", ccdCase.getAppeal().getAppealNumber());
        caseNode.put("status", ccdCase.getAppealStatus());
        caseNode.put("benefitType", ccdCase.getBenefitType().toLowerCase());

        if (ccdCase.getAppellant() != null) {
            caseNode.put("name", ccdCase.getAppellant().getName().getFullName());
        }

        caseNode.set("latestEvents", buildEventArray(ccdCase.buildLatestEvents()));
        caseNode.set("historicalEvents", buildEventArray(ccdCase.buildHistoricalEvents()));

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("appeal", caseNode);

        return root;
    }

    private static ArrayNode buildEventArray(List<Event> events) {

        ArrayNode latestEvents = JsonNodeFactory.instance.arrayNode();

        for (Event event: events) {
            ObjectNode eventNode = JsonNodeFactory.instance.objectNode();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'");

            eventNode.put(DATE, formatter.format(event.getDate()));
            eventNode.put(TYPE, event.getType().toString());
            eventNode.put(CONTENT_KEY, event.getType().getContentKey());

            if (event.getPlaceholders() != null) {
                eventNode = buildPlaceholderArray(event, eventNode);
            }

            latestEvents.add(eventNode);
        }

        return latestEvents;
    }

    public static ObjectNode buildPlaceholderArray(Event event, ObjectNode eventNode) {
        JSONObject json = new JSONObject(event.getPlaceholders());
        if (json.has(HEARING_DATETIME)) {
            eventNode.put(HEARING_DATETIME, json.get(HEARING_DATETIME).toString());
        }
        if (json.has(VENUE_NAME)) {
            eventNode.put(VENUE_NAME, json.get(VENUE_NAME).toString());
        }
        if (json.has(ADDRESS_LINE_1)) {
            eventNode.put(ADDRESS_LINE_1, json.get(ADDRESS_LINE_1).toString());
        }
        if (json.has(ADDRESS_LINE_2)) {
            eventNode.put(ADDRESS_LINE_2, json.get(ADDRESS_LINE_2).toString());
        }
        if (json.has(ADDRESS_LINE_3)) {
            eventNode.put(ADDRESS_LINE_3, json.get(ADDRESS_LINE_3).toString());
        }
        if (json.has(POSTCODE)) {
            eventNode.put(POSTCODE, json.get(POSTCODE).toString());
        }
        if (json.has(GOOGLE_MAP_URL)) {
            eventNode.put(GOOGLE_MAP_URL, json.get(GOOGLE_MAP_URL).toString());
        }
        if (json.has(EVIDENCE_TYPE)) {
            eventNode.put(EVIDENCE_TYPE, json.get(EVIDENCE_TYPE).toString());
        }
        if (json.has(EVIDENCE_PROVIDED_BY)) {
            eventNode.put(EVIDENCE_PROVIDED_BY,
                    json.get(EVIDENCE_PROVIDED_BY).toString());
        }
        if (json.has(DWP_RESPONSE_DATE_LITERAL)) {
            eventNode.put(DWP_RESPONSE_DATE_LITERAL, json.get(DWP_RESPONSE_DATE_LITERAL).toString());
        }
        if (json.has(HEARING_CONTACT_DATE_LITERAL)) {
            eventNode.put(HEARING_CONTACT_DATE_LITERAL,
                    json.get(HEARING_CONTACT_DATE_LITERAL).toString());
        }
        if (json.has(ADJOURNED_LETTER_RECEIVED_BY_DATE)) {
            eventNode.put(ADJOURNED_LETTER_RECEIVED_BY_DATE,
                    json.get(ADJOURNED_LETTER_RECEIVED_BY_DATE).toString());
        }
        if (json.has(DECISION_LETTER_RECEIVE_BY_DATE)) {
            eventNode.put(DECISION_LETTER_RECEIVE_BY_DATE,
                    json.get(DECISION_LETTER_RECEIVE_BY_DATE).toString());
        }

        return eventNode;
    }
}
