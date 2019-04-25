package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;
import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;

@Value
public class SessionCheckMrn {
    private String checkedMrn;

    public SessionCheckMrn(MrnDetails mrnDetails) {
        this.checkedMrn = mrnDetails.getMrnDate() == null || StringUtils.isEmpty(mrnDetails.getMrnDate()) ? "no" : "yes";
    }
}
