package uk.gov.hmcts.reform.sscs.util;

import static java.time.format.DateTimeFormatter.ofPattern;
import static org.apache.commons.lang3.StringUtils.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

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

    public static boolean isDateInTheFuture(LocalDate date) {
        return Optional.ofNullable(date).filter(d -> d.isAfter(LocalDate.now())).isPresent();
    }

    public static boolean isDateInTheFuture(String dateString) {
        return isDateInTheFuture(getLocalDate(dateString).orElse(null));
    }

    public static boolean isDateInThePast(LocalDate date) {
        return Optional.ofNullable(date).filter(d -> d.isBefore(LocalDate.now())).isPresent();
    }

    public static boolean isDateInThePast(LocalDateTime dateTime) {
        return Optional.ofNullable(dateTime).filter(d -> d.isBefore(LocalDateTime.now())).isPresent();
    }

    public static boolean isDateInThePast(String dateString) {
        return isDateInThePast(getLocalDate(dateString).orElse(null));
    }

    public static Optional<LocalDate> getLocalDate(String dateString) {
        return Optional.ofNullable(stripToNull(dateString)).map(LocalDate::parse);
    }

    public static String generateDwpResponseDueDate(int numberOfDays) {
        return LocalDate.now().plusDays(numberOfDays).toString();
    }

    private static String formatUtc(ZonedDateTime zonedDateTime) {
        ZonedDateTime utcZonedDateTime = ZonedDateTime.ofInstant(zonedDateTime.toInstant(), ZoneId.of(UTC));
        return utcZonedDateTime.format(ofPattern(UTC_STRING_FORMAT));
    }

}
