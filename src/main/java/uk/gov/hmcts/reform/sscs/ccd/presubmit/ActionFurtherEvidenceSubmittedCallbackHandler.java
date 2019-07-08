package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.FurtherEvidenceActionDynamicListItems.INFORMATION_RECEIVED_FOR_INTERLOC;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
public class ActionFurtherEvidenceSubmittedCallbackHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    public ActionFurtherEvidenceSubmittedCallbackHandler(CcdService ccdService, IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");
        return (callbackType.equals(CallbackType.SUBMITTED)
            && isInformationReceivedForInterloc(
            callback.getCaseDetails().getCaseData().getFurtherEvidenceAction().getValue().getCode()));
    }

    private boolean isInformationReceivedForInterloc(String code) {
        if (StringUtils.isNotBlank(code)) {
            return code.equalsIgnoreCase(INFORMATION_RECEIVED_FOR_INTERLOC.getCode());
        }
        return false;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setInterlocReviewState("interlocutoryReview");
        ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
            EventType.INTERLOC_INFORMATION_RECEIVED.getCcdType(), "update to Information received event",
            "update to Information received event", idamService.getIdamTokens());

        return new PreSubmitCallbackResponse<>(caseData);
    }

}
