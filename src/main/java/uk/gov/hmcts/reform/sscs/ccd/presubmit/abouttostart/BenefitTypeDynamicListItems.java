package uk.gov.hmcts.reform.sscs.ccd.presubmit.abouttostart;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;

@Getter
public enum BenefitTypeDynamicListItems {
    PIP("pip", "PIP"),
    ESA("esa", "ESA"),
    UC("uc", "UC");

    String code;
    String label;

    public static Optional<BenefitTypeDynamicListItems> findByBenefitType(BenefitType benefitType) {
        return Arrays.stream(BenefitTypeDynamicListItems.values())
            .filter(b -> b.getCode().equalsIgnoreCase(benefitType.getCode())).findFirst();
    }

    BenefitTypeDynamicListItems(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
