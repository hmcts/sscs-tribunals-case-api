package uk.gov.hmcts.reform.sscs.tyanotifications.tya;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.APPELLANT_NAME;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.NAME;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.REPRESENTATIVE_NAME;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationTestRecipients;
import uk.gov.hmcts.reform.sscs.tyanotifications.controller.NotificationController;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.MarkdownTransformationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationHandler;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationSender;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationValidService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.OutOfHoursCalculator;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.ReminderService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.SaveCorrespondenceAsyncService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.SendNotificationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.docmosis.PdfLetterService;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendLetterResponse;
import uk.gov.service.notify.SendSmsResponse;

@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class NotificationsItBase {

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

    @MockitoBean
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

    @MockitoBean
    private IdamService idamService;

    protected String json;

    @Autowired
    private NotificationHandler notificationHandler;

    @MockitoBean
    private OutOfHoursCalculator outOfHoursCalculator;

    @Autowired
    private NotificationConfig notificationConfig;

    @Autowired
    private PdfLetterService pdfLetterService;

    @Autowired
    private DocmosisPdfService docmosisPdfService;

    @MockitoBean
    private DocmosisPdfGenerationService docmosisPdfGenerationService;

    @Mock
    protected PdfStoreService pdfStoreService;

    @Value("${notification.english.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailId;

    @Value("${notification.english.subscriptionCreated.appellant.smsId}")
    private String subscriptionCreatedSmsId;

    private final Boolean saveCorrespondence = false;

    @Mock
    private BulkPrintService bulkPrintService;

    @Mock
    private MarkdownTransformationService markdownTransformationService;
    @Mock
    private SaveCorrespondenceAsyncService saveCorrespondenceAsyncService;

    @Autowired
    @Qualifier("scheduler")
    protected Scheduler quartzScheduler;

    protected NotificationService service;

    @BeforeEach
    public void setup() throws Exception {
        openMocks(this);
        NotificationSender sender = new NotificationSender(notificationClient, null, bulkPrintService, notificationTestRecipients, markdownTransformationService, saveCorrespondenceAsyncService, saveCorrespondence);

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
        service = new NotificationService(factory, reminderService, notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService, false, true);
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
        assertThat(emailTemplateIdCaptor.getAllValues()).containsExactlyElementsOf(expectedEmailTemplateIds);

        if (0 < wantedNumberOfSendEmailInvocations) {
            final Map<String, ?> personalisation = emailPersonalisationCaptor.getValue();
            if (null != personalisation.get(REPRESENTATIVE_NAME)) {
                assertThat(personalisation.get(REPRESENTATIVE_NAME)).isEqualTo(expectedName);
            } else {
                assertThat(personalisation.get(NAME)).isEqualTo(expectedName);
            }
            assertThat(personalisation.get(APPELLANT_NAME)).isEqualTo("Dexter Vasquez");
        }
    }

    protected void validateSmsNotifications(List<String> expectedSmsTemplateIds, int wantedNumberOfSendSmsInvocations) throws NotificationClientException {
        final ArgumentCaptor<String> smsTemplateIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationClient, times(wantedNumberOfSendSmsInvocations))
            .sendSms(smsTemplateIdCaptor.capture(), any(), any(), any(), any());
        assertThat(smsTemplateIdCaptor.getAllValues()).containsExactlyElementsOf(expectedSmsTemplateIds);
    }

    protected void validateLetterNotifications(List<String> expectedLetterTemplateIds, int wantedNumberOfSendLetterInvocations, String expectedName) throws NotificationClientException {
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, Object>> letterPersonalisationCaptor = ArgumentCaptor.forClass(Map.class);
        final ArgumentCaptor<String> letterTemplateIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationClient, atMost(wantedNumberOfSendLetterInvocations))
            .sendLetter(letterTemplateIdCaptor.capture(), letterPersonalisationCaptor.capture(), any());
        assertThat(letterTemplateIdCaptor.getAllValues())
            .containsExactlyElementsOf(expectedLetterTemplateIds.stream()
                .filter(f -> !(f.endsWith(".doc") || f.endsWith(".docx")))
                .toList());

        final int expectedDocmosisLetters = (int) expectedLetterTemplateIds.stream().filter(f -> f.endsWith(".doc") || f.endsWith(".docx")).count();
        if (expectedDocmosisLetters > 0) {
            verify(notificationClient, times(expectedDocmosisLetters)).sendPrecompiledLetterWithInputStream(any(), any());
        } else {
            verify(notificationClient, times(0)).sendPrecompiledLetterWithInputStream(any(), any());
        }

        if (0 < wantedNumberOfSendLetterInvocations) {
            final Map<String, Object> personalisation = letterPersonalisationCaptor.getValue();
            if (null != personalisation.get(REPRESENTATIVE_NAME)) {
                assertThat(personalisation.get(REPRESENTATIVE_NAME)).isEqualTo(expectedName);
            } else {
                assertThat(personalisation.get(NAME)).isEqualTo(expectedName);
            }
            assertThat(personalisation.get(APPELLANT_NAME)).isEqualTo("Dexter Vasquez");
        }
    }


    public MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
