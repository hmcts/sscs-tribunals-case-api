package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public class BusinessDaysCalculatorServiceTest {

    @Test
    public void shouldReturnCorrectBusinessDay() {
        // given
        LocalDate hearingDate = LocalDate.of(2017,12,22);
        LocalTime hearingTime = LocalTime.of(9,30,00);

        // when
        ZonedDateTime decisionDateTime = BusinessDaysCalculatorService.getBusinessDay(
                ZonedDateTime.of(hearingDate, hearingTime, ZoneId.of("Europe/London")), 7);

        // then
        assertEquals(2018, decisionDateTime.getYear());
        assertEquals(01, decisionDateTime.getMonthValue());
        assertEquals(02, decisionDateTime.getDayOfMonth());
        assertEquals(hearingTime, decisionDateTime.toLocalTime());
    }
}
