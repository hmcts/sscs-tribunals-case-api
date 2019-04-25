package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;
import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;

@Value
public class SessionHaveAMrn {
    public String haveAMrn;

    public SessionHaveAMrn(MrnDetails mrnDetails) {
        this.haveAMrn = mrnDetails.getMrnDate() == null || StringUtils.isEmpty(mrnDetails.getMrnDate()) ? "no" : "yes";
    }
}
