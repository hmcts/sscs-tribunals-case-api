package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.model.BankHolidayDivision.SCOTLAND;
import static uk.gov.hmcts.reform.sscs.model.BankHolidayDivision.UK;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import net.objectlab.kit.datecalc.common.HolidayCalendar;
import net.objectlab.kit.datecalc.jdk8.LocalDateKitCalculatorsFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.client.CachedHolidayClient;
import uk.gov.hmcts.reform.sscs.model.BankHolidayDivision;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;

class BusinessDaysCalculatorServiceTest {

    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    @Mock
    private CachedHolidayClient cachedHolidayClient;

    private BusinessDaysCalculatorService businessDaysCalculatorService;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        unregisterRegionalCalendars();
        when(cachedHolidayClient.getHolidays(UK)).thenReturn(Set.of(
            LocalDate.of(2024, 12, 25),
            LocalDate.of(2024, 12, 26),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 4, 6)
        ));
        when(cachedHolidayClient.getHolidays(SCOTLAND)).thenReturn(Set.of(
            LocalDate.of(2024, 12, 25),
            LocalDate.of(2024, 12, 26),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 2),
            LocalDate.of(2026, 11, 30)
        ));

        businessDaysCalculatorService = new BusinessDaysCalculatorService(cachedHolidayClient);
    }

    @AfterEach
    void tearDown() {
        unregisterRegionalCalendars();
    }

    private static void unregisterRegionalCalendars() {
        for (final BankHolidayDivision division : BankHolidayDivision.values()) {
            LocalDateKitCalculatorsFactory.getDefaultInstance().unregisterHolidayCalendar(division.name());
        }
    }

    @Test
    void shouldReturnCorrectBusinessDayWithZonedDateTime() throws IOException {
        final ZonedDateTime startDateTime = ZonedDateTime.of(2024, 12, 22, 9, 30, 0, 0, LONDON);

        final ZonedDateTime result = businessDaysCalculatorService.getBusinessDay(startDateTime, 3);

        assertThat(result).isEqualTo(ZonedDateTime.of(2024, 12, 30, 9, 30, 0, 0, LONDON));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "1 day from Mon 23 Dec 2024 is Tue 24 Dec, 1, 2024-12-24",
        "2 days skips Christmas Day and Boxing Day to Fri 27 Dec, 2, 2024-12-27",
        "3 days skips Christmas and the weekend to Mon 30 Dec, 3, 2024-12-30",
        "4 days lands on Tue 31 Dec, 4, 2024-12-31",
        "5 days lands on Wed 1 Jan 2025, 5, 2025-01-01"
    })
    void shouldReturnCorrectBusinessDayWithLocalDate(final String description, final int numberOfBusinessDays,
                                                     final LocalDate expected) throws IOException {
        final LocalDate result = businessDaysCalculatorService.getBusinessDay(LocalDate.of(2024, 12, 23), numberOfBusinessDays);

        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "Skips New Year's Day but not the Scottish 2 Jan holiday, 2025-12-31, 2026-01-02",
        "Skips the weekend and Easter Monday, 2026-04-03, 2026-04-07",
        "Does not skip the Scottish St Andrew's Day holiday, 2026-11-27, 2026-11-30"
    })
    void shouldUseEnglandAndWalesHolidaysForBusinessDay(final String description, final LocalDate startDate,
                                                        final LocalDate expected) throws IOException {
        final LocalDate result = businessDaysCalculatorService.getBusinessDay(startDate, 1);

        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "Wednesday moves to Thursday, 2026-11-18, UK, 2026-11-19",
        "Friday skips the weekend to Monday, 2026-11-20, UK, 2026-11-23",
        "Saturday start gives Monday, 2026-11-21, UK, 2026-11-23",
        "Sunday start gives Monday, 2026-11-22, UK, 2026-11-23",
        "England and Wales has no St Andrew's Day holiday so Friday gives Mon 30 Nov, 2026-11-27, UK, 2026-11-30",
        "Scotland Friday skips the weekend and St Andrew's Day to Tue 1 Dec, 2026-11-27, SCOTLAND, 2026-12-01",
        "Scotland Saturday start skips St Andrew's Day to Tue 1 Dec, 2026-11-28, SCOTLAND, 2026-12-01",
        "Scotland start on St Andrew's Day gives the following day, 2026-11-30, SCOTLAND, 2026-12-01",
        "England and Wales skips New Year's Day to Fri 2 Jan, 2025-12-31, UK, 2026-01-02",
        "Scotland skips 1 and 2 Jan and the weekend to Mon 5 Jan, 2025-12-31, SCOTLAND, 2026-01-05",
        "Scotland start on New Year's Day skips 2 Jan and the weekend to Mon 5 Jan, 2026-01-01, SCOTLAND, 2026-01-05"
    })
    void shouldReturnNextBusinessDayAfterDate(final String description, final LocalDate date,
                                              final BankHolidayDivision bankHolidayDivision,
                                              final LocalDate expected) throws IOException {
        final LocalDate result = businessDaysCalculatorService.getNextBusinessDay(date, bankHolidayDivision);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldReturnSameNextBusinessDayAsNextWeekdayWhenNoHolidaysApply() throws IOException {
        final LocalDate saturday = LocalDate.of(2026, 11, 21);

        final LocalDate result = businessDaysCalculatorService.getNextBusinessDay(saturday, UK);

        assertThat(result).isEqualTo(DateTimeUtils.nextWeekday(saturday));
    }

    @Test
    void shouldRegisterHolidayCalendarForEachRegion() throws IOException {
        businessDaysCalculatorService.getBusinessDay(LocalDate.of(2026, 1, 1), 1);

        final HolidayCalendar<LocalDate> englandWales =
            LocalDateKitCalculatorsFactory.getDefaultInstance().getHolidayCalendar(UK.name());
        final HolidayCalendar<LocalDate> scotland =
            LocalDateKitCalculatorsFactory.getDefaultInstance().getHolidayCalendar(SCOTLAND.name());

        assertThat(englandWales.getHolidays()).isNotEmpty().doesNotContain(LocalDate.of(2026, 1, 2));
        assertThat(scotland.getHolidays()).contains(LocalDate.of(2026, 1, 2));
    }

    @Test
    void shouldFetchHolidaysForEachRegionWhenCalculatingWithLocalDate() throws IOException {
        businessDaysCalculatorService.getBusinessDay(LocalDate.of(2026, 1, 1), 1);

        verify(cachedHolidayClient).getHolidays(UK);
        verify(cachedHolidayClient).getHolidays(SCOTLAND);
    }

    @Test
    void shouldFetchHolidaysForEachRegionWhenCalculatingWithZonedDateTime() throws IOException {
        businessDaysCalculatorService.getBusinessDay(ZonedDateTime.of(2026, 1, 1, 9, 0, 0, 0, LONDON), 1);

        verify(cachedHolidayClient).getHolidays(UK);
        verify(cachedHolidayClient).getHolidays(SCOTLAND);
    }
}
