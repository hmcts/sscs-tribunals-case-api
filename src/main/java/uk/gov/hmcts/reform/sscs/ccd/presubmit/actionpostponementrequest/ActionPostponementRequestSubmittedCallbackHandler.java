package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class ActionPostponementRequestSubmittedCallbackHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdService ccdService;
    private final IdamService idamService;
    private final boolean workAllocationFeature;

    public ActionPostponementRequestSubmittedCallbackHandler(CcdService ccdService, IdamService idamService,
                                                             @Value("${feature.work-allocation.enabled}") boolean workAllocationFeature) {
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.workAllocationFeature = workAllocationFeature;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");
        return callbackType.equals(CallbackType.SUBMITTED)
                && (callback.getEvent().equals(EventType.ACTION_POSTPONEMENT_REQUEST)
                    || callback.getEvent().equals(EventType.ACTION_POSTPONEMENT_REQUEST_WELSH))
                && workAllocationFeature;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = updateCase(callback, caseData);
        clearTransientFields(sscsCaseDetails.getData());

        return new PreSubmitCallbackResponse<>(sscsCaseDetails.getData());
    }

    private SscsCaseDetails updateCase(Callback<SscsCaseData> callback, SscsCaseData caseData) {
        String actionRequested = caseData.getPostponementRequest().getActionPostponementRequestSelected();
        if (SEND_TO_JUDGE.getValue().equals(actionRequested)) {
            return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                    EventType.POSTPONEMENT_SEND_TO_JUDGE.getCcdType(), "Postponement request sent to judge",
                    "Postponement request sent to judge", idamService.getIdamTokens());
        } else if (REFUSE.getValue().equals(actionRequested)) {
            return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                    EventType.POSTPONEMENT_REFUSED.getCcdType(), "Postponement refused",
                    "Postponement refused", idamService.getIdamTokens());
        } else if (GRANT.getValue().equals(actionRequested)) {
            return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                    EventType.POSTPONEMENT_GRANTED.getCcdType(), "Postponement granted",
                    "Postponement granted", idamService.getIdamTokens());
        }

        return SscsCaseDetails.builder().data(caseData).build();
    }

    private void clearTransientFields(SscsCaseData caseData) {
        caseData.setDocumentGeneration(DocumentGeneration.builder().build());
        caseData.setDocumentStaging(DocumentStaging.builder().build());
        caseData.setTempNoteDetail(null);
        caseData.setShowRip1DocPage(null);

        YesNo unprocessedPostponementRequest = caseData.getPostponementRequest().getUnprocessedPostponementRequest();
        caseData.setPostponementRequest(PostponementRequest.builder()
                .unprocessedPostponementRequest(unprocessedPostponementRequest)
                .build());
    }
}
