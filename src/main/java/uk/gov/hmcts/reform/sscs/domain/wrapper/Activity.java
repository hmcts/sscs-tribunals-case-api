package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity {
    private final String activity;
    private final String selectionKey;

    public Activity(String activity, String selectionKey) {
        this.activity = activity;
        this.selectionKey = selectionKey;
    }

    @Schema(example = "an activity", required = true)
    @JsonProperty(value = "activity")
    public String getActivity() {
        return activity;
    }

    @Schema(example = "2.1", required = true)
    @JsonProperty(value = "selection_key")
    public String getSelectionKey() {
        return selectionKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Activity activity1 = (Activity) o;
        return Objects.equals(activity, activity1.activity)
                && Objects.equals(selectionKey, activity1.selectionKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activity, selectionKey);
    }
}
