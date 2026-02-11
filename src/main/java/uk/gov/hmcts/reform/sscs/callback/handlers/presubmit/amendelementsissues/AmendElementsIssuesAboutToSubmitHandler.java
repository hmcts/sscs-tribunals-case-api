package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.amendelementsissues;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;

@Service
@Slf4j
public class AmendElementsIssuesAboutToSubmitHandler extends ResponseEventsAboutToSubmit
        implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PanelCompositionService panelCompositionService;

    public AmendElementsIssuesAboutToSubmitHandler(PanelCompositionService panelCompositionService) {
        this.panelCompositionService = panelCompositionService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.AMEND_ELEMENTS_ISSUES;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        log.info("Setting case code for case id {}", callback.getCaseDetails().getId());
        setCaseCode(preSubmitCallbackResponse, callback);

        caseData.setPanelMemberComposition(panelCompositionService
                .resetPanelCompIfElementsChanged(caseData, callback.getCaseDetailsBefore()));
        return preSubmitCallbackResponse;
    }
}
