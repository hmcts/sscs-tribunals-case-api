package uk.gov.hmcts.reform.sscs.callback;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class UploadDocumentFurtherEvidenceIt extends AbstractEventIt {

    @MockBean
    private FooterService footerService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setup() throws IOException {
        openMocks(this);
        super.setup();

        Hearing hearing1 = Hearing.builder().value(
                HearingDetails.builder().hearingId("1").venue(Venue.builder().name("venue 1 name").build())
                        .hearingDate("2021-01-20")
                        .time("15:15").build()).build();
        Hearing hearing2 = Hearing.builder().value(
                HearingDetails.builder().hearingId("2").venue(Venue.builder().name("venue 2 name").build())
                        .hearingDate("2021-02-20")
                        .time("15:15").build()).build();
        Hearing hearing3 = Hearing.builder().value(
                HearingDetails.builder().hearingId("3").venue(Venue.builder().name("venue 3 name").build())
                        .hearingDate("2021-03-20")
                        .time("15:15").build()).build();
        Hearing hearing4 = Hearing.builder().value(
                HearingDetails.builder().hearingId("4").venue(Venue.builder().name("venue 4 name").build())
                        .hearingDate("2021-03-20")
                        .time("15:15").build()).build();
        Hearing hearing5 = Hearing.builder().value(
                HearingDetails.builder().hearingId("5").venue(Venue.builder().name("venue 5 name").build())
                        .hearingDate("2021-03-20")
                        .time("15:15").build()).build();

        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("1").build()).build();
        SscsHearingRecording recording2 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("2").build()).build();
        SscsHearingRecording recording3 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("3").build()).build();
        SscsHearingRecording recording4 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("4").build()).build();
        SscsHearingRecording recording5 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder().hearingId("5").build()).build();

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1")
                .state(State.WITH_DWP)
                .interlocReviewState(InterlocReviewState.REVIEW_BY_TCW)
                .hearings(Arrays.asList(hearing1, hearing2, hearing3, hearing4, hearing5))
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().sscsHearingRecordings(Arrays.asList(recording1, recording2, recording3, recording4, recording5)).build())
                .appeal(Appeal.builder()
                        .mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build())
                        .build()).build();


        setJson(sscsCaseData, UPLOAD_DOCUMENT_FURTHER_EVIDENCE);
        given(footerService.isReadablePdf(any())).willReturn(PdfState.OK);
    }

    @Test
    public void midEventGivenOneHearingRecordingRequestDocumentIsUploaded_thenShowHearingsPageAndSetPartiesListForAppellantAndRep() throws Exception {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("yes").build());
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(
                SscsFurtherEvidenceDocDetails.builder().documentType(REQUEST_FOR_HEARING_RECORDING.getId()).documentLink(DocumentLink.builder().documentUrl("url.com").documentFilename("file.pdf").build()).build()).build();
        sscsCaseData.setDraftSscsFurtherEvidenceDocument(List.of(furtherEvidenceDoc));
        setJson(sscsCaseData, UPLOAD_DOCUMENT_FURTHER_EVIDENCE);

        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(MID_EVENT);

        assertThat(response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage(), is(YesNo.YES));
        assertEquals(2, response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().size());
        assertEquals("appellant", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(0).getCode());
        assertEquals("representative", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(1).getCode());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void midEventGivenOneHearingRecordingRequestDocumentIsUploadedAndRepIsSelectedToRequestHearing_thenBuildHearingRequestUi() throws Exception {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("yes").build());
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(
                SscsFurtherEvidenceDocDetails.builder().documentType(REQUEST_FOR_HEARING_RECORDING.getId()).documentLink(DocumentLink.builder().documentUrl("url.com").documentFilename("file.pdf").build()).build()).build();
        sscsCaseData.setDraftSscsFurtherEvidenceDocument(List.of(furtherEvidenceDoc));
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestingParty(new DynamicList("representative"));

        setJson(sscsCaseData, UPLOAD_DOCUMENT_FURTHER_EVIDENCE);

        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(MID_EVENT);

        assertThat(response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage(), is(YesNo.YES));
        assertEquals(5, response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().size());
        assertEquals("5", response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().get(0).getCode());
        assertEquals("No hearing recordings have been released to Representative on this case", response.getData().getSscsHearingRecordingCaseData().getReleasedHearingsTextList());
        assertEquals("There are no outstanding Representative hearing recording requests on this case", response.getData().getSscsHearingRecordingCaseData().getRequestedHearingsTextList());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void midEventGivenOneHearingRecordingRequestDocumentIsUploadedAndRepIsSelectedToRequestHearingAndOutstandingHearingRecordingExists_thenBuildHearingRequestUi() throws Exception {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("yes").build());
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(
                SscsFurtherEvidenceDocDetails.builder().documentType(REQUEST_FOR_HEARING_RECORDING.getId()).documentLink(DocumentLink.builder().documentUrl("url.com").documentFilename("file.pdf").build()).build()).build();
        sscsCaseData.setDraftSscsFurtherEvidenceDocument(List.of(furtherEvidenceDoc));
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestingParty(new DynamicList("representative"));

        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(Collections.singletonList(
                HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty("representative").sscsHearingRecording(
                        SscsHearingRecordingDetails.builder().hearingId("1").hearingDate("12-12-2021").venue("The Court").build()).build()).build()));

        setJson(sscsCaseData, UPLOAD_DOCUMENT_FURTHER_EVIDENCE);

        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(MID_EVENT);

        assertThat(response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage(), is(YesNo.YES));
        assertEquals(4, response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().size());
        assertEquals("5", response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().get(0).getCode());
        assertEquals("No hearing recordings have been released to Representative on this case", response.getData().getSscsHearingRecordingCaseData().getReleasedHearingsTextList());
        assertEquals("The Court 12-12-2021", response.getData().getSscsHearingRecordingCaseData().getRequestedHearingsTextList());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void midEventGivenOneHearingRecordingRequestDocumentIsUploadedAndRepIsSelectedToRequestHearingAndOutstandingHearingRecordingExistsAndReleasedHearingRecordingExists_thenBuildHearingRequestUi() throws Exception {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("yes").build());
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(
                SscsFurtherEvidenceDocDetails.builder().documentType(REQUEST_FOR_HEARING_RECORDING.getId()).documentLink(DocumentLink.builder().documentUrl("url.com").documentFilename("file.pdf").build()).build()).build();
        sscsCaseData.setDraftSscsFurtherEvidenceDocument(List.of(furtherEvidenceDoc));
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestingParty(new DynamicList("representative"));

        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(Collections.singletonList(
                HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty("representative").sscsHearingRecording(
                        SscsHearingRecordingDetails.builder().hearingId("1").hearingDate("12-12-2021").venue("The Court").build()).build()).build()));

        sscsCaseData.getSscsHearingRecordingCaseData().setCitizenReleasedHearings(Collections.singletonList(
                HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty("representative").sscsHearingRecording(
                        SscsHearingRecordingDetails.builder().hearingId("2").hearingDate("21-12-2021").venue("The Basement").build()).build()).build()));

        setJson(sscsCaseData, UPLOAD_DOCUMENT_FURTHER_EVIDENCE);

        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(MID_EVENT);

        assertThat(response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage(), is(YesNo.YES));
        assertEquals(3, response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().size());
        assertEquals("5", response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().get(0).getCode());
        assertEquals("The Basement 21-12-2021", response.getData().getSscsHearingRecordingCaseData().getReleasedHearingsTextList());
        assertEquals("The Court 12-12-2021", response.getData().getSscsHearingRecordingCaseData().getRequestedHearingsTextList());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void midEventGivenOneHearingRecordingRequestDocumentIsUploadedAndRepIsSelectedToRequestHearingAndMultipleOutstandingHearingRecordingExistsAndMultipleReleasedHearingRecordingExists_thenBuildHearingRequestUi() throws Exception {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("yes").build());
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(
                SscsFurtherEvidenceDocDetails.builder().documentType(REQUEST_FOR_HEARING_RECORDING.getId()).documentLink(DocumentLink.builder().documentUrl("url.com").documentFilename("file.pdf").build()).build()).build();
        sscsCaseData.setDraftSscsFurtherEvidenceDocument(List.of(furtherEvidenceDoc));
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestingParty(new DynamicList("representative"));

        HearingRecordingRequest hearingRecordingRequest1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty("representative").sscsHearingRecording(
                SscsHearingRecordingDetails.builder().hearingId("1").hearingDate("12-12-2021").venue("The Court").build()).build()).build();

        HearingRecordingRequest hearingRecordingRequest2 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty("representative").sscsHearingRecording(
                SscsHearingRecordingDetails.builder().hearingId("2").hearingDate("13-12-2021").venue("The Post Office").build()).build()).build();

        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(Arrays.asList(hearingRecordingRequest1, hearingRecordingRequest2));

        HearingRecordingRequest hearingRecordingRequest3 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty("representative").sscsHearingRecording(
                SscsHearingRecordingDetails.builder().hearingId("3").hearingDate("21-12-2021").venue("The Basement").build()).build()).build();

        HearingRecordingRequest hearingRecordingRequest4 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty("representative").sscsHearingRecording(
                SscsHearingRecordingDetails.builder().hearingId("4").hearingDate("22-12-2021").venue("The Old Bailey").build()).build()).build();

        sscsCaseData.getSscsHearingRecordingCaseData().setCitizenReleasedHearings(Arrays.asList(hearingRecordingRequest3, hearingRecordingRequest4));

        setJson(sscsCaseData, UPLOAD_DOCUMENT_FURTHER_EVIDENCE);

        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(MID_EVENT);

        assertThat(response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage(), is(YesNo.YES));
        assertEquals(1, response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().size());
        assertEquals("5", response.getData().getSscsHearingRecordingCaseData().getRequestableHearingDetails().getListItems().get(0).getCode());
        assertEquals("The Basement 21-12-2021, The Old Bailey 22-12-2021", response.getData().getSscsHearingRecordingCaseData().getReleasedHearingsTextList());
        assertEquals("The Court 12-12-2021, The Post Office 13-12-2021", response.getData().getSscsHearingRecordingCaseData().getRequestedHearingsTextList());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    @Parameters({"audio.mp3","video.mp4"})
    public void aboutToSubmitHandleHappyPathWhenAudioVideoFileUploaded(String fileName) throws Exception {
        DocumentLink documentLink = DocumentLink.builder().documentFilename(fileName).documentBinaryUrl("http://doc/222").documentUrl("http://doc/skw").build();
        SscsFurtherEvidenceDoc representativeEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder().documentFileName(fileName).documentLink(documentLink).documentType(APPELLANT_EVIDENCE.getId()).build()).build();
        sscsCaseData.setDraftSscsFurtherEvidenceDocument(List.of(representativeEvidenceDoc));


        setJson(sscsCaseData, UPLOAD_DOCUMENT_FURTHER_EVIDENCE);

        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(ABOUT_TO_SUBMIT);


        assertEquals(1, response.getData().getAudioVideoEvidence().size());
        assertEquals(fileName, response.getData().getAudioVideoEvidence().get(0).getValue().getFileName());
        assertEquals(UploadParty.CTSC, response.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW, response.getData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE, response.getData().getInterlocReferralReason());
        assertNull(response.getData().getDwpState());
        assertNull(response.getData().getDraftSscsFurtherEvidenceDocument());
        assertEquals(YesNo.YES, response.getData().getHasUnprocessedAudioVideoEvidence());
    }


}
