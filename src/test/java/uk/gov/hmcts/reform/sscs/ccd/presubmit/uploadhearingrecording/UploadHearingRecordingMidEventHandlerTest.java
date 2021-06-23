package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecording;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.service.DocumentDownloadService;

@RunWith(JUnitParamsRunner.class)
public class UploadHearingRecordingMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private final UserDetails
        userDetails =
        UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();
    private UploadHearingRecordingMidEventHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private IdamService idamService;
    @Mock
    private DocumentDownloadService documentDownloadService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new UploadHearingRecordingMidEventHandler(documentDownloadService, idamService);
        sscsCaseData = SscsCaseData.builder().appeal(
            Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_HEARING_RECORDING);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }

    @Test
    public void givenAValidHandleAndEventType_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenNonUploadHearingRecordingEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenUploadHearingRecordingEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_HEARING_RECORDING);
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenHearingRecordingListIsNull_ReturnResponse() {
        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordings(null);
        assertNoErrors();
    }

    @Test
    public void givenHearingRecordingListIsEmpty_ReturnResponse() {
        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordings(Collections.EMPTY_LIST);
        assertNoErrors();
    }

    @Test
    public void givenHearingRecordingListHasLessThan500Mb_ReturnResponse() {
        List<HearingRecording> recordings = new ArrayList<>();
        recordings.add(HearingRecording.builder().value(
            HearingRecordingDetails.builder().documentLink(
                DocumentLink.builder()
                    .documentFilename("Test 1.mp4")
                    .documentBinaryUrl("/some-link")
                    .build()).build()).build());

        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordings(unmodifiableList(recordings));
        ResponseEntity<Resource> resource = ResponseEntity.ok(new ByteArrayResource("test".getBytes()));
        when(documentDownloadService.downloadFile(any())).thenReturn(resource);
        when(documentDownloadService.getFileSize(any())).thenReturn(Long.valueOf(500 * 1024 * 1024));
        assertNoErrors();
    }

    @Test
    public void givenHearingRecordingListHasGreaterThan500Mb_ReturnError() {
        List<HearingRecording> recordings = new ArrayList<>();
        recordings.add(HearingRecording.builder().value(
            HearingRecordingDetails.builder().documentLink(
                DocumentLink.builder()
                    .documentFilename("Test 1.mp3")
                    .documentBinaryUrl("/some-link")
                    .build()).build()).build());

        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordings(unmodifiableList(recordings));
        ResponseEntity<Resource> resource = ResponseEntity.ok(new ByteArrayResource("test".getBytes()));
        when(documentDownloadService.downloadFile(any())).thenReturn(resource);
        when(documentDownloadService.getFileSize(any())).thenReturn(Long.valueOf(501 * 1024 * 1024));
        final PreSubmitCallbackResponse<SscsCaseData>
            response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("The upload file size is more than the allowed limit", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenHearingRecordingListHasBadFileExtension_ReturnError() {
        List<HearingRecording> recordings = new ArrayList<>();
        recordings.add(HearingRecording.builder().value(
            HearingRecordingDetails.builder().documentLink(
                DocumentLink.builder()
                    .documentFilename("Test 1.pdf")
                    .documentBinaryUrl("/some-link")
                    .build()).build()).build());

        ResponseEntity<Resource> resource = ResponseEntity.ok(new ByteArrayResource("test".getBytes()));
        when(documentDownloadService.downloadFile(any())).thenReturn(resource);
        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordings(unmodifiableList(recordings));

        final PreSubmitCallbackResponse<SscsCaseData>
            response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("The file type you uploaded is not accepted", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenFileExtensions_thenReturnResult() {

        assertFalse(handler.isInvalidFile("test.mP3"));
        assertFalse(handler.isInvalidFile("test.mp4"));
        assertFalse(handler.isInvalidFile(".MP4test.MP4"));
        assertFalse(handler.isInvalidFile("test.MP3"));
        assertTrue(handler.isInvalidFile("test.MP33"));
        assertTrue(handler.isInvalidFile("test.pdf"));
        assertTrue(handler.isInvalidFile("testmp4"));
    }

    private void assertNoErrors() {
        final PreSubmitCallbackResponse<SscsCaseData>
            response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
    }

}
