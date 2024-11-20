package uk.gov.hmcts.reform.sscs.exception;

import java.io.Serial;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;

public class UnhandleableHearingStateException extends Exception {
    @Serial
    private static final long serialVersionUID = 4010841641319292161L;

    public UnhandleableHearingStateException() {
        super("Unable to handle Hearing State: %s".formatted("null"));
    }

    public UnhandleableHearingStateException(HearingState hearingState) {
        super("Unable to handle Hearing State: %s".formatted(hearingState.getState()));
    }
}
