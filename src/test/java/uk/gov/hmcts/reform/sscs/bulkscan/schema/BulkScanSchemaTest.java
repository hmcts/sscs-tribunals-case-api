package uk.gov.hmcts.reform.sscs.bulkscan.schema;

import java.io.InputStream;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

public class BulkScanSchemaTest {

    final InputStream inputStream = getClass().getResourceAsStream("/config/schema/sscs-bulk-scan-schema.json");

    final JSONObject validJsonData = new JSONObject(
            new JSONTokener(getClass().getResourceAsStream("/schema/valid_ocr_agreed.json")));

    final JSONObject invalidJsonData = new JSONObject(
        new JSONTokener(getClass().getResourceAsStream("/schema/invalid_ocr_key.json")));

    final Schema schema = SchemaLoader.load(new JSONObject(new JSONTokener(inputStream)));

    @Test
    public void givenValidInputAgreedWithOcrProvider_thenValidateAgainstSchema() throws ValidationException {
        schema.validate(validJsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidKey_throwExceptionWhenValidatingAgainstSchema() throws ValidationException {
        schema.validate(invalidJsonData);
    }
}
