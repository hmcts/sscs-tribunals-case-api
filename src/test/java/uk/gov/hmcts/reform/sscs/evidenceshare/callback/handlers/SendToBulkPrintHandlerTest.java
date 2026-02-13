package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.UNREGISTERED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SENT_TO_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import feign.FeignException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.NonPdfBulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.DocumentManagementServiceWrapper;
import uk.gov.hmcts.reform.sscs.factory.DocumentRequestFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@ExtendWith(MockitoExtension.class)
class SendToBulkPrintHandlerTest {

    private final String docUrl = "my/1/url.pdf";
    private final Pdf docPdf = new Pdf(docUrl.getBytes(), "evidence1.pdf");
    private final Pdf docPdf2 = new Pdf(docUrl.getBytes(), "evidence2.pdf");
    private final LocalDateTime now = LocalDateTime.now();
    private final String nowString = (DateTimeFormatter.ISO_LOCAL_DATE).format(now);
    private final CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", singletonList(
        SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentFileName(docPdf.getName())
            .documentType("sscs1")
            .evidenceIssued("No")
            .documentLink(DocumentLink.builder().documentUrl(docUrl)
                .documentFilename(docPdf.getName()).build())
            .build()).build()), APPEAL_CREATED);
    private final Map<String, Object> placeholders = new HashMap<>();
    private final Template template = new Template("bla", "bla2");
    @Mock
    private DocumentManagementServiceWrapper documentManagementServiceWrapper;
    @Mock
    private DocumentRequestFactory documentRequestFactory;
    @Mock
    private BulkPrintService bulkPrintService;
    @Mock
    private PdfStoreService pdfStoreService;
    @Mock
    private EvidenceShareConfig evidenceShareConfig;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;
    @Mock
    private IdamService idamService;
    @Mock
    private Callback<SscsCaseData> callback;
    private SendToBulkPrintHandler handler;
    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> consumerArgumentCaptor;

    @BeforeEach
    void setUp() {
        handler = new SendToBulkPrintHandler(documentManagementServiceWrapper,
            documentRequestFactory, pdfStoreService, bulkPrintService, evidenceShareConfig, updateCcdCaseService,
            idamService, 35, 42, true);
        placeholders.put("Test", "Value");
    }

    @Test
    void givenAValidAppealCreatedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    void givenANonBulkPrintEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @ParameterizedTest
    @ValueSource(strings = {"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL", "RESEND_TO_DWP", "APPEAL_TO_PROCEED"})
    void givenAValidSendToBulkPrintEvent_thenReturnTrue(String eventType) {

        if (!"RESEND_TO_DWP".equals(eventType)) {
            when(callback.getCaseDetails()).thenReturn(caseDetails);
        }

        when(callback.getEvent()).thenReturn(EventType.valueOf(eventType));

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    void givenANonSendToBulkPrintEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    void givenSendToDwpWithDocTranslated_thenReturnTrue() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.SEND_TO_DWP);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    void givenDocTranslationOutstanding_thenReturnFalse() {
        caseDetails.getCaseData().setTranslationWorkOutstanding("Yes");
        caseDetails.getCaseData().isTranslationWorkOutstanding();

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    void givenResendToDwpWithDocTranslationOutstanding_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.RESEND_TO_DWP);
        caseDetails.getCaseData().setTranslationWorkOutstanding("Yes");

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @ParameterizedTest
    @CsvSource({"pip, 35", "childSupport, 42"})
    void givenAMessageWhichFindsATemplate_thenConvertToSscsCaseDataAndAddPdfToCaseAndSendToBulkPrint(String benefitType,
                                                                                                     int expectedResponseDays) {
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(singletonList("paper"));

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(benefitType, "Paper", Arrays.asList(
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(docPdf.getName())
                .documentType("sscs1")
                .documentLink(DocumentLink.builder().documentUrl(docUrl)
                    .documentFilename(docPdf.getName()).build())
                .build()).build(),
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(docPdf2.getName())
                .documentType("appellantEvidence")
                .documentLink(DocumentLink.builder().documentUrl(docUrl)
                    .documentFilename(docPdf2.getName()).build())
                .evidenceIssued("No")
                .build()).build(),
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("filtered out word.doc")
                .documentType("appellantEvidence")
                .documentLink(DocumentLink.builder().documentUrl("/my/1/doc.url")
                    .documentFilename("filtered out word.doc").build())
                .build()).build(),
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("filtered out as there is no documentLink object.pfd")
                .build()).build()), APPEAL_CREATED);

        when(pdfStoreService.download(eq(docUrl))).thenReturn(docPdf.getContent());
        when(documentManagementServiceWrapper.checkIfDlDocumentAlreadyExists(anyList())).thenReturn(true);

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();

        when(documentRequestFactory.create(caseDetails.getCaseData(), nowString)).thenReturn(holder);

        Optional<UUID> expectedOptionalUuid = Optional.of(UUID.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b"));

        when(bulkPrintService.sendToBulkPrint(eq(Arrays.asList(docPdf, docPdf2)), any(), any()))
            .thenReturn(expectedOptionalUuid);

        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED,
            false);

        handler = new SendToBulkPrintHandler(documentManagementServiceWrapper,
            documentRequestFactory, pdfStoreService, bulkPrintService, evidenceShareConfig, updateCcdCaseService,
            idamService, 35, 42, false);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(pdfStoreService, times(2)).download(eq(docUrl));
        verify(bulkPrintService).sendToBulkPrint(eq(Arrays.asList(docPdf, docPdf2)), any(), any());

        String documentList = "Case has been sent to the FTA via Bulk Print with bulk print id: 0f14d0ab-9605-4a62-a9e4-5ed26688389b and with documents: evidence1.pdf, evidence2.pdf";

        verify(updateCcdCaseService)
            .updateCaseV2(eq(123L), eq(SENT_TO_DWP.getCcdType()), eq("Sent to FTA"), eq(documentList), any(),
                consumerArgumentCaptor.capture());

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);

        assertNull(sscsCaseData.getSscsDocument().getFirst().getValue().getEvidenceIssued());
        assertEquals(sscsCaseData.getHmctsDwpState(), SENT_TO_DWP.getCcdType());
        assertEquals(sscsCaseData.getDateSentToDwp(), LocalDate.now().toString());
        assertEquals(sscsCaseData.getDwpDueDate(), LocalDate.now().plusDays(expectedResponseDays).toString());
        assertNull(sscsCaseData.getDwpState());
    }

    @Test
    void givenAnErrorWhenSendToBulkPrint_shouldUpdateCaseInCcdToFlagError() {
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(singletonList("paper"));
        Callback<SscsCaseData> callback = setupMocksForFlagErrorTests();
        handler.handle(CallbackType.SUBMITTED, callback);

        then(updateCcdCaseService)
            .should(times(1))
            .updateCaseV2(eq(123L), eq(EventType.SENT_TO_DWP_ERROR.getCcdType()), any(), any(), any(),
                consumerArgumentCaptor.capture());

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals("failedSending", sscsCaseData.getHmctsDwpState());
    }

    @Test
    void givenAnErrorIfDlDocumentsNotPresentWhenSendToBulkPrint_shouldUpdateCaseInCcdToFlagError() {

        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(singletonList("paper"));
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");
        Template template = new Template("bla", "bla2");

        when(documentManagementServiceWrapper.checkIfDlDocumentAlreadyExists(anyList())).thenReturn(true);

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();
        when(documentRequestFactory.create(caseDetails.getCaseData(), nowString)).thenReturn(holder);

        when(documentManagementServiceWrapper.checkIfDlDocumentAlreadyExists(anyList())).thenReturn(false);

        handler.handle(CallbackType.SUBMITTED, callback);

        then(updateCcdCaseService)
            .should(times(1))
            .updateCaseV2(eq(123L), eq(EventType.SENT_TO_DWP_ERROR.getCcdType()), eq("Send to FTA Error"),
                eq("Triggered from Evidence Share – no DL6/16 present, please validate."), any(),
                consumerArgumentCaptor.capture());

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals("failedSending", sscsCaseData.getHmctsDwpState());
        assertEquals("No", sscsCaseData.getSscsDocument().get(0).getValue().getEvidenceIssued());
    }

    @Test
    void givenNoTemplates_shouldThrowAnExceptionAndFlagError() {

        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(singletonList("paper"));
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(documentRequestFactory.create(caseDetails.getCaseData(), nowString))
            .thenReturn(DocumentHolder.builder().template(null).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        then(updateCcdCaseService)
            .should(times(1))
            .updateCaseV2(eq(123L), eq(EventType.SENT_TO_DWP_ERROR.getCcdType()), eq("Send to FTA Error"),
                eq("Send to FTA Error event has been triggered from Evidence Share service"), any(),
                consumerArgumentCaptor.capture());

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals("failedSending", sscsCaseData.getHmctsDwpState());
    }

    @Test
    void givenNoBulkPrintIdReturned_shouldThrowAnExceptionAndFlagError() {

        when(evidenceShareConfig.getSubmitTypes()).thenReturn(singletonList("paper"));
        Callback<SscsCaseData> callback = setupMocksForFlagErrorTests();

        when(bulkPrintService.sendToBulkPrint(eq(Arrays.asList(docPdf, docPdf2)), any(), any()))
            .thenReturn(Optional.empty());

        handler.handle(CallbackType.SUBMITTED, callback);

        then(updateCcdCaseService)
            .should(times(1))
            .updateCaseV2(eq(123L), eq(EventType.SENT_TO_DWP_ERROR.getCcdType()), eq("Send to FTA Error"),
                eq("Send to FTA Error event has been triggered from Evidence Share service"), any(),
                consumerArgumentCaptor.capture());

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals("failedSending", sscsCaseData.getHmctsDwpState());
    }

    @Test
    void givenAErrorDownloadingDocuments_shouldThrowAnExceptionAndFlagAnError() {
        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(singletonList("paper"));
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();
        when(documentRequestFactory.create(caseDetails.getCaseData(), nowString)).thenReturn(holder);

        final FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn("error");
        when(pdfStoreService.download(eq(docUrl))).thenThrow(feignException);
        when(documentManagementServiceWrapper.checkIfDlDocumentAlreadyExists(anyList())).thenReturn(true);

        handler.handle(CallbackType.SUBMITTED, callback);

        then(updateCcdCaseService)
            .should(times(1))
            .updateCaseV2(eq(123L), eq(EventType.SENT_TO_DWP_ERROR.getCcdType()), eq("Send to FTA Error"),
                eq("Unable to contact dm-store, please try again by running the \"Send to FTA\"."), any(),
                consumerArgumentCaptor.capture());

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals("failedSending", sscsCaseData.getHmctsDwpState());
    }

    @Test
    void givenABrokenPdfException_shouldThrowAnExceptionAndFlagAnError() {

        final Callback<SscsCaseData> callback = setupMocksForFlagErrorTests();
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(singletonList("paper"));
        when(bulkPrintService.sendToBulkPrint(anyList(), any(), any()))
            .thenThrow(new NonPdfBulkPrintException(new RuntimeException("error")));

        handler.handle(CallbackType.SUBMITTED, callback);

        then(updateCcdCaseService)
            .should(times(1))
            .updateCaseV2(eq(123L), eq(EventType.SENT_TO_DWP_ERROR.getCcdType()), eq("Send to FTA Error"),
                eq("Non-PDFs/broken PDFs seen in list of documents, please correct."), any(),
                consumerArgumentCaptor.capture());

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals("failedSending", sscsCaseData.getHmctsDwpState());
    }

    @Test
    void givenAnIdamException_shouldThrowAnExceptionAndFlagAnError() {

        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(singletonList("paper"));
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        final FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn("error");
        when(idamService.getIdamTokens()).thenThrow(feignException).thenReturn(IdamTokens.builder().build());

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();
        when(documentRequestFactory.create(caseDetails.getCaseData(), nowString)).thenReturn(holder);

        handler.handle(CallbackType.SUBMITTED, callback);

        then(updateCcdCaseService)
            .should(times(1))
            .updateCaseV2(eq(123L), eq(EventType.SENT_TO_DWP_ERROR.getCcdType()), eq("Send to FTA Error"),
                eq("Unable to contact idam, please try again by running the \"Send to FTA\"."), any(),
                consumerArgumentCaptor.capture());

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals("failedSending", sscsCaseData.getHmctsDwpState());
    }

    @Test
    void givenAMessageWhichCannotFindATemplate_thenConvertToSscsCaseDataAndDoNotAddPdfToCase() {

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED,
            false);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoMoreInteractions(documentManagementServiceWrapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Online", "COR"})
    void nonReceivedViaPaperCases_doesNotGetSentToBulkPrint(String receivedVia) {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", receivedVia, null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED,
            false);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoMoreInteractions(documentManagementServiceWrapper);
    }

    @Test
    void givenADigitalCase_doesNotGetSentToBulkPrint() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", null, APPEAL_CREATED);
        caseDetails.getCaseData().setCreatedInGapsFrom("readyToList");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED,
            false);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoMoreInteractions(documentManagementServiceWrapper);

        verify(updateCcdCaseService)
            .updateCaseV2(eq(123L), eq(SENT_TO_DWP.getCcdType()), eq("Sent to FTA"),
                eq("Case state is now sent to FTA"), any(), consumerArgumentCaptor.capture());

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(SENT_TO_DWP.getCcdType(), sscsCaseData.getHmctsDwpState());
        assertEquals(UNREGISTERED, sscsCaseData.getDwpState());
    }

    @Test
    void givenNoUuiIdReturnedFromBulkPrint_thenFlagHmctsDwpStateToFailedSending() {

        when(evidenceShareConfig.getSubmitTypes()).thenReturn(singletonList("paper"));

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", singletonList(
            SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(docPdf.getName())
                .documentType("sscs1")
                .documentLink(DocumentLink.builder().documentUrl(docUrl)
                    .documentFilename(docPdf.getName()).build())
                .build()).build()
        ), APPEAL_CREATED);

        when(pdfStoreService.download(eq(docUrl))).thenReturn(docPdf.getContent());
        when(documentManagementServiceWrapper.checkIfDlDocumentAlreadyExists(anyList())).thenReturn(true);

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();

        when(documentRequestFactory.create(caseDetails.getCaseData(), nowString)).thenReturn(holder);

        Optional<UUID> expectedOptionalUuid = Optional.empty();

        when(bulkPrintService.sendToBulkPrint(anyList(), any(), any())).thenReturn(expectedOptionalUuid);

        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED,
            false);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService)
            .updateCaseV2(eq(123L), eq(EventType.SENT_TO_DWP_ERROR.getCcdType()), eq("Send to FTA Error"),
                eq("Send to FTA Error event has been triggered from Evidence Share service"), any(),
                consumerArgumentCaptor.capture());

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals("failedSending", sscsCaseData.getHmctsDwpState());
    }

    @Test
    void givenChildMaintenanceValidEventAndFeatureToggledOn_shouldNotSentToDwp() {

        bulkPrintCaseHappyPathSetUp(Benefit.CHILD_SUPPORT.getShortName(), EventType.VALID_APPEAL_CREATED);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoMoreInteractions(updateCcdCaseService);

    }

    @ParameterizedTest
    @CsvSource({
        "PIP, VALID_APPEAL",
        "childSupport, VALID_APPEAL",
        "PIP, VALID_APPEAL_CREATED"
    })
    void givenNonChildMaintenanceOrNonValidEvent_shouldCallUpdateCcdCaseService(String benefitType, String eventType) {

        bulkPrintCaseHappyPathSetUp(benefitType, EventType.valueOf(eventType));

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(123L), eq(SENT_TO_DWP.getCcdType()), any(), any(), any(),
            consumerArgumentCaptor.capture());
    }

    private void bulkPrintCaseHappyPathSetUp(String benefitType, EventType eventType) {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails(benefitType, "Paper",
            singletonList(
                SscsDocument.builder().value(SscsDocumentDetails.builder()
                    .documentFileName(docPdf.getName())
                    .documentType("sscs1")
                    .documentLink(DocumentLink.builder().documentUrl(docUrl)
                        .documentFilename(docPdf.getName()).build())
                    .build()).build()), VALID_APPEAL);

        when(callback.getEvent()).thenReturn(eventType);
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(singletonList("paper"));
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(pdfStoreService.download(eq(docUrl))).thenReturn(docPdf.getContent());
        when(documentManagementServiceWrapper.checkIfDlDocumentAlreadyExists(anyList())).thenReturn(true);
        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();
        when(documentRequestFactory.create(caseDetails.getCaseData(), nowString)).thenReturn(holder);
        when(bulkPrintService.sendToBulkPrint(eq(List.of(docPdf)), any(), any()))
            .thenReturn(Optional.of(UUID.randomUUID()));
    }

    private Callback<SscsCaseData> setupMocksForFlagErrorTests() {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");
        Template template = new Template("bla", "bla2");

        when(pdfStoreService.download(eq(docUrl))).thenReturn(docPdf.getContent());
        when(documentManagementServiceWrapper.checkIfDlDocumentAlreadyExists(anyList())).thenReturn(true);

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();
        when(documentRequestFactory.create(caseDetails.getCaseData(), nowString)).thenReturn(holder);

        return new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);
    }

    private CaseDetails<SscsCaseData> getCaseDetails(String benefitType, String receivedVia, List<SscsDocument> sscsDocuments,
                                                     State state) {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("123")
            .createdInGapsFrom("validAppeal")
            .caseCreated(nowString)
            .translationWorkOutstanding("No")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(benefitType).build())
                .receivedVia(receivedVia)
                .build())
            .sscsDocument(sscsDocuments)
            .hmctsDwpState("hello")
            .build();

        return new CaseDetails<>(
            123L,
            "jurisdiction",
            state,
            caseData,
            now,
            "Benefit"
        );
    }
}
