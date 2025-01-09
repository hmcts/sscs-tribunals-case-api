package uk.gov.hmcts.reform.sscs.service.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ASSOCIATE_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_INCOMPLETE_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INCOMPLETE_APPLICATION_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaAppointee;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaContactDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaMrn;
import uk.gov.hmcts.reform.sscs.exception.ApplicationErrorException;
import uk.gov.hmcts.reform.sscs.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.sscs.helper.EmailHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAIntoBService;

@RunWith(JUnitParamsRunner.class)
public class SubmitAppealServiceTestV2 {

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

    @Captor
    ArgumentCaptor<UnaryOperator<SscsCaseDetails>> unaryOperatorArgumentCaptor;

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


    public void givenSaveCaseWillReturnSaveCaseOperation(CitizenCcdService citizenCcdService, Long caseDetailsId, SaveCaseOperation saveCaseOperation) {
        given(citizenCcdService.saveCaseV2(any(IdamTokens.class), any(Consumer.class)))
                .willReturn(SaveCaseResult.builder()
                        .caseDetailsId(caseDetailsId)
                        .saveCaseOperation(saveCaseOperation)
                        .build());
    }

    public void givenSaveCaseWillThrow(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.saveCaseV2(any(IdamTokens.class), any(Consumer.class)))
                .willThrow(feignException);
    }

    public void verifyCitizenCcdService(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).saveCaseV2(any(IdamTokens.class), any(Consumer.class));
    }

    public Optional<SaveCaseResult> callSubmitDraftAppeal(SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData, boolean forceCreate) {
        return submitAppealServiceV2.submitDraftAppeal(auth2Token, appealData, forceCreate);
    }

    public void givenUpdateCaseWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails) {
        given(citizenCcdService.updateCaseCitizenV2(any(String.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(UnaryOperator.class)))
                .willReturn(caseDetails);
    }

    public void givenUpdateCaseWillThrowException(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.updateCaseCitizenV2(any(), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(UnaryOperator.class)))
                .willThrow(feignException);
    }

    public Optional<SaveCaseResult> callUpdateDraftAppeal(SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData) {
        return submitAppealServiceV2.updateDraftAppeal(auth2Token, appealData);
    }

    public void verifyUpdateCaseCalledByUpdateDraftAppeal(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).updateCaseCitizenV2(any(), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(UnaryOperator.class));
    }

    public Optional<SaveCaseResult> callArchiveDraftAppeal(SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData, Long caseId) {
        return submitAppealServiceV2.archiveDraftAppeal(auth2Token, appealData, caseId);
    }

    public void verifyArchiveDraftAppeal(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class));
    }

    public void givenArchiveDraftAppealWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails) {
        given(citizenCcdService.archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class)))
                .willReturn(caseDetails);
    }

    public void givenArchiveDraftAppealWillThrow(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class)))
                .willThrow(feignException);
    }

    public void verifyArchiveDraft(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class));
    }

    @Test
    public void shouldCreateDraftCaseWithAppealDetailsWithDraftEventNotForcedCreate() {
        givenSaveCaseWillReturnSaveCaseOperation(citizenCcdService, 123L, SaveCaseOperation.CREATE);

        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealServiceV2, "authorisation", appealData, false);

        verifyCitizenCcdService(citizenCcdService);
        assertTrue(result.isPresent());

    }

    @Test
    public void shouldUpdateDraftCase() {

        uk.gov.hmcts.reform.ccd.client.model.CaseDetails caseDetails =
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().id(12L).build();

        givenUpdateCaseWillReturnCaseDetails(citizenCcdService, caseDetails);

        SyaCaseWrapper caseWrapper = getSyaCaseWrapper("json/sya_with_ccdId.json");
        Optional<SaveCaseResult> result = callUpdateDraftAppeal(submitAppealServiceV2, "authorisation", caseWrapper);

        verifyUpdateCaseCalledByUpdateDraftAppeal(citizenCcdService);
        assertTrue(result.isPresent());
    }

    @Test(expected = FeignException.class)
    public void shouldRaiseExceptionOnUpdateDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(404);
        givenUpdateCaseWillThrowException(citizenCcdService, feignException);

        Optional<SaveCaseResult> result = callUpdateDraftAppeal(submitAppealServiceV2, "authorisation", appealData);

        verifyArchiveDraftAppeal(citizenCcdService);
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldSuppressExceptionIfIts409OnUpdateDraftCaseWithAppealDetailsWithDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(409);
        givenUpdateCaseWillThrowException(citizenCcdService, feignException);

        Optional<SaveCaseResult> result = callUpdateDraftAppeal(submitAppealServiceV2, "authorisation", appealData);

        verifyUpdateCaseCalledByUpdateDraftAppeal(citizenCcdService);
        assertFalse(result.isPresent());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldRaisedExceptionOnUpdateDraftWhenCitizenRoleIsNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        Optional<SaveCaseResult> result = callUpdateDraftAppeal(submitAppealServiceV2, "authorisation", appealData);

        assertFalse(result.isPresent());
    }

    @Test
    public void shouldArchiveDraftCase() {
        givenArchiveDraftAppealWillReturnCaseDetails(citizenCcdService, CaseDetails.builder().build());

        Optional<SaveCaseResult> result = callArchiveDraftAppeal(submitAppealServiceV2, "authorisation", appealData, 112L);

        verifyArchiveDraft(citizenCcdService);
        assertTrue(result.isPresent());
    }

    @Test(expected = FeignException.class)
    public void shouldRaiseExceptionOnArchiveDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(404);
        givenArchiveDraftAppealWillThrow(citizenCcdService, feignException);

        Optional<SaveCaseResult> result = callArchiveDraftAppeal(submitAppealServiceV2, "authorisation", appealData, 112L);

        verifyArchiveDraftAppeal(citizenCcdService);
        assertFalse(result.isPresent());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldRaisedExceptionOnArchiveDraftWhenCitizenRoleIsNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        Optional<SaveCaseResult> result = callArchiveDraftAppeal(submitAppealServiceV2, "authorisation", appealData, 121L);

        assertFalse(result.isPresent());
    }

    @Test
    public void shouldCreateDraftCaseWithAppealDetailsWithDraftEventForcedCreate() {
        given(citizenCcdService.createDraft(any(SscsCaseData.class), any(IdamTokens.class)))
                .willReturn(SaveCaseResult.builder()
                        .caseDetailsId(123L)
                        .saveCaseOperation(SaveCaseOperation.CREATE)
                        .build());

        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealServiceV2, "authorisation", appealData, true);

        verify(citizenCcdService).createDraft(any(SscsCaseData.class), any(IdamTokens.class));
        assertTrue(result.isPresent());

    }

    @Test(expected = FeignException.class)
    public void shouldRaisedExceptionOnCreateDraftCaseWithAppealDetailsWithDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(404);
        givenSaveCaseWillThrow(citizenCcdService, feignException);

        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealServiceV2, "authorisation", appealData, false);

        verifyCitizenCcdService(citizenCcdService);
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldHandleScConflictWithNullNinoForSubmitDraftAppeal() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(HttpStatus.SC_CONFLICT);
        givenSaveCaseWillThrow(citizenCcdService, feignException);
        appealData.getAppellant().setNino(null);


        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealServiceV2, "authorisation", appealData, false);

        verifyCitizenCcdService(citizenCcdService);
        assertEquals(result, Optional.empty());
    }

    @Test
    public void shouldHandleScConflictWithNullNinoForUpdateDraftAppeal() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(409);
        givenUpdateCaseWillThrowException(citizenCcdService, feignException);
        appealData.getAppellant().setNino(null);

        Optional<SaveCaseResult> result = callUpdateDraftAppeal(submitAppealServiceV2, "authorisation", appealData);

        verifyUpdateCaseCalledByUpdateDraftAppeal(citizenCcdService);
        assertEquals(result, Optional.empty());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldRaisedExceptionOnCreateDraftWhenCitizenRoleIsNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealServiceV2, "authorisation", appealData, false);

        assertFalse(result.isPresent());
    }

    @Test
    public void shouldSuppressExceptionIfIts409OnCreateDraftCaseWithAppealDetailsWithDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(409);
        givenSaveCaseWillThrow(citizenCcdService, feignException);

        Optional<SaveCaseResult> result = callSubmitDraftAppeal(submitAppealServiceV2, "authorisation", appealData, false);

        verifyCitizenCcdService(citizenCcdService);
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldGetADraftIfItExists() {
        when(citizenCcdService.findCase(any())).thenReturn(Collections.singletonList(SscsCaseData.builder().build()));
        when(convertAIntoBService.convert(any(SscsCaseData.class))).thenReturn(SessionDraft.builder().build());
        Optional<SessionDraft> optionalSessionDraft = submitAppealServiceV2.getDraftAppeal("authorisation");
        assertTrue(optionalSessionDraft.isPresent());
    }

    @Test
    public void shouldGetNoDraftIfNoneExists() {
        when(citizenCcdService.findCase(any())).thenReturn(Collections.emptyList());
        Optional<SessionDraft> optionalSessionDraft = submitAppealServiceV2.getDraftAppeal("authorisation");
        assertFalse(optionalSessionDraft.isPresent());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldThrowExceptionOnGetDraftWhenCitizenRoleNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        submitAppealServiceV2.getDraftAppeal("authorisation");
    }

    @Test
    public void shouldGetAllDraftsIfItExists() {

        SscsCaseData caseData =  SscsCaseData.builder().build();

        when(citizenCcdService.findCase(any())).thenReturn(List.of(caseData, caseData, caseData));
        when(convertAIntoBService.convert(any(SscsCaseData.class))).thenReturn(SessionDraft.builder().build());
        List<SessionDraft> sessionDrafts = submitAppealServiceV2.getDraftAppeals("authorisation");
        assertEquals(3, sessionDrafts.size());
    }

    @Test
    public void shouldGetEmptyListIfNoDraftsExists() {
        when(citizenCcdService.findCase(any())).thenReturn(Collections.emptyList());
        List<SessionDraft> sessionDrafts = submitAppealServiceV2.getDraftAppeals("authorisation");
        assertEquals(0, sessionDrafts.size());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldThrowExceptionOnGetDraftsWhenCitizenRoleNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        submitAppealServiceV2.getDraftAppeals("authorisation");
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

    @Test
    public void givenCaseDoesNotExistInCcd_shouldCreateCaseWithAppealDetailsWithValidAppealCreatedEvent() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void givenCaseIsIbca_shouldCreateCaseWithAppealDetailsWithValidAppealCreatedEvent() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);
        appealData.setBenefitType(new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation"));
        submitAppealServiceV2.submitAppeal(appealData, userToken);
        appealData.setBenefitType(null);
        verify(ccdService).createCase(capture.capture(), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
        Optional<Benefit> benefitType = capture.getValue().getBenefitType();
        assertTrue(benefitType.isPresent());
        assertEquals(Benefit.INFECTED_BLOOD_COMPENSATION, benefitType.get());
        assertEquals(Benefit.INFECTED_BLOOD_COMPENSATION.getShortName(), capture.getValue().getAppeal().getBenefitType().getCode());
        assertEquals(Benefit.INFECTED_BLOOD_COMPENSATION.getBenefitCode(), capture.getValue().getBenefitCode());
    }


    @Test
    public void givenDraftCaseDoesExistAndCaseSubmitted_shouldUpdateCaseWithAppealDetailsWithDraftToValidAppealCreatedEvent() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder()
                .id(12345678L)
                .data(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                        .build()).build()).build()).build();
        given(updateCcdCaseService.updateCaseV2WithUnaryFunction(
                anyLong(),
                eq(DRAFT_TO_VALID_APPEAL_CREATED.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture()))
                .willReturn(sscsCaseDetails);


        long submittedCaseId = 999L;
        appealData.setCcdCaseId(String.valueOf(submittedCaseId));
        appealData.setIsSaveAndReturn("Yes");
        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(updateCcdCaseService).updateCaseV2WithUnaryFunction(
                eq(submittedCaseId),
                eq(DRAFT_TO_VALID_APPEAL_CREATED.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture());

        SscsCaseDetails updatedSscsCaseDetails = unaryOperatorArgumentCaptor.getValue().apply(sscsCaseDetails);

        assertThat(updatedSscsCaseDetails.getData().getIsSaveAndReturn())
                .isEqualTo("Yes");
    }

    @Test
    public void givenDraftCaseDoesExistAndCaseSubmittedHasNullBenefitType_shouldUpdateCaseWithAppealDetailsWithDraftToValidAppealCreatedEvent() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder()
                .id(12345678L)
                .data(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                        .build()).build()).build()).build();

        long linkedCaseId = 123L;
        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any()))
                .willReturn(Collections.singletonList(
                        SscsCaseDetails.builder()
                                .id(linkedCaseId)
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build()));

        given(updateCcdCaseService.updateCaseV2WithUnaryFunction(
                anyLong(),
                eq(DRAFT_TO_VALID_APPEAL_CREATED.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture()))
                .willReturn(sscsCaseDetails);

        long submittedCaseId = 999L;
        appealData.setCcdCaseId(String.valueOf(submittedCaseId));
        appealData.setIsSaveAndReturn("Yes");
        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(updateCcdCaseService).updateCaseV2WithUnaryFunction(
                eq(submittedCaseId),
                eq(DRAFT_TO_VALID_APPEAL_CREATED.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture());
        SscsCaseDetails updatedSscsCaseDetails = unaryOperatorArgumentCaptor.getValue().apply(sscsCaseDetails);

        assertThat(updatedSscsCaseDetails.getData().getLinkedCasesBoolean())
                .isEqualTo("Yes");
        assertThat(updatedSscsCaseDetails.getData().getAssociatedCase())
                .hasSize(1);
        assertThat(updatedSscsCaseDetails.getData().getAssociatedCase().get(0))
                .isEqualTo(CaseLink.builder().value(
                        CaseLinkDetails.builder().caseReference(String.valueOf(linkedCaseId)).build()).build());
        assertThat(updatedSscsCaseDetails.getData().getIsSaveAndReturn())
                .isEqualTo("Yes");
    }

    @Test
    public void givenDraftCaseDoesExistAndCaseSubmittedHasNullNino_shouldUpdateCaseWithAppealDetailsWithDraftToValidAppealCreatedEvent() {
        byte[] expected = {};
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder()
                .id(12345678L)
                .data(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                        .build()).build()).build()).build();

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any()))
                .willReturn(Collections.singletonList(
                        SscsCaseDetails.builder()
                                .id(12345678L)
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().build())
                                                .benefitType(BenefitType.builder().code(appealData.getBenefitType().getCode()).build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build()));

        given(updateCcdCaseService.updateCaseV2WithUnaryFunction(
                anyLong(),
                eq(DRAFT_TO_VALID_APPEAL_CREATED.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture()))
                .willReturn(sscsCaseDetails);

        long submittedCaseId = 999L;
        appealData.setCcdCaseId(String.valueOf(submittedCaseId));
        appealData.setIsSaveAndReturn("Yes");
        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(updateCcdCaseService).updateCaseV2WithUnaryFunction(
                eq(submittedCaseId),
                eq(DRAFT_TO_VALID_APPEAL_CREATED.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture());

        SscsCaseDetails updatedSscsCaseDetails = unaryOperatorArgumentCaptor.getValue().apply(sscsCaseDetails);
        assertThat(updatedSscsCaseDetails.getId()).isEqualTo(sscsCaseDetails.getId());
        assertThat(updatedSscsCaseDetails.getData().getIsSaveAndReturn())
                .isEqualTo("Yes");

    }

    @Test
    public void givenAssociatedCaseAlreadyExistsInCcd_shouldCreateCaseWithAppealDetailsAndAssociatedCase() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any()))
                .willReturn(Collections.singletonList(
                        SscsCaseDetails.builder()
                                .id(12345678L)
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code(appealData.getBenefitType().getCode()).build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build()
                ));

        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals(1, capture.getValue().getAssociatedCase().size());
        assertEquals("12345678", capture.getValue().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void givenAssociatedCaseAlreadyExistsInCcd_shouldCreateCaseWithAppealDetailsAndAssociatedCaseWithoutMrn() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any()))
                .willReturn(Collections.singletonList(
                        SscsCaseDetails.builder()
                                .id(12345678L)
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code(appealData.getBenefitType().getCode()).build())
                                                .mrnDetails(MrnDetails.builder().build())
                                                .build()).build()).build()
                ));

        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals(1, capture.getValue().getAssociatedCase().size());
        assertEquals("12345678", capture.getValue().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnDateIsMissing_shouldCreateCaseWithAppealDetailsWithIncompleteApplicationEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(null);

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(INCOMPLETE_APPLICATION_RECEIVED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        verify(ccdService, times(0)).updateCase(any(SscsCaseData.class), eq(123L), eq(SEND_TO_DWP.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnDateIsMissing_shouldCreateCaseWithAppealDetailsWithIncompleteApplicationEventIba() {
        byte[] expected = {};
        appealData.getMrn().setDate(null);

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);
        appealData.setBenefitType(new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation"));
        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(INCOMPLETE_APPLICATION_RECEIVED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        verify(ccdService, times(0)).updateCase(any(SscsCaseData.class), eq(123L), eq(SEND_TO_DWP.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void givenDraftCaseDoesExistAndMrnDateIsMissingAndCaseSubmitted_shouldUpdateCaseWithAppealDetailsWithDraftToIncompleteApplicationEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(null);

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        given(ccdService.getByCaseId(eq(123L), any())).willReturn(SscsCaseDetails.builder().build());


        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder()
                .id(12345678L)
                .data(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                .build()).build()).build();

        given(updateCcdCaseService.updateCaseV2WithUnaryFunction(
                anyLong(),
                eq(DRAFT_TO_INCOMPLETE_APPLICATION.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture()))
                .willReturn(sscsCaseDetails);


        long submittedCaseId = 999L;
        appealData.setCcdCaseId(String.valueOf(submittedCaseId));
        appealData.setIsSaveAndReturn("Yes");
        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(updateCcdCaseService).updateCaseV2WithUnaryFunction(
                eq(submittedCaseId),
                eq(DRAFT_TO_INCOMPLETE_APPLICATION.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture());

        SscsCaseDetails updatedSscsCaseDetails = unaryOperatorArgumentCaptor.getValue().apply(sscsCaseDetails);

        assertThat(updatedSscsCaseDetails.getData().getIsSaveAndReturn())
                .isEqualTo("Yes");
    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnIsMissing_shouldCreateCaseWithAppealDetailsWithIncompleteApplicationEvent() {
        byte[] expected = {};
        appealData.setMrn(null);

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(INCOMPLETE_APPLICATION_RECEIVED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        verify(ccdService, times(0)).updateCase(any(SscsCaseData.class), eq(123L), eq(SEND_TO_DWP.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnIsMissing_shouldCreateCaseWithAppealDetailsWithIncompleteApplicationEventIba() {
        byte[] expected = {};
        appealData.setMrn(null);

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);
        appealData.setBenefitType(new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation"));
        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(INCOMPLETE_APPLICATION_RECEIVED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        verify(ccdService, times(0)).updateCase(any(SscsCaseData.class), eq(123L), eq(SEND_TO_DWP.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnDateIsGreaterThan13Months_shouldCreateCaseWithAppealDetailsWithNonCompliantReceivedEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(LocalDate.now().minusMonths(13).minusDays(1));

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(NON_COMPLIANT.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void givenDraftCaseDoesExistAndMrnIsMissingAndCaseSubmitted_shouldUpdateCaseWithAppealDetailsWithDraftToIncompleteApplicationEvent() {
        byte[] expected = {};
        appealData.setMrn(null);

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        given(ccdService.getByCaseId(eq(123L), any())).willReturn(SscsCaseDetails.builder().build());

        appealData.setCcdCaseId("123");
        appealData.setIsSaveAndReturn("Yes");
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder()
                .id(12345678L)
                .data(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                .build()).build()).build();

        given(updateCcdCaseService.updateCaseV2WithUnaryFunction(
                anyLong(),
                eq(DRAFT_TO_INCOMPLETE_APPLICATION.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture()))
                .willReturn(sscsCaseDetails);

        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(updateCcdCaseService).updateCaseV2WithUnaryFunction(
                eq(Long.valueOf(appealData.getCcdCaseId())),
                eq(DRAFT_TO_INCOMPLETE_APPLICATION.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture());

        SscsCaseDetails updatedSscsCaseDetails = unaryOperatorArgumentCaptor.getValue().apply(sscsCaseDetails);

        assertThat(updatedSscsCaseDetails.getData().getIsSaveAndReturn())
                .isEqualTo("Yes");
    }

    @Test
    public void givenDraftCaseDoesExistAndMrnDateIsGreaterThan13MonthsAndCaseSubmitted_shouldUpdateCaseWithAppealDetailsWithDraftToNonCompliantEvent() {
        byte[] expected = {};
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder()
                .id(12345678L)
                .data(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                        .build()).build()).build()).build();


        given(updateCcdCaseService.updateCaseV2WithUnaryFunction(
                anyLong(),
                eq(DRAFT_TO_NON_COMPLIANT.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture()))
                .willReturn(sscsCaseDetails);

        appealData.getMrn().setDate(LocalDate.now().minusMonths(13).minusDays(1));

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.getByCaseId(eq(123L), any())).willReturn(SscsCaseDetails.builder().build());

        long submittedCaseId = 999L;
        appealData.setCcdCaseId(String.valueOf(submittedCaseId));
        appealData.setIsSaveAndReturn("Yes");

        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(updateCcdCaseService).updateCaseV2WithUnaryFunction(
                eq(submittedCaseId),
                eq(DRAFT_TO_NON_COMPLIANT.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                unaryOperatorArgumentCaptor.capture());

        SscsCaseDetails updatedSscsCaseDetails = unaryOperatorArgumentCaptor.getValue().apply(sscsCaseDetails);

        assertThat(updatedSscsCaseDetails.getData().getIsSaveAndReturn())
                .isEqualTo("Yes");
    }

    @Test(expected = CcdException.class)
    public void givenExceptionWhenSearchingForCaseInCcd_shouldThrowException() {
        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any(IdamTokens.class)))
                .willThrow(RuntimeException.class);

        submitAppealServiceV2.submitAppeal(appealData, userToken);
    }

    @Test(expected = CcdException.class)
    public void givenCaseDoesNotExistInCcdAndGivenExceptionWhenCreatingCaseInCcd_shouldThrowException() {
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(null);

        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
                .willThrow(RuntimeException.class);

        submitAppealServiceV2.submitAppeal(appealData, userToken);
    }

    @Test(expected = DuplicateCaseException.class)
    public void givenCaseIsADuplicate_shouldNotResendEmails() {
        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any()))
                .willReturn(Collections.singletonList(
                        SscsCaseDetails.builder()
                                .id(12345678L)
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code(appealData.getBenefitType().getCode()).build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build()
                ));

        submitAppealServiceV2.submitAppeal(appealData, userToken);

        then(pdfServiceClient).should(never()).generateFromHtml(any(byte[].class), anyMap());
    }

    @Test(expected = DuplicateCaseException.class)
    public void givenCaseAlreadyExistsInCcd_shouldNotCreateCaseWithAppealDetails() {
        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any()))
                .willReturn(Arrays.asList(
                        SscsCaseDetails.builder()
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code(appealData.getBenefitType().getCode()).build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build(),
                        SscsCaseDetails.builder()
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code("ESA").build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build(),
                        SscsCaseDetails.builder()
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code(appealData.getBenefitType().getCode()).build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build()
                ));

        submitAppealServiceV2.submitAppeal(appealData, userToken);

        verify(ccdService, never()).createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class));
    }

    @Test
    @Parameters(method = "generateDifferentRpcScenarios")
    public void givenAppellantPostCode_shouldSetRegionAndRpcCorrectly(String expectedRpc, String appellantPostCode) throws JsonProcessingException {
        RegionalProcessingCenter rpc = getRpcObjectForGivenJsonRpc(expectedRpc);
        when(regionalProcessingCenterService.getByPostcode(eq(RegionalProcessingCenterService.getFirstHalfOfPostcode(appellantPostCode)), anyBoolean()))
            .thenReturn(getRpcObjectForGivenJsonRpc(expectedRpc));
        when(airLookupService.lookupAirVenueNameByPostCode(eq(appellantPostCode), any())).thenReturn(rpc.getCity());
        when(venueService.getEpimsIdForVenue(rpc.getCity())).thenReturn("1234");
        when(refDataService.getCourtVenueRefDataByEpimsId("1234")).thenReturn(CourtVenue.builder().courtStatus("Open").regionId("1").build());

        appealData.getAppellant().getContactDetails().setPostCode(appellantPostCode);

        SscsCaseData caseData = submitAppealServiceV2.convertAppealToSscsCaseData(appealData, SscsCaseData.builder().build());

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

    @Test
    @Parameters(method = "generateDifferentRpcScenariosIba")
    public void givenAppellantPostCode_shouldSetRegionAndRpcCorrectlyIba(String expectedRpc, String appellantLocationCode) throws JsonProcessingException {
        RegionalProcessingCenter rpc = getRpcObjectForGivenJsonRpc(expectedRpc);
        when(regionalProcessingCenterService.getByPostcode(appellantLocationCode, true))
            .thenReturn(getRpcObjectForGivenJsonRpc(expectedRpc));
        when(airLookupService.lookupAirVenueNameByPostCode(eq(appellantLocationCode), any())).thenReturn(rpc.getCity());
        when(venueService.getEpimsIdForVenue(rpc.getCity())).thenReturn("1234");
        when(refDataService.getCourtVenueRefDataByEpimsId("1234")).thenReturn(CourtVenue.builder().courtStatus("Open").regionId("1").build());
        SyaBenefitType syaBenefitType = new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation");
        appealData.setBenefitType(syaBenefitType);
        appealData.getAppellant().getContactDetails().setPortOfEntry(appellantLocationCode);
        appealData.getAppellant().getContactDetails().setInMainlandUk(Boolean.FALSE);
        SscsCaseData caseData = submitAppealServiceV2.convertAppealToSscsCaseData(appealData, SscsCaseData.builder().build());
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

    @Test
    public void givenAppointeePostCode_shouldSetRegionAndRpcToAppointee() throws JsonProcessingException {
        when(regionalProcessingCenterService.getByPostcode(eq("B1"), anyBoolean())).thenReturn(getRpcObjectForGivenJsonRpc(BIRMINGHAM_RPC));
        when(airLookupService.lookupAirVenueNameByPostCode(eq("B1 1AA"), any())).thenReturn("Birmingham");

        when(venueService.getEpimsIdForVenue("Birmingham")).thenReturn("1234");
        when(refDataService.getCourtVenueRefDataByEpimsId("1234")).thenReturn(CourtVenue.builder().courtStatus("Open").regionId("1").build());

        SyaContactDetails appointeeContactDetails = new SyaContactDetails();
        appointeeContactDetails.setPostCode("B1 1AA");

        SyaCaseWrapper appealData = getSyaWrapperWithAppointee(appointeeContactDetails);

        SscsCaseData caseData = submitAppealServiceV2.convertAppealToSscsCaseData(appealData, SscsCaseData.builder().build());

        assertRpc(caseData, BIRMINGHAM_RPC);
    }

    @Test
    @Parameters({"", "null"})
    public void givenAppointeeWithNoPostCode_shouldSetRegionAndRpcToNull(@Nullable String postCode) {
        SyaContactDetails appointeeContactDetails = new SyaContactDetails();
        appointeeContactDetails.setPostCode(postCode);

        SyaCaseWrapper appealData = getSyaWrapperWithAppointee(appointeeContactDetails);

        SscsCaseData caseData = submitAppealServiceV2.convertAppealToSscsCaseData(appealData, SscsCaseData.builder().build());

        assertNull(caseData.getRegionalProcessingCenter());
    }

    @Test
    public void givenAppointeeWithNoContactData_shouldSetRegionAndRpcToAppellant() throws JsonProcessingException {
        when(regionalProcessingCenterService.getByPostcode(eq("TN32"), anyBoolean())).thenReturn(getRpcObjectForGivenJsonRpc(BRADFORD_RPC));
        when(airLookupService.lookupAirVenueNameByPostCode(eq("TN32 6PL"), any())).thenReturn("Bradford");
        when(venueService.getEpimsIdForVenue("Bradford")).thenReturn("1234");
        when(refDataService.getCourtVenueRefDataByEpimsId("1234")).thenReturn(CourtVenue.builder().courtStatus("Open").regionId("1").build());

        SyaCaseWrapper appealData = getSyaWrapperWithAppointee(null);
        appealData.setIsAppointee(false);

        SscsCaseData caseData = submitAppealServiceV2.convertAppealToSscsCaseData(appealData, SscsCaseData.builder().build());

        assertRpc(caseData, BRADFORD_RPC);
    }


    @Test
    public void givenAPipCase_thenSetCreatedInGapsFromFieldToReadyToList() {
        SyaBenefitType syaBenefitType = new SyaBenefitType("PIP", "PIP");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("2");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealServiceV2.convertAppealToSscsCaseData(appealData, SscsCaseData.builder().build());
        assertEquals(READY_TO_LIST.getId(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void givenAEsaCase_thenSetCreatedInGapsFromToReadyToList() {
        SyaBenefitType syaBenefitType = new SyaBenefitType("ESA", "ESA");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("Chesterfield DRT");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealServiceV2.convertAppealToSscsCaseData(appealData, SscsCaseData.builder().build());
        assertEquals(READY_TO_LIST.getId(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void givenAUcCaseWithRecoveryFromEstatesOffice_thenSetOfficeCorrectly() {
        SyaBenefitType syaBenefitType = new SyaBenefitType("Universal Credit", "UC");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("Recovery from Estates");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealServiceV2.convertAppealToSscsCaseData(appealData, SscsCaseData.builder().build());
        assertEquals(READY_TO_LIST.getId(), caseData.getCreatedInGapsFrom());
        assertEquals("RfE", caseData.getDwpRegionalCentre());
        assertEquals("UC Recovery from Estates", caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());
    }

    @Test(expected = CcdException.class)
    public void givenExceptionWhenCreatingOrUpdatingIbaCase_shouldThrowException() {
        SyaBenefitType syaBenefitType = new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation");
        appealData.setBenefitType(syaBenefitType);
        given(ccdService.createCase(any(SscsCaseData.class), anyString(), anyString(), anyString(), any(IdamTokens.class)))
                .willThrow(RuntimeException.class);

        submitAppealServiceV2.submitAppeal(appealData, userToken);
    }

    @Test
    public void givenAssociatedCase_thenAddAssociatedCaseLinkToCase() {
        SscsCaseDetails matchingCase = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase);

        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("00000000").build();
        submitAppealServiceV2.addAssociatedCases(
                caseData,
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

        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("00000000").build();
        submitAppealServiceV2.addAssociatedCases(
                caseData,
                matchedByNinoCases);

        assertEquals(2, caseData.getAssociatedCase().size());
        assertEquals("Yes", caseData.getLinkedCasesBoolean());
        assertEquals("12345678", caseData.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("56765676", caseData.getAssociatedCase().get(1).getValue().getCaseReference());
    }

    @Test
    public void addNoAssociatedCases() {
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();

        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("00000000").build();
        submitAppealServiceV2.addAssociatedCases(
                caseData,
                matchedByNinoCases);

        assertNull(caseData.getAssociatedCase());
        assertEquals("No", caseData.getLinkedCasesBoolean());
        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }

    @Test
    @Parameters({
        "PIP, n1w1 wal, Birmingham, appellant, 1, 21",
        "ESA, n1w1 wal, Birmingham, appellant, 1, 21",
        "UC, n1w1 wal, Birmingham, appellant, 1, 21",
        "PIP, NN85 1ss, Northampton, appellant, 2, 30",
        "ESA, NN85 1ss, Northampton, appellant, 2, 30",
        "UC, NN85 1ss, Northampton, appellant, 2, 30",
        "PIP, n1w1 wal, Birmingham, appointee, 1, 21",
        "ESA, n1w1 wal, Birmingham, appointee, 1, 21",
        "UC, n1w1 wal, Birmingham, appointee, 1, 21",
        "PIP, NN85 1ss, Northampton, appointee, 2, 30",
        "ESA, NN85 1ss, Northampton, appointee, 2, 30",
        "UC, NN85 1ss, Northampton, appointee, 2, 30",
    })
    public void shouldSetProcessingVenueBasedOnBenefitTypeAndPostCode(String benefitCode, String postcode, String expectedVenue, String appellantOrAppointee, String epimsId, String regionId) {
        String firstHalfOfPostcode = RegionalProcessingCenterService.getFirstHalfOfPostcode(postcode);
        when(regionalProcessingCenterService.getByPostcode(eq(firstHalfOfPostcode), anyBoolean())).thenReturn(
                RegionalProcessingCenter.builder()
                        .name("rpcName")
                        .postcode("rpcPostcode")
                        .epimsId(epimsId)
                        .build());

        when(venueService.getEpimsIdForVenue(expectedVenue)).thenReturn(epimsId);
        when(airLookupService.lookupAirVenueNameByPostCode(eq(postcode), any())).thenReturn(expectedVenue);
        when(refDataService.getCourtVenueRefDataByEpimsId(epimsId)).thenReturn(CourtVenue.builder().courtStatus("Open").regionId(regionId).build());

        boolean isAppellant = appellantOrAppointee.equals("appellant");
        SyaCaseWrapper appealData = getSyaCaseWrapper(isAppellant ? "json/sya.json" : "sya/allDetailsWithAppointeeWithDifferentAddress.json");
        SyaBenefitType syaBenefitType = new SyaBenefitType(benefitCode, benefitCode);
        appealData.setBenefitType(syaBenefitType);
        if (isAppellant) {
            appealData.getAppellant().getContactDetails().setPostCode(postcode);
        } else {
            appealData.getAppointee().getContactDetails().setPostCode(postcode);
        }

        SscsCaseData caseData = submitAppealServiceV2.convertAppealToSscsCaseData(appealData, SscsCaseData.builder().build());

        assertEquals(expectedVenue, caseData.getProcessingVenue());
        assertNotNull(caseData.getCaseManagementLocation());
        assertEquals(epimsId, caseData.getCaseManagementLocation().getBaseLocation());
        assertEquals(regionId, caseData.getCaseManagementLocation().getRegion());
    }
}
