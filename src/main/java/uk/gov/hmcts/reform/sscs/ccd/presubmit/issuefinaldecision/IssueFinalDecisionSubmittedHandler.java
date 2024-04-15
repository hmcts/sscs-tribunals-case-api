package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correction;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;
import uk.gov.hmcts.reform.sscs.service.event.EventPublisher;

@Component
@Slf4j
@AllArgsConstructor
public class IssueFinalDecisionSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdCallbackMapService ccdCallbackMapService;
    private final EventPublisher eventPublisher;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;


    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED) && (callback.getEvent() == EventType.ISSUE_FINAL_DECISION);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if (isPostHearingsEnabled) {
            Correction correction = caseData.getPostHearing().getCorrection();

            if (isYes(correction.getIsCorrectionFinalDecisionInProgress())) {
                correction.setIsCorrectionFinalDecisionInProgress(NO);
                caseData = ccdCallbackMapService.handleCcdCallbackMap(CorrectionActions.GRANT, caseData);

                return new PreSubmitCallbackResponse<>(caseData);
            }
        }
        log.info("Publishing message for the event {}", callback.getEvent());
        eventPublisher.publishEvent(callback);

        return new PreSubmitCallbackResponse<>(caseData);
    }
}
