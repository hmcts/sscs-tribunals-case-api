package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.model.robotics.RoboticsWrapper;

public class RoboticsServiceTest {

    private RoboticsService service;

    @Before
    public void setup() {
        service = new RoboticsService();
    }

    @Test
    public void validateValidRoboticsAndReturnJsonObject() {
        RoboticsWrapper appealData = RoboticsWrapper.builder().syaCaseWrapper(getSyaCaseWrapper()).ccdCaseId(1L).build();

        JSONObject result = service.generateRobotics(appealData);

        assertNotNull(result);
    }

    @Test(expected = ValidationException.class)
    public void givenInvalidRoboticsThenThrowExceptionWhenValidatedAgainstSchema() {
        RoboticsWrapper appealData = RoboticsWrapper.builder().syaCaseWrapper(getSyaCaseWrapper()).ccdCaseId(null).build();

        service.generateRobotics(appealData);
    }

}