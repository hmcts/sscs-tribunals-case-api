package uk.gov.hmcts.reform.sscs.domain.wrapper;

public class SyaArrangements {

    private Boolean signLanguageInterpreter;

    private Boolean languageInterpreter;

    private Boolean hearingLoop;

    private Boolean accessibleHearingRoom;

    private Boolean other;

    public Boolean getSignLanguageInterpreter() {
        return signLanguageInterpreter;
    }

    public void setSignLanguageInterpreter(Boolean signLanguageInterpreter) {
        this.signLanguageInterpreter = signLanguageInterpreter;
    }

    public Boolean getLanguageInterpreter() {
        return languageInterpreter;
    }

    public void setLanguageInterpreter(Boolean languageInterpreter) {
        this.languageInterpreter = languageInterpreter;
    }

    public Boolean getHearingLoop() {
        return hearingLoop;
    }

    public void setHearingLoop(Boolean hearingLoop) {
        this.hearingLoop = hearingLoop;
    }

    public Boolean getAccessibleHearingRoom() {
        return accessibleHearingRoom;
    }

    public void setAccessibleHearingRoom(Boolean accessibleHearingRoom) {
        this.accessibleHearingRoom = accessibleHearingRoom;
    }

    public Boolean getOther() {
        return other;
    }

    public void setOther(Boolean other) {
        this.other = other;
    }

    @Override
    public String toString() {
        return "SyaArrangements{"
            + "signLanguageInterpreter=" + signLanguageInterpreter
            + ", languageInterpreter=" + languageInterpreter
            + ", hearingLoop=" + hearingLoop
            + ", accessibleHearingRoom=" + accessibleHearingRoom
            + ", other=" + other
            + '}';
    }
}
