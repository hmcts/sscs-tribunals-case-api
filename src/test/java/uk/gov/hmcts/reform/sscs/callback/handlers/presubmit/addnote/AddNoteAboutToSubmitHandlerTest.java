package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.addnote;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADD_NOTE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.addnote.AddNoteAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;

@ExtendWith(MockitoExtension.class)
public class AddNoteAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    @Mock
    private AddNoteService addNoteService;

    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    private AddNoteAboutToSubmitHandler handler;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .createdInGapsFrom(READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();
        caseDetails = new CaseDetails<>(1234L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), ADD_NOTE, false);

        handler = new AddNoteAboutToSubmitHandler(addNoteService);
    }

    @Test
    public void testTempNoteFilledIsNull_thenNotePadIsNull() {
        sscsCaseData.setTempNoteDetail(null);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(addNoteService).addNote(eq(USER_AUTHORISATION), eq(sscsCaseData), eq(null));
    }

    @Test
    public void givenANonAddNoteEvent_thenReturnFalse() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), APPEAL_RECEIVED, false);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAHandleAddNoteEvent_thenReturnTrue() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), ADD_NOTE, false);

        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), APPEAL_RECEIVED, false);

        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    public void testTempNoteFilledIn_thenNoteAddedToNullNotePad() {
        String expectedNote = "Here is my note";
        sscsCaseData.setTempNoteDetail(expectedNote);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(addNoteService).addNote(eq(USER_AUTHORISATION), eq(sscsCaseData), eq(expectedNote));
    }
}
