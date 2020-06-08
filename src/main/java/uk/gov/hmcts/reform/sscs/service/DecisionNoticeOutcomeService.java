package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_IN_FAVOUR_OF_APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_UPHELD;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.wrapper.ComparedRate;

@Slf4j
@Service
public class DecisionNoticeOutcomeService {

    public Outcome determineOutcome(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion() == null
            || sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion() == null) {
            return null;
        } else {

            ComparedRate dailyLivingComparedRate = ComparedRate.getByKey(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
            ComparedRate mobilityComparedRate = ComparedRate.getByKey(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());

            Set<ComparedRate> comparedRates = new HashSet<>();
            comparedRates.add(dailyLivingComparedRate);
            comparedRates.add(mobilityComparedRate);

            // At least one higher,  and non lower, means the decision is in favour of appellant
            if (comparedRates.contains(ComparedRate.Higher)
                && !comparedRates.contains(ComparedRate.Lower)) {
                return DECISION_IN_FAVOUR_OF_APPELLANT;
            } else {
                // Otherwise, decision upheld
                return DECISION_UPHELD;
            }
        }

    }
}
