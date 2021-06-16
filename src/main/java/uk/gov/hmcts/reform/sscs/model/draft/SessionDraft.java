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

    @JsonProperty("HaveContactedDWP")
    SessionHaveContactedDwp haveContactedDwp;

    @JsonProperty("NoMRN")
    SessionNoMrn noMrn;

    @JsonProperty("CheckMRN")
    SessionCheckMrn checkMrn;

    @JsonProperty("MRNOverThirteenMonthsLate")
    SessionMrnOverThirteenMonthsLate mrnOverThirteenMonthsLate;

    @JsonProperty("MRNOverOneMonthLate")
    SessionMrnOverOneMonthLate mrnOverOneMonthLate;

    @JsonProperty("DWPIssuingOffice")
    SessionDwpIssuingOffice dwpIssuingOffice;

    @JsonProperty("DWPIssuingOfficeEsa")
    SessionDwpIssuingOfficeEsa dwpIssuingOfficeEsa;

    @JsonProperty("AppointeeName")
    SessionName appointeeName;

    @JsonProperty("AppointeeDOB")
    SessionDob appointeeDob;

    @JsonProperty("AppointeeContactDetails")
    SessionContactDetails appointeeContactDetails;

    @JsonProperty("Appointee")
    SessionAppointee appointee;

    @JsonProperty("AppellantName")
    SessionName appellantName;

    @JsonProperty("AppellantDOB")
    SessionDob appellantDob;

    @JsonProperty("AppellantNINO")
    SessionAppellantNino appellantNino;

    @JsonProperty("AppellantContactDetails")
    SessionContactDetails appellantContactDetails;

    @JsonProperty("SameAddress")
    SessionSameAddress sameAddress;

    @JsonProperty("TextReminders")
    SessionTextReminders textReminders;

    @JsonProperty("SendToNumber")
    SessionSendToNumber sendToNumber;

    @JsonProperty("EnterMobile")
    SessionEnterMobile enterMobile;

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

    @JsonProperty("EvidenceUpload")
    SessionEvidenceUpload evidenceUpload;

    @JsonProperty("EvidenceDescription")
    SessionEvidenceDescription evidenceDescription;

    @JsonProperty("TheHearing")
    SessionTheHearing theHearing;

    @JsonProperty("HearingOptions")
    SessionHearingOptions hearingOptions;

    @JsonProperty("HearingSupport")
    SessionHearingSupport hearingSupport;

    @JsonProperty("HearingAvailability")
    SessionHearingAvailability hearingAvailability;

    @JsonProperty("DatesCantAttend")
    SessionDatesCantAttend datesCantAttend;

    @JsonProperty("HearingArrangements")
    SessionHearingArrangements hearingArrangements;

    @JsonProperty("Pcq")
    SessionPcqId pcqId;

    @JsonProperty("LanguagePreference")
    SessionLanguagePreferenceWelsh languagePreferenceWelsh;

    @JsonProperty("ccdCaseId")
    String ccdCaseId;
}
