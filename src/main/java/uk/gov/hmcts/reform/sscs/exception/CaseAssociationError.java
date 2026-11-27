package uk.gov.hmcts.reform.sscs.exception;

import lombok.Getter;

@Getter
public enum CaseAssociationError {
    CASE_NOT_FOUND("No case found for the provided appeal reference number."),
    POSTCODE_OR_REFERENCE_NOT_CONFIGURED("Appeal does not contain valid reference or address details to verify."),
    POSTCODE_MISMATCH("Postcode or reference does not match the appeal records."),
    SUBSCRIPTION_MISMATCH("The email address or appeal details do not match the registered subscription.");

    private final String message;

    CaseAssociationError(String message) {
        this.message = message;
    }
}
