package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority.LATEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class RequestOtherPartyDataHandler implements CallbackHandler<SscsCaseData> {

    private static final String SUMMARY = "REQUEST_OTHER_PARTY_DATA";
    private static final String DESCRIPTION = "Requesting other party data";

    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;
    private final boolean cmOtherPartyConfidentialityEnabled;

    public RequestOtherPartyDataHandler(UpdateCcdCaseService updateCcdCaseService, IdamService idamService,
                                        @Value("${feature.cm-other-party-confidentiality.enabled}") boolean cmOtherPartyConfidentialityEnabled) {
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {

        if (!cmOtherPartyConfidentialityEnabled
            || callbackType != CallbackType.SUBMITTED
            || callback.getEvent() != EventType.VALID_APPEAL_CREATED) {
            return false;
        }

        return callback.getCaseDetails().getCaseData().isBenefitType(CHILD_SUPPORT);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            return;
        }

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        long caseId = Long.parseLong(caseData.getCcdCaseId());

        updateCcdCaseService.updateCaseV2(caseId, EventType.REQUEST_OTHER_PARTY_DATA.getCcdType(), SUMMARY, DESCRIPTION,
            idamService.getIdamTokens(), ignored -> log.info("Request other party details for case id {}", caseId));
    }

    @Override
    public DispatchPriority getPriority() {
        return LATEST;
    }
}
