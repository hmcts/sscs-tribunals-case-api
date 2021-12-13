package uk.gov.hmcts.reform.sscs.util;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReissueFurtherEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

import java.util.ArrayList;
import java.util.Optional;

public class ReissueUtils {

    public static void validateSelectedPartyOptions(SscsCaseData sscsCaseData, ArrayList<String> errors) {
        boolean caseHasARepresentative = StringUtils.equalsIgnoreCase("YES", Optional.ofNullable(sscsCaseData.getAppeal().getRep()).map(Representative::getHasRepresentative).orElse("No"));

        if (!isAnyPartySelectedToResend(sscsCaseData, caseHasARepresentative)) {
            errors.add("Select a party to reissue.");
        }
        if (!caseHasARepresentative && YesNo.YES.equals(sscsCaseData.getReissueFurtherEvidence().getResendToRepresentative())) {
            errors.add("Cannot re-issue to the representative as there is no representative on the appeal.");
        }
    }

    private static boolean isAnyPartySelectedToResend(SscsCaseData sscsCaseData, boolean caseHasARepresentative) {
        ReissueFurtherEvidence reissueFurtherEvidence = sscsCaseData.getReissueFurtherEvidence();
        return YesNo.YES.equals(reissueFurtherEvidence.getResendToAppellant()) || (YesNo.YES.equals(reissueFurtherEvidence.getResendToRepresentative()) && caseHasARepresentative);
    }
}
