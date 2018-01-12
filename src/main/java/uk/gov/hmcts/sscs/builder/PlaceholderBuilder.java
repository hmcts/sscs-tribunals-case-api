package uk.gov.hmcts.sscs.builder;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.time.temporal.ChronoUnit.DAYS;
import static uk.gov.hmcts.sscs.domain.corecase.EventType.*;
import static uk.gov.hmcts.sscs.model.AppConstants.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.json.JSONObject;
import uk.gov.hmcts.sscs.domain.corecase.Event;
import uk.gov.hmcts.sscs.domain.corecase.EventType;
import uk.gov.hmcts.sscs.domain.corecase.Hearing;
import uk.gov.hmcts.sscs.service.BusinessDaysCalculatorService;

public class PlaceholderBuilder {

    private PlaceholderBuilder() {

    }

    public static JSONObject addEventPlaceHolders(Event event, JSONObject placeholdersMap,
                                                  List<Hearing> hearings) {

        EventType eventType = event.getType();
        ZonedDateTime eventDate = event.getDate();

        if (placeholdersMap != null) {

            if (APPEAL_RECEIVED.equals(eventType)) {
                placeholdersMap.put(DWP_RESPONSE_DATE_LITERAL,
                    eventDate.plus(MAX_DWP_RESPONSE_DAYS, DAYS).format(ISO_INSTANT));
            } else if (DWP_RESPOND.equals(eventType) || PAST_HEARING_BOOKED.equals(eventType)) {
                placeholdersMap.put(HEARING_CONTACT_DATE_LITERAL,
                    eventDate.plus(DAYS_FROM_DWP_RESPONSE_DATE_FOR_HEARING_CONTACT, DAYS)
                            .format(ISO_INSTANT));
            } else if (POSTPONED.equals(eventType)) {
                placeholdersMap.put(HEARING_CONTACT_DATE_LITERAL,
                    eventDate.plusWeeks(HEARING_DATE_CONTACT_WEEKS).format(ISO_INSTANT));
            } else if (ADJOURNED.equals(eventType)) {
                placeholdersMap.put("adjournedLetterReceivedByDate",
                    eventDate.plusDays(ADJOURNED_LETTER_RECEIVED_MAX_DAYS).format(ISO_INSTANT));
                placeholdersMap.put(HEARING_CONTACT_DATE_LITERAL,
                    eventDate.plusWeeks(HEARING_DATE_CONTACT_WEEKS).format(ISO_INSTANT));
            } else if (HEARING.equals(eventType) || DORMANT.equals(eventType)) {
                ZonedDateTime decisionDateTime = BusinessDaysCalculatorService.getBusinessDay(
                        eventDate, HEARING_DECISION_LETTER_RECEIVED_MAX_DAYS);
                placeholdersMap.put(DECISION_LETTER_RECEIVE_BY_DATE,
                        decisionDateTime.format(ISO_INSTANT));
            } else if (HEARING_BOOKED.equals(eventType) && hearings != null) {
                placeholdersMap = generateHearingJson(hearings, eventDate);
            }
        }
        return placeholdersMap;
    }

    private static JSONObject generateHearingJson(
            List<Hearing> hearings, ZonedDateTime eventDateSet) {

        JSONObject json = new JSONObject();

        if (hearings != null) {
            for (Hearing hearing : hearings) {

                ZonedDateTime localHearingDateTime = hearing.getDateTime()
                        .withZoneSameInstant(ZoneId.of("Europe/London"));

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                if (formatter.format(localHearingDateTime).equals(formatter.format(eventDateSet))) {
                    DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm:ss.SSS'Z'");

                    json.put(HEARING_DATETIME, formatter.format(hearing.getDateTime()
                            .withZoneSameInstant(ZoneId.of("Europe/London"))) + "T"
                            + time.format(hearing.getDateTime()));

                    json.put("venue_name", hearing.getVenueName());
                    json.put("address_line1", hearing.getAddress().getLine1());
                    json.put("address_line2", hearing.getAddress().getLine2());
                    json.put("address_line3", hearing.getAddress().getTown());
                    json.put("postcode", hearing.getAddress().getPostcode());
                    json.put("google_map_url", hearing.getAddress().getGoogleMapUrl());
                }
            }
        }

        return json;
    }
}
