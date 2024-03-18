package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.springframework.web.client.ResourceAccessException;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentManagementService;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.PdfStoreException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.UnableToContactThirdPartyException;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


public class DocumentManagementServiceWrapperTest {

    private final DocumentManagementService documentManagementService = mock(DocumentManagementService.class);
    private final CcdService ccdService = mock(CcdService.class);
    private final Pdf pdf = mock(Pdf.class);

    private final DocumentManagementServiceWrapper service =
        new DocumentManagementServiceWrapper(documentManagementService, ccdService, 3);
    private final DocumentHolder holder = DocumentHolder.builder().build();
    private final SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("1").build();
    private final IdamTokens idamTokens = IdamTokens.builder().build();

    @Test
    public void successfulCallToDocumentManagementService_willBeCalledOnce() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any())).thenReturn(pdf);

        service.generateDocumentAndAddToCcd(holder, caseData, IdamTokens.builder().build());

        verify(documentManagementService).generateDocumentAndAddToCcd(eq(holder), eq(caseData));
        verifyNoMoreInteractions(documentManagementService);
    }

    @Test
    public void givenADlTemplateDoesNotExistOnCase_generateTheTemplate() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any())).thenReturn(pdf);
        SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType("appellantEvidence").build()).build();
        List<SscsDocument> documents = new ArrayList<>();
        documents.add(doc);
        when(ccdService.getByCaseId(any(), any())).thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().sscsDocument(documents).build()).build());

        service.generateDocumentAndAddToCcd(holder, caseData, IdamTokens.builder().build());

        verify(documentManagementService).generateDocumentAndAddToCcd(eq(holder), eq(caseData));
        verifyNoMoreInteractions(documentManagementService);
    }

    @Test
    public void givenACaseHasNoDocuments_generateTheDlTemplate() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any())).thenReturn(pdf);
        when(ccdService.getByCaseId(any(), any())).thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        service.generateDocumentAndAddToCcd(holder, caseData, IdamTokens.builder().build());

        verify(documentManagementService).generateDocumentAndAddToCcd(eq(holder), eq(caseData));
        verifyNoMoreInteractions(documentManagementService);
    }

    @Test
    public void givenADl6AlreadyExistsOnCase_skipGeneratingTheTemplate() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any())).thenReturn(pdf);
        SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType("DL6").build()).build();
        List<SscsDocument> documents = new ArrayList<>();
        documents.add(doc);
        when(ccdService.getByCaseId(any(), any())).thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().sscsDocument(documents).build()).build());

        service.generateDocumentAndAddToCcd(holder, caseData, IdamTokens.builder().build());

        verifyNoMoreInteractions(documentManagementService);
    }

    @Test
    public void givenADl16AlreadyExistsOnCase_skipGeneratingTheTemplate() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any())).thenReturn(pdf);
        SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType("DL16").build()).build();
        List<SscsDocument> documents = new ArrayList<>();
        documents.add(doc);
        when(ccdService.getByCaseId(any(), any())).thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().sscsDocument(documents).build()).build());

        service.generateDocumentAndAddToCcd(holder, caseData, IdamTokens.builder().build());

        verifyNoMoreInteractions(documentManagementService);
    }

    @Test(expected = UnableToContactThirdPartyException.class)
    public void whenPdfGenerationExceptionAIsThrownServiceCallWillNotBeRetried() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any()))
            .thenThrow(new PdfGenerationException("a message", new RuntimeException("blah")));
        try {
            service.generateDocumentAndAddToCcd(holder, caseData, idamTokens);
        } catch (PdfStoreException e) {
            verify(documentManagementService, atLeastOnce()).generateDocumentAndAddToCcd(eq(holder), eq(caseData));
            verifyNoMoreInteractions(documentManagementService);
            throw e;
        }
    }

    @Test(expected = UnableToContactThirdPartyException.class)
    public void whenResourceAccessExceptionIsThrownServiceCallWillNotBeRetried() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any()))
            .thenThrow(new ResourceAccessException("a message", new IOException("blah")));
        try {
            service.generateDocumentAndAddToCcd(holder, caseData, idamTokens);
        } catch (PdfStoreException e) {
            verify(documentManagementService, atLeastOnce()).generateDocumentAndAddToCcd(eq(holder), eq(caseData));
            verifyNoMoreInteractions(documentManagementService);
            throw e;
        }
    }

    @Test
    public void anExceptionWillBeCaughtAndRetriedUntilSuccessful() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any()))
            .thenThrow(new RuntimeException("blah"))
            .thenThrow(new RuntimeException("blah"))
            .thenReturn(pdf);

        service.generateDocumentAndAddToCcd(holder, caseData, idamTokens);

        verify(documentManagementService, times(3)).generateDocumentAndAddToCcd(eq(holder), eq(caseData));
        verifyNoMoreInteractions(documentManagementService);
    }

    @Test(expected = RuntimeException.class)
    public void anExceptionWillBeCaughtAndRetriedUntilItFails() {
        when(documentManagementService.generateDocumentAndAddToCcd(any(), any()))
            .thenThrow(new RuntimeException("blah"))
            .thenThrow(new RuntimeException("blah"))
            .thenThrow(new RuntimeException("blah"));

        try {
            service.generateDocumentAndAddToCcd(holder, caseData, idamTokens);
        } catch (Exception e) {
            verify(documentManagementService, atLeast(3)).generateDocumentAndAddToCcd(any(), any());
            throw e;
        }
    }
}
