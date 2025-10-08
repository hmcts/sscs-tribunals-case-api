package uk.gov.hmcts.reform.sscs.service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.sscs.config.FeignClientConfig;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;

@SuppressWarnings({"PMD.UseObjectForClearerAPI"})
@FeignClient(name = "hmc-hearing", url = "${hmc.url}", configuration = FeignClientConfig.class)
public interface HmcHearingApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String ROLE_ASSIGNMENT_URL = "Role-Assignment-Url";
    String DATA_STORE_URL = "Data-Store-Url";
    String HEARING_ENDPOINT = "/hearing";
    String HEARINGS_ENDPOINT = "/hearings";
    String ID = "id";
    String HMCTS_DEPLOYMENT_ID = "hmctsDeploymentId";

    @PostMapping(value = HEARING_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE)
    HmcUpdateResponse createHearingRequest(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
            @RequestHeader(value = DATA_STORE_URL, required = false) String dataStoreUrl,
            @RequestHeader(value = ROLE_ASSIGNMENT_URL, required = false) String roleAssignmentUrl,
            @RequestBody HearingRequestPayload hearingPayload
    );

    @PutMapping(value = HEARING_ENDPOINT + "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    HmcUpdateResponse updateHearingRequest(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
            @RequestHeader(value = DATA_STORE_URL, required = false) String dataStoreUrl,
            @RequestHeader(value = ROLE_ASSIGNMENT_URL, required = false) String roleAssignmentUrl,
            @PathVariable String id,
            @RequestBody HearingRequestPayload hearingPayload
    );

    @DeleteMapping(value = HEARING_ENDPOINT + "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    HmcUpdateResponse cancelHearingRequest(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
            @RequestHeader(value = DATA_STORE_URL, required = false) String dataStoreUrl,
            @RequestHeader(value = ROLE_ASSIGNMENT_URL, required = false) String roleAssignmentUrl,
            @PathVariable String id,
            @RequestBody HearingCancelRequestPayload hearingDeletePayload
    );

    @GetMapping(HEARING_ENDPOINT + "/{id}")
    HearingGetResponse getHearingRequest(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
            @RequestHeader(value = DATA_STORE_URL, required = false) String dataStoreUrl,
            @RequestHeader(value = ROLE_ASSIGNMENT_URL, required = false) String roleAssignmentUrl,
            @PathVariable String id,
            @RequestParam(name = "isValid", required = false) Boolean isValid
    );

    @GetMapping(HEARINGS_ENDPOINT + "/{caseId}")
    HearingsGetResponse getHearingsRequest(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestHeader(value = DATA_STORE_URL, required = false) String dataStoreUrl,
        @RequestHeader(value = ROLE_ASSIGNMENT_URL, required = false) String roleAssignmentUrl,
        @RequestHeader(value = "hmctsDeploymentId", required = false) String hmctsDeploymentId,
        @PathVariable String caseId,
        @RequestParam(name = "status", required = false) HmcStatus hmcStatus
    );

}
