package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;

public class HearingArrangements {

    private boolean languageInterpreter;
    private String languages;
    private boolean signLanguageInterpreter;
    private String signLangaugeType;
    private boolean hearingLoopRequired;
    private boolean disabledAccessRequired;
    private String otherArrangements;

    public HearingArrangements(boolean languageInterpreter, String languages, boolean signLanguageInterpreter, String signLangaugeType, boolean hearingLoopRequired, boolean disabledAccessRequired, String otherArrangements) {
        this.languageInterpreter = languageInterpreter;
        this.languages = languages;
        this.signLanguageInterpreter = signLanguageInterpreter;
        this.signLangaugeType = signLangaugeType;
        this.hearingLoopRequired = hearingLoopRequired;
        this.disabledAccessRequired = disabledAccessRequired;
        this.otherArrangements = otherArrangements;
    }

    @ApiModelProperty(example = "true", required = true)
    @JsonProperty(value = "language_interpreter")
    public boolean isLanguageInterpreter() {
        return languageInterpreter;
    }

    @ApiModelProperty(example = "French")
    @JsonProperty(value = "languages")
    public String getLanguages() {
        return languages;
    }

    @ApiModelProperty(example = "true", required = true)
    @JsonProperty(value = "sign_language_interpreter")
    public boolean isSignLanguageInterpreter() {
        return signLanguageInterpreter;
    }

    @ApiModelProperty(example = "BSL")
    @JsonProperty(value = "sign_language_type")
    public String getSignLangaugeType() {
        return signLangaugeType;
    }

    @ApiModelProperty(example = "true", required = true)
    @JsonProperty(value = "hearing_loop_required")
    public boolean isHearingLoopRequired() {
        return hearingLoopRequired;
    }

    @ApiModelProperty(example = "true", required = true)
    @JsonProperty(value = "disabled_access_required")
    public boolean isDisabledAccessRequired() {
        return disabledAccessRequired;
    }

    @ApiModelProperty(example = "some other arrangements", required = true)
    @JsonProperty(value = "other_arrangements")
    public String getOtherArrangements() {
        return otherArrangements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HearingArrangements that = (HearingArrangements) o;
        return languageInterpreter == that.languageInterpreter
                && signLanguageInterpreter == that.signLanguageInterpreter
                && hearingLoopRequired == that.hearingLoopRequired
                && disabledAccessRequired == that.disabledAccessRequired
                && Objects.equals(languages, that.languages)
                && Objects.equals(signLangaugeType, that.signLangaugeType)
                && Objects.equals(otherArrangements, that.otherArrangements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(languageInterpreter, languages, signLanguageInterpreter, signLangaugeType, hearingLoopRequired, disabledAccessRequired, otherArrangements);
    }

    @Override
    public String toString() {
        return "HearingArrangements{"
                + "languageInterpreter=" + languageInterpreter
                + ", languages='" + languages + '\''
                + ", signLanguageInterpreter=" + signLanguageInterpreter
                + ", signLangaugeType='" + signLangaugeType + '\''
                + ", hearingLoopRequired=" + hearingLoopRequired
                + ", disabledAccessRequired=" + disabledAccessRequired
                + ", otherArrangements='" + otherArrangements + '\''
                + '}';
    }
}
