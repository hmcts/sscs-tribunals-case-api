package uk.gov.hmcts.reform.sscs.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@AllArgsConstructor
@Data
public class HmcFailureMessage implements Message {

    private String requestType;
    private Long caseID;
    private LocalDateTime timeStamp;
    private String errorCode;
    private String errorMessage;

    @Override
    public String toString() {
        return "HmcFailureMessage{"
                + "requestType='" + requestType + '\''
                + ", caseID=" + caseID
                + ", timeStamp=" + timeStamp
                + ", errorCode='" + errorCode + '\''
                + ", errorMessage='" + errorMessage + '\''
                + '}';
    }
}
