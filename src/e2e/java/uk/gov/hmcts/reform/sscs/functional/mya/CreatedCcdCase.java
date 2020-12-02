package uk.gov.hmcts.reform.sscs.functional.mya;

public class CreatedCcdCase {
    private final String caseId;
    private final String appellantTya;
    private final String jointPartyTya;
    private final String representativeTya;

    public CreatedCcdCase(String caseId, String appellantTya) {
        this.caseId = caseId;
        this.appellantTya = appellantTya;
        jointPartyTya = null;
        representativeTya = null;
    }

    public CreatedCcdCase(String caseId, String appellantTya, String jointPartyTya, String representativeTya) {
        this.caseId = caseId;
        this.appellantTya = appellantTya;
        this.jointPartyTya = jointPartyTya;
        this.representativeTya = representativeTya;
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

    public String getRepresentativeTya() {
        return representativeTya;
    }
}
