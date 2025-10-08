package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.exception.GetHearingException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
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
        log.info("Sending Get Hearing Request for Hearing ID {}, {}, {}", hearingId, roleAssignmentUrl, dataStoreUrl);
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
        log.info("Sending Create Hearing Request for Case ID {}, {}, {} and request:\n{}",
                roleAssignmentUrl,
                dataStoreUrl,
                hearingPayload.getCaseDetails().getCaseId(),
                hearingPayload);
        return hmcHearingApi.createHearingRequest(
                getIdamTokens().getIdamOauth2Token(),
                getIdamTokens().getServiceAuthorization(),
                hmctsDeploymentId,
                dataStoreUrl,
                roleAssignmentUrl,
                hearingPayload);
    }

    public HmcUpdateResponse sendUpdateHearingRequest(HearingRequestPayload hearingPayload, String hearingId) {
        log.info("Sending Update Hearing Request for Case ID {}, {}, {}, HearingId {} and request:\n{}",
                hearingPayload.getCaseDetails().getCaseId(),
                roleAssignmentUrl,
                dataStoreUrl,
                hearingId,
                hearingPayload);
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
        log.info("Sending Cancel Hearing Request for Hearing ID {}, {}, {} and request:\n{}",
                hearingId,
                roleAssignmentUrl,
                dataStoreUrl,
                hearingPayload);
        return hmcHearingApi.cancelHearingRequest(
                getIdamTokens().getIdamOauth2Token(),
                getIdamTokens().getServiceAuthorization(),
                hmctsDeploymentId,
                dataStoreUrl,
                roleAssignmentUrl,
                hearingId,
                hearingPayload);
    }

    public HearingsGetResponse getHearingsRequest(String caseId, HmcStatus hmcStatus) {
        log.info("Sending Get Hearings Request for Case ID {}, {}, {}", caseId, roleAssignmentUrl, dataStoreUrl);
        return hmcHearingApi.getHearingsRequest(
            getIdamTokens().getIdamOauth2Token(),
            getIdamTokens().getServiceAuthorization(),
            dataStoreUrl,
            roleAssignmentUrl,
            hmctsDeploymentId,
            caseId,
            hmcStatus);
    }


    private IdamTokens getIdamTokens() {
        return idamService.getIdamTokens();
    }
}
