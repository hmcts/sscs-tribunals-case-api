package uk.gov.hmcts.reform.sscs.tyanotifications.exception;

import static java.lang.String.format;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class NotificationClientRuntimeException extends RuntimeException {

    public NotificationClientRuntimeException(String caseId, Exception ex) {
        super(format("Exception thrown for case [%s]", caseId), ex);
    }

    public NotificationClientRuntimeException(String caseId) {
        super(format("Exception thrown for case [%s]", caseId));
    }
}
