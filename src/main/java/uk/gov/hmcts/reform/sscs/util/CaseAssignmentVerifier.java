package uk.gov.hmcts.reform.sscs.util;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.RegExUtils.replaceAll;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;

import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;

@Component
public class CaseAssignmentVerifier {

    public boolean verifyPostcodeOrIbcaReference(SscsCaseDetails sscsCaseDetails,
                                                 String postcode, String ibcaReference, String email) {
        if (isNotBlank(postcode)) {
            return verifyAppellantOrOtherPartyPostcode(sscsCaseDetails, postcode, email);
        } else {
            return verifyAppellantOrOtherPartyIbcaReference(sscsCaseDetails, ibcaReference, email);
        }
    }

    private boolean verifyAppellantOrOtherPartyPostcode(SscsCaseDetails sscsCaseDetails,
                                                        String postcode, String email) {
        final List<String> otherPartyPostcodes = emptyIfNull(sscsCaseDetails.getData().getOtherParties()).stream()
                .map(CcdValue::getValue)
                .filter(otherParty -> hasOtherPartyGotEmailSubscription(otherParty, email))
                .map(otherParty -> normalise(otherParty.getAddress().getPostcode()))
                .toList();

        String appellantPostcode = sscsCaseDetails.getData().getAppeal().getAppellant().getAddress().getPostcode();

        return normalise(appellantPostcode).equals(normalise(postcode))
                || otherPartyPostcodes.contains(normalise(postcode));
    }

    private boolean verifyAppellantOrOtherPartyIbcaReference(SscsCaseDetails sscsCaseDetails,
                                                             String ibcaReference, String email) {
        if (isBlank(ibcaReference)) {
            return false;
        }
        final List<String> otherPartyIbcaReferenceList = emptyIfNull(sscsCaseDetails.getData().getOtherParties())
                .stream()
                .map(CcdValue::getValue)
                .filter(otherParty -> hasOtherPartyGotEmailSubscription(otherParty, email))
                .map(otherParty -> otherParty.getIdentity().getIbcaReference().toLowerCase())
                .toList();

        String appellantIbcaReference =
                sscsCaseDetails.getData().getAppeal().getAppellant().getIdentity().getIbcaReference();

        return equalsIgnoreCase(appellantIbcaReference, ibcaReference)
                || otherPartyIbcaReferenceList.contains(ibcaReference.toLowerCase());
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
