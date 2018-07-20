package uk.gov.hmcts.sscs.json;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import java.time.LocalDate;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.model.robotics.RoboticsWrapper;

public class RoboticsJsonMapperTest {

    private RoboticsJsonMapper roboticsJsonMapper = new RoboticsJsonMapper();
    private RoboticsWrapper appeal;

    @Before
    public void setup() {
        appeal = RoboticsWrapper
                    .builder()
                    .syaCaseWrapper(getSyaCaseWrapper())
                    .ccdCaseId(123L)
                    .build();
    }

    @Test
    public void mapsAppealToRoboticsJson() {

        JSONObject roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals(
            "If this fails, add an assertion below, do not just increment the number :)", 13,
            roboticsJson.length()
        );

        assertEquals("002DD", roboticsJson.get("caseCode"));
        assertEquals(123L, roboticsJson.get("caseId"));
        assertEquals("AB877533C", roboticsJson.get("appellantNino"));
        assertEquals("Bedford", roboticsJson.get("appellantPostCode"));
        assertEquals(LocalDate.now().toString(), roboticsJson.get("appealDate"));
        assertEquals("2018-02-01", roboticsJson.get("mrnDate"));
        assertEquals("Lost my paperwork", roboticsJson.get("mrnReasonForBeingLate"));
        assertEquals("Liverpool2 SSO", roboticsJson.get("pipNumber"));
        assertEquals("Oral", roboticsJson.get("hearingType"));
        assertEquals("Mr Joe Bloggs", roboticsJson.get("hearingRequestParty"));

        assertEquals(
            "If this fails, add an assertion below, do not just increment the number :)", 10,
            roboticsJson.getJSONObject("appellant").length()
        );

        assertEquals("Mr", roboticsJson.getJSONObject("appellant").get("title"));
        assertEquals("Joe", roboticsJson.getJSONObject("appellant").get("firstName"));
        assertEquals("Bloggs", roboticsJson.getJSONObject("appellant").get("lastName"));
        assertEquals("123 Hairy Lane", roboticsJson.getJSONObject("appellant").get("addressLine1"));
        assertEquals("Off Hairy Park", roboticsJson.getJSONObject("appellant").get("addressLine2"));
        assertEquals("Hairyfield", roboticsJson.getJSONObject("appellant").get("townOrCity"));
        assertEquals("Kent", roboticsJson.getJSONObject("appellant").get("county"));
        assertEquals("TN32 6PL", roboticsJson.getJSONObject("appellant").get("postCode"));
        assertEquals("07411222222", roboticsJson.getJSONObject("appellant").get("phoneNumber"));
        assertEquals("joe@bloggs.com", roboticsJson.getJSONObject("appellant").get("email"));

        assertEquals(
            "If this fails, add an assertion, do not just increment the number :)", 11,
            roboticsJson.getJSONObject("representative").length()
        );

        assertEquals("Mr", roboticsJson.getJSONObject("representative").get("title"));
        assertEquals("Harry", roboticsJson.getJSONObject("representative").get("firstName"));
        assertEquals("Potter", roboticsJson.getJSONObject("representative").get("lastName"));
        assertEquals("HP Ltd", roboticsJson.getJSONObject("representative").get("organisation"));
        assertEquals("123 Hairy Lane", roboticsJson.getJSONObject("representative").get("addressLine1"));
        assertEquals("Off Hairy Park", roboticsJson.getJSONObject("representative").get("addressLine2"));
        assertEquals("Town", roboticsJson.getJSONObject("representative").get("townOrCity"));
        assertEquals("County", roboticsJson.getJSONObject("representative").get("county"));
        assertEquals("CM14 4LQ", roboticsJson.getJSONObject("representative").get("postCode"));
        assertEquals("07411999999", roboticsJson.getJSONObject("representative").get("phoneNumber"));
        assertEquals("harry.potter@wizards.com", roboticsJson.getJSONObject("representative").get("email"));

        assertEquals(
            "If this fails, add an assertion below, do not just increment the number :)", 6,
            roboticsJson.getJSONObject("hearingArrangements").length()
        );

        assertEquals("Yes", roboticsJson.getJSONObject("hearingArrangements").get("languageInterpreter"));
        assertEquals("Yes", roboticsJson.getJSONObject("hearingArrangements").get("signLanguageInterpreter"));
        assertEquals("Yes", roboticsJson.getJSONObject("hearingArrangements").get("hearingLoop"));
        assertEquals("No", roboticsJson.getJSONObject("hearingArrangements").get("accessibleHearingRoom"));
        assertEquals("Yes, this...", roboticsJson.getJSONObject("hearingArrangements").get("other"));
        assertEquals(3, roboticsJson.getJSONObject("hearingArrangements").getJSONArray("datesCantAttend").length());
        assertEquals("2018-04-04", roboticsJson.getJSONObject("hearingArrangements").getJSONArray("datesCantAttend").get(0));
        assertEquals("2018-04-05", roboticsJson.getJSONObject("hearingArrangements").getJSONArray("datesCantAttend").get(1));
        assertEquals("2018-04-06", roboticsJson.getJSONObject("hearingArrangements").getJSONArray("datesCantAttend").get(2));
    }

    @Test
    public void mapRepTitleToDefaultValuesWhenSetToNull() {

        appeal.getSyaCaseWrapper().getRepresentative().setTitle(null);

        JSONObject roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals("s/m", roboticsJson.getJSONObject("representative").get("title"));
    }

    @Test
    public void mapRepFirstNameToDefaultValuesWhenSetToNull() {

        appeal.getSyaCaseWrapper().getRepresentative().setFirstName(null);

        JSONObject roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals(".", roboticsJson.getJSONObject("representative").get("firstName"));
    }

    @Test
    public void mapRepLastNameToDefaultValuesWhenSetToNull() {

        appeal.getSyaCaseWrapper().getRepresentative().setLastName(null);

        JSONObject roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals(".", roboticsJson.getJSONObject("representative").get("lastName"));
    }

}
