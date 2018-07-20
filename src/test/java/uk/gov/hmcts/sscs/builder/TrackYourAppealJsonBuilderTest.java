package uk.gov.hmcts.sscs.builder;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static uk.gov.hmcts.sscs.model.AppConstants.DWP_RESPONSE_HEARING_CONTACT_DATE_IN_WEEKS;
import static uk.gov.hmcts.sscs.model.AppConstants.PAST_HEARING_BOOKED_IN_WEEKS;
import static uk.gov.hmcts.sscs.util.SerializeJsonMessageManager.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.Event;
import uk.gov.hmcts.sscs.model.ccd.EventDetails;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;

public class TrackYourAppealJsonBuilderTest {

    private TrackYourAppealJsonBuilder trackYourAppealJsonBuilder;

    @Before
    public void setUp() {
        trackYourAppealJsonBuilder = new TrackYourAppealJsonBuilder();
    }

    @Test
    public void appealReceivedTest() throws CcdException {
        CaseData caseData = APPEAL_RECEIVED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(APPEAL_RECEIVED.getSerializedMessage(), objectNode);
    }

    @Test
    public void dwpRespondTest() throws CcdException {
        CaseData caseData = DWP_RESPOND_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(DWP_RESPOND.getSerializedMessage(), objectNode);
    }

    @Test
    public void hearingBookedTest() throws CcdException {
        CaseData caseData = HEARING_BOOKED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(HEARING_BOOKED.getSerializedMessage(), objectNode);
    }

    @Test
    public void adjournedTest() throws CcdException {
        CaseData caseData = ADJOURNED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(ADJOURNED.getSerializedMessage(), objectNode);
    }

    @Test
    public void dormantTest() throws CcdException {
        CaseData caseData = DORMANT_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(DORMANT.getSerializedMessage(), objectNode);
    }

    @Test
    public void hearingTest() throws CcdException {
        CaseData caseData = HEARING_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(HEARING.getSerializedMessage(), objectNode);
    }

    @Test
    public void newHearingBookedTest() throws CcdException {
        CaseData caseData = NEW_HEARING_BOOKED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(NEW_HEARING_BOOKED.getSerializedMessage(), objectNode);
    }

    @Test
    public void pastHearingBookedDate_shouldReturnAPastHearingBookedEvent() throws CcdException {

        Instant instant = Instant.now();

        LocalDateTime localUtcDate = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).minusHours(2).minusWeeks(PAST_HEARING_BOOKED_IN_WEEKS);

        String dwpResponseDateCcd = LocalDateTime.ofInstant(instant, ZoneId.of("Europe/London")).minusHours(2).minusWeeks(PAST_HEARING_BOOKED_IN_WEEKS).toString();
        String dwpResponseDateUtc = localUtcDate.toString();
        String hearingContactDate = localUtcDate.plusWeeks(DWP_RESPONSE_HEARING_CONTACT_DATE_IN_WEEKS).toString();

        CaseData caseData = buildHearingBookedEvent(PAST_HEARING_BOOKED_CCD.getDeserializeMessage(), dwpResponseDateCcd);

        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());

        String updatedString = PAST_HEARING_BOOKED.getSerializedMessage()
                .replace("2017-06-29T11:50:11.987Z", dwpResponseDateUtc + "Z")
                .replace("2017-08-24T11:50:11.437Z", hearingContactDate + "Z");
        assertJsonEquals(updatedString, objectNode);
    }

    @Test
    public void notPastHearingBookedDate_shouldReturnADwpResponseEvent() throws CcdException {

        Instant instant = Instant.now();

        LocalDateTime localUtcDate = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).minusHours(2).minusWeeks(PAST_HEARING_BOOKED_IN_WEEKS - 1);

        String dwpResponseDateCcd = LocalDateTime.ofInstant(instant, ZoneId.of("Europe/London")).minusHours(2).minusWeeks(PAST_HEARING_BOOKED_IN_WEEKS - 1).toString();
        String dwpResponseDateUtc = localUtcDate.toString();
        String hearingContactDate = localUtcDate.plusWeeks(DWP_RESPONSE_HEARING_CONTACT_DATE_IN_WEEKS).toString();

        CaseData caseData = buildHearingBookedEvent(NOT_PAST_HEARING_BOOKED_CCD.getDeserializeMessage(), dwpResponseDateCcd);

        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());

        String updatedString = NOT_PAST_HEARING_BOOKED.getSerializedMessage()
                .replace("2017-06-29T11:50:11.987Z", dwpResponseDateUtc + "Z")
                .replace("2017-08-24T11:50:11.437Z", hearingContactDate + "Z");
        assertJsonEquals(updatedString, objectNode);
    }

    private CaseData buildHearingBookedEvent(CaseData caseData, String dwpResponseDate) {
        Event event = caseData.getEvents().get(0);

        EventDetails details = event.getValue().toBuilder().date(dwpResponseDate).build();
        event = Event.builder().value(details).build();

        List<Event> events = caseData.getEvents();
        events.remove(0);
        events.add(event);

        return caseData.toBuilder().events(events).build();
    }

    @Test
    public void dwpRespondOverdueTest() throws CcdException {
        CaseData caseData = DWP_RESPOND_OVERDUE_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(DWP_RESPOND_OVERDUE.getSerializedMessage(), objectNode);
    }

    @Test
    public void postponedTest() throws CcdException {
        CaseData caseData = POSTPONED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(POSTPONED.getSerializedMessage(), objectNode);
    }

    @Test
    public void withdrawnTest() throws CcdException {
        CaseData caseData = WITHDRAWN_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(WITHDRAWN.getSerializedMessage(), objectNode);
    }

    @Test
    public void closedTest() throws CcdException {
        CaseData caseData = CLOSED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(CLOSED.getSerializedMessage(), objectNode);
    }

    @Test
    public void lapsedRevisedTest() throws CcdException {
        CaseData caseData = LAPSED_REVISED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(LAPSED_REVISED.getSerializedMessage(), objectNode);
    }

    @Test(expected = CcdException.class)
    public void noEventsTest() throws CcdException {
        CaseData caseData = NO_EVENTS_CCD.getDeserializeMessage();
        trackYourAppealJsonBuilder.build(caseData, populateRegionalProcessingCenter());
    }

    @Test
    public void appealCreatedTest() throws CcdException {
        CaseData caseData = APPEAL_CREATED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(APPEAL_CREATED.getSerializedMessage(), objectNode);
    }


    @Test
    public void shouldHandleMissingHearings() throws CcdException {
        CaseData caseData = MISSING_HEARING_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(MISSING_HEARING.getSerializedMessage(), objectNode);

    }

    private RegionalProcessingCenter populateRegionalProcessingCenter() {
        RegionalProcessingCenter regionalProcessingCenter = new RegionalProcessingCenter();
        regionalProcessingCenter.setName("LIVERPOOL");
        regionalProcessingCenter.setAddress1("HM Courts & Tribunals Service");
        regionalProcessingCenter.setAddress2("Social Security & Child Support Appeals");
        regionalProcessingCenter.setAddress3("Prudential Buildings");
        regionalProcessingCenter.setAddress4("36 Dale Street");
        regionalProcessingCenter.setCity("LIVERPOOL");
        regionalProcessingCenter.setPostcode("L2 5UZ");
        regionalProcessingCenter.setPhoneNumber("0300 123 1142");
        regionalProcessingCenter.setFaxNumber("0870 324 0109");
        return regionalProcessingCenter;
    }
    
}

