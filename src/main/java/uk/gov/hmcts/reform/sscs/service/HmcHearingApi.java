package uk.gov.hmcts.reform.sscs.service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import feign.Headers;
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
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;

@SuppressWarnings({"PMD.UseObjectForClearerAPI"})
@FeignClient(name = "hmc-hearing", url = "${hmc.url}", configuration = FeignClientConfig.class)
public interface HmcHearingApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String ROLE_ASSIGNMENT_HEADER = "Role-Assignment-Url: ${role-assignment.api.url}";
    String DATA_STORE_HEADER = "Data-Store-Url: ${core_case_data.api.url}";
    String HEARING_ENDPOINT = "/hearing";
    String ID = "id";
    String HMCTS_DEPLOYMENT_ID = "hmctsDeploymentId";

    @PostMapping(value = HEARING_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Headers({
        ROLE_ASSIGNMENT_HEADER,
        DATA_STORE_HEADER
    })
    HmcUpdateResponse createHearingRequest(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
            @RequestBody HearingRequestPayload hearingPayload
    );

    @PutMapping(value = HEARING_ENDPOINT + "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Headers({
        ROLE_ASSIGNMENT_HEADER,
        DATA_STORE_HEADER
    })
    HmcUpdateResponse updateHearingRequest(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
            @PathVariable String id,
            @RequestBody HearingRequestPayload hearingPayload
    );

    @DeleteMapping(value = HEARING_ENDPOINT + "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Headers({
        ROLE_ASSIGNMENT_HEADER,
        DATA_STORE_HEADER
    })
    HmcUpdateResponse cancelHearingRequest(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
            @PathVariable String id,
            @RequestBody HearingCancelRequestPayload hearingDeletePayload
    );

    @GetMapping(HEARING_ENDPOINT + "/{id}")
    @Headers({
        ROLE_ASSIGNMENT_HEADER,
        DATA_STORE_HEADER
    })
    HearingGetResponse getHearingRequest(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestHeader(value = HMCTS_DEPLOYMENT_ID, required = false) String hmctsDeploymentId,
            @PathVariable String id,
            @RequestParam(name = "isValid", required = false) Boolean isValid
    );
}
