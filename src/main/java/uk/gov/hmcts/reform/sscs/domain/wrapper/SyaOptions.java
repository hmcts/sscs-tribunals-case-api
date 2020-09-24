package uk.gov.hmcts.reform.sscs.domain.wrapper;

import lombok.Data;

@Data
public class SyaOptions {
    private Boolean hearingTypeTelephone;
    private String telephone;
    private Boolean hearingTypeVideo;
    private String email;
    private Boolean hearingTypeFaceToFace;
}
