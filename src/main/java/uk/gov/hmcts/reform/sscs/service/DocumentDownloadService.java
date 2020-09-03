package uk.gov.hmcts.reform.sscs.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentDownloadClientApi;
import uk.gov.hmcts.reform.sscs.service.pdf.data.UploadedEvidence;

@Service
@Slf4j
public class DocumentDownloadService {
    private final DocumentDownloadClientApi documentDownloadClientApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final String documentManagementUrl;

    private static final String OAUTH2_TOKEN = "oauth2Token";
    private static final String USER_ID = "sscs";

    DocumentDownloadService(DocumentDownloadClientApi documentDownloadClientApi,
                            AuthTokenGenerator authTokenGenerator,
                            @Value("${document_management.url}") String documentManagementUrl) {
        this.documentDownloadClientApi = documentDownloadClientApi;
        this.authTokenGenerator = authTokenGenerator;
        this.documentManagementUrl = documentManagementUrl;
    }

    public Long getFileSize(String urlString) {
        ResponseEntity<Resource> response;
        try {
            response = documentDownloadClientApi.downloadBinary(
                OAUTH2_TOKEN,
                authTokenGenerator.generate(),
                "caseworker",
                USER_ID,
                getDownloadUrl(urlString)
            );
            if (response != null && response.getStatusCode() == HttpStatus.OK) {
                Resource responseBody = response.getBody();
                if (responseBody != null) {
                    return responseBody.contentLength();
                }
            }
        } catch (Exception e) {
            log.info("Error when downloading the following Binary file from the Document Management: {} ", urlString, e);
        }
        return 0L;
    }

    public UploadedEvidence downloadFile(String urlString) {
        ResponseEntity<Resource> response;
        try {
            response = documentDownloadClientApi.downloadBinary(
                    OAUTH2_TOKEN,
                    authTokenGenerator.generate(),
                    "",
                    USER_ID,
                    getDownloadUrl(urlString)
            );
            if (HttpStatus.OK.equals(response.getStatusCode())) {
                return new UploadedEvidence(response.getBody(), Objects.requireNonNull(response.getHeaders().get("originalfilename")).get(0), Objects
                    .requireNonNull(response.getHeaders().get(HttpHeaders.CONTENT_TYPE)).get(0));
            } else {
                throw new IllegalStateException("Cannot download document that is stored in CCD got "
                        + "[" + response.getStatusCode() + "] " + response.getBody());
            }
        } catch (Exception e) {
            log.info("Error when downloading the following Binary file from the Document Management: {} ", urlString, e);
        }
        return new UploadedEvidence(null, "", HttpHeaders.CONTENT_TYPE);
    }

    private String getDownloadUrl(String urlString) throws UnsupportedEncodingException {
        String path = urlString.replace(documentManagementUrl, "");
        if (path.startsWith("/")) {
            return path;
        }
        return "/" + URLDecoder.decode(path, "UTF-8");
    }
}
