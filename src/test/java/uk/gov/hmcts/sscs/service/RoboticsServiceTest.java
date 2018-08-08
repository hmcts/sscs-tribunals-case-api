package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static uk.gov.hmcts.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.json.RoboticsJsonMapper;
import uk.gov.hmcts.sscs.json.RoboticsJsonValidator;
import uk.gov.hmcts.sscs.model.robotics.RoboticsWrapper;

public class RoboticsServiceTest {

    private RoboticsJsonMapper roboticsJsonMapper = mock(RoboticsJsonMapper.class);
    private RoboticsJsonValidator roboticsJsonValidator = mock(RoboticsJsonValidator.class);

    private RoboticsService service;

    @Before
    public void setup() {
        service = new RoboticsService(roboticsJsonMapper, roboticsJsonValidator);
    }

    @Test
    public void createValidRoboticsAndReturnAsJsonObject() {

        RoboticsWrapper appeal =
            RoboticsWrapper
                .builder()
                .syaCaseWrapper(getSyaCaseWrapper())
                .ccdCaseId(123L).venueName("Bromley")
                .build();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(appeal)).willReturn(mappedJson);

        JSONObject actualRoboticsJson = service.createRobotics(appeal);

        then(roboticsJsonMapper).should(times(1)).map(appeal);
        then(roboticsJsonValidator).should(times(1)).validate(mappedJson);

        assertEquals(mappedJson, actualRoboticsJson);
    }

}
