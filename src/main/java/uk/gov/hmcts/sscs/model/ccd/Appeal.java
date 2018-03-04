package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Appeal {
    private Appellant appellant;
    private BenefitType benefitType;
    private HearingOptions hearingOptions;

    public Appellant getAppellant() {
        return appellant;
    }

    public void setAppellant(Appellant appellant) {
        this.appellant = appellant;
    }

    public BenefitType getBenefitType() {
        return benefitType;
    }

    public void setBenefitType(BenefitType benefitType) {
        this.benefitType = benefitType;
    }

    public HearingOptions getHearingOptions() {
        return hearingOptions;
    }

    public void setHearingOptions(HearingOptions hearingOptions) {
        this.hearingOptions = hearingOptions;
    }

    @Override
    public String toString() {
        return "Appeal{" +
                "appellant=" + appellant +
                ", benefitType=" + benefitType +
                ", hearingOptions=" + hearingOptions +
                '}';
    }
}
