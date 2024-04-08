package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.UNREGISTERED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;

import feign.FeignException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
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

@RunWith(JUnitParamsRunner.class)
public class SendToBulkPrintHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

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
    private CcdService ccdCaseService;

    @Mock
    private IdamService idamService;

    @Mock
    private Callback<SscsCaseData> callback;

    private SendToBulkPrintHandler handler;

    private LocalDateTime now = LocalDateTime.now();

    private String nowString = (DateTimeFormatter.ISO_LOCAL_DATE).format(now);

    @Captor
    private ArgumentCaptor<SscsCaseData> caseDataCaptor;

    private final String docUrl = "my/1/url.pdf";
    private final Pdf docPdf = new Pdf(docUrl.getBytes(), "evidence1.pdf");
    private final Pdf docPdf2 = new Pdf(docUrl.getBytes(), "evidence2.pdf");

    private final CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", singletonList(
        SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentFileName(docPdf.getName())
            .documentType("sscs1")
            .evidenceIssued("No")
            .documentLink(DocumentLink.builder().documentUrl(docUrl)
                .documentFilename(docPdf.getName()).build())
            .build()).build()), APPEAL_CREATED);


    Map<String, Object> placeholders = new HashMap<>();

    Template template = new Template("bla", "bla2");

    @Before
    public void setUp() {
        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);
        handler = new SendToBulkPrintHandler(documentManagementServiceWrapper,
            documentRequestFactory, pdfStoreService, bulkPrintService, evidenceShareConfig,
            ccdCaseService, idamService, 35, 42);
        when(evidenceShareConfig.getSubmitTypes()).thenReturn(singletonList("paper"));
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        placeholders.put("Test", "Value");
    }

    @Test
    public void givenAValidAppealCreatedEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonBulkPrintEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    @Parameters({"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL", "RESEND_TO_DWP", "APPEAL_TO_PROCEED"})
    public void givenAValidSendToBulkPrintEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonSendToBulkPrintEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenSendToDwpWithDocTranslated_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.SEND_TO_DWP);
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenDocTranslationOutstanding_thenReturnFalse() {
        caseDetails.getCaseData().setTranslationWorkOutstanding("Yes");
        caseDetails.getCaseData().isTranslationWorkOutstanding();
        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenResendToDwpWithDocTranslationOutstanding_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.RESEND_TO_DWP);
        caseDetails.getCaseData().setTranslationWorkOutstanding("Yes");
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    @Parameters({"pip, 35", "childSupport, 42"})
    public void givenAMessageWhichFindsATemplate_thenConvertToSscsCaseDataAndAddPdfToCaseAndSendToBulkPrint(String benefitType, int expectedResponseDays) {

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

        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(pdfStoreService, times(2)).download(eq(docUrl));
        verify(bulkPrintService).sendToBulkPrint(eq(Arrays.asList(docPdf, docPdf2)), any(), any());

        String documentList = "Case has been sent to the FTA via Bulk Print with bulk print id: 0f14d0ab-9605-4a62-a9e4-5ed26688389b and with documents: evidence1.pdf, evidence2.pdf";
        verify(ccdCaseService).updateCase(caseDataCaptor.capture(), eq(123L), eq(EventType.SENT_TO_DWP.getCcdType()), eq("Sent to FTA"), eq(documentList), any());

        List<SscsDocument> docs = caseDataCaptor.getValue().getSscsDocument();
        assertNull(docs.get(0).getValue().getEvidenceIssued());
        assertEquals("sentToDwp", caseDataCaptor.getValue().getHmctsDwpState());
        assertEquals(LocalDate.now().toString(), caseDataCaptor.getValue().getDateSentToDwp());
        assertEquals(LocalDate.now().plusDays(expectedResponseDays).toString(), caseDataCaptor.getValue().getDwpDueDate());
        assertNull(caseDataCaptor.getValue().getDwpState());
    }

    protected Callback<SscsCaseData> setupMocksForFlagErrorTests() {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");
        Template template = new Template("bla", "bla2");

        when(pdfStoreService.download(eq(docUrl))).thenReturn(docPdf.getContent());
        when(documentManagementServiceWrapper.checkIfDlDocumentAlreadyExists(anyList())).thenReturn(true);

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(template).build();
        when(documentRequestFactory.create(caseDetails.getCaseData(), nowString)).thenReturn(holder);

        return new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);
    }

    @Test
    public void givenAnErrorWhenSendToBulkPrint_shouldUpdateCaseInCcdToFlagError() {
        Callback<SscsCaseData> callback = setupMocksForFlagErrorTests();
        ArgumentCaptor<SscsCaseData> caseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);

        handler.handle(CallbackType.SUBMITTED, callback);
        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwpError"), any(), any(), any());

        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());

        List<SscsDocument> docs = caseDataCaptor.getValue().getSscsDocument();
        assertEquals("No", docs.get(0).getValue().getEvidenceIssued());
    }

    @Test
    public void givenAnErrorIfDlDocumentsNotPresentWhenSendToBulkPrint_shouldUpdateCaseInCcdToFlagError() {
        Callback<SscsCaseData> callback = setupMocksForFlagErrorTests();
        when(documentManagementServiceWrapper.checkIfDlDocumentAlreadyExists(anyList())).thenReturn(false);

        handler.handle(CallbackType.SUBMITTED, callback);

        ArgumentCaptor<SscsCaseData> caseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);
        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwpError"), eq("Send to FTA Error"), eq("Triggered from Evidence Share – no DL6/16 present, please validate."), any());

        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());

        List<SscsDocument> docs = caseDataCaptor.getValue().getSscsDocument();
        assertEquals("No", docs.get(0).getValue().getEvidenceIssued());
    }

    @Test
    public void givenNoTemplates_shouldThrowAnExceptionAndFlagError() {
        Callback<SscsCaseData> callback = setupMocksForFlagErrorTests();
        when(documentRequestFactory.create(caseDetails.getCaseData(), nowString))
            .thenReturn(DocumentHolder.builder().template(null).build());

        handler.handle(CallbackType.SUBMITTED, callback);

        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwpError"), eq("Send to FTA Error"), eq("Send to FTA Error event has been triggered from Evidence Share service"), any());
        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());
    }

    @Test
    public void givenNoBulkPrintIdReturned_shouldThrowAnExceptionAndFlagError() {
        Callback<SscsCaseData> callback = setupMocksForFlagErrorTests();

        when(bulkPrintService.sendToBulkPrint(eq(Arrays.asList(docPdf, docPdf2)), any(), any()))
            .thenReturn(Optional.empty());

        handler.handle(CallbackType.SUBMITTED, callback);

        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwpError"), eq("Send to FTA Error"), eq("Send to FTA Error event has been triggered from Evidence Share service"), any());
        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());
    }

    @Test
    public void givenAErrorDownloadingDocuments_shouldThrowAnExceptionAndFlagAnError() {
        final Callback<SscsCaseData> callback = setupMocksForFlagErrorTests();

        final FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn("error");
        when(pdfStoreService.download(eq(docUrl))).thenThrow(feignException);
        when(documentManagementServiceWrapper.checkIfDlDocumentAlreadyExists(anyList())).thenReturn(true);

        handler.handle(CallbackType.SUBMITTED, callback);

        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwpError"), eq("Send to FTA Error"), eq("Unable to contact dm-store, please try again by running the \"Send to FTA\"."), any());
        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());
    }

    @Test
    public void givenABrokenPdfException_shouldThrowAnExceptionAndFlagAnError() {
        final Callback<SscsCaseData> callback = setupMocksForFlagErrorTests();

        when(bulkPrintService.sendToBulkPrint(any(), any(), any()))
            .thenThrow(new NonPdfBulkPrintException(new RuntimeException("error")));

        handler.handle(CallbackType.SUBMITTED, callback);
        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwpError"), eq("Send to FTA Error"), eq("Non-PDFs/broken PDFs seen in list of documents, please correct."), any());
        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());
    }

    @Test
    public void givenAnIdamException_shouldThrowAnExceptionAndFlagAnError() {
        final Callback<SscsCaseData> callback = setupMocksForFlagErrorTests();

        final FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn("error");
        when(idamService.getIdamTokens()).thenThrow(feignException).thenReturn(IdamTokens.builder().build());

        handler.handle(CallbackType.SUBMITTED, callback);
        then(ccdCaseService)
            .should(times(1))
            .updateCase(caseDataCaptor.capture(), eq(123L), eq("sendToDwpError"), eq("Send to FTA Error"), eq("Unable to contact idam, please try again by running the \"Send to FTA\"."), any());
        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());
    }

    @Test
    public void givenAMessageWhichCannotFindATemplate_thenConvertToSscsCaseDataAndDoNotAddPdfToCase() {

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");

        DocumentHolder holder = DocumentHolder.builder().placeholders(placeholders).template(null).build();

        when(documentRequestFactory.create(caseDetails.getCaseData(), nowString)).thenReturn(holder);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoMoreInteractions(documentManagementServiceWrapper);
    }

    @Test
    @Parameters({"Online", "COR"})
    public void nonReceivedViaPaperCases_doesNotGetSentToBulkPrint(String receivedVia) {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", receivedVia, null, APPEAL_CREATED);
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoMoreInteractions(documentManagementServiceWrapper);
    }

    @Test
    public void givenADigitalCase_doesNotGetSentToBulkPrint() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", null, APPEAL_CREATED);
        caseDetails.getCaseData().setCreatedInGapsFrom("readyToList");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);

        handler.handle(CallbackType.SUBMITTED, callback);

        verifyNoMoreInteractions(documentManagementServiceWrapper);

        verify(ccdCaseService).updateCase(caseDataCaptor.capture(), eq(123L), eq(EventType.SENT_TO_DWP.getCcdType()), eq("Sent to FTA"), eq("Case state is now sent to FTA"), any());

        assertEquals("sentToDwp", caseDataCaptor.getValue().getHmctsDwpState());
        assertEquals(UNREGISTERED, caseDataCaptor.getValue().getDwpState());
    }

    @Test
    public void givenNoUuiIdReturnedFromBulkPrint_thenFlagHmctsDwpStateToFailedSending() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("PIP", "Paper", Arrays.asList(
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

        when(bulkPrintService.sendToBulkPrint(any(), any(), any())).thenReturn(expectedOptionalUuid);

        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.VALID_APPEAL_CREATED, false);

        handler.handle(CallbackType.SUBMITTED, callback);

        verify(ccdCaseService).updateCase(caseDataCaptor.capture(), eq(123L), eq(EventType.SENT_TO_DWP_ERROR.getCcdType()), eq("Send to FTA Error"), eq("Send to FTA Error event has been triggered from Evidence Share service"), any());

        assertEquals("failedSending", caseDataCaptor.getValue().getHmctsDwpState());
    }

    private CaseDetails<SscsCaseData> getCaseDetails(String benefitType, String receivedVia, List<SscsDocument> sscsDocuments, State state) {
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
