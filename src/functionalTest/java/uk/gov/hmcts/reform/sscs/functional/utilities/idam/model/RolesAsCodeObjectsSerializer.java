package uk.gov.hmcts.reform.sscs.functional.utilities.idam.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.List;

public class RolesAsCodeObjectsSerializer extends JsonSerializer<List<String>> {

    @Override
    public void serialize(List<String> roles, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();
        if (roles != null) {
            for (String role : roles) {
                gen.writeStartObject();
                gen.writeStringField("code", role);
                gen.writeEndObject();
            }
        }
        gen.writeEndArray();
    }
}
