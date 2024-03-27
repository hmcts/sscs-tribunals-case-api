package uk.gov.hmcts.reform.sscs.tyanotifications.extractor;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPOND;
import static uk.gov.hmcts.reform.sscs.config.SscsConstants.ZONE_ID;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
public class DwpResponseReceivedDateExtractor {

    public Optional<ZonedDateTime> extract(SscsCaseData caseData) {

        final List<Event> allCaseEvents = Optional.ofNullable(caseData.getEvents()).orElse(Collections.emptyList());
        for (Event event : allCaseEvents) {
            if (event.getValue() != null && DWP_RESPOND.equals(event.getValue().getEventType())) {
                return Optional.of(event.getValue().getDateTime());
            }
        }
        LocalTime time = LocalTime.now();
        return Optional.ofNullable(caseData.getDwpResponseDate()).map(date -> ZonedDateTime.parse(date + "T" + time.toString() + "Z").toInstant().atZone(ZoneId.of(ZONE_ID)));
    }

}
