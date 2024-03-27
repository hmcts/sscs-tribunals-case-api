package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ASSOCIATE_CASE;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@Service
public class ReciprocalLinkHandler implements CallbackHandler<SscsCaseData> {

    private final DispatchPriority dispatchPriority;

    private final CcdService ccdService;

    private final IdamService idamService;

    @Autowired
    public ReciprocalLinkHandler(CcdService ccdService,
                                 IdamService idamService) {
        this.dispatchPriority = DispatchPriority.LATEST;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && (callback.getEvent() == EventType.VALID_APPEAL_CREATED
            || callback.getEvent() == EventType.DRAFT_TO_VALID_APPEAL_CREATED
            || callback.getEvent() == EventType.NON_COMPLIANT
            || callback.getEvent() == EventType.DRAFT_TO_NON_COMPLIANT
            || callback.getEvent() == EventType.INCOMPLETE_APPLICATION_RECEIVED
            || callback.getEvent() == EventType.DRAFT_TO_INCOMPLETE_APPLICATION);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        log.info("Reciprocal link handler for case id {}", callback.getCaseDetails().getId());

        IdamTokens idamTokens = idamService.getIdamTokens();

        if (callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getIdentity() != null
            && !StringUtils.isEmpty(callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getIdentity().getNino())) {

            String nino = callback.getCaseDetails().getCaseData().getAppeal().getAppellant().getIdentity().getNino();

            List<SscsCaseDetails> matchedByNinoCases = getMatchedCases(nino, idamTokens);

            if (matchedByNinoCases.size() > 0) {
                log.info("Found " + matchedByNinoCases.size() + " matching cases for Nino " + nino);

                backLinkAssociatedCases(callback.getCaseDetails().getId(), matchedByNinoCases, idamTokens);
            }
        }
    }

    protected List<SscsCaseDetails> getMatchedCases(String nino, IdamTokens idamTokens) {
        return ccdService.findCaseBy("data.appeal.appellant.identity.nino", nino, idamTokens);
    }

    private void backLinkAssociatedCases(Long caseId, List<SscsCaseDetails> matchedByNinoCases, IdamTokens idamTokens) {

        if (matchedByNinoCases.size() > 0 && matchedByNinoCases.size() < 11) {

            CaseLink caseLink = CaseLink.builder().value(
                CaseLinkDetails.builder().caseReference(caseId.toString()).build()).build();

            for (SscsCaseDetails matchedCase : matchedByNinoCases) {

                if (!matchedCase.getId().equals(caseId)) {

                    List<CaseLink> caseLinks = matchedCase.getData().getAssociatedCase() != null ? matchedCase.getData().getAssociatedCase() : new ArrayList<>();

                    caseLinks.add(caseLink);
                    matchedCase.getData().setAssociatedCase(caseLinks);
                    matchedCase.getData().setLinkedCasesBoolean("Yes");

                    log.info("Back linking case id {} to case id {}", caseId, matchedCase.getId().toString());

                    ccdService.updateCase(matchedCase.getData(), matchedCase.getId(), ASSOCIATE_CASE.getCcdType(), "Associate case", "Associated case added", idamTokens);
                }
            }
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
