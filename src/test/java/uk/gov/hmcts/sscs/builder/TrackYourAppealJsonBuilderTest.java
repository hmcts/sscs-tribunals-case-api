package uk.gov.hmcts.sscs.builder;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static uk.gov.hmcts.sscs.util.SerializeJsonMessageManager.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;

import uk.gov.hmcts.sscs.model.ccd.CaseData;

public class TrackYourAppealJsonBuilderTest {

    @Test
    public void appealReceivedJsonTest() {
        CaseData caseData = APPEAL_RECEIVED_CCD.getDeserializeMessage();
        ObjectNode objectNode = TrackYourAppealJsonBuilder.buildTrackYourAppealJson(caseData);
        assertJsonEquals(APPEAL_RECEIVED.getSerializedMessage(), objectNode);
    }

    @Test
    public void dwpRespondJsonTest() {
        CaseData caseData = DWP_RESPOND_CCD.getDeserializeMessage();
        ObjectNode objectNode = TrackYourAppealJsonBuilder.buildTrackYourAppealJson(caseData);
        assertJsonEquals(DWP_RESPOND.getSerializedMessage(), objectNode);
    }

}

