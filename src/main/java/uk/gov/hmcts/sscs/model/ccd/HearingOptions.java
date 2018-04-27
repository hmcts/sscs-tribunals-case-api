package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class HearingOptions {

    private String wantsToAttend;
    private String wantsSupport;
    private String languageInterpreter;
    private String languages;
    private List<String> arrangements;
    private String scheduleHearing;
    private List<ExcludeDate> excludeDates;
    private String other;

    @JsonCreator
    public HearingOptions(@JsonProperty("wantsToAttend") String wantsToAttend,
                          @JsonProperty("wantsSupport") String wantsSupport,
                          @JsonProperty("languageInterpreter") String languageInterpreter,
                          @JsonProperty("languages") String languages,
                          @JsonProperty("arrangements") List<String> arrangements,
                          @JsonProperty("scheduleHearing") String scheduleHearing,
                          @JsonProperty("excludeDates") List<ExcludeDate> excludeDates,
                          @JsonProperty("other") String other) {
        this.wantsToAttend = wantsToAttend;
        this.wantsSupport = wantsSupport;
        this.languageInterpreter = languageInterpreter;
        this.languages = languages;
        this.arrangements = arrangements;
        this.scheduleHearing = scheduleHearing;
        this.excludeDates = excludeDates;
        this.other = other;
    }
}
