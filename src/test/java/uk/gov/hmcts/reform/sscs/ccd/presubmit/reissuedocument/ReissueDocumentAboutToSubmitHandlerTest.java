package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuedocument;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.Arrays;
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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.reissueartifact.ReissueArtifactHandlerTest;

@RunWith(JUnitParamsRunner.class)
public class ReissueDocumentAboutToSubmitHandlerTest extends ReissueArtifactHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private ReissueDocumentAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;
    private SscsDocument document1;
    private SscsDocument document2;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ReissueDocumentAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.REISSUE_DOCUMENT);

        document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(DECISION_NOTICE.getValue())
                .evidenceIssued("Yes")
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();
        document2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file2.pdf")
                .documentType(DIRECTION_NOTICE.getValue())
                .evidenceIssued("Yes")
                .documentLink(DocumentLink.builder().documentUrl("url2").build())
                .build()).build();
        SscsDocument document3 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file3.pdf")
                .documentType(FINAL_DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();
        SscsDocument document4 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file4.pdf")
                .documentType(ADJOURNMENT_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();
        List<SscsDocument> sscsDocuments = Arrays.asList(document1, document2, document3, document4);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .sscsDocument(sscsDocuments)
                .reissueArtifactUi(ReissueArtifactUi.builder()
                        .resendToAppellant(YesNo.YES)
                        .resendToDwp(YesNo.YES)
                        .resendToRepresentative(YesNo.NO)
                        .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("url2", "file2.pdf - appellantEvidence"), null)).build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void returnsAnErrorIfReissuedToRepresentativeWhenThereIsNoRepOnTheAppealToReissueDocument() {
        ReissueArtifactUi reissueArtifactUi = sscsCaseData.getReissueArtifactUi();
        reissueArtifactUi.setResendToAppellant(YesNo.NO);
        reissueArtifactUi.setResendToRepresentative(YesNo.YES);
        reissueArtifactUi.setResendToDwp(YesNo.NO);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Cannot re-issue to the representative as there is no representative on the appeal.", response.getErrors().toArray()[0]);
    }

    @Test
    public void returnsAnErrorIfNoPartySelectedForReissue() {
        ReissueArtifactUi reissueArtifactUi = sscsCaseData.getReissueArtifactUi();
        reissueArtifactUi.setResendToAppellant(YesNo.NO);
        reissueArtifactUi.setResendToRepresentative(YesNo.NO);
        reissueArtifactUi.setResendToDwp(YesNo.NO);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Select a party to reissue.", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenCaseWithMultipleOtherParties_thenBuildTheOtherPartyOptionsSection() {

        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithAppointeeAndRep("1", "", "3"),
                buildOtherPartyWithAppointeeAndRep("4", "5", "6")));

        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
