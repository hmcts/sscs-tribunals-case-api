package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@Value
@JsonDeserialize(builder = SyaHearingOptions.SyaHearingOptionsBuilder.class)
@Builder(builderClassName = "SyaHearingOptionsBuilder", toBuilder = true)
public class SyaHearingOptions {

    private Boolean scheduleHearing;

    private String anythingElse;

    private Boolean wantsSupport;

    private Boolean wantsToAttend;

    private String interpreterLanguageType;

    private String signLanguageType;

    private String[] datesCantAttend;

    private String hearingType;

    @JsonProperty("arrangements")
    private SyaArrangements arrangements;

    @JsonProperty("options")
    private SyaOptions options;

    @JsonPOJOBuilder(withPrefix = "")
    public static class SyaHearingOptionsBuilder {

    }

}
