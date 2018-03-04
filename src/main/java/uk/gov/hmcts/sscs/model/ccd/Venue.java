package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Venue {
    private String venueTown;

    public String getVenueTown() {
        return venueTown;
    }

    public void setVenueTown(String venueTown) {
        this.venueTown = venueTown;
    }

    @Override
    public String toString() {
        return "Venue{" +
                "venueTown='" + venueTown + '\'' +
                '}';
    }
}
