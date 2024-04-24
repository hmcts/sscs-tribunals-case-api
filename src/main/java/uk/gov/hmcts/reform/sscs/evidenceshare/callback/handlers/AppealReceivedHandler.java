package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class AppealReceivedHandler implements CallbackHandler<SscsCaseData> {

    private final DispatchPriority dispatchPriority;

    private final CcdService ccdService;
    private final UpdateCcdCaseService updateCcdCaseService;

    private final IdamService idamService;

    @Value("${feature.trigger-eventV2.enabled}")
    private boolean isTriggerEventV2Enabled;

    @Autowired
    public AppealReceivedHandler(CcdService ccdService,
                                 UpdateCcdCaseService updateCcdCaseService,
                                 IdamService idamService) {
        this.dispatchPriority = DispatchPriority.LATEST;
        this.ccdService = ccdService;
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && (callback.getEvent() == EventType.VALID_APPEAL_CREATED
            || callback.getEvent() == EventType.DRAFT_TO_VALID_APPEAL_CREATED
            || callback.getEvent() == EventType.VALID_APPEAL
            || callback.getEvent() == EventType.INTERLOC_VALID_APPEAL)
            && READY_TO_LIST.getId().equals(callback.getCaseDetails().getCaseData().getCreatedInGapsFrom());
    }

    @Override
    @SuppressWarnings("squid:S2142")
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        try {
            // This handler was causing 409 conflicts with Send to bulk print handler for digital cases as was trying to trigger the sent to fta event at the same time.
            // This sleep should resolve this issue until we address properly with tickets SSCS-7525 and SSCS-7526 which look at removing the appeal received event.
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            log.error("Thread sleep interrupted: " + e.getMessage());
        }

        if (isTriggerEventV2Enabled) {
            log.info("About to update case v2 with appealReceived event for id {}", callback.getCaseDetails().getId());
            updateCcdCaseService.triggerCaseEventV2(
                    callback.getCaseDetails().getId(),
                    APPEAL_RECEIVED.getCcdType(),
                    "Appeal received",
                    "Appeal received event has been triggered from Tribunals API for digital case",
                    idamService.getIdamTokens()
            );
        } else {
            log.info("About to update case with appealReceived event for id {}", callback.getCaseDetails().getId());
            ccdService.updateCase(
                    callback.getCaseDetails().getCaseData(),
                    callback.getCaseDetails().getId(),
                    APPEAL_RECEIVED.getCcdType(),
                    "Appeal received",
                    "Appeal received event has been triggered from Evidence Share for digital case",
                    idamService.getIdamTokens()
            );
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
