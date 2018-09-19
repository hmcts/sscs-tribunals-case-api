package uk.gov.hmcts.sscs.json;

import com.google.common.collect.ImmutableList;
import org.json.JSONObject;
import org.junit.Test;

public class RoboticsJsonValidatorTest {

    private RoboticsJsonValidator roboticsJsonValidator = new RoboticsJsonValidator("/schema/sscs-robotics.json");

    @Test
    public void validateValidRoboticsJson() {

        JSONObject validRoboticsJson = createRoboticsJson();
        roboticsJsonValidator.validate(validRoboticsJson);
    }

    @Test(expected = RuntimeException.class)
    public void validateInvalidRoboticsJson() {

        JSONObject invalidRoboticsJson = createRoboticsJson();
        invalidRoboticsJson.remove("caseId");

        roboticsJsonValidator.validate(invalidRoboticsJson);
    }

    private JSONObject createRoboticsJson() {

        JSONObject roboticsJson = new JSONObject();

        roboticsJson.put("caseCode", "002DD");
        roboticsJson.put("caseId", 123L);
        roboticsJson.put("appellantNino", "AB877533C");
        roboticsJson.put("appellantPostCode", "Bedford");
        roboticsJson.put("appealDate", "2018-03-01");
        roboticsJson.put("mrnDate", "2018-02-01");
        roboticsJson.put("mrnReasonForBeingLate", "Lost my paperwork");
        roboticsJson.put("pipNumber", "Liverpool2 SSO");
        roboticsJson.put("hearingType", "Oral");
        roboticsJson.put("hearingRequestParty", "Mr Joe Bloggs");
        roboticsJson.put("evidencePresent", "Yes");

        roboticsJson.put("appellant", new JSONObject());
        roboticsJson.getJSONObject("appellant").put("title", "Mr");
        roboticsJson.getJSONObject("appellant").put("firstName", "Joe");
        roboticsJson.getJSONObject("appellant").put("lastName", "Bloggs");
        roboticsJson.getJSONObject("appellant").put("addressLine1", "123 Hairy Lane");
        roboticsJson.getJSONObject("appellant").put("addressLine2", "Off Hairy Park");
        roboticsJson.getJSONObject("appellant").put("townOrCity", "Hairyfield");
        roboticsJson.getJSONObject("appellant").put("county", "Kent");
        roboticsJson.getJSONObject("appellant").put("postCode", "TN32 6PL");
        roboticsJson.getJSONObject("appellant").put("phoneNumber", "07411222222");
        roboticsJson.getJSONObject("appellant").put("email", "joe@bloggs.com");

        roboticsJson.put("representative", new JSONObject());
        roboticsJson.getJSONObject("representative").put("firstName", "Harry");
        roboticsJson.getJSONObject("representative").put("lastName", "Potter");
        roboticsJson.getJSONObject("representative").put("organisation", "HP Ltd");
        roboticsJson.getJSONObject("representative").put("addressLine1", "123 Hairy Lane");
        roboticsJson.getJSONObject("representative").put("addressLine2", "Off Hairy Park");
        roboticsJson.getJSONObject("representative").put("townOrCity", "Town");
        roboticsJson.getJSONObject("representative").put("county", "County");
        roboticsJson.getJSONObject("representative").put("postCode", "CM14 4LQ");
        roboticsJson.getJSONObject("representative").put("phoneNumber", "07411999999");
        roboticsJson.getJSONObject("representative").put("email", "harry.potter@wizards.com");

        roboticsJson.put("hearingArrangements", new JSONObject());
        roboticsJson.getJSONObject("hearingArrangements").put("languageInterpreter", "Yes");
        roboticsJson.getJSONObject("hearingArrangements").put("signLanguageInterpreter", "Yes");
        roboticsJson.getJSONObject("hearingArrangements").put("hearingLoop", "Yes");
        roboticsJson.getJSONObject("hearingArrangements").put("accessibleHearingRoom", "No");
        roboticsJson.getJSONObject("hearingArrangements").put("other", "Yes, this...");
        roboticsJson.getJSONObject("hearingArrangements").put(
            "datesCantAttend",
            ImmutableList.of("2018-04-04", "2018-04-05", "2018-04-06")
        );

        return roboticsJson;
    }

}
