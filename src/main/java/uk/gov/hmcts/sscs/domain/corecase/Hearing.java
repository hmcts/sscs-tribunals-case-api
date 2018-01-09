package uk.gov.hmcts.sscs.domain.corecase;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

public class Hearing {

    private TribunalType tribunalType;

    private Boolean languageInterpreterRequired;

    private Boolean signLanguageRequired;

    private Boolean hearingLoopRequired;

    private Boolean hasDisabilityNeeds;

    private String additionalInformation;

    private ExcludeDates[] excludeDates;

    private Address address;

    private String venueName;

    private ZonedDateTime dateTime;

    private Boolean scheduleHearing;

    private Boolean wantsSupport;

    private Boolean wantsToAttend;

    public Hearing() {
    }

    public Hearing(TribunalType tribunalType, Boolean languageInterpreterRequired,
                   Boolean signLanguageRequired, Boolean hearingLoopRequired,
                   Boolean hasDisabilityNeeds, String additionalInformation,
                   ExcludeDates[] excludeDates) {
        this.tribunalType = tribunalType;
        this.languageInterpreterRequired = languageInterpreterRequired;
        this.signLanguageRequired = signLanguageRequired;
        this.hearingLoopRequired = hearingLoopRequired;
        this.hasDisabilityNeeds = hasDisabilityNeeds;
        this.additionalInformation = additionalInformation;
        this.excludeDates = excludeDates;
    }

    public Hearing(Address address, ZonedDateTime dateTime) {
        this.address = address;
        this.dateTime = dateTime;
    }

    public TribunalType getTribunalType() {
        return tribunalType;
    }

    public String getTribunalTypeText() {
        if (tribunalType != null) {
            return tribunalType.toString();
        } else {
            return null;
        }
    }

    public void setTribunalType(TribunalType tribunalType) {
        this.tribunalType = tribunalType;
    }

    public Boolean getLanguageInterpreterRequired() {
        return languageInterpreterRequired;
    }

    public String getLanguageInterpreterRequiredForXml() {
        return languageInterpreterRequired != null && languageInterpreterRequired
                ? "Yes, Language Interpreter required" : "No";
    }

    public void setLanguageInterpreterRequired(Boolean languageInterpreterRequired) {
        this.languageInterpreterRequired = languageInterpreterRequired;
    }

    public Boolean getSignLanguageRequired() {
        return signLanguageRequired;
    }

    public String getSignLanguageRequiredForXml() {
        return signLanguageRequired != null && signLanguageRequired
                ? "Yes, Sign Language Interpreter required" : "No";
    }

    public void setSignLanguageRequired(Boolean signLanguageRequired) {
        this.signLanguageRequired = signLanguageRequired;
    }

    public Boolean getHearingLoopRequired() {
        return hearingLoopRequired;
    }

    public String getHearingLoopRequiredForXml() {
        return hearingLoopRequired != null && hearingLoopRequired
                ? "Yes, A hearing loop is required" : "No";
    }

    public void setHearingLoopRequired(Boolean hearingLoopRequired) {
        this.hearingLoopRequired = hearingLoopRequired;
    }

    public Boolean getHasDisabilityNeeds() {
        return hasDisabilityNeeds;
    }

    public String getHasDisabilityNeedsForXml() {
        return hasDisabilityNeeds != null && hasDisabilityNeeds ? "Yes" : "No";
    }

    public void setHasDisabilityNeeds(Boolean hasDisabilityNeeds) {
        this.hasDisabilityNeeds = hasDisabilityNeeds;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public ExcludeDates[] getExcludeDates() {
        return excludeDates;
    }

    public void setExcludeDates(ExcludeDates[] excludeDates) {
        this.excludeDates = excludeDates;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(ZonedDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Boolean getScheduleHearing() {
        return scheduleHearing;
    }

    public void setScheduleHearing(Boolean scheduleHearing) {
        this.scheduleHearing = scheduleHearing;
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

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    @Override
    public String toString() {
        return "Hearing{"
                + " tribunalType=" + tribunalType
                + ", languageInterpreterRequired='" + languageInterpreterRequired + '\''
                + ", signLanguageRequired='" + signLanguageRequired + '\''
                + ", hearingLoopRequired='" + hearingLoopRequired + '\''
                + ", hasDisabilityNeeds='" + hasDisabilityNeeds + '\''
                + ", additionalInformation='" + additionalInformation + '\''
                + ", excludeDates=" + Arrays.toString(excludeDates)
                + ", address=" + address
                + ", dateTime=" + dateTime
                + ", scheduleHearing=" + scheduleHearing
                + ", wantsSupport=" + wantsSupport
                + ", wantsToAttend=" + wantsToAttend
                + ", venueName=" + venueName
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Hearing hearing = (Hearing) o;
        return tribunalType == hearing.tribunalType
                && Objects.equals(languageInterpreterRequired, hearing.languageInterpreterRequired)
                && Objects.equals(signLanguageRequired, hearing.signLanguageRequired)
                && Objects.equals(hearingLoopRequired, hearing.hearingLoopRequired)
                && Objects.equals(hasDisabilityNeeds, hearing.hasDisabilityNeeds)
                && Objects.equals(additionalInformation, hearing.additionalInformation)
                && Arrays.equals(excludeDates, hearing.excludeDates)
                && Objects.equals(address, hearing.address)
                && Objects.equals(dateTime, hearing.dateTime)
                && Objects.equals(scheduleHearing, hearing.scheduleHearing)
                && Objects.equals(wantsSupport, hearing.wantsSupport)
                && Objects.equals(wantsToAttend, hearing.wantsToAttend)
                && Objects.equals(venueName, hearing.venueName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tribunalType, languageInterpreterRequired, signLanguageRequired,
                hearingLoopRequired, hasDisabilityNeeds, additionalInformation, excludeDates,
                address, dateTime, scheduleHearing, wantsSupport, wantsToAttend, venueName);
    }

}
