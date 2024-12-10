package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
import feign.FeignException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaContactDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaMrn;
import uk.gov.hmcts.reform.sscs.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.service.v2.SubmitAppealService;

public class SubmitAppealServiceTestV1 extends AbstractSubmitAppealServiceTest {

    @Override
    public void givenSaveCaseWillReturnSaveCaseOperation(CitizenCcdService citizenCcdService, Long caseDetailsId, SaveCaseOperation saveCaseOperation) {
        given(citizenCcdService.saveCase(any(SscsCaseData.class), any(IdamTokens.class)))
                .willReturn(SaveCaseResult.builder()
                        .caseDetailsId(caseDetailsId)
                        .saveCaseOperation(saveCaseOperation)
                        .build());
    }

    @Override
    public void givenSaveCaseWillThrow(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.saveCase(any(SscsCaseData.class), any(IdamTokens.class)))
                .willThrow(feignException);
    }

    @Override
    public void verifyCitizenCcdService(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).saveCase(any(SscsCaseData.class), any(IdamTokens.class));
    }

    @Override
    public Optional<SaveCaseResult> callSubmitDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService, SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData, boolean forceCreate) {
        return submitAppealService.submitDraftAppeal(auth2Token, appealData, forceCreate);
    }

    @Override
    public void givenUpdateCaseWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails) {
        given(citizenCcdService.updateCase(any(SscsCaseData.class), any(), any(), any(), any(), any()))
                .willReturn(caseDetails);
    }

    @Override
    public void givenUpdateCaseWillThrowException(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.updateCase(any(SscsCaseData.class), any(), any(), any(), any(), any()))
                .willThrow(feignException);
    }

    @Override
    public Optional<SaveCaseResult> callUpdateDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService, SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData) {
        return submitAppealService.updateDraftAppeal(auth2Token, appealData);
    }

    @Override
    public void verifyUpdateCaseCalledByUpdateDraftAppeal(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).updateCase(any(SscsCaseData.class), any(), any(), any(), any(), any());
    }

    @Override
    public Optional<SaveCaseResult> callArchiveDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService, SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData, Long caseId) {
        return submitAppealService.archiveDraftAppeal(auth2Token, appealData, caseId);
    }

    @Override
    public void verifyArchiveDraftAppeal(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).archiveDraft(any(SscsCaseData.class), any(IdamTokens.class), any());
    }

    @Override
    public void givenArchiveDraftAppealWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails) {
        given(citizenCcdService.archiveDraft(any(SscsCaseData.class), any(IdamTokens.class), any()))
                .willReturn(caseDetails);
    }

    @Override
    public void givenArchiveDraftAppealWillThrow(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.archiveDraft(any(SscsCaseData.class), any(IdamTokens.class), any()))
                .willThrow(feignException);
    }

    @Override
    public void verifyArchiveDraft(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).archiveDraft(any(SscsCaseData.class), any(IdamTokens.class), any());
    }

    @Test
    public void givenCaseDoesNotExistInCcd_shouldCreateCaseWithAppealDetailsWithValidAppealCreatedEvent() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void givenCaseIsIbca_shouldCreateCaseWithAppealDetailsWithValidAppealCreatedEvent() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);
        appealData.setBenefitType(new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation"));
        submitAppealService.submitAppeal(appealData, userToken);
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
        given(ccdService.getByCaseId(eq(123L), any())).willReturn(SscsCaseDetails.builder().build());

        appealData.setCcdCaseId("123");
        appealData.setIsSaveAndReturn("Yes");
        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).updateCase(capture.capture(), eq(123L), eq(DRAFT_TO_VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("Yes", capture.getValue().getIsSaveAndReturn());

    }

    @Test
    public void givenDraftCaseDoesExistAndCaseSubmittedHasNullBenefitType_shouldUpdateCaseWithAppealDetailsWithDraftToValidAppealCreatedEvent() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any()))
                .willReturn(Collections.singletonList(
                        SscsCaseDetails.builder()
                                .id(12345678L)
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build()));
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);
        given(ccdService.getByCaseId(eq(123L), any())).willReturn(SscsCaseDetails.builder().build());

        appealData.setCcdCaseId("123");
        appealData.setIsSaveAndReturn("Yes");
        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).updateCase(capture.capture(), eq(123L), eq(DRAFT_TO_VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("Yes", capture.getValue().getIsSaveAndReturn());

    }

    @Test
    public void givenDraftCaseDoesExistAndCaseSubmittedHasNullNino_shouldUpdateCaseWithAppealDetailsWithDraftToValidAppealCreatedEvent() {
        byte[] expected = {};
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
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);
        given(ccdService.getByCaseId(eq(123L), any())).willReturn(SscsCaseDetails.builder().build());

        appealData.setCcdCaseId("123");
        appealData.setIsSaveAndReturn("Yes");
        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).updateCase(capture.capture(), eq(123L), eq(DRAFT_TO_VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("Yes", capture.getValue().getIsSaveAndReturn());

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

        submitAppealService.submitAppeal(appealData, userToken);

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

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData, userToken);

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
        submitAppealService.submitAppeal(appealData, userToken);

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

        appealData.setCcdCaseId("123");
        appealData.setIsSaveAndReturn("Yes");
        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).updateCase(capture.capture(), eq(123L), eq(DRAFT_TO_INCOMPLETE_APPLICATION.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("Yes", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnIsMissing_shouldCreateCaseWithAppealDetailsWithIncompleteApplicationEvent() {
        byte[] expected = {};
        appealData.setMrn(null);

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData, userToken);

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
        submitAppealService.submitAppeal(appealData, userToken);

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

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(NON_COMPLIANT.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
    }


    @Test
    public void givenDraftCaseDoesExistAndMrnDateIsGreaterThan13MonthsAndCaseSubmitted_shouldUpdateCaseWithAppealDetailsWithDraftToNonCompliantEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(LocalDate.now().minusMonths(13).minusDays(1));

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        given(ccdService.getByCaseId(eq(123L), any())).willReturn(SscsCaseDetails.builder().build());

        appealData.setCcdCaseId("123");
        appealData.setIsSaveAndReturn("Yes");
        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).updateCase(capture.capture(), eq(123L), eq(DRAFT_TO_NON_COMPLIANT.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("Yes", capture.getValue().getIsSaveAndReturn());
    }

    @Test(expected = CcdException.class)
    public void givenExceptionWhenSearchingForCaseInCcd_shouldThrowException() {
        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any(IdamTokens.class)))
                .willThrow(RuntimeException.class);

        submitAppealService.submitAppeal(appealData, userToken);
    }

    @Test(expected = CcdException.class)
    public void givenCaseDoesNotExistInCcdAndGivenExceptionWhenCreatingCaseInCcd_shouldThrowException() {
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(null);

        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
                .willThrow(RuntimeException.class);

        submitAppealService.submitAppeal(appealData, userToken);
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

        submitAppealService.submitAppeal(appealData, userToken);

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

        submitAppealService.submitAppeal(appealData, userToken);

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

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);

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
        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);
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

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);

        assertRpc(caseData, BIRMINGHAM_RPC);
    }

    @Test
    @Parameters({"", "null"})
    public void givenAppointeeWithNoPostCode_shouldSetRegionAndRpcToNull(@Nullable String postCode) {
        SyaContactDetails appointeeContactDetails = new SyaContactDetails();
        appointeeContactDetails.setPostCode(postCode);

        SyaCaseWrapper appealData = getSyaWrapperWithAppointee(appointeeContactDetails);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);

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

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);

        assertRpc(caseData, BRADFORD_RPC);
    }


    @Test
    public void givenAPipCase_thenSetCreatedInGapsFromFieldToReadyToList() {
        SyaBenefitType syaBenefitType = new SyaBenefitType("PIP", "PIP");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("2");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);
        assertEquals(READY_TO_LIST.getId(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void givenAEsaCase_thenSetCreatedInGapsFromToReadyToList() {
        SyaBenefitType syaBenefitType = new SyaBenefitType("ESA", "ESA");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("Chesterfield DRT");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);
        assertEquals(READY_TO_LIST.getId(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void givenAUcCaseWithRecoveryFromEstatesOffice_thenSetOfficeCorrectly() {
        SyaBenefitType syaBenefitType = new SyaBenefitType("Universal Credit", "UC");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("Recovery from Estates");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);
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

        submitAppealService.submitAppeal(appealData, userToken);
    }

    @Test
    public void givenAssociatedCase_thenAddAssociatedCaseLinkToCase() {
        SscsCaseDetails matchingCase = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase);

        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("00000000").build();
        submitAppealService.addAssociatedCases(
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
        submitAppealService.addAssociatedCases(
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
        submitAppealService.addAssociatedCases(
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

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);

        assertEquals(expectedVenue, caseData.getProcessingVenue());
        assertNotNull(caseData.getCaseManagementLocation());
        assertEquals(epimsId, caseData.getCaseManagementLocation().getBaseLocation());
        assertEquals(regionId, caseData.getCaseManagementLocation().getRegion());
    }
}
