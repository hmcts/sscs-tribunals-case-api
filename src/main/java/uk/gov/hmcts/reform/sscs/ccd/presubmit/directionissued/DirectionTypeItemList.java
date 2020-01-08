package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import lombok.Getter;

@Getter
public enum DirectionTypeItemList {
    APPEAL_TO_PROCEED("appealToProceed", "Appeal to Proceed"),
    PROVIDE_INFORMATION("provideInformation", "Provide information"),
    GRANT_EXTENSION("grantExtension", "Allow time extension"),
    REFUSE_EXTENSION("refuseExtension", "Refuse time extension");

    private String code;
    private String label;

    DirectionTypeItemList(String code, String label) {
        this.code = code;
        this.label = label;
    }

}
