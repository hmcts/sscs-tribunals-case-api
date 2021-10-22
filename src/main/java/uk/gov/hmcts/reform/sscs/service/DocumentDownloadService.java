package uk.gov.hmcts.reform.sscs.service;

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
import uk.gov.hmcts.reform.sscs.exception.DocumentNotFoundException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.pdf.data.UploadedEvidence;

@Service
@Slf4j
public class DocumentDownloadService {
    private final DocumentDownloadClientApi documentDownloadClientApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService;
    private final IdamService idamService;

    private static final String OAUTH2_TOKEN = "oauth2Token";
    private static final String USER_ID = "sscs";
    private final String documentManagementUrl;
    private final boolean secureDocStoreEnabled;

    DocumentDownloadService(DocumentDownloadClientApi documentDownloadClientApi,
                            AuthTokenGenerator authTokenGenerator,
                            @Value("${document_management.url}") String documentManagementUrl,
                            @Value("${feature.secure-doc-store.enabled:false}") boolean secureDocStoreEnabled,
                            EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService,
                            IdamService idamService) {
        this.documentDownloadClientApi = documentDownloadClientApi;
        this.authTokenGenerator = authTokenGenerator;
        this.documentManagementUrl = documentManagementUrl;
        this.secureDocStoreEnabled = secureDocStoreEnabled;
        this.evidenceManagementSecureDocStoreService = evidenceManagementSecureDocStoreService;
        this.idamService = idamService;
    }

    public Long getFileSize(String urlString) {
        ResponseEntity<Resource> response;
        try {
            if (secureDocStoreEnabled) {
                IdamTokens idamTokens = idamService.getIdamTokens();
                response = evidenceManagementSecureDocStoreService.downloadResource(urlString, idamTokens);
            } else {
                response = documentDownloadClientApi.downloadBinary(
                        OAUTH2_TOKEN,
                        authTokenGenerator.generate(),
                        "caseworker",
                        USER_ID,
                        getDownloadUrl(urlString)
                );
            }
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

    public ResponseEntity<Resource> downloadFile(String urlString) {
        ResponseEntity<Resource> response = null;
        try {
            if (secureDocStoreEnabled) {
                IdamTokens idamTokens = idamService.getIdamTokens();
                response = evidenceManagementSecureDocStoreService.downloadResource(urlString, idamTokens);
            } else {
                response = documentDownloadClientApi.downloadBinary(
                        OAUTH2_TOKEN,
                        authTokenGenerator.generate(),
                        "caseworker,citizen",
                        USER_ID,
                        getDownloadUrl(urlString)
                );
            }
        } catch (Exception e) {
            log.error("Error when downloading the following Binary file from the Document Management: {} ", urlString, e);
        }
        if (response != null && HttpStatus.OK.equals(response.getStatusCode())) {
            return response;
        }
        throw new DocumentNotFoundException();
    }

    public UploadedEvidence getUploadedEvidence(String urlString) {
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

    protected String getDownloadUrl(String urlString) {
        String path = urlString.replace(documentManagementUrl, "");
        if (path.toLowerCase().contains("documents")) {
            return path;
        }
        return "/documents/" + path + "/binary";
    }
}
