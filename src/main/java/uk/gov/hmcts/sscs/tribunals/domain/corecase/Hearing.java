package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.Arrays;

public class Hearing {

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

    public TribunalType getTribunalType() { return tribunalType; }

    public void setTribunalType(TribunalType tribunalType) { this.tribunalType = tribunalType; }

    public Boolean isLanguageInterpreterRequired() { return isLanguageInterpreterRequired; }

    public void setLanguageInterpreterRequired(Boolean isLanguageInterpreterRequired) { this.isLanguageInterpreterRequired = isLanguageInterpreterRequired; }

    public Boolean isSignLanguageRequired() { return isSignLanguageRequired; }

    public void setSignLanguageRequired(Boolean isSignLanguageRequired) { this.isSignLanguageRequired = isSignLanguageRequired; }

    public Boolean isHearingLoopRequired() { return isHearingLoopRequired; }

    public void setHearingLoopRequired(Boolean isHearingLoopRequired) { this.isHearingLoopRequired = isHearingLoopRequired; }

    public Boolean getHasDisabilityNeeds() { return hasDisabilityNeeds; }

    public void setHasDisabilityNeeds(Boolean hasDisabilityNeeds) { this.hasDisabilityNeeds = hasDisabilityNeeds; }

    public String getAdditionalInformation() { return additionalInformation; }

    public void setAdditionalInformation(String additionalInformation) { this.additionalInformation = additionalInformation; }

    @XmlElementWrapper(name="excludeDates")
    @XmlElement(name="exclude", type=ExcludeDates.class)
    public ExcludeDates[] getExcludeDates() { return excludeDates; }

    public void setExcludeDates(ExcludeDates[] excludeDates) { this.excludeDates = excludeDates; }

    @Override
    public String toString() {
        return "Hearing{"
                + " tribunalType=" + tribunalType
                + ", isLanguageInterpreterRequired=" + isLanguageInterpreterRequired
                + ", isSignLanguageRequired=" + isSignLanguageRequired
                + ", isHearingLoopRequired=" + isHearingLoopRequired
                + ", hasDisabilityNeeds=" + hasDisabilityNeeds
                + ", additionalInformation='" + additionalInformation + '\''
                + ", excludeDates=" + Arrays.toString(excludeDates)
                + '}';
    }
}
