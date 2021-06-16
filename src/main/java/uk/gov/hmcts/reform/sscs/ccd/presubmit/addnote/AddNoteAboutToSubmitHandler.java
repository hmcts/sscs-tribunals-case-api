package uk.gov.hmcts.reform.sscs.ccd.presubmit.addnote;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.findLabelById;

import java.time.LocalDate;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@Component
@Slf4j
public class AddNoteAboutToSubmitHandler  implements PreSubmitCallbackHandler<SscsCaseData> {

    protected final UserDetailsService userDetailsService;

    @Autowired
    public AddNoteAboutToSubmitHandler(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
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
                || callback.getEvent() == EventType.HMCTS_RESPONSE_REVIEWED
                || callback.getEvent() == EventType.UPDATE_WELSH_PREFERENCE);
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
                && StringUtils.isNoneBlank(sscsCaseData.getInterlocReferralReason()) && nonNull(sscsCaseData.getSelectWhoReviewsCase())) {
            String reasonLabel = findLabelById(sscsCaseData.getInterlocReferralReason());
            if (nonNull(note) && StringUtils.isNoneBlank(note)) {
                note = "Referred to interloc for " + sscsCaseData.getSelectWhoReviewsCase().getValue().getLabel().toLowerCase() + " - " + reasonLabel + " - " + note;
            } else {
                note = "Referred to interloc for " + sscsCaseData.getSelectWhoReviewsCase().getValue().getLabel().toLowerCase() + " - " + reasonLabel;
            }
        }

        if (nonNull(note) && StringUtils.isNoneBlank(note)) {
            Note newNote = Note.builder().value(NoteDetails.builder().noteDetail(note).noteDate(LocalDate.now().toString())
                    .author(userDetailsService.buildLoggedInUserName(userAuthorisation)).build()).build();
            if (sscsCaseData.getAppealNotePad() == null || sscsCaseData.getAppealNotePad().getNotesCollection() == null) {
                sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(new ArrayList<Note>()).build());
            }
            sscsCaseData.getAppealNotePad().getNotesCollection().add(newNote);
            sscsCaseData.setTempNoteDetail(null);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

}
