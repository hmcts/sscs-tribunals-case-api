package uk.gov.hmcts.reform.sscs.ccd.presubmit.informationreceived;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.findLabelById;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;

@Component
@Slf4j
public class InformationReceivedAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public AddNoteService addNoteService;

    @Autowired
    public InformationReceivedAboutToSubmitHandler(AddNoteService addNoteService) {
        this.addNoteService = addNoteService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.INTERLOC_INFORMATION_RECEIVED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        sscsCaseData.setInterlocReferralDate(LocalDate.now().toString());

        String interlocReviewState = sscsCaseData.getInterlocReviewState();


        if (interlocReviewState.equals(InterlocReviewState.REVIEW_BY_JUDGE.getId())
                || interlocReviewState.equals(InterlocReviewState.REVIEW_BY_TCW.getId())) {

            String finalNote;

            if (interlocReviewState.equals(InterlocReviewState.REVIEW_BY_JUDGE.getId())) {
                finalNote = "Referred to interloc for review by judge – " + findLabelById(sscsCaseData.getInterlocReferralReason());
            } else {
                finalNote = "Referred to interloc for review by TCW – " + findLabelById(sscsCaseData.getInterlocReferralReason());
            }

            String tempNote = sscsCaseData.getTempNoteDetail();

            if (StringUtils.isNotEmpty(tempNote)) {
                finalNote = finalNote + " - " + tempNote;
            }

            addNoteService.addNote(userAuthorisation, sscsCaseData, finalNote);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
