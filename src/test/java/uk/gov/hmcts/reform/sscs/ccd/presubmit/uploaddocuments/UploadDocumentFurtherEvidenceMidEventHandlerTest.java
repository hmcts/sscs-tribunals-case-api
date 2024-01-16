package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static java.lang.String.format;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.REQUEST_FOR_HEARING_RECORDING;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.FileUploadScenario.FILE_UPLOAD_IS_EMPTY;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.FileUploadScenario.FILE_UPLOAD_IS_NULL;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.HearingRecordingRequestService;

@RunWith(JUnitParamsRunner.class)
public class UploadDocumentFurtherEvidenceMidEventHandlerTest extends BaseHandlerTest {

    private static final String UPLOAD_DOCUMENT_FE_CALLBACK_JSON = "uploaddocument/uploadDocumentFECallback.json";

    private static final String USER_AUTHORISATION = "Bearer token";
    private UploadDocumentFurtherEvidenceMidEventHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private HearingRecordingRequestService hearingRecordingRequestService;
    private SscsCaseData sscsCaseData;

    @Mock
    private FooterService footerService;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new UploadDocumentFurtherEvidenceMidEventHandler(hearingRecordingRequestService, footerService);
        sscsCaseData = SscsCaseData.builder()
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
                        .sscsHearingRecordings(List.of(recording(1)))
                        .build())
                .appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(footerService.isReadablePdf(any())).thenReturn(PdfState.OK);

        super.setUp();
    }

    static SscsHearingRecording recording(int hearingId) {
        return SscsHearingRecording.builder()
                .value(SscsHearingRecordingDetails.builder()
                        .hearingType("Adjourned")
                        .uploadDate(LocalDate.now().toString())
                        .recordings(List.of(HearingRecordingDetails.builder()
                                .value(DocumentLink.builder().documentBinaryUrl(format("https://example/%s", hearingId)).build())
                                .build()))
                        .build())
                .build();
    }

    @Test
    public void givenAValidHandleAndEventType_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "ABOUT_TO_START", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenNonUploadDocumentFurtherEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenUploadDocumentFurtherEvidenceEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE);
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenOneHearingRecordingRequestDocumentIsUploaded_thenShowHearingsPageAndSetPartiesListForAppellant() {
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder()
                .documentType(REQUEST_FOR_HEARING_RECORDING.getId()).documentLink(DocumentLink.builder().documentUrl("url.com").documentFilename("file.pdf").build()).build()).build();

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(Collections.singletonList(furtherEvidenceDoc));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(YES, response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage());
        assertEquals(1, response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().size());
        assertEquals("appellant", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(0).getCode());
    }

    @Test
    public void givenOneHearingRecordingRequestDocumentIsUploaded_thenShowHearingsPageAndSetPartiesListForAppellantAndRep() {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("yes").build());
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder()
                .documentType(REQUEST_FOR_HEARING_RECORDING.getId()).documentLink(DocumentLink.builder().documentUrl("url.com").documentFilename("file.pdf").build()).build()).build();

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(Collections.singletonList(furtherEvidenceDoc));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(YES, response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage());
        assertEquals(2, response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().size());
        assertEquals("appellant", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(0).getCode());
        assertEquals("representative", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(1).getCode());
    }

    @Test
    public void givenOneHearingRecordingRequestDocumentIsUploaded_thenShowHearingsPageAndSetPartiesListForAppellantAndJointParty() {
        sscsCaseData.getJointParty().setHasJointParty(YES);
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder()
                .documentType(REQUEST_FOR_HEARING_RECORDING.getId()).documentLink(DocumentLink.builder().documentUrl("url.com").documentFilename("file.pdf").build()).build()).build();

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(Collections.singletonList(furtherEvidenceDoc));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(YES, response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage());
        assertEquals(2, response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().size());
        assertEquals("appellant", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(0).getCode());
        assertEquals("jointParty", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(1).getCode());
    }

    @Test
    public void givenMoreThanOneHearingRecordingRequestDocumentIsUploaded_thenShowError() {
        SscsFurtherEvidenceDoc furtherEvidenceDoc1 = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder()
                .documentType(REQUEST_FOR_HEARING_RECORDING.getId()).documentLink(DocumentLink.builder().documentUrl("url.com").documentFilename("file.pdf").build()).build()).build();
        SscsFurtherEvidenceDoc furtherEvidenceDoc2 = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder()
                .documentType(REQUEST_FOR_HEARING_RECORDING.getId()).documentLink(DocumentLink.builder().documentUrl("url.com").documentFilename("file.pdf").build()).build()).build();

        List<SscsFurtherEvidenceDoc> sscsFurtherEvidenceDocList = new ArrayList<>();
        sscsFurtherEvidenceDocList.add(furtherEvidenceDoc1);
        sscsFurtherEvidenceDocList.add(furtherEvidenceDoc2);

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(sscsFurtherEvidenceDocList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Only one request for hearing recording can be submitted at a time"));
    }

    @Test
    public void givenNoHearingRecordingRequestDocumentIsUploaded_thenDoNotShowHearingsPag() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage());
    }

    @Test
    @Parameters({"APPELLANT", "REPRESENTATIVE", "JOINT_PARTY"})
    public void givenARequestHearingRecordingEvent_thenBuildTheUi(PartyItemList partyItem) {
        PreSubmitCallbackResponse response = new PreSubmitCallbackResponse(callback.getCaseDetails().getCaseData());

        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder()
                .documentType(REQUEST_FOR_HEARING_RECORDING.getId()).documentLink(DocumentLink.builder().documentUrl("url.com").documentFilename("file.pdf").build()).build()).build();
        sscsCaseData.setDraftSscsFurtherEvidenceDocument(Collections.singletonList(furtherEvidenceDoc));
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestingParty(new DynamicList(partyItem.getCode()));

        when(hearingRecordingRequestService.buildHearingRecordingUi(any(PreSubmitCallbackResponse.class), eq(partyItem)))
                .thenReturn(response);

        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verify(hearingRecordingRequestService).buildHearingRecordingUi(any(PreSubmitCallbackResponse.class), eq(partyItem));
    }

    @Test
    @Parameters({
        "MID_EVENT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,representativeEvidence,appellantEvidence,FILE_UPLOAD_IS_EMPTY",
        "MID_EVENT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,representativeEvidence,appellantEvidence,FILE_UPLOAD_IS_NULL",
        "MID_EVENT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,appellantEvidence,null",
        "MID_EVENT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,representativeEvidence,null",
        "MID_EVENT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,,null",
        "MID_EVENT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,,FILE_UPLOAD_IS_NULL"
    })
    public void handleErrorScenariosWhenSomeFieldsAreNotProvided(@Nullable CallbackType callbackType,
                                                                 @Nullable EventType eventType, String state,
                                                                 @Nullable String documentType,
                                                                 @Nullable String documentType2,
                                                                 @Nullable FileUploadScenario fileUploadScenario)
            throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(eventType,state, documentType, documentType2,
                UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        if (FILE_UPLOAD_IS_EMPTY.equals(fileUploadScenario)) {
            callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(Collections.emptyList());
        }
        if (FILE_UPLOAD_IS_NULL.equals(fileUploadScenario)) {
            callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(callbackType, callback,
                USER_AUTHORISATION);

        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("You need to provide a file and a document type"))
                .count();
        assertEquals(1, numberOfExpectedError);
    }

    @Test
    public void handleDocumentUploadWhereUploadedFileIsNotAValid() throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,
                "withDwp",
                "representativeEvidence", "appellantEvidence",
                UPLOAD_DOCUMENT_FE_CALLBACK_JSON);
        List<SscsFurtherEvidenceDoc> draftDocuments = Collections.singletonList(SscsFurtherEvidenceDoc.builder()
                .value(SscsFurtherEvidenceDocDetails.builder()
                        .documentFileName("word.docx")
                        .documentType("representativeEvidence")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("http://dm-store:5005/documents/abe3b75a-7a72-4e68-b136-4349b7d4f655")
                                .documentFilename("word.docx").build())
                        .build())
                .build());
        callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(draftDocuments);
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(CallbackType.MID_EVENT, callback, USER_AUTHORISATION);
        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());
        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("You need to upload PDF, MP3 or MP4 file only"))
                .count();
        assertEquals(1, numberOfExpectedError);
    }


    @Test
    public void handleDocumentUploadWhereUploadedFileIsNotAReadablePdf() throws Exception {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,
                "withDwp",
                "representativeEvidence", "appellantEvidence",
                UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        List<SscsFurtherEvidenceDoc> draftDocuments = Collections.singletonList(SscsFurtherEvidenceDoc.builder()
                .value(SscsFurtherEvidenceDocDetails.builder()
                        .documentFileName("badPdf.pdf")
                        .documentType("representativeEvidence")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("http://dm-store:5005/documents/abe3b75a-7a72-4e68-b136-4349b7d4f655")
                                .documentFilename("bdPdf.pdf").build())
                        .build())
                .build());

        when(footerService.isReadablePdf(any())).thenReturn(PdfState.UNREADABLE);

        callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(draftDocuments);
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(CallbackType.MID_EVENT, callback, USER_AUTHORISATION);

        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());

        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("Your PDF Document is not readable."))
                .count();
        assertEquals(1, numberOfExpectedError);
    }

    @Test
    public void handleDocumentUploadWhereUploadedFileIsPasswordProtectedPdf() throws Exception {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,
                "withDwp",
                "representativeEvidence", "appellantEvidence",
                UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        List<SscsFurtherEvidenceDoc> draftDocuments = Collections.singletonList(SscsFurtherEvidenceDoc.builder()
                .value(SscsFurtherEvidenceDocDetails.builder()
                        .documentFileName("badPdf.pdf")
                        .documentType("representativeEvidence")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("http://dm-store:5005/documents/abe3b75a-7a72-4e68-b136-4349b7d4f655")
                                .documentFilename("bdPdf.pdf").build())
                        .build())
                .build());

        when(footerService.isReadablePdf(any())).thenReturn(PdfState.PASSWORD_ENCRYPTED);

        callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(draftDocuments);
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(CallbackType.MID_EVENT, callback, USER_AUTHORISATION);

        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());

        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("Your PDF Document cannot be password protected."))
                .count();
        assertEquals(1, numberOfExpectedError);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }

}
