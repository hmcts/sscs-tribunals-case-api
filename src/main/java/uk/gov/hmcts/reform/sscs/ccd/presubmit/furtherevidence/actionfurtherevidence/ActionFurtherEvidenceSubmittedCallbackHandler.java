package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.URGENT_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.*;

import java.time.LocalDate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
public class ActionFurtherEvidenceSubmittedCallbackHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String TCW_REVIEW_POSTPONEMENT_REQUEST = "Review hearing postponement request";
    public static final String TCW_REVIEW_SEND_TO_JUDGE = "Send a case to a judge for review";
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
            caseData.setInterlocReferralDate(LocalDate.now().toString());
            return setInterlocReviewStateFieldAndTriggerEvent(caseData, callback.getCaseDetails().getId(),
                    REVIEW_BY_JUDGE.getId(), INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,
                    EventType.INTERLOC_INFORMATION_RECEIVED_ACTION_FURTHER_EVIDENCE, "Interloc information received event");
        }
        if (isFurtherEvidenceActionOptionValid(caseData.getFurtherEvidenceAction(),
                INFORMATION_RECEIVED_FOR_INTERLOC_TCW)) {
            caseData.setInterlocReferralDate(LocalDate.now().toString());
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
            String reason = null;
            if (emptyIfNull(caseData.getSscsDocument()).stream()
                    .anyMatch(document -> document.getValue().getDocumentType() != null
                            && document.getValue().getDocumentType().equals(DocumentType.POSTPONEMENT_REQUEST
                            .getValue()))) {
                reason = TCW_REVIEW_POSTPONEMENT_REQUEST;
            } else {
                reason = TCW_REVIEW_SEND_TO_JUDGE;
            }

            return setInterlocReviewStateFieldAndTriggerEvent(caseData, callback.getCaseDetails().getId(),
                    REVIEW_BY_TCW.getId(), SEND_TO_INTERLOC_REVIEW_BY_TCW,
                    EventType.VALID_SEND_TO_INTERLOC, reason);
        }
        if (isFurtherEvidenceActionOptionValid(caseData.getFurtherEvidenceAction(), OTHER_DOCUMENT_MANUAL)
                && isValidUrgentDocument(caseData)) {
            return setMakeCaseUrgentTriggerEvent(caseData, callback.getCaseDetails().getId(),
                    OTHER_DOCUMENT_MANUAL, EventType.MAKE_CASE_URGENT, "Send a case to urgent hearing");
        }
        if (isFurtherEvidenceActionOptionValid(caseData.getFurtherEvidenceAction(), OTHER_DOCUMENT_MANUAL)) {
            return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                    EventType.ISSUE_FURTHER_EVIDENCE.getCcdType(), "Actioned manually",
                    "Actioned manually", idamService.getIdamTokens());
        }
        return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.ISSUE_FURTHER_EVIDENCE.getCcdType(), "Issue to all parties",
                "Issue to all parties", idamService.getIdamTokens());
    }

    private boolean isValidUrgentDocument(SscsCaseData caseData) {
        return ((StringUtils.isEmpty(caseData.getUrgentCase()) || "No".equalsIgnoreCase(caseData.getUrgentCase()))
                && (StringUtils.isEmpty(caseData.getTranslationWorkOutstanding()) || "No".equalsIgnoreCase(caseData.getTranslationWorkOutstanding()))
                && !CollectionUtils.isEmpty(caseData.getSscsDocument())
                && caseData.getSscsDocument().stream().filter(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType())).count() > 0);
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
