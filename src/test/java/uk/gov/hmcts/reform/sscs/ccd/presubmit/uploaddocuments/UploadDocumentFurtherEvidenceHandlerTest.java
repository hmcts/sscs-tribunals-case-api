package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.FileUploadScenario.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.exceptions.PdfPasswordException;

@RunWith(JUnitParamsRunner.class)
public class UploadDocumentFurtherEvidenceHandlerTest extends BaseHandlerTest {

    @Mock
    private FooterService footerService;

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String UPLOAD_DOCUMENT_FE_CALLBACK_JSON = "uploaddocument/uploadDocumentFECallback.json";
    private static final String UPLOAD_AUDIO_VIDEO_DOCUMENT_FE_CALLBACK_JSON = "uploaddocument/uploadAudioVideoDocumentFECallback.json";

    UploadDocumentFurtherEvidenceHandler handler;

    @Before
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        handler = new UploadDocumentFurtherEvidenceHandler(true, footerService);

        try {
            when(footerService.isReadablePdf(any())).thenReturn(true);
        } catch (Exception e) {
            // continue
        }

        super.setUp();
    }

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,Medical evidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,Other evidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,appellantEvidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,representativeEvidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,sscs1,dl6,true,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,Decision Notice,dl6,true,false",
        "ABOUT_TO_START,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,representativeEvidence,appellantEvidence,false, true",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,Medical evidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,Other evidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,appellantEvidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,representativeEvidence,appellantEvidence,true, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,dl6,sscs1,true,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,appealCreated,DWP response,dl6,true,false",
        "ABOUT_TO_SUBMIT,APPEAL_RECEIVED,withDwp,representativeEvidence,appellantEvidence,false,true",
        "ABOUT_TO_SUBMIT,APPEAL_RECEIVED,withDwp,dl6,appellantEvidence,false, true",
        "null,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,appellantEvidence,appellantEvidence,false, true",
        "ABOUT_TO_SUBMIT,null,withDwp,appellantEvidence,appellantEvidence,false, false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,,true,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,nullSscsDocuments,appellantEvidence,true,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,nullDocumentType,appellantEvidence,true,false",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,nullSscsDocument,appellantEvidence,true,false"
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, String state,
                          @Nullable String documentType,@Nullable String documentType2, boolean expectedResult,
                          boolean expectToInitDrafts)
        throws IOException {
        Callback<SscsCaseData> actualCallback = buildTestCallbackGivenData(eventType, state,
            documentType, documentType2, UPLOAD_DOCUMENT_FE_CALLBACK_JSON);
        boolean actualResult = handler.canHandle(callbackType, actualCallback);

        assertEquals(expectedResult, actualResult);

        if (expectToInitDrafts) {
            assertNull(actualCallback.getCaseDetails().getCaseData().getDraftSscsFurtherEvidenceDocument());
        }
    }

    @Test
    public void handleHappyPathWhenAllFieldsAreProvided() throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,"appealCreated",
            "representativeEvidence", "appellantEvidence", UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, callback,
            USER_AUTHORISATION);

        assertThatJson(actualCaseData).isEqualTo(getExpectedResponse());
        assertEquals("feReceived", actualCaseData.getData().getDwpState());
        assertNull(actualCaseData.getData().getDraftSscsFurtherEvidenceDocument());
    }

    @Test
    public void handleHappyPathWhenAudioVideoFileUploaded() throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,"appealCreated",
                DocumentType.OTHER_EVIDENCE.getId(), DocumentType.APPELLANT_EVIDENCE.getId(), UPLOAD_AUDIO_VIDEO_DOCUMENT_FE_CALLBACK_JSON);

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, callback,
                USER_AUTHORISATION);

        assertEquals(1, actualCaseData.getData().getScannedDocuments().size());
        assertEquals("reps-some-name.pdf", actualCaseData.getData().getScannedDocuments().get(0).getValue().getFileName());
        assertEquals("other", actualCaseData.getData().getScannedDocuments().get(0).getValue().getType());
        assertEquals(1, actualCaseData.getData().getAudioVideoEvidence().size());
        assertEquals("appellant-some-name.mp3", actualCaseData.getData().getAudioVideoEvidence().get(0).getValue().getFileName());
        assertEquals(DocumentType.APPELLANT_EVIDENCE.getId(), actualCaseData.getData().getAudioVideoEvidence().get(0).getValue().getDocumentType());
        assertEquals(AudioVideoUploadParty.CTSC, actualCaseData.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), actualCaseData.getData().getInterlocReviewState());
        assertEquals("feReceived", actualCaseData.getData().getDwpState());
        assertNull(actualCaseData.getData().getDraftSscsFurtherEvidenceDocument());
    }

    @Test
    public void dwpStateIsNotSetWhenStateIsWithDwp() throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,"withDwp",
                "representativeEvidence", "appellantEvidence", UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, callback,
                USER_AUTHORISATION);

        assertNull(actualCaseData.getData().getDwpState());
    }

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,representativeEvidence,appellantEvidence,FILE_UPLOAD_IS_EMPTY",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,representativeEvidence,appellantEvidence,FILE_UPLOAD_IS_NULL",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,appellantEvidence,null",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,representativeEvidence,null",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,,null",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,,FILE_UPLOAD_IS_NULL"
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
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());
        long numberOfExpectedError = actualResponse.getErrors().stream()
            .filter(error -> error.equalsIgnoreCase("You need to provide a file and a document type"))
            .count();
        assertEquals(1, numberOfExpectedError);
    }

    @Test
    public void handleDocumentUploadWhereUploadedFileIsNotAPdf() throws IOException {
        UploadDocumentFurtherEvidenceHandler handler = new UploadDocumentFurtherEvidenceHandler(false, footerService);
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
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());
        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("You need to upload PDF documents only"))
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
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());
        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("You need to upload PDF,MP3 or MP4 file only"))
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

        when(footerService.isReadablePdf(any())).thenThrow(new Exception());

        callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(draftDocuments);
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

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

        when(footerService.isReadablePdf(any())).thenThrow(new PdfPasswordException("not allowed bro"));

        callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(draftDocuments);
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());

        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("Your PDF Document cannot be password protected."))
                .count();
        assertEquals(1, numberOfExpectedError);
    }

    private String getExpectedResponse() throws IOException {
        String expectedResponse = fetchData("uploaddocument/" + "expectedUploadDocumentFECallbackResponse.json");
        return expectedResponse.replace("DOCUMENT_DATE_ADDED_PLACEHOLDER", LocalDate.now().atStartOfDay().toString());
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_START,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp",
        "ABOUT_TO_SUBMIT,null,withDwp",
        "null,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType,
                                          @Nullable String state)
        throws IOException {
        handler.handle(callbackType, buildTestCallbackGivenData(eventType, state,
            "representativeEvidence", "appellantEvidence",
            UPLOAD_DOCUMENT_FE_CALLBACK_JSON), USER_AUTHORISATION);
    }
}
