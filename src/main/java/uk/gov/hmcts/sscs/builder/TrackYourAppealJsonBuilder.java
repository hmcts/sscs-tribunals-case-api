package uk.gov.hmcts.sscs.builder;

import static java.time.LocalDateTime.of;
import static java.time.LocalDateTime.parse;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.sscs.domain.corecase.EventType.EVIDENCE_RECEIVED;
import static uk.gov.hmcts.sscs.model.AppConstants.*;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import net.objectlab.kit.datecalc.common.DateCalculator;
import net.objectlab.kit.datecalc.jdk8.LocalDateKitCalculatorsFactory;

import uk.gov.hmcts.sscs.domain.corecase.EventType;
import uk.gov.hmcts.sscs.model.ccd.*;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;

public class TrackYourAppealJsonBuilder {

    private TrackYourAppealJsonBuilder() {

    }

    public static ObjectNode buildTrackYourAppealJson(CaseData caseData,
                                                      RegionalProcessingCenter regionalProcessingCenter) {

        Collections.sort(caseData.getEvents(), Comparator.reverseOrder());

        ObjectNode caseNode = JsonNodeFactory.instance.objectNode();
        caseNode.put("caseReference", caseData.getCaseReference());
        caseNode.put("appealNumber", caseData.getSubscriptions().getAppellantSubscription().getTya());
        caseNode.put("status", getAppealStatus(caseData.getEvents()));
        caseNode.put("benefitType", caseData.getAppeal().getBenefitType().getCode().toLowerCase());

        if (caseData.getAppeal().getAppellant() != null) {
            caseNode.put("name", caseData.getAppeal().getAppellant().getName().getFullName());
            caseNode.put("surname", caseData.getAppeal().getAppellant().getName().getLastName());
        }

        List<Event> latestEvents = buildLatestEvents(caseData.getEvents());
        caseNode.set("latestEvents", buildEventArray(latestEvents, caseData));
        List<Event> historicalEvents = buildHistoricalEvents(caseData.getEvents(), latestEvents);
        if (!historicalEvents.isEmpty()) {
            caseNode.set("historicalEvents", buildEventArray(historicalEvents, caseData));
        }

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        processRpcDetails(regionalProcessingCenter, caseNode);
        root.set("appeal", caseNode);

        return root;
    }

    private static ArrayNode buildEventArray(List<Event> events, CaseData caseData) {

        ArrayNode eventsNode = JsonNodeFactory.instance.arrayNode();

        for (Event event: events) {
            ObjectNode eventNode = JsonNodeFactory.instance.objectNode();

            eventNode.put(DATE, getUtcDate((event)));
            eventNode.put(TYPE, getEventType(event).toString());
            eventNode.put(CONTENT_KEY,"status." + getEventType(event).getType());

            buildEventNode(event, eventNode, caseData);

            eventsNode.add(eventNode);
        }

        return eventsNode;
    }

    private static List<Event> buildLatestEvents(List<Event> events) {
        List<Event> latestEvents = new ArrayList<>();

        for (Event event: events) {
            if (EVIDENCE_RECEIVED.equals(getEventType(event))) {
                latestEvents.add(event);
            } else {
                latestEvents.add(event);
                break;
            }
        }

        return latestEvents;
    }

    private static List<Event> buildHistoricalEvents(List<Event> events, List<Event> latestEvents) {

        return events.stream().skip(latestEvents.size()).collect(toList());

    }

    private static String getAppealStatus(List<Event> events) {
        String appealStatus = "";

        if (null != events && !events.isEmpty()) {
            for (Event event : events) {
                if (getEventType(event).getOrder() > 0) {
                    appealStatus = getEventType(event).toString();
                    break;
                }
            }
        }
        return appealStatus;
    }

    private static void buildEventNode(Event event, ObjectNode eventNode, CaseData caseData) {

        switch (getEventType(event)) {
            case APPEAL_RECEIVED :
            case DWP_RESPOND_OVERDUE :
                eventNode.put(DWP_RESPONSE_DATE_LITERAL, getCalculatedDate(event, MAX_DWP_RESPONSE_DAYS, true));
                break;
            case EVIDENCE_RECEIVED :
                Document document = getDocument(event, caseData);
                if (document != null) {
                    eventNode.put(EVIDENCE_TYPE, document.getValue().getEvidenceType());
                    eventNode.put(EVIDENCE_PROVIDED_BY, document.getValue().getEvidenceProvidedBy());
                }
                break;
            case DWP_RESPOND :
            case PAST_HEARING_BOOKED :
            case POSTPONED :
                eventNode.put(HEARING_CONTACT_DATE_LITERAL, getCalculatedDate(event,
                        DAYS_FROM_DWP_RESPONSE_DATE_FOR_HEARING_CONTACT, true));
                break;
            case HEARING_BOOKED :
            case NEW_HEARING_BOOKED :
                Hearing hearing = getHearing(event, caseData.getHearings());
                hearing = (hearing == null) ? caseData.getHearings().get(0) : hearing;
                if (hearing != null) {
                    eventNode.put(POSTCODE, hearing.getValue().getVenue().getAddress().getPostcode());
                    eventNode.put(HEARING_DATETIME,
                            getHearingDateTime(hearing.getValue().getHearingDate(), hearing.getValue().getTime()));
                    eventNode.put(VENUE_NAME, hearing.getValue().getVenue().getName());
                    eventNode.put(ADDRESS_LINE_1, hearing.getValue().getVenue().getAddress().getLine1());
                    eventNode.put(ADDRESS_LINE_2, hearing.getValue().getVenue().getAddress().getLine2());
                    eventNode.put(ADDRESS_LINE_3, hearing.getValue().getVenue().getAddress().getTown());
                    eventNode.put(GOOGLE_MAP_URL, hearing.getValue().getVenue().getGoogleMapLink());
                }
                break;
            case ADJOURNED :
                eventNode.put(ADJOURNED_DATE, getUtcDate((event)));
                eventNode.put(HEARING_CONTACT_DATE_LITERAL, getCalculatedDate(event,
                        HEARING_DATE_CONTACT_WEEKS, false));
                eventNode.put(ADJOURNED_LETTER_RECEIVED_BY_DATE, getCalculatedDate(event,
                        ADJOURNED_LETTER_RECEIVED_MAX_DAYS, true));
                eventNode.put(HEARING_CONTACT_DATE_LITERAL, getCalculatedDate(event,
                        HEARING_DATE_CONTACT_WEEKS, false));
                break;
            case DORMANT :
            case HEARING :
                eventNode.put(DECISION_LETTER_RECEIVE_BY_DATE, getBusinessDay(event,
                        HEARING_DECISION_LETTER_RECEIVED_MAX_DAYS));
                break;
            default: break;
        }
    }

    private static EventType getEventType(Event event) {
        return EventType.getEventTypeByCcdType(event.getValue().getType());
    }

    private static String getUtcDate(Event event) {
        return formatDateTime(parse(event.getValue().getDate()));
    }

    private static String getCalculatedDate(Event event, int days, boolean isDays) {
        if (isDays) {
            return formatDateTime(parse(event.getValue().getDate()).plusDays(days));
        } else {
            return formatDateTime(parse(event.getValue().getDate()).plusWeeks(days));
        }
    }

    private static String getHearingDateTime(String localDate, String localTime) {
        return formatDateTime(LocalDateTime.of(LocalDate.parse(localDate), LocalTime.parse(localTime)));
    }

    private static String formatDateTime(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + "T"
                + localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS'Z'"));
    }

    private static void processRpcDetails(RegionalProcessingCenter regionalProcessingCenter, ObjectNode caseNode) {
        if (null != regionalProcessingCenter) {
            ObjectNode rpcNode = JsonNodeFactory.instance.objectNode();

            rpcNode.put("name", regionalProcessingCenter.getName());
            rpcNode.set("addressLines", buildRpcAddressArray(regionalProcessingCenter));
            rpcNode.put("city", regionalProcessingCenter.getCity());
            rpcNode.put("postcode", regionalProcessingCenter.getPostcode());
            rpcNode.put("phoneNumber", regionalProcessingCenter.getPhoneNumber());
            rpcNode.put("faxNumber", regionalProcessingCenter.getFaxNumber());

            caseNode.set("regionalProcessingCenter", rpcNode);
        }
    }

    private static ArrayNode buildRpcAddressArray(RegionalProcessingCenter regionalProcessingCenter) {
        ArrayNode rpcAddressArray = JsonNodeFactory.instance.arrayNode();

        rpcAddressArray.add(regionalProcessingCenter.getAddress1());
        rpcAddressArray.add(regionalProcessingCenter.getAddress2());
        rpcAddressArray.add(regionalProcessingCenter.getAddress3());
        rpcAddressArray.add(regionalProcessingCenter.getAddress4());

        return rpcAddressArray;
    }

    public static String getBusinessDay(Event event, int numberOfBusinessDays) {
        LocalDateTime localDateTime = parse(event.getValue().getDate());
        LocalDate startDate = localDateTime.toLocalDate();
        DateCalculator<LocalDate> dateCalculator = LocalDateKitCalculatorsFactory.forwardCalculator("UK");
        dateCalculator.setStartDate(startDate);
        LocalDate decisionDate = dateCalculator.moveByBusinessDays(numberOfBusinessDays).getCurrentBusinessDate();
        return formatDateTime(of(decisionDate, localDateTime.toLocalTime()));
    }

    private static Hearing getHearing(Event event, List<Hearing> hearings) {
        Optional<Hearing> optionalHearing = hearings.stream()
                .filter(hearing ->
                        hearing.getValue().getEventDate() != null && getDate(event.getValue().getDate()).equals(
                                getDate(hearing.getValue().getEventDate()))).findFirst();
        return optionalHearing.orElse(null);
    }

    private static String getDate(String localDateTime) {
        return LocalDateTime.parse(localDateTime).toLocalDate().toString();
    }

    private static Map<Event, Document> buildEventDocumentMap(CaseData caseData) {

        Map<Event, Document> eventDocumentMap = new HashMap<>();
        Evidence evidence = caseData.getEvidence();
        List<Document> documentList = evidence != null ? evidence.getDocuments() : null;

        if (documentList != null && !documentList.isEmpty()) {
            List<Event> events = caseData.getEvents();
            List<Document> documents = caseData.getEvidence().getDocuments();
            Collections.sort(documents, Comparator.reverseOrder());
            if (null != events && !events.isEmpty()) {
                int documentIndex = 0;
                for (Event event : events) {
                    if (EVIDENCE_RECEIVED.equals(getEventType(event))) {
                        eventDocumentMap.put(event, documents.get(documentIndex));
                        documentIndex++;
                    }
                }
            }
        }

        return  eventDocumentMap;
    }

    private static Document getDocument(Event event, CaseData caseData) {
        return buildEventDocumentMap(caseData).get(event);
    }
}
