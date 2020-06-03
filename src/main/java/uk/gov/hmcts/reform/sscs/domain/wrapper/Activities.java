package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activities {
    private final List<Activity> dailyLiving;
    private final List<Activity> mobility;

    public Activities(List<Activity> dailyLiving, List<Activity> mobility) {
        this.dailyLiving = dailyLiving;
        this.mobility = mobility;
    }

    @JsonProperty(value = "daily_living")
    public List<Activity> getDailyLiving() {
        return dailyLiving;
    }

    @JsonProperty(value = "mobility")
    public List<Activity> getMobility() {
        return mobility;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Activities that = (Activities) o;
        return Objects.equals(dailyLiving, that.dailyLiving)
                && Objects.equals(mobility, that.mobility);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dailyLiving, mobility);
    }
}
