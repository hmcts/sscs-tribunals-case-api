package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.reissuefurtherevidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;

import java.util.*;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;

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
        openMocks(this);
        handler = new ReissueFurtherEvidenceAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.REISSUE_FURTHER_EVIDENCE);

        document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(REPRESENTATIVE_EVIDENCE.getValue())
                .evidenceIssued("Yes")
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .editedDocumentLink(DocumentLink.builder().documentFilename("editedFile1").documentUrl("editedUrl").build())
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
                .reissueArtifactUi(ReissueArtifactUi.builder()
                        .resendToAppellant(YesNo.YES)
                        .resendToDwp(YesNo.YES)
                        .resendToRepresentative(YesNo.NO)
                        .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("url2", "file2.pdf - appellantEvidence"), null)).build())
                .build();

        SscsWelshDocument welshDocument1 = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder()
                .documentFileName("welshFile1.pdf")
                .documentType(APPELLANT_EVIDENCE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("welshUrl1").build())
                .build()).build();
        SscsWelshDocument welshDocument2 = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder()
                .documentFileName("welshFile2.pdf")
                .documentType(REPRESENTATIVE_EVIDENCE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("welshUrl2").build())
                .build()).build();
        SscsWelshDocument welshDocument3 = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder()
                .documentFileName("welshFile3.pdf")
                .documentType(DWP_EVIDENCE.getValue())
                .documentLink(DocumentLink.builder().documentUrl("welshUrl3").build())
                .build()).build();
        List<SscsWelshDocument> sscsWelshDocuments = Arrays.asList(welshDocument1, welshDocument2, welshDocument3);
        sscsCaseData.setSscsWelshDocuments(sscsWelshDocuments);


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
    @Parameters({"url1, file1.pdf - representativeEvidence, APPELLANT", "editedUrl, editedFile1.pdf, APPELLANT", "url2, file2.pdf - appellantEvidence, DWP", "welshUrl1, welshFile1.pdf - appellantEvidence, APPELLANT", "welshUrl2, welshFile2.pdf - representativeEvidence, APPELLANT"})
    public void setsEvidenceHandledFlagToNoForDocumentSelected(String selectedUrl, String selectedLabel, PartyItemList newSender) {

        sscsCaseData = sscsCaseData.toBuilder()
                .originalSender(new DynamicList(
                        new DynamicListItem(newSender.getCode(), newSender.getLabel()),
                        Arrays.asList(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()),
                                new DynamicListItem(DWP.getCode(), DWP.getLabel()))
                ))
                .reissueArtifactUi(ReissueArtifactUi.builder().resendToAppellant(YesNo.YES)
                        .resendToDwp(YesNo.YES)
                        .resendToRepresentative(YesNo.NO)
                        .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem(selectedUrl, selectedLabel), null)).build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());


        Optional<? extends AbstractDocumentDetails> selectedDocumentValue = Stream.of(sscsCaseData.getSscsDocument(), sscsCaseData.getSscsWelshDocuments())
            .flatMap(x -> x == null ? null : x.stream())
            .filter(f -> f.getValue().getDocumentLink().getDocumentUrl().equals(selectedUrl)
                || (f.getValue().getEditedDocumentLink() != null && f.getValue().getEditedDocumentLink().getDocumentUrl().equals(selectedUrl)))
            .map(f -> f.getValue())
            .findFirst();

        assertEquals("No", selectedDocumentValue.map(AbstractDocumentDetails::getEvidenceIssued).orElse("Unknown"));
        assertEquals(newSender.getCode() + "Evidence", selectedDocumentValue.map(AbstractDocumentDetails::getDocumentType).orElse("Unknown"));

        DocumentType expectedDocumentTypeOfUnselectedDocument =
            (selectedUrl.equals("url1") || selectedUrl.equals("editedUrl")) ? APPELLANT_EVIDENCE : REPRESENTATIVE_EVIDENCE;
        Optional<SscsDocumentDetails> otherDocumentValue = response.getData().getSscsDocument().stream()
                .filter(f -> !(f.getValue().getDocumentLink().getDocumentUrl().equals(selectedUrl)
                        || (f.getValue().getEditedDocumentLink() != null && f.getValue().getEditedDocumentLink().getDocumentUrl().equals(selectedUrl))))
                .map(f -> f.getValue())
                .findFirst();
        assertEquals("Yes", otherDocumentValue.map(SscsDocumentDetails::getEvidenceIssued).orElse("Unknown"));
        assertEquals(expectedDocumentTypeOfUnselectedDocument.getValue(), otherDocumentValue.map(SscsDocumentDetails::getDocumentType).orElse("Unknown"));
    }

    @Test
    @Parameters({"videoRecording.mp4, videoDocument", "audioEvidence.mp3, audioDocument"})
    public void doesNotReturnErrorIfAvEvidenceIsPresent(String fileName, String fileType) {
        SscsDocument avDocument = SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                        .documentType(fileType)
                        .documentFileName(fileName)
                        .documentLink(null)
                        .build()
        ).build();
        List<SscsDocument> sscsDocs = new ArrayList<SscsDocument>();
        sscsDocs.add(avDocument);
        sscsDocs.add(document2);
        sscsCaseData.setSscsDocument(sscsDocs);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(Collections.EMPTY_SET, response.getErrors());
    }

    @Test
    public void returnAnErrorIfNoSelectedDocument() {
        sscsCaseData.getReissueArtifactUi().setReissueFurtherEvidenceDocument(null);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Select a document to re-issue further evidence.", response.getErrors().toArray()[0]);
    }

    @Test
    @Parameters({"Yes", "No"})
    public void doesNotReturnAnErrorIfNoSelectedOriginalSender(String includeOtherParty) {
        sscsCaseData.setOriginalSender(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertEquals(document2.toBuilder().value(document2.getValue().toBuilder().evidenceIssued("No").build()).build(), response.getData().getSscsDocument().get(1));
        assertEquals(document1, response.getData().getSscsDocument().get(0));
    }

    @NotNull
    private List<OtherPartyOption> getOtherPartyOptions(YesNo resendToOtherParty) {
        return Collections.singletonList(OtherPartyOption
                .builder()
                .value(OtherPartyOptionDetails
                        .builder()
                        .otherPartyOptionId("1")
                        .otherPartyOptionName("Tony Stark")
                        .resendToOtherParty(resendToOtherParty)
                        .build())
                .build());
    }

    @Test
    @Parameters({"Yes", "No"})
    public void returnsAnErrorIfThereIsNoPartySelectedToReIssueFurtherEvidence(String includeOtherParty) {
        ReissueArtifactUi reissueFurtherEvidence = sscsCaseData.getReissueArtifactUi();
        reissueFurtherEvidence.setResendToAppellant(YesNo.NO);
        reissueFurtherEvidence.setResendToRepresentative(YesNo.NO);
        reissueFurtherEvidence.setResendToDwp(YesNo.NO);
        if ("Yes".equals(includeOtherParty)) {
            sscsCaseData.setOtherParties(Collections.singletonList(buildOtherParty("1")));
            sscsCaseData.getReissueArtifactUi().setOtherPartyOptions(getOtherPartyOptions(YesNo.NO));
        }

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Select a party to reissue.", response.getErrors().toArray()[0]);
    }

    @Test
    public void returnsAnErrorIfItCouldNotFindTheSelectedDocumentToReIssueFurtherEvidence() {
        sscsCaseData.getReissueArtifactUi().setReissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("code", "label"), null));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Could not find the selected document with url 'code' to re-issue further evidence in the appeal with id 'ccdId'.", response.getErrors().toArray()[0]);
    }

    @Test
    public void returnsAnErrorIfReIssuedToRepresentativeWhenThereIsNoRepOnTheAppealToReIssueFurtherEvidence() {
        sscsCaseData.getReissueArtifactUi().setResendToRepresentative(YesNo.YES);

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

    public static CcdValue<OtherParty> buildOtherParty(String id) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .name(Name.builder().firstName("Tony").lastName("Stark").build())
                        .build())
                .build();
    }
}
