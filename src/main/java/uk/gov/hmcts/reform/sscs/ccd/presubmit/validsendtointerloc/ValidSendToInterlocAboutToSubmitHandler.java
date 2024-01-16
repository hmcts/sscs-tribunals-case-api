package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty.REP;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.REPRESENTATIVE;

import java.time.LocalDate;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;
import uk.gov.hmcts.reform.sscs.service.PostponementRequestService;

@Component
@Slf4j
public class ValidSendToInterlocAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PostponementRequestService postponementRequestService;

    @Autowired
    public ValidSendToInterlocAboutToSubmitHandler(PostponementRequestService postponementRequestService) {
        this.postponementRequestService = postponementRequestService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && (callback.getEvent() == EventType.VALID_SEND_TO_INTERLOC
                || callback.getEvent() == EventType.ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        if (isDynamicListEmpty(sscsCaseData.getSelectWhoReviewsCase())) {
            PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
            preSubmitCallbackResponse.addError("Must select who reviews the appeal.");
            return preSubmitCallbackResponse;
        }

        return processSendToInterloc(callback, sscsCaseData);
    }

    private PreSubmitCallbackResponse<SscsCaseData> processSendToInterloc(Callback<SscsCaseData> callback, SscsCaseData sscsCaseData) {
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
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
            postponementRequestService.processPostponementRequest(sscsCaseData, uploadParty);
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
        return preSubmitCallbackResponse;
    }

    private boolean isPostponementRequestInterlocSendToTcw(SscsCaseData sscsCaseData) {
        return SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId().equals(sscsCaseData.getSelectWhoReviewsCase().getValue().getCode());
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
