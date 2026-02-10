package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.validsendtointerloc;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_SEND_TO_INTERLOC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty.REP;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.REPRESENTATIVE;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.SelectWhoReviewsCase;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.PostponementRequestService;

@Component
@Slf4j
public class ValidSendToInterlocAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PostponementRequestService postponementRequestService;
    private final AddNoteService addNoteService;


    @Autowired
    public ValidSendToInterlocAboutToSubmitHandler(PostponementRequestService postponementRequestService,
                                                   AddNoteService addNoteService) {
        this.postponementRequestService = postponementRequestService;
        this.addNoteService = addNoteService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(ABOUT_TO_SUBMIT)
            && (callback.getEvent() == VALID_SEND_TO_INTERLOC
                || callback.getEvent() == ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (isDynamicListEmpty(sscsCaseData.getSelectWhoReviewsCase())) {
            PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
            preSubmitCallbackResponse.addError("Must select who reviews the appeal.");
            return preSubmitCallbackResponse;
        }
        return processSendToInterloc(callback, sscsCaseData, userAuthorisation);
    }

    private PreSubmitCallbackResponse<SscsCaseData> processSendToInterloc(Callback<SscsCaseData> callback,
                                                                          SscsCaseData sscsCaseData, String userAuth) {
        var preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (isPostponementRequestInterlocSendToTcw(sscsCaseData)) {
            if (isDynamicListEmpty(sscsCaseData.getOriginalSender())) {
                preSubmitCallbackResponse.addError("Must select original sender");
                return preSubmitCallbackResponse;
            }
            if (!callback.isIgnoreWarnings()) {
                preSubmitCallbackResponse.addWarning("Are you sure you want to postpone the hearing?");
                return preSubmitCallbackResponse;
            }
            UploadParty uploadParty = getUploadParty(sscsCaseData.getOriginalSender());
            postponementRequestService.processPostponementRequest(sscsCaseData, uploadParty, Optional.empty());
        } else {
            InterlocReviewState interlocState = Arrays.stream(InterlocReviewState.values())
                .filter(x -> x.getCcdDefinition().equals(sscsCaseData.getSelectWhoReviewsCase().getValue().getCode()))
                .findFirst()
                .orElse(null);
            sscsCaseData.setInterlocReviewState(interlocState);
        }
        sscsCaseData.setSelectWhoReviewsCase(null);
        log.info("Setting interloc referral date to {}  for caseId {}", LocalDate.now(), sscsCaseData.getCcdCaseId());
        sscsCaseData.setInterlocReferralDate(LocalDate.now());
        sscsCaseData.setDirectionDueDate(null);
        addNoteService.addNote(userAuth, sscsCaseData, sscsCaseData.getTempNoteDetail());
        return preSubmitCallbackResponse;
    }

    private boolean isPostponementRequestInterlocSendToTcw(SscsCaseData sscsCaseData) {
        return SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId()
                .equals(sscsCaseData.getSelectWhoReviewsCase().getValue().getCode());
    }

    private UploadParty getUploadParty(DynamicList originalSender) {
        return REPRESENTATIVE.getCode().equals(originalSender.getValue().getCode())
                ? REP : UploadParty.fromValue(originalSender.getValue().getCode());
    }

    private boolean isDynamicListEmpty(DynamicList originalSender) {
        return originalSender == null
                || originalSender.getValue() == null
                || originalSender.getValue().getCode() == null;
    }

}
