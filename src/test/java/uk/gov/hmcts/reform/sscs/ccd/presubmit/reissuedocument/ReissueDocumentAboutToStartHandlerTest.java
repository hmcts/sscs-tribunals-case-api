package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuedocument;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class ReissueDocumentAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ReissueDocumentAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        handler = new ReissueDocumentAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.REISSUE_DOCUMENT);

        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file5.pdf")
                .documentType(null)
                .documentLink(DocumentLink.builder().documentUrl("url5").build())
                .build()).build();

        SscsDocument document2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();

        SscsDocument document3 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(DIRECTION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();

        SscsDocument document4 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(FINAL_DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();

        SscsDocument document5 = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentFileName("file1.pdf")
            .documentType(ADJOURNMENT_NOTICE.getValue())
            .documentLink(DocumentLink.builder().documentUrl("url1").build())
            .build()).build();

        List<SscsDocument> sscsDocuments = asList(document1, document2, document3, document4, document5);


        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build())
                .sscsDocument(sscsDocuments)
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
    }

    @Test
    @Parameters({"MID_EVENT", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void populateDocumentDropdownWithAllDocumentTypesAvailableToReissue() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertEquals(4, response.getData().getReissueFurtherEvidence().getReissueFurtherEvidenceDocument().getListItems().size());
        assertEquals(new DynamicListItem("decisionIssued", "Decision Notice"), response.getData().getReissueFurtherEvidence().getReissueFurtherEvidenceDocument().getListItems().get(0));
        assertEquals(new DynamicListItem("directionIssued", "Directions Notice"), response.getData().getReissueFurtherEvidence().getReissueFurtherEvidenceDocument().getListItems().get(1));
        assertEquals(new DynamicListItem("issueFinalDecision", "Final Decision Notice"), response.getData().getReissueFurtherEvidence().getReissueFurtherEvidenceDocument().getListItems().get(2));
        assertEquals(new DynamicListItem("issueAdjournmentNotice", "Adjournment Notice"), response.getData().getReissueFurtherEvidence().getReissueFurtherEvidenceDocument().getListItems().get(3));
        assertNull(response.getData().getReissueFurtherEvidence().getResendToAppellant());
        assertNull(response.getData().getReissueFurtherEvidence().getResendToRepresentative());
    }

    @Test
    public void populateDocumentDropdownWithWelshDocumentTypesAvailableToReissue() {

        SscsWelshDocument documentWelsh1 = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder()
                .documentFileName("file5.pdf")
                .documentType(null)
                .documentLink(DocumentLink.builder().documentUrl("url5").build())
                .build()).build();

        SscsWelshDocument documentWelsh2 = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();

        SscsWelshDocument documentWelsh3 = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(DIRECTION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();

        List<SscsWelshDocument> sscsWelshDocuments = asList(documentWelsh1, documentWelsh2, documentWelsh3);

        sscsCaseData.setSscsWelshDocuments(sscsWelshDocuments);

        sscsCaseData.setLanguagePreferenceWelsh("Yes");
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertEquals(4, response.getData().getReissueFurtherEvidence().getReissueFurtherEvidenceDocument().getListItems().size());
        assertEquals(new DynamicListItem("decisionIssuedWelsh", "Decision Notice"), response.getData().getReissueFurtherEvidence().getReissueFurtherEvidenceDocument().getListItems().get(0));
        assertEquals(new DynamicListItem("directionIssuedWelsh", "Directions Notice"), response.getData().getReissueFurtherEvidence().getReissueFurtherEvidenceDocument().getListItems().get(1));
        assertEquals(new DynamicListItem("issueFinalDecision", "Final Decision Notice"), response.getData().getReissueFurtherEvidence().getReissueFurtherEvidenceDocument().getListItems().get(2));
        assertEquals(new DynamicListItem("issueAdjournmentNotice", "Adjournment Notice"), response.getData().getReissueFurtherEvidence().getReissueFurtherEvidenceDocument().getListItems().get(3));
        assertNull(response.getData().getReissueFurtherEvidence().getResendToAppellant());
        assertNull(response.getData().getReissueFurtherEvidence().getResendToRepresentative());
    }

    @Test
    public void willNotPopulateDocumentDropdownWhenThereAreNoDocumentsAvailableToReissue() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertNull(response.getData().getReissueFurtherEvidence().getReissueFurtherEvidenceDocument());
        assertEquals("There are no documents in this appeal available to reissue.", response.getErrors().iterator().next());
    }
}
