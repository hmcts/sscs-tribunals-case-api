package uk.gov.hmcts.reform.sscs.ccd.presubmit.getfirsttierdocuments;

import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.bundling.BundlingHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
@RequiredArgsConstructor
public class GetFirstTierDocumentsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Value("${feature.postHearingsB.enabled}")
    private final boolean isPostHearingsBEnabled;

    @Autowired
    private final BundlingHandler bundlingHandler;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.GET_FIRST_TIER_DOCUMENTS
                && isPostHearingsEnabled
                && isPostHearingsBEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        return bundlingHandler.handle(callback);
    }
}
