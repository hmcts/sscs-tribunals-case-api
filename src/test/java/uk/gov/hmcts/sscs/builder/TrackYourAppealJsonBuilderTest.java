package uk.gov.hmcts.sscs.builder;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.corecase.*;

public class TrackYourAppealJsonBuilderTest {

    TrackYourAppealJsonBuilder builder;

    @Before
    public void setUp() {
        builder = new TrackYourAppealJsonBuilder();
    }

    @Test
    public void buildJsonFromCcdCase() throws JSONException {
        Appeal appeal = new Appeal();
        appeal.setAppealNumber("mj876");

        CcdCase ccdCase = new CcdCase();
        ccdCase.setCaseReference("SC/12345");
        ccdCase.setAppeal(appeal);
        ccdCase.setAppellant(new Appellant(new Name("Mr", "R", "Smith"),
                new Address(), "", "", "", ""));
        ccdCase.setAppealStatus(EventType.DWP_RESPOND.toString());
        ccdCase.setBenefitType("ESA");

        String date1 = "2017-12-01T12:00:50.297Z";

        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("hearing_date_time", date1);
        jsonObject1.put("venue_name", "The Ritz");
        jsonObject1.put("address_line1", "My road");
        jsonObject1.put("address_line2", "Village green");
        jsonObject1.put("address_line3", "Sheparton");
        jsonObject1.put("postcode", "SH15TH");
        jsonObject1.put("google_map_url", "http://mymap.com");

        String date2 = "2017-11-01T12:00:50.297Z";

        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put("evidence_type", "Medical Evidence");
        jsonObject2.put("evidence_provided_by", "Appellant");
        jsonObject2.put("dwpResponseDate", date2);
        jsonObject2.put("hearing_contact_date", date2);
        jsonObject2.put("adjournedLetterReceivedByDate", date2);
        jsonObject2.put("decisionLetterReceiveByDate", date2);

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

        ObjectNode result = builder.buildTrackYourAppealJson(ccdCase);

        JsonNode caseNodeResult = result.get("appeal");

        assertThat(caseNodeResult.get("caseReference").toString(), is("\""
                + ccdCase.getCaseReference() + "\""));
        assertThat(caseNodeResult.get("appealNumber").toString(), is("\""
                + ccdCase.getAppeal().getAppealNumber() + "\""));
        assertThat(caseNodeResult.get("name").toString(), is("\""
                + ccdCase.getAppellant().getName().getFullName() + "\""));
        assertThat(caseNodeResult.get("status").toString(), is("\""
                + ccdCase.getAppealStatus().toString() + "\""));
        assertThat(caseNodeResult.get("benefitType").toString(), is("\""
                + ccdCase.getBenefitType().toLowerCase() + "\""));

    }
}
