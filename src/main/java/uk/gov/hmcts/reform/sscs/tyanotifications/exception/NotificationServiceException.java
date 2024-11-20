package uk.gov.hmcts.reform.sscs.tyanotifications.exception;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import uk.gov.service.notify.NotificationClientException;


@SuppressWarnings("squid:MaximumInheritanceDepth")
public class NotificationServiceException extends RuntimeException {

    private String govNotifyErrorCode = EMPTY;

    public NotificationServiceException(String caseId, Exception ex) {
        super("Exception thrown for case [%s]".formatted(caseId), ex);
        if (ex.getClass().isAssignableFrom(NotificationClientException.class)) {
            govNotifyErrorCode = String.valueOf(((NotificationClientException) ex).getHttpResult());
        }
    }

    public String getGovNotifyErrorCode() {
        return govNotifyErrorCode;
    }
}
