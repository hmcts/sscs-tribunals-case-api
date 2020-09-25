package uk.gov.hmcts.reform.sscs.builder;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_RESPONSE_HEARING_CONTACT_DATE_IN_WEEKS;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.PAST_HEARING_BOOKED_IN_WEEKS;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;

public class TrackYourAppealJsonBuilderTest {

    private TrackYourAppealJsonBuilder trackYourAppealJsonBuilder;

    @Before
    public void setUp() {
        trackYourAppealJsonBuilder = new TrackYourAppealJsonBuilder();
    }

    @Test
    public void createdInGapsFromTest() {
        SscsCaseData caseData = APPEAL_CREATED_WITH_CREATEDINGAPSFROM_FIELD_CCD_RESPONSE.getDeserializeMessage();

        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData, populateRegionalProcessingCenter(), 1L);

        String actualValue = String.valueOf(objectNode.findValue("createdInGapsFrom"));
        assertJsonEquals("readyToList", actualValue);
    }

    @Test
    public void appealReceivedTest() throws CcdException {
        SscsCaseData caseData = APPEAL_RECEIVED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(APPEAL_RECEIVED.getSerializedMessage(), objectNode);
    }

    @Test
    public void dwpRespondTest() throws CcdException {
        SscsCaseData caseData = DWP_RESPOND_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(DWP_RESPOND.getSerializedMessage(), objectNode);
    }

    @Test
    public void dwpRespondWhenPaperCaseOlderThan8WeeksTest() throws CcdException {
        SscsCaseData caseData = DWP_RESPOND_PAPER_CASE_OLDER_THAN_8_WEEKS_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(DWP_RESPOND_PAPER_CASE_OLDER_THAN_8_WEEKS.getSerializedMessage(), objectNode);
    }

    @Test
    public void hearingBookedTest() throws CcdException {
        SscsCaseData caseData = HEARING_BOOKED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(HEARING_BOOKED.getSerializedMessage(), objectNode);
    }

    @Test
    public void adjournedTest() throws CcdException {
        SscsCaseData caseData = ADJOURNED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(ADJOURNED.getSerializedMessage(), objectNode);
    }

    @Test
    public void dormantTest() throws CcdException {
        SscsCaseData caseData = DORMANT_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(DORMANT.getSerializedMessage(), objectNode);
    }

    @Test
    public void hearingTest() throws CcdException {
        SscsCaseData caseData = HEARING_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(HEARING.getSerializedMessage(), objectNode);
    }

    @Test
    public void newHearingBookedTest() throws CcdException {
        SscsCaseData caseData = NEW_HEARING_BOOKED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(NEW_HEARING_BOOKED.getSerializedMessage(), objectNode);
    }

    @Test
    public void pastHearingBookedDate_shouldReturnAPastHearingBookedEvent() throws CcdException {

        Instant instant = Instant.now();

        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("Europe/London")).minusHours(2).minusWeeks(PAST_HEARING_BOOKED_IN_WEEKS);

        String dwpResponseDateCcd = LocalDateTime.ofInstant(instant, ZoneId.of("Europe/London")).minusHours(2).minusWeeks(PAST_HEARING_BOOKED_IN_WEEKS).toString();
        String dwpResponseDateUtc = DateTimeUtils.convertLocalDateTimetoUtc(localDateTime);
        String hearingContactDate = DateTimeUtils.convertLocalDateTimetoUtc(localDateTime.plusWeeks(DWP_RESPONSE_HEARING_CONTACT_DATE_IN_WEEKS));

        SscsCaseData caseData = buildHearingBookedEvent(PAST_HEARING_BOOKED_CCD.getDeserializeMessage(), dwpResponseDateCcd);

        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);

        String updatedString = PAST_HEARING_BOOKED.getSerializedMessage()
            .replace("2017-06-29T11:50:11.987Z", dwpResponseDateUtc)
            .replace("2017-08-24T11:50:11.437Z", hearingContactDate);
        assertJsonEquals(updatedString, objectNode);
    }

    @Test
    public void notPastHearingBookedDate_shouldReturnADwpResponseEvent() throws CcdException {

        Instant instant = Instant.now();

        LocalDateTime localUtcDate = LocalDateTime.ofInstant(instant, ZoneId.of("Europe/London")).minusHours(2).minusWeeks(PAST_HEARING_BOOKED_IN_WEEKS - 1);

        String dwpResponseDateCcd = localUtcDate.toString();
        String dwpResponseDateUtc = DateTimeUtils.convertLocalDateTimetoUtc(localUtcDate);
        String hearingContactDate = DateTimeUtils.convertLocalDateTimetoUtc(localUtcDate.plusWeeks(DWP_RESPONSE_HEARING_CONTACT_DATE_IN_WEEKS));

        SscsCaseData caseData = buildHearingBookedEvent(NOT_PAST_HEARING_BOOKED_CCD.getDeserializeMessage(), dwpResponseDateCcd);

        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);

        String updatedString = NOT_PAST_HEARING_BOOKED.getSerializedMessage()
            .replace("2017-06-29T11:50:11.987Z", dwpResponseDateUtc)
            .replace("2017-08-24T11:50:11.437Z", hearingContactDate);
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
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(DWP_RESPOND_OVERDUE.getSerializedMessage(), objectNode);
    }

    @Test
    public void postponedTest() throws CcdException {
        SscsCaseData caseData = POSTPONED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(POSTPONED.getSerializedMessage(), objectNode);
    }

    @Test
    public void withdrawnTest() throws CcdException {
        SscsCaseData caseData = WITHDRAWN_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(WITHDRAWN.getSerializedMessage(), objectNode);
    }

    @Test
    public void closedTest() throws CcdException {
        SscsCaseData caseData = CLOSED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(CLOSED.getSerializedMessage(), objectNode);
    }

    @Test
    public void lapsedRevisedTest() throws CcdException {
        SscsCaseData caseData = LAPSED_REVISED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(LAPSED_REVISED.getSerializedMessage(), objectNode);
    }

    @Test
    public void emptyEventDateShouldBeIgnoredTest() throws CcdException {
        SscsCaseData caseData = EMPTY_EVENT_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(LAPSED_REVISED.getSerializedMessage(), objectNode);
    }

    @Test(expected = CcdException.class)
    public void noEventsTest() throws CcdException {
        SscsCaseData caseData = NO_EVENTS_CCD.getDeserializeMessage();
        trackYourAppealJsonBuilder.build(caseData, populateRegionalProcessingCenter(), 1L);
    }

    @Test
    public void appealCreatedTest() throws CcdException {
        SscsCaseData caseData = APPEAL_CREATED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(APPEAL_CREATED.getSerializedMessage(), objectNode);
    }

    @Test
    public void appealCreatedWithSubscriptionTest() throws CcdException {
        SscsCaseData caseData = APPEAL_CREATED_WITH_SUBSCRIPTION_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(APPEAL_CREATED_WITH_SUBSCRIPTION.getSerializedMessage(), objectNode);
    }

    @Test
    public void appealCreatedWithOnlyAppellantEmailSubscriptionTest() throws CcdException {
        SscsCaseData caseData = APPEAL_CREATED_WITH_APPELLANT_SUBSCRIPTION_CCD.getDeserializeMessage();

        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(APPEAL_CREATED_WITH_APPELLANT_SUBSCRIPTION.getSerializedMessage(), objectNode);
    }

    @Test
    public void appealCreatedWithContactDetails() throws CcdException {
        SscsCaseData caseData = APPEAL_CREATED_WITH_SUBSCRIPTION_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter(), 1L);

        assertJsonEquals(APPEAL_CREATED_WITH_SUBSCRIPTION.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldHandleMissingHearings() throws CcdException {
        SscsCaseData caseData = MISSING_HEARING_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(MISSING_HEARING.getSerializedMessage(), objectNode);

    }

    @Test
    public void shouldReturnHearingTypeIfPresentInCcd() {
        SscsCaseData caseData = APPEAL_WITH_HEARING_TYPE_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(APPEAL_WITH_HEARING_TYPE.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnHearingTypeAndExelaAddressIfPresentInCcd() {
        SscsCaseData caseData = APPEAL_WITH_HEARING_TYPE_AND_STATE_READY_TO_LIST_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(APPEAL_WITH_HEARING_TYPE_AND_STATE_READY_TO_LIST.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnOralHearingTypeIfNoHearingTypeInCcdAndWantsToAttendIsYes() {
        SscsCaseData caseData = APPEAL_WITH_WANTS_TO_ATTEND_YES_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(APPEAL_WITH_WANTS_TO_ATTEND_YES.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnPaperlHearingTypeIfNoHearingTypeInCcdAndWantsToAttendIsNo() {
        SscsCaseData caseData = APPEAL_WITH_WANTS_TO_ATTEND_NO_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(APPEAL_WITH_WANTS_TO_ATTEND_NO.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnOralHearingTypeIfThereIsNoHearingTypeOrWantsToAttendFieldInCcd() {
        SscsCaseData caseData = APPEAL_WITH_WANTS_TO_ATTEND_IS_NOT_PRESENT_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(APPEAL_WITH_WANTS_TO_ATTEND_IS_NOT_PRESENT.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnOralHearingTypeIfThereIsNoHearingOptionsFieldInCcd() {
        SscsCaseData caseData = APPEAL_WITH_NO_HEARING_OPTIONS_IN_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(APPEAL_WITH_NO_HEARING_OPTIONS.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldNotReturnHearingrRelatedEventsForPaperCase() {
        SscsCaseData caseData = HEARING_BOOKED_PAPER_CASE_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(HEARING_BOOKED_PAPER_CASE.getSerializedMessage(), objectNode);
    }


    @Test
    public void shouldReturnCaseIdInTheAppealResponse() {
        SscsCaseData caseData = APPEAL_RECEIVED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
            populateRegionalProcessingCenter(), 1L);
        assertJsonEquals(APPEAL_RECEIVED.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnCaseIdInTheMyaAppealResponse() {
        SscsCaseData caseData = APPEAL_RECEIVED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter(), 1L, true, "appealCreated");
        assertJsonEquals(APPEAL_RECEIVED_MYA.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnCaseIdInTheMyaDwpResponse() {
        SscsCaseData caseData = DWP_RESPOND_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter(), 1L, true, "responseReceived");
        assertJsonEquals(DWP_RESPOND_MYA.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnCaseIdInTheMyaHearingResponse() {
        SscsCaseData caseData = HEARING_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter(), 1L, true, "hearing");
        assertJsonEquals(HEARING_MYA.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnCaseIdInTheMyaDormantResponse() {
        SscsCaseData caseData = DORMANT_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter(), 1L, true, "dormantAppealState");
        assertJsonEquals(DORMANT_MYA.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnHideHearingFlagInTheMyaNotListableResponse() {
        SscsCaseData caseData = NOT_LISTABLE_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter(), 1L, true, "notListable");
        assertJsonEquals(NOT_LISTABLE_MYA.getSerializedMessage(), objectNode);
    }

    @Test
    public void shouldReturnHideHearingFlagInTheMyaResponseWithAdjournedHearing() {
        SscsCaseData caseData = ADJOURNED_HEARING_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter(), 1L, true, "hearing");
        assertJsonEquals(ADJOURNED_HEARING_MYA.getSerializedMessage(), objectNode);
    }

    private RegionalProcessingCenter populateRegionalProcessingCenter() {
        return RegionalProcessingCenter.builder()
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
    }

}

