package uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;

public class DocumentManagementServiceTest {

    private AuthTokenGenerator authTokenGenerator;
    private DocumentUploadClientApi documentUploadClientApi;
    private String authToken;
    private List<MultipartFile> files;
    private DocumentStoreService documentStoreService;
    private DocumentStoreClient documentStoreClient;

    @Before
    public void setUp() {
        authToken = "authToken";
        files = singletonList(mock(MultipartFile.class));
        authTokenGenerator = mock(AuthTokenGenerator.class);
        documentStoreClient = mock(DocumentStoreClient.class);
        documentUploadClientApi = mock(DocumentUploadClientApi.class);
        documentStoreService = new DocumentStoreService(authTokenGenerator, documentStoreClient, documentUploadClientApi);
    }

    @Test
    public void passesUploadToDocumentUploadClientApi() {
        when(authTokenGenerator.generate()).thenReturn(authToken);

        documentStoreService.upload(files);

        verify(documentUploadClientApi).upload("oauth2Token", authToken, "sscs", files);
    }

    @Test(expected = IllegalFileTypeException.class)
    public void throwsIllegalFileTypeExceptionIfDocumentStoreCannotStoreFile() {
        when(documentUploadClientApi.upload(any(), any(), any(), any())).thenThrow(new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY));

        documentStoreService.upload(files);
    }

    @Test
    public void delete() {
        String someDocumentId = "someDocumentId";
        String someToken = "someToken";
        when(authTokenGenerator.generate()).thenReturn(someToken);

        documentStoreService.delete(someDocumentId);

        verify(documentStoreClient).deleteDocument("oauth2Token", someToken, "sscs", someDocumentId);
    }
}
