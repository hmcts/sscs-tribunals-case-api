package uk.gov.hmcts.sscs.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.MockitoAnnotations.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.reminder.ReminderResponse;
import uk.gov.hmcts.sscs.service.CcdService;

public class ReminderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CcdService ccdService;

    private ReminderController controller;

    @Before
    public void setUp() {
        initMocks(this);
        controller = new ReminderController(ccdService);
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedReminder() throws Exception {

        String json = "{\n"
                + "  \"caseId\": \"12345\",\n"
                + "  \"eventId\": \"hearingReminderNotification\"\n"
                + "}";

        mockMvc.perform(post("/reminder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test(expected = CcdException.class)
    public void shouldHandleCcdExceptionWhenAMalformedReminderIsReceived() {
        ReminderResponse reminderResponse = new ReminderResponse("12345","hearingReminderNotification");

        doThrow(new CcdException(new Exception("Malformed Reminder Response"))).when(ccdService).updateCase(
            null, Long.valueOf(reminderResponse.getCaseId()), reminderResponse.getEventId());

        controller.reminder(reminderResponse);
    }
}
