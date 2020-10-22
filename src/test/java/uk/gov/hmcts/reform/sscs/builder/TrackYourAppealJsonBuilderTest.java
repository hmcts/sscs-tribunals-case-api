package uk.gov.hmcts.reform.sscs.builder;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class TrackYourAppealJsonBuilderTest {

    private TrackYourAppealJsonBuilder trackYourAppealJsonBuilder;

    @Before
    public void setUp() {
        trackYourAppealJsonBuilder = new TrackYourAppealJsonBuilder();
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

