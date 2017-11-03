package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.time.ZonedDateTime;
import java.util.Arrays;

public class Hearing {

    private ZonedDateTime time;

    private String judge;

    private String venue;

    private ZonedDateTime date;

    private HearingType hearingType;

    private Boolean isOralHearingRequired;

    private Boolean isLanguageInterpreterRequired;

    private Boolean isSignLanguageRequired;

    private Boolean isHearingLoopRequired;

    private Boolean hasDisabilityNeeds;

    private String additionalInformation;

    private ExcludeDates[] excludeDates;

    public ZonedDateTime getTime() { return time; }

    public void setTime(ZonedDateTime time) { this.time = time; }

    public String getJudge() { return judge; }

    public void setJudge(String judge) { this.judge = judge; }

    public String getVenue() { return venue; }

    public void setVenue(String venue) { this.venue = venue; }

    public ZonedDateTime getDate() { return date; }

    public void setDate(ZonedDateTime date) { this.date = date; }

    public Boolean getOralHearingRequired() { return isOralHearingRequired; }

    public void setOralHearingRequired(Boolean oralHearingRequired) { isOralHearingRequired = oralHearingRequired; }

    public HearingType getHearingType() { return hearingType; }

    public void setHearingType(HearingType hearingType) { this.hearingType = hearingType; }

    public Boolean getLanguageInterpreterRequired() { return isLanguageInterpreterRequired; }

    public void setLanguageInterpreterRequired(Boolean languageInterpreterRequired) { isLanguageInterpreterRequired = languageInterpreterRequired; }

    public Boolean getSignLanguageRequired() { return isSignLanguageRequired; }

    public void setSignLanguageRequired(Boolean signLanguageRequired) { isSignLanguageRequired = signLanguageRequired; }

    public Boolean getHearingLoopRequired() { return isHearingLoopRequired; }

    public void setHearingLoopRequired(Boolean hearingLoopRequired) { isHearingLoopRequired = hearingLoopRequired; }

    public Boolean getHasDisabilityNeeds() { return hasDisabilityNeeds; }

    public void setHasDisabilityNeeds(Boolean hasDisabilityNeeds) { this.hasDisabilityNeeds = hasDisabilityNeeds; }

    public String getAdditionalInformation() { return additionalInformation; }

    public void setAdditionalInformation(String additionalInformation) { this.additionalInformation = additionalInformation; }

    public ExcludeDates[] getExcludeDates() { return excludeDates; }

    public void setExcludeDates(ExcludeDates[] excludeDates) { this.excludeDates = excludeDates; }

    @Override
    public String toString() {
        return "Hearing{" + " time=" + time + ", judge='" + judge + '\'' + ", venue='" + venue + '\'' + ", date=" + date + ", hearingType=" + hearingType + ", isLanguageInterpreterRequired=" + isLanguageInterpreterRequired + ", isSignLanguageRequired=" + isSignLanguageRequired + ", isHearingLoopRequired=" + isHearingLoopRequired + ", hasDisabilityNeeds=" + hasDisabilityNeeds + ", additionalInformation='" + additionalInformation + '\'' + ", excludeDates=" + Arrays.toString(excludeDates) + '}';
    }
}
