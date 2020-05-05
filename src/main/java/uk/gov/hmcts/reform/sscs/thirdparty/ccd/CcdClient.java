package uk.gov.hmcts.reform.sscs.thirdparty.ccd;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi.SERVICE_AUTHORIZATION;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.sscs.thirdparty.ccd.api.CcdAddUser;
import uk.gov.hmcts.reform.sscs.thirdparty.ccd.api.CcdHistoryEvent;

@FeignClient(
        name = "Ccd",
        url = "${core_case_data.api.url}"
)
public interface CcdClient {
    @PostMapping(value = "/caseworkers/{userId}/jurisdictions/{jurisdictionId}/case-types/{caseType}/cases/{caseId}/users")
    void addUserToCase(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
            @PathVariable("userId") String userId,
            @PathVariable("jurisdictionId") String jurisdictionId,
            @PathVariable("caseType") String caseType,
            @PathVariable("caseId") long caseId,
            @RequestBody CcdAddUser caseDataContent
    );

    @DeleteMapping(value = "/caseworkers/{userId}/jurisdictions/{jurisdictionId}/case-types/{caseType}/cases/{caseId}/users/{idToDelete}")
    void removeUserFromCase(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
            @PathVariable("userId") String userId,
            @PathVariable("jurisdictionId") String jurisdictionId,
            @PathVariable("caseType") String caseType,
            @PathVariable("caseId") long caseId,
            @PathVariable("idToDelete") String idToDelete
    );

    @GetMapping(
            value = "/caseworkers/{userId}/jurisdictions/{jurisdictionId}/case-types/{caseType}/cases/{caseId}/events",
            headers = CONTENT_TYPE + "=" + APPLICATION_JSON_VALUE
    )
    List<CcdHistoryEvent> getHistoryEvents(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
            @PathVariable("userId") String userId,
            @PathVariable("jurisdictionId") String jurisdictionId,
            @PathVariable("caseType") String caseType,
            @PathVariable("caseId") long caseId
    );
}
