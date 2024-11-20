package uk.gov.hmcts.reform.sscs.tyanotifications.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class NotificationClientRuntimeException extends RuntimeException {

    public NotificationClientRuntimeException(String caseId, Exception ex) {
        super("Exception thrown for case [%s]".formatted(caseId), ex);
    }

    public NotificationClientRuntimeException(String caseId) {
        super("Exception thrown for case [%s]".formatted(caseId));
    }
}
