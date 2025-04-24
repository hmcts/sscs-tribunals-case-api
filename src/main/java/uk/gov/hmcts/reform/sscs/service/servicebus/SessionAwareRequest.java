package uk.gov.hmcts.reform.sscs.service.servicebus;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface SessionAwareRequest {

    @JsonIgnore
    String getSessionId();
}
