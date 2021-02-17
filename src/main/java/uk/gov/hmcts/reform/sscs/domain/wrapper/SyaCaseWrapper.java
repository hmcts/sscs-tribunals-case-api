package uk.gov.hmcts.reform.sscs.domain.wrapper;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

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

    @JsonProperty("pcqId")
    private String pcqId;

    private String evidenceProvide;

    @JsonProperty("isSaveAndReturn")
    private String isSaveAndReturn;

    @JsonProperty("languagePreferenceWelsh")
    private Boolean languagePreferenceWelsh;

    @JsonProperty("ccdCaseId")
    private String ccdCaseId;

    public SyaContactDetails getContactDetails() {
        if (null == appellant && null == appointee) {
            return null;
        }
        return null != appellant && null != appointee && ofNullable(isAppointee).orElse(false)
                ? appointee.getContactDetails()
                : appellant != null ? appellant.getContactDetails() : null;
    }

}
