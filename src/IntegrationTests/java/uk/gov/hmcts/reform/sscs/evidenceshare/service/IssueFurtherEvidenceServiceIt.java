package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.domain.PdfDocumentRequest;
import uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.IssueFurtherEvidenceHandler;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementSecureDocStoreService;
import uk.gov.hmcts.reform.sscs.service.servicebus.TopicConsumer;


@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_es_it.properties")
public class IssueFurtherEvidenceServiceIt {

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
    private EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService;

    @MockBean
    private CcdService ccdService;

    @MockBean
    private SscsCcdConvertService sscsCcdConvertService;

    @MockBean
    private UpdateCcdCaseService updateCcdCaseService;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private BulkPrintService bulkPrintService;

    @MockBean
    private DocmosisTemplateConfig docmosisTemplateConfig;

    @Autowired
    @Qualifier("issueFurtherEvidenceHandler")
    private IssueFurtherEvidenceHandler handler;

    @Autowired
    private TopicConsumer topicConsumer;

    @MockBean
    protected AirLookupService airLookupService;

    @Captor
    ArgumentCaptor<ArrayList<Pdf>> documentCaptor;

    @Captor
    ArgumentCaptor<PdfDocumentRequest> pdfDocumentRequest;

    private byte[] fileContent;

    protected Session session = Session.getInstance(new Properties());

    protected MimeMessage message;

    Optional<UUID> expectedOptionalUuid = Optional.of(UUID.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b"));

    Map<LanguagePreference, Map<String, Map<String, String>>> template = new HashMap<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Before
    public void setup() throws Exception {
        Map<String, String> nameMap;
        Map<String, Map<String, String>> englishDocs = new HashMap<>();
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00010.doc");
        englishDocs.put(DocumentType.DL6.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00011.doc");
        englishDocs.put(DocumentType.DL16.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00068.doc");
        englishDocs.put("d609-97", nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00069.doc");
        englishDocs.put("d609-98", nameMap);

        Map<String, Map<String, String>> welshDocs = new HashMap<>();
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00010.doc");
        welshDocs.put(DocumentType.DL6.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00011.doc");
        welshDocs.put(DocumentType.DL16.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-WEL-00469.docx");
        welshDocs.put("d609-97", nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-WEL-00470.docx");
        welshDocs.put("d609-98", nameMap);

        template.put(LanguagePreference.ENGLISH, englishDocs);
        template.put(LanguagePreference.WELSH, welshDocs);

        message = new MimeMessage(session);

        fileContent = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("evidenceshare/myPdf.pdf"));

        when(evidenceManagementSecureDocStoreService.download(any(), any())).thenReturn(fileContent);
    }

    @Test
    public void appealWithAppellantAndFurtherEvidenceFromAppellant_shouldSend609_97ToAppellantAndNotSend609_98() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(fileContent, HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any(), any(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/issueFurtherEvidenceCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockCcdCaseDataForStartEvent(json);

        topicConsumer.onMessage(json, "1");

        verify(bulkPrintService).sendToBulkPrint(any(), any(), any(), any(), any());

        assertEquals(1, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                eq(12345656789L),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                any(IdamTokens.class),
                any(Function.class)
        );
    }

    @Test
    @Parameters({"Rep", "JointParty"})
    public void appealWithAppellantAndRepFurtherEvidenceFromAppellant_shouldSend609_97ToAppellantAnd609_98ToParty(String party) throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(fileContent, HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any(), any(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource(String.format("evidenceshare/issueFurtherEvidenceCallbackWith%s.json", party))).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockCcdCaseDataForStartEvent(json);

        topicConsumer.onMessage(json, "1");

        verify(bulkPrintService, times(2)).sendToBulkPrint(any(), any(), any(), any(), any());

        assertEquals(2, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(1).get(1).getName());

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                eq(12345656789L),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                any(IdamTokens.class),
                any(Function.class)
        );
    }

    @Test
    public void appealWithAppellantAndRepFurtherEvidenceFromRep_shouldSend609_97ToRepAnd609_98ToAppellant() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(fileContent, HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any(), any(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/issueFurtherEvidenceCallbackWithRepEvidence.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockCcdCaseDataForStartEvent(json);

        topicConsumer.onMessage(json, "1");

        verify(bulkPrintService, times(2)).sendToBulkPrint(any(), any(), any(), any(), any());

        assertEquals(2, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(1).get(1).getName());

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                eq(12345656789L),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                any(IdamTokens.class),
                any(Function.class)
        );
    }

    @Test
    public void appealWithAppellantFurtherEvidenceAndRepFurtherEvidence_shouldSend609_97ToRepAndAppellantAnd609_98ToAppellantAndRep() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(fileContent, HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any(), any(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/issueFurtherEvidenceCallbackWithAppellantEvidenceAndRepEvidence.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockCcdCaseDataForStartEvent(json);

        topicConsumer.onMessage(json, "1");

        verify(bulkPrintService, times(4)).sendToBulkPrint(any(), any(), any(), any(), any());

        assertEquals(4, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());
        assertEquals(2, documentCaptor.getAllValues().get(2).size());
        assertEquals(2, documentCaptor.getAllValues().get(3).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("appellant-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("appellant-document", documentCaptor.getAllValues().get(1).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(2).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(2).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(2).get(1).getName());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(3).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(3).get(0).getName());
        assertEquals("rep-document", documentCaptor.getAllValues().get(3).get(1).getName());

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                eq(12345656789L),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                any(IdamTokens.class),
                any(Function.class)
        );
    }

    @Test
    public void appealWithFurtherEvidenceFromDwp_shouldNotSend609_97AndSend609_98ToAppellant() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(fileContent, HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any(), any(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/issueFurtherEvidenceCallbackWithDwpEvidence.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockCcdCaseDataForStartEvent(json);

        topicConsumer.onMessage(json, "1");

        verify(bulkPrintService).sendToBulkPrint(any(), any(), any(), any(), any());

        assertEquals(1, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                eq(12345656789L),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                any(IdamTokens.class),
                any(Function.class)
        );
    }

    @Test
    @Parameters({"Rep", "JointParty"})
    public void appealWithRepAndFurtherEvidenceFromDwp_shouldNotSend609_97AndSend609_98ToAppellantAndParty(String party) throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(fileContent, HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any(), any(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource(String.format("evidenceshare/issueFurtherEvidenceCallbackWith%sAndEvidenceFromDwp.json", party))).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockCcdCaseDataForStartEvent(json);

        topicConsumer.onMessage(json, "1");

        verify(bulkPrintService, times(2)).sendToBulkPrint(any(), any(), any(), any(), any());

        assertEquals(2, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Peter Hyland", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(1).get(1).getName());

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                eq(12345656789L),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                any(IdamTokens.class),
                any(Function.class)
        );
    }

    @Test
    @Parameters({"OtherParty", "OtherPartyAppointee"})
    public void appealWithAppellantAndOtherPartyAndFurtherEvidenceFromOtherParty_shouldSend609_97ToOtherPartyAndSend609_98ToAppellant(String otherPartyType) throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(fileContent, HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any(), any(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource(String.format("evidenceshare/issueFurtherEvidenceCallbackWith%sEvidence.json", otherPartyType))).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockCcdCaseDataForStartEvent(json);

        topicConsumer.onMessage(json, "1");

        verify(bulkPrintService, times(2)).sendToBulkPrint(any(), any(), any(), any(), any());

        assertEquals(2, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());

        String expectedName = otherPartyType.equals("OtherParty") ? "John Lewis" : "Wendy Smith";
        assertEquals(expectedName, pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(1).get(1).getName());

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                eq(12345656789L),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                any(IdamTokens.class),
                any(Function.class)
        );
    }

    @Test
    public void appealWithAppellantAndOtherPartyRepAndFurtherEvidenceFromOtherPartyRep_shouldSend609_97ToOtherPartyRepAndSend609_98ToOtherPartyAndAppellant() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(fileContent, HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any(), any(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/issueFurtherEvidenceCallbackWithOtherPartyRepEvidence.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockCcdCaseDataForStartEvent(json);

        topicConsumer.onMessage(json, "1");

        verify(bulkPrintService, times(3)).sendToBulkPrint(any(), any(), any(), any(), any());

        assertEquals(3, documentCaptor.getAllValues().size());
        assertEquals(2, documentCaptor.getAllValues().get(0).size());
        assertEquals(2, documentCaptor.getAllValues().get(1).size());
        assertEquals(2, documentCaptor.getAllValues().get(2).size());

        assertEquals("Test Rep", pdfDocumentRequest.getAllValues().get(0).getData().get("name"));
        assertEquals("609-97-template (original sender)", documentCaptor.getAllValues().get(0).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(0).get(1).getName());

        assertEquals("Sarah Smith", pdfDocumentRequest.getAllValues().get(1).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(1).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(1).get(1).getName());

        assertEquals("Wendy Smith", pdfDocumentRequest.getAllValues().get(2).getData().get("name"));
        assertEquals("609-98-template (other parties)", documentCaptor.getAllValues().get(2).get(0).getName());
        assertEquals("evidence-document", documentCaptor.getAllValues().get(2).get(1).getName());

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                eq(12345656789L),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                any(IdamTokens.class),
                any(Function.class)
        );
    }

    @Test
    public void appealWithAppellantAndMultipleOtherPartiesAndMultipleFurtherEvidenceFromOtherParties_shouldSend609_97ToOtherPartyRepAndSend609_98ToAllOtherPartiesAndAppellant() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(fileContent, HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any(), any(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/issueFurtherEvidenceCallbackWithMultipleOtherPartyRepEvidence.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockCcdCaseDataForStartEvent(json);

        topicConsumer.onMessage(json, "1");

        verify(bulkPrintService, times(5)).sendToBulkPrint(any(), any(), any(), any(), any());

        assertEquals(5, documentCaptor.getAllValues().size());
        assertEquals(3, documentCaptor.getAllValues().get(0).size());
        assertEquals(3, documentCaptor.getAllValues().get(1).size());
        assertEquals(3, documentCaptor.getAllValues().get(2).size());
        assertEquals(3, documentCaptor.getAllValues().get(3).size());
        assertEquals(3, documentCaptor.getAllValues().get(4).size());

        Integer pdfIndex = findPdfDocumentRequestIndex("Test Rep");
        assertNotNull(pdfIndex);
        assertThat(documentCaptor.getAllValues().get(pdfIndex))
            .hasSize(3)
            .extracting(Pdf::getName)
            .contains("609-97-template (original sender)", "evidence-document2", "evidence-document");

        pdfIndex = findPdfDocumentRequestIndex("Sarah Smith");
        assertNotNull(pdfIndex);
        assertThat(documentCaptor.getAllValues().get(pdfIndex))
            .hasSize(3)
            .extracting(Pdf::getName)
            .contains("609-98-template (other parties)", "evidence-document2", "evidence-document");

        pdfIndex = findPdfDocumentRequestIndex("Wendy Smith");
        assertNotNull(pdfIndex);
        assertThat(documentCaptor.getAllValues().get(pdfIndex))
            .hasSize(3)
            .extracting(Pdf::getName)
            .contains("609-98-template (other parties)", "evidence-document2", "evidence-document");

        pdfIndex = findPdfDocumentRequestIndex("Shelly Barat");
        assertNotNull(pdfIndex);
        assertThat(documentCaptor.getAllValues().get(pdfIndex))
            .hasSize(3)
            .extracting(Pdf::getName)
            .contains("609-98-template (other parties)", "evidence-document2", "evidence-document");

        pdfIndex = findPdfDocumentRequestIndex("Robert Brokenshire");
        assertNotNull(pdfIndex);
        assertThat(documentCaptor.getAllValues().get(pdfIndex))
            .hasSize(3)
            .extracting(Pdf::getName)
            .contains("609-98-template (other parties)", "evidence-document2", "evidence-document");

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                eq(12345656789L),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                any(IdamTokens.class),
                any(Function.class)
        );
    }

    @Test
    public void appealWithAppellantAndFurtherEvidenceFromAppellantWithReasonableAdjustment_shouldReloadCaseData() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(fileContent, HttpStatus.OK))
            .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any(), any(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        List<Correspondence> reasonableadjustments = new ArrayList<>();
        Correspondence correspondence = new Correspondence(CorrespondenceDetails.builder().reasonableAdjustmentStatus(ReasonableAdjustmentStatus.REQUIRED)
            .to("Sarah Smith").build());
        reasonableadjustments.add(correspondence);
        when(ccdService.getByCaseId(any(), any())).thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder()
            .ccdCaseId("12345656789")
            .reasonableAdjustmentsLetters(ReasonableAdjustmentsLetters.builder()
                .appellant(reasonableadjustments).representative(Collections.emptyList()).appointee(Collections.emptyList())
                .jointParty(Collections.emptyList()).build()).build()).build());

        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
            .getResource("evidenceshare/issueFurtherEvidenceCallbackReasonableAdjustment.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockCcdCaseDataForStartEvent(json);

        topicConsumer.onMessage(json, "1");

        verify(bulkPrintService).sendToBulkPrint(any(), any(), any(), any(), any());

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                eq(12345656789L),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                any(IdamTokens.class),
                any(Function.class)
        );
    }

    @Test
    public void shouldFailSendingFurtherEvidenceWhenAppellantNameIsMissingAndBulkPrintServiceShouldNotBeInvoked() throws IOException {
        assertNotNull("IssueFurtherEvidenceHandler must be autowired", handler);

        doReturn(new ResponseEntity<>(fileContent, HttpStatus.OK))
                .when(restTemplate).postForEntity(anyString(), pdfDocumentRequest.capture(), eq(byte[].class));
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        when(bulkPrintService.sendToBulkPrint(documentCaptor.capture(), any(), any(), any(), any())).thenReturn(expectedOptionalUuid);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        // we are able to cause the issue further evidence to fail by setting to null the Appellant.Name in the issueFurtherEvidenceFaultyCallback.json
        String path = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
                .getResource("evidenceshare/issueFurtherEvidenceFaultyCallback.json")).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockCcdCaseDataForStartEvent(json);

        topicConsumer.onMessage(json, "1");

        verifyNoInteractions(bulkPrintService);
    }


    private Integer findPdfDocumentRequestIndex(String name) {
        for (Integer index = 0; index < pdfDocumentRequest.getAllValues().size(); index++) {
            if (name.equals(pdfDocumentRequest.getAllValues().get(index).getData().get("name"))) {
                return index;
            }
        }
        return null;
    }

    private void mockCcdCaseDataForStartEvent(String json) throws JsonProcessingException {
        Map<String, Object> caseDataMap = OBJECT_MAPPER.convertValue(OBJECT_MAPPER.readTree(json).get("case_details").get("case_data"), Map.class);
        caseDataMap.put("ccdCaseId", "12345656789");

        var startEventResponse = StartEventResponse.builder().caseDetails(CaseDetails.builder().data(caseDataMap).build()).build();

        when(ccdClient.startEvent(any(IdamTokens.class), any(), eq(EventType.ISSUE_FURTHER_EVIDENCE.getCcdType()))).thenReturn(startEventResponse);

        var sscsCaseDetails = SscsCaseDetails.builder().data(OBJECT_MAPPER.convertValue(caseDataMap, SscsCaseData.class)).build();
        when(sscsCcdConvertService.getCaseDetails(startEventResponse)).thenReturn(sscsCaseDetails);
    }
}
