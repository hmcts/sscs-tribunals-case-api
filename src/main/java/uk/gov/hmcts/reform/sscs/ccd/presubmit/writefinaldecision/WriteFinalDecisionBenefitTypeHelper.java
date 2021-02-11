package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public abstract class WriteFinalDecisionBenefitTypeHelper {

    public static String getBenefitType(SscsCaseData sscsCaseData) {
        String benefitType = sscsCaseData.getAppeal() == null ? null :
            sscsCaseData.getAppeal().getBenefitType() == null ? null :
                sscsCaseData.getAppeal().getBenefitType().getCode();

        if (benefitType != null && !benefitType.equals("PIP") && !benefitType.equals("ESA") && !benefitType.equals("UC")) {
            return "GEN";
        }

        return benefitType;
    }
}
