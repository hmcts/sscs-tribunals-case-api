package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class SyaHearingOptions {

    private Boolean scheduleHearing;

    private String anythingElse;

    private Boolean wantsSupport;

    private Boolean wantsToAttend;

    private String interpreterLanguageType;

    private String signLanguageType;

    private String[] datesCantAttend;

    @JsonProperty("arrangements")
    private SyaArrangements arrangements;

    public SyaHearingOptions() {
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

    public SyaArrangements getArrangements() {
        return arrangements;
    }

    public void setArrangements(SyaArrangements arrangements) {
        this.arrangements = arrangements;
    }

    @Override
    public String toString() {
        return "SyaHearing{"
                + "scheduleHearing=" + scheduleHearing
                + ", anythingElse='" + anythingElse + '\''
                + ", wantsSupport=" + wantsSupport
                + ", wantsToAttend=" + wantsToAttend
                + ", datesCantAttend=" + Arrays.toString(datesCantAttend)
                + ", arrangements=" + arrangements
                + ", interpreterLanguageType=" + interpreterLanguageType
                + ", signLanguageType=" + signLanguageType
                + '}';
    }

    public String getInterpreterLanguageType() {
        return interpreterLanguageType;
    }

    public void setInterpreterLanguageType(String interpreterLanguageType) {
        this.interpreterLanguageType = interpreterLanguageType;
    }

    public String getSignLanguageType() {
        return signLanguageType;
    }

    public void setSignLanguageType(String signLanguageType) {
        this.signLanguageType = signLanguageType;
    }
}
