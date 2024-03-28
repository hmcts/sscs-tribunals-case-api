package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REINSTATEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.URGENT_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDate;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class UploadWelshDocumentsSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final IdamService idamService;
    private final CcdClient ccdClient;
    private final SscsCcdConvertService sscsCcdConvertService;
    private final UpdateCcdCaseService updateCcdCaseService;

    @Autowired
    public UploadWelshDocumentsSubmittedHandler(IdamService idamService,
                                                CcdClient ccdClient,
                                                SscsCcdConvertService sscsCcdConvertService,
                                                UpdateCcdCaseService updateCcdCaseService) {
        this.idamService = idamService;
        this.ccdClient = ccdClient;
        this.sscsCcdConvertService = sscsCcdConvertService;
        this.updateCcdCaseService = updateCcdCaseService;
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

        IdamTokens idamTokens = idamService.getIdamTokens();
        StartEventResponse startEventResponse = ccdClient.startEvent(
                idamTokens, callback.getCaseDetails().getId(), EventType.UPDATE_CASE_ONLY.getCcdType());
        SscsCaseData sscsCaseData = sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData());

        String nextEvent = sscsCaseData.getSscsWelshPreviewNextEvent();
        log.info("Next event to submit  {} for case reference {}", nextEvent, sscsCaseData.getCcdCaseId());

        Consumer<SscsCaseData> mutator = caseData -> caseData.setSscsWelshPreviewNextEvent(null);

        if (isValidUrgentHearingDocument(sscsCaseData)) {
            setMakeCaseUrgentTriggerEvent(callback.getCaseDetails().getId(), mutator);
        } else if (isReinstatementRequest(sscsCaseData)) {
            setReinstatementRequest(sscsCaseData, callback.getCaseDetails().getId(), nextEvent);
        } else {
            log.info("Submitting Next Event {} using updateCaseV2 for {}", nextEvent, callback.getCaseDetails().getId());
            updateCcdCaseService.updateCaseV2(
                    callback.getCaseDetails().getId(),
                    nextEvent,
                    "Upload welsh document",
                    "Upload welsh document",
                    idamService.getIdamTokens(),
                    mutator
            );
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

        log.info("Is Reinstatement Request: translationOutstanding = {}. isEngReinstatement = {}. isWelshDocReinstatement = {}",isTranslationsOutstanding, isDocReinstatement, isWelshReinstatement);
        return (isTranslationsOutstanding && (isDocReinstatement || isWelshReinstatement));
    }

    private SscsCaseDetails setMakeCaseUrgentTriggerEvent(Long caseId, Consumer<SscsCaseData> caseDataConsumer) {
        log.info("Using updateCaseV2 to update case with 'makeCaseUrgent' event for {}", caseId);
        return updateCcdCaseService.updateCaseV2(
                caseId,
                EventType.MAKE_CASE_URGENT.getCcdType(),
                "Send a case to urgent hearing",
                OTHER_DOCUMENT_MANUAL.getLabel(),
                idamService.getIdamTokens(),
                caseDataConsumer
        );
    }

    private SscsCaseData setReinstatementRequest(SscsCaseData sscsCaseData, Long caseId, String nextEvent) {

        log.info("Setting Reinstatement Request for Welsh Case {}", caseId);
        Consumer<SscsCaseData> caseDataConsumer = data -> {
            data.setSscsWelshPreviewNextEvent(null);
            data.setReinstatementRegistered(LocalDate.now());
            data.setReinstatementOutcome(RequestOutcome.IN_PROGRESS);

            State previousState = sscsCaseData.getPreviousState();

            if (previousState != null
                    && (previousState.equals(State.DORMANT_APPEAL_STATE) || previousState.equals(State.VOID_STATE))) {
                data.setPreviousState(State.INTERLOCUTORY_REVIEW_STATE);
                log.info("{} setting previousState from {} to interlocutoryReviewState", sscsCaseData.getCcdCaseId(), previousState.getId());
            }

            data.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        };

        log.info("Using updateCaseV2 to update case with '{}' event for {}", nextEvent, caseId);
        updateCcdCaseService.updateCaseV2(caseId, nextEvent, "Upload Welsh Document",
                "Upload Welsh Document", idamService.getIdamTokens(), caseDataConsumer);

        return sscsCaseData;
    }
}
