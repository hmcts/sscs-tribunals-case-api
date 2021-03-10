package uk.gov.hmcts.reform.sscs.ccd.presubmit.addnote;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class AddNoteAboutToSubmitHandler  implements PreSubmitCallbackHandler<SscsCaseData> {

    protected final IdamClient idamClient;

    @Autowired
    public AddNoteAboutToSubmitHandler(IdamClient idamClient) {
        this.idamClient = idamClient;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ADD_NOTE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        String note = sscsCaseData.getTempNoteDetail();
        Note newNote = Note.builder().value(NoteDetails.builder().noteDetail(note).noteDate(LocalDate.now().toString())
                .author(buildLoggedInUserName(userAuthorisation)).build()).build();
        if (sscsCaseData.getAppealNotePad() == null || sscsCaseData.getAppealNotePad().getNotesCollection() == null) {
            sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(new ArrayList<Note>()).build());
        }
        sscsCaseData.getAppealNotePad().getNotesCollection().add(newNote);

        sscsCaseData.setTempNoteDetail(null);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }


    protected String buildLoggedInUserName(String userAuthorisation) {
        UserDetails userDetails = idamClient.getUserDetails(userAuthorisation);
        if (userDetails == null) {
            throw new IllegalStateException("Unable to obtain signed in user details");
        }
        return userDetails.getFullName();
    }
}
