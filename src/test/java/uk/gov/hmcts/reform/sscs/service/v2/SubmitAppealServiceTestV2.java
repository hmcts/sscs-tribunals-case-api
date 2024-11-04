package uk.gov.hmcts.reform.sscs.service.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_INCOMPLETE_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INCOMPLETE_APPLICATION_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import feign.FeignException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.service.AbstractSubmitAppealServiceTest;

public class SubmitAppealServiceTestV2 extends AbstractSubmitAppealServiceTest {

    @Captor
    ArgumentCaptor<UnaryOperator<SscsCaseDetails>> unaryOperatorArgumentCaptor;

    @Override
    public void givenSaveCaseWillReturnSaveCaseOperation(CitizenCcdService citizenCcdService, Long caseDetailsId, SaveCaseOperation saveCaseOperation) {
        given(citizenCcdService.saveCaseV2(any(IdamTokens.class), any(Consumer.class)))
                .willReturn(SaveCaseResult.builder()
                        .caseDetailsId(caseDetailsId)
                        .saveCaseOperation(saveCaseOperation)
                        .build());
    }

    @Override
    public void givenSaveCaseWillThrow(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.saveCaseV2(any(IdamTokens.class), any(Consumer.class)))
                .willThrow(feignException);
    }

    @Override
    public void verifyCitizenCcdService(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).saveCaseV2(any(IdamTokens.class), any(Consumer.class));
    }

    @Override
    public Optional<SaveCaseResult> callSubmitDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService, SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData, boolean forceCreate) {
        return submitAppealServiceV2.submitDraftAppeal(auth2Token, appealData, forceCreate);
    }

    @Override
    public void givenUpdateCaseWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails) {
        given(citizenCcdService.updateCaseCitizenV2(any(String.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(UnaryOperator.class)))
                .willReturn(caseDetails);
    }

    @Override
    public void givenUpdateCaseWillThrowException(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.updateCaseCitizenV2(any(), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(UnaryOperator.class)))
                .willThrow(feignException);
    }

    @Override
    public Optional<SaveCaseResult> callUpdateDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService, SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData) {
        return submitAppealServiceV2.updateDraftAppeal(auth2Token, appealData);
    }

    @Override
    public void verifyUpdateCaseCalledByUpdateDraftAppeal(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).updateCaseCitizenV2(any(), any(String.class), any(String.class), any(String.class), any(IdamTokens.class), any(UnaryOperator.class));
    }

    @Override
    public Optional<SaveCaseResult> callArchiveDraftAppeal(uk.gov.hmcts.reform.sscs.service.SubmitAppealService submitAppealService, SubmitAppealService submitAppealServiceV2, String auth2Token, SyaCaseWrapper appealData, Long caseId) {
        return submitAppealServiceV2.archiveDraftAppeal(auth2Token, appealData, caseId);
    }

    @Override
    public void verifyArchiveDraftAppeal(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class));
    }

    @Override
    public void givenArchiveDraftAppealWillReturnCaseDetails(CitizenCcdService citizenCcdService, CaseDetails caseDetails) {
        given(citizenCcdService.archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class)))
                .willReturn(caseDetails);
    }

    @Override
    public void givenArchiveDraftAppealWillThrow(CitizenCcdService citizenCcdService, FeignException feignException) {
        given(citizenCcdService.archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class)))
                .willThrow(feignException);
    }

    @Override
    public void verifyArchiveDraft(CitizenCcdService citizenCcdService) {
        verify(citizenCcdService).archiveDraftV2(any(IdamTokens.class), any(Long.class), any(Consumer.class));
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
        assertThat(updatedSscsCaseDetails.getData().getAssociatedCase().size())
                .isEqualTo(1);
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

}
