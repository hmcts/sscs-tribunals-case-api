package uk.gov.hmcts.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class  SyaCaseWrapper {

    @JsonProperty("hearing")
    private SyaHearing hearing;

    @JsonProperty("reasonsForAppealing")
    private SyaReasonsForAppealing reasonsForAppealing;

    @JsonProperty("hasRepresentative")
    private Boolean hasRepresentative;

    @JsonProperty("representative")
    private SyaRepresentative representative;

    @JsonProperty("benefitType")
    private SyaBenefitType benefitType;

    @JsonProperty("smsNotify")
    private SyaSmsNotify smsNotify;

    @JsonProperty("mrn")
    private SyaMrn mrn;

    private Boolean isAppointee;

    @JsonProperty("appellant")
    private SyaAppellant appellant;

    @JsonProperty("signAndSubmit")
    private SyaSignAndSubmit signAndSubmit;


    public SyaCaseWrapper() {
        // For Json
    }

    public SyaHearing getHearing() {
        return hearing;
    }

    public void setHearing(SyaHearing syaHearing) {
        this.hearing = syaHearing;
    }

    public SyaReasonsForAppealing getReasonsForAppealing() {
        return reasonsForAppealing;
    }

    public void setReasonsForAppealing(SyaReasonsForAppealing syaReasonsForAppealing) {
        this.reasonsForAppealing = syaReasonsForAppealing;
    }

    public SyaRepresentative getRepresentative() {
        return representative;
    }

    public void setRepresentative(SyaRepresentative syaRepresentative) {
        this.representative = syaRepresentative;
    }

    public SyaBenefitType getBenefitType() {
        return benefitType;
    }

    public void setBenefitType(SyaBenefitType benefitType) {
        this.benefitType = benefitType;
    }

    public SyaSmsNotify getSmsNotify() {
        return smsNotify;
    }

    public void setSmsNotify(SyaSmsNotify syaSmsNotify) {
        this.smsNotify = syaSmsNotify;
    }

    public SyaMrn getMrn() {
        return mrn;
    }

    public void setMrn(SyaMrn syaMrn) {
        this.mrn = syaMrn;
    }

    public Boolean getIsAppointee() {
        return isAppointee;
    }

    public void setIsAppointee(Boolean isAppointee) {
        this.isAppointee = isAppointee;
    }

    public SyaAppellant getAppellant() {
        return appellant;
    }

    public void setAppellant(SyaAppellant syaAppellant) {
        this.appellant = syaAppellant;
    }

    @Override
    public String toString() {
        return "SyaCaseWrapper{"
                +  "hearing=" + hearing
                + ", reasonsForAppealing=" + reasonsForAppealing
                + ", representative=" + representative
                + ", benefitType='" + benefitType + '\''
                + ", smsNotify=" + smsNotify
                + ", mrn=" + mrn
                + ", isAppointee=" + isAppointee
                + ", appellant=" + appellant
                + ", signAndSubmit=" + signAndSubmit
                + '}';
    }
}
