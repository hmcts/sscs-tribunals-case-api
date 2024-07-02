package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRole;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkAllocationFields;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkAllocationService {

    @Autowired
    private final CaseAssignmentApi caseAssignmentApi;

    @Autowired
    private final IdamService idamService;

    public WorkAllocationFields updateAssignedCaseRoles(CaseDetails<SscsCaseData> caseDetails) {
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        WorkAllocationFields workAllocationFields = sscsCaseData.getWorkAllocationFields();

        List<String> previouslyAssignedCaseRoles = mergeRoleList(
                sscsCaseData.getWorkAllocationFields().getPreviouslyAssignedCaseRoles(),
                sscsCaseData.getWorkAllocationFields().getAssignedCaseRoles());

        workAllocationFields.setAssignedCaseRoles(newlyAssignedCaseRoles(caseDetails.getId(), previouslyAssignedCaseRoles));
        workAllocationFields.setPreviouslyAssignedCaseRoles(previouslyAssignedCaseRoles);

        return workAllocationFields;
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

