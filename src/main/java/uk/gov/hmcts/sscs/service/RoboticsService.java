package uk.gov.hmcts.sscs.service;

import java.io.InputStream;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.json.RoboticsJsonGenerator;
import uk.gov.hmcts.sscs.model.robotics.RoboticsWrapper;

@Service
public class RoboticsService {

    private final InputStream inputStream;

    private final Schema schema;

    public RoboticsService() {
        inputStream = getClass().getResourceAsStream("/schema/sscs-robotics.json");
        schema = SchemaLoader.load(new JSONObject(new JSONTokener(inputStream)));
    }

    public JSONObject generateRobotics(RoboticsWrapper appeal) {

        JSONObject roboticsJson = RoboticsJsonGenerator.create(appeal);

        schema.validate(roboticsJson);

        return roboticsJson;
    }
}
