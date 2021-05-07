package uk.gov.hmcts.reform.sscs.util;

import static uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome.IN_PROGRESS;

import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


public final class ConfidentialityRequestUtil {

    private ConfidentialityRequestUtil() {
        //
    }

    public static boolean isAtLeastOneRequestInProgress(SscsCaseData sscsCaseData) {
        return isAppellantRequestInProgress(sscsCaseData)
                || isJointPartyRequestInProgress(sscsCaseData);
    }

    private static boolean isAppellantRequestInProgress(SscsCaseData sscsCaseData) {
        return IN_PROGRESS
                .equals(getRequestOutcome(sscsCaseData.getConfidentialityRequestOutcomeAppellant()));
    }

    private static boolean isJointPartyRequestInProgress(SscsCaseData sscsCaseData) {
        return IN_PROGRESS
                .equals(getRequestOutcome(sscsCaseData.getConfidentialityRequestOutcomeJointParty()));
    }

    private static RequestOutcome getRequestOutcome(DatedRequestOutcome datedRequestOutcome) {
        return datedRequestOutcome == null ? null : datedRequestOutcome.getRequestOutcome();
    }
}
