package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityAnswer {

    private String activityAnswerNumber;
    private String activityAnswerLetter;
    private String activityAnswerValue;
    private int activityAnswerPoints;
}
