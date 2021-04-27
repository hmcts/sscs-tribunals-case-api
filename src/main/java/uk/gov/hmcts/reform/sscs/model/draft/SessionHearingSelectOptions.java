package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;

@Value
public class SessionHearingSelectOptions {
    private SessionHearingOptionsTelephone telephone;
    private SessionHearingOptionsVideo video;
    private SessionHearingOptionsFaceToFace faceToFace;
}
