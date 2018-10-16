package uk.gov.hmcts.reform.sscs.domain.wrapper;

public class Reason {

    private String whatYouDisagreeWith;
    private String reasonForAppealing;

    public String getWhatYouDisagreeWith() {
        return whatYouDisagreeWith;
    }

    public void setWhatYouDisagreeWith(String whatYouDisagreeWith) {
        this.whatYouDisagreeWith = whatYouDisagreeWith;
    }

    public String getReasonForAppealing() {
        return reasonForAppealing;
    }

    public void setReasonForAppealing(String reasonForAppealing) {
        this.reasonForAppealing = reasonForAppealing;
    }
}
