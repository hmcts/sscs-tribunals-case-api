package uk.gov.hmcts.sscs.builder;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.json.JSONObject;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.corecase.Event;

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

            eventNode.put("date", formatter.format(event.getDate()));
            eventNode.put("type", event.getType().toString());
            eventNode.put("contentKey", event.getType().getContentKey());

            if (event.getPlaceholders() != null) {
                ObjectNode placeholderNode = JsonNodeFactory.instance.objectNode();

                JSONObject json = new JSONObject(event.getPlaceholders());
                if (json.has("hearing_date_time")) {
                    eventNode.put("hearingDateTime", json.get("hearing_date_time").toString());
                }
                if (json.has("venue_name")) {
                    eventNode.put("venueName", json.get("venue_name").toString());
                }
                if (json.has("address_line1")) {
                    eventNode.put("addressLine1", json.get("address_line1").toString());
                }
                if (json.has("address_line2")) {
                    eventNode.put("addressLine2", json.get("address_line2").toString());
                }
                if (json.has("address_line3")) {
                    eventNode.put("addressLine3", json.get("address_line3").toString());
                }
                if (json.has("postcode")) {
                    eventNode.put("postcode", json.get("postcode").toString());
                }
                if (json.has("google_map_url")) {
                    eventNode.put("googleMapUrl", json.get("google_map_url").toString());
                }
                if (json.has("evidence_type")) {
                    eventNode.put("evidenceType", json.get("evidence_type").toString());
                }
                if (json.has("evidence_provided_by")) {
                    eventNode.put("evidenceProvidedBy",
                            json.get("evidence_provided_by").toString());
                }
                if (json.has("dwpResponseDate")) {
                    eventNode.put("dwpResponseDate", json.get("dwpResponseDate").toString());
                }
                if (json.has("hearing_contact_date")) {
                    eventNode.put("hearingContactDate",
                            json.get("hearing_contact_date").toString());
                }
                if (json.has("adjournedLetterReceivedByDate")) {
                    eventNode.put("adjournedLetterReceivedByDate",
                            json.get("adjournedLetterReceivedByDate").toString());
                }
                if (json.has("decisionLetterReceiveByDate")) {
                    eventNode.put("decisionLetterReceiveByDate",
                            json.get("decisionLetterReceiveByDate").toString());
                }
            }

            latestEvents.add(eventNode);
        }

        return latestEvents;
    }
}
