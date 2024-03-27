package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.URGENT_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.*;

import java.time.LocalDate;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RequiredArgsConstructor
@Service
@Slf4j
public class ActionFurtherEvidenceSubmittedCallbackHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String TCW_REVIEW_POSTPONEMENT_REQUEST = "Review hearing postponement request";
    public static final String TCW_REVIEW_SEND_TO_JUDGE = "Send a case to a judge for review";

    private final CcdService ccdService;

    private final UpdateCcdCaseService updateCcdCaseService;

    private final CcdClient ccdClient;

    private final SscsCcdConvertService sscsCcdConvertService;

    private final IdamService idamService;

    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Value("${feature.postHearingsB.enabled}")
    private final boolean isPostHearingsBEnabled;

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
        return isFurtherEvidenceActionOptionValid(furtherEvidenceAction, ADMIN_ACTION_CORRECTION)
                || isFurtherEvidenceActionOptionValid(furtherEvidenceAction, INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE)
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
        SscsCaseDetails sscsCaseDetails = updateCase(callback);

        return new PreSubmitCallbackResponse<>(sscsCaseDetails.getData());
    }

    private SscsCaseDetails updateCase(Callback<SscsCaseData> callback) {
        IdamTokens idamTokens = idamService.getIdamTokens();
        StartEventResponse startEventResponse = ccdClient.startEvent(idamTokens, callback.getCaseDetails().getId(), EventType.UPDATE_CASE_ONLY.getCcdType());
        var caseData = sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData());

        DynamicList furtherEvidenceAction = caseData.getFurtherEvidenceAction();

        if (isPostHearingsEnabled && isFurtherEvidenceActionOptionValid(furtherEvidenceAction, ADMIN_ACTION_CORRECTION)) {
            // TODO 10581 navigate user to Admin correction screen
            return setInterlocReviewStateFieldAndTriggerEvent(
                    sscsCaseData -> {
                    },
                    callback.getCaseDetails().getId(),
                    AWAITING_ADMIN_ACTION, ADMIN_ACTION_CORRECTION,
                    EventType.CORRECTION_REQUEST, "Admin action correction");
        }
        if (isFurtherEvidenceActionOptionValid(furtherEvidenceAction, INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE)) {

            return setInterlocReviewStateFieldAndTriggerEvent(sscsCaseData -> sscsCaseData.setInterlocReferralDate(LocalDate.now()),
                    callback.getCaseDetails().getId(),
                    REVIEW_BY_JUDGE, INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,
                    EventType.INTERLOC_INFORMATION_RECEIVED_ACTION_FURTHER_EVIDENCE, "Interloc information received event");
        }
        if (isFurtherEvidenceActionOptionValid(furtherEvidenceAction, INFORMATION_RECEIVED_FOR_INTERLOC_TCW)) {

            return setInterlocReviewStateFieldAndTriggerEvent(sscsCaseData -> sscsCaseData.setInterlocReferralDate(LocalDate.now()),
                    callback.getCaseDetails().getId(),
                    REVIEW_BY_TCW, INFORMATION_RECEIVED_FOR_INTERLOC_TCW,
                    EventType.INTERLOC_INFORMATION_RECEIVED_ACTION_FURTHER_EVIDENCE, "Interloc information received event");
        }
        if (isFurtherEvidenceActionOptionValid(furtherEvidenceAction, SEND_TO_INTERLOC_REVIEW_BY_JUDGE)) {
            PostHearingRequestType postHearingRequestType = caseData.getPostHearing().getRequestType();

            if (isPostHearingsEnabled && nonNull(postHearingRequestType)) {
                return handlePostHearing(callback, caseData, postHearingRequestType);
            }

            if (isPostHearingsBEnabled && isPostHearingOtherRequest(caseData)) {
                log.info("Updating case using updateCaseV2 to trigger '{}' for caseId: {}, interlocReviewState: {}",
                        EventType.POST_HEARING_OTHER.getCcdType(),
                        callback.getCaseDetails().getId(),
                        REVIEW_BY_JUDGE.getCcdDefinition());
                return updateCcdCaseService.updateCaseV2(callback.getCaseDetails().getId(),
                        EventType.POST_HEARING_OTHER.getCcdType(), "Post hearing application 'Other'",
                        "Post hearing application 'Other'", idamService.getIdamTokens(),
                        sscsCaseData -> sscsCaseData.setInterlocReviewState(REVIEW_BY_JUDGE)
                );
            }

            return setInterlocReviewStateFieldAndTriggerEvent(sscsCaseData -> setSelectWhoReviewsCaseField(sscsCaseData, REVIEW_BY_JUDGE),
                    callback.getCaseDetails().getId(),
                    REVIEW_BY_JUDGE, SEND_TO_INTERLOC_REVIEW_BY_JUDGE,
                    EventType.VALID_SEND_TO_INTERLOC, TCW_REVIEW_SEND_TO_JUDGE);
        }
        if (isFurtherEvidenceActionOptionValid(furtherEvidenceAction, SEND_TO_INTERLOC_REVIEW_BY_TCW)) {
            Consumer<SscsCaseData> caseDataConsumer = sscsCaseData -> {
                setSelectWhoReviewsCaseField(sscsCaseData, REVIEW_BY_TCW);
                if (isPostponementRequest(sscsCaseData)) {
                    sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST);
                }
            };

            return setInterlocReviewStateFieldAndTriggerEvent(caseDataConsumer, callback.getCaseDetails().getId(),
                    REVIEW_BY_TCW, SEND_TO_INTERLOC_REVIEW_BY_TCW, EventType.VALID_SEND_TO_INTERLOC,
                    TCW_REVIEW_SEND_TO_JUDGE);
        }
        if (isFurtherEvidenceActionOptionValid(furtherEvidenceAction, OTHER_DOCUMENT_MANUAL)
                && isValidUrgentDocument(caseData)) {
            return setMakeCaseUrgentTriggerEvent(callback.getCaseDetails().getId(),
                    OTHER_DOCUMENT_MANUAL, EventType.MAKE_CASE_URGENT, "Send a case to urgent hearing");
        }
        if (isFurtherEvidenceActionOptionValid(furtherEvidenceAction, OTHER_DOCUMENT_MANUAL)) {
            log.info("Updating case using triggerCaseEventV2 for event {}, caseId {}",
                    EventType.ISSUE_FURTHER_EVIDENCE.getCcdType(),
                    callback.getCaseDetails().getId());
            return updateCcdCaseService.triggerCaseEventV2(callback.getCaseDetails().getId(),
                    EventType.ISSUE_FURTHER_EVIDENCE.getCcdType(), "Actioned manually",
                    "Actioned manually", idamService.getIdamTokens());
        }

        log.info("Updating case using triggerCaseEventV2 for event: {}, event description: {}, caseId: {}",
                EventType.ISSUE_FURTHER_EVIDENCE.getCcdType(),
                "Issue to all parties",
                callback.getCaseDetails().getId());
        return updateCcdCaseService.triggerCaseEventV2(callback.getCaseDetails().getId(),
                EventType.ISSUE_FURTHER_EVIDENCE.getCcdType(), "Issue to all parties",
                "Issue to all parties", idamService.getIdamTokens());
    }

    private SscsCaseDetails handlePostHearing(Callback<SscsCaseData> callback, SscsCaseData caseData, PostHearingRequestType postHearingRequestType) {
        switch (postHearingRequestType) {
            case SET_ASIDE -> {

                return setInterlocReviewStateFieldAndTriggerEvent(
                        sscsCaseData -> {
                        },
                        callback.getCaseDetails().getId(),
                        REVIEW_BY_JUDGE, SEND_TO_INTERLOC_REVIEW_BY_JUDGE,
                        EventType.SET_ASIDE_REQUEST, "Set aside request");
            }
            case CORRECTION -> {

                return setInterlocReviewStateFieldAndTriggerEvent(
                        sscsCaseData -> {
                        },
                        callback.getCaseDetails().getId(),
                        REVIEW_BY_JUDGE, SEND_TO_INTERLOC_REVIEW_BY_JUDGE,
                        EventType.CORRECTION_REQUEST, "Correction request");
            }
            case STATEMENT_OF_REASONS -> {

                return setInterlocReviewStateFieldAndTriggerEvent(
                        sscsCaseData -> {
                        },
                        callback.getCaseDetails().getId(),
                        REVIEW_BY_JUDGE, SEND_TO_INTERLOC_REVIEW_BY_JUDGE,
                        EventType.SOR_REQUEST, "Statement of reasons request");
            }
            case LIBERTY_TO_APPLY -> {

                if (isPostHearingsBEnabled) {
                    return setInterlocReviewStateFieldAndTriggerEvent(
                            sscsCaseData -> {
                            },
                            callback.getCaseDetails().getId(),
                            REVIEW_BY_JUDGE, SEND_TO_INTERLOC_REVIEW_BY_JUDGE,
                            EventType.LIBERTY_TO_APPLY_REQUEST, "Liberty to apply request");
                }
                throw new IllegalStateException("Post hearings B is not enabled");
            }
            case PERMISSION_TO_APPEAL -> {

                if (isPostHearingsBEnabled) {
                    return setInterlocReviewStateFieldAndTriggerEvent(
                            sscsCaseData -> {
                            },
                            callback.getCaseDetails().getId(),
                            REVIEW_BY_JUDGE, SEND_TO_INTERLOC_REVIEW_BY_JUDGE,
                            EventType.PERMISSION_TO_APPEAL_REQUEST, "Permission to appeal request");
                }
                throw new IllegalStateException("Post hearings B is not enabled");
            }
            default ->
                    throw new IllegalArgumentException("Post hearing request type is not implemented or recognised: " + postHearingRequestType);
        }
    }

    private boolean isPostponementRequest(SscsCaseData caseData) {
        return emptyIfNull(caseData.getSscsDocument()).stream()
                .anyMatch(document -> document.getValue().getDocumentType() != null
                        && document.getValue().getDocumentType().equals(DocumentType.POSTPONEMENT_REQUEST
                        .getValue()));
    }

    private boolean isPostHearingOtherRequest(SscsCaseData caseData) {
        return emptyIfNull(caseData.getSscsDocument()).stream()
                .anyMatch(document -> document.getValue().getDocumentType() != null
                        && document.getValue().getDocumentType().equals(DocumentType.POST_HEARING_OTHER
                        .getValue()));
    }

    private boolean isValidUrgentDocument(SscsCaseData caseData) {
        return ((StringUtils.isEmpty(caseData.getUrgentCase()) || "No".equalsIgnoreCase(caseData.getUrgentCase()))
                && (StringUtils.isEmpty(caseData.getTranslationWorkOutstanding()) || "No".equalsIgnoreCase(caseData.getTranslationWorkOutstanding()))
                && !CollectionUtils.isEmpty(caseData.getSscsDocument())
                && caseData.getSscsDocument().stream().filter(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType())).count() > 0);
    }

    private void setSelectWhoReviewsCaseField(SscsCaseData caseData, InterlocReviewState reviewByWhom) {
        DynamicListItem reviewByJudgeItem = new DynamicListItem(reviewByWhom.getCcdDefinition(), null);
        caseData.setSelectWhoReviewsCase(new DynamicList(reviewByJudgeItem, null));
    }

    private SscsCaseDetails setInterlocReviewStateFieldAndTriggerEvent(
            Consumer<SscsCaseData> sscsCaseDataConsumer,
            Long caseId,
            InterlocReviewState interlocReviewState,
            FurtherEvidenceActionDynamicListItems interlocType,
            EventType eventType,
            String summary) {

        log.info(
                "Updating case using updateCaseV2 to trigger '{}' for caseId: {}, "
                        + "interlocReviewState: {}, interlocType: {}",
                eventType.getCcdType(), caseId, interlocReviewState.getCcdDefinition(),
                interlocType.getLabel());
        return updateCcdCaseService.updateCaseV2(
                caseId,
                eventType.getCcdType(),
                summary,
                interlocType.getLabel(),
                idamService.getIdamTokens(),
                sscsCaseDataConsumer.andThen(
                        sscsCaseData -> sscsCaseData.setInterlocReviewState(interlocReviewState)
                )
        );
    }

    private SscsCaseDetails setMakeCaseUrgentTriggerEvent(
            Long caseId,
            FurtherEvidenceActionDynamicListItems interlocType, EventType eventType, String summary) {
        log.info("Updating case using updateCaseV2 to trigger '{}' for caseId {}", eventType.getCcdType(), caseId);
        return updateCcdCaseService.triggerCaseEventV2(
                caseId,
                eventType.getCcdType(), summary,
                interlocType.getLabel(), idamService.getIdamTokens()
        );
    }

}
