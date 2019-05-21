package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentDownloadClientApi;

@Service
public class DocumentDownloadService {
    private final DocumentDownloadClientApi documentDownloadClientApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final String documentManagementUrl;

    private static final String OAUTH2_TOKEN = "oauth2Token";
    private static final String USER_ID = "sscs";

    public DocumentDownloadService(DocumentDownloadClientApi documentDownloadClientApi,
                                   AuthTokenGenerator authTokenGenerator,
                                   @Value("${document_management.url}") String documentManagementUrl) {
        this.documentDownloadClientApi = documentDownloadClientApi;
        this.authTokenGenerator = authTokenGenerator;
        this.documentManagementUrl = documentManagementUrl;
    }

    public Long getFileSize(String urlString) {
        ResponseEntity<Resource> response = documentDownloadClientApi.downloadBinary(
            OAUTH2_TOKEN,
            authTokenGenerator.generate(),
            "",
            USER_ID,
            getDownloadUrl(urlString)
        );
        if (HttpStatus.OK.equals(response.getStatusCode())) {
            try {
                return response.getBody().contentLength();
            } catch (IOException ioe) {
                throw new IllegalStateException("Cannot download document to get size that is stored in CCD got "
                    + "[" + response.getStatusCode() + "] " + response.getBody());
            }
        } else {
            throw new IllegalStateException("Cannot download document that is stored in CCD got "
                + "[" + response.getStatusCode() + "] " + response.getBody());
        }
    }

    private String getDownloadUrl(String urlString) {
        String path = urlString.replace(documentManagementUrl, "");
        if (path.startsWith("/")) {
            return path;
        }

        try {
            return "/" + URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException("Cannot download document that is stored in CCD got UnsupportedEncodingException");
        }
    }
}