package uk.gov.hmcts.reform.sscs.exception;

import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;

public class UnhandleableHearingStateException extends Exception {
    private static final long serialVersionUID = 4010841641319292161L;

    public UnhandleableHearingStateException() {
        super(String.format("Unable to handle Hearing State: %s", "null"));
    }

    public UnhandleableHearingStateException(HearingState hearingState) {
        super(String.format("Unable to handle Hearing State: %s", hearingState.getState()));
    }
}
