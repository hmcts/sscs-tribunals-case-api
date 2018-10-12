package uk.gov.hmcts.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static uk.gov.hmcts.sscs.util.SyaJsonMessageSerializer.*;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.domain.robotics.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.service.RoboticsService;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RoboticsServiceIt {

    @Autowired
    private RoboticsService roboticsService;

    @Autowired
    private SubmitYourAppealToCcdCaseDataDeserializer deserializer;

    @Test
    public void givenSyaData_makeValidRoboticsJsonThatValidatesAgainstSchema() {

        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();

        RoboticsWrapper appeal =
            RoboticsWrapper
                .builder()
                .sscsCaseData(deserializer.convertSyaToCcdCaseData(syaCaseWrapper))
                .ccdCaseId(1234L)
                .evidencePresent("Yes")
                .build();

        JSONObject result = roboticsService.createRobotics(appeal);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertTrue(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
    }

    @Test
    public void givenSyaDataWithoutRepresentative_makeValidRoboticsJsonThatValidatesAgainstSchema() {

        SyaCaseWrapper syaCaseWrapper = WITHOUT_REPRESENTATIVE.getDeserializeMessage();
        RoboticsWrapper appeal =
            RoboticsWrapper
                .builder()
                .sscsCaseData(deserializer.convertSyaToCcdCaseData(syaCaseWrapper))
                .ccdCaseId(1234L)
                .evidencePresent("Yes")
                .build();

        JSONObject result = roboticsService.createRobotics(appeal);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
    }

    @Test
    public void givenSyaDataWithoutHearingArrangements_makeValidRoboticsJsonThatValidatesAgainstSchema() {

        SyaCaseWrapper syaCaseWrapper = WITHOUT_HEARING.getDeserializeMessage();
        RoboticsWrapper appeal =
            RoboticsWrapper
                .builder()
                .sscsCaseData(deserializer.convertSyaToCcdCaseData(syaCaseWrapper))
                .ccdCaseId(1234L)
                .evidencePresent("Yes")
                .build();

        JSONObject result = roboticsService.createRobotics(appeal);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertTrue(result.has("representative"));
        assertFalse(result.has("hearingArrangements"));
    }

}
