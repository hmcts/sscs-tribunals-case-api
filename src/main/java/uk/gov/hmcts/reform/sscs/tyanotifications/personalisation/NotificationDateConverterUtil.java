package uk.gov.hmcts.reform.sscs.tyanotifications.personalisation;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class NotificationDateConverterUtil {

    public static final DateTimeFormatter COH_DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final DateTimeFormatter EMAIL_DATE_FORMATTER = ofPattern("d MMMM yyyy");

    public String toEmailDate(String cohDate) {
        LocalDate localDate = LocalDate.parse(cohDate, COH_DATE_FORMATTER);
        return toEmailDate(localDate);
    }

    public String toEmailDate(LocalDate localDate) {
        return localDate.format(EMAIL_DATE_FORMATTER);
    }
}
