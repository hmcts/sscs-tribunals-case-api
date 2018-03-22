package uk.gov.hmcts.sscs.util;

import static java.time.format.DateTimeFormatter.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;


public final class DateTimeUtils {

    public static final String EUROPE_LONDON = "Europe/London";
    public static final String UTC = "UTC";
    public static final String UTC_STRING_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private DateTimeUtils() {
        //
    }
    
    public static String convertLocalDateTimetoUtc(String localDateStr, String localTimeStr) {

        LocalDate localDate = LocalDate.parse(localDateStr);
        LocalTime localTime = LocalTime.parse(localTimeStr);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDate, localTime, ZoneId.of(EUROPE_LONDON));
        ZonedDateTime utcZonedDateTime = ZonedDateTime.ofInstant(zonedDateTime.toInstant(), ZoneId.of(UTC));
        return utcZonedDateTime.format(ofPattern(UTC_STRING_FORMAT));

    }
    
}
