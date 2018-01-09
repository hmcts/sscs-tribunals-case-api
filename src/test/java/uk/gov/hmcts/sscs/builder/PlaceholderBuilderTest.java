package uk.gov.hmcts.sscs.builder;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.hmcts.sscs.builder.PlaceholderBuilder.addEventPlaceHolders;
import static uk.gov.hmcts.sscs.model.AppConstants.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.corecase.Address;
import uk.gov.hmcts.sscs.domain.corecase.Event;
import uk.gov.hmcts.sscs.domain.corecase.EventType;
import uk.gov.hmcts.sscs.domain.corecase.Hearing;
import uk.gov.hmcts.sscs.service.BusinessDaysCalculatorService;

public class PlaceholderBuilderTest {

    @Test
    public void addAppealReceivedPlaceholder() {
        ZonedDateTime now = ZonedDateTime.now();
        Event event = new Event(now, EventType.APPEAL_RECEIVED);

        try {
            JSONObject json = addEventPlaceHolders(event, new JSONObject(), null);
            assertThat(json.get(DWP_RESPONSE_DATE_LITERAL),
                    is(now.plusDays(MAX_DWP_RESPONSE_DAYS).format(ISO_INSTANT)));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void addDwpRespondPlaceholder() {
        ZonedDateTime now = ZonedDateTime.now();
        Event event = new Event(now, EventType.DWP_RESPOND);

        try {
            JSONObject json = addEventPlaceHolders(event, new JSONObject(), null);
            assertThat(json.get(HEARING_CONTACT_DATE_LITERAL),
                    is(now.plusDays(DAYS_FROM_DWP_RESPONSE_DATE_FOR_HEARING_CONTACT)
                            .format(ISO_INSTANT)));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void addPastHearingBookedPlaceholder() {
        ZonedDateTime now = ZonedDateTime.now();
        Event event = new Event(now, EventType.PAST_HEARING_BOOKED);

        try {
            JSONObject json = addEventPlaceHolders(event, new JSONObject(), null);
            assertThat(json.get(HEARING_CONTACT_DATE_LITERAL),
                    is(now.plusDays(DAYS_FROM_DWP_RESPONSE_DATE_FOR_HEARING_CONTACT)
                            .format(ISO_INSTANT)));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void addPostponedPlaceholder() {
        ZonedDateTime now = ZonedDateTime.now();
        Event event = new Event(now, EventType.POSTPONED);

        try {
            JSONObject json = addEventPlaceHolders(event, new JSONObject(), null);
            assertThat(json.get(HEARING_CONTACT_DATE_LITERAL),
                    is(now.plusWeeks(HEARING_DATE_CONTACT_WEEKS).format(ISO_INSTANT)));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void addAdjournedPlaceholder() {
        ZonedDateTime now = ZonedDateTime.now();
        Event event = new Event(now, EventType.ADJOURNED);

        try {
            JSONObject json = addEventPlaceHolders(event, new JSONObject(), null);
            assertThat(json.get("adjournedLetterReceivedByDate"),
                    is(now.plusDays(ADJOURNED_LETTER_RECEIVED_MAX_DAYS).format(ISO_INSTANT)));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void addHearingPlaceholder() {
        ZonedDateTime now = ZonedDateTime.now();
        Event event = new Event(now, EventType.HEARING);
        ZonedDateTime decisionDateTime = BusinessDaysCalculatorService.getBusinessDay(
                now, HEARING_DECISION_LETTER_RECEIVED_MAX_DAYS);

        try {
            JSONObject json = addEventPlaceHolders(event, new JSONObject(), null);
            assertThat(json.get(DECISION_LETTER_RECEIVE_BY_DATE),
                    is(decisionDateTime.format(ISO_INSTANT)));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void addDormantPlaceholder() {
        ZonedDateTime now = ZonedDateTime.now();
        Event event = new Event(now, EventType.DORMANT);
        ZonedDateTime decisionDateTime = BusinessDaysCalculatorService.getBusinessDay(
                now, HEARING_DECISION_LETTER_RECEIVED_MAX_DAYS);

        try {
            JSONObject json = addEventPlaceHolders(event, new JSONObject(), null);
            assertThat(json.get(DECISION_LETTER_RECEIVE_BY_DATE),
                    is(decisionDateTime.format(ISO_INSTANT)));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void addHearingBookedPlaceholderWhenHearingDateAndEventDateAreOnSameDay() {
        ZonedDateTime hearingDateTime = ZonedDateTime.parse(
                "2017-12-01T12:00:00.123Z", DateTimeFormatter.ISO_ZONED_DATE_TIME);
        ZonedDateTime eventDateTime = ZonedDateTime.parse(
                "2017-12-01T16:00:00.123Z", DateTimeFormatter.ISO_ZONED_DATE_TIME);

        Address address = new Address("The court house", "", "Farndon", "Chester", "CH1 6YT",
                "http://chester.com");
        Hearing hearing = new Hearing(address, hearingDateTime);
        hearing.setVenueName("Chester court");

        List<Hearing> hearings = new ArrayList<>();
        hearings.add(hearing);

        Event event = new Event(eventDateTime, EventType.HEARING_BOOKED);

        try {
            JSONObject result = addEventPlaceHolders(event, new JSONObject(), hearings);
            assertThat(result.get(HEARING_DATETIME), is("2017-12-01T12:00:00.123Z"));
            assertThat(result.get("venue_name"), is(hearing.getVenueName()));
            assertThat(result.get("address_line1"), is(hearing.getAddress().getLine1()));
            assertThat(result.get("address_line2"), is(hearing.getAddress().getLine2()));
            assertThat(result.get("address_line3"), is(hearing.getAddress().getTown()));
            assertThat(result.get("postcode"), is(hearing.getAddress().getPostcode()));
            assertThat(result.get("google_map_url"), is(hearing.getAddress().getGoogleMapUrl()));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void doNotGenerateHearingBookedPlaceholderWhenHearingTimeAndEventTimeAreNotOnSameDay() {
        ZonedDateTime hearingDateTime = ZonedDateTime.parse("2017-12-01T12:00:00.123Z",
                DateTimeFormatter.ISO_ZONED_DATE_TIME);
        ZonedDateTime eventDateTime = ZonedDateTime.parse("2017-12-02T16:00:00.123Z",
                DateTimeFormatter.ISO_ZONED_DATE_TIME);

        Hearing hearing = new Hearing(new Address(), hearingDateTime);

        List<Hearing> hearings = new ArrayList<>();
        hearings.add(hearing);

        Event event = new Event(eventDateTime, EventType.HEARING_BOOKED);

        JSONObject result = addEventPlaceHolders(event, new JSONObject(), hearings);
        assertThat(result.toString(), is("{}"));
    }

    @Test
    public void addHearingBookedPlaceholderWithGmtTime() {
        ZonedDateTime hearingDateTime = ZonedDateTime.parse("2017-12-01T12:00:00.123+00:00",
                DateTimeFormatter.ISO_ZONED_DATE_TIME);
        ZonedDateTime eventDateTime = ZonedDateTime.parse("2017-12-01T16:00:00.123+00:00",
                DateTimeFormatter.ISO_ZONED_DATE_TIME);

        Address address = new Address("Chester court", "The court house", "Farndon", "Chester",
                "CH1 6YT", "http://chester.com");
        Hearing hearing = new Hearing(address, hearingDateTime);

        List<Hearing> hearings = new ArrayList<>();
        hearings.add(hearing);

        Event event = new Event(eventDateTime, EventType.HEARING_BOOKED);

        try {
            JSONObject result = addEventPlaceHolders(event, new JSONObject(), hearings);
            assertThat(result.get(HEARING_DATETIME), is("2017-12-01T12:00:00.123Z"));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void addHearingBookedPlaceholderWithBstTime() {
        ZonedDateTime hearingDateTime = ZonedDateTime.parse("2017-08-01T12:00:00.123+01:00",
                DateTimeFormatter.ISO_ZONED_DATE_TIME);
        ZonedDateTime eventDateTime = ZonedDateTime.parse("2017-08-01T16:00:00.123+01:00",
                DateTimeFormatter.ISO_ZONED_DATE_TIME);

        Address address = new Address("Chester court", "The court house", "Farndon", "Chester",
                "CH1 6YT", "http://chester.com");
        Hearing hearing = new Hearing(address, hearingDateTime);

        List<Hearing> hearings = new ArrayList<>();
        hearings.add(hearing);

        Event event = new Event(eventDateTime, EventType.HEARING_BOOKED);

        try {
            JSONObject result = addEventPlaceHolders(event, new JSONObject(), hearings);
            assertThat(result.get(HEARING_DATETIME), is("2017-08-01T12:00:00.123Z"));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("Should not have thrown any exception");
        }
    }
}
