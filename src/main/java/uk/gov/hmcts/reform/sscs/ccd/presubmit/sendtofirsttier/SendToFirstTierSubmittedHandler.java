package uk.gov.hmcts.reform.sscs.ccd.presubmit.sendtofirsttier;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;

@Service
@Slf4j
@RequiredArgsConstructor
public class SendToFirstTierSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final CcdCallbackMapService ccdCallbackMapService;

    @Value("${feature.postHearingsB.enabled}")
    private final boolean isPostHearingsBEnabled;

    @Value("${feature.handle-ccd-callbackMap-v2.enabled}")
    private boolean isHandleCcdCallbackMapV2Enabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent() == EventType.SEND_TO_FIRST_TIER
                && isPostHearingsBEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if (isHandleCcdCallbackMapV2Enabled) {
            Optional<SscsCaseData> sscsCaseDataOptional = ccdCallbackMapService.handleCcdCallbackMapV2(
                    caseData.getPostHearing().getSendToFirstTier().getAction(),
                    sscsCaseData -> { },
                    callback.getCaseDetails().getId()
            );
            return new PreSubmitCallbackResponse<>(sscsCaseDataOptional.orElse(caseData));
        } else {
            caseData = ccdCallbackMapService.handleCcdCallbackMap(caseData.getPostHearing().getSendToFirstTier().getAction(), caseData);
            return new PreSubmitCallbackResponse<>(caseData);
        }
    }
}
