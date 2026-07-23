package uk.gov.hmcts.reform.sscs.util;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class DateTimeUtilsTest {

    @Test
    public void shouldConvertAndReturnBstLocalTimeToUtc() {

        String localDate = "2018-05-01";
        String localTime = "03:15:00";

        String localDateTimetoUtc = DateTimeUtils.convertLocalDateLocalTimetoUtc(localDate, localTime);

        assertThat(localDateTimetoUtc, is("2018-05-01T02:15:00.000Z"));
    }

    @Test
    public void shouldConvertAndReturnGmtLocalTimetToUtc() {

        String localDate = "2018-12-01";
        String localTime = "10:15:00";

        String localDateTimetoUtc = DateTimeUtils.convertLocalDateLocalTimetoUtc(localDate, localTime);

        assertThat(localDateTimetoUtc, is("2018-12-01T10:15:00.000Z"));
    }

    @Test
    public void shouldConvertAndReturnBstLocalDateTimeToUtc() {

        String localDateTimeStr = "2018-05-01T00:15:00";

        LocalDateTime localDateTime = LocalDateTime.parse(localDateTimeStr);
        String localDateTimetoUtc = DateTimeUtils.convertLocalDateTimetoUtc(localDateTime);

        assertThat(localDateTimetoUtc, is("2018-04-30T23:15:00.000Z"));
    }

    @Test
    public void shouldConvertAndReturnGmtLocalDateTimeToUtc() {

        String localDateTimeStr = "2018-12-01T10:15:00";

        LocalDateTime localDateTime = LocalDateTime.parse(localDateTimeStr);
        String localDateTimetoUtc = DateTimeUtils.convertLocalDateTimetoUtc(localDateTime);

        assertThat(localDateTimetoUtc, is("2018-12-01T10:15:00.000Z"));
    }

    @Test
    public void shouldReturnTrueForTodayTimeInPast() {
        assertTrue(DateTimeUtils.isDateInThePast(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    public void shouldReturnFalseForTodayTime() {
        assertFalse(DateTimeUtils.isDateInThePast(LocalDateTime.now().plusMinutes(1)));
    }

    @Test
    @Parameters({"2018-12-02", "2020-05-14"})
    public void validGetLocalDateWillReturnAnOption(String dateString) {
        assertThat(DateTimeUtils.getLocalDate(dateString), is(of(LocalDate.parse(dateString))));
    }

    @Test
    @Parameters({"null", "", " "})
    public void nullOrEmptyGetLocalDateWillReturnAnEmptyOption(@Nullable String dateString) {
        assertThat(DateTimeUtils.getLocalDate(dateString), is(empty()));
    }

    @Test
    public void isTrueWhenIsDateIsInPast() {
        assertThat(DateTimeUtils.isDateInThePast("2018-12-02"), is(true));
        assertThat(DateTimeUtils.isDateInThePast(LocalDate.parse("2018-12-02")), is(true));
    }

    @Test
    public void isFalseWhenIsDateIsNotInPast() {
        assertThat(DateTimeUtils.isDateInThePast("5018-12-02"), is(false));
        assertThat(DateTimeUtils.isDateInThePast(LocalDate.parse("5018-12-02")), is(false));
        assertThat(DateTimeUtils.isDateInThePast(LocalDate.now()), is(false));
        assertThat(DateTimeUtils.isDateInThePast(LocalDate.now().toString()), is(false));
    }

    @Test
    public void isTrueWhenIsDateIsInTheFuture() {
        assertThat(DateTimeUtils.isDateInTheFuture("5018-12-02"), is(true));
        assertThat(DateTimeUtils.isDateInTheFuture(LocalDate.parse("5018-12-02")), is(true));
    }

    @Test
    public void isFalseWhenIsDateIsNotInTheFuture() {
        assertThat(DateTimeUtils.isDateInTheFuture("2017-12-02"), is(false));
        assertThat(DateTimeUtils.isDateInTheFuture(LocalDate.parse("2017-12-02")), is(false));
        assertThat(DateTimeUtils.isDateInTheFuture(LocalDate.now()), is(false));
        assertThat(DateTimeUtils.isDateInTheFuture(LocalDate.now().toString()), is(false));
    }

}
