package uk.gov.hmcts.reform.sscs.document;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.healthcheck.InternalHealth;

@FeignClient(name = "document-management-metadata-download-api", url = "${document_management.url}",
    configuration = EvidenceMetadataDownloadClientApi.DownloadConfiguration.class)
public interface EvidenceMetadataDownloadClientApi {
    static final String USER_ID = "user-id";

    @GetMapping(value = "{document_metadata_uri}")
    Document getDocumentMetadata(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuth,
        @RequestHeader(USER_ID) String userId,
        @RequestHeader("user-roles") String userRoles,
        @PathVariable("document_metadata_uri") String documentMetadataUri
    );


    @GetMapping(
        value = "/health",
        headers = CONTENT_TYPE + "=" + APPLICATION_JSON_VALUE
    )
    InternalHealth health();

    class DownloadConfiguration {
        @Bean
        @Primary
        Decoder feignDecoder(ObjectMapper objectMapper) {
            return new JacksonDecoder(objectMapper);
        }
    }
}

