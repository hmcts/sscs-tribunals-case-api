package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SessionDraft {
    @JsonProperty("BenefitType")
    SessionBenefitType benefitType;

    @JsonProperty("PostcodeChecker")
    SessionPostcodeChecker postcode;

    @JsonProperty("CreateAccount")
    SessionCreateAccount createAccount;

    @JsonProperty("HaveAMRN")
    SessionHaveAMrn haveAMrn;

    @JsonProperty("MRNDate")
    SessionMrnDate mrnDate;

    @JsonProperty("CheckMRN")
    SessionCheckMrn checkMrn;

    @JsonProperty("MRNOverThirteenMonthsLate")
    SessionMrnOverThirteenMonthsLate mrnOverThirteenMonthsLate;
}