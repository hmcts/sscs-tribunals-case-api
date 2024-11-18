package uk.gov.hmcts.reform.sscs.tyanotifications.tya;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationTestRecipients;
import uk.gov.hmcts.reform.sscs.tyanotifications.controller.NotificationController;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.docmosis.PdfLetterService;
import uk.gov.service.notify.*;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class NotificationsItBase {
    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    protected MockMvc mockMvc;

    @Mock
    public NotificationClient notificationClient;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private SendSmsResponse sendSmsResponse;

    @Mock
    private SendLetterResponse sendLetterResponse;

    @Mock
    private ReminderService reminderService;

    @MockBean
    protected AuthorisationService authorisationService;

    @Mock
    private NotificationTestRecipients notificationTestRecipients;

    @Autowired
    private NotificationValidService notificationValidService;

    @Autowired
    private NotificationFactory factory;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private IdamService idamService;

    protected String json;

    @Autowired
    private NotificationHandler notificationHandler;

    @MockBean
    private OutOfHoursCalculator outOfHoursCalculator;

    @Autowired
    private NotificationConfig notificationConfig;

    @Autowired
    private PdfLetterService pdfLetterService;

    @Autowired
    private DocmosisPdfService docmosisPdfService;

    @MockBean
    private DocmosisPdfGenerationService docmosisPdfGenerationService;

    @Mock
    protected PdfStoreService pdfStoreService;

    @Value("${notification.english.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailId;

    @Value("${notification.english.subscriptionCreated.appellant.smsId}")
    private String subscriptionCreatedSmsId;

    private final Boolean saveCorrespondence = false;

    @Mock
    private MarkdownTransformationService markdownTransformationService;

    @Mock
    private SaveCorrespondenceAsyncService saveCorrespondenceAsyncService;

    @Autowired
    @Qualifier("scheduler")
    protected Scheduler quartzScheduler;

    protected NotificationService service;

    @Before
    public void setup() throws Exception {
        NotificationSender sender = new NotificationSender(notificationClient, null, notificationTestRecipients, markdownTransformationService, saveCorrespondenceAsyncService, saveCorrespondence);

        SendNotificationService sendNotificationService = new SendNotificationService(sender, notificationHandler, notificationValidService, pdfLetterService, pdfStoreService);

        setupNotificationService(sendNotificationService);

        NotificationController controller = new NotificationController(service, authorisationService, ccdService, deserializer, idamService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        when(notificationClient.sendEmail(any(), any(), any(), any()))
            .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        when(notificationClient.sendSms(any(), any(), any(), any(), any()))
            .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        when(notificationClient.sendLetter(any(), any(), any()))
            .thenReturn(sendLetterResponse);
        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any()))
            .thenReturn(sendLetterResponse);
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        outOfHoursCalculator = mock(OutOfHoursCalculator.class);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);

        byte[] pdfbytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
        when(docmosisPdfGenerationService.generatePdf(any())).thenReturn(pdfbytes);
    }

    private void setupNotificationService(SendNotificationService sendNotificationService) {
        service = new NotificationService(factory, reminderService, notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService, false);
    }

    protected NotificationService getNotificationService() {
        return service;
    }

    protected void setupReminderController(NotificationService service) {
        ReminderTestController reminderTestController = new ReminderTestController(service, authorisationService, ccdService, deserializer, idamService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(reminderTestController).build();

    }

    protected void validateEmailNotifications(List<String> expectedEmailTemplateIds,
                                              int wantedNumberOfSendEmailInvocations, String expectedName)
        throws NotificationClientException {

        ArgumentCaptor<String> emailTemplateIdCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> emailPersonalisationCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient, times(wantedNumberOfSendEmailInvocations))
            .sendEmail(emailTemplateIdCaptor.capture(), any(), emailPersonalisationCaptor.capture(), any());
        assertArrayEquals(expectedEmailTemplateIds.toArray(), emailTemplateIdCaptor.getAllValues().toArray());

        if (0 < wantedNumberOfSendEmailInvocations) {
            Map<String, ?> personalisation = emailPersonalisationCaptor.getValue();
            if (null != personalisation.get(REPRESENTATIVE_NAME)) {
                assertEquals(expectedName, personalisation.get(REPRESENTATIVE_NAME));
            } else {
                assertEquals(expectedName, personalisation.get(NAME));
            }
            assertEquals("Dexter Vasquez", personalisation.get(APPELLANT_NAME));
        }
    }

    protected void validateSmsNotifications(List<String> expectedSmsTemplateIds, int wantedNumberOfSendSmsInvocations) throws NotificationClientException {
        ArgumentCaptor<String> smsTemplateIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationClient, times(wantedNumberOfSendSmsInvocations))
            .sendSms(smsTemplateIdCaptor.capture(), any(), any(), any(), any());
        assertArrayEquals(expectedSmsTemplateIds.toArray(), smsTemplateIdCaptor.getAllValues().toArray());
    }

    protected void validateLetterNotifications(List<String> expectedLetterTemplateIds, int wantedNumberOfSendLetterInvocations, String expectedName) throws NotificationClientException {
        ArgumentCaptor<Map<String, Object>> letterPersonalisationCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> letterTemplateIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationClient, atMost(wantedNumberOfSendLetterInvocations))
            .sendLetter(letterTemplateIdCaptor.capture(), letterPersonalisationCaptor.capture(), any());
        assertArrayEquals(expectedLetterTemplateIds.stream().filter(f -> !(f.endsWith(".doc") || f.endsWith(".docx"))).toArray(), letterTemplateIdCaptor.getAllValues().toArray());

        int expectedDocmosisLetters = expectedLetterTemplateIds.stream().filter(f -> f.endsWith(".doc") || f.endsWith(".docx")).toArray().length;
        if (expectedDocmosisLetters > 0) {
            verify(notificationClient, times(expectedDocmosisLetters)).sendPrecompiledLetterWithInputStream(any(), any());
        } else {
            verify(notificationClient, times(0)).sendPrecompiledLetterWithInputStream(any(), any());
        }

        if (0 < wantedNumberOfSendLetterInvocations) {
            Map<String, Object> personalisation = letterPersonalisationCaptor.getValue();
            if (null != personalisation.get(REPRESENTATIVE_NAME)) {
                assertEquals(expectedName, personalisation.get(REPRESENTATIVE_NAME));
            } else {
                assertEquals(expectedName, personalisation.get(NAME));
            }
            assertEquals("Dexter Vasquez", personalisation.get(APPELLANT_NAME));
        }
    }


    public MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
