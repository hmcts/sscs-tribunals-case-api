package uk.gov.hmcts.reform.sscs.domain.wrapper;

public enum UserType {
    APPELLANT("Appellant"),
    APPOINTEE("Appointee"),
    REP("Representative"),
    JOINT_PARTY("JointParty"),
    SUPPORTER("Supporter"),
    OTHER_PARTY("OtherParty"),
    OTHER_PARTY_APPOINTEE("OtherPartyAppointee"),
    OTHER_PARTY_REP("OtherPartyRepresentative");

    private String type;

    UserType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
