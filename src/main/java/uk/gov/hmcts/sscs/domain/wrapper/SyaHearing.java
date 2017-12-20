package uk.gov.hmcts.sscs.domain.wrapper;

import java.util.Arrays;

public class SyaHearing {

    private Boolean scheduleHearing;

    private String anythingElse;

    private Boolean wantsSupport;

    private Boolean wantsToAttend;

    private String[] datesCantAttend;

    private String[] arrangements;

    public SyaHearing() {
        // For JSON
    }

    public Boolean getScheduleHearing() {
        return scheduleHearing;
    }

    public void setScheduleHearing(Boolean scheduleHearing) {
        this.scheduleHearing = scheduleHearing;
    }

    public String getAnythingElse() {
        return anythingElse;
    }

    public void setAnythingElse(String anythingElse) {
        this.anythingElse = anythingElse;
    }

    public Boolean getWantsSupport() {
        return wantsSupport;
    }

    public void setWantsSupport(Boolean wantsSupport) {
        this.wantsSupport = wantsSupport;
    }

    public Boolean getWantsToAttend() {
        return wantsToAttend;
    }

    public void setWantsToAttend(Boolean wantsToAttend) {
        this.wantsToAttend = wantsToAttend;
    }

    public String[] getDatesCantAttend() {
        return datesCantAttend;
    }

    public void setDatesCantAttend(String[] datesCantAttend) {
        this.datesCantAttend = datesCantAttend;
    }

    public String[] getArrangements() {
        return arrangements;
    }

    public void setArrangements(String[] arrangements) {
        this.arrangements = arrangements;
    }

    @Override
    public String toString() {
        return "SyaHearing{"
                + " scheduleHearing=" + scheduleHearing
                + ", anythingElse='" + anythingElse + '\''
                + ", wantsSupport=" + wantsSupport
                + ", wantsToAttend=" + wantsToAttend
                + ", datesCantAttend=" + Arrays.toString(datesCantAttend)
                + ", arrangements=" + Arrays.toString(arrangements)
                + '}';
    }
}
