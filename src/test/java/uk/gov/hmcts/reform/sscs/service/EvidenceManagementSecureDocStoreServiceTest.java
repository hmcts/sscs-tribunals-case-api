package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClient;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.UnsupportedDocumentTypeException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class EvidenceManagementSecureDocStoreServiceTest {

    public static final String SERVICE_AUTHORIZATION = "service-authorization";
    private static final IdamTokens IDAM_TOKENS = IdamTokens.builder().userId("123").roles(List.of("caseworker")).idamOauth2Token("idamOauth2Token").serviceAuthorization(SERVICE_AUTHORIZATION).build();

    @Mock
    private CaseDocumentClient caseDocumentClient;

    private EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService;

    @Before
    public void setUp() {
        openMocks(this);
        evidenceManagementSecureDocStoreService = new EvidenceManagementSecureDocStoreService(caseDocumentClient);
    }

    @Test
    public void uploadDocumentShouldCallUploadDocumentManagementClient() {

        List<MultipartFile> files = Collections.emptyList();

        UploadResponse expectedUploadResponse = mock(UploadResponse.class);

        when(caseDocumentClient.uploadDocuments(eq(IDAM_TOKENS.getIdamOauth2Token()), eq(SERVICE_AUTHORIZATION), eq("Benefit"), eq("SSCS"), any()))
                .thenReturn(expectedUploadResponse);

        UploadResponse actualUploadedResponse = evidenceManagementSecureDocStoreService.upload(files, IDAM_TOKENS);

        verify(caseDocumentClient, times(1))
                .uploadDocuments(eq(IDAM_TOKENS.getIdamOauth2Token()), eq(SERVICE_AUTHORIZATION), eq("Benefit"), eq("SSCS"), any());

        assertEquals(actualUploadedResponse, expectedUploadResponse);
    }

    @Test(expected = UnsupportedDocumentTypeException.class)
    public void uploadDocumentShouldThrowUnSupportedDocumentTypeExceptionIfAnyGivenDocumentTypeIsNotSupportedByDocumentStore() {
        List<MultipartFile> files = mockMultipartFiles();

        when(caseDocumentClient.uploadDocuments(eq(IDAM_TOKENS.getIdamOauth2Token()), eq(SERVICE_AUTHORIZATION), eq("Benefit"), eq("SSCS"), any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY));

        evidenceManagementSecureDocStoreService.upload(files, IDAM_TOKENS);

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

        when(caseDocumentClient.uploadDocuments(eq(IDAM_TOKENS.getIdamOauth2Token()), eq(SERVICE_AUTHORIZATION), eq("Benefit"), eq("SSCS"), any()))
                .thenThrow(new Exception("AppealNumber"));

        evidenceManagementSecureDocStoreService.upload(files, IDAM_TOKENS);
    }

    @Test
    public void downloadDocumentShouldDownloadSpecifiedDocument() {
        ResponseEntity<Resource> mockResponseEntity = mock(ResponseEntity.class);
        ByteArrayResource stubbedResource = new ByteArrayResource(new byte[] {});
        when(mockResponseEntity.getBody()).thenReturn(stubbedResource);


        Document.Link stubbedLink = new Document.Link();
        stubbedLink.href = "http://localhost:4506/documents/eb8cbfaa-37c3-4644-aa77-b9a2e2c72332";
        Document.Links stubbedLinks = new Document.Links();
        stubbedLinks.binary = stubbedLink;

        String documentHref = URI.create(stubbedLink.href).getPath().replaceFirst("/", "");
        when(caseDocumentClient.getDocumentBinary(IDAM_TOKENS.getIdamOauth2Token(), SERVICE_AUTHORIZATION, documentHref)).thenReturn(mockResponseEntity);

        evidenceManagementSecureDocStoreService.download(stubbedLink.href, IDAM_TOKENS);

        verify(mockResponseEntity, times(1)).getBody();
    }

    @Test
    public void downloadDocumentWhenBodyIsEmpty() {
        ResponseEntity<Resource> mockResponseEntity = mock(ResponseEntity.class);
        when(mockResponseEntity.getBody()).thenReturn(null);

        Document.Link stubbedLink = new Document.Link();
        stubbedLink.href = "http://localhost:4506/documents/eb8cbfaa-37c3-4644-aa77-b9a2e2c72332";
        Document.Links stubbedLinks = new Document.Links();
        stubbedLinks.binary = stubbedLink;

        String documentHref = URI.create(stubbedLink.href).getPath().replaceFirst("/", "");
        when(caseDocumentClient.getDocumentBinary(IDAM_TOKENS.getIdamOauth2Token(), SERVICE_AUTHORIZATION, documentHref)).thenReturn(mockResponseEntity);

        evidenceManagementSecureDocStoreService.download(stubbedLink.href, IDAM_TOKENS);

        verify(mockResponseEntity, times(1)).getBody();
    }

    @Test(expected = UnsupportedDocumentTypeException.class)
    public void downloadDocumentShouldThrowExceptionWhenDocumentNotFound() {
        ResponseEntity<Resource> mockResponseEntity = mock(ResponseEntity.class);
        ByteArrayResource stubbedResource = new ByteArrayResource(new byte[] {});
        when(mockResponseEntity.getBody()).thenReturn(stubbedResource);

        Document.Link stubbedLink = new Document.Link();
        stubbedLink.href = "http://localhost:4506/documents/eb8cbfaa-37c3-4644-aa77-b9a2e2c72332";
        Document.Links stubbedLinks = new Document.Links();
        stubbedLinks.binary = stubbedLink;

        String documentHref = URI.create(stubbedLink.href).getPath().replaceFirst("/", "");
        when(caseDocumentClient.getDocumentBinary(IDAM_TOKENS.getIdamOauth2Token(), IDAM_TOKENS.getServiceAuthorization(), documentHref)).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        evidenceManagementSecureDocStoreService.download(stubbedLink.href, IDAM_TOKENS);
    }

    @Test(expected = Exception.class)
    public void downloadDocumentShouldRethrowAnyExceptionIfItsNotHttpClientErrorException() {
        Document.Link stubbedLink = new Document.Link();
        stubbedLink.href = "http://localhost:4506/documents/eb8cbfaa-37c3-4644-aa77-b9a2e2c72332";
        Document.Links stubbedLinks = new Document.Links();
        stubbedLinks.binary = stubbedLink;

        when(caseDocumentClient.getDocumentBinary(IDAM_TOKENS.getIdamOauth2Token(), IDAM_TOKENS.getServiceAuthorization(), stubbedLink.href)).thenThrow(new Exception("AppealNumber"));

        evidenceManagementSecureDocStoreService.download(stubbedLink.href, IDAM_TOKENS);
    }
}
