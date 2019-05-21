package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentDownloadClientApi;

@RunWith(MockitoJUnitRunner.class)
public class DocumentDownloadServiceTest {

    @Mock
    private DocumentDownloadClientApi documentDownloadClientApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    private DocumentDownloadService documentDownloadService;


    @Before
    public void setUp() {
        documentDownloadService = new DocumentDownloadService(documentDownloadClientApi,
            authTokenGenerator, "http://dm-store:4506");

        //noinspection unchecked
        ResponseEntity<Resource> response = ResponseEntity.ok(new ByteArrayResource("test".getBytes()));
        given(documentDownloadClientApi.downloadBinary(any(), any(), any(), any(), any()))
            .willReturn(response);
    }

    @Test
    public void givenDocumentUrl_shouldReturnFileSize() {
        long size = documentDownloadService
            .getFileSize("http://dm-store:4506/documents/19cd94a8-4280-406b-92c7-090b735159ca");
        assertEquals(4L, size);
    }

    @Test
    public void givenUrl_shouldGetDownLoadUrl() {
        documentDownloadService
            .getFileSize("http://dm-store:4506/documents/19cd94a8-4280-406b-92c7-090b735159ca");

        then(documentDownloadClientApi).should().downloadBinary(
            "oauth2Token", null, "", "sscs",
            "/documents/19cd94a8-4280-406b-92c7-090b735159ca");
    }
}