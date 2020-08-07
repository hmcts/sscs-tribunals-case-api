package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Component
@Slf4j
public class AssociatedCaseLinkHelper {

    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    public AssociatedCaseLinkHelper(CcdService ccdService, IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    public SscsCaseData linkCaseByNino(SscsCaseData sscsCaseData) {
        SscsCaseData updatedSscsCaseData = sscsCaseData;
        Identity identity = sscsCaseData.getAppeal().getAppellant().getIdentity();
        final String nino = (identity != null) ? identity.getNino() : null;
        if (!StringUtils.isEmpty(nino)) {
            List<SscsCaseDetails> matchedByNinoCases = getMatchedCases(nino, idamService.getIdamTokens());
            if (!matchedByNinoCases.isEmpty()) {
                log.info("Found " + matchedByNinoCases.size() + " matching cases for Nino " + nino);
                updatedSscsCaseData = addAssociatedCases(sscsCaseData, matchedByNinoCases);
            }
        }
        return updatedSscsCaseData;
    }

    protected List<SscsCaseDetails> getMatchedCases(String nino, IdamTokens idamTokens) {
        HashMap<String, String> map = new HashMap<String, String>();

        map.put("case.appeal.appellant.identity.nino", nino);

        return ccdService.findCaseBy(map, idamTokens);
    }

    protected SscsCaseData addAssociatedCases(SscsCaseData caseData, List<SscsCaseDetails> matchedByNinoCases) {
        log.info("Adding " + matchedByNinoCases.size() + " associated cases for case id {}", caseData.getCcdCaseId());

        List<CaseLink> associatedCases = new ArrayList<>();

        for (SscsCaseDetails sscsCaseDetails: matchedByNinoCases) {
            log.info("Linking case " + sscsCaseDetails.getId().toString());
            associatedCases.add(CaseLink.builder().value(
                    CaseLinkDetails.builder().caseReference(sscsCaseDetails.getId().toString()).build()).build());
        }

        if (!matchedByNinoCases.isEmpty()) {
            caseData.setAssociatedCase(associatedCases);
            caseData.setLinkedCasesBoolean("Yes");
            addLinkToOtherAssociatedCases(matchedByNinoCases, caseData.getCcdCaseId());
        } else {
            caseData.setLinkedCasesBoolean("No");
        }
        return caseData;
    }

    private void addLinkToOtherAssociatedCases(List<SscsCaseDetails> matchedByNinoCases, String caseId) {
        if (!matchedByNinoCases.isEmpty() && !StringUtils.isEmpty(caseId)) {
            for (SscsCaseDetails sscsCaseDetails: matchedByNinoCases) {
                SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                List<CaseLink> linkList = Optional.ofNullable(sscsCaseData.getAssociatedCase()).orElse(Lists.newArrayList());
                linkList.add(CaseLink.builder().value(
                        CaseLinkDetails.builder().caseReference(caseId).build()).build());
                sscsCaseData.setAssociatedCase(linkList);
                sscsCaseData.setLinkedCasesBoolean("Yes");
                ccdService.updateCase(sscsCaseData, Long.valueOf(sscsCaseData.getCcdCaseId()), EventType.UPDATE_CASE_ONLY.getCcdType(), "updated case only", "Auto linked case added", idamService.getIdamTokens());
            }
        }
    }
}