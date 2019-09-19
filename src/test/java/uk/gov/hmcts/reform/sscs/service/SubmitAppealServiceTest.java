package uk.gov.hmcts.reform.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.pdf;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import java.time.LocalDate;
import java.util.*;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.document.EvidenceDownloadClientApi;
import uk.gov.hmcts.reform.sscs.document.EvidenceMetadataDownloadClientApi;
import uk.gov.hmcts.reform.sscs.domain.email.Email;
import uk.gov.hmcts.reform.sscs.domain.email.SubmitYourAppealEmailTemplate;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaMrn;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAintoBService;

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

    @SuppressWarnings("PMD.UnusedPrivateField")
    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private IdamService idamService;

    @Captor
    private ArgumentCaptor<Email> emailCaptor;

    private SubmitYourAppealEmailTemplate submitYourAppealEmailTemplate;

    private SubmitAppealService submitAppealService;

    private final SyaCaseWrapper appealData = getSyaCaseWrapper();

    private final JSONObject json = new JSONObject();

    private final String userToken = null;

    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private DocumentUploadClientApi documentUploadClientApi;
    @Mock
    private EvidenceDownloadClientApi evidenceDownloadClientApi;
    @Mock
    private EvidenceMetadataDownloadClientApi evidenceMetadataDownloadClientApi;
    @Mock
    private ConvertAintoBService convertAintoBService;

    private List<String> offices;

    @Before
    public void setUp() {
        when(airLookupService.lookupRegionalCentre("CF10")).thenReturn("Cardiff");

        submitYourAppealEmailTemplate =
            new SubmitYourAppealEmailTemplate("from", "to", "message");

        RegionalProcessingCenterService regionalProcessingCenterService =
            new RegionalProcessingCenterService(airLookupService);
        regionalProcessingCenterService.init();

        offices = new ArrayList<>();
        offices.add("1");
        offices.add("Balham DRT");

        SscsPdfService sscsPdfService = new SscsPdfService(TEMPLATE_PATH, pdfServiceClient, emailService,
                submitYourAppealEmailTemplate, ccdPdfService);

        submitAppealService = new SubmitAppealService(
            ccdService, citizenCcdService, sscsPdfService, regionalProcessingCenterService,
            idamService, convertAintoBService, offices);

        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
            .willReturn(SscsCaseDetails.builder().id(123L).build());

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        given(emailService.generateUniqueEmailId(any(Appellant.class))).willReturn("Bloggs_33C");

    }

    @Test
    public void givenCaseDoesNotExistInCcd_shouldCreateCaseWithAppealDetailsWithAppealCreatedEventAndTriggerSentToDwpEvent() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(any(SscsCaseData.class), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        verify(ccdService).updateCase(any(SscsCaseData.class), eq(123L), eq(SEND_TO_DWP.getCcdType()), eq("Send to DWP"), eq("Send to DWP event has been triggered from Tribunals service"), any(IdamTokens.class));
    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnDateIsMissing_shouldCreateCaseWithAppealDetailsWithIncompleteApplicationEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(null);

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(any(SscsCaseData.class), eq(INCOMPLETE_APPLICATION_RECEIVED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        verify(ccdService, times(0)).updateCase(any(SscsCaseData.class), eq(123L), eq(SEND_TO_DWP.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnDateIsGreaterThan13Months_shouldCreateCaseWithAppealDetailsWithNonCompliantReceivedEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(LocalDate.now().minusMonths(13).minusDays(1));

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(any(SscsCaseData.class), eq(NON_COMPLIANT.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
    }

    @Test
    public void shouldCreateDraftCaseWithAppealDetailsWithDraftEvent() {
        given(citizenCcdService.saveCase(any(SscsCaseData.class), any(IdamTokens.class)))
            .willReturn(SaveCaseResult.builder()
                .caseDetailsId(123L)
                .saveCaseOperation(SaveCaseOperation.CREATE)
                .build());

        submitAppealService.submitDraftAppeal("authorisation", appealData);

        verify(citizenCcdService).saveCase(any(SscsCaseData.class), any(IdamTokens.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldGetADraftIfItExists() {
        when(citizenCcdService.findCase(any())).thenReturn(Collections.singletonList(SscsCaseData.builder().build()));
        when(convertAintoBService.convert(any(SscsCaseData.class))).thenReturn(SessionDraft.builder().build());
        Optional<SessionDraft> optionalSessionDraft = submitAppealService.getDraftAppeal("authorisation");
        assertTrue(optionalSessionDraft.isPresent());
    }

    @Test
    public void shouldGetNoDraftIfNoneExists() {
        when(citizenCcdService.findCase(any())).thenReturn(Collections.emptyList());
        Optional<SessionDraft> optionalSessionDraft = submitAppealService.getDraftAppeal("authorisation");
        assertFalse(optionalSessionDraft.isPresent());
    }

    @Test
    public void shouldCreatePdfWithAppealDetails() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
            any())).willReturn(expected);

        submitAppealService.submitAppeal(appealData, userToken);

        Email expectedEmail = submitYourAppealEmailTemplate.generateEmail(
            "Bloggs_33C",
            newArrayList(pdf(expected, "Bloggs_33C.pdf"))
        );
        verify(emailService).sendEmail(expectedEmail);
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
    public void givenAPipCaseWithReadyToListOffice_thenSetCreatedInGapsFromFieldToReadyToList() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        SyaBenefitType syaBenefitType = new SyaBenefitType("PIP", "PIP");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("1");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.prepareCaseForCcd(appealData, "CF10");
        assertEquals(READY_TO_LIST.name(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void givenAPipCaseWithValidAppealOffice_thenSetCreatedInGapsFromFieldToValidAppeal() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        SyaBenefitType syaBenefitType = new SyaBenefitType("PIP", "PIP");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("2");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.prepareCaseForCcd(appealData, "CF10");
        assertEquals(State.VALID_APPEAL.name(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void givenAEsaCaseWithReadyToListOffice_thenSetCreatedInGapsFromToReadyToList() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        SyaBenefitType syaBenefitType = new SyaBenefitType("ESA", "ESA");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("Balham DRT");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.prepareCaseForCcd(appealData, "CF10");
        assertEquals(READY_TO_LIST.name(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void givenAEsaCaseWithValidAppealOffice_thenSetCreatedInGapsFromFieldToValidAppeal() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        SyaBenefitType syaBenefitType = new SyaBenefitType("ESA", "ESA");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("Chesterfield DRT");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.prepareCaseForCcd(appealData, "CF10");
        assertEquals(VALID_APPEAL.name(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void shouldUpdateCcdWithPdf() {
        Document stubbedDocument = new Document();
        Document.Link stubbedLink = new Document.Link();
        stubbedLink.href = "http://localhost:4506/documents/eb8cbfaa-37c3-4644-aa77-b9a2e2c72332";
        Document.Links stubbedLinks = new Document.Links();
        stubbedLinks.binary = stubbedLink;
        stubbedDocument.links = stubbedLinks;

        byte[] expected = {1, 2, 3};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
            any(Map.class))).willReturn(expected);
        long ccdId = 987L;
        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class))).willReturn(SscsCaseDetails.builder().id(ccdId)
            .build());
        SyaCaseWrapper appealData = getSyaCaseWrapper("json/sya_with_evidence.json");

        submitAppealService.submitAppeal(appealData, null);

        verify(ccdPdfService).mergeDocIntoCcd(
            eq("Bloggs_33C.pdf"),
            any(),
            eq(ccdId),
            argThat(caseData -> caseData.getSscsDocument().size() == 2),
            any(),
            eq("sscs1")
        );
    }

    @Test(expected = CcdException.class)
    public void givenExceptionWhenSearchingForCaseInCcd_shouldThrowException() {
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(SscsCaseData.class), any(IdamTokens.class)))
            .willThrow(RuntimeException.class);

        submitAppealService.submitAppeal(appealData, userToken);
    }

    @Test(expected = CcdException.class)
    public void givenCaseDoesNotExistInCcdAndGivenExceptionWhenCreatingCaseInCcd_shouldThrowException() {
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(SscsCaseData.class), any(IdamTokens.class)))
            .willReturn(null);

        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
            .willThrow(RuntimeException.class);

        submitAppealService.submitAppeal(appealData, userToken);
    }

    @Test
    public void givenCaseIsADuplicate_shouldNotResendEmails() {
        SscsCaseDetails duplicateCase = SscsCaseDetails.builder().build();
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(SscsCaseData.class), any(IdamTokens.class)))
            .willReturn(duplicateCase);

        submitAppealService.submitAppeal(appealData, userToken);

        then(pdfServiceClient).should(never()).generateFromHtml(any(byte[].class), anyMap());
    }

    @Test
    public void givenCaseAlreadyExistsInCcd_shouldNotCreateCaseWithAppealDetails() {
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any()))
            .willReturn(SscsCaseDetails.builder().build());

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService, never()).createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class));
    }

    @Test
    public void willArchiveADraftOnceAppealIsSubmitted() {
        String userToken = "MyCitizenToken";
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(any(SscsCaseData.class), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        verify(citizenCcdService).draftArchived(any(SscsCaseData.class), any(IdamTokens.class), any(IdamTokens.class));


    }

    @Test
    public void addAssociatedCases() {
        SscsCaseDetails matchingCase = SscsCaseDetails.builder().id(12345678L).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase);

        SscsCaseData caseData = submitAppealService.addAssociatedCases(
            SscsCaseData.builder().caseReference("00000000").build(),
            matchedByNinoCases);

        assertEquals(1, caseData.getAssociatedCase().size());
    }

    @Test
    public void getMatchedCases() {
        given(ccdService.findCaseBy(any(),any())).willReturn(Arrays.asList(
                SscsCaseDetails.builder().id(12345678L).build()
        ));
        List<SscsCaseDetails> matchedCases = submitAppealService.getMatchedCases("ABCDEFG", idamService.getIdamTokens());

        assertEquals(1, matchedCases.size());
    }
}
