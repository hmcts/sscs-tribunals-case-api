package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import java.time.ZonedDateTime;
import java.util.Arrays;

public class Hearing {

    private ZonedDateTime time;

    private String judge;

    private String venue;

    private ZonedDateTime date;

    private TribunalType tribunalType;

    private Boolean isLanguageInterpreterRequired;

    private Boolean isSignLanguageRequired;

    private Boolean isHearingLoopRequired;

    private Boolean hasDisabilityNeeds;

    private String additionalInformation;

    private ExcludeDates[] excludeDates;

    public Hearing(TribunalType tribunalType, Boolean isLanguageInterpreterRequired, Boolean isSignLanguageRequired, Boolean isHearingLoopRequired, Boolean hasDisabilityNeeds, String additionalInformation, ExcludeDates[] excludeDates) {
        this.tribunalType = tribunalType;
        this.isLanguageInterpreterRequired = isLanguageInterpreterRequired;
        this.isSignLanguageRequired = isSignLanguageRequired;
        this.isHearingLoopRequired = isHearingLoopRequired;
        this.hasDisabilityNeeds = hasDisabilityNeeds;
        this.additionalInformation = additionalInformation;
        this.excludeDates = excludeDates;
    }

    @XmlTransient
    public ZonedDateTime getTime() { return time; }

    public void setTime(ZonedDateTime time) { this.time = time; }

    @XmlTransient
    public String getJudge() { return judge; }

    public void setJudge(String judge) { this.judge = judge; }

    @XmlTransient
    public String getVenue() { return venue; }

    public void setVenue(String venue) { this.venue = venue; }

    @XmlTransient
    public ZonedDateTime getDate() { return date; }

    public void setDate(ZonedDateTime date) { this.date = date; }

    @XmlElement
    public TribunalType getTribunalType() { return tribunalType; }

    public void setTribunalType(TribunalType tribunalType) { this.tribunalType = tribunalType; }

    @XmlElement
    public Boolean isLanguageInterpreterRequired() { return isLanguageInterpreterRequired; }

    public void setLanguageInterpreterRequired(Boolean isLanguageInterpreterRequired) { this.isLanguageInterpreterRequired = isLanguageInterpreterRequired; }

    @XmlElement
    public Boolean isSignLanguageRequired() { return isSignLanguageRequired; }

    public void setSignLanguageRequired(Boolean isSignLanguageRequired) { this.isSignLanguageRequired = isSignLanguageRequired; }

    @XmlElement
    public Boolean isHearingLoopRequired() { return isHearingLoopRequired; }

    public void setHearingLoopRequired(Boolean isHearingLoopRequired) { this.isHearingLoopRequired = isHearingLoopRequired; }

    @XmlElement
    public Boolean getHasDisabilityNeeds() { return hasDisabilityNeeds; }

    public void setHasDisabilityNeeds(Boolean hasDisabilityNeeds) { this.hasDisabilityNeeds = hasDisabilityNeeds; }

    @XmlElement
    public String getAdditionalInformation() { return additionalInformation; }

    public void setAdditionalInformation(String additionalInformation) { this.additionalInformation = additionalInformation; }

    @XmlElementWrapper(name="excludeDates")
    @XmlElement(name="exclude", type=ExcludeDates.class)
    public ExcludeDates[] getExcludeDates() { return excludeDates; }

    public void setExcludeDates(ExcludeDates[] excludeDates) { this.excludeDates = excludeDates; }

    @Override
    public String toString() {
        return "Hearing{" + " time=" + time + ", judge='" + judge + '\'' + ", venue='" + venue + '\'' + ", date=" + date + ", tribunalType=" + tribunalType + ", isLanguageInterpreterRequired=" + isLanguageInterpreterRequired + ", isSignLanguageRequired=" + isSignLanguageRequired + ", isHearingLoopRequired=" + isHearingLoopRequired + ", hasDisabilityNeeds=" + hasDisabilityNeeds + ", additionalInformation='" + additionalInformation + '\'' + ", excludeDates=" + Arrays.toString(excludeDates) + '}';
    }
}
