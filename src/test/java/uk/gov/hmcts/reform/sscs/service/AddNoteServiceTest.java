package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

public class AddNoteServiceTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private UserDetailsService userDetailsService;

    private AddNoteService addNoteService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setup() {
        openMocks(this);
        addNoteService = new AddNoteService(userDetailsService);

        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
                .forename("Chris").surname("Davis").build().getFullName());

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();
    }

    @Test
    public void givenNoteNotBlank_AddNoteToAppealNotePad() {
        addNoteService.addNote(USER_AUTHORISATION, sscsCaseData, "new note");
        assertEquals(1, sscsCaseData.getAppealNotePad().getNotesCollection().size());
        assertEquals("new note", sscsCaseData.getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
    }
}
