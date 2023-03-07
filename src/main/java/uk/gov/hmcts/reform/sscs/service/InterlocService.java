package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty.REP;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.REPRESENTATIVE;

import java.time.LocalDate;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;

@Service
@Slf4j
public class InterlocService {

    private final PostponementRequestService postponementRequestService;

    @Autowired
    public InterlocService(PostponementRequestService postponementRequestService) {
        this.postponementRequestService = postponementRequestService;
    }

    public PreSubmitCallbackResponse<SscsCaseData> processSendToInterloc(Callback<SscsCaseData> callback, SscsCaseData sscsCaseData) {
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
