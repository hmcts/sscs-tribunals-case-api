package uk.gov.hmcts.reform.sscs.ccd.presubmit.addnote;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.LocalDate;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Note;
import uk.gov.hmcts.reform.sscs.ccd.domain.NoteDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.NotePad;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class AddNoteAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String USER_NAME = "forename surname";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    @Mock
    private IdamClient idamClient;

    private AddNoteAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;

    private final Note existingNote = getNote("01/02/2020", "text", null);

    @Before
    public void setUp() {
        openMocks(this);
        handler = new AddNoteAboutToSubmitHandler(idamClient);

        when(callback.getEvent()).thenReturn(EventType.ADD_NOTE);
        NotePad appealNotePad = NotePad.builder().notesCollection(singletonList(existingNote)).build();
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appealNotePad(appealNotePad).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder().ccdCaseId("ccdId").appealNotePad(appealNotePad).build();
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);

        final UserDetails userDetails = UserDetails.builder().forename("forename").surname("surname").build();
        when(idamClient.getUserDetails(eq(USER_AUTHORISATION))).thenReturn(userDetails);
    }

    private Note getNote(String date, String text, String userName) {
        return Note.builder().value(NoteDetails.builder().noteDate(date).noteDetail(text).userName(userName).build()).build();
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenANonAddNoteEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenNoChangeToNotes_thenLeaveCaseUnchanged() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getAppealNotePad(), is(caseDetailsBefore.getCaseData().getAppealNotePad()));
    }

    @Test
    public void givenAnEditToAnExistingNote_thenUpdateTheNote() {
        final Note note = getNote(existingNote.getValue().getNoteDate(), "newText", "previous user");
        sscsCaseData = sscsCaseData.toBuilder().appealNotePad(NotePad.builder().notesCollection(singletonList(note)).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getAppealNotePad().getNotesCollection().size(), is(1));
        assertThat(response.getData().getAppealNotePad().getNotesCollection().get(0), is(getNote(note.getValue().getNoteDate(), note.getValue().getNoteDetail(), USER_NAME)));
    }

    @Test
    public void givenAddingANewNote_thenUpdateTheCaseWithTheNewNoteAndFillInTheLoggedInUserName() {
        final Note note = getNote("12/02/2021", "newText", null);
        sscsCaseData = sscsCaseData.toBuilder().appealNotePad(NotePad.builder().notesCollection(asList(existingNote, note)).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getAppealNotePad().getNotesCollection().size(), is(2));
        assertThat(response.getData().getAppealNotePad().getNotesCollection().get(0), is(existingNote));
        assertThat(response.getData().getAppealNotePad().getNotesCollection().get(1), is(getNote(note.getValue().getNoteDate(), note.getValue().getNoteDetail(), USER_NAME)));
    }

    @Test
    public void givenAddingANewNoteWithNoDate_thenUpdateTheCaseWithTheNewNoteAndFillInTheLoggedInUserNameAndCurrentDate() {
        final Note note = getNote(null, "newText", null);
        sscsCaseData = sscsCaseData.toBuilder().appealNotePad(NotePad.builder().notesCollection(asList(existingNote, note)).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getAppealNotePad().getNotesCollection().size(), is(2));
        assertThat(response.getData().getAppealNotePad().getNotesCollection().get(0), is(existingNote));
        assertThat(response.getData().getAppealNotePad().getNotesCollection().get(1), is(getNote(LocalDate.now().toString(), note.getValue().getNoteDetail(), USER_NAME)));
    }

    @Test
    public void givenUserRemovesTheNote_thenUpdateTheCaseWithNoNotes() {
        sscsCaseData = sscsCaseData.toBuilder().appealNotePad(NotePad.builder().notesCollection(emptyList()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getAppealNotePad().getNotesCollection().size(), is(0));
    }

}