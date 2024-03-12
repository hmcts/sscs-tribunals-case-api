package uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi.SERVICE_AUTHORIZATION;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "documentManagement",
        url = "${document_management.url}"
)
public interface DocumentStoreClient {
    @DeleteMapping(value = "/documents/{documentId}")
    void deleteDocument(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
            @RequestHeader("user-id") String userId,
            @PathVariable("documentId") String documentId);
}
