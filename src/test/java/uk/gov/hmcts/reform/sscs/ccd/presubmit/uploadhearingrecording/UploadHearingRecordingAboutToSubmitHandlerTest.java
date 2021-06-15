package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording.HearingTypeForRecording.ADJOURNED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording.HearingTypeForRecording.FINAL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecording;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecording;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecordingCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecordingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;

@RunWith(JUnitParamsRunner.class)
public class UploadHearingRecordingAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private final UserDetails
        userDetails =
        UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();
    private UploadHearingRecordingAboutToSubmitHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private IdamService idamService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new UploadHearingRecordingAboutToSubmitHandler(idamService);
        sscsCaseData = SscsCaseData.builder().appeal(
            Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_HEARING_RECORDING);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }

    @Test
    public void givenAValidHandleAndEventType_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenNonUploadHearingRecordingEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenUploadHearingRecordingEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_HEARING_RECORDING);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenHearingRecordings_thenReturnSscsRecordings() {
        List<HearingRecording> hearingRecordings = new ArrayList<>();
        hearingRecordings.add(HearingRecording.builder()
            .value(HearingRecordingDetails.builder()
                .hearingType(ADJOURNED.getKey())
                .documentLink(DocumentLink.builder()
                    .documentFilename("test-file1.MP4")
                    .documentUrl("/some-url")
                    .documentBinaryUrl("/some-url/binary")
                    .build()).build()).build());
        hearingRecordings.add(HearingRecording.builder()
            .value(HearingRecordingDetails.builder()
                .hearingType(FINAL.getKey())
                .documentLink(DocumentLink.builder()
                    .documentFilename("test-file2.mp3")
                    .documentUrl("/some-url2")
                    .documentBinaryUrl("/some-url2/binary")
                    .build()).build()).build());
        hearingRecordings.add(HearingRecording.builder()
            .value(HearingRecordingDetails.builder()
                .hearingType(ADJOURNED.getKey())
                .documentLink(DocumentLink.builder()
                    .documentFilename("test-file3.mp4")
                    .documentUrl("/some-url3")
                    .documentBinaryUrl("/some-url3/binary")
                    .build()).build()).build());

        List<SscsHearingRecording> existingRecordings = new ArrayList<>();
        existingRecordings.add(SscsHearingRecording.builder()
            .value(SscsHearingRecordingDetails.builder()
                .hearingType(ADJOURNED.getKey())
                .uploadDate("today")
                .hearingDate("06-06-2021 05:00:00 PM")
                .documentLink(DocumentLink.builder()
                    .documentFilename("Adjourned good - value 06 Jun 2021.mp4")
                    .documentUrl("/some-url")
                    .documentBinaryUrl("/some-url/binary")
                    .build()).build()).build());
        existingRecordings.add(SscsHearingRecording.builder()
            .value(SscsHearingRecordingDetails.builder()
                .hearingType(ADJOURNED.getKey())
                .uploadDate("today")
                .hearingDate("08-06-2021 11:15:00 AM")
                .documentLink(DocumentLink.builder()
                    .documentFilename("Adjourned test 08 Jun 2021.mp3")
                    .documentUrl("/some-url8")
                    .documentBinaryUrl("/some-url8/binary")
                    .build()).build()).build());

        sscsCaseData.setSscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
            .selectHearingDetails(new DynamicList(
                new DynamicListItem("2222", "good - value 17:00:00 06 Jun 2021"), Collections.EMPTY_LIST))
            .hearingRecordings(hearingRecordings)
            .sscsHearingRecordings(existingRecordings)
            .build());
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(YesNo.YES, response.getData().getSscsHearingRecordingCaseData().getShowHearingRecordings());
        List<SscsHearingRecording> sscsHearingRecordings =
            response.getData().getSscsHearingRecordingCaseData().getSscsHearingRecordings();
        assertEquals(5, sscsHearingRecordings.size());
        assertNotNull(sscsHearingRecordings.get(1));
        assertEquals("Adjourned good - value 06 Jun 2021 (2).MP4",
            sscsHearingRecordings.get(1).getValue().getDocumentLink().getDocumentFilename());
        assertEquals("adjourned", sscsHearingRecordings.get(1).getValue().getHearingType());
        assertEquals("06-06-2021 05:00:00 PM", sscsHearingRecordings.get(1).getValue().getHearingDate());
        assertNotNull(sscsHearingRecordings.get(1).getValue().getUploadDate());

        assertNotNull(sscsHearingRecordings.get(2));
        assertEquals("Final good - value 06 Jun 2021.mp3",
            sscsHearingRecordings.get(2).getValue().getDocumentLink().getDocumentFilename());
        assertEquals("final", sscsHearingRecordings.get(2).getValue().getHearingType());
        assertNotNull(sscsHearingRecordings.get(2).getValue().getUploadDate());

        assertNotNull(sscsHearingRecordings.get(3));
        assertEquals("Adjourned good - value 06 Jun 2021 (3).mp4",
            sscsHearingRecordings.get(3).getValue().getDocumentLink().getDocumentFilename());
        assertEquals("adjourned", sscsHearingRecordings.get(3).getValue().getHearingType());
        assertNotNull(sscsHearingRecordings.get(3).getValue().getUploadDate());

        assertNotNull(sscsHearingRecordings.get(4));
        assertEquals("Adjourned test 08 Jun 2021.mp3",
            sscsHearingRecordings.get(4).getValue().getDocumentLink().getDocumentFilename());
        assertEquals("adjourned", sscsHearingRecordings.get(4).getValue().getHearingType());
        assertEquals("08-06-2021 11:15:00 AM", sscsHearingRecordings.get(4).getValue().getHearingDate());

    }

    @Test
    public void givenNullHearingRecordings_thenZeroSscsRecordings() {
        sscsCaseData.setSscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
            .selectHearingDetails(new DynamicList(
                new DynamicListItem("2222", "good - value 09:00:00 06 Jun 2021"), Collections.EMPTY_LIST))
            .hearingRecordings(null)
            .sscsHearingRecordings(null)
            .build());
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        List<SscsHearingRecording> sscsHearingRecordings =
            response.getData().getSscsHearingRecordingCaseData().getSscsHearingRecordings();
        assertNull(sscsHearingRecordings);
        assertNull(response.getData().getSscsHearingRecordingCaseData().getShowHearingRecordings());

    }

    @Test
    public void givenSelectedHearingIsPresent_thenReturnVenueDate() {
        DynamicList dynamicList = new DynamicList(
            new DynamicListItem("2222", "good - value 09:00:15 06 Jun 2021"), Collections.EMPTY_LIST);
        assertEquals("good - value 06 Jun 2021", handler.getHearingVenueDate(dynamicList));
    }

    @Test
    public void givenSelectedHearingPresent_thenReturnDateTime() {
        DynamicList dynamicList = new DynamicList(
            new DynamicListItem("2222", "good - value 09:00:17 06 Jun 2021"), Collections.EMPTY_LIST);
        assertEquals("09:00:17 06 Jun 2021", handler.getHearingDateTime(dynamicList));
    }

    @Test
    public void givenHearingRecordingPresent_thenReturnFileExtension() {
        HearingRecording hearingRecording = HearingRecording.builder()
            .value(HearingRecordingDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentFilename("test-file.MP4").build()).build()).build();
        assertEquals(".MP4", handler.getFileExtension(hearingRecording));
    }

    @Test
    public void givenSingleRecordingPresent_thenReturnFileName() {
        String fileName = handler.createFileName("test 06 Jun 2021", 1, ADJOURNED.getValue());
        assertEquals("Adjourned test 06 Jun 2021", fileName);
    }

    @Test
    public void givenMultipleRecordingsPresent_thenReturnFileName() {
        String fileName = handler.createFileName("test 06 Jun 2021", 3, FINAL.getValue());
        assertEquals("Final test 06 Jun 2021 (3)", fileName);
    }

}
