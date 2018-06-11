package uk.gov.hmcts.sscs.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;

public class RoboticsSchemaTest {

    InputStream inputStream = getClass().getResourceAsStream("/schema/sscs-robotics.json");

    JSONObject jsonData;

    Schema schema;

    @Before
    public void setup() {
        schema =  SchemaLoader.load(new JSONObject(new JSONTokener(inputStream)));

        jsonData = new JSONObject(
                new JSONTokener(getClass().getResourceAsStream("/schema/valid_robotics.json")));
    }

    @Test
    public void givenValidInput_thenValidateAgainstSchema() throws ValidationException {
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForCaseCode_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "002CC", "caseCode");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForNino_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "JB01X123B", "appellantNino");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForPostCode_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "B231ABXXX", "appellant", "postCode");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForPhoneNumber_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "0798", "appellant", "phoneNumber");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForCaseCreatedDate_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "2018/06/01", "caseCreatedDate");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForMrnDate_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "2018/06/02", "mrnDate");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForAppealDate_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "2018/06/03", "appealDate");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForHearingType_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "Computer", "hearingType");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForWantsToAttendHearing_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "Bla", "wantsToAttendHearing");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForLanguageInterpreter_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "Bla", "hearingArrangements", "languageInterpreter");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenYesInputForLanguageInterpreterWithNoInterpreterLanguageType_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "Yes", "hearingArrangements", "languageInterpreter");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForSignLanguageInterpreter_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "Bla", "hearingArrangements", "signLanguageInterpreter");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForHearingLoop_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "Bla", "hearingArrangements", "hearingLoop");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForAccessibleHearingRoom_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "Bla", "hearingArrangements", "accessibleHearingRoom");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForDisabilityAccess_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedJson(jsonData.toString(), "Bla", "hearingArrangements", "disabilityAccess");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForHearingDatesCantAttend_throwExceptionWhenValidatingAgainstSchema() throws ValidationException {
        jsonData = new JSONObject(jsonData.toString().replace("2018-08-12", "2018/08/12"));
        schema.validate(jsonData);
    }

    private static JSONObject updateEmbeddedJson(String json, String value, String... keys) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map map = objectMapper.readValue(json, Map.class);

        Map t = map;
        for (int i = 0; i < keys.length - 1; i++) {
            t = (Map) t.get(keys[i]);
        }

        t.put(keys[keys.length - 1], value);

        JSONObject jsonObject = new JSONObject(objectMapper.writeValueAsString(map));

        return jsonObject;
    }

}
