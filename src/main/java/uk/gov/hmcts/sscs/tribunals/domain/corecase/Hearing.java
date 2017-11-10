package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.util.Arrays;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

public class Hearing {

    private TribunalType tribunalType;

    private String languageInterpreterRequired;

    private String signLanguageRequired;

    private String hearingLoopRequired;

    private String hasDisabilityNeeds;

    private String additionalInformation;

    private ExcludeDates[] excludeDates;

    public Hearing() {
    }

    public Hearing(TribunalType tribunalType, String languageInterpreterRequired,
                   String signLanguageRequired, String hearingLoopRequired,
                   String hasDisabilityNeeds, String additionalInformation,
                   ExcludeDates[] excludeDates) {
        this.tribunalType = tribunalType;
        this.languageInterpreterRequired = languageInterpreterRequired;
        this.signLanguageRequired = signLanguageRequired;
        this.hearingLoopRequired = hearingLoopRequired;
        this.hasDisabilityNeeds = hasDisabilityNeeds;
        this.additionalInformation = additionalInformation;
        this.excludeDates = excludeDates;
    }

    public TribunalType getTribunalType() {
        return tribunalType;
    }

    public void setTribunalType(TribunalType tribunalType) {
        this.tribunalType = tribunalType;
    }

    @XmlTransient
    public String getLanguageInterpreterRequired() {
        return languageInterpreterRequired;
    }

    @XmlElement(name = "languageInterpreterRequired", required = true)
    public String getLanguageInterpreterRequiredForXml() {
        return (languageInterpreterRequired == "Yes") ? "Yes, Language Interpreter required" : "No";
    }

    public void setLanguageInterpreterRequired(String languageInterpreterRequired) {
        this.languageInterpreterRequired = languageInterpreterRequired;
    }

    @XmlTransient
    public String getSignLanguageRequired() {
        return signLanguageRequired;
    }

    @XmlElement(name = "signLanguageRequired", required = true)
    public String getSignLanguageRequiredForXml() {
        return (signLanguageRequired == "Yes") ? "Yes, Sign Language Interpreter required" : "No";
    }

    public void setSignLanguageRequired(String signLanguageRequired) {
        this.signLanguageRequired = signLanguageRequired;
    }

    @XmlTransient
    public String getHearingLoopRequired() {
        return hearingLoopRequired;
    }

    @XmlElement(name = "hearingLoopRequired", required = true)
    public String getHearingLoopRequiredForXml() {
        return (hearingLoopRequired == "Yes") ? "Yes, A hearing loop is required" : "No";
    }

    public void setHearingLoopRequired(String hearingLoopRequired) {
        this.hearingLoopRequired = hearingLoopRequired;
    }

    public String getHasDisabilityNeeds() {
        return hasDisabilityNeeds;
    }

    public void setHasDisabilityNeeds(String hasDisabilityNeeds) {
        this.hasDisabilityNeeds = hasDisabilityNeeds;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    @XmlElementWrapper(name = "excludeDates")
    @XmlElement(name = "exclude", type = ExcludeDates.class)
    public ExcludeDates[] getExcludeDates() {
        return excludeDates;
    }

    public void setExcludeDates(ExcludeDates[] excludeDates) {
        this.excludeDates = excludeDates;
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
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Hearing)) {
            return false;
        }
        Hearing hearing = (Hearing) o;
        return tribunalType == hearing.tribunalType
                && Objects.equals(languageInterpreterRequired, hearing.languageInterpreterRequired)
                && Objects.equals(signLanguageRequired, hearing.signLanguageRequired)
                && Objects.equals(hearingLoopRequired, hearing.hearingLoopRequired)
                && Objects.equals(hasDisabilityNeeds, hearing.hasDisabilityNeeds)
                && Objects.equals(additionalInformation, hearing.additionalInformation)
                && Arrays.equals(excludeDates, hearing.excludeDates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tribunalType, languageInterpreterRequired, signLanguageRequired,
                hearingLoopRequired, hasDisabilityNeeds, additionalInformation, excludeDates);
    }
}
