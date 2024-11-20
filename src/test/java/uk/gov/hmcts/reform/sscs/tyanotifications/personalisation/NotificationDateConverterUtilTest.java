package uk.gov.hmcts.reform.sscs.tyanotifications.personalisation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class NotificationDateConverterUtilTest {
    @Test
    public void doesNotAdd0ToStartOfDateFromString() {
        String emailDate = new NotificationDateConverterUtil().toEmailDate("2018-09-01T23:59:59Z");
        assertThat(emailDate, is("1 September 2018"));
    }

    @Test
    public void dateInMonthIs2DigitsFromString() {
        String emailDate = new NotificationDateConverterUtil().toEmailDate("2018-09-12T23:59:59Z");
        assertThat(emailDate, is("12 September 2018"));
    }

    @Test
    public void doesNotAdd0ToStartOfDateFromLocalDate() {
        String emailDate = new NotificationDateConverterUtil().toEmailDate(LocalDate.of(2018, 9, 1));
        assertThat(emailDate, is("1 September 2018"));
    }

    @Test
    public void dateInMonthIs2DigitsFromLocalDate() {
        String emailDate = new NotificationDateConverterUtil().toEmailDate(LocalDate.of(2018, 9, 12));
        assertThat(emailDate, is("12 September 2018"));
    }


}
