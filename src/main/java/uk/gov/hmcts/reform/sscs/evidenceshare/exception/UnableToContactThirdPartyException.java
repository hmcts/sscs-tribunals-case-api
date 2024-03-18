package uk.gov.hmcts.reform.sscs.evidenceshare.exception;

import static java.lang.String.format;

public class UnableToContactThirdPartyException extends RuntimeException {
    public static final long serialVersionUID = 7696769908351871145L;

    public UnableToContactThirdPartyException(String thirdParty, Throwable exception) {
        super(format("Unable to contact %s, please try again by running the \"Send to FTA\".", thirdParty), exception);
    }

}
