package uk.gov.hmcts.reform.sscs.functional.mya;

public class OnlineHearing {
    private String emailAddress;
    private String hearingId;
    private String questionId;
    private String caseId;

    public OnlineHearing(String emailAddress, String hearingId, String questionId, String caseId) {
        this.emailAddress = emailAddress;
        this.hearingId = hearingId;
        this.questionId = questionId;
        this.caseId = caseId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getHearingId() {
        return hearingId;
    }

    public void setHearingId(String hearingId) {
        this.hearingId = hearingId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }
}
