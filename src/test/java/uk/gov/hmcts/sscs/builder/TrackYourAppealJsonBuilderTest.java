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
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;

public class TrackYourAppealJsonBuilderTest {

    private TrackYourAppealJsonBuilder trackYourAppealJsonBuilder;

    @Before
    public void setUp() {
        trackYourAppealJsonBuilder = new TrackYourAppealJsonBuilder();
    }

    @Test
    public void appealReceivedTest() throws CcdException {
        SscsCaseData caseData = APPEAL_RECEIVED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(APPEAL_RECEIVED.getSerializedMessage(), objectNode);
    }

    @Test
    public void dwpRespondTest() throws CcdException {
        SscsCaseData caseData = DWP_RESPOND_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(DWP_RESPOND.getSerializedMessage(), objectNode);
    }

    @Test
    public void hearingBookedTest() throws CcdException {
        SscsCaseData caseData = HEARING_BOOKED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(HEARING_BOOKED.getSerializedMessage(), objectNode);
    }

    @Test
    public void adjournedTest() throws CcdException {
        SscsCaseData caseData = ADJOURNED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(ADJOURNED.getSerializedMessage(), objectNode);
    }

    @Test
    public void dormantTest() throws CcdException {
        SscsCaseData caseData = DORMANT_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(DORMANT.getSerializedMessage(), objectNode);
    }

    @Test
    public void hearingTest() throws CcdException {
        SscsCaseData caseData = HEARING_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(HEARING.getSerializedMessage(), objectNode);
    }

    @Test
    public void newHearingBookedTest() throws CcdException {
        SscsCaseData caseData = NEW_HEARING_BOOKED_CCD.getDeserializeMessage();
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

        SscsCaseData caseData = buildHearingBookedEvent(PAST_HEARING_BOOKED_CCD.getDeserializeMessage(), dwpResponseDateCcd);

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

        SscsCaseData caseData = buildHearingBookedEvent(NOT_PAST_HEARING_BOOKED_CCD.getDeserializeMessage(), dwpResponseDateCcd);

        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());

        String updatedString = NOT_PAST_HEARING_BOOKED.getSerializedMessage()
                .replace("2017-06-29T11:50:11.987Z", dwpResponseDateUtc + "Z")
                .replace("2017-08-24T11:50:11.437Z", hearingContactDate + "Z");
        assertJsonEquals(updatedString, objectNode);
    }

    private SscsCaseData buildHearingBookedEvent(SscsCaseData caseData, String dwpResponseDate) {
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
        SscsCaseData caseData = DWP_RESPOND_OVERDUE_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(DWP_RESPOND_OVERDUE.getSerializedMessage(), objectNode);
    }

    @Test
    public void postponedTest() throws CcdException {
        SscsCaseData caseData = POSTPONED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(POSTPONED.getSerializedMessage(), objectNode);
    }

    @Test
    public void withdrawnTest() throws CcdException {
        SscsCaseData caseData = WITHDRAWN_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(WITHDRAWN.getSerializedMessage(), objectNode);
    }

    @Test
    public void closedTest() throws CcdException {
        SscsCaseData caseData = CLOSED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(CLOSED.getSerializedMessage(), objectNode);
    }

    @Test
    public void lapsedRevisedTest() throws CcdException {
        SscsCaseData caseData = LAPSED_REVISED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(LAPSED_REVISED.getSerializedMessage(), objectNode);
    }

    @Test
    public void emptyEventDateShouldBeIgnoredTest() throws CcdException {
        SscsCaseData caseData = EMPTY_EVENT_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(LAPSED_REVISED.getSerializedMessage(), objectNode);
    }

    @Test(expected = CcdException.class)
    public void noEventsTest() throws CcdException {
        SscsCaseData caseData = NO_EVENTS_CCD.getDeserializeMessage();
        trackYourAppealJsonBuilder.build(caseData, populateRegionalProcessingCenter());
    }

    @Test
    public void appealCreatedTest() throws CcdException {
        SscsCaseData caseData = APPEAL_CREATED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(APPEAL_CREATED.getSerializedMessage(), objectNode);
    }


    @Test
    public void shouldHandleMissingHearings() throws CcdException {
        SscsCaseData caseData = MISSING_HEARING_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(MISSING_HEARING.getSerializedMessage(), objectNode);

    }

    @Test
    public void shouldReturnHearingTypeIfPresentInCcd() {
        SscsCaseData caseData = APPEAL_WITH_HEARING_TYPE_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(APPEAL_WITH_HEARING_TYPE.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnOralHearingTypeIfNoHearingTypeInCcdAndWantsToAttendIsYes() {
        SscsCaseData caseData = APPEAL_WITH_WANTS_TO_ATTEND_YES_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        System.out.println(objectNode.textValue());
        assertJsonEquals(APPEAL_WITH_WANTS_TO_ATTEND_YES.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnPaperlHearingTypeIfNoHearingTypeInCcdAndWantsToAttendIsNo() {
        SscsCaseData caseData = APPEAL_WITH_WANTS_TO_ATTEND_NO_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        System.out.println(objectNode.textValue());
        assertJsonEquals(APPEAL_WITH_WANTS_TO_ATTEND_NO.getSerializedMessage(), objectNode);
    }

    private RegionalProcessingCenter populateRegionalProcessingCenter() {
        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
                .name("LIVERPOOL")
                .address1("HM Courts & Tribunals Service")
                .address2("Social Security & Child Support Appeals")
                .address3("Prudential Buildings")
                .address4("36 Dale Street")
                .city("LIVERPOOL")
                .postcode("L2 5UZ")
                .phoneNumber("0300 123 1142")
                .faxNumber("0870 324 0109")
                .build();
        return rpc;
    }
    
}

