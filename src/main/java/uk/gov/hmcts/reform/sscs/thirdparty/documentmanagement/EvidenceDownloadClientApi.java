package uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "document-management-download-api", url = "${document_management.url}")
public interface EvidenceDownloadClientApi {
    static final String USER_ID = "user-id";

    @GetMapping(value = "{document_download_uri}")
    ResponseEntity<Resource> downloadBinary(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuth,
        @RequestHeader(USER_ID) String userId,
        @RequestHeader("user-roles") String userRoles,
        @PathVariable("document_download_uri") String documentDownloadUri
    );
}
