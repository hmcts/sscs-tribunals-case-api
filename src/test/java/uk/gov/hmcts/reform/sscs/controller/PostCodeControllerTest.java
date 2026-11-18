package uk.gov.hmcts.reform.sscs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getMaskedPostcode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;

@ExtendWith(MockitoExtension.class)
public class PostCodeControllerTest {

    private static final String POSTCODE = "CM120NS";

    @Mock
    private AirLookupService airLookupService;

    @InjectMocks
    private PostCodeController controller;

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(PostCodeController.class);

    @BeforeEach
    public void setUp() {
        ((Logger) LoggerFactory.getLogger(PostCodeController.class)).setLevel(Level.DEBUG);
    }

    @Test
    void shouldMaskPostcodeInLogsWhenRegionalCentreFound() {
        when(airLookupService.lookupRegionalCentre(POSTCODE)).thenReturn("Birmingham");

        ResponseEntity<String> response = controller.getRegionalCentre(POSTCODE);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        logCapture.assertLogContains("Found regional centre Birmingham for post code " + getMaskedPostcode(POSTCODE), Level.DEBUG)
            .assertLogDoesNotContain(POSTCODE, Level.DEBUG);
    }

    @Test
    void shouldMaskPostcodeInLogsWhenRegionalCentreNotFound() {
        when(airLookupService.lookupRegionalCentre(POSTCODE)).thenReturn(null);

        ResponseEntity<String> response = controller.getRegionalCentre(POSTCODE);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        logCapture.assertLogContains("Could not find postcode " + getMaskedPostcode(POSTCODE), Level.WARN)
            .assertLogDoesNotContain(POSTCODE, Level.WARN);
    }
}
