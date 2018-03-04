package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HearingDetails {
    private Venue venue;
    private String hearingDate;
    private String time;
    private String adjourned;

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public String getHearingDate() {
        return hearingDate;
    }

    public void setHearingDate(String hearingDate) {
        this.hearingDate = hearingDate;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAdjourned() {
        return adjourned;
    }

    public void setAdjourned(String adjourned) {
        this.adjourned = adjourned;
    }

    @Override
    public String toString() {
        return "HearingDetails{" +
                "venue=" + venue +
                ", hearingDate='" + hearingDate + '\'' +
                ", time='" + time + '\'' +
                ", adjourned='" + adjourned + '\'' +
                '}';
    }
}
