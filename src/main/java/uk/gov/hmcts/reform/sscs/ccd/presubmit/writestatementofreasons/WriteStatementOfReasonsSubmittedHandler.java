package uk.gov.hmcts.reform.sscs.ccd.presubmit.writestatementofreasons;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.WriteStatementOfReasons;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class WriteStatementOfReasonsSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdCallbackMapService ccdCallbackMapService;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;
    @Value("${feature.handle-ccd-callbackMap-v2.enabled}")
    private boolean isHandleCcdCallbackMapV2Enabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.SOR_WRITE
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        long caseId = Long.parseLong(caseData.getCcdCaseId());

        log.info("Write Statement of Reasons: processing submitted handler for case {}", caseId);

        CcdCallbackMap callbackMap = WriteStatementOfReasons.IN_TIME;

        Consumer<SscsCaseData> sscsCaseDataConsumer = sscsCaseData -> SscsUtil.clearPostHearingFields(sscsCaseData, isPostHearingsEnabled);

        if (isHandleCcdCallbackMapV2Enabled) {
            caseData = ccdCallbackMapService.handleCcdCallbackMapV2(
                    callbackMap,
                    caseId,
                    sscsCaseDataConsumer
            );
        } else {
            sscsCaseDataConsumer.accept(caseData);
            caseData = ccdCallbackMapService.handleCcdCallbackMap(callbackMap, caseData);
        }
        return new PreSubmitCallbackResponse<>(caseData);
    }
}
