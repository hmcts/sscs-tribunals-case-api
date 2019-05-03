package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
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

    @JsonProperty("DWPIssuingOffice")
    SessionDwpIssuingOffice dwpIssuingOffice;

    @JsonProperty("DWPIssuingOfficeEsa")
    SessionDwpIssuingOfficeEsa dwpIssuingOfficeEsa;

    @JsonProperty("Appointee")
    SessionAppointee appointee;

    @JsonProperty("AppellantName")
    SessionAppellantName appellantName;

    @JsonProperty("AppellantDOB")
    SessionAppellantDob appellantDob;

    @JsonProperty("AppellantNINO")
    SessionAppellantNino appellantNino;

    @JsonProperty("AppellantContactDetails")
    SessionAppellantContactDetails appellantContactDetails;

    @JsonProperty("TextReminders")
    SessionTextReminders textReminders;

    @JsonProperty("SendToNumber")
    SessionSendToNumber sendToNumber;

    @JsonProperty("SmsConfirmation")
    SessionSmsConfirmation smsConfirmation;

    @JsonProperty("Representative")
    SessionRepresentative representative;

    @JsonProperty("RepresentativeDetails")
    SessionRepresentativeDetails representativeDetails;

    @JsonProperty("ReasonForAppealing")
    SessionReasonForAppealing reasonForAppealing;

    @JsonProperty("OtherReasonForAppealing")
    SessionOtherReasonForAppealing otherReasonForAppealing;

    @JsonProperty("EvidenceProvide")
    SessionEvidenceProvide evidenceProvide;

    @JsonProperty("TheHearing")
    SessionTheHearing theHearing;

    @JsonProperty("HearingSupport")
    SessionHearingSupport hearingSupport;

    @JsonProperty("HearingArrangements")
    SessionHearingArrangements hearingArrangements;

    @JsonProperty("NotAttendingHearing")
    SessionNotAttendingHearing notAttendingHearing;
}