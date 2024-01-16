package uk.gov.hmcts.reform.sscs.ccd.presubmit.procesaudiovideo;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.*;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoEvidenceMidEventHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;

@RunWith(JUnitParamsRunner.class)
public class ProcessAudioVideoEvidenceMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String URL = "http://dm-store/documents/123";

    private static final String DOCUMENT_MANAGEMENT_URL = "http://dm-store/documents";

    private static final LocalDate DATE = LocalDate.of(2021, 1,1);

    private static final DocumentLink DOCUMENT_LINK = DocumentLink.builder().documentFilename("music.mp3").documentUrl(DOCUMENT_MANAGEMENT_URL + "/2124-12").documentBinaryUrl(DOCUMENT_MANAGEMENT_URL + "/2124-12/binary").build();

    private final UserDetails userDetails = UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();

    private ProcessAudioVideoEvidenceMidEventHandler handler;

    @Spy
    private DocumentConfiguration documentConfiguration;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    private ArgumentCaptor<GenerateFileParams> capture;

    @Mock
    private GenerateFile generateFile;

    @Mock
    private IdamService idamService;

    @Before
    public void setUp() {
        openMocks(this);

        Map<EventType, String> englishEventTypeDocs = new HashMap<>();
        englishEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-ENG-directions-notice.docx");

        Map<LanguagePreference, Map<EventType, String>> documents =  new HashMap<>();
        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);

        documentConfiguration.setDocuments(documents);
        handler = new ProcessAudioVideoEvidenceMidEventHandler(generateFile, documentConfiguration, idamService);

        sscsCaseData = SscsCaseData.builder()
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .directionNoticeContent("Body Content")
                .build())
            .directionDueDate(LocalDate.now().plusDays(1).toString())
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
            .processAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.ISSUE_DIRECTIONS_NOTICE.getCode()))
            .selectedAudioVideoEvidence(new DynamicList(DOCUMENT_MANAGEMENT_URL + "/2124-12"))
            .audioVideoEvidence(singletonList(AudioVideoEvidence.builder().value(
                AudioVideoEvidenceDetails.builder()
                    .documentLink(DOCUMENT_LINK)
                    .fileName("music.mp3")
                    .partyUploaded(UploadParty.APPELLANT)
                    .dateAdded(DATE)
                    .build())
                .build()))
            .selectedAudioVideoEvidenceDetails(AudioVideoEvidenceDetails.builder().build())
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT")
                        .lastName("Last'NamE")
                        .build())
                    .identity(Identity.builder().build())
                    .build())
                .build()).build();

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.PROCESS_AUDIO_VIDEO);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(generateFile.assemble(any())).thenReturn(URL);

        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }

    @Test
    public void givenLanguagePreferenceIsEnglish_NoticeIsGeneratedAndPopulatedInPreviewDocumentField() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDocumentStaging().getPreviewDocument(), is(notNullValue()));
        final DocumentLink expectedDocumentLink = DocumentLink.builder()
                .documentFilename(String.format("Directions Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .documentBinaryUrl(URL + "/binary")
                .documentUrl(URL)
                .build();
        assertThat(response.getData().getDocumentStaging().getPreviewDocument(), is(expectedDocumentLink));

        verify(generateFile, times(1)).assemble(any());
        verifyTemplateBody(
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.DIRECTION_ISSUED));
    }

    @Test
    public void giveSendToJudge_thenDoNotSetPreviewDocument() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.SEND_TO_JUDGE.getCode()));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDocumentStaging().getPreviewDocument(), is(nullValue()));
        verifyNoInteractions(generateFile);
    }

    @Test
    public void giveSendToAdmin_thenDoNotSetPreviewDocument() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.SEND_TO_ADMIN.getCode()));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDocumentStaging().getPreviewDocument(), is(nullValue()));
        verifyNoInteractions(generateFile);
    }

    @Test
    public void givenSelectedAudioVideoEvidenceDetailsIsNull_thenDoNotSetPreviewDocument_AndBuildNewSelectedAudioVideoEvidenceDetailsObject() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.SEND_TO_ADMIN.getCode()));
        sscsCaseData.setSelectedAudioVideoEvidenceDetails(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        final AudioVideoEvidenceDetails expectedEvidenceDetails = AudioVideoEvidenceDetails.builder()
                .partyUploaded(UploadParty.APPELLANT)
                .dateAdded(DATE)
                .documentType("Audio document")
                .documentLink(DOCUMENT_LINK)
                .fileName("music.mp3")
                .build();

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDocumentStaging().getPreviewDocument(), is(nullValue()));
        verifyNoInteractions(generateFile);
        assertEquals(response.getData().getSelectedAudioVideoEvidenceDetails(), expectedEvidenceDetails);
    }

    @Test
    public void givenDirectionsDueDateIsToday_ThenDisplayAnError() {
        sscsCaseData.setDirectionDueDate(LocalDate.now().toString());
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ISSUE_DIRECTIONS_NOTICE.getCode()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Directions due date must be in the future", error);
    }

    @Test
    public void givenDirectionsDueDateIsBeforeToday_ThenDisplayAnError() {
        String yesterdayDate = LocalDate.now().plus(-1, ChronoUnit.DAYS).toString();
        sscsCaseData.setDirectionDueDate(yesterdayDate);
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ISSUE_DIRECTIONS_NOTICE.getCode()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Directions due date must be in the future", error);
    }

    @Test
    public void givenDirectionsDueDateIsBeforeTodayAndProcessAudioVideoActionIsNotIssueDirectionsNotice_ThenDoNotDisplayAnError() {
        String yesterdayDate = LocalDate.now().plus(-1, ChronoUnit.DAYS).toString();
        sscsCaseData.setDirectionDueDate(yesterdayDate);
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ADMIT_EVIDENCE.getCode()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    public void givenActionDropDownNotSetThenSetValueAndOptions() {
        sscsCaseData.setProcessAudioVideoAction(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        DynamicListItem item = new DynamicListItem(ISSUE_DIRECTIONS_NOTICE.getCode(), ISSUE_DIRECTIONS_NOTICE.getLabel());
        DynamicListItem item2 = new DynamicListItem(SEND_TO_JUDGE.getCode(), SEND_TO_JUDGE.getLabel());
        List<DynamicListItem> items = List.of(item, item2);

        DynamicList expectedAudioActions = new DynamicList(item, items);

        assertThat(response.getErrors().size(), is(0));
        assertEquals(expectedAudioActions, response.getData().getProcessAudioVideoAction());
    }

    @Test
    public void givenActionDropDownIsSetThenSetJustOptions() {
        DynamicList adminDL = new DynamicList(SEND_TO_ADMIN.getCode());
        sscsCaseData.setProcessAudioVideoAction(adminDL);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        DynamicListItem item = new DynamicListItem(ISSUE_DIRECTIONS_NOTICE.getCode(), ISSUE_DIRECTIONS_NOTICE.getLabel());
        DynamicListItem item2 = new DynamicListItem(SEND_TO_JUDGE.getCode(), SEND_TO_JUDGE.getLabel());
        List<DynamicListItem> items = List.of(item, item2);

        DynamicList expectedAudioActions = new DynamicList(adminDL.getValue(), items);

        assertThat(response.getErrors().size(), is(0));
        assertEquals(expectedAudioActions, response.getData().getProcessAudioVideoAction());
    }


    @Test
    public void givenJudgeRole_thenUserCanProcessMoreAudioVideoActions() {
        userDetails.getRoles().add(JUDGE.getValue());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        SscsCaseData responseData = response.getData();
        assertEquals(4, responseData.getProcessAudioVideoAction().getListItems().size());
        assertEquals(ISSUE_DIRECTIONS_NOTICE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), ISSUE_DIRECTIONS_NOTICE.getCode()));
        assertEquals(ADMIT_EVIDENCE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), ADMIT_EVIDENCE.getCode()));
        assertEquals(EXCLUDE_EVIDENCE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), EXCLUDE_EVIDENCE.getCode()));
        assertEquals(SEND_TO_ADMIN.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), SEND_TO_ADMIN.getCode()));
    }

    @Test
    public void superUser_willGetAllActions() {
        userDetails.getRoles().add(SUPER_USER.getValue());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        SscsCaseData responseData = response.getData();
        assertEquals(5, responseData.getProcessAudioVideoAction().getListItems().size());
        assertEquals(ISSUE_DIRECTIONS_NOTICE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), ISSUE_DIRECTIONS_NOTICE.getCode()));
        assertEquals(ADMIT_EVIDENCE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), ADMIT_EVIDENCE.getCode()));
        assertEquals(EXCLUDE_EVIDENCE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), EXCLUDE_EVIDENCE.getCode()));
        assertEquals(SEND_TO_JUDGE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), SEND_TO_JUDGE.getCode()));
        assertEquals(SEND_TO_ADMIN.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), SEND_TO_ADMIN.getCode()));
    }

    @Test
    public void givenTcwRole_verifyUserActions() {
        userDetails.getRoles().add(TCW.getValue());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        SscsCaseData responseData = response.getData();
        assertEquals(5, responseData.getProcessAudioVideoAction().getListItems().size());
        assertEquals(ISSUE_DIRECTIONS_NOTICE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), ISSUE_DIRECTIONS_NOTICE.getCode()));
        assertEquals(ADMIT_EVIDENCE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), ADMIT_EVIDENCE.getCode()));
        assertEquals(EXCLUDE_EVIDENCE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), EXCLUDE_EVIDENCE.getCode()));
        assertEquals(SEND_TO_JUDGE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), SEND_TO_JUDGE.getCode()));
        assertEquals(SEND_TO_ADMIN.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), SEND_TO_ADMIN.getCode()));
    }

    @Test
    public void givenEvidenceDropDownNotSetThenSetValueAndOptions() {
        sscsCaseData.setSelectedAudioVideoEvidence(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        DynamicListItem item = new DynamicListItem(DOCUMENT_MANAGEMENT_URL + "/2124-12", "music.mp3");
        List<DynamicListItem> items = List.of(item);

        DynamicList expectedEvidences = new DynamicList(item, items);

        assertThat(response.getErrors().size(), is(0));
        assertEquals(expectedEvidences, response.getData().getSelectedAudioVideoEvidence());
    }

    @Test
    public void givenEvidenceDropDownSetThenSetOptions() {
        DynamicList selectedEv =  new DynamicList("my-ev.pdf");
        sscsCaseData.setSelectedAudioVideoEvidence(selectedEv);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        DynamicListItem item = new DynamicListItem(DOCUMENT_MANAGEMENT_URL + "/2124-12", "music.mp3");
        List<DynamicListItem> items = List.of(item);

        DynamicList expectedEvidences = new DynamicList(selectedEv.getValue(), items);

        assertThat(response.getErrors().size(), is(0));
        assertEquals(expectedEvidences, response.getData().getSelectedAudioVideoEvidence());
    }

    private void verifyTemplateBody(String templateId) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertEquals(NoticeIssuedTemplateBody.ENGLISH_IMAGE, payload.getImage());
        assertEquals("DIRECTIONS NOTICE", payload.getNoticeType());
        assertEquals("APPELLANT Last'NamE", payload.getAppellantFullName());
        assertEquals(sscsCaseData.getDocumentGeneration().getDirectionNoticeContent(), payload.getNoticeBody());
        assertEquals(templateId, value.getTemplateId());
    }

    private String getItemCodeInList(DynamicList dynamicList, String item) {
        return dynamicList.getListItems().stream()
                .filter(o -> item.equals(o.getCode()))
                .findFirst()
                .map(DynamicListItem::getCode)
                .orElse(null);
    }
}
