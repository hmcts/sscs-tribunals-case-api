package uk.gov.hmcts.reform.sscs.ccd.presubmit.associatecase;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.AssociatedCaseLinkHelper;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
public class AssociateCaseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData>  {

    private final CcdService ccdService;
    private final AssociatedCaseLinkHelper associatedCaseLinkHelper;
    private final IdamService idamService;



    public AssociateCaseAboutToSubmitHandler(CcdService ccdService, AssociatedCaseLinkHelper associatedCaseLinkHelper, IdamService idamService) {
        this.ccdService = ccdService;
        this.associatedCaseLinkHelper = associatedCaseLinkHelper;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ASSOCIATE_CASE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle associate case about to submit callback");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        List<CaseLink> caseLinks = sscsCaseData.getAssociatedCase();
        var cases = new ArrayList<SscsCaseDetails>();
        for (CaseLink caseLink : caseLinks) {
            SscsCaseDetails retrievedCase = ccdService.getByCaseId(
                    Long.parseLong(caseLink.getValue().getCaseReference()),
                    idamService.getIdamTokens());
            if (retrievedCase != null) {
                if (retrievedCase.getData().getAssociatedCase() != null) {
                    Boolean otherCaseMatch = retrievedCase.getData().getAssociatedCase()
                            .stream().anyMatch(c -> c.getValue().getCaseReference().equals(caseDetails.getCaseData().getCcdCaseId()));
                    if (!otherCaseMatch) {
                        cases.add(retrievedCase);
                    }
                } else {
                    cases.add(retrievedCase);
                }
            }
        }
        if (!cases.isEmpty()) {
            associatedCaseLinkHelper.addLinkToOtherAssociatedCasesV2Enabled(cases, caseDetails.getCaseData().getCcdCaseId());
        }
        return preSubmitCallbackResponse;
    }

}
