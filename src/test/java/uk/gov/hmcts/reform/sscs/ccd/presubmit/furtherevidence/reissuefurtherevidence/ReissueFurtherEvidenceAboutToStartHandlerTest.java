package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.reissuefurtherevidence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

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
public class ReissueFurtherEvidenceAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private ReissueFurtherEvidenceAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ReissueFurtherEvidenceAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.REISSUE_FURTHER_EVIDENCE);

        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentFileName("file1.pdf")
            .documentType(APPELLANT_EVIDENCE.getValue())
            .documentLink(DocumentLink.builder().documentUrl("url1").build())
            .build()).build();
        SscsDocument document2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentFileName("file2.pdf")
            .documentType(REPRESENTATIVE_EVIDENCE.getValue())
            .documentLink(DocumentLink.builder().documentUrl("url2").build())
            .build()).build();
        SscsDocument document3 = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentFileName("file3.pdf")
            .documentType(DWP_EVIDENCE.getValue())
            .documentLink(DocumentLink.builder().documentUrl("url3").build())
            .build()).build();
        List<SscsDocument> sscsDocuments = Arrays.asList(document1, document2, document3);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build())
            .sscsDocument(sscsDocuments)
            .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
    }

    @Test
    @Parameters({"MID_EVENT", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void populateDocumentDropdownWithAllSscsDocuments() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertEquals(3, response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getListItems().size());
        assertEquals(new DynamicListItem("url1", "file1.pdf -  Appellant evidence"), response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getListItems().get(0));
        assertEquals(new DynamicListItem("url2", "file2.pdf -  Representative evidence"), response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getListItems().get(1));
        assertEquals(new DynamicListItem("url3", "file3.pdf -  Dwp evidence"), response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getListItems().get(2));
        assertNull(response.getData().getOriginalSender());
    }

    @Test
    public void willNotPopulateDocumentDropdownWhenThereAreNoSscsDocuments() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertNull(response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument());
        assertEquals("There are no evidence documents in the appeal. Cannot reissue further evidence.", response.getErrors().iterator().next());
    }

    @Test
    public void willNotPopulateDocumentDropdownWhenThereAreNoSscsDocumentsOfDocumentTypeEvidence() {
        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentFileName("file1.pdf")
            .documentType(DL6.getValue())
            .documentLink(DocumentLink.builder().documentUrl("url1").build())
            .build()).build();

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).sscsDocument(Collections.singletonList(document1)).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertNull(response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument());
        assertEquals("There are no evidence documents in the appeal. Cannot reissue further evidence.", response.getErrors().iterator().next());
    }

    @Test
    public void givenCaseWithMultipleOtherParties_thenBuildTheOtherPartyOptionsSection() {

        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithAppointeeAndRep("1", "2", "3"),
                buildOtherPartyWithAppointeeAndRep("4", "5", "6")));

        final PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().size(), is(6));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(0).getValue().getOtherPartyOptionName(), is("Peter Parker"));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(0).getValue().getOtherPartyOptionId(), is("1"));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(1).getValue().getOtherPartyOptionName(), is("Otto Octavius - Appointee"));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(1).getValue().getOtherPartyOptionId(), is("2"));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(2).getValue().getOtherPartyOptionName(), is("Harry Osbourne - Representative"));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(2).getValue().getOtherPartyOptionId(), is("3"));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(3).getValue().getOtherPartyOptionName(), is("Peter Parker"));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(3).getValue().getOtherPartyOptionId(), is("4"));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(4).getValue().getOtherPartyOptionName(), is("Otto Octavius - Appointee"));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(4).getValue().getOtherPartyOptionId(), is("5"));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(5).getValue().getOtherPartyOptionName(), is("Harry Osbourne - Representative"));
        assertThat(response.getData().getReissueArtifactUi().getOtherPartyOptions().get(5).getValue().getOtherPartyOptionId(), is("6"));
    }

    @Test
    public void shouldIncludeValidWelshDocumentsInDropdown() {

        SscsWelshDocument document1 = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder()
            .documentFileName("welshFile1.pdf")
            .documentType(APPELLANT_EVIDENCE.getValue())
            .documentDateAdded("09-09-2020")
            .documentLink(DocumentLink.builder().documentFilename("welshFile1.pdf").documentUrl("welshUrl1").build())
            .build()).build();
        SscsWelshDocument document2 = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder()
            .documentFileName("welshFile2.pdf")
            .documentDateAdded("09-09-2020")
            .documentType(REPRESENTATIVE_EVIDENCE.getValue())
            .documentLink(DocumentLink.builder().documentFilename("welshFile2.pdf").documentUrl("welshUrl2").build())
            .build()).build();
        SscsWelshDocument document3 = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder()
            .documentFileName("welshFile3.pdf")
            .documentType(DWP_EVIDENCE.getValue())
            .documentDateAdded("09-09-2020")
            .documentLink(DocumentLink.builder().documentFilename("welshFile3.pdf").documentUrl("welshUrl3").build())
            .build()).build();
        List<SscsWelshDocument> sscsWelshDocuments = Arrays.asList(document1, document2, document3);
        sscsCaseData.setSscsWelshDocuments(sscsWelshDocuments);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertEquals(6, response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getListItems().size());
        assertEquals(new DynamicListItem("url1", "file1.pdf -  Appellant evidence"), response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getListItems().get(0));
        assertEquals(new DynamicListItem("url2", "file2.pdf -  Representative evidence"), response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getListItems().get(1));
        assertEquals(new DynamicListItem("url3", "file3.pdf -  Dwp evidence"), response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getListItems().get(2));
        assertNull(response.getData().getOriginalSender());
        assertEquals(new DynamicListItem("welshUrl1", "Bilingual - welshFile1.pdf -  Appellant evidence"), response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getListItems().get(3));
        assertEquals(new DynamicListItem("welshUrl2", "Bilingual - welshFile2.pdf -  Representative evidence"), response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getListItems().get(4));
        assertEquals(new DynamicListItem("welshUrl3", "Bilingual - welshFile3.pdf -  Dwp evidence"), response.getData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getListItems().get(5));
        assertNull(response.getData().getOriginalSender());
    }

    public static CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .name(Name.builder().firstName("Peter").lastName("Parker").build())
                        .isAppointee(YES.getValue())
                        .appointee(Appointee.builder().id(appointeeId).name(Name.builder().firstName("Otto").lastName("Octavius").build()).build())
                        .rep(Representative.builder().id(repId).name(Name.builder().firstName("Harry").lastName("Osbourne").build()).hasRepresentative(YES.getValue()).build())
                        .build())
                .build();
    }
}
