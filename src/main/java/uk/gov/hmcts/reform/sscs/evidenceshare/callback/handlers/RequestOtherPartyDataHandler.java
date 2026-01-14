package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority.LATEST;
import static uk.gov.hmcts.reform.sscs.featureflag.FeatureFlag.SSCS_CHILD_MAINTENANCE_FT;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.FeatureToggleService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class RequestOtherPartyDataHandler implements CallbackHandler<SscsCaseData> {

    private static final String SUMMARY = "REQUEST_OTHER_PARTY_DATA";
    private static final String DESCRIPTION = "Requesting other party data";

    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;
    private final FeatureToggleService featureToggleService;

    public RequestOtherPartyDataHandler(UpdateCcdCaseService updateCcdCaseService, IdamService idamService,
                                        FeatureToggleService featureToggleService) {
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
        this.featureToggleService = featureToggleService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!featureToggleService.isEnabled(SSCS_CHILD_MAINTENANCE_FT, null, null)) {
            return false;
        }

        if (callbackType != CallbackType.SUBMITTED || callback.getEvent() != EventType.VALID_APPEAL_CREATED) {
            return false;
        }

        return callback.getCaseDetails()
            .getCaseData()
            .getBenefitType()
            .map(Benefit.CHILD_SUPPORT::equals)
            .orElse(false);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            return;
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        long caseId = Long.parseLong(caseData.getCcdCaseId());

        updateCcdCaseService.updateCaseV2(caseId, EventType.REQUEST_OTHER_PARTY_DATA.getCcdType(), SUMMARY, DESCRIPTION,
            idamService.getIdamTokens(), ignored -> log.info("Request other party details for case id {}", caseId));
    }

    @Override
    public DispatchPriority getPriority() {
        return LATEST;
    }
}
