package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_TO_PROCEED;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class IssueDirectionHandler implements CallbackHandler<SscsCaseData> {

    private final DispatchPriority dispatchPriority;

    private final UpdateCcdCaseService updateCcdCaseService;

    private final IdamService idamService;

    @Autowired
    public IssueDirectionHandler(UpdateCcdCaseService ccdService,
                                 IdamService idamService) {
        this.dispatchPriority = DispatchPriority.EARLY;
        this.updateCcdCaseService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        log.info("IssueDirectionHandler canHandle method called for caseId {} and callbackType {}", callback.getCaseDetails().getId(), callbackType);
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.DIRECTION_ISSUED
            && callback.getCaseDetails().getState().equals(State.INTERLOCUTORY_REVIEW_STATE)
            && callback.getCaseDetails().getCaseData().getDirectionTypeDl() != null
            && DirectionType.APPEAL_TO_PROCEED.toString().equals(callback.getCaseDetails().getCaseData().getDirectionTypeDl().getValue().getCode());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        log.info("About to update case v2 with appealToProceed event for id {}", callback.getCaseDetails().getId());
        try {
            updateCcdCaseService.updateCaseV2(
                    callback.getCaseDetails().getId(),
                    APPEAL_TO_PROCEED.getCcdType(),
                    "Appeal to proceed",
                    "Appeal proceed event triggered",
                    idamService.getIdamTokens(),
                    sscsCaseData -> sscsCaseData.setDirectionTypeDl(null)
            );
        } catch (FeignException.UnprocessableEntity e) {
            log.error(format("appealToProceed event failed for caseId %s, root cause is %s", callback.getCaseDetails().getId(), getRootCauseMessage(e)), e);
            throw e;
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
