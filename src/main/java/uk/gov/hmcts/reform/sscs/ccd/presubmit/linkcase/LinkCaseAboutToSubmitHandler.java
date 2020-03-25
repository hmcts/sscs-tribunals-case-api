package uk.gov.hmcts.reform.sscs.ccd.presubmit.linkcase;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        Set<CaseLink> linkedCaseSet = new HashSet<>();
        linkedCaseSet.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference(sscsCaseData.getCcdCaseId()).build()).build());
        linkedCaseSet.addAll(preSubmitCallbackResponse.getData().getLinkedCase());

        linkedCaseSet = buildUniqueSetOfLinkedCases(preSubmitCallbackResponse.getData().getLinkedCase(), linkedCaseSet);

        updateLinkedCases(linkedCaseSet);
        return preSubmitCallbackResponse;
    }

    private Set<CaseLink> buildUniqueSetOfLinkedCases(List<CaseLink> caseLinks, Set<CaseLink> linkedCaseSet) {
        for (CaseLink caseLink : caseLinks) {

            SscsCaseDetails sscsCaseDetails = ccdService.getByCaseId(Long.valueOf(caseLink.getValue().getCaseReference()), idamService.getIdamTokens());

            if (sscsCaseDetails.getData().getLinkedCase() != null && !linkedCaseSet.containsAll(sscsCaseDetails.getData().getLinkedCase())) {
                linkedCaseSet.addAll(sscsCaseDetails.getData().getLinkedCase());
            }
        }

        return linkedCaseSet;
    }

    private void updateLinkedCases(Set<CaseLink> linkedCaseSet) {
        for (CaseLink caseLink : linkedCaseSet) {

            SscsCaseDetails sscsCaseDetails = ccdService.getByCaseId(Long.valueOf(caseLink.getValue().getCaseReference()), idamService.getIdamTokens());

            List<CaseLink> linkedCaseList = Lists.newArrayList(linkedCaseSet);
            linkedCaseList.remove(caseLink);

            if (sscsCaseDetails != null && (sscsCaseDetails.getData().getLinkedCase() == null || !sscsCaseDetails.getData().getLinkedCase().containsAll(linkedCaseList))) {
                sscsCaseDetails.getData().setLinkedCase(linkedCaseList);
                ccdService.updateCase(sscsCaseDetails.getData(), sscsCaseDetails.getId(), EventType.CASE_UPDATED.getCcdType(), "Case updated", "Linked case added", idamService.getIdamTokens());
            }
        }
    }
}
