package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import lombok.Data;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Data
public class HearingObject {

    StateOfHearing state;
    private String sscsCaseData;

    public HearingObject(){
        super();
    }

    public HearingObject(String sscsCaseData, StateOfHearing state) {
        this.sscsCaseData = sscsCaseData;
        this.state = state;
    }
}
