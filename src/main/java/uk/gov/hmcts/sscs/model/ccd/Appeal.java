package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class Appeal {
    private String mrnDate;
    private String mrnLateReason;
    private String mrnMissingReason;
    private Appellant appellant;
    private BenefitType benefitType;
    private HearingOptions hearingOptions;
    private AppealReasons appealReasons;
    private Representative rep;

    public Appeal(@JsonProperty("mrnDate") String mrnDate,
                  @JsonProperty("mrnLateReason") String mrnLateReason,
                  @JsonProperty("mrnMissingReason") String mrnMissingReason,
                  @JsonProperty("appellant") Appellant appellant,
                  @JsonProperty("benefitType") BenefitType benefitType,
                  @JsonProperty("hearingOptions") HearingOptions hearingOptions,
                  @JsonProperty("appealReasons") AppealReasons appealReasons,
                  @JsonProperty("rep") Representative rep) {
        this.mrnDate = mrnDate;
        this.mrnLateReason = mrnLateReason;
        this.mrnMissingReason = mrnMissingReason;
        this.appellant = appellant;
        this.benefitType = benefitType;
        this.hearingOptions = hearingOptions;
        this.appealReasons = appealReasons;
        this.rep = rep;
    }
}
