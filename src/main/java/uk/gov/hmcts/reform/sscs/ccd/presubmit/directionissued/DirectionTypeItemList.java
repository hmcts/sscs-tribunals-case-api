package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import lombok.Getter;

@Getter
public enum DirectionTypeItemList {

    APPEAL_TO_PROCEED("appealToProceed", "Appeal to Proceed"),
    PROVIDE_INFORMATION("provideInformation", "Provide information"),
    GRANT_EXTENSION("grantExtension", "Allow time extension"),
    REFUSE_EXTENSION("refuseExtension", "Refuse time extension"),
    GRANT_REINSTATEMENT("grantReinstatement", "Grant reinstatement"),
    REFUSE_REINSTATEMENT("refuseReinstatement", "Refuse reinstatement"),
    GRANT_URGENT_HEARING("grantUrgentHearing", "Grant urgent hearing"),
    REFUSE_URGENT_HEARING("refuseUrgentHearing", "Refuse urgent hearing"),
    REFUSE_HEARING_RECORDING_REQUEST("refuseHearingRecordingRequest", "Refuse hearing recording request");

    private String code;
    private String label;

    DirectionTypeItemList(String code, String label) {
        this.code = code;
        this.label = label;
    }

}
