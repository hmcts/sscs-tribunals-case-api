package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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
    public void givenUploadingHearingRecordingsToHearingWithRecodingAlreadyExisting_thenSscsRecordingsShouldBeOverridenWithNewDocuments() {
        List<HearingRecordingDetails> details = new ArrayList<>();
        details.add(
            HearingRecordingDetails.builder().value(
                DocumentLink.builder()
                    .documentFilename("Adjourned good - value 06 Jun 2021.mp4")
                    .documentUrl("/some-url")
                    .documentBinaryUrl("/some-url/binary")
                    .build()).build());

        List<SscsHearingRecording> existingRecordings = new ArrayList<>();
        existingRecordings.add(SscsHearingRecording.builder()
                .value(SscsHearingRecordingDetails.builder()
                    .hearingType(ADJOURNED.getKey())
                    .uploadDate("today")
                    .hearingDate("06-06-2021 05:00:00 PM")
                    .hearingId("2222")
                    .venue("good - value")
                    .recordings(details).build())
                .build());
        existingRecordings.add(SscsHearingRecording.builder()
            .value(SscsHearingRecordingDetails.builder()
                    .hearingType(ADJOURNED.getKey())
                    .uploadDate("today")
                    .hearingDate("08-06-2021 11:15:00 AM")
                    .hearingId("3333")
                    .recordings(new ArrayList<>()).build())
                .build());
        existingRecordings.add(SscsHearingRecording.builder()
            .value(SscsHearingRecordingDetails.builder()
                    .hearingType(FINAL.getKey())
                    .uploadDate("today")
                    .hearingDate("07-06-2021 05:00:00 PM")
                    .hearingId("4444")
                    .recordings(new ArrayList<>()).build())
                .build());

        sscsCaseData.setSscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
            .selectHearingDetails(new DynamicList(
                new DynamicListItem("2222", "good - value 17:00:00 06 Jun 2021"), Collections.EMPTY_LIST))
            .hearingRecording(createHearingRecording(ADJOURNED.getKey()))
            .sscsHearingRecordings(existingRecordings)
            .build());

        sscsCaseData.setHearings(singletonList(Hearing.builder().value(
                HearingDetails.builder().hearingId("2222").venue(Venue.builder().name("good - value").build())
                        .hearingDate("2021-06-06")
                        .time("17:00").build()).build()));

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        List<SscsHearingRecording> sscsHearingRecordings =
            response.getData().getSscsHearingRecordingCaseData().getSscsHearingRecordings();
        assertEquals(3, sscsHearingRecordings.size());
        assertNotNull(sscsHearingRecordings.get(1));
        assertNotNull(sscsHearingRecordings.get(2));

        assertEquals("adjourned", sscsHearingRecordings.get(0).getValue().getHearingType());
        assertEquals("06-06-2021 05:00:00 PM", sscsHearingRecordings.get(0).getValue().getHearingDate());
        assertEquals("good - value", sscsHearingRecordings.get(0).getValue().getVenue());
        assertEquals("2222", sscsHearingRecordings.get(0).getValue().getHearingId());
        assertNotNull(sscsHearingRecordings.get(0).getValue().getUploadDate());
        assertEquals(3, sscsHearingRecordings.get(0).getValue().getRecordings().size());

        assertEquals("Adjourned good - value 06 Jun 2021.MP4",
            sscsHearingRecordings.get(0).getValue().getRecordings().get(0).getValue().getDocumentFilename());
        assertEquals("Adjourned good - value 06 Jun 2021 (2).mp3",
            sscsHearingRecordings.get(0).getValue().getRecordings().get(1).getValue().getDocumentFilename());
        assertEquals("Adjourned good - value 06 Jun 2021 (3).mp4",
            sscsHearingRecordings.get(0).getValue().getRecordings().get(2).getValue().getDocumentFilename());

        assertEquals("final", sscsHearingRecordings.get(1).getValue().getHearingType());
        assertEquals("07-06-2021 05:00:00 PM", sscsHearingRecordings.get(1).getValue().getHearingDate());
        assertNotNull(sscsHearingRecordings.get(1).getValue().getUploadDate());
        assertEquals(0, sscsHearingRecordings.get(1).getValue().getRecordings().size());

        assertEquals("adjourned", sscsHearingRecordings.get(2).getValue().getHearingType());
        assertEquals("08-06-2021 11:15:00 AM", sscsHearingRecordings.get(2).getValue().getHearingDate());
        assertNotNull(sscsHearingRecordings.get(2).getValue().getUploadDate());
        assertEquals(0, sscsHearingRecordings.get(2).getValue().getRecordings().size());

    }

    @Test
    public void givenUploadingHearingRecordingsToHearingWithRequestedRequests_thenSscsRecordingsInRequestsShouldBeOverridenWithNewDocuments() {
        List<HearingRecordingDetails> details = new ArrayList<>();
        details.add(
                HearingRecordingDetails.builder().value(
                        DocumentLink.builder()
                                .documentFilename("Adjourned good - value 06 Jun 2021.mp4")
                                .documentUrl("/some-url")
                                .documentBinaryUrl("/some-url/binary")
                                .build()).build());

        List<SscsHearingRecording> existingRecordings = new ArrayList<>();
        existingRecordings.add(SscsHearingRecording.builder()
                .value(SscsHearingRecordingDetails.builder()
                        .hearingType(ADJOURNED.getKey())
                        .uploadDate("today")
                        .hearingDate("06-06-2021 05:00:00 PM")
                        .hearingId("2222")
                        .venue("good - value")
                        .recordings(details).build())
                .build());

        List<HearingRecordingRequest> requested = new ArrayList<>();
        requested.add(HearingRecordingRequest.builder()
                .value(HearingRecordingRequestDetails.builder()
                        .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                .hearingType(ADJOURNED.getKey())
                                .uploadDate("today")
                                .hearingDate("06-06-2021 05:00:00 PM")
                                .hearingId("2222")
                                .venue("good - value")
                                .recordings(details).build())
                        .build())
                .build());

        sscsCaseData.setSscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
                .selectHearingDetails(new DynamicList(
                        new DynamicListItem("2222", "good - value 17:00:00 06 Jun 2021"), Collections.EMPTY_LIST))
                .hearingRecording(createHearingRecording(ADJOURNED.getKey()))
                .sscsHearingRecordings(existingRecordings)
                .requestedHearings(requested)
                .build());

        sscsCaseData.setHearings(singletonList(Hearing.builder().value(
                HearingDetails.builder().hearingId("2222").venue(Venue.builder().name("good - value").build())
                        .hearingDate("2021-06-06")
                        .time("17:00").build()).build()));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        SscsHearingRecordingDetails sscsHearingRecording =
                response.getData().getSscsHearingRecordingCaseData().getRequestedHearings().get(0).getValue().getSscsHearingRecording();

        assertEquals("adjourned", sscsHearingRecording.getHearingType());
        assertEquals("06-06-2021 05:00:00 PM", sscsHearingRecording.getHearingDate());
        assertEquals("good - value", sscsHearingRecording.getVenue());
        assertEquals("2222", sscsHearingRecording.getHearingId());
        assertNotNull(sscsHearingRecording.getUploadDate());
        assertEquals(3, sscsHearingRecording.getRecordings().size());

        assertEquals("Adjourned good - value 06 Jun 2021.MP4",
                sscsHearingRecording.getRecordings().get(0).getValue().getDocumentFilename());
        assertEquals("Adjourned good - value 06 Jun 2021 (2).mp3",
                sscsHearingRecording.getRecordings().get(1).getValue().getDocumentFilename());
        assertEquals("Adjourned good - value 06 Jun 2021 (3).mp4",
                sscsHearingRecording.getRecordings().get(2).getValue().getDocumentFilename());
    }


    @Test
    public void givenUploadingHearingRecordingsToHearingWithReleasedRequests_thenSscsRecordingsInRequestsShouldBeOverridenWithNewDocuments() {
        List<HearingRecordingDetails> details = new ArrayList<>();
        details.add(
                HearingRecordingDetails.builder().value(
                        DocumentLink.builder()
                                .documentFilename("Adjourned good - value 06 Jun 2021.mp4")
                                .documentUrl("/some-url")
                                .documentBinaryUrl("/some-url/binary")
                                .build()).build());

        List<SscsHearingRecording> existingRecordings = new ArrayList<>();
        existingRecordings.add(SscsHearingRecording.builder()
                .value(SscsHearingRecordingDetails.builder()
                        .hearingType(ADJOURNED.getKey())
                        .uploadDate("today")
                        .hearingDate("06-06-2021 05:00:00 PM")
                        .hearingId("2222")
                        .venue("good - value")
                        .recordings(details).build())
                .build());

        List<HearingRecordingRequest> released = new ArrayList<>();
        released.add(HearingRecordingRequest.builder()
                .value(HearingRecordingRequestDetails.builder()
                        .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                .hearingType(ADJOURNED.getKey())
                                .uploadDate("today")
                                .hearingDate("06-06-2021 05:00:00 PM")
                                .hearingId("2222")
                                .venue("good - value")
                                .recordings(details).build())
                        .build())
                .build());

        sscsCaseData.setSscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
                .selectHearingDetails(new DynamicList(
                        new DynamicListItem("2222", "good - value 17:00:00 06 Jun 2021"), Collections.EMPTY_LIST))
                .hearingRecording(createHearingRecording(ADJOURNED.getKey()))
                .sscsHearingRecordings(existingRecordings)
                .dwpReleasedHearings(released)
                .build());

        sscsCaseData.setHearings(singletonList(Hearing.builder().value(
                HearingDetails.builder().hearingId("2222").venue(Venue.builder().name("good - value").build())
                        .hearingDate("2021-06-06")
                        .time("17:00").build()).build()));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        SscsHearingRecordingDetails sscsHearingRecording =
                response.getData().getSscsHearingRecordingCaseData().getDwpReleasedHearings().get(0).getValue().getSscsHearingRecording();

        assertEquals("adjourned", sscsHearingRecording.getHearingType());
        assertEquals("06-06-2021 05:00:00 PM", sscsHearingRecording.getHearingDate());
        assertEquals("good - value", sscsHearingRecording.getVenue());
        assertEquals("2222", sscsHearingRecording.getHearingId());
        assertNotNull(sscsHearingRecording.getUploadDate());
        assertEquals(3, sscsHearingRecording.getRecordings().size());

        assertEquals("Adjourned good - value 06 Jun 2021.MP4",
                sscsHearingRecording.getRecordings().get(0).getValue().getDocumentFilename());
        assertEquals("Adjourned good - value 06 Jun 2021 (2).mp3",
                sscsHearingRecording.getRecordings().get(1).getValue().getDocumentFilename());
        assertEquals("Adjourned good - value 06 Jun 2021 (3).mp4",
                sscsHearingRecording.getRecordings().get(2).getValue().getDocumentFilename());
    }

    @Test
    public void givenNoExistingHearingRecordings_thenCreateSscsRecordings() {

        sscsCaseData.setSscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
            .selectHearingDetails(new DynamicList(
                new DynamicListItem("2222", "good - value 17:00:00 06 Jun 2021"), Collections.EMPTY_LIST))
            .hearingRecording(createHearingRecording(FINAL.getKey()))
            .sscsHearingRecordings(null)
            .build());

        sscsCaseData.setHearings(singletonList(Hearing.builder().value(
                HearingDetails.builder().hearingId("2222").venue(Venue.builder().name("good - value").build())
                        .hearingDate("2021-06-06")
                        .time("17:00").build()).build()));

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        List<SscsHearingRecording> sscsHearingRecordings =
            response.getData().getSscsHearingRecordingCaseData().getSscsHearingRecordings();
        assertEquals(1, sscsHearingRecordings.size());
        assertNotNull(sscsHearingRecordings.get(0));

        assertEquals("final", sscsHearingRecordings.get(0).getValue().getHearingType());
        assertEquals("06-06-2021 05:00:00 PM", sscsHearingRecordings.get(0).getValue().getHearingDate());
        assertNotNull(sscsHearingRecordings.get(0).getValue().getUploadDate());
        assertEquals(3, sscsHearingRecordings.get(0).getValue().getRecordings().size());

        assertEquals("Final good - value 06 Jun 2021.MP4",
            sscsHearingRecordings.get(0).getValue().getRecordings().get(0).getValue().getDocumentFilename());
        assertEquals("Final good - value 06 Jun 2021 (2).mp3",
            sscsHearingRecordings.get(0).getValue().getRecordings().get(1).getValue().getDocumentFilename());
        assertEquals("Final good - value 06 Jun 2021 (3).mp4",
            sscsHearingRecordings.get(0).getValue().getRecordings().get(2).getValue().getDocumentFilename());

    }

    private HearingRecording createHearingRecording(String hearingType) {
        List<HearingRecordingDetails> details = new ArrayList<>();
        details.add(
            HearingRecordingDetails.builder()
                .value(DocumentLink.builder()
                    .documentFilename("test-file1.MP4")
                    .documentUrl("/some-url")
                    .documentBinaryUrl("/some-url/binary")
                    .build()).build());
        details.add(
            HearingRecordingDetails.builder()
                .value(
                    DocumentLink.builder()
                        .documentFilename("test-file2.mp3")
                        .documentUrl("/some-url2")
                        .documentBinaryUrl("/some-url2/binary")
                        .build()).build());
        details.add(
            HearingRecordingDetails.builder()
                .value(
                    DocumentLink.builder()
                        .documentFilename("test-file3.mp4")
                        .documentUrl("/some-url3")
                        .documentBinaryUrl("/some-url3/binary")
                        .build()).build());
        return HearingRecording.builder()
            .hearingType(hearingType)
            .recordings(details)
            .build();
    }

    @Test
    public void givenValidValue_thenCreateSscsRecordings() {
        SscsHearingRecording sscsHearingRecording = handler.createSscsHearingRecording("today", FINAL.getKey(), "aHearingId", "AVenueName");

        assertNotNull(sscsHearingRecording);
        assertEquals("final", sscsHearingRecording.getValue().getHearingType());
        assertEquals("today", sscsHearingRecording.getValue().getHearingDate());
        assertNotNull(sscsHearingRecording.getValue().getUploadDate());
        assertNotNull(sscsHearingRecording.getValue().getRecordings());
        assertEquals(0, sscsHearingRecording.getValue().getRecordings().size());

    }

    @Test
    public void givenNullHearingRecordings_thenZeroSscsRecordings() {
        sscsCaseData.setSscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
            .selectHearingDetails(new DynamicList(
                new DynamicListItem("2222", "good - value 09:00:00 06 Jun 2021"), Collections.EMPTY_LIST))
            .hearingRecording(null)
            .sscsHearingRecordings(null)
            .build());
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        List<SscsHearingRecording> sscsHearingRecordings =
            response.getData().getSscsHearingRecordingCaseData().getSscsHearingRecordings();
        assertNull(sscsHearingRecordings);
    }

    @Test
    public void givenHearingRecordingPresent_thenReturnFileExtension() {
        DocumentLink documentLinks = DocumentLink.builder()
            .documentFilename("test-file.MP4").build();

        assertEquals(".MP4", handler.getFileExtension(documentLinks));
    }

    @Test
    public void givenSingleRecordingPresent_thenReturnFileName() {
        DocumentLink documentLinks = DocumentLink.builder().documentFilename("test-file.MP4").build();
        String fileName = handler.createFileName("test", "06 Jun 2021", 1, ADJOURNED.getValue(), documentLinks);
        assertEquals("Adjourned test 06 Jun 2021.MP4", fileName);
    }

    @Test
    public void givenMultipleRecordingsPresent_thenReturnFileName() {
        DocumentLink documentLinks = DocumentLink.builder().documentFilename("test-file.MP4").build();
        String fileName = handler.createFileName("test", "06 Jun 2021", 3, FINAL.getValue(), documentLinks);
        assertEquals("Final test 06 Jun 2021 (3).MP4", fileName);
    }

}
