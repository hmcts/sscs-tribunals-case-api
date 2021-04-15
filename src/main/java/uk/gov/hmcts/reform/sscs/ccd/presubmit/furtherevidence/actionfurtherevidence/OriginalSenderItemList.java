package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import lombok.Getter;

@Getter
public enum OriginalSenderItemList {
    APPELLANT("appellant", "Appellant (or Appointee)", "Appellant evidence"),
    REPRESENTATIVE("representative", "Representative", "Representative evidence"),
    DWP("dwp", "DWP", "DWP evidence"),
    JOINT_PARTY("jointParty", "Joint party", "Joint party evidence");

    private final String documentFooter;
    private final String code;
    private final String label;

    OriginalSenderItemList(String code, String label, String documentFooter) {
        this.code = code;
        this.label = label;
        this.documentFooter = documentFooter;
    }
}