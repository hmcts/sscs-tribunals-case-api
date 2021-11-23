package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpanelcomposition;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.panelcomposition.PanelCompositionService;


@Service
@Slf4j
public class ConfirmPanelCompositionSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private PanelCompositionService panelCompositionService;

    @Autowired
    public ConfirmPanelCompositionSubmittedHandler(PanelCompositionService panelCompositionService) {
        this.panelCompositionService = panelCompositionService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent() == EventType.CONFIRM_PANEL_COMPOSITION;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = panelCompositionService.processCaseState(callback, caseData);

        return new PreSubmitCallbackResponse<>(sscsCaseDetails.getData());
    }
}
