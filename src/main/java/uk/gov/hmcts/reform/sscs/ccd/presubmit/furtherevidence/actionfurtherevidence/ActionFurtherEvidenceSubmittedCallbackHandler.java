package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.URGENT_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
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
            && furtherEvidenceActionOptionValidation(furtherEvidenceAction);
    }

    private boolean furtherEvidenceActionOptionValidation(DynamicList furtherEvidenceAction) {
        return isFurtherEvidenceActionOptionValid(furtherEvidenceAction, INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE)
            || isFurtherEvidenceActionOptionValid(furtherEvidenceAction, INFORMATION_RECEIVED_FOR_INTERLOC_TCW)
            || isFurtherEvidenceActionOptionValid(furtherEvidenceAction, ISSUE_FURTHER_EVIDENCE)
            || isFurtherEvidenceActionOptionValid(furtherEvidenceAction, SEND_TO_INTERLOC_REVIEW_BY_JUDGE)
            || isFurtherEvidenceActionOptionValid(furtherEvidenceAction, SEND_TO_INTERLOC_REVIEW_BY_TCW)
            || isFurtherEvidenceActionOptionValid(furtherEvidenceAction, OTHER_DOCUMENT_MANUAL);
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
            return setInterlocReviewStateFieldAndTriggerEvent(caseData, callback.getCaseDetails().getId(),
                AWAITING_ADMIN_ACTION.getId(), INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,
                EventType.INTERLOC_INFORMATION_RECEIVED_ACTION_FURTHER_EVIDENCE, "Interloc information received event");
        }
        if (isFurtherEvidenceActionOptionValid(caseData.getFurtherEvidenceAction(),
            INFORMATION_RECEIVED_FOR_INTERLOC_TCW)) {
            return setInterlocReviewStateFieldAndTriggerEvent(caseData, callback.getCaseDetails().getId(),
                REVIEW_BY_TCW.getId(), INFORMATION_RECEIVED_FOR_INTERLOC_TCW,
                EventType.INTERLOC_INFORMATION_RECEIVED_ACTION_FURTHER_EVIDENCE, "Interloc information received event");
        }
        if (isFurtherEvidenceActionOptionValid(caseData.getFurtherEvidenceAction(),
            SEND_TO_INTERLOC_REVIEW_BY_JUDGE)) {
            setSelectWhoReviewsCaseField(caseData, REVIEW_BY_JUDGE);
            return setInterlocReviewStateFieldAndTriggerEvent(caseData, callback.getCaseDetails().getId(),
                REVIEW_BY_JUDGE.getId(), SEND_TO_INTERLOC_REVIEW_BY_JUDGE,
                EventType.VALID_SEND_TO_INTERLOC, "Send a case to a judge for review");
        }
        if (isFurtherEvidenceActionOptionValid(caseData.getFurtherEvidenceAction(), SEND_TO_INTERLOC_REVIEW_BY_TCW)) {
            setSelectWhoReviewsCaseField(caseData, REVIEW_BY_TCW);
            return setInterlocReviewStateFieldAndTriggerEvent(caseData, callback.getCaseDetails().getId(),
                REVIEW_BY_TCW.getId(), SEND_TO_INTERLOC_REVIEW_BY_TCW,
                EventType.VALID_SEND_TO_INTERLOC, "Send a case to a judge for review");
        }
        if (isFurtherEvidenceActionOptionValid(caseData.getFurtherEvidenceAction(), OTHER_DOCUMENT_MANUAL)
                && !CollectionUtils.isEmpty(caseData.getSscsDocument())
                && caseData.getSscsDocument().stream().filter(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType())).count() > 0) {
            return setMakeCaseUrgentTriggerEvent(caseData, callback.getCaseDetails().getId(),
                    OTHER_DOCUMENT_MANUAL, EventType.MAKE_CASE_URGENT, "Send a case to urgent hearing");
        }
        return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
            EventType.ISSUE_FURTHER_EVIDENCE.getCcdType(), "Issue to all parties",
            "Issue to all parties", idamService.getIdamTokens());
    }

    private void setSelectWhoReviewsCaseField(SscsCaseData caseData, InterlocReviewState reviewByWhom) {
        DynamicListItem reviewByJudgeItem = new DynamicListItem(reviewByWhom.getId(), null);
        caseData.setSelectWhoReviewsCase(new DynamicList(reviewByJudgeItem, null));
    }

    private SscsCaseDetails setInterlocReviewStateFieldAndTriggerEvent(
        SscsCaseData caseData, Long caseId,
        String interlocReviewState,
        FurtherEvidenceActionDynamicListItems interlocType, EventType eventType, String summary) {
        caseData.setInterlocReviewState(interlocReviewState);
        return ccdService.updateCase(caseData, caseId,
            eventType.getCcdType(), summary,
            interlocType.getLabel(), idamService.getIdamTokens());
    }

    private SscsCaseDetails setMakeCaseUrgentTriggerEvent(
            SscsCaseData caseData, Long caseId,
            FurtherEvidenceActionDynamicListItems interlocType, EventType eventType, String summary) {
        return ccdService.updateCase(caseData, caseId,
                eventType.getCcdType(), summary,
                interlocType.getLabel(), idamService.getIdamTokens());
    }

}
