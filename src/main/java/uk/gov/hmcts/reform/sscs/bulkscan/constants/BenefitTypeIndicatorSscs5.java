package uk.gov.hmcts.reform.sscs.bulkscan.constants;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import lombok.Getter;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;

@Getter
public enum BenefitTypeIndicatorSscs5 {
    TAX_CREDIT("is_benefit_type_tax_credit", Benefit.TAX_CREDIT),
    GUARDIANS_ALLOWANCE("is_benefit_type_guardians_allowance", Benefit.GUARDIANS_ALLOWANCE),
    TAX_FREE_CHILDCARE("is_benefit_type_tax_free_childcare", Benefit.TAX_FREE_CHILDCARE),
    HOME_RESPONSIBILITIES_PROTECTION("is_benefit_type_home_responsibilities_protection", Benefit.HOME_RESPONSIBILITIES_PROTECTION),
    CHILD_BENEFIT("is_benefit_type_child_benefit", Benefit.CHILD_BENEFIT),
    THIRTY_HOURS_FREE_CHILDCARE("is_benefit_type_30_hours_tax_free_childcare", Benefit.THIRTY_HOURS_FREE_CHILDCARE),
    GUARANTEED_MINIMUM_PENSION("is_benefit_type_guaranteed_minimum_pension", Benefit.GUARANTEED_MINIMUM_PENSION),
    NATIONAL_INSURANCE_CREDITS("is_benefit_type_national_insurance_credits", Benefit.NATIONAL_INSURANCE_CREDITS);

    private final String indicatorString;
    private final Benefit benefit;

    BenefitTypeIndicatorSscs5(String indicatorString, Benefit benefit) {
        this.indicatorString = indicatorString;
        this.benefit = benefit;
    }

    public static List<String> getAllIndicatorStrings() {
        return stream(values())
            .map(BenefitTypeIndicatorSscs5::getIndicatorString)
            .collect(toList());
    }

    public static Optional<Benefit> findByIndicatorString(String indicatorString) {
        return stream(values())
            .filter(v -> v.getIndicatorString().equals(indicatorString))
            .map(BenefitTypeIndicatorSscs5::getBenefit)
            .findFirst();
    }

}
