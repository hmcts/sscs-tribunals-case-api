package uk.gov.hmcts.reform.sscs.notifications.gov.notify.service;

import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

@Service
public class DateTimeProvider {
    public ZonedDateTime now() {
        return ZonedDateTime.now();
    }
}
