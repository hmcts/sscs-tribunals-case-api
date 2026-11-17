package uk.gov.hmcts.reform.sscs.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;
import uk.gov.hmcts.reform.sscs.model.BankHolidayDivision;

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
        when(responseBody.string()).thenReturn("""
            {
              "england-and-wales": {
                "division": "england-and-wales",
                "events": [
                  {
                    "title": "Christmas Day",
                    "date": "2024-12-25",
                    "notes": "",
                    "bunting": true
                  },
                  {
                    "title": "Boxing Day",
                    "date": "2024-12-26",
                    "notes": "",
                    "bunting": false
                  }
                ]
              },
              "scotland": {
                "division": "scotland",
                "events": [
                  {
                    "title": "St Andrews Day",
                    "date": "2024-11-30",
                    "notes": "",
                    "bunting": true
                  },
                  {
                    "title": "2nd January",
                    "date": "2024-01-02",
                    "notes": "",
                    "bunting": true
                  }
                ]
              }
            }""");

        cachedHolidayClient = new CachedHolidayClient(httpClient);
    }

    @Test
    void shouldFetchHolidaysFromApiForEnglandAndWales() throws IOException {
        final Set<LocalDate> holidays = cachedHolidayClient.getHolidays(BankHolidayDivision.UK);

        assertThat(holidays).containsExactlyInAnyOrder(LocalDate.of(2024, 12, 25), LocalDate.of(2024, 12, 26));

        verify(httpClient, once()).newCall(any());
    }

    @Test
    void shouldFetchHolidaysFromApiForScotland() throws IOException {
        final Set<LocalDate> holidays = cachedHolidayClient.getHolidays(BankHolidayDivision.SCOTLAND);

        assertThat(holidays).containsExactlyInAnyOrder(LocalDate.of(2024, 11, 30), LocalDate.of(2024, 1, 2));

        verify(httpClient, once()).newCall(any());
    }

    @ParameterizedTest
    @EnumSource(BankHolidayDivision.class)
    void shouldReturnCachedHolidaysOnSubsequentCalls(BankHolidayDivision bankHolidayDivision) throws IOException {
        final Set<LocalDate> holidaysFirstCall = cachedHolidayClient.getHolidays(bankHolidayDivision);
        final Set<LocalDate> holidaysSecondCall = cachedHolidayClient.getHolidays(bankHolidayDivision);

        assertThat(holidaysSecondCall).isSameAs(holidaysFirstCall);

        verify(httpClient, once()).newCall(any());
    }

    @Test
    void shouldThrowIoExceptionForUnsuccessfulResponse() {
        when(response.isSuccessful()).thenReturn(false);

        assertThatThrownBy(() -> cachedHolidayClient.getHolidays(BankHolidayDivision.UK))
            .isInstanceOf(IOException.class)
            .hasMessage("Response unsuccessful: " + response);
    }

    @Test
    void shouldThrowIoExceptionForFailedApiCall() throws IOException {
        when(mockCall.execute()).thenThrow(new IOException("Network error"));

        assertThatThrownBy(() -> cachedHolidayClient.getHolidays(BankHolidayDivision.UK))
            .isInstanceOf(IOException.class)
            .hasMessage("Network error");
    }

    private static VerificationMode once() {
        return times(1);
    }
}
