package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;

@Value
public class SessionMrnDateDetails {
    private String day;
    private String month;
    private String year;

    public SessionMrnDateDetails(MrnDetails mrnDetails) {
        if (mrnDetails.getMrnDate() != null && !StringUtils.isEmpty(mrnDetails.getMrnDate())) {
            day = mrnDetails.getMrnDate().substring(0,2);
            month = mrnDetails.getMrnDate().substring(3,5);
            year = mrnDetails.getMrnDate().substring(6,10);
        } else {
            day = "";
            month = "";
            year = "";
        }
    }
}
