package uk.gov.hmcts.sscs.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        String venueName = "Bromley";

        RoboticsWrapper appeal =
            RoboticsWrapper
                .builder()
                .syaCaseWrapper(getSyaCaseWrapper())
                .ccdCaseId(123L).venueName(venueName)
                .build();

        JSONObject roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals(
            "If this fails, add an assertion below, do not just increment the number :)", 13,
            roboticsJson.length()
        );

        assertEquals("002DD", roboticsJson.get("caseCode"));
        assertEquals(123L, roboticsJson.get("caseId"));
        assertEquals("AB877533C", roboticsJson.get("appellantNino"));
        assertEquals(venueName, roboticsJson.get("appellantPostCode"));
        assertEquals(LocalDate.now().toString(), roboticsJson.get("appealDate"));
        assertEquals("2018-02-01", roboticsJson.get("mrnDate"));
        assertEquals("Lost my paperwork", roboticsJson.get("mrnReasonForBeingLate"));
        assertEquals("DWP PIP (1)", roboticsJson.get("pipNumber"));
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

        assertEquals("An interpreter language", roboticsJson.getJSONObject("hearingArrangements").get("languageInterpreter"));
        assertEquals("A sign language", roboticsJson.getJSONObject("hearingArrangements").get("signLanguageInterpreter"));
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

    @Test
    public void givenLanguageInterpreterIsTrue_thenSetToLanguageInterpreterType() {

        appeal.getSyaCaseWrapper().getSyaHearingOptions().setInterpreterLanguageType("My Language");
        appeal.getSyaCaseWrapper().getSyaHearingOptions().getArrangements().setLanguageInterpreter(true);

        JSONObject roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals("My Language", roboticsJson.getJSONObject("hearingArrangements").get("languageInterpreter"));
    }

    @Test
    public void givenLanguageInterpreterIsFalse_thenDoNotSetLanguageInterpreter() {

        appeal.getSyaCaseWrapper().getSyaHearingOptions().setInterpreterLanguageType("My Language");
        appeal.getSyaCaseWrapper().getSyaHearingOptions().getArrangements().setLanguageInterpreter(false);

        JSONObject roboticsJson = roboticsJsonMapper.map(appeal);

        assertFalse(roboticsJson.getJSONObject("hearingArrangements").has("languageInterpreter"));
    }

    @Test
    public void givenLanguageInterpreterIsTrueAndInterpreterLanguageTypeIsNull_thenDoNotSetLanguageInterpreter() {

        appeal.getSyaCaseWrapper().getSyaHearingOptions().setInterpreterLanguageType(null);
        appeal.getSyaCaseWrapper().getSyaHearingOptions().getArrangements().setLanguageInterpreter(true);

        JSONObject roboticsJson = roboticsJsonMapper.map(appeal);

        assertFalse(roboticsJson.getJSONObject("hearingArrangements").has("languageInterpreter"));
    }

    @Test
    public void givenSignLanguageInterpreterIsTrue_thenSetToSignLanguageInterpreterType() {

        appeal.getSyaCaseWrapper().getSyaHearingOptions().setSignLanguageType("My Language");
        appeal.getSyaCaseWrapper().getSyaHearingOptions().getArrangements().setSignLanguageInterpreter(true);

        JSONObject roboticsJson = roboticsJsonMapper.map(appeal);

        assertEquals("My Language", roboticsJson.getJSONObject("hearingArrangements").get("signLanguageInterpreter"));
    }

    @Test
    public void givenSignLanguageInterpreterIsFalse_thenDoNotSetSignLanguageInterpreter() {

        appeal.getSyaCaseWrapper().getSyaHearingOptions().setSignLanguageType("My Language");
        appeal.getSyaCaseWrapper().getSyaHearingOptions().getArrangements().setSignLanguageInterpreter(false);

        JSONObject roboticsJson = roboticsJsonMapper.map(appeal);

        assertFalse(roboticsJson.getJSONObject("hearingArrangements").has("signLanguageInterpreter"));
    }

    @Test
    public void givenSignLanguageInterpreterIsTrueAndSignInterpreterLanguageTypeIsNull_thenDoNotSetSignLanguageInterpreter() {

        appeal.getSyaCaseWrapper().getSyaHearingOptions().setSignLanguageType(null);
        appeal.getSyaCaseWrapper().getSyaHearingOptions().getArrangements().setSignLanguageInterpreter(true);

        JSONObject roboticsJson = roboticsJsonMapper.map(appeal);

        assertFalse(roboticsJson.getJSONObject("hearingArrangements").has("signLanguageInterpreter"));
    }

}
