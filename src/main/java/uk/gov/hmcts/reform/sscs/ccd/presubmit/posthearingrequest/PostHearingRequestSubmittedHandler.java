package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingrequest;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostHearingRequestSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdCallbackMapService ccdCallbackMapService;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.POST_HEARING_REQUEST
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if (!isNull(caseData.getSscsDocument())) {
            log.info("Documents size is {}", caseData.getSscsDocument().size());
        }

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        Long caseId = Long.valueOf(caseData.getCcdCaseId());

        PostHearing postHearing = caseData.getPostHearing();
        PostHearingRequestType typeSelected = postHearing.getRequestType();
        log.info("Post Hearing Request: handling postHearing {} for case {}", typeSelected,  caseId);

        CcdCallbackMap callbackMap = postHearing.getRequestType();

        if (isNull(callbackMap)) {
            response.addError(String.format("Invalid Post Hearing Request Type Selected %s or request "
                    + "selected as callback is null",
                typeSelected));
            return response;
        }

        SscsUtil.clearPostHearingFields(caseData, isPostHearingsEnabled);

        caseData = ccdCallbackMapService.handleCcdCallbackMap(callbackMap, caseData);

        if (!isNull(caseData.getSscsDocument()) && !caseData.getSscsDocument().isEmpty()) {
            log.info("Document Is {}", caseData.getSscsDocument().get(caseData.getSscsDocument().size() - 1).getValue());

            var doc = caseData.getLatestDocumentForDocumentType(DocumentType.STATEMENT_OF_REASONS_APPLICATION);

            if (doc != null) {
                log.info("Sor Document {}", doc.getValue());
            }
        }

        return new PreSubmitCallbackResponse<>(caseData);
    }
}
