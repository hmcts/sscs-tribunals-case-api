package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

public enum EsaQuestionType {
    SCHEDULE_2(""),  SCHEDULE_3("schedule3");

    private String questionKeyPrefix;

    EsaQuestionType(String questionKeyPrefix) {
        this.questionKeyPrefix = questionKeyPrefix;
    }

    public String getQuestionKeyPrefix() {
        return questionKeyPrefix;
    }
}
