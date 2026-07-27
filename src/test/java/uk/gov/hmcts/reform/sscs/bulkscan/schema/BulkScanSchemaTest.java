package uk.gov.hmcts.reform.sscs.bulkscan.schema;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class BulkScanSchemaTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    private static final String SCHEMA_PATH = "/config/schema/sscs-bulk-scan-schema.json";

    private Set<ValidationMessage> validate(String jsonPath) throws IOException {
        try (
            InputStream schemaStream = getClass().getResourceAsStream(SCHEMA_PATH);
            InputStream jsonStream = getClass().getResourceAsStream(jsonPath)
        ) {
            JsonNode schemaNode = mapper.readTree(schemaStream);
            JsonNode jsonNode = mapper.readTree(jsonStream);
            JsonSchema schema = schemaFactory.getSchema(schemaNode);
            return schema.validate(jsonNode);
        }
    }

    @Test
    public void givenValidInputAgreedWithOcrProvider_thenValidateAgainstSchema() throws IOException {
        Set<ValidationMessage> errors = validate("/schema/valid_ocr_agreed.json");
        assertTrue(errors.isEmpty());
    }

    @Test
    public void givenInvalidKey_throwExceptionWhenValidatingAgainstSchema() throws IOException {
        Set<ValidationMessage> errors = validate("/schema/invalid_ocr_key.json");
        assertFalse(errors.isEmpty());
    }
}
