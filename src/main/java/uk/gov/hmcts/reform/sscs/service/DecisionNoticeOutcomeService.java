package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_IN_FAVOUR_OF_APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_UPHELD;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Slf4j
public abstract class DecisionNoticeOutcomeService {

    private String benefitType;

    protected DecisionNoticeOutcomeService(String benefitType) {
        this.benefitType = benefitType;
    }

    public String getBenefitType() {
        return benefitType;
    }

    public abstract Outcome determineOutcome(SscsCaseData sscsCaseData);

    /**
     * Due to a bug with CCD related to hidden fields, hidden fields are not being unset
     * on the final submission from CCD, so we need to reset them here
     * See https://tools.hmcts.net/jira/browse/RDM-8200
     * This method provides a hook to temporarily workaround this issue, and allow
     * hidden fields to be unset.
     *
     */
    public abstract void performPreOutcomeIntegrityAdjustments(SscsCaseData sscsCaseData);

    public abstract Outcome determineOutcomeWithValidation(SscsCaseData sscsCaseData);

    protected Outcome useExplicitySetOutcome(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getWriteFinalDecisionAllowedOrRefused() == null) {
            return null;
        } else {
            if ("allowed".equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionAllowedOrRefused())) {
                return DECISION_IN_FAVOUR_OF_APPELLANT;
            } else {
                return DECISION_UPHELD;
            }
        }
    }

}
