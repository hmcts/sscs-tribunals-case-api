package uk.gov.hmcts.reform.sscs.util;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.getLocalDateTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DateTimeUtilsTest {

    @Test
    void shouldConvertAndReturnBstLocalTimeToUtc() {

        String localDate = "2018-05-01";
        String localTime = "03:15:00";

        String localDateTimetoUtc = DateTimeUtils.convertLocalDateLocalTimetoUtc(localDate, localTime);

        assertThat(localDateTimetoUtc).isEqualTo("2018-05-01T02:15:00.000Z");
    }

    @Test
    void shouldConvertAndReturnGmtLocalTimetToUtc() {

        String localDate = "2018-12-01";
        String localTime = "10:15:00";

        String localDateTimetoUtc = DateTimeUtils.convertLocalDateLocalTimetoUtc(localDate, localTime);

        assertThat(localDateTimetoUtc).isEqualTo("2018-12-01T10:15:00.000Z");
    }

    @Test
    void shouldConvertAndReturnBstLocalDateTimeToUtc() {

        String localDateTimeStr = "2018-05-01T00:15:00";

        LocalDateTime localDateTime = LocalDateTime.parse(localDateTimeStr);
        String localDateTimetoUtc = DateTimeUtils.convertLocalDateTimetoUtc(localDateTime);

        assertThat(localDateTimetoUtc).isEqualTo("2018-04-30T23:15:00.000Z");
    }

    @Test
    void shouldConvertAndReturnGmtLocalDateTimeToUtc() {

        String localDateTimeStr = "2018-12-01T10:15:00";

        LocalDateTime localDateTime = LocalDateTime.parse(localDateTimeStr);
        String localDateTimetoUtc = DateTimeUtils.convertLocalDateTimetoUtc(localDateTime);

        assertThat(localDateTimetoUtc).isEqualTo("2018-12-01T10:15:00.000Z");
    }

    @Test
    void shouldReturnTrueForTodayTimeInPast() {
        assertThat(DateTimeUtils.isDateInThePast(LocalDateTime.now().minusMinutes(1))).isTrue();
    }

    @Test
    void shouldReturnFalseForTodayTime() {
        assertThat(DateTimeUtils.isDateInThePast(LocalDateTime.now().plusMinutes(1))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2018-12-02", "2020-05-14"})
    void validGetLocalDateWillReturnAnOption(String dateString) {
        assertThat(DateTimeUtils.getLocalDate(dateString)).isEqualTo(of(LocalDate.parse(dateString)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void nullOrEmptyGetLocalDateWillReturnAnEmptyOption(String dateString) {
        assertThat(DateTimeUtils.getLocalDate(dateString)).isEqualTo(empty());
    }

    @Test
    void isTrueWhenIsDateIsInPast() {
        assertThat(DateTimeUtils.isDateInThePast("2018-12-02")).isTrue();
        assertThat(DateTimeUtils.isDateInThePast(LocalDate.parse("2018-12-02"))).isTrue();
    }

    @Test
    void isFalseWhenIsDateIsNotInPast() {
        assertThat(DateTimeUtils.isDateInThePast("5018-12-02")).isFalse();
        assertThat(DateTimeUtils.isDateInThePast(LocalDate.parse("5018-12-02"))).isFalse();
        assertThat(DateTimeUtils.isDateInThePast(LocalDate.now())).isFalse();
        assertThat(DateTimeUtils.isDateInThePast(LocalDate.now().toString())).isFalse();
    }

    @Test
    void isTrueWhenIsDateIsInTheFuture() {
        assertThat(DateTimeUtils.isDateInTheFuture("5018-12-02")).isTrue();
        assertThat(DateTimeUtils.isDateInTheFuture(LocalDate.parse("5018-12-02"))).isTrue();
    }

    @Test
    void isFalseWhenIsDateIsNotInTheFuture() {
        assertThat(DateTimeUtils.isDateInTheFuture("2017-12-02")).isFalse();
        assertThat(DateTimeUtils.isDateInTheFuture(LocalDate.parse("2017-12-02"))).isFalse();
        assertThat(DateTimeUtils.isDateInTheFuture(LocalDate.now())).isFalse();
        assertThat(DateTimeUtils.isDateInTheFuture(LocalDate.now().toString())).isFalse();
    }


    @Test
    void shouldReturnCurrentUkLocalDateTime() {
        LocalDateTime ukLocalDateTime = getLocalDateTime();
        ZoneId ukZone = ZoneId.of("Europe/London");
        ZonedDateTime zonedDateTimeNow = ZonedDateTime.now(ukZone);

        assertThat(ukLocalDateTime.getDayOfYear()).isEqualTo(zonedDateTimeNow.getDayOfYear());
        assertThat(ukLocalDateTime.getHour()).isEqualTo(zonedDateTimeNow.getHour());
        assertThat(ukLocalDateTime.getMinute()).isEqualTo(zonedDateTimeNow.getMinute());
    }

}