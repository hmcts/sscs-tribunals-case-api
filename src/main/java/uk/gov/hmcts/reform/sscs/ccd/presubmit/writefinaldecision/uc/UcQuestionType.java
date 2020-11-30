package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

public enum UcQuestionType {
    SCHEDULE_6(""),  SCHEDULE_7("schedule7");

    private String questionKeyPrefix;

    UcQuestionType(String questionKeyPrefix) {
        this.questionKeyPrefix = questionKeyPrefix;
    }

    public String getQuestionKeyPrefix() {
        return questionKeyPrefix;
    }
}
