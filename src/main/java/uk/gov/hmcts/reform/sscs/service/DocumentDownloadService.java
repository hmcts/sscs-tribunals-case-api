package uk.gov.hmcts.reform.sscs.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.document.DocumentDownloadClientApi;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class DocumentDownloadService {
    private final DocumentDownloadClientApi documentDownloadClientApi;
    private final IdamService idamService;
    private final String documentManagementUrl;

    DocumentDownloadService(DocumentDownloadClientApi documentDownloadClientApi,
                            IdamService idamService,
                            @Value("${document_management.url}") String documentManagementUrl) {
        this.documentDownloadClientApi = documentDownloadClientApi;
        this.idamService = idamService;
        this.documentManagementUrl = documentManagementUrl;
    }

    public Long getFileSize(String urlString) {
        ResponseEntity<Resource> response;
        try {
            IdamTokens idamTokens = idamService.getIdamTokens();
            response = documentDownloadClientApi.downloadBinary(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                "",
                idamTokens.getUserId(),
                getDownloadUrl(urlString)
            );
            if (response != null && response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().contentLength();
            }
        } catch (Exception e) {
            log.info("Error when downloading the following Binary file from the Document Management: {} ", urlString, e);
        }
        return 0L;
    }

    private String getDownloadUrl(String urlString) throws UnsupportedEncodingException {
        String path = urlString.replace(documentManagementUrl, "");
        if (path.startsWith("/")) {
            return path;
        }
        return "/" + URLDecoder.decode(path, "UTF-8");
    }
}