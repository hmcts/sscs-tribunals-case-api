package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuefurtherevidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.DWP;

import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList;

@RunWith(JUnitParamsRunner.class)
public class ReissueFurtherEvidenceAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private ReissueFurtherEvidenceAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;
    private SscsDocument document1;
    private SscsDocument document2;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new ReissueFurtherEvidenceAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.REISSUE_FURTHER_EVIDENCE);

        document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(REPRESENTATIVE_EVIDENCE.getValue())
                .evidenceIssued("Yes")
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();
        document2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file2.pdf")
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("Yes")
                .documentLink(DocumentLink.builder().documentUrl("url2").build())
                .build()).build();
        List<SscsDocument> sscsDocuments = Arrays.asList(document1, document2);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .sscsDocument(sscsDocuments)
                .resendToAppellant("YES")
                .resendToDwp("YES")
                .resendToRepresentative("No")
                .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("url2", "file2.pdf - appellantEvidence"), null))
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
    @Parameters({"url1, file1.pdf - representativeEvidence, APPELLANT", "url2, file2.pdf - appellantEvidence, DWP"})
    public void setsEvidenceHandledFlagToNoForDocumentSelected(String selectedUrl, String selectedLabel, OriginalSenderItemList newSender) {

        sscsCaseData = sscsCaseData.toBuilder()
                .originalSender(new DynamicList(
                        new DynamicListItem(newSender.getCode(), newSender.getLabel()),
                        Arrays.asList(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()),
                                new DynamicListItem(DWP.getCode(), DWP.getLabel()))
                ))
                .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem(selectedUrl, selectedLabel), null)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        Optional<SscsDocumentDetails> selectedDocumentValue = response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentLink().getDocumentUrl().equals(selectedUrl)).map(f -> f.getValue()).findFirst();

        assertEquals("No", selectedDocumentValue.map(SscsDocumentDetails::getEvidenceIssued).orElse("Unknown"));
        assertEquals(newSender.getCode() + "Evidence", selectedDocumentValue.map(SscsDocumentDetails::getDocumentType).orElse("Unknown"));

        DocumentType expectedDocumentTypeOfUnselectedDocument = (selectedUrl.equals("url1")) ? APPELLANT_EVIDENCE : REPRESENTATIVE_EVIDENCE;
        Optional<SscsDocumentDetails> otherDocumentValue = response.getData().getSscsDocument().stream().filter(f -> !f.getValue().getDocumentLink().getDocumentUrl().equals(selectedUrl)).map(f -> f.getValue()).findFirst();
        assertEquals("Yes", otherDocumentValue.map(SscsDocumentDetails::getEvidenceIssued).orElse("Unknown"));
        assertEquals(expectedDocumentTypeOfUnselectedDocument.getValue(), otherDocumentValue.map(SscsDocumentDetails::getDocumentType).orElse("Unknown"));
    }

    @Test
    public void returnAnErrorIfNoSelectedDocument() {
        sscsCaseData = sscsCaseData.toBuilder().reissueFurtherEvidenceDocument(null).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Select a document to re-issue further evidence.", response.getErrors().toArray()[0]);
    }

    @Test
    public void doesNotReturnAnErrorIfNoSelectedOriginalSender() {
        sscsCaseData = sscsCaseData.toBuilder().originalSender(null).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertEquals(document2.toBuilder().value(document2.getValue().toBuilder().evidenceIssued("No").build()).build(), response.getData().getSscsDocument().get(1));
        assertEquals(document1, response.getData().getSscsDocument().get(0));
    }

    @Test
    public void returnsAnErrorIfThereIsNoPartySelectedToReIssueFurtherEvidence() {
        sscsCaseData = sscsCaseData.toBuilder().resendToRepresentative("NO").resendToDwp("NO").resendToAppellant("NO").build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Select a party to reissue the further evidence.", response.getErrors().toArray()[0]);
    }

    @Test
    public void returnsAnErrorIfItCouldNotFindTheSelectedDocumentToReIssueFurtherEvidence() {
        sscsCaseData = sscsCaseData.toBuilder().reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("code", "label"), null)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Could not find the selected document with url 'code' to re-issue further evidence in the appeal with id 'ccdId'.", response.getErrors().toArray()[0]);
    }

    @Test
    public void returnsAnErrorIfReIssuedToRepresentativeWhenThereIsNoRepOnTheAppealToReIssueFurtherEvidence() {
        sscsCaseData = sscsCaseData.toBuilder().resendToRepresentative("YES").build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Cannot re-issue to the representative as there is no representative on the appeal.", response.getErrors().toArray()[0]);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }


}
