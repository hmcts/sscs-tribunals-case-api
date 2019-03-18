package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SyaCaseWrapper {

    @JsonIgnore
    private String caseType;

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

    @JsonProperty("isAppointee")
    private Boolean isAppointee;

    @JsonProperty("appellant")
    private SyaAppellant appellant;

    @JsonProperty("appointee")
    private SyaAppointee appointee;

    @JsonProperty("signAndSubmit")
    private SyaSignAndSubmit signAndSubmit;

    public SyaContactDetails getContactDetails() {
        if (null == appellant) {
            if (null == appointee) {
                return null;
            }
        }
        return ((null != getAppellant().getIsAddressSameAsAppointee()) && getAppellant().getIsAddressSameAsAppointee())
                ? getAppointee().getContactDetails()
                : getAppellant().getContactDetails();
    }

}
