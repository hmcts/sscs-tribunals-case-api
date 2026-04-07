package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@Service
public class AppealReceivedHandler implements CallbackHandler<SscsCaseData> {

    private final DispatchPriority dispatchPriority;

    private final UpdateCcdCaseService updateCcdCaseService;

    private final IdamService idamService;

    private final EvidenceShareConfig evidenceShareConfig;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public AppealReceivedHandler(UpdateCcdCaseService updateCcdCaseService,
                                 IdamService idamService,
                                 EvidenceShareConfig evidenceShareConfig) {
        this.dispatchPriority = DispatchPriority.LATEST;
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
        this.evidenceShareConfig = evidenceShareConfig;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
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
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        Long caseId = callback.getCaseDetails().getId();
        long delayMs = evidenceShareConfig.getAppealReceivedDelayMs();
        IdamTokens idamTokens = idamService.getIdamTokens();

        log.info("Scheduling appealReceived event for case id {} with delay of {}ms", caseId, delayMs);
        // This handler was causing 409 conflicts with Send to bulk print handler for digital cases as was trying to trigger the sent to fta event at the same time.
        // This delay should resolve this issue until we address properly with tickets SSCS-7525 and SSCS-7526 which look at removing the appeal received event.
        scheduler.schedule(() -> {
            try {
                log.info("About to update case v2 with appealReceived event for id {}", caseId);
                updateCcdCaseService.triggerCaseEventV2(
                        caseId,
                        APPEAL_RECEIVED.getCcdType(),
                        "Appeal received",
                        "Appeal received event has been triggered from Tribunals API for digital case",
                        idamTokens
                );
            } catch (Exception e) {
                log.error("Failed to trigger appealReceived event for case id {}: {}", caseId, e.getMessage(), e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
