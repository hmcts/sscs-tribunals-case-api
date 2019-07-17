package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import lombok.Getter;

@Getter
public enum OriginalSenderItemList {
    APPELLANT("appellant", "Appellant (or Appointee)"),
    REPRESENTATIVE("representative", "Representative");

    private String code;
    private String label;

    OriginalSenderItemList(String code, String label) {
        this.code = code;
        this.label = label;
    }
}