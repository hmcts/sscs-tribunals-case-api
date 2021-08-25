package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import lombok.Getter;

@Getter
public enum ExtensionNextEventItemList {
    SEND_TO_LISTING("sendToListing", "List for hearing"),
    SEND_TO_VALID_APPEAL("sendToValidAppeal", "Make valid appeal"),
    NO_FURTHER_ACTION("noFurtherAction", "No further action");

    private String code;
    private String label;

    ExtensionNextEventItemList(String code, String label) {
        this.code = code;
        this.label = label;
    }
}