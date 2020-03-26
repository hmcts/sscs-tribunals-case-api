package uk.gov.hmcts.reform.sscs.ccd.presubmit.linkcase;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Lists;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
public class LinkCaseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    public LinkCaseAboutToSubmitHandler(CcdService ccdService,
                                        IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.LINK_A_CASE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        Map<CaseLink, SscsCaseData> linkedCaseMap = new HashMap<>();

        linkedCaseMap.put(CaseLink.builder().value(CaseLinkDetails.builder().caseReference(sscsCaseData.getCcdCaseId()).build()).build(), caseDetails.getCaseData());

        linkedCaseMap = buildUniqueSetOfLinkedCases(preSubmitCallbackResponse.getData().getLinkedCase(), linkedCaseMap);

        updateLinkedCases(linkedCaseMap);
        return preSubmitCallbackResponse;
    }

    private Map<CaseLink, SscsCaseData> buildUniqueSetOfLinkedCases(List<CaseLink> caseLinks, Map<CaseLink, SscsCaseData> linkedCaseMap) {
        if (caseLinks != null) {
            for (CaseLink caseLink : caseLinks) {

                SscsCaseDetails sscsCaseDetails = ccdService.getByCaseId(Long.valueOf(caseLink.getValue().getCaseReference()), idamService.getIdamTokens());

                if (!linkedCaseMap.containsKey(caseLink)) {
                    linkedCaseMap.put(caseLink, sscsCaseDetails.getData());
                    buildUniqueSetOfLinkedCases(sscsCaseDetails.getData().getLinkedCase(), linkedCaseMap);
                }
            }
        }

        return linkedCaseMap;
    }

    private void updateLinkedCases(Map<CaseLink, SscsCaseData> linkedCaseMap) {
        for (CaseLink caseLink : linkedCaseMap.keySet()) {

            SscsCaseData sscsCaseData = linkedCaseMap.get(caseLink);

            List<CaseLink> linkedCaseList = Lists.newArrayList(linkedCaseMap.keySet());
            linkedCaseList.remove(caseLink);

            if (sscsCaseData!= null && (sscsCaseData.getLinkedCase() == null || !sscsCaseData.getLinkedCase().containsAll(linkedCaseList))) {
                sscsCaseData.setLinkedCase(linkedCaseList);
                ccdService.updateCase(sscsCaseData, Long.valueOf(sscsCaseData.getCcdCaseId()), EventType.CASE_UPDATED.getCcdType(), "Case updated", "Linked case added", idamService.getIdamTokens());
            }
        }
    }
}
