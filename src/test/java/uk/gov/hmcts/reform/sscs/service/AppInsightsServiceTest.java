package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.model.HmcFailureMessage;
import uk.gov.hmcts.reform.sscs.model.Message;

@ExtendWith(MockitoExtension.class)
public class AppInsightsServiceTest {

    private static final String REQUEST_TYPE = "request";
    private static final Long CASE_ID = 1000000000L;
    private static final LocalDateTime TIME_STAMP = LocalDateTime.now();
    private static final String ERROR_CODE = "error code";
    private static final String ERROR_MESSAGE = "error message";

    private Message message;

    @BeforeEach
    public void setUp() {
        message = messageInit();
    }

    @Test
    public void testAppInsightsServiceDoesNotThrowJpe() {
        AppInsightsService appInsightsService = new AppInsightsService(new ObjectMapper());

        assertThatNoException().isThrownBy(
            () -> appInsightsService.sendAppInsightsEvent(message)
        );
    }

    @Test
    public void test() throws JsonProcessingException {
        ObjectMapper om = Mockito.spy(new ObjectMapper());
        om.findAndRegisterModules();

        AppInsightsService appInsightsService = new AppInsightsService(om);

        Mockito.when(om.writeValueAsString(message)).thenThrow(JsonProcessingException.class);

        assertThatExceptionOfType(JsonProcessingException.class).isThrownBy(
            () -> appInsightsService.sendAppInsightsEvent(message)
        );
    }

    private Message messageInit() {
        return new HmcFailureMessage(REQUEST_TYPE, CASE_ID, TIME_STAMP, ERROR_CODE, ERROR_MESSAGE);
    }
}
