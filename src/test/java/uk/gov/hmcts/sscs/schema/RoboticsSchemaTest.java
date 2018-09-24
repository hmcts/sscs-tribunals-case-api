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
import org.junit.Test;

public class RoboticsSchemaTest {

    InputStream inputStream = getClass().getResourceAsStream("/schema/sscs-robotics.json");

    JSONObject jsonData = new JSONObject(
            new JSONTokener(getClass().getResourceAsStream("/schema/valid_robotics_agreed.json")));

    Schema schema = SchemaLoader.load(new JSONObject(new JSONTokener(inputStream)));

    @Test
    public void givenValidInputAgreedWithAutomationTeam_thenValidateAgainstSchema() throws ValidationException {
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForCaseCode_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "002CC", "caseCode");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForPostCode_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "B231ABXXX", "appellant", "postCode");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForPhoneNumber_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "0798", "appellant", "phoneNumber");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForCaseCreatedDate_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "2018/06/01", "caseCreatedDate");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForMrnDate_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "2018/06/02", "mrnDate");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForAppealDate_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "2018/06/03", "appealDate");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForHearingType_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Computer", "hearingType");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenOralInputForLanguageInterpreterWithNoHearingRequestParty_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData =  updateEmbeddedProperty(jsonData.toString(), "Oral", "hearingType");
        jsonData = removeProperty(jsonData.toString(), "hearingRequestParty");
        schema.validate(jsonData);
    }

    @Test
    public void givenPaperInputForLanguageInterpreterWithNoHearingRequestParty_doesNotThrowExceptionWhenValidatingAgainstSchema() throws IOException {
        jsonData =  updateEmbeddedProperty(jsonData.toString(), "Paper", "hearingType");
        jsonData = removeProperty(jsonData.toString(), "hearingRequestParty");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForWantsToAttendHearing_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Bla", "wantsToAttendHearing");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForHearingLoop_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Bla", "hearingArrangements", "hearingLoop");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForAccessibleHearingRoom_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Bla", "hearingArrangements", "accessibleHearingRoom");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForDisabilityAccess_throwExceptionWhenValidatingAgainstSchema() throws ValidationException, IOException {
        jsonData = updateEmbeddedProperty(jsonData.toString(), "Bla", "hearingArrangements", "disabilityAccess");
        schema.validate(jsonData);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidInputForHearingDatesCantAttend_throwExceptionWhenValidatingAgainstSchema() throws ValidationException {
        jsonData = new JSONObject(jsonData.toString().replace("2018-08-12", "2018/08/12"));
        schema.validate(jsonData);
    }

    private static JSONObject updateEmbeddedProperty(String json, String value, String... keys) throws IOException {
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

    private static JSONObject removeProperty(String json, String key) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map map = objectMapper.readValue(json, Map.class);

        map.remove(key);

        String jsonString = objectMapper.writeValueAsString(map);
        JSONObject jsonObject = new JSONObject(jsonString);

        return jsonObject;
    }

}
