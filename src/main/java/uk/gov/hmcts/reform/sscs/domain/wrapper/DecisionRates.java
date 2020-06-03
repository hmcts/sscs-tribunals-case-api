package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DecisionRates {
    private final Rate dailyLiving;
    private final Rate mobility;
    private final ComparedRate comparedToDwpAward;

    public DecisionRates(Rate dailyLiving, Rate mobility, ComparedRate comparedToDwpAward) {
        this.dailyLiving = dailyLiving;
        this.mobility = mobility;
        this.comparedToDwpAward = comparedToDwpAward;
    }

    @JsonProperty(value = "daily_living")
    public Rate getDailyLiving() {
        return dailyLiving;
    }

    @JsonProperty(value = "mobility")
    public Rate getMobility() {
        return mobility;
    }

    @JsonProperty(value = "compared_to_dwp")
    public ComparedRate getComparedToDwpAward() {
        return comparedToDwpAward;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DecisionRates that = (DecisionRates) o;
        return dailyLiving == that.dailyLiving
                && mobility == that.mobility
                && comparedToDwpAward == that.comparedToDwpAward;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dailyLiving, mobility, comparedToDwpAward);
    }

    @Override
    public String toString() {
        return "DecisionRates{"
                + "dailyLiving=" + dailyLiving
                + ", mobility=" + mobility
                + ", comparedToDwpAward=" + comparedToDwpAward
                + '}';
    }
}
