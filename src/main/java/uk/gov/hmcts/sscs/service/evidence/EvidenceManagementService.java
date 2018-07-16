package uk.gov.hmcts.sscs.service.evidence;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.sscs.exception.UnSupportedDocumentTypeException;

@Service
public class EvidenceManagementService {

    public static final String DUMMY_OAUTH_2_TOKEN = "oauth2Token";

    private final AuthTokenGenerator authTokenGenerator;
    private final DocumentUploadClientApi documentUploadClientApi;

    @Autowired
    public EvidenceManagementService(
        AuthTokenGenerator authTokenGenerator,
        DocumentUploadClientApi documentUploadClientApi
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.documentUploadClientApi = documentUploadClientApi;
    }

    public UploadResponse upload(List<MultipartFile> files) {

        String serviceAuthorization = authTokenGenerator.generate();

        try {
            return documentUploadClientApi.upload(
                    DUMMY_OAUTH_2_TOKEN,
                    serviceAuthorization,
                    files
            );
        } catch (Throwable throwable) {
            if (throwable instanceof HttpClientErrorException) {
                throw new UnSupportedDocumentTypeException(throwable);
            }
            throw throwable;
        }
    }

}
