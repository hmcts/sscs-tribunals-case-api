package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaMrn;
import uk.gov.hmcts.reform.sscs.helper.EmailHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAIntoBService;

@RunWith(JUnitParamsRunner.class)
public class SubmitAppealServiceTest {
    private static final String TEMPLATE_PATH = "/templates/appellant_appeal_template.html";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    @Mock
    private CcdService ccdService;

    @Mock
    private CitizenCcdService citizenCcdService;

    @Mock
    private CcdPdfService ccdPdfService;

    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    private EmailHelper emailHelper;

    @Mock
    private IdamService idamService;

    private SubmitAppealService submitAppealService;

    private final SyaCaseWrapper appealData = getSyaCaseWrapper();

    private final String userToken = "user token";

    @Captor
    private ArgumentCaptor<SscsCaseData> capture;

    @Mock
    private ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService;

    private static final RegionalProcessingCenterService regionalProcessingCenterService;

    private SscsPdfService sscsPdfService;

    static {
        AirLookupService airLookupService = new AirLookupService();
        airLookupService.init();
        regionalProcessingCenterService = new RegionalProcessingCenterService(airLookupService);
        regionalProcessingCenterService.init();
    }

    public static final String BIRMINGHAM_RPC = "{\n"
        + "    \"name\" : \"BIRMINGHAM\",\n"
        + "    \"address1\" : \"HM Courts & Tribunals Service\",\n"
        + "    \"address2\" : \"Social Security & Child Support Appeals\",\n"
        + "    \"address3\" : \"Administrative Support Centre\",\n"
        + "    \"address4\" : \"PO Box 14620\",\n"
        + "    \"city\" : \"BIRMINGHAM\",\n"
        + "    \"postcode\" : \"B16 6FR\",\n"
        + "    \"phoneNumber\" : \"0300 123 1142\",\n"
        + "    \"faxNumber\" : \"0126 434 7983\",\n"
        + "    \"email\" : \"Birmingham-SYA-Receipts@justice.gov.uk\"\n"
        + "  }";

    public static final String BRADFORD_RPC = "{\n"
        + "    \"name\" : \"BRADFORD\",\n"
        + "    \"address1\": \"HM Courts & Tribunals Service\",\n"
        + "    \"address2\": \"Social Security & Child Support Appeals\",\n"
        + "    \"address3\": \"Phoenix House\",\n"
        + "    \"address4\": \"Rushton Avenue\",\n"
        + "    \"city\": \"BRADFORD\",\n"
        + "    \"postcode\": \"BD3 7BH\",\n"
        + "    \"phoneNumber\" : \"0300 123 1142\",\n"
        + "    \"faxNumber\" : \"0126 434 7983\",\n"
        + "    \"email\" : \"SSCS_Bradford@justice.gov.uk\"\n"
        + "  }";

    public static final String SUTTON_RPC = "{\n"
        + "    \"name\" : \"SUTTON\",\n"
        + "    \"address1\" : \"HM Courts & Tribunals Service\",\n"
        + "    \"address2\" : \"Social Security & Child Support Appeals\",\n"
        + "    \"address3\" : \"Copthall House\",\n"
        + "    \"address4\" : \"9 The Pavement, Grove Road\",\n"
        + "    \"city\" : \"SUTTON\",\n"
        + "    \"postcode\" : \"SM1 1DA\",\n"
        + "    \"phoneNumber\" : \"0300 123 1142\",\n"
        + "    \"faxNumber\" : \"0870 739 4229\",\n"
        + "    \"email\" : \"Sutton_SYA_Respons@justice.gov.uk\"\n"
        + "  }";

    private List<String> offices;

    @Before
    public void setUp() {
        offices = new ArrayList<>();
        offices.add("DWP PIP (1)");
        offices.add("Balham DRT");
        offices.add("Watford DRT");
        offices.add("Sheffield DRT");

        sscsPdfService = new SscsPdfService(TEMPLATE_PATH, pdfServiceClient, ccdPdfService);

        submitAppealService = new SubmitAppealService(
            ccdService, citizenCcdService, sscsPdfService, regionalProcessingCenterService,
            idamService, convertAIntoBService, offices);

        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
            .willReturn(SscsCaseDetails.builder().id(123L).build());

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build());
        given(emailHelper.generateUniqueEmailId(any(Appellant.class))).willReturn("Bloggs_33C");

    }

    @Test
    public void givenCaseDoesNotExistInCcd_shouldCreateCaseWithAppealDetailsWithValidAppealCreatedEvent() {
        submitAppealService = new SubmitAppealService(
                ccdService, citizenCcdService, sscsPdfService, regionalProcessingCenterService,
                idamService, convertAIntoBService, offices);

        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(any(SscsCaseData.class), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
    }

    @Test
    public void givenAssociatedCaseAlreadyExistsInCcd_shouldCreateCaseWithAppealDetailsAndAssociatedCase() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCaseBy(any(), any())).willReturn(Collections.singletonList(
                SscsCaseDetails.builder().id(12345678L).build()
        ));

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals(1, capture.getValue().getAssociatedCase().size());
        assertEquals("12345678", capture.getValue().getAssociatedCase().get(0).getValue().getCaseReference());
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

        Optional<SaveCaseResult> result = submitAppealService.submitDraftAppeal("authorisation", appealData);

        verify(citizenCcdService).saveCase(any(SscsCaseData.class), any(IdamTokens.class));
        Assert.assertTrue(result.isPresent());

    }

    @Test(expected = FeignException.class)
    public void shouldRaisedExceptionOnCreateDraftCaseWithAppealDetailsWithDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(404);
        given(citizenCcdService.saveCase(any(SscsCaseData.class), any(IdamTokens.class)))
            .willThrow(feignException);

        Optional<SaveCaseResult> result = submitAppealService.submitDraftAppeal("authorisation", appealData);

        verify(citizenCcdService).saveCase(any(SscsCaseData.class), any(IdamTokens.class));
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void shouldSuppressExceptionIfIts409OnCreateDraftCaseWithAppealDetailsWithDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(409);
        given(citizenCcdService.saveCase(any(SscsCaseData.class), any(IdamTokens.class)))
            .willThrow(feignException);

        Optional<SaveCaseResult> result = submitAppealService.submitDraftAppeal("authorisation", appealData);

        verify(citizenCcdService).saveCase(any(SscsCaseData.class), any(IdamTokens.class));
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void shouldGetADraftIfItExists() {
        when(citizenCcdService.findCase(any())).thenReturn(Collections.singletonList(SscsCaseData.builder().build()));
        when(convertAIntoBService.convert(any(SscsCaseData.class))).thenReturn(SessionDraft.builder().build());
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
    @Parameters(method = "generateDifferentRpcScenarios")
    public void givenAppellantPostCode_shouldSetRegionAndRpcCorrectly(String expectedRpc, String appellantPostCode)
        throws JsonProcessingException {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        appealData.getAppellant().getContactDetails().setPostCode(appellantPostCode);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);

        RegionalProcessingCenter actualRpc = caseData.getRegionalProcessingCenter();
        RegionalProcessingCenter expectedRpcObject = getRpcObjectForGivenJsonRpc(expectedRpc);
        assertThat(actualRpc, is(expectedRpcObject));
        assertEquals(expectedRpcObject.getName(), caseData.getRegion());
    }

    private RegionalProcessingCenter getRpcObjectForGivenJsonRpc(String jsonRpc) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonRpc, RegionalProcessingCenter.class);
    }

    public Object[] generateDifferentRpcScenarios() {
        return new Object[]{
            new Object[]{BRADFORD_RPC, "TN32 6PL"},
            new Object[]{BRADFORD_RPC, "OX1 1AE"},
            new Object[]{BIRMINGHAM_RPC, "B1 1AA"},
            new Object[]{SUTTON_RPC, "EN1 1AA"},
            new Object[]{SUTTON_RPC, "KT19 0SZ"},
            new Object[]{BIRMINGHAM_RPC, "DE23 2PD"}
        };
    }

    @Test
    public void givenAPipCaseWithReadyToListOffice_thenSetCreatedInGapsFromFieldToReadyToList() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        SyaBenefitType syaBenefitType = new SyaBenefitType("PIP", "PIP");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("1");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);
        assertEquals(READY_TO_LIST.getId(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void givenAPipCaseWithValidAppealOffice_thenSetCreatedInGapsFromFieldToValidAppeal() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        SyaBenefitType syaBenefitType = new SyaBenefitType("PIP", "PIP");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("2");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);
        assertEquals(State.VALID_APPEAL.getId(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void givenAEsaCaseWithReadyToListOffice_thenSetCreatedInGapsFromToReadyToList() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        SyaBenefitType syaBenefitType = new SyaBenefitType("ESA", "ESA");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("Watford DRT");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);
        assertEquals(READY_TO_LIST.getId(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void givenAEsaCaseWithValidAppealOffice_thenSetCreatedInGapsFromFieldToValidAppeal() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        SyaBenefitType syaBenefitType = new SyaBenefitType("ESA", "ESA");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("Chesterfield DRT");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);
        assertEquals(State.VALID_APPEAL.getId(), caseData.getCreatedInGapsFrom());
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
    public void givenAssociatedCase_thenAddAssociatedCaseLinkToCase() {
        SscsCaseDetails matchingCase = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase);

        SscsCaseData caseData = submitAppealService.addAssociatedCases(
            SscsCaseData.builder().ccdCaseId("00000000").build(),
            matchedByNinoCases);

        assertEquals(1, caseData.getAssociatedCase().size());
        assertEquals("Yes", caseData.getLinkedCasesBoolean());
        assertEquals("12345678", caseData.getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void givenMultipleAssociatedCases_thenAddAllAssociatedCaseLinksToCase() {
        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);

        SscsCaseData caseData = submitAppealService.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("00000000").build(),
                matchedByNinoCases);

        assertEquals(2, caseData.getAssociatedCase().size());
        assertEquals("Yes", caseData.getLinkedCasesBoolean());
        assertEquals("12345678", caseData.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("56765676", caseData.getAssociatedCase().get(1).getValue().getCaseReference());
    }

    @Test
    public void addNoAssociatedCases() {
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();

        SscsCaseData caseData = submitAppealService.addAssociatedCases(
            SscsCaseData.builder().ccdCaseId("00000000").build(),
            matchedByNinoCases);

        assertNull(caseData.getAssociatedCase());
        assertEquals("No", caseData.getLinkedCasesBoolean());
        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }

    @Test
    public void getMatchedCases() {
        given(ccdService.findCaseBy(any(), any())).willReturn(Collections.singletonList(
            SscsCaseDetails.builder().id(12345678L).build()
        ));
        List<SscsCaseDetails> matchedCases = submitAppealService.getMatchedCases("ABCDEFG", idamService.getIdamTokens());

        assertEquals(1, matchedCases.size());
    }
}
