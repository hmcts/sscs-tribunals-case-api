package uk.gov.hmcts.sscs.controller;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.hmcts.sscs.domain.reminder.ReminderResponse;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.service.CcdService;

@RunWith(MockitoJUnitRunner.class)
public class ReminderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CcdService ccdService;

    private ReminderController controller;

    private ReminderResponse reminderResponse;

    @Before
    public void setUp() {
        controller = new ReminderController(ccdService);
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    @Ignore
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
    public void shouldHandleExceptionForGetRequest() throws Exception {
        reminderResponse = new ReminderResponse("12345", "hearingReminderNotification");

        doThrow(new CcdException("Malformed Reminder Response ")).when(ccdService).createEvent(reminderResponse);

        controller.reminder(reminderResponse);
    }
}
