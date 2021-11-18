package uk.gov.hmcts.reform.sscs.util;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.RegExUtils.replaceAll;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.lowerCase;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

@Component
public class PostcodeUtil {
    public boolean hasAppellantOrOtherPartyPostcode(SscsCaseDetails sscsCaseDetails, String postcode) {
        String normalisedPostcode = normalise((postcode));
        final List<String> otherPartyPostcodes = emptyIfNull(sscsCaseDetails.getData().getOtherParties()).stream()
                .map(CcdValue::getValue)
                .map(op -> normalise(op.getAddress().getPostcode()))
                .collect(Collectors.toList());

        String appealPostcode = sscsCaseDetails.getData().getAppeal().getAppellant().getAddress().getPostcode();
        String normalisedAppealPostcode = normalise(appealPostcode);

        return normalisedPostcode.equals(normalisedAppealPostcode) || otherPartyPostcodes.contains(normalisedPostcode);
    }

    private String normalise(String originalPostcode) {
        return lowerCase(replaceAll(originalPostcode," ", EMPTY));
    }
}
