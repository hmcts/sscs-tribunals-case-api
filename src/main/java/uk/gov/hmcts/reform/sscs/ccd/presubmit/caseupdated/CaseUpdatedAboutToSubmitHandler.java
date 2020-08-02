package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Component
@Slf4j
public class CaseUpdatedAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    CaseUpdatedAboutToSubmitHandler(RegionalProcessingCenterService regionalProcessingCenterService,
                                    CcdService ccdService,
                                    IdamService idamService) {
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.CASE_UPDATED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        setCaseCode(sscsCaseData);

        if (sscsCaseData.getAppeal().getAppellant() != null && sscsCaseData.getAppeal().getAppellant().getAddress() != null && sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode() != null) {
            RegionalProcessingCenter rpc = regionalProcessingCenterService.getByPostcode(sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode());
            sscsCaseData.setRegionalProcessingCenter(rpc);

            if (rpc != null) {
                sscsCaseData.setRegion(rpc.getName());
            }
        }

        Identity identity = sscsCaseData.getAppeal().getAppellant().getIdentity();
        final String nino = (identity != null) ? identity.getNino() : null;
        if (!StringUtils.isEmpty(nino)) {
            List<SscsCaseDetails> matchedByNinoCases = getMatchedCases(nino, idamService.getIdamTokens());
            if (!matchedByNinoCases.isEmpty()) {
                log.info("Found " + matchedByNinoCases.size() + " matching cases for Nino " + nino);

                addAssociatedCases(sscsCaseData, matchedByNinoCases);
            }
        }

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        return preSubmitCallbackResponse;
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
            return caseData.toBuilder().associatedCase(associatedCases).linkedCasesBoolean("Yes").build();
        } else {
            return caseData.toBuilder().linkedCasesBoolean("No").build();
        }
    }
}