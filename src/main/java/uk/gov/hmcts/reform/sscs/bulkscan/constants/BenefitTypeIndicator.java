package uk.gov.hmcts.reform.sscs.bulkscan.constants;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import lombok.Getter;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;

@Getter
public enum BenefitTypeIndicator {
    PIP("is_benefit_type_pip", Benefit.PIP),
    ESA("is_benefit_type_esa", Benefit.ESA),
    UC("is_benefit_type_uc", Benefit.UC);

    private final String indicatorString;
    private final Benefit benefit;

    BenefitTypeIndicator(String indicatorString, Benefit benefit) {
        this.indicatorString = indicatorString;
        this.benefit = benefit;
    }

    public static List<String> getAllIndicatorStrings() {
        return stream(values())
            .map(BenefitTypeIndicator::getIndicatorString)
            .collect(toList());
    }

    public static Optional<Benefit> findByIndicatorString(String indicatorString) {
        return stream(values())
            .filter(v -> v.getIndicatorString().equals(indicatorString))
            .map(BenefitTypeIndicator::getBenefit)
            .findFirst();
    }

}
