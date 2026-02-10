package uk.gov.hmcts.reform.sscs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar;
import net.objectlab.kit.datecalc.jdk8.LocalDateKitCalculatorsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BusinessDaysCalculatorServiceTest {

    @Mock
    private CachedHolidayClient cachedHolidayClient;

    private BusinessDaysCalculatorService businessDaysCalculatorService;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        Set<LocalDate> holidays = new HashSet<>();
        holidays.add(LocalDate.of(2024, 12, 25));
        holidays.add(LocalDate.of(2024, 12, 26));
        when(cachedHolidayClient.getHolidays()).thenReturn(holidays);

        businessDaysCalculatorService = new BusinessDaysCalculatorService(cachedHolidayClient);
    }

    @Test
    void shouldReturnCorrectBusinessDayWithZonedDateTime() throws IOException {
        ZonedDateTime startDateTime = ZonedDateTime.of(2024, 12, 22, 9, 30, 0, 0, ZoneId.of("Europe/London"));
        ZonedDateTime result = businessDaysCalculatorService.getBusinessDay(startDateTime, 3);

        assertEquals(ZonedDateTime.of(2024, 12, 30, 9, 30, 0, 0, ZoneId.of("Europe/London")), result);
    }

    @Test
    void shouldReturnCorrectBusinessDayWithLocalDate() throws IOException {
        LocalDate startDate = LocalDate.of(2024, 12, 23);

        assertEquals(LocalDate.of(2024, 12, 24),
            businessDaysCalculatorService.getBusinessDay(startDate, 1));
        assertEquals(LocalDate.of(2024, 12, 27),
            businessDaysCalculatorService.getBusinessDay(startDate, 2));
        assertEquals(LocalDate.of(2024, 12, 30),
            businessDaysCalculatorService.getBusinessDay(startDate, 3));
        assertEquals(LocalDate.of(2024, 12, 31),
            businessDaysCalculatorService.getBusinessDay(startDate, 4));
        assertEquals(LocalDate.of(2025, 1, 1),
            businessDaysCalculatorService.getBusinessDay(startDate, 5));
    }

    @Test
    void shouldInitializeHolidaysCorrectly() {
        Set<LocalDate> holidays = new HashSet<>();
        holidays.add(LocalDate.of(2023, 12, 25));
        holidays.add(LocalDate.of(2023, 12, 26));

        DefaultHolidayCalendar<LocalDate> ukCalendar = new DefaultHolidayCalendar<>();
        ukCalendar.setHolidays(holidays);

        LocalDateKitCalculatorsFactory.getDefaultInstance().registerHolidays("UK", ukCalendar);

        assertNotNull(LocalDateKitCalculatorsFactory.getDefaultInstance().getHolidayCalendar("UK"));
    }
}
