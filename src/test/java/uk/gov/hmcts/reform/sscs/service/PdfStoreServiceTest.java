package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import feign.FeignException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class PdfStoreServiceTest {

    private final String expectedHref = "some link";
    private final byte[] content = new byte[]{1, 2, 3};
    private final String filename = "filename";
    public static final String SSCS_USER = "sscs";

    private final EvidenceManagementService evidenceManagementService;
    private final EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService;
    private final PdfStoreService pdfStoreService;
    private final PdfStoreService pdfStoreSecureDocStore;
    private final IdamService idamService;
    private final List<MultipartFile> files;

    public PdfStoreServiceTest() {
        evidenceManagementService = mock(EvidenceManagementService.class);
        evidenceManagementSecureDocStoreService = mock(EvidenceManagementSecureDocStoreService.class);
        idamService = mock(IdamService.class);
        pdfStoreService = new PdfStoreService(evidenceManagementService, evidenceManagementSecureDocStoreService, false, idamService);
        pdfStoreSecureDocStore = new PdfStoreService(evidenceManagementService, evidenceManagementSecureDocStoreService, true, idamService);

        files = singletonList(ByteArrayMultipartFile.builder().content(content).name(filename).contentType(APPLICATION_PDF).build());
    }

    @Test
    public void uploadsPdfAndExtractsLink() {
        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(files, SSCS_USER)).thenReturn(uploadResponse);

        List<SscsDocument> documents = pdfStoreService.store(content, filename, "appellantEvidence");

        assertThat(documents.size(), is(1));
        SscsDocumentDetails value = documents.get(0).getValue();
        assertThat(value.getDocumentFileName(), is(filename));
        assertThat(value.getDocumentLink().getDocumentUrl(), is(expectedHref));
    }

    @Test
    public void cannotConnectToDocumentStore() {
        when(evidenceManagementService.upload(files, SSCS_USER)).thenThrow(new RestClientException("Cannot connect"));
        List<SscsDocument> documents = pdfStoreService.store(content, filename, "appellantEvidence");

        assertThat(documents.size(), is(0));
    }

    @Test
    public void cannotConnectToDocumentStoreWithDocumentTranslationStatus() {
        when(evidenceManagementService.upload(files, SSCS_USER)).thenThrow(new RestClientException("Cannot connect"));
        List<SscsDocument> documents = pdfStoreService.store(content, filename, "appellantEvidence", SscsDocumentTranslationStatus.TRANSLATION_COMPLETE);

        assertThat(documents.size(), is(0));
    }

    @Test
    public void uploadsPdfAndExtractsLinkForSecureDocStore() {
        uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse uploadResponse = createUploadResponseSecureDocStore();
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token("idamOauth2Token").build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(evidenceManagementSecureDocStoreService.upload(files, idamTokens)).thenReturn(uploadResponse);

        List<SscsDocument> documents = pdfStoreSecureDocStore.store(content, filename, "appellantEvidence");

        assertThat(documents.size(), is(1));
        SscsDocumentDetails value = documents.get(0).getValue();
        assertThat(value.getDocumentFileName(), is(filename));
        assertThat(value.getDocumentLink().getDocumentUrl(), is(expectedHref));
    }

    @Test
    public void uploadsPdfAndExtractsLinkForSecureDocStoreWithDocumentTranslationStatus() {
        uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse uploadResponse = createUploadResponseSecureDocStore();
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token("idamOauth2Token").build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(evidenceManagementSecureDocStoreService.upload(files, idamTokens)).thenReturn(uploadResponse);

        List<SscsDocument> documents = pdfStoreSecureDocStore.store(content, filename, "appellantEvidence", SscsDocumentTranslationStatus.TRANSLATION_COMPLETE);

        assertThat(documents.size(), is(1));
        SscsDocumentDetails value = documents.get(0).getValue();
        assertThat(value.getDocumentFileName(), is(filename));
        assertThat(value.getDocumentLink().getDocumentUrl(), is(expectedHref));
    }

    @Test
    public void uploadsPdfForSecureDocStoreWhenExceptionIsThrown() {
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token("idamOauth2Token").build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(evidenceManagementSecureDocStoreService.upload(files, idamTokens)).thenThrow(RestClientException.class);

        List<SscsDocument> documents = pdfStoreSecureDocStore.store(content, filename, "appellantEvidence", SscsDocumentTranslationStatus.TRANSLATION_COMPLETE);

        assertThat(documents.size(), is(0));
    }

    @Test
    public void downloadPdfIfNotSecureDocStore() {
        when(evidenceManagementService.download(any(URI.class), eq(SSCS_USER))).thenReturn(content);
        byte[] expectedContent = pdfStoreService.download("http://test");

        assertThat(expectedContent, is(content));
        verify(evidenceManagementService).download(any(URI.class), eq(SSCS_USER));
        verifyNoInteractions(evidenceManagementSecureDocStoreService);
    }

    @Test
    public void downloadPdfSecureDocStore() {
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token("idamOauth2Token").build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(evidenceManagementSecureDocStoreService.download(any(), eq(idamTokens))).thenReturn(content);
        byte[] expectedContent = pdfStoreSecureDocStore.download("http://test");

        assertThat(expectedContent, is(content));
        verify(evidenceManagementSecureDocStoreService).download(any(), eq(idamTokens));
        verifyNoInteractions(evidenceManagementService);
    }

    @Test
    public void downloadPdfSecureDocStoreWhenThrowException() {
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token("idamOauth2Token").build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(evidenceManagementService.download(any(URI.class), eq(SSCS_USER))).thenReturn(content);
        when(evidenceManagementSecureDocStoreService.download(any(), eq(idamTokens))).thenThrow(FeignException.class);
        byte[] expectedContent = pdfStoreSecureDocStore.download("http://test");

        assertThat(expectedContent, is(content));
        verify(evidenceManagementSecureDocStoreService).download(any(), eq(idamTokens));
        verify(evidenceManagementService).download(any(URI.class), eq(SSCS_USER));
    }

    private uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse createUploadResponseSecureDocStore() {
        uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse response = mock(uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse.class);
        uk.gov.hmcts.reform.ccd.document.am.model.Document document = createDocumentSecureDocStore();
        when(response.getDocuments()).thenReturn(Collections.singletonList(document));
        return response;
    }

    private uk.gov.hmcts.reform.ccd.document.am.model.Document createDocumentSecureDocStore() {
        uk.gov.hmcts.reform.ccd.document.am.model.Document.Links links = new uk.gov.hmcts.reform.ccd.document.am.model.Document.Links();
        uk.gov.hmcts.reform.ccd.document.am.model.Document.Link link = new uk.gov.hmcts.reform.ccd.document.am.model.Document.Link();
        link.href = expectedHref;
        links.self = link;
        return uk.gov.hmcts.reform.ccd.document.am.model.Document.builder().links(links).build();
    }

    private UploadResponse createUploadResponse() {
        UploadResponse response = mock(UploadResponse.class);
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(response.getEmbedded()).thenReturn(embedded);
        Document document = createDocument();
        when(embedded.getDocuments()).thenReturn(Collections.singletonList(document));
        return response;
    }

    private Document createDocument() {
        Document document = new Document();
        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = expectedHref;
        links.self = link;
        document.links = links;
        return document;
    }

}