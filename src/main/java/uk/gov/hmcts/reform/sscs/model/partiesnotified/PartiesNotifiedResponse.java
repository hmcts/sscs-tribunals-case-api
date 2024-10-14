package uk.gov.hmcts.reform.sscs.model.partiesnotified;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartiesNotifiedResponse {

    private LocalDateTime partiesNotified;

    private Long requestVersion;

    private LocalDateTime responseReceivedDateTime;

    private ServiceData serviceData;
}
