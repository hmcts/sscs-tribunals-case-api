package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadfurtherevidence;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DraftSscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DraftSscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@RunWith(JUnitParamsRunner.class)
public class UploadFurtherEvidenceAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private UploadFurtherEvidenceAboutToSubmitHandler handler;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UploadFurtherEvidenceAboutToSubmitHandler();
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_FURTHER_EVIDENCE);
        sscsCaseData = SscsCaseData.builder().state(State.VALID_APPEAL).appeal(Appeal.builder().build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonUploadFurtherEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    @Parameters({
            "fileName,Please add a file name",
            "documentType,Please select a document type",
            "documentLink,Please upload a file"}
            )
    public void shouldCatchErrorInDraftFurtherEvidenceDocument(String nullField, String expectedErrorMessage) {
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments(nullField);
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is(expectedErrorMessage));
        assertThat(response.getData().getDraftFurtherEvidenceDocuments(), is(draftDocs));
        assertThat(response.getData().getSscsDocument(), is(nullValue()));
    }

    @NotNull
    private List<DraftSscsDocument> getDraftSscsDocuments(String nullField) {
        final DraftSscsDocument doc = DraftSscsDocument.builder().value(DraftSscsDocumentDetails.builder()
                .documentFileName(nullField.contains("fileName") ? null : "File1.pdf")
                .documentType(nullField.contains("documentType") ? null : "documentType")
                .documentLink(nullField.contains("documentLink") ? null : DocumentLink.builder().documentUrl("documentUrl").build())
                .build()).build();
        return unmodifiableList(singletonList(doc));
    }

    @Test
    public void shouldHandleNoDraftUploads() {
        sscsCaseData.setDraftFurtherEvidenceDocuments(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDraftFurtherEvidenceDocuments(), is(nullValue()));
        assertThat(response.getData().getSscsDocument(), is(nullValue()));
    }

    @Test
    public void shouldMoveOneDraftUploadsToSscsDocuments() {
        sscsCaseData.setDraftFurtherEvidenceDocuments(getDraftSscsDocuments(""));
        sscsCaseData.setSscsDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDraftFurtherEvidenceDocuments(), is(nullValue()));
        assertThat(response.getData().getSscsDocument().size(), is(1));
    }

    @Test
    public void shouldMoveTwoDraftUploadsToSscsDocumentsWhenOneSscsDocumentExists() {
        ArrayList<DraftSscsDocument> draftSscsDocuments = new ArrayList<>();
        draftSscsDocuments.addAll(getDraftSscsDocuments(""));
        draftSscsDocuments.addAll(getDraftSscsDocuments(""));
        sscsCaseData.setSscsDocument(unmodifiableList(singletonList(SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build())));
        sscsCaseData.setDraftFurtherEvidenceDocuments(unmodifiableList(draftSscsDocuments));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDraftFurtherEvidenceDocuments(), is(nullValue()));
        assertThat(response.getData().getSscsDocument().size(), is(3));
    }

}