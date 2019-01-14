package uk.gov.hmcts.reform.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.pdf;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.document.EvidenceDownloadClientApi;
import uk.gov.hmcts.reform.sscs.document.EvidenceMetadataDownloadClientApi;
import uk.gov.hmcts.reform.sscs.domain.email.Email;
import uk.gov.hmcts.reform.sscs.domain.email.RoboticsEmailTemplate;
import uk.gov.hmcts.reform.sscs.domain.email.SubmitYourAppealEmailTemplate;
import uk.gov.hmcts.reform.sscs.domain.pdf.PdfWrapper;
import uk.gov.hmcts.reform.sscs.domain.robotics.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaEvidence;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.json.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.json.RoboticsJsonValidator;

@RunWith(MockitoJUnitRunner.class)
public class SubmitAppealServiceTest {
    private static final String TEMPLATE_PATH = "/templates/appellant_appeal_template.html";

    @Mock
    private CcdService ccdService;

    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    private EmailService emailService;

    @Mock
    private AirLookupService airLookupService;

    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private IdamService idamService;

    @Mock
    private RoboticsJsonMapper roboticsJsonMapper;

    @Mock
    private RoboticsJsonValidator roboticsJsonValidator;

    @Captor
    private ArgumentCaptor<Map<String, Object>> captor;

    @Captor
    private ArgumentCaptor<Email> emailCaptor;

    private SubmitYourAppealEmailTemplate submitYourAppealEmailTemplate;

    private RoboticsEmailTemplate roboticsEmailTemplate;

    private SscsPdfService sscsPdfService;

    private RoboticsService roboticsService;

    private SubmitAppealService submitAppealService;

    private RegionalProcessingCenterService regionalProcessingCenterService;

    private SyaCaseWrapper appealData = getSyaCaseWrapper();

    private RoboticsWrapper roboticsWrapper;

    private JSONObject json = new JSONObject();

    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private DocumentUploadClientApi documentUploadClientApi;
    @Mock
    private EvidenceDownloadClientApi evidenceDownloadClientApi;
    @Mock
    private EvidenceMetadataDownloadClientApi evidenceMetadataDownloadClientApi;

    private EvidenceManagementService evidenceManagementService;

    private RoboticsJsonUploadService roboticsJsonUploadService;

    @Before
    public void setUp() {
        submitYourAppealEmailTemplate = new SubmitYourAppealEmailTemplate("from", "to", "message");
        roboticsEmailTemplate = new RoboticsEmailTemplate("from", "to", "message");

        sscsPdfService = new SscsPdfService(TEMPLATE_PATH, pdfServiceClient, emailService, pdfStoreService, submitYourAppealEmailTemplate, ccdService);
        roboticsService = new RoboticsService(airLookupService, emailService, roboticsJsonMapper, roboticsJsonValidator, roboticsEmailTemplate);

        regionalProcessingCenterService = new RegionalProcessingCenterService(airLookupService);
        regionalProcessingCenterService.init();

        ResponseEntity<Resource> mockResponseEntity = mock(ResponseEntity.class);
        ByteArrayResource stubbedResource = new ByteArrayResource(new byte[] {});
        when(mockResponseEntity.getBody()).thenReturn(stubbedResource);

        when(authTokenGenerator.generate()).thenReturn("token");
        when(evidenceDownloadClientApi.downloadBinary(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockResponseEntity);

        UploadResponse mockUploadResponse = mock(UploadResponse.class);

        UploadResponse.Embedded mockEmbedded = mock(UploadResponse.Embedded.class);
        when(mockUploadResponse.getEmbedded()).thenReturn(mockEmbedded);

        List<Document> mockDocuments = mock(List.class);
        when(mockEmbedded.getDocuments()).thenReturn(mockDocuments);

        Document mockDocument = mock(Document.class);
        when(mockDocuments.get(0)).thenReturn(mockDocument);

        Document.Links mockLinks = mock(Document.Links.class);
        mockDocument.links = new Document.Links();
        mockDocument.links.binary = new Document.Link();
        mockDocument.links.binary.href = "http://example.com/document";

        when(documentUploadClientApi.upload(anyString(), anyString(), anyString(), anyList())).thenReturn(mockUploadResponse);

        evidenceManagementService = new EvidenceManagementService(authTokenGenerator, documentUploadClientApi, evidenceDownloadClientApi, evidenceMetadataDownloadClientApi);
        roboticsJsonUploadService = new RoboticsJsonUploadService(documentUploadClientApi, authTokenGenerator, ccdService);

        submitAppealService = new SubmitAppealService(ccdService,
                sscsPdfService, roboticsService,
                airLookupService, regionalProcessingCenterService, idamService, evidenceManagementService,
                roboticsJsonUploadService);

        given(ccdService.createCase(any(SscsCaseData.class), any(IdamTokens.class)))
            .willReturn(SscsCaseDetails.builder().id(123L).build());

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        given(emailService.generateUniqueEmailId(any(Appellant.class))).willReturn("Bloggs_33C");

        roboticsWrapper = RoboticsWrapper.builder().sscsCaseData(
                convertSyaToCcdCaseData(appealData)).ccdCaseId(123L).evidencePresent("No").build();

        given(roboticsJsonMapper.map(any())).willReturn(json);
    }

    @Test
    public void shouldSendPdfByEmailWhenCcdIsDown() {
        given(ccdService.createCase(any(SscsCaseData.class), any(IdamTokens.class))).willThrow(new CcdException(
            "Error while creating case in CCD"));

        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), captor.capture()))
            .willReturn(expected);

        roboticsWrapper = RoboticsWrapper.builder().sscsCaseData(
                convertSyaToCcdCaseData(appealData)).ccdCaseId(null).evidencePresent("No").build();

        submitAppealService.submitAppeal(appealData);

        verify(ccdService, never()).updateCase(any(), any(), any(), any(), any(), any());
        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(emailService, times(2)).sendEmail(any(Email.class));

        assertNull(getPdfWrapper().getCcdCaseId());
    }

    private PdfWrapper getPdfWrapper() {
        Map<String, Object> placeHolders = captor.getAllValues().get(0);
        return (PdfWrapper) placeHolders.get("PdfWrapper");
    }

    @Test
    public void givenCaseDoesNotExistInCcd_shouldCreateCaseWithAppealDetails() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
            any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData);

        verify(ccdService).createCase(any(SscsCaseData.class), any(IdamTokens.class));
    }

    @Test
    public void givenCaseAlreadyExistsInCcd_shouldNotCreateCaseWithAppealDetails() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(SscsCaseDetails.builder().build());

        roboticsWrapper = RoboticsWrapper.builder().sscsCaseData(
                convertSyaToCcdCaseData(appealData)).ccdCaseId(null).evidencePresent("No").build();

        submitAppealService.submitAppeal(appealData);

        verify(ccdService, never()).createCase(any(SscsCaseData.class), any(IdamTokens.class));
    }

    @Test
    public void shouldCreatePdfWithAppealDetails() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
            any())).willReturn(expected);

        submitAppealService.submitAppeal(appealData);

        Email expectedEmail = submitYourAppealEmailTemplate.generateEmail(
                "Bloggs_33C",
                newArrayList(pdf(expected, "Bloggs_33C.pdf"))
        );
        verify(emailService).sendEmail(expectedEmail);
    }

    @Test
    public void shouldSendRoboticsByEmailPdfOnly() {

        byte[] expected = {};

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any(Map.class))).willReturn(expected);

        submitAppealService.submitAppeal(appealData);

        verify(emailService, times(2)).sendEmail(emailCaptor.capture());
        Email roboticsEmail = emailCaptor.getAllValues().get(1);
        assertEquals("Expecting 2 attachments", 2, roboticsEmail.getAttachments().size());
    }

    @Test
    public void shouldSendRoboticsByEmailWithEvidence() {

        byte[] expected = {};

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any(Map.class))).willReturn(expected);

        uk.gov.hmcts.reform.document.domain.Document stubbedDocument = new uk.gov.hmcts.reform.document.domain.Document();
        uk.gov.hmcts.reform.document.domain.Document.Link stubbedLink = new uk.gov.hmcts.reform.document.domain.Document.Link();
        stubbedLink.href = "http://localhost:4506/documents/eb8cbfaa-37c3-4644-aa77-b9a2e2c72332";
        uk.gov.hmcts.reform.document.domain.Document.Links stubbedLinks = new Document.Links();
        stubbedLinks.binary = stubbedLink;
        stubbedDocument.links = stubbedLinks;
        given(evidenceMetadataDownloadClientApi.getDocumentMetadata(anyString(), anyString(), anyString(), anyString(), anyString())).willReturn(stubbedDocument);

        submitAppealService.submitAppeal(appealDataWithEvidence());

        verify(emailService, times(2)).sendEmail(emailCaptor.capture());
        Email roboticsEmail = emailCaptor.getAllValues().get(1);
        assertEquals("Expecting 5 attachments", 5, roboticsEmail.getAttachments().size());
    }

    @Test
    public void testPostcodeSplit() {
        assertEquals("TN32", submitAppealService.getFirstHalfOfPostcode("TN32 6PL"));
    }

    @Test
    public void testPostcodeSplitWithNoSpace() {
        assertEquals("TN32", submitAppealService.getFirstHalfOfPostcode("TN326PL"));
    }

    @Test
    public void testInvalidPostCode() {
        assertEquals("", submitAppealService.getFirstHalfOfPostcode(""));
    }

    @Test
    public void testNullPostCode() {
        appealData.getAppellant().getContactDetails().setPostCode(null);

        assertEquals("", submitAppealService.getFirstHalfOfPostcode(null));
    }

    @Test
    public void testRegionAddedToCase() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        SscsCaseData caseData = submitAppealService.transformAppealToCaseData(appealData,"Cardiff", rpc);
        assertEquals("Cardiff", caseData.getRegion());
    }

    @Test
    public void shouldStorePdfInDocumentStore() {
        byte[] expected = {1, 2, 3};

        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);

        submitAppealService.submitAppeal(appealData);

        then(pdfStoreService).should().store(expected, "Bloggs_33C.pdf");
    }

    @Test
    public void shouldUpdateCcdWithPdf() {
        byte[] expected = {1, 2, 3};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);
        long ccdId = 987L;
        given(ccdService.createCase(any(SscsCaseData.class), any(IdamTokens.class))).willReturn(SscsCaseDetails.builder().id(ccdId).build());
        SscsDocument pdfDocument = new SscsDocument(SscsDocumentDetails.builder().build());
        List<SscsDocument> sscsDocuments = singletonList(pdfDocument);
        given(pdfStoreService.store(expected, "Bloggs_33C.pdf")).willReturn(sscsDocuments);

        roboticsWrapper = RoboticsWrapper.builder().sscsCaseData(
                convertSyaToCcdCaseData(appealData)).ccdCaseId(987L).evidencePresent("No").build();

        submitAppealService.submitAppeal(appealData);

        verify(ccdService).updateCase(
                argThat(caseData ->  sscsDocuments.equals(caseData.getSscsDocument())),
                eq(ccdId),
                eq("uploadDocument"),
                any(), any(), any()
        );
    }

    @Test
    public void shouldUpdateCcdWithPdfCombinedWithEvidence() {
        uk.gov.hmcts.reform.document.domain.Document stubbedDocument = new uk.gov.hmcts.reform.document.domain.Document();
        uk.gov.hmcts.reform.document.domain.Document.Link stubbedLink = new uk.gov.hmcts.reform.document.domain.Document.Link();
        stubbedLink.href = "http://localhost:4506/documents/eb8cbfaa-37c3-4644-aa77-b9a2e2c72332";
        uk.gov.hmcts.reform.document.domain.Document.Links stubbedLinks = new Document.Links();
        stubbedLinks.binary = stubbedLink;
        stubbedDocument.links = stubbedLinks;
        given(evidenceMetadataDownloadClientApi.getDocumentMetadata(anyString(), anyString(), anyString(), anyString(), anyString())).willReturn(stubbedDocument);

        byte[] expected = {1, 2, 3};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);
        long ccdId = 987L;
        given(ccdService.createCase(any(SscsCaseData.class), any(IdamTokens.class))).willReturn(SscsCaseDetails.builder().id(ccdId)
                .build());
        SscsDocument pdfDocument = new SscsDocument(SscsDocumentDetails.builder().build());
        List<SscsDocument> sscsDocuments = singletonList(pdfDocument);
        given(pdfStoreService.store(expected, "Bloggs_33C.pdf")).willReturn(sscsDocuments);
        SyaCaseWrapper appealData = getSyaCaseWrapper("json/sya_with_evidence.json");

        roboticsWrapper = RoboticsWrapper.builder().sscsCaseData(
                convertSyaToCcdCaseData(appealData)).ccdCaseId(987L).evidencePresent("Yes").build();

        submitAppealService.submitAppeal(appealData);

        verify(ccdService).updateCase(
                argThat(caseData -> caseData.getSscsDocument().size() == 3
                        && caseData.getSscsDocument().get(2).equals(sscsDocuments.get(0))),
                eq(ccdId),
                eq("uploadDocument"),
                any(), any(), any()
        );
    }

    private SyaCaseWrapper appealDataWithEvidence() {
        SyaEvidence evidence1 = new SyaEvidence("http://localhost/1", "letter.pdf", LocalDate.now());
        SyaEvidence evidence2 = new SyaEvidence("http://localhost/2", "photo.jpg", LocalDate.now());
        SyaEvidence evidence3 = new SyaEvidence("http://localhost/3", "report.png", LocalDate.now());
        SyaCaseWrapper appealDataWithEvidence = getSyaCaseWrapper();
        appealDataWithEvidence.getReasonsForAppealing()
                .setEvidences(Arrays.asList(evidence1, evidence2, evidence3));
        return appealDataWithEvidence;
    }
}
