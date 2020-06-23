package uk.gov.hmcts.reform.sscs.util;

import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import org.hamcrest.Matchers;
import org.junit.Test;

public class DateTimeUtilsTest {

    @Test
    public void shouldConvertAndReturnBstLocalTimetToUtc() {

        String localDate = "2018-05-01";
        String localTime = "03:15:00";

        String localDateTimetoUtc = DateTimeUtils.convertLocalDateLocalTimetoUtc(localDate, localTime);

        assertThat(localDateTimetoUtc, Matchers.equalTo("2018-05-01T02:15:00.000Z"));
    }

    @Test
    public void shouldConvertAndReturnGmtLocalTimetToUtc() {

        String localDate = "2018-12-01";
        String localTime = "10:15:00";

        String localDateTimetoUtc = DateTimeUtils.convertLocalDateLocalTimetoUtc(localDate, localTime);

        assertThat(localDateTimetoUtc, Matchers.equalTo("2018-12-01T10:15:00.000Z"));
    }

    @Test
    public void shouldConvertAndReturnBstLocalDateTimeToUtc() {

        String localDateTimeStr = "2018-05-01T00:15:00";

        LocalDateTime localDateTime = LocalDateTime.parse(localDateTimeStr);
        String localDateTimetoUtc = DateTimeUtils.convertLocalDateTimetoUtc(localDateTime);

        assertThat(localDateTimetoUtc, Matchers.equalTo("2018-04-30T23:15:00.000Z"));
    }

    @Test
    public void shouldConvertAndReturnGmtLocalDateTimeToUtc() {

        String localDateTimeStr = "2018-12-01T10:15:00";

        LocalDateTime localDateTime = LocalDateTime.parse(localDateTimeStr);
        String localDateTimetoUtc = DateTimeUtils.convertLocalDateTimetoUtc(localDateTime);

        assertThat(localDateTimetoUtc, Matchers.equalTo("2018-12-01T10:15:00.000Z"));
    }
}