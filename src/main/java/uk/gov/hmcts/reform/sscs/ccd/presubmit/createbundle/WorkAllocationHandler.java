package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRole;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class WorkAllocationHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean workAllocationFeature;

    private IdamService idamService;

    private CaseAssignmentApi caseAssignmentApi;

    public WorkAllocationHandler(CaseAssignmentApi caseAssignmentApi, IdamService idamService,
                                 @Value("${feature.work-allocation.enabled}") boolean workAllocationFeature) {
        this.caseAssignmentApi = caseAssignmentApi;
        this.idamService = idamService;
        this.workAllocationFeature = workAllocationFeature;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && (callback.getEvent() == EventType.CREATE_BUNDLE
                ||  callback.getEvent() == EventType.NEW_CASE_ROLES_ASSIGNED)
                && workAllocationFeature;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        List<String> previouslyAssignedCaseRoles = mergeRoleList(
                sscsCaseData.getWorkAllocationFields().getPreviouslyAssignedCaseRoles(),
                sscsCaseData.getWorkAllocationFields().getAssignedCaseRoles());

        sscsCaseData.getWorkAllocationFields().setAssignedCaseRoles(newlyAssignedCaseRoles(caseDetails.getId(), previouslyAssignedCaseRoles));
        sscsCaseData.getWorkAllocationFields().setPreviouslyAssignedCaseRoles(previouslyAssignedCaseRoles);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private List<String> newlyAssignedCaseRoles(long caseId, List<String> excludeRoles) {
        IdamTokens tokens = idamService.getIdamTokens();

        CaseAssignmentUserRolesResource response = caseAssignmentApi.getUserRoles(
                tokens.getIdamOauth2Token(),
                tokens.getServiceAuthorization(),
                List.of(Long.toString(caseId)));

        if (response != null && response.getCaseAssignmentUserRoles() != null) {
            log.info("WA newly assigned roles {}", response.getCaseAssignmentUserRoles());
            return response.getCaseAssignmentUserRoles().stream()
                    .map(CaseAssignmentUserRole::getCaseRole)
                    .distinct()
                    .filter(r -> !excludeRoles.contains(r))
                    .collect(Collectors.toList());
        }

        return null;
    }

    private List<String> mergeRoleList(List<String> previousRoles, List<String> currentRoles) {

        log.info("WA previous roles: {}", previousRoles);
        log.info("WA current roles {}", currentRoles);

        List<String> mergedRoles = new ArrayList<>();

        if (previousRoles != null) {
            mergedRoles.addAll(previousRoles);
        }

        if (currentRoles != null) {
            for (String role : currentRoles) {
                if (!mergedRoles.contains(role)) {
                    mergedRoles.add(role);
                }
            }
        }
        return mergedRoles;
    }
}

