package uk.gov.hmcts.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS;
import static uk.gov.hmcts.sscs.util.SyaJsonMessageSerializer.WITHOUT_HEARING;
import static uk.gov.hmcts.sscs.util.SyaJsonMessageSerializer.WITHOUT_REPRESENTATIVE;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.model.robotics.RoboticsWrapper;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RoboticsServiceIt {

    @Autowired
    private RoboticsService roboticsService;

    @Test
    public void givenSyaData_makeValidRoboticsJsonThatValidatesAgainstSchema() {

        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        RoboticsWrapper appeal =
            RoboticsWrapper
                .builder()
                .syaCaseWrapper(syaCaseWrapper)
                .ccdCaseId(1234L)
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
                .syaCaseWrapper(syaCaseWrapper)
                .ccdCaseId(1234L)
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
                .syaCaseWrapper(syaCaseWrapper)
                .ccdCaseId(1234L)
                .build();

        JSONObject result = roboticsService.createRobotics(appeal);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertTrue(result.has("representative"));
        assertFalse(result.has("hearingArrangements"));
    }

}
