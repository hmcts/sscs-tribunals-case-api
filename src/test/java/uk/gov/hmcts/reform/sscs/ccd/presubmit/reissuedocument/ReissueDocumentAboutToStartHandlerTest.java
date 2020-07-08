package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuedocument;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.Arrays;
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

@RunWith(JUnitParamsRunner.class)
public class ReissueDocumentAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private ReissueDocumentAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new ReissueDocumentAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.REISSUE_DOCUMENT);

        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();

        SscsDocument document2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(DIRECTION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();

        SscsDocument document3 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(FINAL_DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();
        List<SscsDocument> sscsDocuments = Arrays.asList(document1, document2, document3);
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
    public void populateDocumentDropdownWithAllDocumentTypesAvailableToReissu() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertEquals(3, response.getData().getReissueFurtherEvidenceDocument().getListItems().size());
        assertEquals(new DynamicListItem("decisionIssued", "Decision Notice"), response.getData().getReissueFurtherEvidenceDocument().getListItems().get(0));
        assertEquals(new DynamicListItem("directionIssued", "Directions Notice"), response.getData().getReissueFurtherEvidenceDocument().getListItems().get(1));
        assertEquals(new DynamicListItem("issueFinalDecision", "Final Decision Notice"), response.getData().getReissueFurtherEvidenceDocument().getListItems().get(2));
        assertNull(response.getData().getResendToAppellant());
        assertNull(response.getData().getResendToRepresentative());
    }

    @Test
    public void willNotPopulateDocumentDropdownWhenThereAreNoDocumentsAvailableToReissue() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertNull(response.getData().getReissueFurtherEvidenceDocument());
        assertEquals("There are no documents in this appeal available to reissue.", response.getErrors().iterator().next());
    }
}
