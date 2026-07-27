package uk.gov.hmcts.reform.sscs.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CachedHolidayClientTest {

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private Call mockCall;

    @Mock
    private Response response;

    @Mock
    private ResponseBody responseBody;

    private CachedHolidayClient cachedHolidayClient;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        when(httpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("{\"england-and-wales\":{\"events\":[{\"date\":\"2024-12-25\"},{\"date\":\"2024-12-26\"}]}}");

        cachedHolidayClient = new CachedHolidayClient(httpClient);
    }

    @Test
    void shouldFetchHolidaysFromApi() throws IOException {
        Set<LocalDate> holidays = cachedHolidayClient.getHolidays();

        assertNotNull(holidays);
        assertEquals(2, holidays.size());
        assertTrue(holidays.contains(LocalDate.of(2024, 12, 25)));
        assertTrue(holidays.contains(LocalDate.of(2024, 12, 26)));

        // Verify API call was made
        verify(mockCall, times(1)).execute();
    }

    @Test
    void shouldReturnCachedHolidaysOnSubsequentCalls() throws IOException {
        Set<LocalDate> holidaysFirstCall = cachedHolidayClient.getHolidays();
        Set<LocalDate> holidaysSecondCall = cachedHolidayClient.getHolidays();

        assertSame(holidaysFirstCall, holidaysSecondCall);

        // Verify API call was made only once
        verify(mockCall, times(1)).execute();
    }

    @Test
    void shouldThrowIoExceptionForUnsuccessfulResponse() throws IOException {
        when(response.isSuccessful()).thenReturn(false);

        IOException exception = assertThrows(IOException.class, () -> cachedHolidayClient.getHolidays());
        assertEquals("Response unsuccessful: " + response, exception.getMessage());
    }

    @Test
    void shouldThrowIoExceptionForFailedApiCall() throws IOException {
        when(mockCall.execute()).thenThrow(new IOException("Network error"));

        IOException exception = assertThrows(IOException.class, () -> cachedHolidayClient.getHolidays());
        assertEquals("Network error", exception.getMessage());
    }
}