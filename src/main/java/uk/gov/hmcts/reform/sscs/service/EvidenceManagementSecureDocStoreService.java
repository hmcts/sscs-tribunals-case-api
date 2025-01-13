package uk.gov.hmcts.reform.sscs.service;

import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClient;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.UnsupportedDocumentTypeException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class EvidenceManagementSecureDocStoreService {

    private final CaseDocumentClient caseDocumentClient;

    @Autowired
    public EvidenceManagementSecureDocStoreService(CaseDocumentClient caseDocumentClient) {
        this.caseDocumentClient = caseDocumentClient;
    }

    public UploadResponse upload(List<MultipartFile> files, IdamTokens idamTokens) {

        try {
            return caseDocumentClient.uploadDocuments(idamTokens.getIdamOauth2Token(), idamTokens.getServiceAuthorization(), "Benefit", "SSCS", files);
        } catch (HttpClientErrorException httpClientErrorException) {
            log.error("Secure Doc Store service failed to upload documents...", httpClientErrorException);
            if (null != files) {
                logFiles(files);
            }
            throw new UnsupportedDocumentTypeException(httpClientErrorException);
        }
    }

    public byte[] download(String selfHref, IdamTokens idamTokens) {
        try {
            ResponseEntity<Resource> responseEntity = downloadResource(selfHref, idamTokens);

            ByteArrayResource resource = (ByteArrayResource) responseEntity.getBody();
            return (resource != null) ? resource.getByteArray() : new byte[0];
        } catch (HttpClientErrorException httpClientErrorException) {
            log.info("Secure Doc Store service failed to download document message {}", httpClientErrorException.getMessage());
            throw new UnsupportedDocumentTypeException(httpClientErrorException);
        }
    }

    public ResponseEntity<Resource> downloadResource(String selfHref, IdamTokens idamTokens) {
        String documentHref = URI.create(selfHref).getPath().replaceFirst("/", "");
        return caseDocumentClient.getDocumentBinary(idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(), documentHref);
    }

    private void logFiles(List<MultipartFile> files) {
        files.forEach(file -> {
            log.info("Name: {}", file.getName());
            log.info("OriginalName {}", file.getOriginalFilename());
        });
    }

}
