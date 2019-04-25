package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;

@Value
public class SessionBenefitType {
    private String benefitType;

    public SessionBenefitType(BenefitType appealBenefitType) {
        this.benefitType = appealBenefitType.getDescription() + " (" + appealBenefitType.getCode() + ")";
    }
}
