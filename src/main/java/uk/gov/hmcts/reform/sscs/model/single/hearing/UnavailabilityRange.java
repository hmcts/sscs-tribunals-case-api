package uk.gov.hmcts.reform.sscs.model.single.hearing;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnavailabilityRange {

    private LocalDate unavailableFromDate;

    private LocalDate unavailableToDate;

    private String unavailabilityType;
}
