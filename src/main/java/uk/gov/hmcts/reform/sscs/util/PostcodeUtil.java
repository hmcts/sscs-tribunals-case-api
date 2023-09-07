package uk.gov.hmcts.reform.sscs.util;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.RegExUtils.replaceAll;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.lowerCase;

import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;

@Component
public class PostcodeUtil {
    public boolean hasAppellantOrOtherPartyPostcode(SscsCaseDetails sscsCaseDetails, String postcode, String email) {
        String normalisedPostcode = normalise((postcode));
        final List<String> otherPartyPostcodes = emptyIfNull(sscsCaseDetails.getData().getOtherParties()).stream()
                .map(CcdValue::getValue)
                .filter(op -> hasOtherPartyGotEmailSubscription(op, email))
                .map(op -> normalise(op.getAddress().getPostcode()))
                .toList();

        String appealPostcode = sscsCaseDetails.getData().getAppeal().getAppellant().getAddress().getPostcode();
        String normalisedAppealPostcode = normalise(appealPostcode);

        return normalisedPostcode.equals(normalisedAppealPostcode) || otherPartyPostcodes.contains(normalisedPostcode);
    }

    private boolean hasOtherPartyGotEmailSubscription(OtherParty otherParty, String email) {
        return hasEmailSubscription(otherParty.getOtherPartySubscription(), email)
                || hasEmailSubscription(otherParty.getOtherPartyAppointeeSubscription(), email)
                || hasEmailSubscription(otherParty.getOtherPartyRepresentativeSubscription(), email);
    }

    private boolean hasEmailSubscription(Subscription subscription, String email) {
        return subscription != null && equalsIgnoreCase(subscription.getEmail(), email);
    }

    private String normalise(String originalPostcode) {
        return lowerCase(replaceAll(originalPostcode," ", EMPTY));
    }
}
