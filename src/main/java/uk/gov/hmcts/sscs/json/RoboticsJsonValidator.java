package uk.gov.hmcts.sscs.json;

import java.io.InputStream;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RoboticsJsonValidator {

    private final String schemaResourceLocation;
    private Schema schema = null;

    @Autowired
    public RoboticsJsonValidator(
        @Value("${robotics.schema.resource.location}") String schemaResourceLocation
    ) {
        this.schemaResourceLocation = schemaResourceLocation;
    }

    public void validate(JSONObject roboticsJson) {

        tryLoadSchema();

        schema.validate(roboticsJson);
    }

    private synchronized void tryLoadSchema() {

        if (schema != null) {
            return;
        }

        InputStream inputStream = getClass().getResourceAsStream(schemaResourceLocation);
        schema = SchemaLoader.load(new JSONObject(new JSONTokener(inputStream)));
    }
}
