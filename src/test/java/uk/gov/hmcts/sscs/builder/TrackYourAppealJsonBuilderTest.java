package uk.gov.hmcts.sscs.builder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.sscs.model.AppConstants.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.corecase.*;
import uk.gov.hmcts.sscs.domain.tya.RegionalProcessingCenter;

public class TrackYourAppealJsonBuilderTest {

    CcdCase ccdCase;
    Appeal appeal;

    @Before
    public void setup() {
        appeal = new Appeal();
        appeal.setAppealNumber("mj876");

        ccdCase = new CcdCase();
        ccdCase.setCaseReference("SC/12345");
        ccdCase.setBenefitType("ESA");
        ccdCase.setAppealStatus(EventType.DWP_RESPOND.toString());
        ccdCase.setAppeal(appeal);
        ccdCase.setRegionalProcessingCenter(populateRegionalProcessingCenter());
    }

    @Test
    public void buildJsonFromCcdCase() throws JSONException {

        ccdCase.setAppellant(new Appellant(new Name("Mr", "R", "Smith"),
                new Address(), "", "", "", ""));


        String date1 = "2017-12-01T12:00:50.297Z";

        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put(HEARING_DATETIME, date1);
        jsonObject1.put(VENUE_NAME, "The Ritz");
        jsonObject1.put(ADDRESS_LINE_1, "My road");
        jsonObject1.put(ADDRESS_LINE_2, "Village green");
        jsonObject1.put(ADDRESS_LINE_3, "Sheparton");
        jsonObject1.put(POSTCODE, "SH15TH");
        jsonObject1.put(GOOGLE_MAP_URL, "http://mymap.com");

        String date2 = "2017-11-01T12:00:50.297Z";

        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put(EVIDENCE_TYPE, "Medical Evidence");
        jsonObject2.put(EVIDENCE_PROVIDED_BY, "Appellant");
        jsonObject2.put(DWP_RESPONSE_DATE_LITERAL, date2);
        jsonObject2.put(HEARING_CONTACT_DATE_LITERAL, date2);
        jsonObject2.put(ADJOURNED_LETTER_RECEIVED_BY_DATE, date2);
        jsonObject2.put(DECISION_LETTER_RECEIVE_BY_DATE, date2);

        Event event1 = new Event(ZonedDateTime.now(), EventType.HEARING_BOOKED);
        Event event2 = new Event(ZonedDateTime.now().minusDays(1), EventType.APPEAL_RECEIVED);
        List<Event> events = new ArrayList<Event>() {
            {
                add(event1);
                add(event2);
            }
        };
        ccdCase.setEvents(events);

        ccdCase.buildLatestEvents();
        ccdCase.buildHistoricalEvents();

        ObjectNode result = TrackYourAppealJsonBuilder.buildTrackYourAppealJson(ccdCase);

        JsonNode caseNodeResult = result.get("appeal");

        assertThat(caseNodeResult.get("caseReference").toString(),
                is("\"" + ccdCase.getCaseReference() + "\""));
        assertThat(caseNodeResult.get("appealNumber").toString(),
                is("\"" + ccdCase.getAppeal().getAppealNumber() + "\""));
        assertThat(caseNodeResult.get("name").toString(),
                is("\"" + ccdCase.getAppellant().getName().getFullName() + "\""));
        assertThat(caseNodeResult.get("status").toString(),
                is("\"" + ccdCase.getAppealStatus().toString() + "\""));
        assertThat(caseNodeResult.get("benefitType").toString(),
                is("\"" + ccdCase.getBenefitType().toLowerCase() + "\""));

        JsonNode regionalProcessingCenter = caseNodeResult.get("regionalProcessingCenter");

        assertThat(regionalProcessingCenter.get("name").toString(), equalTo("\"BIRMINGHAM\""));
        assertThat(regionalProcessingCenter.get("addressLines").toString(),
                equalTo("[\"HM Courts & Tribunals Service\","
                + "\"Social Security & Child Support Appeals\",\"Administrative Support Centre\",\"PO Box 14620\"]"));
        assertThat(regionalProcessingCenter.get("city").toString(), equalTo("\"BIRMINGHAM\""));
        assertThat(regionalProcessingCenter.get("postcode").toString(), equalTo("\"B16 6FR\""));
        assertThat(regionalProcessingCenter.get("phoneNumber").toString(), equalTo("\"0300 123 1142\""));
        assertThat(regionalProcessingCenter.get("faxNumber").toString(), equalTo("\"0126 434 7983\""));

        //TODO: Placeholder Array needs to be tested by uncommenting the below when creating the Events for CCD. This can only be done when we know what the data will look like.
        //    JsonNode latestEventsResult = caseNodeResult.get("latestEvents").get(0);
        //
        //    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
        //  "yyyy-MM-dd HH:mm:ss.SSS'Z'");
        //
        //    assertThat(latestEventsResult.get("date").toString(), is("\""
        // + formatter.format(event1.getDate()) + "\""));
        //    assertThat(latestEventsResult.get("type").toString(),
        // is("\"" + event1.getType() + "\""));
        //    assertThat(latestEventsResult.get("contentKey").toString(), is("\""
        // + event1.getType().getContentKey() + "\""));
        //    assertThat(latestEventsResult.get("hearingDateTime").toString(),
        // is("\"" + date1 + "\""));
        //    assertThat(latestEventsResult.get("venueName").toString(), is("\"The Ritz\""));
        //    assertThat(latestEventsResult.get("addressLine1").toString(), is("\"My road\""));
        //    assertThat(latestEventsResult.get("addressLine2").toString(),
        // is("\"Village green\""));
        //    assertThat(latestEventsResult.get("addressLine3").toString(), is("\"Sheparton\""));
        //    assertThat(latestEventsResult.get("postcode").toString(), is("\"SH15TH\""));
        //    assertThat(latestEventsResult.get("googleMapUrl").toString(),
        // is("\"http://mymap.com\""));
        //
        //    JsonNode historicalEventsResult = caseNodeResult.get("historicalEvents").get(0);
        //
        //    assertThat(historicalEventsResult.get("date").toString(), is("\""
        // + formatter.format(event2.getDate()) + "\""));
        //    assertThat(historicalEventsResult.get("type").toString(), is("\""
        // + event2.getType() + "\""));
        //    assertThat(historicalEventsResult.get("contentKey").toString(), is("\""
        // + event2.getType().getContentKey() + "\""));
        //    assertThat(historicalEventsResult.get("evidenceType").toString(),
        // is("\"Medical Evidence\""));
        //    assertThat(historicalEventsResult.get("evidenceProvidedBy").toString(),
        // is("\"Appellant\""));
        //    assertThat(historicalEventsResult.get("dwpResponseDate").toString(),
        // is("\"" + date2 + "\""));
        //    assertThat(historicalEventsResult.get("hearingContactDate").toString(),
        // is("\"" + date2 + "\""));
        //   assertThat(historicalEventsResult.get("adjournedLetterReceivedByDate").toString(),
        // is("\"" + date2 + "\""));
        //    assertThat(historicalEventsResult.get("decisionLetterReceiveByDate").toString(),
        //      is("\"" + date2 + "\""));

    }

    @Test
    public void testNullAppellantDoesNotBuildAppellantNameJson() throws JSONException {
        ccdCase.setAppellant(null);

        ObjectNode result = TrackYourAppealJsonBuilder.buildTrackYourAppealJson(ccdCase);

        JsonNode caseNodeResult = result.get("appeal");

        assertNull(caseNodeResult.get("name"));
    }

    @Test
    public void testEmptyPlaceholderDetailsDoesNotBuildPlaceholderJson() throws JSONException {
        Event event = new Event(ZonedDateTime.now(), EventType.HEARING_BOOKED);
        event.setPlaceholders(new JSONObject().toString());
        ObjectNode eventNode = JsonNodeFactory.instance.objectNode();

        ObjectNode result = TrackYourAppealJsonBuilder.buildPlaceholderArray(event, eventNode);

        JsonNode caseNodeResult = result.get(HEARING_DATETIME);

        assertNull(caseNodeResult);
    }


    private RegionalProcessingCenter populateRegionalProcessingCenter() {
        RegionalProcessingCenter regionalProcessingCenter = new RegionalProcessingCenter();
        regionalProcessingCenter.setName("BIRMINGHAM");
        regionalProcessingCenter.setAddress1("HM Courts & Tribunals Service");
        regionalProcessingCenter.setAddress2("Social Security & Child Support Appeals");
        regionalProcessingCenter.setAddress3("Administrative Support Centre");
        regionalProcessingCenter.setAddress4("PO Box 14620");
        regionalProcessingCenter.setCity("BIRMINGHAM");
        regionalProcessingCenter.setPostcode("B16 6FR");
        regionalProcessingCenter.setPhoneNumber("0300 123 1142");
        regionalProcessingCenter.setFaxNumber("0126 434 7983");
        return regionalProcessingCenter;
    }

}

