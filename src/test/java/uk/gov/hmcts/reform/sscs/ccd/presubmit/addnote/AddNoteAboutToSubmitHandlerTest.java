package uk.gov.hmcts.reform.sscs.ccd.presubmit.addnote;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.LocalDate;
import java.util.ArrayList;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class AddNoteAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private AddNoteAboutToSubmitHandler handler;

    @Mock
    private IdamClient idamClient;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new AddNoteAboutToSubmitHandler(idamClient);

        when(callback.getEvent()).thenReturn(EventType.ADD_NOTE);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(idamClient.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
                .forename("Chris").surname("Davis").build());
    }

    @Test
    @Parameters({"APPEAL_RECEIVED"})
    public void givenANonAddNoteEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ADD_NOTE"})
    public void givenAHandleAddNoteEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void testTempNoteFilledIn_thenNoteAddedToNullNotePad() {
        sscsCaseData.setTempNoteDetail("Here is my note");

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Here is my note", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
        assertEquals("Chris Davis", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getAuthor());
        assertEquals(LocalDate.now().toString(), response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDate());
    }

    @Test
    public void testTempNoteFilledIn_thenNoteAddedToEmptyCollection() {
        sscsCaseData.setTempNoteDetail("Here is my note");
        Note oldNote = Note.builder().value(NoteDetails.builder().noteDetail("Existing note").noteDate(LocalDate.now().toString())
                .author("A user").build()).build();

        sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(new ArrayList<Note>()).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Here is my note", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
        assertEquals("Chris Davis", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getAuthor());
        assertEquals(LocalDate.now().toString(), response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDate());
    }

    @Test
    public void testTempNoteFilledIn_thenNoteAddedToCollectionOfOne() {
        sscsCaseData.setTempNoteDetail("Here is my note");
        Note oldNote = Note.builder().value(NoteDetails.builder().noteDetail("Existing note").noteDate(LocalDate.now().toString())
                .author("A user").build()).build();

        sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(new ArrayList<Note>()).build());
        sscsCaseData.getAppealNotePad().getNotesCollection().add(oldNote);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(2, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Here is my note", response.getData().getAppealNotePad().getNotesCollection().get(1).getValue().getNoteDetail());
        assertEquals("Chris Davis", response.getData().getAppealNotePad().getNotesCollection().get(1).getValue().getAuthor());
        assertEquals(LocalDate.now().toString(), response.getData().getAppealNotePad().getNotesCollection().get(1).getValue().getNoteDate());
    }

    @Test
    public void testTempNoteFilledIn_thenNoteAddedToNullCollection() {
        sscsCaseData.setTempNoteDetail("Here is my note");
        Note oldNote = Note.builder().value(NoteDetails.builder().noteDetail("Existing note").noteDate(LocalDate.now().toString())
                .author("A user").build()).build();

        sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(null).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Here is my note", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
        assertEquals("Chris Davis", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getAuthor());
        assertEquals(LocalDate.now().toString(), response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDate());
    }

    @Test(expected = IllegalStateException.class)
    public void testNoUserDetails_thenThrowsException() {
        when(idamClient.getUserDetails(USER_AUTHORISATION)).thenReturn(null);

        sscsCaseData.setTempNoteDetail("Here is my note");
        Note oldNote = Note.builder().value(NoteDetails.builder().noteDetail("Existing note").noteDate(LocalDate.now().toString())
                .author("A user").build()).build();

        sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(null).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
