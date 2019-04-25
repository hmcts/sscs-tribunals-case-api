package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionMrnOverThirteenMonthsLate {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public String reasonForBeingLate;

    public SessionMrnOverThirteenMonthsLate(MrnDetails mrnDetails) {
        this.reasonForBeingLate = mrnOverThirteenMonthsLate(mrnDetails)
            ? mrnDetails.getMrnLateReason()
            : null;
    }

    public static Boolean mrnOverThirteenMonthsLate(MrnDetails mrnDetails) {
        LocalDate mrnDate = LocalDate.parse(mrnDetails.getMrnDate(), DATE_FORMATTER);
        return mrnDate.plusMonths(13L).isBefore(LocalDate.now());
    }
}
