package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentDownloadClientApi;
import uk.gov.hmcts.reform.sscs.exception.DocumentNotFoundException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.pdf.data.UploadedEvidence;

@RunWith(JUnitParamsRunner.class)
public class DocumentDownloadServiceTest {

    @Mock
    private DocumentDownloadClientApi documentDownloadClientApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService;
    @Mock
    IdamService idamService;
    private DocumentDownloadService documentDownloadService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    String urlString;

    @Before
    public void setUp() {
        documentDownloadService = new DocumentDownloadService(documentDownloadClientApi,
            authTokenGenerator, "http://dm-store:4506", false,
                evidenceManagementSecureDocStoreService, idamService);
        urlString = "http://dm-store:4506/documents/someDocId/binary";
    }

    @Test
    public void givenDocumentId_shouldReturnFileSize() {
        ResponseEntity<Resource> response = ResponseEntity.ok(new ByteArrayResource("test".getBytes()));
        given(documentDownloadClientApi.downloadBinary(any(), any(), any(), any(), any()))
                .willReturn(response);
        long size = documentDownloadService
            .getFileSize("19cd94a8-4280-406b-92c7-090b735159ca");
        assertEquals(4L, size);
    }

    @Test
    public void givenDocumentId_shouldReturnFileSizeSecure() {
        documentDownloadService = new DocumentDownloadService(documentDownloadClientApi,
                authTokenGenerator, "http://dm-store:4506", true,
                evidenceManagementSecureDocStoreService, idamService);

        ResponseEntity<Resource> response = ResponseEntity.ok(new ByteArrayResource("test".getBytes()));
        given(evidenceManagementSecureDocStoreService.downloadResource(any(), any()))
                .willReturn(response);
        long size = documentDownloadService
                .getFileSize("19cd94a8-4280-406b-92c7-090b735159ca");
        assertEquals(4L, size);
    }

    @Test
    public void givenFullDocumentUrl_shouldReturnFileSize() {
        ResponseEntity<Resource> response = ResponseEntity.ok(new ByteArrayResource("test".getBytes()));
        given(documentDownloadClientApi.downloadBinary(any(), any(), any(), any(), any()))
                .willReturn(response);
        long size = documentDownloadService
                .getFileSize("http://dm-store:4506/documents/19cd94a8-4280-406b-92c7-090b735159ca");
        assertEquals(4L, size);
    }

    @Test
    public void givenFullDocumentUrl_shouldReturnFileSizeSecure() {
        documentDownloadService = new DocumentDownloadService(documentDownloadClientApi,
                authTokenGenerator, "http://dm-store:4506", true,
                evidenceManagementSecureDocStoreService, idamService);

        ResponseEntity<Resource> response = ResponseEntity.ok(new ByteArrayResource("test".getBytes()));
        given(evidenceManagementSecureDocStoreService.downloadResource(any(), any()))
                .willReturn(response);
        long size = documentDownloadService
                .getFileSize("http://dm-store:4506/documents/19cd94a8-4280-406b-92c7-090b735159ca");
        assertEquals(4L, size);
    }

    @Test
    public void givenUrl_shouldGetDownLoadUrl() {
        ResponseEntity<Resource> response = ResponseEntity.ok(new ByteArrayResource("test".getBytes()));
        given(documentDownloadClientApi.downloadBinary(any(), any(), any(), any(), any()))
                .willReturn(response);
        documentDownloadService
            .getFileSize("19cd94a8-4280-406b-92c7-090b735159ca");

        then(documentDownloadClientApi).should().downloadBinary(
            "oauth2Token", null, "caseworker", "sscs",
            "/documents/19cd94a8-4280-406b-92c7-090b735159ca/binary");
    }

    @Test
    public void givenErrorWhenDownloadingBinaryFile_shouldReturnZeroSizeByDefault() {
        given(documentDownloadClientApi.downloadBinary(any(), any(), any(), any(), any()))
            .willThrow(RuntimeException.class);
        long size = documentDownloadService
            .getFileSize("19cd94a8-4280-406b-92c7-090b735159ca");
        assertEquals(0L, size);
    }

    @Test
    public void givenErrorWhenDownloadingBinaryFile_shouldReturnZeroSizeByDefaultSecure() {
        documentDownloadService = new DocumentDownloadService(documentDownloadClientApi,
                authTokenGenerator, "http://dm-store:4506", true,
                evidenceManagementSecureDocStoreService, idamService);

        given(evidenceManagementSecureDocStoreService.downloadResource(any(), any()))
                .willThrow(RuntimeException.class);
        long size = documentDownloadService
                .getFileSize("19cd94a8-4280-406b-92c7-090b735159ca");
        assertEquals(0L, size);
    }

    @Test
    @Parameters(method = "getDifferentResponseScenarios")
    public void givenResponseIsNull_shouldReturnZeroSizeByDefault(ResponseEntity<Resource> response) {
        //noinspection unchecked
        given(documentDownloadClientApi.downloadBinary(any(), any(), any(), any(), any()))
            .willReturn(response);
        long size = documentDownloadService
            .getFileSize("19cd94a8-4280-406b-92c7-090b735159ca");
        assertEquals(0L, size);
    }

    @Test
    public void canGetUploadedEvidence() {
        Resource expectedResource = mock(Resource.class);

        HttpHeaders headers = new HttpHeaders();
        String filename = "filename";
        headers.add("originalfilename", filename);
        String contentType = "application/pdf";
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        ResponseEntity<Resource> expectedResponse = new ResponseEntity<>(expectedResource, headers, HttpStatus.OK);

        given(documentDownloadClientApi.downloadBinary(any(), any(), any(), any(), any()))
                .willReturn(expectedResponse);

        String urlString = "http://somedomain/documents/someDocId/binary";
        UploadedEvidence downloadFile = documentDownloadService.getUploadedEvidence(urlString);

        assertThat(downloadFile, is(new UploadedEvidence(expectedResource, filename, contentType)));
    }

    @Test
    public void canDownloadFile() {
        Resource expectedResource = mock(Resource.class);

        HttpHeaders headers = new HttpHeaders();
        String filename = "filename";
        headers.add("originalfilename", filename);
        String contentType = "application/pdf";
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        ResponseEntity<Resource> expectedResponse = new ResponseEntity<>(expectedResource, headers, HttpStatus.OK);

        given(documentDownloadClientApi.downloadBinary(any(), any(), any(), any(), any()))
                .willReturn(expectedResponse);

        String urlString = "http://somedomain/documents/someDocId/binary";
        ResponseEntity<Resource> downloadFile = documentDownloadService.downloadFile(urlString);

        assertThat(downloadFile.getStatusCode(), is(HttpStatus.OK));
        assertThat(downloadFile.getBody(), equalTo(expectedResource));
        assertTrue(downloadFile.getHeaders().get(HttpHeaders.CONTENT_TYPE).contains(contentType));
    }

    @Test
    public void canDownloadFileSecure() {
        documentDownloadService = new DocumentDownloadService(documentDownloadClientApi,
                authTokenGenerator, "http://dm-store:4506", true,
                evidenceManagementSecureDocStoreService, idamService);

        Resource expectedResource = mock(Resource.class);

        HttpHeaders headers = new HttpHeaders();
        String filename = "filename";
        headers.add("originalfilename", filename);
        String contentType = "application/pdf";
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        ResponseEntity<Resource> expectedResponse = new ResponseEntity<>(expectedResource, headers, HttpStatus.OK);

        given(evidenceManagementSecureDocStoreService.downloadResource(any(), any()))
                .willReturn(expectedResponse);

        String urlString = "http://somedomain/documents/someDocId/binary";
        ResponseEntity<Resource> downloadFile = documentDownloadService.downloadFile(urlString);

        assertThat(downloadFile.getStatusCode(), is(HttpStatus.OK));
        assertThat(downloadFile.getBody(), equalTo(expectedResource));
        assertTrue(downloadFile.getHeaders().get(HttpHeaders.CONTENT_TYPE).contains(contentType));
    }

    @Test(expected = DocumentNotFoundException.class)
    public void expectedExceptionForNullDownloadFile() {
        given(documentDownloadClientApi.downloadBinary(any(), any(), any(), any(), any()))
                .willReturn(null);

        String urlString = "http://somedomain/documents/someDocId/binary";
        documentDownloadService.downloadFile(urlString);
    }

    @Test(expected = DocumentNotFoundException.class)
    public void expectedExceptionForNullDownloadFileSecure() {
        documentDownloadService = new DocumentDownloadService(documentDownloadClientApi,
            authTokenGenerator, "http://dm-store:4506", true,
            evidenceManagementSecureDocStoreService, idamService);

        given(evidenceManagementSecureDocStoreService.downloadResource(any(), any()))
                .willReturn(null);

        String urlString = "http://somedomain/documents/someDocId/binary";
        documentDownloadService.downloadFile(urlString);
    }


    @Test(expected = DocumentNotFoundException.class)
    public void expectedExceptionForError() {
        given(documentDownloadClientApi.downloadBinary(any(), any(), any(), any(), any()))
                .willThrow();

        String urlString = "http://somedomain/documents/someDocId/binary";
        documentDownloadService.downloadFile(urlString);
    }

    @Test(expected = DocumentNotFoundException.class)
    public void expectedExceptionForErrorSecure() {
        documentDownloadService = new DocumentDownloadService(documentDownloadClientApi,
                authTokenGenerator, "http://dm-store:4506", true,
                evidenceManagementSecureDocStoreService, idamService);

        given(evidenceManagementSecureDocStoreService.downloadResource(any(), any()))
                .willThrow();

        String urlString = "http://somedomain/documents/someDocId/binary";
        documentDownloadService.downloadFile(urlString);
    }

    @Test
    public void givenId_thenReturnUrl() {
        String id = "someDocId";
        assertEquals("/documents/" + id + "/binary", documentDownloadService.getDownloadUrl(id));
    }

    @Test
    public void givenUrl_thenReturnUrl() {
        String id = "someDocId";
        assertEquals("/documents/" + id + "/binary", documentDownloadService.getDownloadUrl(urlString));
    }

    @SuppressWarnings("unused")
    private Object[][] getDifferentResponseScenarios() {
        ResponseEntity<Resource> response = ResponseEntity.notFound().build();
        return new Object[][]{
            new Object[]{response},
            new Object[]{null}
        };
    }
}
