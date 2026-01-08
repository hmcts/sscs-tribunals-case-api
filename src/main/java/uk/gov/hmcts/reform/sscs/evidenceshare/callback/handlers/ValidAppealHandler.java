package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority.LATEST;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class ValidAppealHandler implements CallbackHandler<SscsCaseData> {

    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;

    @Autowired
    public ValidAppealHandler(UpdateCcdCaseService updateCcdCaseService, IdamService idamService) {
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        final Benefit benefitType = caseData.getBenefitType().orElse(null);

        return callbackType == CallbackType.SUBMITTED && callback.getEvent() == EventType.VALID_APPEAL && benefitType == Benefit.CHILD_SUPPORT;
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {

        if (!canHandle(callbackType, callback)) {
            return;
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        updateCcdCaseService.updateCaseV2(
            Long.valueOf(caseData.getCcdCaseId()),
            EventType.REQUEST_OTHER_PARTY_DATA.getCcdType(),
            "REQUEST_OTHER_PARTY_DATA",
            "Requesting other party data",
            idamService.getIdamTokens(),
            sscsCaseDetails -> log.info("Request other party details for case id {}", caseData.getCcdCaseId()));

    }

    @Override
    public DispatchPriority getPriority() {
        return LATEST;
    }

}
