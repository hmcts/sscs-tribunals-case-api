package uk.gov.hmcts.sscs.schema;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

public class RoboticsSchemaTest {

    JSONObject jsonSchema = new JSONObject(
            new JSONTokener(RoboticsSchemaTest.class.getResourceAsStream("/schema/sscs-robotics.json")));
    JSONObject jsonSubject;

    @Test(expected = ValidationException.class)
    public void givenInvalidInput_throwExceptionWhenValidatingAgainstSchema() throws ValidationException {

        jsonSubject = new JSONObject(
                new JSONTokener(RoboticsSchemaTest.class.getResourceAsStream("/schema/invalid_robotics.json")));

        Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(jsonSubject);
    }

    @Test
    public void givenValidInput_thenValidateAgainstSchema() throws ValidationException {
        jsonSubject = new JSONObject(
                new JSONTokener(RoboticsSchemaTest.class.getResourceAsStream("/schema/valid_robotics.json")));

        Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(jsonSubject);
    }

}
