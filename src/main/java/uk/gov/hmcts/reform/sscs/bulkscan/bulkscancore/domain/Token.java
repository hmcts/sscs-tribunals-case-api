package uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder

public class Token {

    private String userAuthToken;
    private String serviceAuthToken;
    private String userId;

}
