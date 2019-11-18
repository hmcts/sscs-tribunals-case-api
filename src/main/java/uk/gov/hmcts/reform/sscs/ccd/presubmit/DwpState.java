package uk.gov.hmcts.reform.sscs.ccd.presubmit;

public enum DwpState {
    WITHDRAWAL_RECEIVED("withdrawalReceived", "Withdrawal received"),
    WITHDRAWN("Withdrawn", "Withdrawn"),
    FE_RECEIVED("feReceived", "FE received"),
    FE_ACTIONED_NR("feActionedNR", "FE Actioned - NR"),
    FE_ACTIONED_NA("feActionedNA", "FE Actioned - NA"),
    DIRECTION_ACTION_REQUIRED("directionActionRequired", "Direction - action req'd"),
    DIRECTION_RESPONDED("directionResponded", "Direction - responded"),
    HEARING_POSTPONED("hearingPostponed", null), REP_ADDED("repAdded", null),
    EXTENSION_REQUESTED("extensionRequested", null);

    private String id;
    private String label;

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    DwpState(String id, String label) {
        this.id = id;
        this.label = label;
    }
}
