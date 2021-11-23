package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.panelcomposition.PanelCompositionService;

import java.util.List;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.assignOtherPartyId;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.updateOtherPartyUcb;


@Component
@Slf4j
public class UpdateOtherPartySubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private PanelCompositionService panelCompositionService;

    @Autowired
    public UpdateOtherPartySubmittedHandler(PanelCompositionService panelCompositionService) {
        this.panelCompositionService = panelCompositionService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent() == EventType.UPDATE_OTHER_PARTY_DATA;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = panelCompositionService.processCaseState(callback, caseData);

        if (sscsCaseDetails.getState() != null && sscsCaseDetails.getState().equals(State.READY_TO_LIST)) {
            sscsCaseDetails.getData().setDwpDueDate(null);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseDetails.getData());
    }
}
