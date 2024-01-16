package uk.gov.hmcts.reform.sscs.ccd.presubmit.addnote;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;

@Component
@Slf4j
public class AddNoteAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    protected final AddNoteService addNoteService;

    @Autowired
    public AddNoteAboutToSubmitHandler(AddNoteService addNoteService) {
        this.addNoteService = addNoteService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && (callback.getEvent() == EventType.ADD_NOTE
                || callback.getEvent() == EventType.NON_COMPLIANT_SEND_TO_INTERLOC
                || callback.getEvent() == EventType.ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE
                || callback.getEvent() == EventType.INTERLOC_SEND_TO_TCW
                || callback.getEvent() == EventType.TCW_REFER_TO_JUDGE
                || callback.getEvent() == EventType.SEND_TO_ADMIN
                || callback.getEvent() == EventType.ADMIN_APPEAL_WITHDRAWN
                || callback.getEvent() == EventType.HMCTS_RESPONSE_REVIEWED);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        String note = sscsCaseData.getTempNoteDetail();

        if (callback.getEvent() == EventType.HMCTS_RESPONSE_REVIEWED && nonNull(sscsCaseData.getInterlocReferralReason())
                && sscsCaseData.getInterlocReferralReason() != InterlocReferralReason.NONE
                && nonNull(sscsCaseData.getSelectWhoReviewsCase())) {

            String reasonLabel = sscsCaseData.getInterlocReferralReason().getDescription();

            log.info("Add note details for case id {} - select who reviews case: {}, interloc referral reason: {}",
                    sscsCaseData.getCcdCaseId(), sscsCaseData.getSelectWhoReviewsCase(), sscsCaseData.getInterlocReferralReason());

            String whoReviewsCaseCode = sscsCaseData.getSelectWhoReviewsCase().getValue().getCode();

            Optional<String> whoReviewsCaseLabel = Arrays.stream(SelectWhoReviewsCase.values())
                    .filter(selectWhoReviewsCase -> selectWhoReviewsCase.getId().equals(whoReviewsCaseCode))
                    .map(selectWhoReviewsCase -> selectWhoReviewsCase.getLabel().toLowerCase())
                    .findFirst();

            if (nonNull(note) && StringUtils.isNoneBlank(note)) {
                note = "Referred to interloc for " + whoReviewsCaseLabel.orElse("") + " - " + reasonLabel + " - " + note;
            } else {
                note = "Referred to interloc for " + whoReviewsCaseLabel.orElse("") + " - " + reasonLabel;
            }
        }

        addNoteService.addNote(userAuthorisation, sscsCaseData, note);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

}
