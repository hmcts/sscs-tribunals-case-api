package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.exception.GetHearingException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;

@Slf4j
@RequiredArgsConstructor
@Service
public class HmcHearingApiService {

    private final HmcHearingApi hmcHearingApi;
    private final IdamService idamService;
    @Value("${hmc.deployment-id}")
    private String hmctsDeploymentId;
    @Value("${role-assignment.api.url:#{null}}")
    private String roleAssignmentUrl;
    @Value("${core_case_data.api.url:#{null}}")
    private String dataStoreUrl;

    public HearingGetResponse getHearingRequest(String hearingId) throws GetHearingException {
        log.debug("Sending Get Hearing Request for Hearing ID {}", hearingId);
        log.info("HMC Headers {}, {}", roleAssignmentUrl, dataStoreUrl);
        HearingGetResponse hearingResponse = hmcHearingApi.getHearingRequest(
                getIdamTokens().getIdamOauth2Token(),
                getIdamTokens().getServiceAuthorization(),
                hmctsDeploymentId,
                dataStoreUrl,
                roleAssignmentUrl,
                hearingId,
                null);
        if (isNull(hearingResponse)) {
            throw new GetHearingException(String.format("Failed to retrieve hearing with Id: %s from HMC", hearingId));
        }
        return hearingResponse;
    }

    public HmcUpdateResponse sendCreateHearingRequest(HearingRequestPayload hearingPayload) {
        log.debug("Sending Create Hearing Request for Case ID {} and request:\n{}",
                hearingPayload.getCaseDetails().getCaseId(),
                hearingPayload);
        log.info("HMC Headers {}, {}", roleAssignmentUrl, dataStoreUrl);
        return hmcHearingApi.createHearingRequest(
                getIdamTokens().getIdamOauth2Token(),
                getIdamTokens().getServiceAuthorization(),
                hmctsDeploymentId,
                dataStoreUrl,
                roleAssignmentUrl,
                hearingPayload);
    }

    public HmcUpdateResponse sendUpdateHearingRequest(HearingRequestPayload hearingPayload, String hearingId) {
        log.debug("Sending Update Hearing Request for Case ID {}, HearingId {} and request:\n{}",
                hearingPayload.getCaseDetails().getCaseId(),
                hearingId,
                hearingPayload);
        log.info("HMC Headers {}, {}", roleAssignmentUrl, dataStoreUrl);
        return hmcHearingApi.updateHearingRequest(
                getIdamTokens().getIdamOauth2Token(),
                getIdamTokens().getServiceAuthorization(),
                hmctsDeploymentId,
                dataStoreUrl,
                roleAssignmentUrl,
                hearingId,
                hearingPayload);
    }

    public HmcUpdateResponse sendCancelHearingRequest(HearingCancelRequestPayload hearingPayload, String hearingId) {
        log.debug("Sending Cancel Hearing Request for Hearing ID {} and request:\n{}",
                hearingId,
                hearingPayload);
        log.info("HMC Headers {}, {}", roleAssignmentUrl, dataStoreUrl);
        return hmcHearingApi.cancelHearingRequest(
                getIdamTokens().getIdamOauth2Token(),
                getIdamTokens().getServiceAuthorization(),
                hmctsDeploymentId,
                dataStoreUrl,
                roleAssignmentUrl,
                hearingId,
                hearingPayload);
    }

    private IdamTokens getIdamTokens() {
        return idamService.getIdamTokens();
    }
}
