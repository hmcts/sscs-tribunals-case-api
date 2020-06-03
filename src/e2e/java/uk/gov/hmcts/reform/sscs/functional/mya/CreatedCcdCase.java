package uk.gov.hmcts.reform.sscs.functional.mya;

public class CreatedCcdCase {
    private final String caseId;
    private final String appellantTya;

    public CreatedCcdCase(String caseId, String appellantTya) {
        this.caseId = caseId;
        this.appellantTya = appellantTya;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getAppellantTya() {
        return appellantTya;
    }
}
