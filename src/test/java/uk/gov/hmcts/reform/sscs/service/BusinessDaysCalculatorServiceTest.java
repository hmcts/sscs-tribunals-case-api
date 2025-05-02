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
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BusinessDaysCalculatorServiceTest {

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private Call mockCall;

    @Mock
    private Response response;

    @Mock
    private ResponseBody responseBody;

    private BusinessDaysCalculatorService businessDaysCalculatorService;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        when(httpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("{\"england-and-wales\":{\"events\":[{\"date\":\"2024-12-25\"},{\"date\":\"2024-12-26\"}]}}");

        businessDaysCalculatorService = new BusinessDaysCalculatorService(httpClient);
    }

    @Test
    void shouldReturnCorrectBusinessDayWithZonedDateTime() {
        ZonedDateTime startDateTime = ZonedDateTime.of(2024, 12, 22, 9, 30, 0, 0, ZoneId.of("Europe/London"));
        ZonedDateTime result = businessDaysCalculatorService.getBusinessDay(startDateTime, 3);

        assertEquals(ZonedDateTime.of(2024, 12, 30, 9, 30, 0, 0, ZoneId.of("Europe/London")), result);
    }

    @Test
    void shouldReturnCorrectBusinessDayWithLocalDate() {
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
    void shouldHandleUnsuccessfulApiResponse() {
        when(response.isSuccessful()).thenReturn(false);

        IOException exception = assertThrows(IOException.class, () -> new BusinessDaysCalculatorService(httpClient));
        assertEquals("Response unsuccessful: " + response, exception.getMessage());
    }

    @Test
    void shouldHandleFailedCall() throws IOException {
        when(mockCall.execute()).thenThrow(new IOException("Network error"));

        IOException exception = assertThrows(IOException.class, () -> new BusinessDaysCalculatorService(httpClient));
        assertEquals("Network error", exception.getMessage());
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