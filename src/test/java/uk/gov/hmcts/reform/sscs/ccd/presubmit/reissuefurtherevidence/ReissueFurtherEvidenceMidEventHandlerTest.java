package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuefurtherevidence;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.DWP;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.REPRESENTATIVE;

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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList;

@RunWith(JUnitParamsRunner.class)
public class ReissueFurtherEvidenceMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private ReissueFurtherEvidenceMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new ReissueFurtherEvidenceMidEventHandler();

        when(callback.getEvent()).thenReturn(EventType.REISSUE_FURTHER_EVIDENCE);

        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType("representativeEvidence")
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();
        SscsDocument document2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file2.pdf")
                .documentType("appellantEvidence")
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
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"url1, file1.pdf - representativeEvidence", "url2, file2.pdf - appellantEvidence"})
    public void populateOriginalSenderFromSelectedDocumentToReIssueFurtherEvidence(String code, String label) {
        sscsCaseData = sscsCaseData.toBuilder().reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem(code, label), null)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());

        OriginalSenderItemList expectedSelectedOriginalSender = (label.contains("appellant")) ? APPELLANT : REPRESENTATIVE;

        assertEquals(expectedSelectedOriginalSender.getCode(), response.getData().getOriginalSender().getValue().getCode());
        assertEquals(expectedSelectedOriginalSender.getLabel(), response.getData().getOriginalSender().getValue().getLabel());
        assertEquals(Arrays.asList(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()),
                new DynamicListItem(DWP.getCode(), DWP.getLabel())),
                response.getData().getOriginalSender().getListItems());
    }

    @Test
    public void populatesRepresentativeOriginalSenderIfThereIsARepresentativeOnTheAppeal() {
        sscsCaseData = sscsCaseData.toBuilder()
                .appeal(sscsCaseData.getAppeal().toBuilder()
                        .rep(Representative.builder()
                                .hasRepresentative("yes")
                                .name(Name.builder()
                                        .title("Mrs")
                                        .firstName("Rebecca")
                                        .lastName("Rep")
                                        .build())
                                .build())
                        .build())
                .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("url1", "file1.pdf - representativeEvidence"), null)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(Arrays.asList(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()),
                new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel()),
                new DynamicListItem(DWP.getCode(), DWP.getLabel())
                ),
                response.getData().getOriginalSender().getListItems());
    }

    @Test
    public void returnAnErrorIfNoSelectedDocument() {
        sscsCaseData = sscsCaseData.toBuilder().reissueFurtherEvidenceDocument(null).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Select a document to re-issue further evidence.", response.getErrors().toArray()[0]);
    }

    @Test
    public void returnsAnErrorIfItCouldNotFindTheSelectedDocumentToReIssueFurtherEvidence() {
        sscsCaseData = sscsCaseData.toBuilder().reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("code", "label"), null)).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Could not find the selected document with url 'code' to re-issue further evidence in the appeal with id 'ccdId'.", response.getErrors().toArray()[0]);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }


}
