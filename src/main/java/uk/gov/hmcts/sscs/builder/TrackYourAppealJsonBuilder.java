package uk.gov.hmcts.sscs.builder;

import static java.time.LocalDateTime.of;
import static java.time.LocalDateTime.parse;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.sscs.domain.corecase.EventType.*;
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

import org.springframework.stereotype.Service;

import uk.gov.hmcts.sscs.domain.corecase.EventType;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.*;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;
import uk.gov.hmcts.sscs.util.DateTimeUtils;

@Service
public class TrackYourAppealJsonBuilder {

    private Map<Event, Document> eventDocumentMap;
    private Map<Event, Hearing> eventHearingMap;

    public ObjectNode build(CaseData caseData,
                            RegionalProcessingCenter regionalProcessingCenter) throws CcdException {

        List<Event> eventList = caseData.getEvents();
        if (eventList == null || eventList.isEmpty()) {
            throw new CcdException("No events exists for this appeal");
        }

        eventList.sort(Comparator.reverseOrder());
        eventDocumentMap = buildEventDocumentMap(caseData);
        eventHearingMap = buildEventHearingMap(caseData);

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
        caseNode.set("latestEvents", buildEventArray(latestEvents));
        List<Event> historicalEvents = buildHistoricalEvents(caseData.getEvents(), latestEvents);
        if (!historicalEvents.isEmpty()) {
            caseNode.set("historicalEvents", buildEventArray(historicalEvents));
        }

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        processRpcDetails(regionalProcessingCenter, caseNode);
        root.set("appeal", caseNode);

        return root;
    }

    private ArrayNode buildEventArray(List<Event> events) {

        ArrayNode eventsNode = JsonNodeFactory.instance.arrayNode();

        for (Event event: events) {
            ObjectNode eventNode = JsonNodeFactory.instance.objectNode();

            eventNode.put(DATE, getUtcDate((event)));
            eventNode.put(TYPE, getEventType(event).toString());
            eventNode.put(CONTENT_KEY,"status." + getEventType(event).getType());

            buildEventNode(event, eventNode);

            eventsNode.add(eventNode);
        }

        return eventsNode;
    }

    private List<Event> buildLatestEvents(List<Event> events) {
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

    private List<Event> buildHistoricalEvents(List<Event> events, List<Event> latestEvents) {

        return events.stream().skip(latestEvents.size()).collect(toList());

    }

    private String getAppealStatus(List<Event> events) {
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

    private void buildEventNode(Event event, ObjectNode eventNode) {

        switch (getEventType(event)) {
            case APPEAL_RECEIVED :
            case DWP_RESPOND_OVERDUE :
                eventNode.put(DWP_RESPONSE_DATE_LITERAL, getCalculatedDate(event, MAX_DWP_RESPONSE_DAYS, true));
                break;
            case EVIDENCE_RECEIVED :
                Document document = eventDocumentMap.get(event);
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
                Hearing hearing = eventHearingMap.get(event);
                if (hearing != null) {
                    eventNode.put(POSTCODE, hearing.getValue().getVenue().getAddress().getPostcode());
                    eventNode.put(HEARING_DATETIME,
                            DateTimeUtils.convertLocalDateTimetoUtc(hearing.getValue().getHearingDate(), hearing.getValue().getTime()));
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

    private EventType getEventType(Event event) {
        return EventType.getEventTypeByCcdType(event.getValue().getType());
    }

    private String getUtcDate(Event event) {
        return formatDateTime(parse(event.getValue().getDate()));
    }

    private String getCalculatedDate(Event event, int days, boolean isDays) {
        if (isDays) {
            return formatDateTime(parse(event.getValue().getDate()).plusDays(days));
        } else {
            return formatDateTime(parse(event.getValue().getDate()).plusWeeks(days));
        }
    }

    private String getHearingDateTime(String localDate, String localTime) {
        return formatDateTime(LocalDateTime.of(LocalDate.parse(localDate), LocalTime.parse(localTime)));
    }

    private String formatDateTime(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + "T"
                + localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS'Z'"));
    }

    private void processRpcDetails(RegionalProcessingCenter regionalProcessingCenter, ObjectNode caseNode) {
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

    private ArrayNode buildRpcAddressArray(RegionalProcessingCenter regionalProcessingCenter) {
        ArrayNode rpcAddressArray = JsonNodeFactory.instance.arrayNode();

        rpcAddressArray.add(regionalProcessingCenter.getAddress1());
        rpcAddressArray.add(regionalProcessingCenter.getAddress2());
        rpcAddressArray.add(regionalProcessingCenter.getAddress3());
        rpcAddressArray.add(regionalProcessingCenter.getAddress4());

        return rpcAddressArray;
    }

    private String getBusinessDay(Event event, int numberOfBusinessDays) {
        LocalDateTime localDateTime = parse(event.getValue().getDate());
        LocalDate startDate = localDateTime.toLocalDate();
        DateCalculator<LocalDate> dateCalculator = LocalDateKitCalculatorsFactory.forwardCalculator("UK");
        dateCalculator.setStartDate(startDate);
        LocalDate decisionDate = dateCalculator.moveByBusinessDays(numberOfBusinessDays).getCurrentBusinessDate();
        return formatDateTime(of(decisionDate, localDateTime.toLocalTime()));
    }

    private Map<Event, Document> buildEventDocumentMap(CaseData caseData) {

        eventDocumentMap = new HashMap<>();
        Evidence evidence = caseData.getEvidence();
        List<Document> documentList = evidence != null ? evidence.getDocuments() : null;

        if (documentList != null && !documentList.isEmpty()) {
            List<Event> events = caseData.getEvents();

            documentList.sort(Comparator.reverseOrder());

            if (null != events && !events.isEmpty()) {
                int documentIndex = 0;
                for (Event event : events) {
                    if (EVIDENCE_RECEIVED.equals(getEventType(event))) {
                        eventDocumentMap.put(event, documentList.get(documentIndex));
                        documentIndex++;
                    }
                }
            }
        }

        return  eventDocumentMap;
    }

    private Map<Event, Hearing> buildEventHearingMap(CaseData caseData) {

        eventHearingMap = new HashMap<>();
        List<Hearing> hearingList = caseData.getHearings();

        if (hearingList != null && !hearingList.isEmpty()) {
            List<Event> events = caseData.getEvents();

            hearingList.sort(Comparator.reverseOrder());

            if (null != events && !events.isEmpty()) {
                int hearingIndex = 0;
                for (Event event : events) {
                    if (HEARING_BOOKED.equals(getEventType(event))
                            || NEW_HEARING_BOOKED.equals(getEventType(event))) {
                        eventHearingMap.put(event, hearingList.get(hearingIndex));
                        hearingIndex++;
                    }
                }
            }
        }

        return  eventHearingMap;
    }

}
