package uk.gov.hmcts.reform.sscs.ccd.presubmit.getfirsttierdocuments;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdCallbackMap;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.GetFirstTierDocumentsActions.BUNDLE_CREATED;

@Service
@Slf4j
@RequiredArgsConstructor
public class GetFirstTierDocumentsSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final CcdCallbackMapService ccdCallbackMapService;

    @Value("${feature.postHearingsB.enabled}")
    private final boolean isPostHearingsBEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.GET_FIRST_TIER_DOCUMENTS
            && isPostHearingsBEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();


        Long caseId = Long.valueOf(caseData.getCcdCaseId());
        CcdCallbackMap callbackMap = BUNDLE_CREATED;
        log.info("Get first tier documents: handling action {} for case {}", callbackMap, caseId);

        caseData = ccdCallbackMapService.handleCcdCallbackMap(callbackMap, caseData);

        return new PreSubmitCallbackResponse<>(caseData);
    }
}
