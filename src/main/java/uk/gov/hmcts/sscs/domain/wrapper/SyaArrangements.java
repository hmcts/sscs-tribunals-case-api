package uk.gov.hmcts.sscs.domain.wrapper;

public class SyaArrangements {

    private Boolean signLanguageInterpreter;

    private Boolean languageInterpreter;

    private Boolean disabledAccess;

    private Boolean hearingLoop;


    public SyaArrangements() {
    }

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

    public Boolean getDisabledAccess() {
        return disabledAccess;
    }

    public void setDisabledAccess(Boolean disabledAccess) {
        this.disabledAccess = disabledAccess;
    }

    public Boolean getHearingLoop() {
        return hearingLoop;
    }

    public void setHearingLoop(Boolean hearingLoop) {
        this.hearingLoop = hearingLoop;
    }

    @Override
    public String toString() {
        return "SyaArrangements{"
            + "signLanguageInterpreter=" + signLanguageInterpreter
            + ", languageInterpreter=" + languageInterpreter
            + ", disabledAccess=" + disabledAccess
            + ", hearingLoop=" + hearingLoop
            + '}';
    }
}
