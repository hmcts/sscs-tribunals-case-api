package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.INFORMATION_RECEIVED_FOR_INTERLOC_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
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
        DynamicList furtherEvidenceAction = callback.getCaseDetails().getCaseData().getFurtherEvidenceAction();
        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent().equals(EventType.ACTION_FURTHER_EVIDENCE)
            && (isFurtherEvidenceActionOptionValid(furtherEvidenceAction, INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE)
            || isFurtherEvidenceActionOptionValid(furtherEvidenceAction, INFORMATION_RECEIVED_FOR_INTERLOC_TCW)
            || isFurtherEvidenceActionOptionValid(furtherEvidenceAction, ISSUE_FURTHER_EVIDENCE)
            || isFurtherEvidenceActionOptionValid(furtherEvidenceAction, SEND_TO_INTERLOC_REVIEW_BY_JUDGE));
    }

    private boolean isFurtherEvidenceActionOptionValid(DynamicList furtherEvidenceActionList,
                                                       FurtherEvidenceActionDynamicListItems interlocType) {
        if (furtherEvidenceActionList != null && furtherEvidenceActionList.getValue() != null
            && StringUtils.isNotBlank(furtherEvidenceActionList.getValue().getCode())) {
            return furtherEvidenceActionList.getValue().getCode().equals(interlocType.getCode());
        }
        return false;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = updateCase(callback, caseData);

        return new PreSubmitCallbackResponse<>(sscsCaseDetails.getData());
    }

    private SscsCaseDetails updateCase(Callback<SscsCaseData> callback, SscsCaseData caseData) {
        if (isFurtherEvidenceActionOptionValid(caseData.getFurtherEvidenceAction(),
            INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE)) {
            return updateCaseInformationReceivedForInterlocDetails(caseData, callback.getCaseDetails().getId(),
                "reviewByJudge", INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,
                EventType.INTERLOC_INFORMATION_RECEIVED, "Interloc information received event");
        }
        if (isFurtherEvidenceActionOptionValid(caseData.getFurtherEvidenceAction(),
            INFORMATION_RECEIVED_FOR_INTERLOC_TCW)) {
            return updateCaseInformationReceivedForInterlocDetails(caseData, callback.getCaseDetails().getId(),
                "reviewByTcw", INFORMATION_RECEIVED_FOR_INTERLOC_TCW,
                EventType.INTERLOC_INFORMATION_RECEIVED, "Interloc information received event");
        }
        if (isFurtherEvidenceActionOptionValid(caseData.getFurtherEvidenceAction(),
            SEND_TO_INTERLOC_REVIEW_BY_JUDGE)) {
            return updateCaseInformationReceivedForInterlocDetails(caseData, callback.getCaseDetails().getId(),
                "reviewByJudge", SEND_TO_INTERLOC_REVIEW_BY_JUDGE,
                EventType.VALID_SEND_TO_INTERLOC, "Send a case to a judge for review");
        }
        return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
            EventType.ISSUE_FURTHER_EVIDENCE.getCcdType(), "Issue to all parties",
            "Issue to all parties", idamService.getIdamTokens());
    }

    private SscsCaseDetails updateCaseInformationReceivedForInterlocDetails(
        SscsCaseData caseData, Long caseId,
        String interlocReviewState,
        FurtherEvidenceActionDynamicListItems interlocType, EventType eventType, String summary) {

        caseData.setInterlocReviewState(interlocReviewState);
        return ccdService.updateCase(caseData, caseId,
            eventType.getCcdType(), summary,
            interlocType.getLabel(), idamService.getIdamTokens());
    }

}
