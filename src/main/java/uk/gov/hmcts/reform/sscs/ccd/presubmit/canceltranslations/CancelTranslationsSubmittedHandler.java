package uk.gov.hmcts.reform.sscs.ccd.presubmit.canceltranslations;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REINSTATEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.URGENT_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDate;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class CancelTranslationsSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;
    private final CcdClient ccdClient;
    private final SscsCcdConvertService sscsCcdConvertService;

    public CancelTranslationsSubmittedHandler(IdamService idamService,
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
            && callback.getEvent().equals(EventType.CANCEL_TRANSLATIONS)
            && !callback.getCaseDetails().getState().equals(State.INTERLOCUTORY_REVIEW_STATE)
            && StringUtils.isNotEmpty(callback.getCaseDetails().getCaseData().getSscsWelshPreviewNextEvent());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        IdamTokens idamTokens = idamService.getIdamTokens();
        StartEventResponse startEventResponse = ccdClient.startEvent(
                idamTokens, callback.getCaseDetails().getId(), EventType.UPDATE_CASE_ONLY.getCcdType());
        SscsCaseData caseData = sscsCcdConvertService.getCaseData(startEventResponse.getCaseDetails().getData());

        String sscsWelshPreviewNextEvent = caseData.getSscsWelshPreviewNextEvent();
        log.info("sscsWelshPreviewNextEvent is {}  for case id : {}",
                sscsWelshPreviewNextEvent, caseData.getCcdCaseId());

        Consumer<SscsCaseData> caseDataConsumer = sscsCaseData -> sscsCaseData.setSscsWelshPreviewNextEvent(null);

        if (isValidUrgentDocument(callback.getCaseDetails().getCaseData())) {
            setMakeCaseUrgentTriggerEvent(callback.getCaseDetails().getId(), caseDataConsumer);

        } else if (isValidResinstatementRequestDocument(callback.getCaseDetails().getCaseData())) {

            updateForReinstatementRequestEvent(callback.getCaseDetails().getId(), sscsWelshPreviewNextEvent);

        } else {
            log.info("Using updateCaseV2 to trigger SscsWelshPreviewNextEvent '{}' for {}",
                    sscsWelshPreviewNextEvent, callback.getCaseDetails().getId());

            updateCcdCaseService.updateCaseV2(
                    callback.getCaseDetails().getId(),
                    sscsWelshPreviewNextEvent,
                    "Cancel welsh translations",
                    "Cancel welsh translations",
                    idamService.getIdamTokens(),
                    sscsCaseData -> sscsCaseData.setSscsWelshPreviewNextEvent(null));
        }

        return new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
    }

    private boolean isValidUrgentDocument(SscsCaseData caseData) {
        return (!"Yes".equalsIgnoreCase(caseData.getUrgentCase())
                && (StringUtils.isEmpty(caseData.getTranslationWorkOutstanding()) || "No".equalsIgnoreCase(caseData.getTranslationWorkOutstanding()))
                && (!CollectionUtils.isEmpty(caseData.getSscsDocument()) && caseData.getSscsDocument().stream().anyMatch(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType()))));
    }

    private boolean isValidResinstatementRequestDocument(SscsCaseData caseData) {
        return (StringUtils.isEmpty(caseData.getTranslationWorkOutstanding()) || "No".equalsIgnoreCase(caseData.getTranslationWorkOutstanding()))
                && caseData.getReinstatementOutcome() == null
                && (!CollectionUtils.isEmpty(caseData.getSscsDocument()) && caseData.getSscsDocument().stream().anyMatch(d -> REINSTATEMENT_REQUEST.getValue().equals(d.getValue().getDocumentType())));
    }

    private SscsCaseDetails setMakeCaseUrgentTriggerEvent(Long caseId, Consumer<SscsCaseData> caseDataConsumer) {
        log.info("Using updateCaseV2 to trigger 'makeCaseUrgent' event for {}", caseId);
        return updateCcdCaseService.updateCaseV2(
                caseId,
                EventType.MAKE_CASE_URGENT.getCcdType(),
                "Send a case to urgent hearing",
                OTHER_DOCUMENT_MANUAL.getLabel(),
                idamService.getIdamTokens(),
                caseDataConsumer
        );
    }

    private SscsCaseDetails updateForReinstatementRequestEvent(Long caseId, String sscsWelshPreviewNextEvent) {
        log.info("Using updateCaseV2 to trigger SscsWelshPreviewNextEvent '{}' for {}",
                sscsWelshPreviewNextEvent, caseId);

        Consumer<SscsCaseData> mutator = caseData -> {
            caseData.setSscsWelshPreviewNextEvent(null);
            caseData.setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
            caseData.setReinstatementRegistered(LocalDate.now());
            caseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        };

        return updateCcdCaseService.updateCaseV2(
            caseId,
            sscsWelshPreviewNextEvent,
            "Set Reinstatement Request",
            "Set Reinstatement Request",
            idamService.getIdamTokens(),
            mutator);
    }
}
