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
public class EsaDecisionNoticeOutcomeService extends DecisionNoticeOutcomeService {

    public EsaDecisionNoticeOutcomeService(String benefitType) {
        super(benefitType);
    }
    
    public Outcome determineOutcome(SscsCaseData sscsCaseData) {
        return useExplicitySetOutcome(sscsCaseData);
    }
}
