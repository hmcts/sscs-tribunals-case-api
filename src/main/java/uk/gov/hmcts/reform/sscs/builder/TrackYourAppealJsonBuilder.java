package uk.gov.hmcts.reform.sscs.builder;

import static java.time.LocalDateTime.of;
import static java.time.LocalDateTime.parse;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.ADDRESS_LINE_1;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.ADDRESS_LINE_2;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.ADDRESS_LINE_3;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.ADJOURNED_DATE;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.ADJOURNED_HEARING_DATE_CONTACT_WEEKS;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.ADJOURNED_LETTER_RECEIVED_BY_DATE;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.ADJOURNED_LETTER_RECEIVED_MAX_DAYS;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.CONTENT_KEY;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DATE;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DECISION_LETTER_RECEIVE_BY_DATE;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DORMANT_TO_CLOSED_DURATION_IN_MONTHS;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_RESPONSE_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_RESPONSE_HEARING_CONTACT_DATE_IN_WEEKS;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.EVIDENCE_PROVIDED_BY;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.EVIDENCE_TYPE;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.GOOGLE_MAP_URL;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.HEARING_CONTACT_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.HEARING_DATETIME;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.HEARING_DECISION_LETTER_RECEIVED_MAX_DAYS;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.MAX_DWP_RESPONSE_DAYS;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.PAST_HEARING_BOOKED_IN_WEEKS;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.POSTCODE;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.TYPE;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.VENUE_NAME;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.convertLocalDateLocalTimetoUtc;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import net.objectlab.kit.datecalc.common.DateCalculator;
import net.objectlab.kit.datecalc.jdk8.LocalDateKitCalculatorsFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.service.event.PaperCaseEventFilterUtil;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;

@Service
public class TrackYourAppealJsonBuilder {
    public static final String ORAL = "oral";
    private static final Logger LOG = getLogger(TrackYourAppealJsonBuilder.class);
    public static final String YES = "Yes";
    public static final String PAPER = "paper";

    public ObjectNode build(SscsCaseData caseData,
                            RegionalProcessingCenter regionalProcessingCenter, Long caseId) {
        return build(caseData, regionalProcessingCenter,caseId, false, null);
    }

    public ObjectNode build(SscsCaseData caseData,
                            RegionalProcessingCenter regionalProcessingCenter, Long caseId, boolean mya, String state) {

        LOG.info("CaseNode=" + caseData.toString());

        // Create appealReceived eventType for appealCreated CCD event

        LOG.info("aseData.getCaseCreated()" + caseData.getCaseCreated());

        List<Event> eventList = caseData.getEvents();
        if (eventList == null || eventList.isEmpty()) {
            if (caseData.getCaseCreated() != null) {
                caseData = createAppealReceivedEventTypeForAppealCreatedEvent(caseData);
            } else {
                String message = "No events exist for this appeal with case id: " + caseId;
                CcdException ccdException = new CcdException(message);
                LOG.error(message, ccdException);
                throw ccdException;
            }
        }
        createEvidenceResponseEvents(caseData);
        caseData.getEvents().removeIf(a -> a.getValue().getDate() == null);
        eventList = caseData.getEvents();
        eventList.sort(Comparator.reverseOrder());
        processExceptions(eventList, getHearingType(caseData).equals(PAPER));

        if (getHearingType(caseData).equals(PAPER)) {
            PaperCaseEventFilterUtil.removeNonPaperCaseEvents(eventList);
        }

        

        ObjectNode caseNode = JsonNodeFactory.instance.objectNode();
        caseNode.put("caseId", String.valueOf(caseId));
        caseNode.put("caseReference", caseData.getCaseReference());
        Subscription appellantSubscription = caseData.getSubscriptions().getAppellantSubscription();
        if (appellantSubscription != null) {
            caseNode.put("appealNumber", appellantSubscription.getTya());
        }
        if (mya) {
            LOG.info("Is MYA case with state " + state);
            List<String> appealReceivedStates = Arrays.asList("incompleteApplication",
                    "incompleteApplicationInformationReqsted", "interlocutoryReviewState", "pendingAppeal");

            List<String> withDwpStates = Arrays.asList("appealCreated", "validAppeal", "withDwp");

            List<String> dwpRespondStates = Arrays.asList("readyToList", "responseReceived");

            List<String> hearingStates = Arrays.asList("hearing", "outcome");

            List<String> closedStates = Arrays.asList("closed", "incompleteApplicationVoidState",
                    "voidState", "dormantAppealState");

            if (appealReceivedStates.contains(state)) {
                caseNode.put("status", "APPEAL_RECEIVED");
            } else if (withDwpStates.contains(state)) {
                caseNode.put("status", "WITH_DWP");
            } else if (dwpRespondStates.contains(state)) {
                caseNode.put("status", "DWP_RESPOND");
            } else if (hearingStates.contains(state)) {
                caseNode.put("status", "HEARING_BOOKED");
            }  else if (closedStates.contains(state)) {
                caseNode.put("status", "CLOSED");
            }

        } else {
            caseNode.put("status", getAppealStatus(caseData.getEvents()));
        }
        caseNode.put("benefitType", caseData.getAppeal().getBenefitType().getCode().toLowerCase());
        caseNode.put("hearingType", getHearingType(caseData));
        if (StringUtils.isNotBlank(caseData.getCreatedInGapsFrom())) {
            caseNode.put("createdInGapsFrom", caseData.getCreatedInGapsFrom());
        }

        if (caseData.getAppeal().getAppellant() != null) {
            caseNode.put("name", caseData.getAppeal().getAppellant().getName().getFullName());
            caseNode.put("surname", caseData.getAppeal().getAppellant().getName().getLastName());
            if (caseData.getAppeal().getAppellant().getContact() != null) {
                caseNode.set("contact", getContactNode(caseData));
            }
        }

        boolean isDigitalCase = isCaseStateReadyToList(caseData);


        List<Event> latestEvents = buildLatestEvents(caseData.getEvents());


        Map<Event, Document> eventDocumentMap = buildEventDocumentMap(caseData);
        Map<Event, Hearing> eventHearingMap = buildEventHearingMap(caseData);
        caseNode.set("latestEvents", buildEventArray(latestEvents, eventDocumentMap, eventHearingMap));
        List<Event> historicalEvents = buildHistoricalEvents(caseData.getEvents(), latestEvents);
        if (!historicalEvents.isEmpty()) {
            caseNode.set("historicalEvents", buildEventArray(historicalEvents, eventDocumentMap, eventHearingMap));
        }

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        processRpcDetails(regionalProcessingCenter, caseNode, isDigitalCase);
        root.set("appeal", caseNode);

        root.set("subscriptions", buildSubscriptions(caseData.getSubscriptions()));

        return root;
    }

    private ObjectNode getContactNode(SscsCaseData caseData) {
        Contact contact = caseData.getAppeal().getAppellant().getContact();

        ObjectNode contactNode = JsonNodeFactory.instance.objectNode();

        if (contact.getPhone() != null) {
            contactNode.put("phone", contact.getPhone());
        }
        if (contact.getEmail() != null) {
            contactNode.put("email", contact.getEmail());
        }
        if (contact.getMobile() != null) {
            contactNode.put("mobile", contact.getMobile());
        }
        return contactNode;
    }

    private ObjectNode buildSubscriptions(Subscriptions subscriptions) {
        ObjectNode subscriptionsNode = JsonNodeFactory.instance.objectNode();

        if (subscriptions != null) {
            addSubscription(subscriptionsNode, subscriptions.getAppellantSubscription(), "Appellant");
            addSubscription(subscriptionsNode, subscriptions.getAppointeeSubscription(), "Appointee");
            addSubscription(subscriptionsNode, subscriptions.getRepresentativeSubscription(), "Representative");
            addSubscription(subscriptionsNode, subscriptions.getSupporterSubscription(), "Supporter");
        }

        return subscriptionsNode;
    }

    private void addSubscription(ObjectNode subscriptionsNode, Subscription subscription, String type) {
        if (subscription != null && (subscription.isEmailSubscribed() || subscription.isSmsSubscribed())) {
            ObjectNode subscriptionNode = JsonNodeFactory.instance.objectNode();

            if (subscription.isEmailSubscribed()) {
                subscriptionNode.put("email", subscription.getEmail());
            }
            if (subscription.isSmsSubscribed()) {
                subscriptionNode.put("mobile", subscription.getMobile());
            }
            subscriptionsNode.set(type, subscriptionNode);
        }
    }

    private String getHearingType(SscsCaseData caseData) {
        if (null != caseData.getAppeal().getHearingType()) {
            return caseData.getAppeal().getHearingType();
        }

        if (null != caseData.getAppeal().getHearingOptions()) {

            String wantsToAttend = caseData.getAppeal().getHearingOptions().getWantsToAttend();

            if (null != wantsToAttend) {
                return wantsToAttend.equals(YES) ? ORAL : PAPER;
            }
        }
        return ORAL;
    }

    private void processExceptions(List<Event> eventList, Boolean isPaperCase) {
        if (null != eventList && !eventList.isEmpty()) {

            Event currentEvent = eventList.get(0);

            if (isPastHearingBookedDate(currentEvent, isPaperCase)) {
                setLatestEventAs(eventList, currentEvent, PAST_HEARING_BOOKED);
            } else if (isNewHearingBookedEvent(eventList)) {
                setLatestEventAs(eventList, currentEvent, NEW_HEARING_BOOKED);
            } else if (isAppealClosed(currentEvent)) {
                setLatestEventAs(eventList, currentEvent, CLOSED);
            } else if (isDwpRespondOverdue(currentEvent)) {
                setLatestEventAs(eventList, currentEvent, DWP_RESPOND_OVERDUE);
            }
        }
    }

    private void setLatestEventAs(List<Event> eventList, Event currentEvent, EventType eventType) {
        Event event = Event.builder().value(currentEvent.getValue().toBuilder().type(eventType
            .getCcdType()).build()).build();
        eventList.set(0, event);
    }

    private boolean isPastHearingBookedDate(Event event, Boolean isPaperCase) {
        return DWP_RESPOND.equals(getEventType(event))
            && LocalDateTime.now().isAfter(
            LocalDateTime.parse(event.getValue().getDate()).plusWeeks(PAST_HEARING_BOOKED_IN_WEEKS))
            && !isPaperCase;
    }

    private boolean isNewHearingBookedEvent(List<Event> eventList) {
        return eventList.size() > 1
            && HEARING_BOOKED.equals(getEventType(eventList.get(0)))
            && (POSTPONED.equals(getEventType(eventList.get(1)))
            || ADJOURNED.equals(getEventType(eventList.get(1))));
    }

    private boolean isAppealClosed(Event event) {
        return DORMANT.equals(getEventType(event))
            && LocalDateTime.now().isAfter(
            LocalDateTime.parse(event.getValue().getDate()).plusMonths(
                DORMANT_TO_CLOSED_DURATION_IN_MONTHS));
    }

    private boolean isDwpRespondOverdue(Event event) {
        return APPEAL_RECEIVED.equals(getEventType(event))
            && LocalDateTime.now().isAfter(LocalDateTime.parse(event.getValue().getDate()).plusDays(
            MAX_DWP_RESPONSE_DAYS));
    }

    private ArrayNode buildEventArray(List<Event> events, Map<Event, Document> eventDocumentMap, Map<Event, Hearing> eventHearingMap) {

        ArrayNode eventsNode = JsonNodeFactory.instance.arrayNode();

        for (Event event : events) {
            ObjectNode eventNode = JsonNodeFactory.instance.objectNode();

            eventNode.put(DATE, getUtcDate((event)));
            eventNode.put(TYPE, getEventType(event).toString());
            eventNode.put(CONTENT_KEY, "status." + getEventType(event).getType());

            buildEventNode(event, eventNode, eventDocumentMap, eventHearingMap);

            eventsNode.add(eventNode);
        }

        return eventsNode;
    }

    private List<Event> buildLatestEvents(List<Event> events) {
        List<Event> latestEvents = new ArrayList<>();

        for (Event event : events) {
            if (EVIDENCE_RECEIVED.equals(getEventType(event))) {
                latestEvents.add(event);
            } else {
                latestEvents.add(event);
                break;
            }
        }

        return latestEvents;
    }

    private boolean isCaseStateReadyToList(SscsCaseData caseData) {
        return State.READY_TO_LIST.toString().equals(caseData.getCreatedInGapsFrom()) ? true : false;
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

    private void buildEventNode(Event event, ObjectNode eventNode, Map<Event, Document> eventDocumentMap, Map<Event, Hearing> eventHearingMap) {

        switch (getEventType(event)) {
            case APPEAL_RECEIVED:
            case DWP_RESPOND_OVERDUE:
                eventNode.put(DWP_RESPONSE_DATE_LITERAL, getCalculatedDate(event, MAX_DWP_RESPONSE_DAYS, true));
                break;
            case EVIDENCE_RECEIVED:
                Document document = eventDocumentMap.get(event);
                if (document != null) {
                    eventNode.put(EVIDENCE_TYPE, document.getValue().getEvidenceType());
                    eventNode.put(EVIDENCE_PROVIDED_BY, document.getValue().getEvidenceProvidedBy());
                }
                break;
            case DWP_RESPOND:
            case PAST_HEARING_BOOKED:
            case POSTPONED:
                eventNode.put(HEARING_CONTACT_DATE_LITERAL, getCalculatedDate(event,
                    DWP_RESPONSE_HEARING_CONTACT_DATE_IN_WEEKS, false));
                break;
            case HEARING_BOOKED:
            case NEW_HEARING_BOOKED:
                Hearing hearing = eventHearingMap.get(event);
                if (hearing != null) {
                    eventNode.put(POSTCODE, hearing.getValue().getVenue().getAddress().getPostcode());
                    eventNode.put(HEARING_DATETIME,
                        convertLocalDateLocalTimetoUtc(hearing.getValue().getHearingDate(), hearing.getValue().getTime()));
                    eventNode.put(VENUE_NAME, hearing.getValue().getVenue().getName());
                    eventNode.put(ADDRESS_LINE_1, hearing.getValue().getVenue().getAddress().getLine1());
                    eventNode.put(ADDRESS_LINE_2, hearing.getValue().getVenue().getAddress().getLine2());
                    eventNode.put(ADDRESS_LINE_3, hearing.getValue().getVenue().getAddress().getTown());
                    eventNode.put(GOOGLE_MAP_URL, hearing.getValue().getVenue().getGoogleMapLink());
                }
                break;
            case ADJOURNED:
                eventNode.put(ADJOURNED_DATE, getUtcDate((event)));
                eventNode.put(HEARING_CONTACT_DATE_LITERAL, getCalculatedDate(event,
                    ADJOURNED_HEARING_DATE_CONTACT_WEEKS, false));
                eventNode.put(ADJOURNED_LETTER_RECEIVED_BY_DATE, getCalculatedDate(event,
                    ADJOURNED_LETTER_RECEIVED_MAX_DAYS, true));
                eventNode.put(HEARING_CONTACT_DATE_LITERAL, getCalculatedDate(event,
                    ADJOURNED_HEARING_DATE_CONTACT_WEEKS, false));
                break;
            case DORMANT:
            case HEARING:
                eventNode.put(DECISION_LETTER_RECEIVE_BY_DATE, getBusinessDay(event,
                    HEARING_DECISION_LETTER_RECEIVED_MAX_DAYS));
                break;
            default:
                break;
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

    private String formatDateTime(LocalDateTime localDateTime) {
        return DateTimeUtils.convertLocalDateTimetoUtc(localDateTime);
    }

    private void processRpcDetails(RegionalProcessingCenter regionalProcessingCenter, ObjectNode caseNode, boolean isDigitalCase) {
        if (null != regionalProcessingCenter) {
            ObjectNode rpcNode = JsonNodeFactory.instance.objectNode();

            rpcNode.put("name", (isDigitalCase) ? "HMCTS SSCS" : regionalProcessingCenter.getName());
            rpcNode.set("addressLines", (isDigitalCase) ? buildDigitalCaseRpcAddressArray() : buildRpcAddressArray(regionalProcessingCenter));
            rpcNode.put("city", (isDigitalCase) ? "Harlow" : regionalProcessingCenter.getCity());
            rpcNode.put("postcode", (isDigitalCase) ? "CM20 9QF" : regionalProcessingCenter.getPostcode());
            rpcNode.put("phoneNumber", (isDigitalCase) ? "" : regionalProcessingCenter.getPhoneNumber());
            rpcNode.put("faxNumber", (isDigitalCase) ? "" : regionalProcessingCenter.getFaxNumber());

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

    private ArrayNode buildDigitalCaseRpcAddressArray() {
        ArrayNode rpcAddressArray = JsonNodeFactory.instance.arrayNode();

        rpcAddressArray.add("HMCTS SSCS");
        rpcAddressArray.add("PO BOX 12626");

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

    private void createEvidenceResponseEvents(SscsCaseData caseData) {
        Evidence evidence = caseData.getEvidence();
        List<Document> documentList = evidence != null ? evidence.getDocuments() : null;

        if (documentList != null && !documentList.isEmpty()) {
            List<Event> events = new ArrayList<>();
            for (Document document : documentList) {
                if (document != null && document.getValue() != null) {
                    EventDetails eventDetails = EventDetails.builder()
                        .date(LocalDate.parse(document.getValue().getDateReceived()).atStartOfDay().plusHours(1)
                            .toString())
                        .type(EventType.EVIDENCE_RECEIVED.getCcdType())
                        .description("Evidence received")
                        .build();

                    events.add(Event.builder()
                        .value(eventDetails)
                        .build());
                }
            }
            caseData.getEvents().addAll(events);
        }
    }

    private Map<Event, Document> buildEventDocumentMap(SscsCaseData caseData) {

        Map<Event, Document> eventDocumentMap = new HashMap<>();
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

        return eventDocumentMap;
    }

    private Map<Event, Hearing> buildEventHearingMap(SscsCaseData caseData) {

        Map<Event, Hearing> eventHearingMap = new HashMap<>();
        List<Hearing> hearingList = caseData.getHearings();

        if (hearingList != null && !hearingList.isEmpty()) {
            List<Event> events = caseData.getEvents();

            hearingList.sort(Comparator.reverseOrder());

            if (null != events && !events.isEmpty()) {
                int hearingIndex = 0;
                for (Event event : events) {
                    if (HEARING_BOOKED.equals(getEventType(event))
                        || NEW_HEARING_BOOKED.equals(getEventType(event))) {
                        if (hearingIndex < hearingList.size()) {
                            eventHearingMap.put(event, hearingList.get(hearingIndex));
                            hearingIndex++;
                        }
                    }
                }
            }
        }

        return eventHearingMap;
    }

    private SscsCaseData createAppealReceivedEventTypeForAppealCreatedEvent(SscsCaseData caseData) {

        EventDetails eventDetails = EventDetails.builder()
            .date(LocalDate.parse(caseData.getCaseCreated()).atStartOfDay().plusHours(1).toString())
            .type(EventType.APPEAL_RECEIVED.getCcdType())
            .description("Appeal received")
            .build();

        Event event = Event.builder()
            .value(eventDetails)
            .build();

        List<Event> events = new ArrayList<>();
        events.add(event);

        return caseData.toBuilder().events(events).build();

    }

}
