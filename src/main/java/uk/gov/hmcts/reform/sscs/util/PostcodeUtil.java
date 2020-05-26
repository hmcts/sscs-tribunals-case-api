package uk.gov.hmcts.reform.sscs.util;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

@Component
public class PostcodeUtil {
    public boolean hasAppellantPostcode(SscsCaseDetails sscsCaseDetails, String postcode) {
        String normalisedPostcode = normalise((postcode));
        String appealPostcode = sscsCaseDetails.getData().getAppeal().getAppellant().getAddress().getPostcode();
        String normalisedAppealPostcode = normalise(appealPostcode);

        return normalisedPostcode.equals(normalisedAppealPostcode);
    }

    private String normalise(String originalPostcode) {
        return originalPostcode.replaceAll(" ", "").toLowerCase();
    }
}
