package uk.gov.hmcts.sscs.json;

import static uk.gov.hmcts.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS;
import static uk.gov.hmcts.sscs.util.SyaJsonMessageSerializer.WITHOUT_REPRESENTATIVE;

import java.io.InputStream;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.model.robotics.RoboticsWrapper;

public class RoboticsJsonGeneratorTest {

    InputStream inputStream = getClass().getResourceAsStream("/schema/sscs-robotics.json");

    Schema schema;

    @Before
    public void setup() {
        schema =  SchemaLoader.load(new JSONObject(new JSONTokener(inputStream)));
    }

    @Test
    public void givenSyaData_makeValidRoboticsJsonThatValidatesAgainstSchema() {

        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        RoboticsWrapper roboticsWrapper = RoboticsWrapper.builder().syaCaseWrapper(syaCaseWrapper).ccdCaseId(1234L).build();

        JSONObject result = RoboticsJsonGenerator.create(roboticsWrapper);

        schema.validate(result);
    }

    @Test
    public void givenSyaDataWithoutRepresentative_makeValidRoboticsJsonThatValidatesAgainstSchema() {

        SyaCaseWrapper syaCaseWrapper = WITHOUT_REPRESENTATIVE.getDeserializeMessage();
        RoboticsWrapper roboticsWrapper = RoboticsWrapper.builder().syaCaseWrapper(syaCaseWrapper).ccdCaseId(1234L).build();

        JSONObject result = RoboticsJsonGenerator.create(roboticsWrapper);

        schema.validate(result);
    }

}