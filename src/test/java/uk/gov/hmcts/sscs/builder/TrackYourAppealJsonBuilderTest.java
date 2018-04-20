package uk.gov.hmcts.sscs.builder;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static uk.gov.hmcts.sscs.util.SerializeJsonMessageManager.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
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
    @Ignore
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
    public void pastHearingBookedTest() throws CcdException {
        CaseData caseData = PAST_HEARING_BOOKED_CCD.getDeserializeMessage();
        ObjectNode objectNode = trackYourAppealJsonBuilder.build(caseData,
                populateRegionalProcessingCenter());
        assertJsonEquals(PAST_HEARING_BOOKED.getSerializedMessage(), objectNode);
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

