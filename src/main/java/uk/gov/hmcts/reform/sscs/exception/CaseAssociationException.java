package uk.gov.hmcts.reform.sscs.exception;

import java.io.Serial;
import lombok.Getter;

@Getter
public class CaseAssociationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final CaseAssociationError error;

    public CaseAssociationException(CaseAssociationError error) {
        super(error.getMessage());
        this.error = error;
    }
}
