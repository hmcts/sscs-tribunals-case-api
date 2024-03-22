package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Component
@Slf4j
public class AssociatedCaseLinkHelper {

    private final CcdService ccdService;
    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;
    @Value("${feature.add-link-to-other-associated-cases-v2.enabled}")
    private boolean addLinkToOtherAssociatedCasesV2Enabled;

    @Autowired
    public AssociatedCaseLinkHelper(CcdService ccdService, IdamService idamService, UpdateCcdCaseService updateCcdCaseService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.updateCcdCaseService = updateCcdCaseService;
    }

    public SscsCaseData linkCaseByNino(SscsCaseData sscsCaseData, Optional<CaseDetails<SscsCaseData>> previousSscsCaseDataCaseDetails) {
        SscsCaseData updatedSscsCaseData = sscsCaseData;
        String previousNino = null;
        if (previousSscsCaseDataCaseDetails.isPresent()) {
            SscsCaseData sscsCaseDataBefore = previousSscsCaseDataCaseDetails.get().getCaseData();
            Identity identityPrevious = sscsCaseDataBefore.getAppeal().getAppellant().getIdentity();
            previousNino = (identityPrevious != null) ? identityPrevious.getNino() : null;
        }
        Identity identity = sscsCaseData.getAppeal().getAppellant().getIdentity();
        final String nino = (identity != null) ? identity.getNino() : null;
        if (!StringUtils.isEmpty(nino) && StringUtils.isEmpty(previousNino)) {
            List<SscsCaseDetails> matchedByNinoCases = getMatchedCases(nino, idamService.getIdamTokens());
            if (!matchedByNinoCases.isEmpty()) {
                log.info("Found " + matchedByNinoCases.size() + " matching cases for Nino " + nino);
                updatedSscsCaseData = addAssociatedCases(sscsCaseData, matchedByNinoCases);
            }
        }

        return updatedSscsCaseData;
    }

    protected List<SscsCaseDetails> getMatchedCases(String nino, IdamTokens idamTokens) {
        return ccdService.findCaseBy("data.appeal.appellant.identity.nino", nino, idamTokens);
    }

    protected SscsCaseData addAssociatedCases(SscsCaseData caseData, List<SscsCaseDetails> matchedByNinoCases) {
        log.info("Adding " + matchedByNinoCases.size() + " associated cases for case id {}", caseData.getCcdCaseId());

        Set<CaseLink> associatedCases = new HashSet<>();

        for (SscsCaseDetails sscsCaseDetails: matchedByNinoCases) {
            log.info("Linking case " + sscsCaseDetails.getId().toString());
            associatedCases.add(CaseLink.builder().value(
                    CaseLinkDetails.builder().caseReference(sscsCaseDetails.getId().toString()).build()).build());
        }

        if (!matchedByNinoCases.isEmpty()) {
            caseData.setAssociatedCase(new ArrayList<>(associatedCases));
            caseData.setLinkedCasesBoolean("Yes");
            if (addLinkToOtherAssociatedCasesV2Enabled) {
                addLinkToOtherAssociatedCasesV2Enabled(matchedByNinoCases, caseData.getCcdCaseId());
            } else {
                addLinkToOtherAssociatedCases(matchedByNinoCases, caseData.getCcdCaseId());
            }

        } else {
            caseData.setLinkedCasesBoolean("No");
        }
        return caseData;
    }

    private void addLinkToOtherAssociatedCases(List<SscsCaseDetails> matchedByNinoCases, String caseId) {
        log.info("Adding link to other associated cases");
        if (!matchedByNinoCases.isEmpty() && !StringUtils.isEmpty(caseId)) {
            for (SscsCaseDetails sscsCaseDetails: matchedByNinoCases) {
                SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                List<CaseLink> linkList = Optional.ofNullable(sscsCaseData.getAssociatedCase()).orElse(new ArrayList<>());
                linkList.add(CaseLink.builder().value(
                        CaseLinkDetails.builder().caseReference(caseId).build()).build());
                Set<CaseLink> uniqueLinkSet = new HashSet<>(linkList);
                sscsCaseData.setAssociatedCase(new ArrayList<>(uniqueLinkSet));
                sscsCaseData.setLinkedCasesBoolean("Yes");
                ccdService.updateCase(sscsCaseData, Long.valueOf(sscsCaseData.getCcdCaseId()), EventType.UPDATE_CASE_ONLY.getCcdType(), "updated case only", "Auto linked case added", idamService.getIdamTokens());
            }
        }
    }

    private void addLinkToOtherAssociatedCasesV2Enabled(List<SscsCaseDetails> matchedByNinoCases, String caseId) {
        log.info("Adding link to other associated cases V2");
        if (!matchedByNinoCases.isEmpty() && !StringUtils.isEmpty(caseId)) {
            for (SscsCaseDetails sscsCaseDetails: matchedByNinoCases) {
                Long currentCaseId = Long.parseLong(sscsCaseDetails.getData().getCcdCaseId());
                log.info("Linking case {} to case {} using V2", currentCaseId, caseId);
                updateCcdCaseService.updateCaseV2(currentCaseId, EventType.UPDATE_CASE_ONLY.getCcdType(),
                        "updated case only", "Auto linked case added",
                        idamService.getIdamTokens(), caseData -> mutator(caseData, caseId));
            }
        }
    }

    private void mutator(SscsCaseData sscsCaseData, String caseId) {
        List<CaseLink> linkList = Optional.ofNullable(sscsCaseData.getAssociatedCase()).orElse(new ArrayList<>());
        linkList.add(CaseLink.builder().value(
                CaseLinkDetails.builder().caseReference(caseId).build()).build());
        Set<CaseLink> uniqueLinkSet = new HashSet<>(linkList);
        sscsCaseData.setAssociatedCase(new ArrayList<>(uniqueLinkSet));
        sscsCaseData.setLinkedCasesBoolean("Yes");
    }
}
