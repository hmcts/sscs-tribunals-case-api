package uk.gov.hmcts.reform.sscs.ccd.presubmit.canceltranslations;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
@Slf4j
public class CancelTranslationsSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final CcdService ccdService;
    private final IdamService idamService;

    public CancelTranslationsSubmittedHandler(CcdService ccdService,
                                              IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent().equals(EventType.CANCEL_TRANSLATIONS)
            && !callback.getCaseDetails().getState().equals(State.INTERLOCUTORY_REVIEW_STATE)
            && StringUtils.isNotEmpty(callback.getCaseDetails().getCaseData().getSscsWelshPreviewNextEvent());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
            String sscsWelshPreviewNextEvent = callback.getCaseDetails().getCaseData().getSscsWelshPreviewNextEvent();
            callback.getCaseDetails().getCaseData().setSscsWelshPreviewNextEvent(null);
            SscsCaseDetails
                    sscsCaseDetails = ccdService
                    .updateCase(callback.getCaseDetails().getCaseData(), callback.getCaseDetails().getId(),
                            sscsWelshPreviewNextEvent, "Cancel welsh translations", "Cancel welsh translations",
                            idamService.getIdamTokens());

            return new PreSubmitCallbackResponse<>(sscsCaseDetails.getData());
    }
}
