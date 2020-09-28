package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import lombok.Getter;

@Getter
public enum OriginalSenderItemList {
    APPELLANT("appellant", "Appellant (or Appointee)"),
    REPRESENTATIVE("representative", "Representative"),
    DWP("dwp", "DWP"),
    JOINT_PARTY("jointParty", "Joint party");

    private String code;
    private String label;

    OriginalSenderItemList(String code, String label) {
        this.code = code;
        this.label = label;
    }
}