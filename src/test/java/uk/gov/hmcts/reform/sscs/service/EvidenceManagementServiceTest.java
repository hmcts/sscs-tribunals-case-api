package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.UnsupportedDocumentTypeException;
import uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement.EvidenceDownloadClientApi;
import uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement.EvidenceMetadataDownloadClientApi;

public class EvidenceManagementServiceTest {

    public static final String SERVICE_AUTHORIZATION = "service-authorization";
    public static final String SSCS_USER = "sscs";

    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private DocumentUploadClientApi documentUploadClientApi;
    @Mock
    private EvidenceMetadataDownloadClientApi evidenceMetadataDownloadClientApi;
    @Mock
    private EvidenceDownloadClientApi evidenceDownloadClientApi;

    private EvidenceManagementService evidenceManagementService;

    @Before
    public void setUp() {
        openMocks(this);
        evidenceManagementService = new EvidenceManagementService(authTokenGenerator, documentUploadClientApi, evidenceDownloadClientApi, evidenceMetadataDownloadClientApi);
    }

    @Test
    public void uploadDocumentShouldCallUploadDocumentManagementClient() {

        List<MultipartFile> files = Collections.emptyList();

        UploadResponse expectedUploadResponse = mock(UploadResponse.class);

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION);
        when(documentUploadClientApi.upload(any(), eq(SERVICE_AUTHORIZATION), any(), any(), any(), eq(files)))
                .thenReturn(expectedUploadResponse);

        UploadResponse actualUploadedResponse = evidenceManagementService.upload(files, SSCS_USER);

        verify(documentUploadClientApi, times(1))
                .upload(any(), eq(SERVICE_AUTHORIZATION), any(), any(), any(), eq(files));

        assertEquals(actualUploadedResponse, expectedUploadResponse);
    }

    @Test(expected = UnsupportedDocumentTypeException.class)
    public void uploadDocumentShouldThrowUnSupportedDocumentTypeExceptionIfAnyGivenDocumentTypeIsNotSupportedByDocumentStore() {
        List<MultipartFile> files = mockMultipartFiles();

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION);
        when(documentUploadClientApi.upload(any(), eq(SERVICE_AUTHORIZATION), any(), any(), any(), eq(files)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY));

        evidenceManagementService.upload(files, SSCS_USER);

    }

    private List<MultipartFile> mockMultipartFiles() {
        MultipartFile value = mock(MultipartFile.class);
        when(value.getName()).thenReturn("testFile.txt");
        when(value.getOriginalFilename()).thenReturn("OriginalTestFile.txt");

        List<MultipartFile> files = new ArrayList<>();
        files.add(value);
        return files;
    }

    @Test(expected = Exception.class)
    public void uploadDocumentShouldRethrowAnyExceptionIfItsNotHttpClientErrorException() {
        List<MultipartFile> files = Collections.emptyList();

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION);
        when(documentUploadClientApi.upload(any(), eq(SERVICE_AUTHORIZATION), anyString(), eq(files)))
                .thenThrow(new Exception("AppealNumber"));

        evidenceManagementService.upload(files, SSCS_USER);
    }

    @Test
    public void downloadDocumentShouldDownloadSpecifiedDocument() {
        ResponseEntity<Resource> mockResponseEntity = mock(ResponseEntity.class);
        ByteArrayResource stubbedResource = new ByteArrayResource(new byte[] {});
        when(mockResponseEntity.getBody()).thenReturn(stubbedResource);


        Document stubbedDocument = new Document();
        Document.Link stubbedLink = new Document.Link();
        stubbedLink.href = "http://localhost:4506/documents/eb8cbfaa-37c3-4644-aa77-b9a2e2c72332";
        Document.Links stubbedLinks = new Document.Links();
        stubbedLinks.binary = stubbedLink;
        stubbedDocument.links = stubbedLinks;

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION);
        when(evidenceMetadataDownloadClientApi.getDocumentMetadata(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(stubbedDocument);
        when(evidenceDownloadClientApi.downloadBinary(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockResponseEntity);

        evidenceManagementService.download(URI.create("http://localhost:4506/somefile.doc"), SSCS_USER);

        verify(mockResponseEntity, times(1)).getBody();
    }

    @Test
    public void downloadDocumentWhenBodyIsEmpty() {
        ResponseEntity<Resource> mockResponseEntity = mock(ResponseEntity.class);
        when(mockResponseEntity.getBody()).thenReturn(null);

        Document stubbedDocument = new Document();
        Document.Link stubbedLink = new Document.Link();
        stubbedLink.href = "http://localhost:4506/documents/eb8cbfaa-37c3-4644-aa77-b9a2e2c72332";
        Document.Links stubbedLinks = new Document.Links();
        stubbedLinks.binary = stubbedLink;
        stubbedDocument.links = stubbedLinks;

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION);
        when(evidenceMetadataDownloadClientApi.getDocumentMetadata(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(stubbedDocument);
        when(evidenceDownloadClientApi.downloadBinary(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockResponseEntity);

        evidenceManagementService.download(URI.create("http://localhost:4506/somefile.doc"), SSCS_USER);

        verify(mockResponseEntity, times(1)).getBody();
    }

    @Test(expected = UnsupportedDocumentTypeException.class)
    public void downloadDocumentShoudlThrowExceptionWhenDocumentNotFound() {
        ResponseEntity<Resource> mockResponseEntity = mock(ResponseEntity.class);
        ByteArrayResource stubbedResource = new ByteArrayResource(new byte[] {});
        when(mockResponseEntity.getBody()).thenReturn(stubbedResource);

        Document stubbedDocument = new Document();
        Document.Link stubbedLink = new Document.Link();
        stubbedLink.href = "http://localhost:4506/documents/eb8cbfaa-37c3-4644-aa77-b9a2e2c72332";
        Document.Links stubbedLinks = new Document.Links();
        stubbedLinks.binary = stubbedLink;
        stubbedDocument.links = stubbedLinks;

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION);
        when(evidenceMetadataDownloadClientApi.getDocumentMetadata(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(stubbedDocument);
        when(evidenceDownloadClientApi.downloadBinary(anyString(), anyString(), anyString(), anyString(), anyString())).thenThrow(new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY));

        evidenceManagementService.download(URI.create("http://localhost:4506/somefile.doc"), SSCS_USER);
    }

    @Test(expected = Exception.class)
    public void downloadDocumentShouldRethrowAnyExceptionIfItsNotHttpClientErrorException() {

        Document stubbedDocument = new Document();
        Document.Link stubbedLink = new Document.Link();
        stubbedLink.href = "http://localhost:4506/documents/eb8cbfaa-37c3-4644-aa77-b9a2e2c72332";
        Document.Links stubbedLinks = new Document.Links();
        stubbedLinks.binary = stubbedLink;
        stubbedDocument.links = stubbedLinks;

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION);
        when(evidenceMetadataDownloadClientApi.getDocumentMetadata(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(stubbedDocument);
        when(evidenceDownloadClientApi.downloadBinary(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new Exception("AppealNumber"));

        evidenceManagementService.download(URI.create("http://localhost:4506/somefile.doc"), SSCS_USER);
    }
}
