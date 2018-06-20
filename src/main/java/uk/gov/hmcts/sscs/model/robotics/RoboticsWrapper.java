package uk.gov.hmcts.sscs.model.robotics;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;

@Value
@Builder
public class RoboticsWrapper {

    private SyaCaseWrapper syaCaseWrapper;

    private long ccdCaseId;
}
