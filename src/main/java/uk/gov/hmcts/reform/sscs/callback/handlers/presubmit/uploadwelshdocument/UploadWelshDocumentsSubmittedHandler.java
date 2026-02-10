package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.uploadwelshdocument;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REINSTATEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.URGENT_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.MAKE_CASE_URGENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDate;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
@Slf4j
public class UploadWelshDocumentsSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;


    @Autowired
    public UploadWelshDocumentsSubmittedHandler(IdamService idamService,
                                                UpdateCcdCaseService updateCcdCaseService) {
        this.idamService = idamService;
        this.updateCcdCaseService = updateCcdCaseService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent().equals(EventType.UPLOAD_WELSH_DOCUMENT)
                && !callback.getCaseDetails().getState().equals(INTERLOCUTORY_REVIEW_STATE)
                && StringUtils.isNotEmpty(callback.getCaseDetails().getCaseData().getSscsWelshPreviewNextEvent());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        String nextEvent = callback.getCaseDetails().getCaseData().getSscsWelshPreviewNextEvent();
        Consumer<SscsCaseDetails> mutator = sscsCaseDetails -> sscsCaseDetails.getData().setSscsWelshPreviewNextEvent(null);

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        String eventType = nextEvent;
        String description = "Upload Welsh document";
        String summary = "Upload Welsh document";
        Consumer<SscsCaseDetails> finalMutator = mutator;

        if (isValidUrgentHearingDocument(sscsCaseData)) {
            eventType = MAKE_CASE_URGENT.getCcdType();
            description = "Send a case to urgent hearing";
            summary = OTHER_DOCUMENT_MANUAL.getLabel();
        } else if (isReinstatementRequest(sscsCaseData)) {
            finalMutator = setReinstatementRequest(mutator);
        }

        log.info("For case reference {} next event to submit {} ", sscsCaseData.getCcdCaseId(), eventType);

        sscsCaseData = updateCcdCaseService.updateCaseV2(
                callback.getCaseDetails().getId(),
                eventType,
                description,
                summary,
                idamService.getIdamTokens(),
                finalMutator
        ).getData();

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private boolean isValidUrgentHearingDocument(SscsCaseData caseData) {
        return (!"Yes".equalsIgnoreCase(caseData.getUrgentCase())
                && (StringUtils.isEmpty(caseData.getTranslationWorkOutstanding()) || "No".equalsIgnoreCase(caseData.getTranslationWorkOutstanding()))
                && (!CollectionUtils.isEmpty(caseData.getSscsDocument()) && caseData.getSscsDocument().stream().anyMatch(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType()))
                || (!CollectionUtils.isEmpty(caseData.getSscsWelshDocuments()) && caseData.getSscsWelshDocuments().stream().anyMatch(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType())))));
    }

    private boolean isReinstatementRequest(SscsCaseData caseData) {

        Boolean isTranslationsOutstanding = (StringUtils.isEmpty(caseData.getTranslationWorkOutstanding()) || "No".equalsIgnoreCase(caseData.getTranslationWorkOutstanding()));
        Boolean isDocReinstatement = !CollectionUtils.isEmpty(caseData.getSscsDocument()) && caseData.getSscsDocument().stream().anyMatch(d -> REINSTATEMENT_REQUEST.getValue().equals(d.getValue().getDocumentType()));
        Boolean isWelshReinstatement = (!CollectionUtils.isEmpty(caseData.getSscsWelshDocuments()) && caseData.getSscsWelshDocuments().stream().anyMatch(d -> REINSTATEMENT_REQUEST.getValue().equals(d.getValue().getDocumentType())));

        log.info("Is Reinstatement Request: translationOutstanding = {}. isEngReintstatement = {}. isWelshDocReinstatement = {}",isTranslationsOutstanding, isDocReinstatement, isWelshReinstatement);
        return (isTranslationsOutstanding && (isDocReinstatement || isWelshReinstatement));
    }

    private Consumer<SscsCaseDetails> setReinstatementRequest(Consumer<SscsCaseDetails> mutator) {

        return sscsCaseDetails -> {
            mutator.accept(sscsCaseDetails);
            SscsCaseData data = sscsCaseDetails.getData();

            data.setReinstatementRegistered(LocalDate.now());
            data.setReinstatementOutcome(RequestOutcome.IN_PROGRESS);

            State previousState = data.getPreviousState();

            if (previousState != null && (previousState == DORMANT_APPEAL_STATE || previousState == VOID_STATE)) {
                data.setPreviousState(INTERLOCUTORY_REVIEW_STATE);
                log.info("{} setting previousState from {} to interlocutoryReviewState", data.getCcdCaseId(), previousState.getId());
            }
            data.setInterlocReviewState(REVIEW_BY_JUDGE);
        };
    }
}
