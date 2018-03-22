package uk.gov.hmcts.sscs.util;

import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import org.junit.Test;

public class DateTimeUtilsTest {

    @Test
    public void shouldConvertAndReturnBstLocalTimetToUtc() {

        String localDate = "2018-05-01";
        String localTime = "03:15:00";

        String localDateTimetoUtc = DateTimeUtils.convertLocalDateTimetoUtc(localDate, localTime);

        assertThat(localDateTimetoUtc, Matchers.equalTo("2018-05-01T02:15:00.000Z"));
    }

    @Test
    public void shouldConvertAndReturnGmtLocalTimetToUtc() {

        String localDate = "2018-12-01";
        String localTime = "10:15:00";

        String localDateTimetoUtc = DateTimeUtils.convertLocalDateTimetoUtc(localDate, localTime);

        assertThat(localDateTimetoUtc, Matchers.equalTo("2018-12-01T10:15:00.000Z"));
    }
}