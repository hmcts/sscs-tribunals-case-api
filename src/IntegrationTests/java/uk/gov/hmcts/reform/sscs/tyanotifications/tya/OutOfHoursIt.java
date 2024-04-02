package uk.gov.hmcts.reform.sscs.tyanotifications.tya;

import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.helper.IntegrationTestHelper.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationTestRecipients;
import uk.gov.hmcts.reform.sscs.tyanotifications.controller.NotificationController;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.tyanotifications.helper.IntegrationTestHelper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.docmosis.PdfLetterService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder.JobGroupGenerator;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("tya-integration")
@AutoConfigureMockMvc
public class OutOfHoursIt {

    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    MockMvc mockMvc;

    NotificationController controller;

    @Mock
    NotificationClient notificationClient;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private SendSmsResponse sendSmsResponse;


    @Mock
    ReminderService reminderService;

    @Autowired
    NotificationValidService notificationValidService;

    @MockBean
    private AuthorisationService authorisationService;

    @Mock
    NotificationTestRecipients notificationTestRecipients;

    @Autowired
    NotificationFactory factory;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private IdamService idamService;

    String json;

    @Autowired
    private NotificationHandler notificationHandler;

    @MockBean
    private OutOfHoursCalculator outOfHoursCalculator;

    @Autowired
    private NotificationConfig notificationConfig;

    @Autowired
    private PdfStoreService pdfStoreService;

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private JobGroupGenerator jobGroupGenerator;

    @Autowired
    private PdfLetterService pdfLetterService;

    @Autowired
    @Qualifier("scheduler")
    private Scheduler quartzScheduler;

    @Mock
    private MarkdownTransformationService markdownTransformationService;

    @Mock
    private SaveCorrespondenceAsyncService saveCorrespondenceAsyncService;

    @Before
    public void setup() throws Exception {
        outOfHoursCalculator = mock(OutOfHoursCalculator.class);
        LocalDateTime dateBefore = LocalDateTime.now();
        ZonedDateTime zoned = ZonedDateTime.ofLocal(dateBefore, ZoneId.of(AppConstants.ZONE_ID), null);
        when(outOfHoursCalculator.getStartOfNextInHoursPeriod()).thenReturn(zoned);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(true);

        notificationHandler = new NotificationHandler(outOfHoursCalculator, jobScheduler, jobGroupGenerator);

        NotificationSender sender = new NotificationSender(notificationClient, null, notificationTestRecipients, markdownTransformationService, saveCorrespondenceAsyncService, false);
        SendNotificationService sendNotificationService = new SendNotificationService(sender, notificationHandler, notificationValidService, pdfLetterService, pdfStoreService);
        NotificationService service = new NotificationService(factory, reminderService, notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService, false);
        controller = new NotificationController(service, authorisationService, ccdService, deserializer, idamService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        when(notificationClient.sendEmail(any(), any(), any(), any()))
            .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        when(notificationClient.sendSms(any(), any(), any(), any(), any()))
            .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

    }

    @Test
    public void scheduleOutOfHoursNotificationWithAnAppellantSubscription() throws Exception {
        try {
            quartzScheduler.clear();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Job scheduler is empty at start", 0);

        json = updateEmbeddedJson(json, null, "case_details", "case_data", "subscriptions",
            "representativeSubscription");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Appeal received scheduled", "appealReceived", 1);
    }

    @Test
    public void scheduleOutOfHoursNotificationWithAnAppellantAndRepresentativeSubscription() throws Exception {
        try {
            quartzScheduler.clear();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Job scheduler is empty at start", 0);

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Appeal received scheduled", "appealReceived", 1);
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
