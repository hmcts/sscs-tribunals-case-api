package uk.gov.hmcts.sscs.transform.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.sscs.domain.corecase.*;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


class TrackYourAppealJsonFromCcdCaseSerializer extends StdSerializer<CcdCase> {

    private static final Logger log = LoggerFactory.getLogger(TrackYourAppealJsonFromCcdCaseSerializer.class);

    private ObjectMapper mapper;

    TrackYourAppealJsonFromCcdCaseSerializer(Class<CcdCase> t) {
        super(t);

        mapper = new ObjectMapper();

        SimpleModule mod = new SimpleModule();
        mod.addSerializer(this);

        mapper.registerModule(mod);
    }

    public String handle(CcdCase ccdCase) {
        String json = "";
        try {
            json = mapper.writeValueAsString(ccdCase);
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException : ", e);
        }
        return json;
    }

    @Override
    public void serialize(CcdCase ccd,
                          JsonGenerator jgen,
                          SerializerProvider sp) throws IOException {

        jgen.writeStartObject();

        writeCcd(jgen, ccd);

        generateEventJson(jgen, ccd.getEvents());

        jgen.writeEndObject();
    }

    private void writeCcd(JsonGenerator jgen, CcdCase ccd) throws IOException {
        checkAndWrite(jgen, "caseReference", ccd.getCaseReference());
        checkAndWrite(jgen, "status", ccd.getEventType() != null ? ccd.getEventType().name() : null);

        writeCcdAppeal(jgen, ccd.getAppeal());
        writeCcdAppellant(jgen, ccd.getAppellant());
    }

    private void writeCcdAppeal(JsonGenerator jgen, Appeal appeal) throws IOException {
        if (appeal == null) return;

        checkAndWrite(jgen, "appealNumber", appeal.getAppealNumber());
    }

    private void writeCcdAppellant(JsonGenerator jgen, Appellant appellant) throws IOException {
        if (appellant == null) return;

        checkAndWrite(jgen, "name", appellant.getName() != null ? appellant.getName().getFullName() : null);
    }

    private void generateEventJson(JsonGenerator jgen, List<Event> events)
            throws IOException {
        if (events.isEmpty()) {
            return;
        }

        jgen.writeArrayFieldStart("events");

        for (Event event: events) {
            jgen.writeStartObject();

            writeEvent(jgen, event);
            jgen.writeEndObject();
        }
        jgen.writeEndArray();
    }

    private void writeEvent(JsonGenerator jgen, Event event) throws IOException {
        checkAndWriteFormat(jgen, "date", event.getDate());
        checkAndWrite(jgen, "type", event.getType() != null ? event.getType().name() : null);
        checkAndWrite(jgen, "evidenceType", event.getEvidenceType());
        checkAndWrite(jgen, "contentKey", event.getContentKey());
        checkAndWriteFormat(jgen, "dwpResponseDate", event.getDwpResponseDate());
        checkAndWriteFormat(jgen, "decisionLetterReceivedByDate", event.getDecisionLetterReceivedByDate());
        checkAndWriteFormat(jgen, "adjournedLetterReceivedByDate", event.getAdjournedLetterReceivedByDate());
        checkAndWriteFormat(jgen, "adjournedDate", event.getAdjournedDate());
        checkAndWriteFormat(jgen, "hearingContactDate", event.getHearingContactDate());
        checkAndWrite(jgen, "evidenceProvidedBy", event.getEvidenceProvidedBy());

        writeHearing(jgen, event.getHearing());
    }

    private void writeHearing(JsonGenerator jgen, Hearing hearing) throws IOException {
        if (hearing == null) return;
        checkAndWriteFormat(jgen, "hearingDateTime", hearing.getDateTime());
        writeAddress(jgen, hearing.getAddress());
    }

    private void writeAddress(JsonGenerator jgen, Address address) throws IOException {
        if (address == null) return;

        checkAndWrite(jgen, "addressLine1", address.getLine1());
        checkAndWrite(jgen, "addressLine2", address.getLine2());
        checkAndWrite(jgen, "addressLine3", address.getTown());
        checkAndWrite(jgen, "addressCounty", address.getCounty());
        checkAndWrite(jgen, "postcode", address.getPostcode());
        checkAndWrite(jgen, "googleMapUrl", address.getGoogleMapUrl());

    }

    private void checkAndWrite(JsonGenerator jgen, String label, String value) throws IOException {
        if (value != null) {
            jgen.writeStringField(label, value);
        }
    }

    private transient DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'");

    private void checkAndWriteFormat(JsonGenerator jgen, String label, ZonedDateTime value) throws IOException {
        if (value != null) {
            jgen.writeStringField(label, value.format(f));
        }
    }
}