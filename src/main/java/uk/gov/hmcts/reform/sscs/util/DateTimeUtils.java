package uk.gov.hmcts.reform.sscs.util;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.*;
import java.time.format.DateTimeFormatter;


public final class DateTimeUtils {

    private static final String EUROPE_LONDON = "Europe/London";
    private static final String UTC = "UTC";
    private static final String UTC_STRING_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final DateTimeFormatter DATEFORMATTER = DateTimeFormatter.ofPattern(UTC_STRING_FORMAT);

    private DateTimeUtils() {
        //
    }
    
    public static String convertLocalDateLocalTimetoUtc(String localDateStr, String localTimeStr) {

        LocalDate localDate = LocalDate.parse(localDateStr);
        LocalTime localTime = LocalTime.parse(localTimeStr);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDate, localTime, ZoneId.of(EUROPE_LONDON));
        return formatUtc(zonedDateTime);

    }


    public static LocalDateTime getLocalDateTime(String localDateStr, String localTimeStr) {
        LocalDate localDate = LocalDate.parse(localDateStr);
        LocalTime localTime = LocalTime.parse(localTimeStr);
        return LocalDateTime.of(localDate, localTime);
    }

    public static String convertLocalDateTimetoUtc(LocalDateTime localDateTime) {

        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of(EUROPE_LONDON));
        return formatUtc(zonedDateTime);

    }

    private static String formatUtc(ZonedDateTime zonedDateTime) {
        ZonedDateTime utcZonedDateTime = ZonedDateTime.ofInstant(zonedDateTime.toInstant(), ZoneId.of(UTC));
        return utcZonedDateTime.format(ofPattern(UTC_STRING_FORMAT));
    }

}
