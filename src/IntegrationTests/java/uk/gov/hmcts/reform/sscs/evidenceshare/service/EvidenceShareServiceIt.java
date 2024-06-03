package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.document.EvidenceDownloadClientApi;
import uk.gov.hmcts.reform.sscs.document.EvidenceMetadataDownloadClientApi;
import uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.RoboticsCallbackHandler;
import uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.SendToBulkPrintHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementSecureDocStoreService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.servicebus.TopicConsumer;


@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_es_it.properties")
public class EvidenceShareServiceIt {

    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    //end of rules needed for junitParamsRunner

    @MockBean
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private IdamService idamService;

    @MockBean
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private CcdClient ccdClient;

    @MockBean
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private EvidenceDownloadClientApi evidenceDownloadClientApi;

    @MockBean
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private EvidenceMetadataDownloadClientApi evidenceMetadataDownloadClientApi;

    @MockBean
    private EvidenceManagementService evidenceManagementService;

    @MockBean
    private EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService;

    @MockBean
    private CcdService ccdService;

    @MockBean
    private UpdateCcdCaseService updateCcdCaseService;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private BulkPrintService bulkPrintService;

    @Autowired
    private RoboticsService roboticsService;

    @MockBean
    private EmailService emailService;

    @Autowired
    private SendToBulkPrintHandler bulkPrintHandler;

    @Autowired
    private RoboticsCallbackHandler roboticsCallbackHandler;

    @Autowired
    private TopicConsumer topicConsumer;

    @Autowired
    private PdfStoreService pdfStoreService;

    @Captor
    ArgumentCaptor<ArrayList<Pdf>> documentCaptor;

    @Captor
    private ArgumentCaptor<SscsCaseData> caseDataCaptor;

    @MockBean
    protected AirLookupService airLookupService;

    private static final String FILE_CONTENT = "Welcome to PDF document service";

    protected Session session = Session.getInstance(new Properties());

    protected MimeMessage message;

    @MockBean(name = "sendGridMailSender")
    protected JavaMailSender mailSender;

    Optional<UUID> expectedOptionalUuid = Optional.of(UUID.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b"));

    @Before
    public void setup() {
        message = new MimeMessage(session);
        when(mailSender.createMimeMessage()).thenReturn(message);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        assertNotNull("SendToBulkPrintHandler must be autowired", bulkPrintHandler);
    }

    @Test
    public void givenDigitalCaseWithMrnDateWithin30Days_shouldGenerateDL6TemplateAndAndAddToCaseInCcdAndSendToRoboticsAndBulkPrintInCorrectOrder() throws IOException {
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/validAppealCreatedCallbackWithMrn.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = updateMrnDate(json, LocalDate.now().toString());
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());
        json = json.replace("CASE_STATE", "validAppeal");
        json = json.replace("CCD_EVENT_ID", "validAppealCreated");
        json = json.replace("CREATED_IN_GAPS_FROM", "readyToList");

        when(ccdService.updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq("Case state is now sent to FTA"), any())).thenReturn(SscsCaseDetails.builder().build());

        topicConsumer.onMessage(json, "1");

        verify(ccdService).updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq("Case state is now sent to FTA"), any());
    }

    @Test
    public void givenDigitalCaseWithMrnDateOlderThan30Days_shouldGenerateDL16TemplateAndAndAddToCaseInCcdAndTriggerSentToDwpEvent() throws IOException {
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/validAppealCreatedCallbackWithMrn.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().minusDays(31).toString());
        json = json.replace("CASE_STATE", "validAppeal");
        json = json.replace("CCD_EVENT_ID", "validAppealCreated");
        json = json.replace("CREATED_IN_GAPS_FROM", "readyToList");

        when(ccdService.updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq("Case state is now sent to FTA"), any())).thenReturn(SscsCaseDetails.builder().build());

        topicConsumer.onMessage(json, "1");

        verify(ccdService).updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq("Case state is now sent to FTA"), any());
    }

    @Test
    public void givenNonDigitalCaseAndSecureDocstoreOff_shouldGenerateDlDocumentTemplateAndAndAddToCaseInCcdAndSendToRoboticsAndBulkPrint() throws IOException {
        //FIXME: Remove this test once secureDocStoreEnabled feature switched on
        ReflectionTestUtils.setField(pdfStoreService, "secureDocStoreEnabled", false);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/validAppealCreatedCallbackWithMrn.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().minusDays(31).toString());
        json = json.replace("CASE_STATE", "validAppeal");
        json = json.replace("CCD_EVENT_ID", "validAppealCreated");
        json = json.replace("CREATED_IN_GAPS_FROM", "validAppeal");

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), eq("sscs"))).thenReturn(uploadResponse);
        when(ccdService.updateCase(any(), any(), any(), any(), eq("Uploaded dl16-12345656789.pdf into SSCS"), any())).thenReturn(SscsCaseDetails.builder().build());

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any())).thenReturn(expectedOptionalUuid);

        String documentList = "Case has been sent to the FTA via Bulk Print with bulk print id: 0f14d0ab-9605-4a62-a9e4-5ed26688389b and with documents: dl16-12345656789.pdf, sscs1.pdf, filename1.pdf";
        when(ccdService.updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq(documentList), any())).thenReturn(SscsCaseDetails.builder().build());

        topicConsumer.onMessage(json, "1");

        Assert.assertEquals(3, documentCaptor.getValue().size());
        Assert.assertEquals("dl16-12345656789.pdf", documentCaptor.getValue().get(0).getName());
        Assert.assertEquals("sscs1.pdf", documentCaptor.getValue().get(1).getName());
        Assert.assertEquals("filename1.pdf", documentCaptor.getValue().get(2).getName());

        verify(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));
        verify(evidenceManagementService).upload(any(), eq("sscs"));
        verify(ccdService).updateCase(any(), any(), any(), any(), eq("Uploaded dl16-12345656789.pdf into SSCS"), any());
        verify(bulkPrintService).sendToBulkPrint(any(), any(), any());
        verify(emailService).sendEmail(anyLong(), any());

        verify(ccdService).updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq(documentList), any());
    }

    @Test
    public void givenNonDigitalCaseAndSecureDocStoreOn_shouldGenerateDlDocumentTemplateAndAndAddToCaseInCcdAndSendToRoboticsAndBulkPrint() throws IOException {
        ReflectionTestUtils.setField(pdfStoreService, "secureDocStoreEnabled", true);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/validAppealCreatedCallbackWithMrn.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().minusDays(31).toString());
        json = json.replace("CASE_STATE", "validAppeal");
        json = json.replace("CCD_EVENT_ID", "validAppealCreated");
        json = json.replace("CREATED_IN_GAPS_FROM", "validAppeal");

        doReturn(new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));

        uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse uploadResponse = createSecureUploadResponse();
        when(evidenceManagementSecureDocStoreService.upload(any(), any())).thenReturn(uploadResponse);
        when(ccdService.updateCase(any(), any(), any(), any(), eq("Uploaded dl16-12345656789.pdf into SSCS"), any())).thenReturn(SscsCaseDetails.builder().build());

        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any())).thenReturn(expectedOptionalUuid);

        String documentList = "Case has been sent to the FTA via Bulk Print with bulk print id: 0f14d0ab-9605-4a62-a9e4-5ed26688389b and with documents: dl16-12345656789.pdf, sscs1.pdf, filename1.pdf";
        when(ccdService.updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq(documentList), any())).thenReturn(SscsCaseDetails.builder().build());

        topicConsumer.onMessage(json, "1");

        Assert.assertEquals(3, documentCaptor.getValue().size());
        Assert.assertEquals("dl16-12345656789.pdf", documentCaptor.getValue().get(0).getName());
        Assert.assertEquals("sscs1.pdf", documentCaptor.getValue().get(1).getName());
        Assert.assertEquals("filename1.pdf", documentCaptor.getValue().get(2).getName());

        verify(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));
        verify(evidenceManagementSecureDocStoreService).upload(any(), any());
        verify(ccdService).updateCase(any(), any(), any(), any(), eq("Uploaded dl16-12345656789.pdf into SSCS"), any());
        verify(bulkPrintService).sendToBulkPrint(any(), any(), any());
        verify(emailService).sendEmail(anyLong(), any());

        verify(ccdService).updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq(documentList), any());
    }

    @Test
    public void givenDigitalCaseInReadyToListState_shouldSendToRoboticsAndUpdateDwpOffice() throws IOException {
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/validAppealCreatedCallbackWithMrn.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().minusDays(31).toString());
        json = json.replace("CASE_STATE", "readyToList");
        json = json.replace("CCD_EVENT_ID", "readyToList");
        json = json.replace("CREATED_IN_GAPS_FROM", "readyToList");

        topicConsumer.onMessage(json, "1");

        verify(emailService).sendEmail(anyLong(), any());

        verify(ccdService).updateCase(caseDataCaptor.capture(), any(), eq(EventType.CASE_UPDATED.getCcdType()), any(), any(), any());
        assertEquals("DWP PIP (2)", caseDataCaptor.getValue().getAppeal().getMrnDetails().getDwpIssuingOffice());
    }

    @Test
    public void appealWithNoMrnDate_shouldNotGenerateTemplateOrAddToCcdAndShouldUpdateCaseWithSecondaryState()
        throws IOException {
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/validAppealCreatedCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("CREATED_IN_GAPS_FROM", "validAppeal");

        ArgumentCaptor<SscsCaseData> caseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);

        topicConsumer.onMessage(json, "1");

        then(ccdService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), any(), eq("sendToDwpError"), any(), any(), any());
        assertNull(caseDataCaptor.getValue().getAppeal().getMrnDetails().getMrnDate());
        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());

        verifyNoMoreInteractions(restTemplate);
        verifyNoMoreInteractions(evidenceManagementService);
        verify(emailService).sendEmail(anyLong(), any());
    }

    @Test
    public void nonReceivedViaPaper_shouldNotBeBulkPrintedAndStateShouldBeUpdated() throws IOException {
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/validAppealCreatedCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("PAPER", "ONLINE");
        json = json.replace("CREATED_IN_GAPS_FROM", "validAppeal");

        topicConsumer.onMessage(json, "1");

        verifyNoMoreInteractions(restTemplate);
        verifyNoMoreInteractions(evidenceManagementService);
        verify(ccdService).updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq("Case state is now sent to FTA"), any());
        verify(emailService).sendEmail(anyLong(), any());
    }

    @Test
    public void givenADigitalCase_shouldNotBeBulkPrintedAndStateShouldBeUpdatedAndNotSentToRobotics() throws IOException {
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/validAppealCreatedCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = json.replace("CREATED_IN_GAPS_FROM", "readyToList");

        topicConsumer.onMessage(json, "1");

        verifyNoMoreInteractions(restTemplate);
        verifyNoMoreInteractions(evidenceManagementService);
        verify(ccdService).updateCase(any(), any(), eq(EventType.SENT_TO_DWP.getCcdType()), any(), eq("Case state is now sent to FTA"), any());
        verifyNoMoreInteractions(emailService);
    }

    private String updateMrnDate(String json, String updatedDate) {
        json = json.replace("2019-01-01", updatedDate);

        return json;
    }

    private UploadResponse createUploadResponse() {
        UploadResponse response = mock(UploadResponse.class);
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(response.getEmbedded()).thenReturn(embedded);
        Document document = createDocument();
        when(embedded.getDocuments()).thenReturn(Collections.singletonList(document));
        return response;
    }

    private uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse createSecureUploadResponse() {
        uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse response = mock(uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse.class);
        uk.gov.hmcts.reform.ccd.document.am.model.Document document = createSecureDocument();
        when(response.getDocuments()).thenReturn(Collections.singletonList(document));
        return response;
    }

    private Document createDocument() {
        Document document = new Document();
        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = "http://link.com";
        links.self = link;
        document.links = links;
        return document;
    }

    private uk.gov.hmcts.reform.ccd.document.am.model.Document createSecureDocument() {

        uk.gov.hmcts.reform.ccd.document.am.model.Document.Links links = new uk.gov.hmcts.reform.ccd.document.am.model.Document.Links();
        uk.gov.hmcts.reform.ccd.document.am.model.Document.Link link = new uk.gov.hmcts.reform.ccd.document.am.model.Document.Link();
        link.href = "http://link.com";
        links.self = link;
        uk.gov.hmcts.reform.ccd.document.am.model.Document document = uk.gov.hmcts.reform.ccd.document.am.model.Document.builder().links(links).build();

        return document;
    }
}
