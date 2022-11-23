package uk.gov.hmcts.reform.sscs.ccd.presubmit.requestposthearing;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.RequestFormat.GENERATE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestFormat;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestPostHearingTypes;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
@RequiredArgsConstructor
public class RequestPostHearingMidEventHandler  implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String PAGE_ID_GENERATE_DOCUMENT = "generateDocument";
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.REQUEST_POST_HEARING
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String pageId = callback.getPageId();
        String caseId = caseData.getCcdCaseId();
        log.info("Post Hearing Request: handling callback with pageId {} for caseId {}", pageId, caseId);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        RequestPostHearingTypes typeSelected = caseData.getPostHearing().getRequestTypeSelected();
        log.info("Post Hearing Request: handing action {} for case {}", typeSelected,  caseId);

        RequestFormat requestFormat = getRequestFormat(caseData.getPostHearing());

        if (PAGE_ID_GENERATE_DOCUMENT.equals(pageId) && GENERATE.equals(requestFormat)) {
            log.info("Post Hearing Request: Generating notice for caseId {}", caseId);
            // TODO SSCS-10983 put doc generation here
        }

        return response;
    }

    @Nullable
    private static RequestFormat getRequestFormat(PostHearing postHearing) {
        RequestPostHearingTypes typeSelected = postHearing.getRequestTypeSelected();
        if (isNull(typeSelected)) {
            return null;
        }
        switch (typeSelected) {
            case SET_ASIDE:
                return postHearing.getSetAside().getRequestFormat();
            default:
                return null;
        }
    }
}
