package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REQUEST_TRANSLATION_FROM_WLU;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.WelshException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.RequestTranslationService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class RequestTranslationCallbackHandler implements CallbackHandler<SscsCaseData> {

    private final RequestTranslationService requestTranslationService;
    private final DispatchPriority dispatchPriority;
    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    public RequestTranslationCallbackHandler(RequestTranslationService requestTranslationService,
                                             CcdService ccdService,
                                             IdamService idamService) {
        this.requestTranslationService = requestTranslationService;
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.dispatchPriority = DispatchPriority.EARLIEST;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && (callback.getEvent() == REQUEST_TRANSLATION_FROM_WLU);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!callback.getCaseDetails().getCaseData().isLanguagePreferenceWelsh()) {
            throw new IllegalStateException("Error: This action is only available for Welsh cases");
        }

        log.info("Processing wlu translation for case id {} in evidence share service",
            callback.getCaseDetails().getId());

        try {
            log.info("sending email for case  id {}", callback.getCaseDetails().getId());
            if (requestTranslationService.sendCaseToWlu(callback.getCaseDetails()) && callback.getEvent() == REQUEST_TRANSLATION_FROM_WLU) {
                ccdService.updateCase(callback.getCaseDetails().getCaseData(), Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId()),
                    CASE_UPDATED.getCcdType(), "Case translations sent to wlu", "Updated case with date sent to wlu",
                    idamService.getIdamTokens());
            }
        } catch (WelshException e) {
            log.error("Error when sending to request translation from wlu: {}", callback.getCaseDetails().getId(), e);
        }
    }


    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
