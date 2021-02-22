package uk.gov.hmcts.reform.sscs.ccd.presubmit.addnote;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.subtract;
import static org.apache.commons.compress.utils.Lists.newArrayList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class AddNoteAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final List<EventType> EVENTS_WITH_NOTES = asList(EventType.ADD_NOTE,
            EventType.INTERLOC_SEND_TO_TCW, EventType.ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE,
            EventType.SEND_TO_ADMIN, EventType.TCW_REFER_TO_JUDGE, EventType.NON_COMPLIANT_SEND_TO_INTERLOC,
            EventType.ADMIN_APPEAL_WITHDRAWN);

    protected final IdamClient idamClient;

    @Autowired
    public AddNoteAboutToSubmitHandler(IdamClient idamClient) {
        this.idamClient = idamClient;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT) && EVENTS_WITH_NOTES.contains(callback.getEvent());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        final List<Note> copyOfCurrentNotes = new ArrayList<>(ofNullable(sscsCaseData.getAppealNotePad()).map(NotePad::getNotesCollection).orElse(newArrayList()));
        final List<Note> oldNotes = callback.getCaseDetailsBefore().filter(f -> f.getCaseData().getAppealNotePad() != null).map(f -> f.getCaseData().getAppealNotePad().getNotesCollection()).orElse(newArrayList());
        final List<Note> editedNotes = subtract(copyOfCurrentNotes, oldNotes);
        if (isNotEmpty(copyOfCurrentNotes)) {
            setAppealNotePad(sscsCaseData, editedNotes, buildLoggedInUserName(userAuthorisation));
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setAppealNotePad(final SscsCaseData sscsCaseData, final List<Note> editedNotes, final String loggedInUserName) {
        List<Note> newNotePad = sscsCaseData.getAppealNotePad().getNotesCollection().stream().map(note -> {
            if (editedNotes.contains(note)) {
                String date = note.getValue().getNoteDate();
                if (StringUtils.isEmpty(date)) {
                    date = LocalDate.now().toString();
                }
                return note.toBuilder().value(note.getValue().toBuilder().noteDate(date).userName(loggedInUserName).build()).build();
            }
            return note;
        }).collect(toList());
        sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(newNotePad).build());
    }

    protected String buildLoggedInUserName(String userAuthorisation) {
        UserDetails userDetails = idamClient.getUserDetails(userAuthorisation);
        if (userDetails == null) {
            throw new IllegalStateException("Unable to obtain signed in user details");
        }
        return userDetails.getFullName();
    }
}
