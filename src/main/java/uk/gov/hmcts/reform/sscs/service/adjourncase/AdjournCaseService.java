package uk.gov.hmcts.reform.sscs.service.adjourncase;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@Slf4j
public class AdjournCaseService {

    private AdjournCaseService() {
        // remove default constructor
    }

    public static void clearTransientFields(SscsCaseData sscsCaseData, boolean isAdjournmentEnabled) {
        log.info("Clearing transient adjournment case fields for caseId {}", sscsCaseData.getCcdCaseId());

        sscsCaseData.setAdjournment(Adjournment.builder().build());

        if (isAdjournmentEnabled) {
            sscsCaseData.getAdjournment().setAdjournmentInProgress(YesNo.NO);
        }
    }
}
