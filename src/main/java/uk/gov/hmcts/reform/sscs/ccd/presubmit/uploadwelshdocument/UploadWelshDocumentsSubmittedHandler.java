package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REINSTATEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.URGENT_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
@Slf4j
public class UploadWelshDocumentsSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    public UploadWelshDocumentsSubmittedHandler(CcdService ccdService, IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent().equals(EventType.UPLOAD_WELSH_DOCUMENT)
                && !callback.getCaseDetails().getState().equals(State.INTERLOCUTORY_REVIEW_STATE)
                && StringUtils.isNotEmpty(callback.getCaseDetails().getCaseData().getSscsWelshPreviewNextEvent());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        String nextEvent = callback.getCaseDetails().getCaseData().getSscsWelshPreviewNextEvent();
        log.info("Next event to submit  {}", nextEvent);

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        sscsCaseData.setSscsWelshPreviewNextEvent(null);

        if (isValidUrgentHearingDocument(sscsCaseData)) {
            setMakeCaseUrgentTriggerEvent(sscsCaseData, callback.getCaseDetails().getId(),
                    OTHER_DOCUMENT_MANUAL, EventType.MAKE_CASE_URGENT, "Send a case to urgent hearing");
        } else if (isReinstatementRequest(sscsCaseData)) {
            sscsCaseData = setReinstatementRequest(sscsCaseData, callback.getCaseDetails().getId(), nextEvent);
        } else {
            log.info("Submitting Next Event {}", nextEvent);
            ccdService.updateCase(sscsCaseData, callback.getCaseDetails().getId(),
                    nextEvent, "Upload welsh document",
                    "Upload welsh document", idamService.getIdamTokens());
        }
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

    private SscsCaseDetails setMakeCaseUrgentTriggerEvent(
            SscsCaseData caseData, Long caseId,
            FurtherEvidenceActionDynamicListItems interlocType, EventType eventType, String summary) {
        return ccdService.updateCase(caseData, caseId,
                eventType.getCcdType(), summary,
                interlocType.getLabel(), idamService.getIdamTokens());
    }

    private SscsCaseData setReinstatementRequest(SscsCaseData sscsCaseData, Long caseId, String nextEvent) {

        log.info("Setting Reinstatement Request for Welsh Case {}", caseId);

        sscsCaseData.setReinstatementRegistered(LocalDate.now());
        sscsCaseData.setReinstatementOutcome(RequestOutcome.IN_PROGRESS);

        State previousState = sscsCaseData.getPreviousState();

        if (previousState != null
            && (previousState.equals(State.DORMANT_APPEAL_STATE) || previousState.equals(State.VOID_STATE))) {
            sscsCaseData.setPreviousState(State.INTERLOCUTORY_REVIEW_STATE);
            log.info("{} setting previousState from {} to interlocutoryReviewState", sscsCaseData.getCcdCaseId(), previousState.getId());
        }

        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);

        ccdService.updateCase(sscsCaseData, caseId, nextEvent, "Upload Welsh Document",
                "Upload Welsh Document", idamService.getIdamTokens());

        return sscsCaseData;
    }
}
