package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_IN_FAVOUR_OF_APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_UPHELD;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.ComparedRate;

@Slf4j
@Service
public class PipDecisionNoticeOutcomeService extends DecisionNoticeOutcomeService {

    public PipDecisionNoticeOutcomeService() {
        super("PIP");
    }

    public Outcome determineOutcome(SscsCaseData sscsCaseData) {

        if (sscsCaseData.getWriteFinalDecisionIsDescriptorFlow() == null) {
            // We need at least this flag to be set in order to determine outcome
            return null;
        } else {
            if (sscsCaseData.isDailyLivingAndOrMobilityDecision()) {
                if ("yes".equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionGenerateNotice())) {
                    // If we are generating the notice we use the daily living/mobility descriptors
                    // to determine outcome
                    return determineGenerateNoticeDailyLivingOrMobilityFlowOutcome(sscsCaseData);
                } else {
                    // If we are not generating the notice we use an explicitly set outcome
                    return useExplicitySetOutcome(sscsCaseData);
                }
            } else {
                // If we are in the non-descriptor flow we use an explicitly set outcome.
                return useExplicitySetOutcome(sscsCaseData);
            }
        }
    }

    private Outcome determineGenerateNoticeDailyLivingOrMobilityFlowOutcome(SscsCaseData sscsCaseData) {

        // Daily living and or/mobility

        if ((!AwardType.NOT_CONSIDERED.getKey().equalsIgnoreCase(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion())
            && sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion() == null)
            || (!AwardType.NOT_CONSIDERED.getKey().equalsIgnoreCase(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion())
            && sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion() == null)) {
            return null;
        } else {

            try {

                ComparedRate dailyLivingComparedRate = AwardType.NOT_CONSIDERED.getKey()
                    .equalsIgnoreCase(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()) ? null :
                    ComparedRate.getByKey(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());

                ComparedRate mobilityComparedRate = AwardType.NOT_CONSIDERED.getKey()
                    .equalsIgnoreCase(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()) ? null : ComparedRate.getByKey(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());

                Set<ComparedRate> comparedRates = new HashSet<>();
                if (dailyLivingComparedRate != null) {
                    comparedRates.add(dailyLivingComparedRate);
                }
                if (mobilityComparedRate != null) {
                    comparedRates.add(mobilityComparedRate);
                }

                // At least one higher,  and non lower, means the decision is in favour of appellant
                if (comparedRates.contains(ComparedRate.Higher)) {
                    return DECISION_IN_FAVOUR_OF_APPELLANT;
                } else {
                    // Otherwise, decision upheld
                    return DECISION_UPHELD;
                }

            } catch (IllegalArgumentException e) {
                log.error(e.getMessage());
                return null;
            }
        }
    }
}
