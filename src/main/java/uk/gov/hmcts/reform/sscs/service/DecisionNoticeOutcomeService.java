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
