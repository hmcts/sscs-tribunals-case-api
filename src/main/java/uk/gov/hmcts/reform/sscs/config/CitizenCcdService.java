package uk.gov.hmcts.reform.sscs.config;

import static java.util.stream.Stream.of;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_ARCHIVED;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;

@Service
@Slf4j
public class CitizenCcdService {

    private final CitizenCcdClient citizenCcdClient;
    private final SscsCcdConvertService sscsCcdConvertService;
    private final CcdService ccdService;

    @Autowired
    CitizenCcdService(CitizenCcdClient citizenCcdClient,
                      SscsCcdConvertService sscsCcdConvertService,
                      CcdService ccdService) {
        this.citizenCcdClient = citizenCcdClient;
        this.sscsCcdConvertService = sscsCcdConvertService;
        this.ccdService = ccdService;
    }


    public List<SscsCaseData> findCase(IdamTokens idamTokens) {
        return searchForCitizen(idamTokens)
            .stream()
            .map(f -> {
                SscsCaseData caseData = sscsCcdConvertService.getCaseData(f.getData());
                caseData.setCcdCaseId(f.getId().toString());
                return caseData;
            })
            .collect(Collectors.toList());
    }

    public List<CaseDetails> searchForCitizen(IdamTokens idamTokens) {
        return citizenCcdClient.searchForCitizen(idamTokens);
    }

    public List<CaseDetails> searchForCitizenAllCases(IdamTokens idamTokens) {
        return citizenCcdClient.searchForCitizenAllCases(idamTokens);
    }

    public SaveCaseResult saveCase(SscsCaseData caseData, IdamTokens idamTokens) {

        List<CaseDetails> caseDetailsList = citizenCcdClient.searchForCitizen(idamTokens);

        CaseDetails caseDetails;

        if (CollectionUtils.isNotEmpty(caseDetailsList)) {

            String caseId = caseDetailsList.get(0).getId().toString();
            caseDetails = updateCase(caseData, EventType.UPDATE_DRAFT.getCcdType(), "Update draft",
                "Update draft in CCD", idamTokens, caseId);

            return SaveCaseResult.builder()
                .caseDetailsId(caseDetails.getId())
                .saveCaseOperation(SaveCaseOperation.UPDATE)
                .build();
        } else {
            return createDraft(caseData, idamTokens);
        }
    }

    @Retryable
    public SaveCaseResult createDraft(SscsCaseData caseData, IdamTokens idamTokens) {

        CaseDetails caseDetails;
        caseDetails = newCase(caseData, EventType.CREATE_DRAFT.getCcdType(), "Create draft",
                "Create draft in CCD", idamTokens);

        return SaveCaseResult.builder()
                .caseDetailsId(caseDetails.getId())
                .saveCaseOperation(SaveCaseOperation.CREATE)
                .build();
    }

    public CaseDetails archiveDraft(SscsCaseData caseData, IdamTokens userIdamTokens, Long caseId) {
        log.info("Archiving Draft for caseId {} with user roles {}", caseId, userIdamTokens.getRoles().toString());
        return updateCase(caseData, DRAFT_ARCHIVED.getCcdType(), "SSCS Archive Draft", "SSCS Archive Draft", userIdamTokens, caseId.toString());
    }

    private CaseDetails newCase(SscsCaseData caseData, String eventType, String summary, String description, IdamTokens idamTokens) {
        log.info("Creating a draft for a user.");
        CaseDetails caseDetails;
        StartEventResponse startEventResponse = citizenCcdClient.startCaseForCitizen(idamTokens, eventType);
        CaseDataContent caseDataContent = sscsCcdConvertService.getCaseDataContent(caseData, startEventResponse, summary, description);
        caseDetails = citizenCcdClient.submitForCitizen(idamTokens, caseDataContent);
        return caseDetails;
    }

    @Retryable
    public CaseDetails updateCase(SscsCaseData caseData, String eventType, String summary, String description, IdamTokens idamTokens, String caseId) {
        log.info("Updating a draft with caseId {}.", caseId);
        CaseDetails caseDetails;
        StartEventResponse startEventResponse = citizenCcdClient.startEventForCitizen(idamTokens, caseId, eventType);
        CaseDataContent caseDataContent = sscsCcdConvertService.getCaseDataContent(caseData, startEventResponse, summary, description);
        caseDetails = citizenCcdClient.submitEventForCitizen(idamTokens, caseId, caseDataContent);
        return caseDetails;
    }

    @Retryable
    public void associateCaseToCitizen(IdamTokens citizenIdamTokens, Long caseId, IdamTokens idamTokens) {
        SscsCaseDetails sscsCaseDetails = ccdService.getByCaseId(caseId, idamTokens);
        if (sscsCaseDetails != null && sscsCaseDetails.getData() != null) {
            SscsCaseData caseData = sscsCaseDetails.getData();
            if (hasAppellantPostcode(caseData)) {
                if (caseHasSubscriptionWithEmail(citizenIdamTokens, caseData)) {
                    log.info("Associate case: Found matching subscription email for case id {}", caseId);
                    citizenCcdClient.addUserToCase(idamTokens, citizenIdamTokens.getUserId(), caseId);
                } else {
                    log.info("Associate case: Subscription email does not match id {}", caseId);
                }
            } else {
                log.info("Associate case: Postcode does not exists for case id {}", caseId);
            }
        }
    }

    public void addUserToCase(IdamTokens idamTokens, String userIdToAdd, Long caseId) {
        citizenCcdClient.addUserToCase(idamTokens, userIdToAdd, caseId);
    }

    private boolean hasAppellantPostcode(SscsCaseData caseData) {
        return caseData.getAppeal() != null && caseData.getAppeal().getAppellant() != null
                && caseData.getAppeal().getAppellant().getAddress() != null
                && caseData.getAppeal().getAppellant().getAddress().getPostcode() != null;
    }

    private boolean caseHasSubscriptionWithEmail(IdamTokens citizenIdamTokens, SscsCaseData caseData) {
        Subscriptions subscriptions = caseData.getSubscriptions();
        return of(subscriptions.getAppellantSubscription(), subscriptions.getAppointeeSubscription(), subscriptions.getRepresentativeSubscription())
                .anyMatch(subscription -> subscription != null && citizenIdamTokens.getEmail().equalsIgnoreCase(subscription.getEmail()));
    }

}
