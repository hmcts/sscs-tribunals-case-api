package uk.gov.hmcts.reform.sscs.tyanotifications.tya;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.tyanotifications.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.tyanotifications.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.controller.NotificationController;
import uk.gov.hmcts.reform.sscs.tyanotifications.helper.IntegrationTestHelper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.OutOfHoursCalculator;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.docmosis.PdfLetterService;
import uk.gov.service.notify.*;

@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class HearingReminderIt {

    MockMvc mockMvc;

    NotificationController controller;

    @Autowired
    NotificationService notificationService;

    @MockBean
    private AuthorisationService authorisationService;

    @MockBean(name = "notificationClient")
    NotificationClient client;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private SendSmsResponse sendSmsResponse;

    @Mock
    private SendLetterResponse sendLetterResponse;

    @MockBean
    private JobExecutor<String> jobExecutor;

    @MockBean
    private OutOfHoursCalculator outOfHoursCalculator;

    @MockBean
    private PdfLetterService pdfLetterService;

    @Autowired
    @Qualifier("scheduler")
    private Scheduler quartzScheduler;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private IdamService idamService;

    @BeforeEach
    public void setup() throws NotificationClientException {
        controller = new NotificationController(notificationService, authorisationService, ccdService, deserializer, idamService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(client.sendEmail(any(), any(), any(), any()))
            .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        when(client.sendSms(any(), any(), any(), any(), any()))
            .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        when(client.sendLetter(any(), any(), any()))
            .thenReturn(sendLetterResponse);
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());


        when(pdfLetterService.generateLetter(any(), any(), any()))
            .thenReturn(new byte[0]);
        when(pdfLetterService.buildCoversheet(any(), any()))
            .thenReturn(new byte[0]);


        outOfHoursCalculator = mock(OutOfHoursCalculator.class);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);
    }

    @Test
    public void shouldScheduleHearingReminderThenRemoveWhenPostponed() throws Exception {

        ReflectionTestUtils.setField(notificationService, "covid19Feature", false);

        try {
            quartzScheduler.clear();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Job scheduler is empty at start", 0);

        sendEvent("hearingBooked");

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Hearing reminders scheduled", "hearingReminder", 2);

        IntegrationTestHelper.assertScheduledJobTriggerAt(
            quartzScheduler,
            "First hearing reminder scheduled",
            "hearingReminder",
            "2048-01-05T11:00:00Z"
        );

        IntegrationTestHelper.assertScheduledJobTriggerAt(
            quartzScheduler,
            "Second hearing reminder scheduled",
            "hearingReminder",
            "2048-01-11T11:00:00Z"
        );

        sendEvent("hearingPostponed");

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Hearing reminders were removed", "hearingReminder", 0);
    }

    private void sendEvent(String event) throws Exception {

        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        String ccdResponseJson = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        ccdResponseJson = ccdResponseJson.replace("appealReceived", event);
        ccdResponseJson = ccdResponseJson.replace("\"hearingDate\": \"2018-01-12\"", "\"hearingDate\": \"2048-01-12\"");

        HttpServletResponse sendResponse = getResponse(getRequestWithAuthHeader(ccdResponseJson));
        assertHttpStatus(sendResponse, HttpStatus.OK);
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
