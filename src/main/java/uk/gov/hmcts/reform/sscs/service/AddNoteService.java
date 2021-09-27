package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDate;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Note;
import uk.gov.hmcts.reform.sscs.ccd.domain.NoteDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.NotePad;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class AddNoteService {

    protected final UserDetailsService userDetailsService;

    @Autowired
    public AddNoteService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public void addNote(String userAuthorisation, SscsCaseData sscsCaseData, String note) {
        if (StringUtils.isNotEmpty(note)) {
            Note newNote = Note.builder().value(NoteDetails.builder().noteDetail(note).noteDate(LocalDate.now().toString())
                    .author(userDetailsService.buildLoggedInUserName(userAuthorisation)).build()).build();
            if (sscsCaseData.getAppealNotePad() == null || sscsCaseData.getAppealNotePad().getNotesCollection() == null) {
                sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(new ArrayList<Note>()).build());
            }
            sscsCaseData.getAppealNotePad().getNotesCollection().add(newNote);
            sscsCaseData.setTempNoteDetail(null);
        }
    }
}
