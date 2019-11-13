package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Component
@Slf4j
public class HmctsResponseReviewedSubmittedHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    public HmctsResponseReviewedSubmittedHandler(CcdService ccdService, IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.HMCTS_RESPONSE_REVIEWED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.getIsInterlocRequired() != null && sscsCaseData.getIsInterlocRequired().equals("Yes")) {
            String whoToReview = sscsCaseData.getSelectWhoReviewsCase().getValue().getCode().equals("reviewByJudge") ? "Judge" : "TCW";
            updateCase(sscsCaseData, callback.getCaseDetails().getId(), EventType.VALID_SEND_TO_INTERLOC, "Send to interloc", "Send a case to a " + whoToReview + " for review");
        } else {
            updateCase(sscsCaseData, callback.getCaseDetails().getId(), EventType.READY_TO_LIST, "Ready to list", "Makes an appeal ready to list");
        }

        return preSubmitCallbackResponse;
    }

    private void updateCase(SscsCaseData caseData, Long caseId, EventType eventType, String summary, String description) {
        ccdService.updateCase(caseData, caseId, eventType.getCcdType(), summary, description, idamService.getIdamTokens());
    }
}
