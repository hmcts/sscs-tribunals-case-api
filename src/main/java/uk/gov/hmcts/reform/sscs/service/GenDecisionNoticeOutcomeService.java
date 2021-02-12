package uk.gov.hmcts.reform.sscs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Slf4j
@Service
public class GenDecisionNoticeOutcomeService extends DecisionNoticeOutcomeService {

    public GenDecisionNoticeOutcomeService(GenDecisionNoticeQuestionService questionService) {
        super("GEN", questionService);
    }

    public Outcome determineOutcome(SscsCaseData sscsCaseData) {
        return useExplicitySetOutcome(sscsCaseData);
    }

    @Override
    public void performPreOutcomeIntegrityAdjustments(SscsCaseData sscsCaseData) {
        // N/A for GEN
    }

    @Override
    public Outcome determineOutcomeWithValidation(SscsCaseData sscsCaseData) {
        return determineOutcome(sscsCaseData);
    }
}
