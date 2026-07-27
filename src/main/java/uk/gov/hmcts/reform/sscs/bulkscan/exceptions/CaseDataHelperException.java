package uk.gov.hmcts.reform.sscs.bulkscan.exceptions;

import static java.lang.String.format;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class CaseDataHelperException extends RuntimeException {

    public CaseDataHelperException(String exceptionRecordId, Exception ex) {
        super(format("Exception thrown for exception record [%s]", exceptionRecordId), ex);
    }
}
