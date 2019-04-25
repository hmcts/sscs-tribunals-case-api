package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionDraft {
    @JsonProperty(value = "BenefitType")
    SessionBenefitType benefitType;

    @JsonProperty(value = "PostcodeChecker")
    SessionPostcodeChecker postcode;

    @JsonProperty(value = "CreateAccount")
    SessionCreateAccount createAccount;

    @JsonProperty(value = "HaveAMRN")
    SessionHaveAMrn haveAMrn;

    @JsonProperty(value = "MRNDate")
    SessionMrnDate mrnDate;

    @JsonProperty(value = "CheckMRN")
    SessionCheckMrn checkMrn;

    @JsonProperty(value = "MRNOverThirteenMonthsLate")
    SessionMrnOverThirteenMonthsLate mrnOverThirteenMonthsLate;
}