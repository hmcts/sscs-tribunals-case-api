package uk.gov.hmcts.reform.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INCOMPLETE_APPLICATION_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SYA_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.pdf;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import java.time.LocalDate;
import java.util.Arrays;
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
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.document.EvidenceDownloadClientApi;
import uk.gov.hmcts.reform.sscs.document.EvidenceMetadataDownloadClientApi;
import uk.gov.hmcts.reform.sscs.domain.email.Email;
import uk.gov.hmcts.reform.sscs.domain.email.RoboticsEmailTemplate;
import uk.gov.hmcts.reform.sscs.domain.email.SubmitYourAppealEmailTemplate;
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
    private CitizenCcdService citizenCcdService;

    @Mock
    private CcdPdfService ccdPdfService;

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

    @Mock
    private RoboticsJsonUploadService roboticsJsonUploadService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> captor;

    @Captor
    private ArgumentCaptor<Email> emailCaptor;

    private SubmitYourAppealEmailTemplate submitYourAppealEmailTemplate;

    private SubmitAppealService submitAppealService;

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

    @Before
    public void setUp() {
        when(airLookupService.lookupRegionalCentre("CF10")).thenReturn("Cardiff");

        submitYourAppealEmailTemplate =
                new SubmitYourAppealEmailTemplate("from", "to", "message");

        RegionalProcessingCenterService regionalProcessingCenterService =
                new RegionalProcessingCenterService(airLookupService);
        regionalProcessingCenterService.init();

        ResponseEntity<Resource> mockResponseEntity = mock(ResponseEntity.class);
        ByteArrayResource stubbedResource = new ByteArrayResource(new byte[]{});
        when(mockResponseEntity.getBody()).thenReturn(stubbedResource);

        when(authTokenGenerator.generate()).thenReturn("token");
        when(evidenceDownloadClientApi.downloadBinary(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockResponseEntity);

        EvidenceManagementService evidenceManagementService =
                new EvidenceManagementService(authTokenGenerator, documentUploadClientApi, evidenceDownloadClientApi,
                        evidenceMetadataDownloadClientApi);

        SscsPdfService sscsPdfService = new SscsPdfService(TEMPLATE_PATH, pdfServiceClient, emailService,
                submitYourAppealEmailTemplate, ccdPdfService);

        RoboticsEmailTemplate roboticsEmailTemplate =
                new RoboticsEmailTemplate("from", "to", "message");

        RoboticsService roboticsService = new RoboticsService(airLookupService, emailService, roboticsJsonMapper,
                roboticsJsonValidator, roboticsEmailTemplate, roboticsJsonUploadService);

        submitAppealService = new SubmitAppealService(
                ccdService, citizenCcdService, sscsPdfService, roboticsService, regionalProcessingCenterService,
                idamService, evidenceManagementService);

        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().id(123L).build());

        given(citizenCcdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().id(123L).build());

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        given(emailService.generateUniqueEmailId(any(Appellant.class))).willReturn("Bloggs_33C");

        roboticsWrapper = RoboticsWrapper.builder().sscsCaseData(
                convertSyaToCcdCaseData(appealData)).ccdCaseId(123L).evidencePresent("No").build();

        given(roboticsJsonMapper.map(any())).willReturn(json);
    }

    @Test
    public void givenCaseDoesNotExistInCcd_shouldCreateCaseWithAppealDetailsWithAppealCreatedEvent() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData);

        verify(ccdService).createCase(any(SscsCaseData.class), eq(SYA_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnDateIsMissing_shouldCreateCaseWithAppealDetailsWithIncompleteApplicationEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(null);

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData);

        verify(ccdService).createCase(any(SscsCaseData.class), eq(INCOMPLETE_APPLICATION_RECEIVED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnDateIsGreaterThan13Months_shouldCreateCaseWithAppealDetailsWithNonCompliantReceivedEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(LocalDate.now().minusMonths(13).minusDays(1));

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData);

        verify(ccdService).createCase(any(SscsCaseData.class), eq(NON_COMPLIANT.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
    }

    @Test
    public void shouldCreateDraftCaseWithAppealDetailsWithDraftEvent() {
        submitAppealService.submitDraftAppeal("authorisation", appealData);

        verify(citizenCcdService).createCase(any(SscsCaseData.class), eq(DRAFT.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
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
    public void testPrepareCaseForCcd() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        SscsCaseData caseData = submitAppealService.prepareCaseForCcd(appealData, "CF10");
        assertEquals("CARDIFF", caseData.getRegion());
    }

    @Test
    public void shouldUpdateCcdWithPdf() {
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
        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class))).willReturn(SscsCaseDetails.builder().id(ccdId)
                .build());
        SyaCaseWrapper appealData = getSyaCaseWrapper("json/sya_with_evidence.json");

        roboticsWrapper = RoboticsWrapper.builder()
                .sscsCaseData(convertSyaToCcdCaseData(appealData)).ccdCaseId(987L).evidencePresent("Yes").build();

        submitAppealService.submitAppeal(appealData);

        verify(ccdPdfService).mergeDocIntoCcd(
                eq("Bloggs_33C.pdf"),
                any(),
                eq(ccdId),
                argThat(caseData -> caseData.getSscsDocument().size() == 2),
                any()
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

    @Test(expected = CcdException.class)
    public void givenExceptionWhenSearchingForCaseInCcd_shouldThrowException() {
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(SscsCaseData.class), any(IdamTokens.class)))
                .willThrow(RuntimeException.class);

        submitAppealService.submitAppeal(appealData);
    }

    @Test(expected = CcdException.class)
    public void givenCaseDoesNotExistInCcdAndGivenExceptionWhenCreatingCaseInCcd_shouldThrowException() {
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(SscsCaseData.class), any(IdamTokens.class)))
                .willReturn(null);

        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
                .willThrow(RuntimeException.class);

        submitAppealService.submitAppeal(appealData);
    }

    @Test
    public void givenCaseIsADuplicate_shouldNotResendEmails() {
        SscsCaseDetails duplicateCase = SscsCaseDetails.builder().build();
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(SscsCaseData.class), any(IdamTokens.class)))
                .willReturn(duplicateCase);

        submitAppealService.submitAppeal(appealData);

        then(pdfServiceClient).should(never()).generateFromHtml(any(byte[].class), anyMap());
    }

    @Test
    public void givenCaseAlreadyExistsInCcd_shouldNotCreateCaseWithAppealDetails() {
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any()))
                .willReturn(SscsCaseDetails.builder().build());

        roboticsWrapper = RoboticsWrapper.builder()
                .sscsCaseData(convertSyaToCcdCaseData(appealData)).ccdCaseId(null).evidencePresent("No").build();

        submitAppealService.submitAppeal(appealData);

        verify(ccdService, never()).createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class));
    }
}
