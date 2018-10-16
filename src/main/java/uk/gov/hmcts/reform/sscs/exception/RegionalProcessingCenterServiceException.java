package uk.gov.hmcts.reform.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class RegionalProcessingCenterServiceException extends UnknownErrorCodeException {
    public RegionalProcessingCenterServiceException(Throwable cause) {
        super(AlertLevel.P4, cause);
    }
}