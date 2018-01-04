package uk.gov.hmcts.sscs.json;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import uk.gov.hmcts.sscs.builder.CcdCaseBuilder;
import uk.gov.hmcts.sscs.domain.corecase.*;

public class CcdCaseSerializerTest {

    private CcdCaseSerializer ccdCaseSerializer;
    private Appeal appeal;
    private Appellant appellant;
    private String caseReference;
    private Event event1;
    private Event event2;
    private CcdCase ccdCase;

    private void buildCaseData() {
        appeal = new Appeal();
        appeal.setAppealNumber("ME100");

        appellant = new Appellant(new Name("Dr", "Kenny", "Rodgers"),
                null, null, null, null, null);

        caseReference = "SC777/77/77777";

        Address hearingAddress = CcdCaseBuilder.address(true);

        Hearing hearingEvent = new Hearing(hearingAddress, ZonedDateTime.now());

        event1 = new Event(ZonedDateTime.now(), ZonedDateTime.now(), EventType.ADJOURNED, "medical",
            "The medical company", EventType.ADJOURNED.getContentKey(), hearingEvent,
            ZonedDateTime.now(), ZonedDateTime.now(), ZonedDateTime.now(), ZonedDateTime.now());

        ccdCase = new CcdCase(appeal, appellant, null, null, null, caseReference,
            EventType.APPEAL_RECEIVED, Arrays.asList(event1));

        ccdCaseSerializer = new CcdCaseSerializer(CcdCase.class);
    }

    private void buildPartialCaseData() {
        caseReference = "SC777/77/77777";

        appeal = new Appeal();
        appeal.setAppealNumber("ME100");

        appellant = new Appellant(new Name("Dr", "Kenny", "Rodgers"),
                null, null, null, null, null);

        event1 = new Event(ZonedDateTime.now(), null, EventType.DORMANT, null,
               null,  EventType.DORMANT.getContentKey(), null, ZonedDateTime.now(),null, null, null);

        ccdCase = new CcdCase(appeal, appellant, null, null, null, caseReference,
                EventType.DORMANT, Arrays.asList(event1));

        ccdCaseSerializer = new CcdCaseSerializer(CcdCase.class);
    }

    private void buildMultipleEventData() {
        event1 = new Event(ZonedDateTime.now(), ZonedDateTime.now(), EventType.ADJOURNED, "medical",
                EventType.ADJOURNED.getContentKey());
        event2 = new Event(ZonedDateTime.now(), ZonedDateTime.now(), EventType.DWP_RESPOND, null,
                EventType.DWP_RESPOND.getContentKey());

        ccdCase = new CcdCase(null, null, null, null, null, null, null,
                Arrays.asList(event1, event2));

        ccdCaseSerializer = new CcdCaseSerializer(CcdCase.class);
    }

    @Test
    public void serializeAllCaseDataToJson() throws JSONException {

        buildCaseData();

        String expected = "{"
            + "\"caseReference\":\"" + caseReference + "\","
            + "\"appealNumber\":\"" + appeal.getAppealNumber() + "\","
            + "\"name\":\"" + appellant.getName().getFullName() + "\","
            + "\"status\":\"APPEAL_RECEIVED\","
            + "\"events\": [{"
                + "\"date\":\"" + event1.getDate().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\","
                + "\"type\":\"" + event1.getType().name() + "\","
                + "\"evidenceType\":\"" + event1.getEvidenceType() + "\","
                + "\"evidenceProvidedBy\":\"" + event1.getEvidenceProvidedBy() + "\","
                + "\"contentKey\":\"" + event1.getContentKey() + "\","
                + "\"dwpResponseDate\":\"" + event1.getDwpResponseDate().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\","
                + "\"addressLine1\":\"" + event1.getHearing().getAddress().getLine1() + "\","
                + "\"addressLine2\":\"" + event1.getHearing().getAddress().getLine2() + "\","
                + "\"addressLine3\":\"" + event1.getHearing().getAddress().getTown() + "\","
                + "\"addressCounty\":\"" + event1.getHearing().getAddress().getCounty() + "\","
                + "\"postcode\":\"" + event1.getHearing().getAddress().getPostcode() + "\","
                + "\"hearingDateTime\":\"" + event1.getHearing().getDateTime().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'"))  + "\","
                + "\"googleMapUrl\":\"" + event1.getHearing().getAddress().getGoogleMapUrl() + "\","
                + "\"decisionLetterReceivedByDate\":\"" + event1.getDecisionLetterReceivedByDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\","
                + "\"adjournedLetterReceivedByDate\":\"" + event1
                .getAdjournedLetterReceivedByDate().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\","
                + "\"adjournedDate\":\"" + event1.getAdjournedDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\","
                + "\"hearingContactDate\":\"" + event1.getHearingContactDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\""
            + "}]}";

        JSONAssert.assertEquals(expected, ccdCaseSerializer.handle(ccdCase), true);
    }

    @Test
    public void ignoreMissingDataFromJson() throws JSONException {

        buildPartialCaseData();

        String expected = "{"
            + "\"caseReference\":\"" + caseReference + "\","
            + "\"appealNumber\":\"" + appeal.getAppealNumber() + "\","
            + "\"name\":\"" + appellant.getName().getFullName() + "\","
            + "\"status\":\"DORMANT\","
            + "\"events\": [{"
            + "\"date\":\"" + event1.getDate().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\","
            + "\"type\":\"" + event1.getType().name() + "\","
            + "\"contentKey\":\"" + event1.getContentKey() + "\","
            + "\"decisionLetterReceivedByDate\":\"" + event1.getDecisionLetterReceivedByDate()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\""
            + "}]}";

        JSONAssert.assertEquals(expected, ccdCaseSerializer.handle(ccdCase), true);
    }

    @Test
    public void serializeMultipleEventsToJson() throws JSONException {

        buildMultipleEventData();

        String expected = "{"
            + "\"events\":[{"
            + "\"date\":\"" + event1.getDate().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\","
            + "\"type\":\"" + event1.getType().name() + "\","
            + "\"evidenceType\":\"" + event1.getEvidenceType() + "\","
            + "\"contentKey\":\"" + event1.getContentKey() + "\","
            + "\"dwpResponseDate\":\"" + event1.getDwpResponseDate().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\""
            + "},{"
            + "\"date\":\"" + event2.getDate().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\","
            + "\"type\":\"" + event2.getType().name() + "\","
            + "\"contentKey\":\"" + event2.getContentKey() + "\","
            + "\"dwpResponseDate\":\"" + event2.getDwpResponseDate().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'")) + "\""
            + "}]}";

        JSONAssert.assertEquals(expected, ccdCaseSerializer.handle(ccdCase), true);
    }
}
