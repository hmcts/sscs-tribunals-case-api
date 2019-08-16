package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuefurtherevidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

@RunWith(JUnitParamsRunner.class)
public class ReissueFurtherEvidenceAboutToSubmitHandlerTest {
    private ReissueFurtherEvidenceAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new ReissueFurtherEvidenceAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.REISSUE_FURTHER_EVIDENCE);

        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType("representativeEvidence")
                .evidenceIssued("Yes")
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();
        SscsDocument document2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file2.pdf")
                .documentType("appellantEvidence")
                .evidenceIssued("Yes")
                .documentLink(DocumentLink.builder().documentUrl("url2").build())
                .build()).build();
        List<SscsDocument> sscsDocuments = Arrays.asList(document1, document2);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .sscsDocument(sscsDocuments)
                .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("url1", "file2.pdf - appellantEvidence"), null))
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"url1, file1.pdf - representativeEvidence", "url2, file2.pdf - appellantEvidence"})
    public void setsEvidenceHandledFlagToNoForDocumentSelected(String code, String label) {
        sscsCaseData = sscsCaseData.toBuilder().reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem(code, label), null)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        Optional<String> selectedDocumentValue = response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentLink().getDocumentUrl().equals(code)).map(f -> f.getValue().getEvidenceIssued()).findFirst();
        assertEquals("No", selectedDocumentValue.orElse("Unknown"));
        Optional<String> otherDocumentValue = response.getData().getSscsDocument().stream().filter(f -> !f.getValue().getDocumentLink().getDocumentUrl().equals(code)).map(f -> f.getValue().getEvidenceIssued()).findFirst();
        assertEquals("Yes", otherDocumentValue.orElse("Unknown"));
    }

    @Test
    public void returnAnErrorIfNoSelectedDocument() {
        sscsCaseData = sscsCaseData.toBuilder().reissueFurtherEvidenceDocument(null).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals(1, response.getErrors().size());
        assertEquals("Select a document to re-issue further evidence.", response.getErrors().toArray()[0]);
    }

    @Test
    public void returnsAnErrorIfItCouldNotFindTheSelectedDocumentToReIssueFurtherEvidence() {
        sscsCaseData = sscsCaseData.toBuilder().reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("code", "label"), null)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals(1, response.getErrors().size());
        assertEquals("Could not find the selected document with url 'code' to re-issue further evidence in the appeal with id 'ccdId'.", response.getErrors().toArray()[0]);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback);
    }


}
