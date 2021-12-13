package uk.gov.hmcts.reform.sscs.util;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReissueFurtherEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

import java.util.ArrayList;
import java.util.Optional;

import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isOtherPartyPresent;

public class ReissueUtils {

    public static void validateSelectedPartyOptions(SscsCaseData sscsCaseData, ArrayList<String> errors, boolean checkOtherParty) {
        boolean caseHasARepresentative = StringUtils.equalsIgnoreCase("YES", Optional.ofNullable(sscsCaseData.getAppeal().getRep()).map(Representative::getHasRepresentative).orElse("No"));

        if (!isAnyPartySelectedToResend(sscsCaseData, caseHasARepresentative, checkOtherParty)) {
            errors.add("Select a party to reissue.");
        }
        if (!caseHasARepresentative && YesNo.isYes(sscsCaseData.getReissueFurtherEvidence().getResendToRepresentative())) {
            errors.add("Cannot re-issue to the representative as there is no representative on the appeal.");
        }
    }

    private static boolean isAnyPartySelectedToResend(SscsCaseData sscsCaseData, boolean caseHasARepresentative, boolean checkOtherParty) {
        ReissueFurtherEvidence reissueFurtherEvidence = sscsCaseData.getReissueFurtherEvidence();
        return YesNo.isYes(reissueFurtherEvidence.getResendToAppellant())
                || (YesNo.isYes(reissueFurtherEvidence.getResendToRepresentative()) && caseHasARepresentative)
                || (checkOtherParty && isOtherPartyPresent(sscsCaseData) && isAnyOtherPartySelected(sscsCaseData));
    }

    private static boolean isAnyOtherPartySelected(SscsCaseData sscsCaseData) {
        ReissueFurtherEvidence reissueFurtherEvidence = sscsCaseData.getReissueFurtherEvidence();
        return reissueFurtherEvidence.getOtherPartyOptions() != null
                && !reissueFurtherEvidence.getOtherPartyOptions().isEmpty()
                && reissueFurtherEvidence.getOtherPartyOptions().stream()
                .anyMatch(otherPartyOption -> otherPartyOption.getValue() != null
                        && YesNo.isYes(otherPartyOption.getValue().getResendToOtherParty()));
    }
}
