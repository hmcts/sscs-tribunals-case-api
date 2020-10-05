package uk.gov.hmcts.reform.sscs.functional.mya;

public class CreatedCcdCase {
    private final String caseId;
    private final String appellantTya;
    private final String jointPartyTya;

    public CreatedCcdCase(String caseId, String appellantTya) {
        this.caseId = caseId;
        this.appellantTya = appellantTya;
        jointPartyTya = null;
    }

    public CreatedCcdCase(String caseId, String appellantTya, String jointPartyTya) {
        this.caseId = caseId;
        this.appellantTya = appellantTya;
        this.jointPartyTya = jointPartyTya;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getAppellantTya() {
        return appellantTya;
    }

    public String getJointPartyTya() {
        return jointPartyTya;
    }
}
