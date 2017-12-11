package uk.gov.hmcts.sscs.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.sscs.tribunals.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.tribunals.domain.corecase.Event;


class CcdCaseSerializer extends StdSerializer<CcdCase> {

    private static final Logger log = LoggerFactory.getLogger(CcdCaseSerializer.class);

    private ObjectMapper mapper;

    public CcdCaseSerializer(Class<CcdCase> t) {
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

        if (ccd.getCaseReference() != null) {
            jgen.writeStringField("caseReference", ccd.getCaseReference());
        }

        if (ccd.getStatus() != null) {
            jgen.writeStringField("status", ccd.getStatus().name());
        }

        if (ccd.getAppeal() != null && ccd.getAppeal().getAppealNumber() != null) {
            jgen.writeStringField("appealNumber", ccd.getAppeal().getAppealNumber());
        }

        if (ccd.getAppellant() != null && ccd.getAppellant().getName().getFullName() != null) {
            jgen.writeStringField("name", ccd.getAppellant().getName().getFullName());
        }

        jgen = generateEventJson(jgen, ccd.getEvents());

        jgen.writeEndObject();
    }



    public JsonGenerator generateEventJson(JsonGenerator jgen, List<Event> events)
            throws IOException {
        if (events.isEmpty()) {
            return jgen;
        }

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'");

        jgen.writeArrayFieldStart("events");

        for (Event event: events) {
            jgen.writeStartObject();

            if (event.getDate() != null) {
                jgen.writeStringField("date", event.getDate().format(f));
            }

            if (event.getType() != null) {
                jgen.writeStringField("type", event.getType().name());
            }

            if (event.getEvidenceType() != null) {
                jgen.writeStringField("evidenceType", event.getEvidenceType());
            }

            if (event.getContentKey() != null) {
                jgen.writeStringField("contentKey", event.getContentKey());
            }

            if (event.getDwpResponseDate() != null) {
                jgen.writeStringField("dwpResponseDate", event.getDwpResponseDate().format(f));
            }

            if (event.getDecisionLetterReceivedByDate() != null) {
                jgen.writeStringField("decisionLetterReceivedByDate",
                        event.getDecisionLetterReceivedByDate().format(f));
            }

            if (event.getAdjournedLetterReceivedByDate() != null) {
                jgen.writeStringField("adjournedLetterReceivedByDate", event
                        .getAdjournedLetterReceivedByDate().format(f));
            }

            if (event.getAdjournedDate() != null) {
                jgen.writeStringField("adjournedDate", event.getAdjournedDate().format(f));
            }

            if (event.getHearingContactDate() != null) {
                jgen.writeStringField("hearingContactDate", event.getHearingContactDate()
                        .format(f));
            }

            if (event.getEvidenceProvidedBy() != null) {
                jgen.writeStringField("evidenceProvidedBy", event.getEvidenceProvidedBy());
            }

            if (event.getHearing() != null) {

                if (event.getHearing().getDateTime() != null) {
                    jgen.writeStringField("hearingDateTime", event.getHearing().getDateTime()
                            .format(f));
                }

                if (event.getHearing().getAddress() != null) {

                    if (event.getHearing().getAddress().getLine1() != null) {
                        jgen.writeStringField("addressLine1",
                                event.getHearing().getAddress().getLine1());
                    }

                    if (event.getHearing().getAddress().getLine2() != null) {
                        jgen.writeStringField("addressLine2",
                                event.getHearing().getAddress().getLine2());
                    }

                    if (event.getHearing().getAddress().getTown() != null) {
                        jgen.writeStringField("addressLine3",
                                event.getHearing().getAddress().getTown());
                    }

                    if (event.getHearing().getAddress().getCounty() != null) {
                        jgen.writeStringField("addressCounty",
                                event.getHearing().getAddress().getCounty());
                    }

                    if (event.getHearing().getAddress().getPostcode() != null) {
                        jgen.writeStringField("postcode",
                                event.getHearing().getAddress().getPostcode());
                    }

                    if (event.getHearing().getAddress().getGoogleMapUrl() != null) {
                        jgen.writeStringField("googleMapUrl",
                                event.getHearing().getAddress().getGoogleMapUrl());
                    }
                }
            }
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

        return jgen;
    }
}