package uk.gov.hmcts.sscs.service;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.sscs.model.ByteArrayMultipartFile;

public class PdfStoreServiceTest {

    private final String expectedHref = "some link";
    private final byte[] content = new byte[]{1, 2, 3};
    private final String filename = "filename";
    private final EvidenceManagementService evidenceManagementService;
    private final List<MultipartFile> files;

    public PdfStoreServiceTest() {
        evidenceManagementService = mock(EvidenceManagementService.class);
        files = singletonList(new ByteArrayMultipartFile(content, filename, APPLICATION_PDF));
    }

    @Test
    public void uploadsPdfAndExtractsLink() {
        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(files)).thenReturn(uploadResponse);

        List<SscsDocument> documents = new PdfStoreService(evidenceManagementService).store(content, filename);

        assertThat(documents.size(), is(1));
        SscsDocumentDetails value = documents.get(0).getValue();
        assertThat(value.getDocumentFileName(), is(filename));
        assertThat(value.getDocumentLink().getDocumentUrl(), is(expectedHref));
    }

    @Test
    public void cannotConnectToDocumentStore() {
        when(evidenceManagementService.upload(files)).thenThrow(new RestClientException("Cannot connect"));
        List<SscsDocument> documents = new PdfStoreService(evidenceManagementService).store(content, filename);

        assertThat(documents.size(), is(0));
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