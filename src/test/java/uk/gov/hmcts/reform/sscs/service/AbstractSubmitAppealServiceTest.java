package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.apache.http.HttpStatus;
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
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaAppointee;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaContactDetails;
import uk.gov.hmcts.reform.sscs.exception.ApplicationErrorException;
import uk.gov.hmcts.reform.sscs.helper.EmailHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAIntoBService;
import uk.gov.hmcts.reform.sscs.service.v2.SubmitAppealService;

@RunWith(JUnitParamsRunner.class)
public abstract class AbstractSubmitAppealServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    @Mock
    protected CcdService ccdService;

    @Mock
    private CitizenCcdService citizenCcdService;

    @Mock
    protected RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private IdamService idamService;

    @Mock
    private ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService;

    @Mock
    protected AirLookupService airLookupService;

    @Mock
    protected RefDataService refDataService;

    @Mock
    protected VenueService venueService;

    @Mock
    protected PDFServiceClient pdfServiceClient;

    @Mock
    private EmailHelper emailHelper;

    @Mock
    protected UpdateCcdCaseService updateCcdCaseService;

    protected uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService;
    protected SubmitAppealService submitAppealServiceV2;

    protected final SyaCaseWrapper appealData = getSyaCaseWrapper();

    protected final String userToken = "user token";


    @Captor
    protected ArgumentCaptor<SscsCaseData> capture;

    public static final String BIRMINGHAM_RPC = """
            {
                "name" : "BIRMINGHAM",
                "address1" : "HM Courts & Tribunals Service",
                "address2" : "Social Security & Child Support Appeals",
                "address3" : "Administrative Support Centre",
                "address4" : "PO Box 14620",
                "city" : "BIRMINGHAM",
                "postcode" : "B16 6FR",
                "phoneNumber" : "0300 123 1142",
                "faxNumber" : "0126 434 7983",
                "email" : "Birmingham-SYA-Receipts@justice.gov.uk",
                "hearingRoute" : "gaps",
                "epimsId" : "815833"
              }""";

    public static final String BRADFORD_RPC = """
            {
                "name" : "BRADFORD",
                "address1": "HM Courts & Tribunals Service",
                "address2": "Social Security & Child Support Appeals",
                "address3": "Phoenix House",
                "address4": "Rushton Avenue",
                "city": "BRADFORD",
                "postcode": "BD3 7BH",
                "phoneNumber" : "0300 123 1142",
                "faxNumber" : "0126 434 7983",
                "email" : "SSCS_Bradford@justice.gov.uk",
                "hearingRoute" : "gaps",
                "epimsId" : "698118"
              }""";

    public static final String SUTTON_RPC = """
            {
                "name" : "SUTTON",
                "address1" : "HM Courts & Tribunals Service",
                "address2" : "Social Security & Child Support Appeals",
                "address3" : "Copthall House",
                "address4" : "9 The Pavement, Grove Road",
                "city" : "SUTTON",
                "postcode" : "SM1 1DA",
                "phoneNumber" : "0300 123 1142",
                "faxNumber" : "0870 739 4229",
                "email" : "Sutton_SYA_Respons@justice.gov.uk",
                "hearingRoute" : "gaps",
                "epimsId" : "37792"
              }""";

    @Before
    public void setup() {
        appealData.getMrn().setDate(LocalDate.now().minusMonths(1));

        submitAppealService = new uk.gov.hmcts.reform.sscs.service.SubmitAppealService(
                ccdService,
                citizenCcdService,
                regionalProcessingCenterService,
                idamService,
                convertAIntoBService,
                airLookupService,
                refDataService,
                venueService,
                true);

        submitAppealServiceV2 = new SubmitAppealService(
                ccdService,
                citizenCcdService,
                regionalProcessingCenterService,
                idamService,
                convertAIntoBService,
                airLookupService,
                refDataService,
                venueService,
                updateCcdCaseService,
                true);

        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().id(123L).build());

        given(ccdService.updateCase(any(SscsCaseData.class), any(), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().id(123L).build());

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().roles(List.of("citizen")).build());
        given(emailHelper.generateUniqueEmailId(any(Appellant.class))).willReturn("Bloggs_33C");
    }


    public abstract void givenSaveCaseWillReturnSaveCaseOperation(CitizenCcdService citizenCcdService,
                                                                  Long caseDetailsId,
                                                                  SaveCaseOperation saveCaseOperation);

    public abstract void givenSaveCaseWillThrow(CitizenCcdService citizenCcdService,
                                                FeignException feignException);

    public abstract void verifyCitizenCcdService(CitizenCcdService citizenCcdService);

    public abstract Optional<SaveCaseResult> callSubmitDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService,
                                                                   SubmitAppealService submitAppealServiceV2,
                                                                   String auth2Token,
                                                                   SyaCaseWrapper appealData,
                                                                   boolean forceCreate);

    public abstract Optional<SaveCaseResult> callArchiveDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService,
                                                                    SubmitAppealService submitAppealServiceV2,
                                                                    String auth2Token,
                                                                    SyaCaseWrapper appealData,
                                                                    Long caseId);

    public abstract void verifyArchiveDraftAppeal(CitizenCcdService citizenCcdService);

    public abstract void givenArchiveDraftAppealWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails);

    public abstract void givenArchiveDraftAppealWillThrow(CitizenCcdService citizenCcdService, FeignException feignException);

    public abstract void verifyArchiveDraft(CitizenCcdService citizenCcdService);

    @Test
    public void shouldCreateDraftCaseWithAppealDetailsWithDraftEventNotForcedCreate() {
        givenSaveCaseWillReturnSaveCaseOperation(citizenCcdService, 123L, SaveCaseOperation.CREATE);

        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData, false);

        verifyCitizenCcdService(citizenCcdService);
        assertTrue(result.isPresent());

    }

    public abstract void givenUpdateCaseWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails);

    public abstract void givenUpdateCaseWillThrowException(CitizenCcdService citizenCcdService, FeignException feignException);

    public abstract Optional<SaveCaseResult> callUpdateDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService, SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData);

    public abstract void verifyUpdateCaseCalledByUpdateDraftAppeal(CitizenCcdService citizenCcdService);

    @Test
    public void shouldUpdateDraftCase() {

        uk.gov.hmcts.reform.ccd.client.model.CaseDetails caseDetails =
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().id(12L).build();

        givenUpdateCaseWillReturnCaseDetails(citizenCcdService, caseDetails);

        SyaCaseWrapper caseWrapper = getSyaCaseWrapper("json/sya_with_ccdId.json");
        Optional<SaveCaseResult> result = callUpdateDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", caseWrapper);

        verifyUpdateCaseCalledByUpdateDraftAppeal(citizenCcdService);
        assertTrue(result.isPresent());
    }

    @Test(expected = FeignException.class)
    public void shouldRaiseExceptionOnUpdateDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(404);
        givenUpdateCaseWillThrowException(citizenCcdService, feignException);

        Optional<SaveCaseResult> result = callUpdateDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData);

        verifyArchiveDraftAppeal(citizenCcdService);
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldSuppressExceptionIfIts409OnUpdateDraftCaseWithAppealDetailsWithDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(409);
        givenUpdateCaseWillThrowException(citizenCcdService, feignException);

        Optional<SaveCaseResult> result = callUpdateDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData);

        verifyUpdateCaseCalledByUpdateDraftAppeal(citizenCcdService);
        assertFalse(result.isPresent());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldRaisedExceptionOnUpdateDraftWhenCitizenRoleIsNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        Optional<SaveCaseResult> result = callUpdateDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData);

        assertFalse(result.isPresent());
    }

    @Test
    public void shouldArchiveDraftCase() {
        givenArchiveDraftAppealWillReturnCaseDetails(citizenCcdService, CaseDetails.builder().build());

        Optional<SaveCaseResult> result = callArchiveDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData, 112L);

        verifyArchiveDraft(citizenCcdService);
        assertTrue(result.isPresent());
    }

    @Test(expected = FeignException.class)
    public void shouldRaiseExceptionOnArchiveDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(404);
        givenArchiveDraftAppealWillThrow(citizenCcdService, feignException);

        Optional<SaveCaseResult> result = callArchiveDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData, 112L);

        verifyArchiveDraftAppeal(citizenCcdService);
        assertFalse(result.isPresent());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldRaisedExceptionOnArchiveDraftWhenCitizenRoleIsNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        Optional<SaveCaseResult> result = callArchiveDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData, 121L);

        assertFalse(result.isPresent());
    }

    @Test
    public void shouldCreateDraftCaseWithAppealDetailsWithDraftEventForcedCreate() {
        given(citizenCcdService.createDraft(any(SscsCaseData.class), any(IdamTokens.class)))
                .willReturn(SaveCaseResult.builder()
                        .caseDetailsId(123L)
                        .saveCaseOperation(SaveCaseOperation.CREATE)
                        .build());

        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData, true);

        verify(citizenCcdService).createDraft(any(SscsCaseData.class), any(IdamTokens.class));
        assertTrue(result.isPresent());

    }

    @Test(expected = FeignException.class)
    public void shouldRaisedExceptionOnCreateDraftCaseWithAppealDetailsWithDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(404);
        givenSaveCaseWillThrow(citizenCcdService, feignException);

        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData, false);

        verifyCitizenCcdService(citizenCcdService);
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldHandleScConflictWithNullNinoForSubmitDraftAppeal() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(HttpStatus.SC_CONFLICT);
        givenSaveCaseWillThrow(citizenCcdService, feignException);
        appealData.getAppellant().setNino(null);


        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData, false);

        verifyCitizenCcdService(citizenCcdService);
        assertEquals(result, Optional.empty());
    }

    @Test
    public void shouldHandleScConflictWithNullNinoForUpdateDraftAppeal() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(409);
        givenUpdateCaseWillThrowException(citizenCcdService, feignException);
        appealData.getAppellant().setNino(null);

        Optional<SaveCaseResult> result = callUpdateDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData);

        verifyUpdateCaseCalledByUpdateDraftAppeal(citizenCcdService);
        assertEquals(result, Optional.empty());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldRaisedExceptionOnCreateDraftWhenCitizenRoleIsNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData, false);

        assertFalse(result.isPresent());
    }

    @Test
    public void shouldSuppressExceptionIfIts409OnCreateDraftCaseWithAppealDetailsWithDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(409);
        givenSaveCaseWillThrow(citizenCcdService, feignException);

        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealService, submitAppealServiceV2, "authorisation", appealData, false);

        verifyCitizenCcdService(citizenCcdService);
        assertFalse(result.isPresent());
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

    @Test(expected = ApplicationErrorException.class)
    public void shouldThrowExceptionOnGetDraftWhenCitizenRoleNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        submitAppealService.getDraftAppeal("authorisation");
    }

    @Test
    public void shouldGetAllDraftsIfItExists() {

        SscsCaseData caseData =  SscsCaseData.builder().build();

        when(citizenCcdService.findCase(any())).thenReturn(List.of(caseData, caseData, caseData));
        when(convertAIntoBService.convert(any(SscsCaseData.class))).thenReturn(SessionDraft.builder().build());
        List<SessionDraft> sessionDrafts = submitAppealService.getDraftAppeals("authorisation");
        assertEquals(3, sessionDrafts.size());
    }

    @Test
    public void shouldGetEmptyListIfNoDraftsExists() {
        when(citizenCcdService.findCase(any())).thenReturn(Collections.emptyList());
        List<SessionDraft> sessionDrafts = submitAppealService.getDraftAppeals("authorisation");
        assertEquals(0, sessionDrafts.size());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldThrowExceptionOnGetDraftsWhenCitizenRoleNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        submitAppealService.getDraftAppeals("authorisation");
    }

    protected SyaCaseWrapper getSyaWrapperWithAppointee(SyaContactDetails appointeeContact) {
        SyaAppointee appointee = new SyaAppointee();
        appointee.setContactDetails(appointeeContact);

        appealData.getAppellant().getContactDetails().setPostCode("TN32 6PL");
        appealData.setAppointee(appointee);
        appealData.setIsAppointee(true);

        return appealData;
    }

    protected RegionalProcessingCenter getRpcObjectForGivenJsonRpc(String jsonRpc) throws JsonProcessingException {
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

    public Object[] generateDifferentRpcScenariosIba() {
        return new Object[]{
            new Object[]{BRADFORD_RPC, "GB000084"},
            new Object[]{BRADFORD_RPC, "GB003090"}
        };
    }

    @Test
    public void getMatchedCases() {
        given(ccdService.findCaseBy(any(), any(), any())).willReturn(Collections.singletonList(
                SscsCaseDetails.builder().id(12345678L).build()
        ));
        List<SscsCaseDetails> matchedCases = submitAppealService.getMatchedCases("ABCDEFG", idamService.getIdamTokens());

        assertEquals(1, matchedCases.size());
    }

    protected void assertRpc(SscsCaseData caseData, String expectedRpc) throws JsonProcessingException {
        RegionalProcessingCenter actualRpc = caseData.getRegionalProcessingCenter();
        RegionalProcessingCenter expectedRpcObject = getRpcObjectForGivenJsonRpc(expectedRpc);
        assertThat(actualRpc)
                .usingRecursiveComparison()
                .ignoringFields("hearingRoute","epimsId")
                .isEqualTo(expectedRpcObject);
        assertThat(actualRpc)
                .extracting("hearingRoute","epimsId")
                .doesNotContainNull();

        assertEquals(expectedRpcObject.getName(), caseData.getRegion());
    }
}
