package uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;

@Slf4j
@Service
public class DocumentStoreService {
    private static final String OAUTH2_TOKEN = "oauth2Token";
    private static final String USER_ID = "sscs";
    private final AuthTokenGenerator authTokenGenerator;
    private final DocumentStoreClient documentStoreClient;
    private final DocumentUploadClientApi documentUploadClientApi;

    public DocumentStoreService(AuthTokenGenerator authTokenGenerator,
                                DocumentStoreClient documentStoreClient,
                                DocumentUploadClientApi documentUploadClientApi) {
        this.authTokenGenerator = authTokenGenerator;
        this.documentStoreClient = documentStoreClient;
        this.documentUploadClientApi = documentUploadClientApi;
    }

    public UploadResponse upload(List<MultipartFile> files) {
        String serviceAuthorization = authTokenGenerator.generate();


        try {
            return documentUploadClientApi.upload(OAUTH2_TOKEN, serviceAuthorization, USER_ID, files);
        } catch (HttpClientErrorException exc) {
            if (exc.getStatusCode().equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
                log.error("Cannot upload evidence [" + files.get(0).getName() + "]", exc);
                throw new IllegalFileTypeException(files.get(0).getName());
            }
            throw exc;
        }
    }

    public void delete(String documentId) {
        documentStoreClient.deleteDocument(OAUTH2_TOKEN, authTokenGenerator.generate(), USER_ID, documentId);
    }


}
