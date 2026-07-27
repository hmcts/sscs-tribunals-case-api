package uk.gov.hmcts.reform.sscs.ccd.presubmit.amendspecialism;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;

@Service
@Slf4j
public class AmendSpecialismAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PanelCompositionService panelCompositionService;

    @Autowired
    public AmendSpecialismAboutToSubmitHandler(PanelCompositionService panelCompositionService) {
        this.panelCompositionService = panelCompositionService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.AMEND_SPECIALISM;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        var preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        caseData.setPanelMemberComposition(panelCompositionService
                .resetPanelCompositionIfStale(caseData, callback.getCaseDetailsBefore()));
        log.info("PanelComposition set to ({}) for case id {}",
                callback.getCaseDetails().getId(), caseData.getPanelMemberComposition());

        return preSubmitCallbackResponse;
    }
}
