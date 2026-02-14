package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.updatenotlistable;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Component
@Slf4j
public class UpdateNotListableSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;

    @Autowired
    public UpdateNotListableSubmittedHandler(UpdateCcdCaseService updateCcdCaseService, IdamService idamService) {
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.UPDATE_NOT_LISTABLE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (YES.equals(sscsCaseData.getShouldReadyToListBeTriggered())) {
            try {
                updateCcdCaseService.updateCaseV2(
                    callback.getCaseDetails().getId(),
                    READY_TO_LIST.getCcdType(),
                    "Ready to list",
                    "Makes an appeal ready to list",
                    idamService.getIdamTokens(),
                    // sscsCaseDetails -> sscsCaseDetails.getData().setIgnoreCallbackWarnings(YES));
                    sscsCaseDetails -> sscsCaseDetails.getData().setShouldReadyToListBeTriggered(null));
            } catch (FeignException e) {
                log.error(
                    "{}. CCD response: {}",
                    String.format("Could not update event %s for case %d", READY_TO_LIST, callback.getCaseDetails().getId()),
                    // exception.contentUTF8() uses response body internally
                    e.responseBody().isPresent() ? e.contentUTF8() : e.getMessage()
                );
                throw e;
            }
        }

        return preSubmitCallbackResponse;
    }
}
