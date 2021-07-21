package uk.gov.hmcts.reform.sscs.ccd.presubmit.addnote;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.HMCTS_RESPONSE_REVIEWED;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@RunWith(JUnitParamsRunner.class)
public class AddNoteAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private AddNoteAboutToSubmitHandler handler;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        AddNoteService addNoteService = new AddNoteService(userDetailsService);
        handler = new AddNoteAboutToSubmitHandler(addNoteService);

        when(callback.getEvent()).thenReturn(EventType.ADD_NOTE);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
                .forename("Chris").surname("Davis").build().getFullName());
    }

    @Test
    public void testTempNoteFilledIsNull_thenNotePadIsNull() {
        sscsCaseData.setTempNoteDetail(null);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getAppealNotePad());;
    }

    @Test
    public void testTempNoteFilledIsNullAndResponseReviewedEvent_thenNoteIsAdded() {
        when(callback.getEvent()).thenReturn(HMCTS_RESPONSE_REVIEWED);
        sscsCaseData.setTempNoteDetail(null);
        sscsCaseData.setInterlocReferralReason("over300Pages");
        sscsCaseData.setSelectWhoReviewsCase(new DynamicList("Review by Judge"));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Referred to interloc for review by judge - Over 300 pages", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
    }

    @Test
    public void testTempNoteFilledIsNull_thenNoNoteAddedToCollection() {
        sscsCaseData.setTempNoteDetail(null);

        Note oldNote = Note.builder().value(NoteDetails.builder().noteDetail("Existing note").noteDate(LocalDate.now().toString())
                .author("A user").build()).build();

        sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(List.of(oldNote)).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
    }

    @Test
    public void testInterlocReferralReasonIsNoneAndResponseReviewedEvent_thenNoNoteIsAdded() {
        when(callback.getEvent()).thenReturn(HMCTS_RESPONSE_REVIEWED);
        sscsCaseData.setTempNoteDetail(null);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE.getId());
        sscsCaseData.setSelectWhoReviewsCase(new DynamicList("Review by Judge"));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getAppealNotePad());
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
    @Parameters({"timeExtension, Time extension",
                 "phmeRequest, PHME request",
                 "over300Pages, Over 300 pages",
                 "over13months, Over 13 months",
                 "other, Other",
                 "noResponseToDirection, No response to a direction"})
    public void ifEventIsResponseReviewed_AddInterlocReferralReasonToNote(String interlocReferralId, String interlocReferralLabel) {
        when(callback.getEvent()).thenReturn(HMCTS_RESPONSE_REVIEWED);
        sscsCaseData.setTempNoteDetail("Here is my note");
        sscsCaseData.setInterlocReferralReason(interlocReferralId);
        sscsCaseData.setSelectWhoReviewsCase(new DynamicList("Review by Judge"));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String expectedNote = "Referred to interloc for review by judge - " + interlocReferralLabel + " - Here is my note";

        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals(expectedNote, response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
        assertEquals("Chris Davis", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getAuthor());
        assertEquals(LocalDate.now().toString(), response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDate());
    }

    @Test
    public void testTempNoteFilledIn_thenNoteAddedToEmptyCollection() {
        sscsCaseData.setTempNoteDetail("Here is my note");

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
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        sscsCaseData.setTempNoteDetail("Here is my note");

        sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(null).build());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
