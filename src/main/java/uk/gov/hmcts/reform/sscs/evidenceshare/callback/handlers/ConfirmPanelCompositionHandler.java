package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.PanelCompositionService;

@Slf4j
@Service
public class ConfirmPanelCompositionHandler implements CallbackHandler<SscsCaseData> {

    private PanelCompositionService panelCompositionService;

    @Autowired
    public ConfirmPanelCompositionHandler(PanelCompositionService panelCompositionService) {
        this.panelCompositionService = panelCompositionService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        log.info("ConfirmPanelCompositionHandler canHandle method called for caseId {} and callbackType {}", callback.getCaseDetails().getId(), callbackType);
        requireNonNull(callback, "callback must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.CONFIRM_PANEL_COMPOSITION
            && callback.getCaseDetails().getCaseData().getAppeal() != null
            && callback.getCaseDetails().getCaseData().getAppeal().getBenefitType() != null
            && StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().getCode(), Benefit.CHILD_SUPPORT.getShortName());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            log.info("Cannot handle this event for case id: {}", callback.getCaseDetails().getId());
            throw new IllegalStateException("Cannot handle callback");
        }

        panelCompositionService.processCaseState(callback, callback.getCaseDetails().getCaseData(), callback.getEvent());
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
