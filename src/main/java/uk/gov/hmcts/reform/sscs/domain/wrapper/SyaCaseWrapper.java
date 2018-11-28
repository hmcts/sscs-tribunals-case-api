package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyaCaseWrapper {

    @JsonProperty("hearing")
    private SyaHearingOptions syaHearingOptions;

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

    @JsonProperty("appointee")
    private SyaAppointee appointee;

    private Boolean isAddressSameAsAppointee;

    @JsonProperty("signAndSubmit")
    private SyaSignAndSubmit signAndSubmit;


    public SyaCaseWrapper() {
        // For Json
    }

    public SyaHearingOptions getSyaHearingOptions() {
        return syaHearingOptions;
    }

    public void setSyaHearingOptions(SyaHearingOptions syaHearingOptions) {
        this.syaHearingOptions = syaHearingOptions;
    }

    public SyaReasonsForAppealing getReasonsForAppealing() {
        return reasonsForAppealing;
    }

    public void setReasonsForAppealing(SyaReasonsForAppealing syaReasonsForAppealing) {
        this.reasonsForAppealing = syaReasonsForAppealing;
    }

    public Boolean hasRepresentative() {
        return hasRepresentative;
    }

    public void setHasRepresentative(Boolean hasRepresentative) {
        this.hasRepresentative = hasRepresentative;
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

    public SyaAppointee getAppointee() {
        return appointee;
    }

    public void setAppointee(SyaAppointee syaAppointee) {
        this.appointee = syaAppointee;
    }

    public Boolean getIsAddressSameAsAppointee() {
        return isAddressSameAsAppointee;
    }

    public void setIsAddressSameAsAppointee(Boolean isAddressSameAsAppointee) {
        this.isAddressSameAsAppointee = isAddressSameAsAppointee;
    }

    public SyaSignAndSubmit getSignAndSubmit() {
        return signAndSubmit;
    }

    public void setSignAndSubmit(SyaSignAndSubmit signAndSubmit) {
        this.signAndSubmit = signAndSubmit;
    }

    public SyaContactDetails getContactDetails() {
        return ((null != getIsAddressSameAsAppointee()) && getIsAddressSameAsAppointee()) ?
            getAppointee().getContactDetails() :
            getAppellant().getContactDetails();
    }

    @Override
    public String toString() {
        return "SyaCaseWrapper{"
                +  "syaHearingOptions=" + syaHearingOptions
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
