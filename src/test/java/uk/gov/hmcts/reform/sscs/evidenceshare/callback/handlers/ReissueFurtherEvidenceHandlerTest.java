package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartyOption;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartyOptionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReissueArtifactUi;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.FurtherEvidenceService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class ReissueFurtherEvidenceHandlerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private FurtherEvidenceService furtherEvidenceService;

    @Mock
    private IdamService idamService;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @InjectMocks
    private ReissueFurtherEvidenceHandler handler;

    @Captor
    ArgumentCaptor<SscsCaseData> captor;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> sscsCaseDetailsCaptor;

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenHandleIsCalled_shouldThrowException() {
        handler.handle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder()
                .reissueArtifactUi(ReissueArtifactUi.builder()
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ISSUE_FURTHER_EVIDENCE", "EVIDENCE_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenEventTypeIsNotReIssueFurtherEvidence_willThrowAnException(EventType eventType) {
        handler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder()
                .reissueArtifactUi(ReissueArtifactUi.builder()
                    .build()).build(), INTERLOCUTORY_REVIEW_STATE, eventType));
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_shouldThrowException() {
        handler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = IllegalStateException.class)
    public void givenHandleMethodIsCalled_shouldThrowExceptionIfCanNotBeHandled() {
        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(false);

        handler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().reissueArtifactUi(ReissueArtifactUi.builder()
                    .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("url", "label"), null)).build())
                .build(), INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));
    }

    @Test
    public void determineResizedDescriptionCorrectly() {

        SscsDocument doc = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .resizedDocumentLink(
                        DocumentLink
                            .builder()
                            .documentUrl("someurl.com")
                            .build())
                    .build())
            .build();

        String result = handler.determineDescription(doc);

        assertEquals("Update document evidence reissued flags after re-issuing further evidence to DWP and attached resized document(s)", result);
    }

    @Test
    public void determineNonResizedDescriptionCorrectly() {

        SscsDocument doc = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails.builder().build())
            .build();

        String result = handler.determineDescription(doc);

        assertEquals("Update document evidence reissued flags after re-issuing further evidence to DWP", result);
    }

    @Test
    @Parameters({"APPELLANT_EVIDENCE, true, true, true",
        "REPRESENTATIVE_EVIDENCE, false, false, true",
        "DWP_EVIDENCE, true, true, true",
        "APPELLANT_EVIDENCE, true, false, true",
        "APPELLANT_EVIDENCE, false, true, true",
        "APPELLANT_EVIDENCE, true, true, false",
        "APPELLANT_EVIDENCE, true, false, false",
        "APPELLANT_EVIDENCE, false, true, false"
    })
    public void givenIssueFurtherEvidenceCallback_shouldReissueEvidenceForAppellantAndRepAndDwpAndV2Enabled(DocumentType documentType,
                                                                                                boolean resendToAppellant,
                                                                                                boolean resendToRepresentative,
                                                                                                boolean isEnglish) {
        if (resendToAppellant || resendToRepresentative) {
            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        }

        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(true);

        AbstractDocument sscsDocumentNotIssued = null;
        if (isEnglish) {
            sscsDocumentNotIssued = SscsDocument.builder()
                    .value(SscsDocumentDetails.builder()
                            .documentLink(DocumentLink.builder().documentUrl("www.acme.co.uk").build())
                            .documentType(documentType.getValue())
                            .evidenceIssued("No")
                            .build())
                    .build();
        } else {
            sscsDocumentNotIssued = SscsWelshDocument.builder()
                    .value(SscsWelshDocumentDetails.builder()
                            .documentLink(DocumentLink.builder().documentUrl("www.acme.co.uk").build())
                            .documentType(documentType.getValue())
                            .evidenceIssued("No")
                            .build())
                    .build();
        }


        DynamicListItem dynamicListItem = new DynamicListItem(
                sscsDocumentNotIssued.getValue().getDocumentLink().getDocumentUrl(), "a label");
        DynamicList dynamicList = new DynamicList(dynamicListItem, Collections.singletonList(dynamicListItem));
        SscsCaseData caseData = SscsCaseData.builder()
                .ccdCaseId("1563382899630221")
                .appeal(Appeal.builder().rep(Representative.builder().hasRepresentative("YES").build()).build())
                .reissueArtifactUi(ReissueArtifactUi.builder()
                        .reissueFurtherEvidenceDocument(dynamicList)
                        .resendToAppellant(resendToAppellant ? YesNo.YES : YesNo.NO)
                        .resendToRepresentative(resendToRepresentative ? YesNo.YES : YesNo.NO)
                        .build())
                .build();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();

        if (isEnglish) {
            caseData.setSscsDocument(Collections.singletonList((SscsDocument) sscsDocumentNotIssued));
        } else {
            caseData.setSscsWelshDocuments(Collections.singletonList((SscsWelshDocument) sscsDocumentNotIssued));
        }

        handler.handle(CallbackType.SUBMITTED,
                HandlerHelper.buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));

        verify(furtherEvidenceService).canHandleAnyDocument(eq(caseData.getSscsDocument()));

        List<FurtherEvidenceLetterType> allowedLetterTypes = new ArrayList<>();
        if (resendToAppellant) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.APPELLANT_LETTER);
        }
        if (resendToRepresentative) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.REPRESENTATIVE_LETTER);
        }

        verify(furtherEvidenceService).issue(eq(Collections.singletonList(sscsDocumentNotIssued)), eq(caseData), eq(documentType), eq(allowedLetterTypes), eq(null));

        verifyNoMoreInteractions(furtherEvidenceService);

        if (resendToAppellant || resendToRepresentative) {
            verify(updateCcdCaseService).updateCaseV2(any(Long.class), eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                    any(), any(), any(IdamTokens.class), sscsCaseDetailsCaptor.capture());
            sscsCaseDetailsCaptor.getValue().accept(sscsCaseDetails);
            if (isEnglish) {
                assertEquals("Yes", sscsCaseDetails.getData().getSscsDocument().get(0).getValue().getEvidenceIssued());
            } else {
                assertEquals("Yes", sscsCaseDetails.getData().getSscsWelshDocuments().get(0).getValue().getEvidenceIssued());
            }
        } else {
            verifyNoInteractions(updateCcdCaseService);
        }

    }

    @Test
    @Parameters({"APPELLANT_EVIDENCE, true, true, true, false, false",
        "REPRESENTATIVE_EVIDENCE, false, false, true, false, false",
        "DWP_EVIDENCE, true, true, true, false, false",
        "APPELLANT_EVIDENCE, true, false, true, false, false",
        "APPELLANT_EVIDENCE, false, true, true, false, false",
        "APPELLANT_EVIDENCE, true, true, false, false, true",
        "APPELLANT_EVIDENCE, true, false, false, false, false",
        "APPELLANT_EVIDENCE, false, true, false, false, false",
        "APPELLANT_EVIDENCE, true, true, true, true, false",
        "REPRESENTATIVE_EVIDENCE, false, false, true, true, true",
        "REPRESENTATIVE_EVIDENCE, false, false, true, false, true",
        "REPRESENTATIVE_EVIDENCE, false, false, true, true, false",
        "DWP_EVIDENCE, true, true, true, true, false",
        "APPELLANT_EVIDENCE, true, false, true, true, true",
        "APPELLANT_EVIDENCE, false, true, true, true, false",
        "APPELLANT_EVIDENCE, true, true, false, true, false",
        "APPELLANT_EVIDENCE, true, false, false, true, false",
        "APPELLANT_EVIDENCE, false, true, false, true, false",
        "OTHER_PARTY_EVIDENCE, true, true, true, true, false",
        "OTHER_PARTY_EVIDENCE, false, false, true, true, true",
        "OTHER_PARTY_EVIDENCE, false, false, true, false, true",
        "OTHER_PARTY_EVIDENCE, false, false, true, true, false",
        "OTHER_PARTY_EVIDENCE, true, true, true, true, false",
        "OTHER_PARTY_REPRESENTATIVE_EVIDENCE, true, false, true, true, true",
        "OTHER_PARTY_REPRESENTATIVE_EVIDENCE, false, true, true, true, false",
        "OTHER_PARTY_REPRESENTATIVE_EVIDENCE, true, true, false, true, false",
        "OTHER_PARTY_REPRESENTATIVE_EVIDENCE, true, false, false, true, false",
        "OTHER_PARTY_REPRESENTATIVE_EVIDENCE, false, true, false, true, false",
    })
    public void givenIssueFurtherEvidenceCallback_shouldReissueEvidenceForAppellantAndRepAndDwpAndOtherPartyAndV2Enabled(DocumentType documentType,
                                                                                                             boolean resendToAppellant,
                                                                                                             boolean resendToRepresentative,
                                                                                                             boolean isEnglish,
                                                                                                             boolean resendToOtherParty,
                                                                                                             boolean resendToOtherPartyRep) {
        if (resendToAppellant || resendToRepresentative || resendToOtherParty || resendToOtherPartyRep) {
            when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        }

        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(true);

        AbstractDocument sscsDocumentNotIssued = null;
        if (isEnglish) {
            sscsDocumentNotIssued = SscsDocument.builder()
                    .value(SscsDocumentDetails.builder()
                            .documentLink(DocumentLink.builder().documentUrl("www.acme.co.uk").build())
                            .documentType(documentType.getValue())
                            .evidenceIssued("No").originalSenderOtherPartyId("3")
                            .build())
                    .build();
        } else {
            sscsDocumentNotIssued = SscsWelshDocument.builder()
                    .value(SscsWelshDocumentDetails.builder()
                            .documentLink(DocumentLink.builder().documentUrl("www.acme.co.uk").build())
                            .documentType(documentType.getValue())
                            .evidenceIssued("No")
                            .build())
                    .build();
        }


        DynamicListItem dynamicListItem = new DynamicListItem(
                sscsDocumentNotIssued.getValue().getDocumentLink().getDocumentUrl(), "a label");
        DynamicList dynamicList = new DynamicList(dynamicListItem, Collections.singletonList(dynamicListItem));
        SscsCaseData caseData = SscsCaseData.builder()
                .ccdCaseId("1563382899630221")
                .appeal(Appeal.builder().rep(Representative.builder().hasRepresentative("YES").build()).build())
                .reissueArtifactUi(ReissueArtifactUi.builder()
                        .reissueFurtherEvidenceDocument(dynamicList)
                        .resendToAppellant(resendToAppellant ? YesNo.YES : YesNo.NO)
                        .resendToRepresentative(resendToRepresentative ? YesNo.YES : YesNo.NO)
                        .otherPartyOptions(getOtherPartyOptions(resendToOtherParty ? YesNo.YES : YesNo.NO,
                                resendToOtherPartyRep ? YesNo.YES : YesNo.NO))
                        .build())
                .build();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();

        if (isEnglish) {
            caseData.setSscsDocument(Collections.singletonList((SscsDocument) sscsDocumentNotIssued));
        } else {
            caseData.setSscsWelshDocuments(Collections.singletonList((SscsWelshDocument) sscsDocumentNotIssued));
        }

        handler.handle(CallbackType.SUBMITTED,
                HandlerHelper.buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));

        verify(furtherEvidenceService).canHandleAnyDocument(eq(caseData.getSscsDocument()));

        List<FurtherEvidenceLetterType> allowedLetterTypes = new ArrayList<>();
        if (resendToAppellant) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.APPELLANT_LETTER);
        }
        if (resendToRepresentative) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.REPRESENTATIVE_LETTER);
        }
        if (resendToOtherParty) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.OTHER_PARTY_LETTER);
        }
        if (resendToOtherPartyRep) {
            allowedLetterTypes.add(FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER);
        }

        verify(furtherEvidenceService).issue(eq(Collections.singletonList(sscsDocumentNotIssued)), eq(caseData), eq(documentType), eq(allowedLetterTypes), eq(resendToOtherParty && isEnglish ? "3" : null));

        verifyNoMoreInteractions(furtherEvidenceService);

        if (resendToAppellant || resendToRepresentative || resendToOtherParty || resendToOtherPartyRep) {
            verify(updateCcdCaseService).updateCaseV2(any(Long.class), eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                    any(), any(), any(IdamTokens.class), sscsCaseDetailsCaptor.capture());
            sscsCaseDetailsCaptor.getValue().accept(sscsCaseDetails);
            if (isEnglish) {
                assertEquals("Yes", sscsCaseDetails.getData().getSscsDocument().get(0).getValue().getEvidenceIssued());
            } else {
                assertEquals("Yes", sscsCaseDetails.getData().getSscsWelshDocuments().get(0).getValue().getEvidenceIssued());
            }
        } else {
            verifyNoInteractions(updateCcdCaseService);
        }

    }

    @Test
    @Parameters({"Second.doc", "SecondRedacted.doc",})
    public void givenIssueFurtherEvidenceCallback_shouldReissueChosenEvidence(String chosenDoc) {

        SscsDocument doc1 = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentUrl("First.doc").build())
                .editedDocumentLink(DocumentLink.builder().documentUrl("FirstRedacted.doc").build())
                .build())
            .build();
        SscsDocument doc2 = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentUrl("Second.doc").build())
                .editedDocumentLink(DocumentLink.builder().documentUrl("SecondRedacted.doc").build())
                .build())
            .build();
        SscsDocument doc3 = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentUrl("Third.doc").build())
                .build())
            .build();

        DynamicListItem dynamicListItem1 = new DynamicListItem(
            doc1.getValue().getEditedDocumentLink().getDocumentUrl(), "First document");
        DynamicListItem dynamicListItem2 = new DynamicListItem(chosenDoc, "Second document");
        DynamicListItem dynamicListItem3 = new DynamicListItem(
            doc3.getValue().getDocumentLink().getDocumentUrl(), "Third document");
        DynamicList dynamicList = new DynamicList(dynamicListItem2, List.of(dynamicListItem1, dynamicListItem2, dynamicListItem3));
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221")
            .reissueArtifactUi(ReissueArtifactUi.builder()
                .reissueFurtherEvidenceDocument(dynamicList)
                .build())
            .build();
        caseData.setSscsDocument(List.of(doc1, doc2, doc3));

        when(furtherEvidenceService.canHandleAnyDocument(List.of(doc1, doc2, doc3))).thenReturn(true);
        doNothing().when(furtherEvidenceService)
            .issue(List.of(doc1), caseData, APPELLANT_EVIDENCE, List.of(), null);

        handler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));

        verify(furtherEvidenceService).issue(
            argThat(argument -> argument.size() == 1 && argument.get(0) == doc2),
            eq(caseData), eq(APPELLANT_EVIDENCE), eq(List.of()), eq(null)
        );
    }

    @Test
    public void givenIssueFurtherEvidenceCallback_shouldReissueChosenEvidenceWhenSscsDocumentsContainsVideoAudioDocs() {
        SscsDocument pdfDoc = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder().documentUrl("pdf.doc").build())
                        .editedDocumentLink(DocumentLink.builder().documentUrl("pdfRedacted.doc").build())
                        .build())
                .build();
        SscsDocument videoDoc = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .avDocumentLink(DocumentLink.builder().documentUrl("video.mp4").build())
                        .build())
                .build();
        SscsDocument audioDoc = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder().documentUrl("audio.mp3").build())
                        .build())
                .build();

        DynamicListItem dynamicListItem = new DynamicListItem(
                pdfDoc.getValue().getEditedDocumentLink().getDocumentUrl(), "First document");

        DynamicList dynamicList = new DynamicList(dynamicListItem, List.of(dynamicListItem));
        SscsCaseData caseData = SscsCaseData.builder()
                .ccdCaseId("1563382899630221")
                .reissueArtifactUi(ReissueArtifactUi.builder()
                        .reissueFurtherEvidenceDocument(dynamicList)
                        .build())
                .build();
        caseData.setSscsDocument(List.of(audioDoc, videoDoc, pdfDoc));

        when(furtherEvidenceService.canHandleAnyDocument(List.of(audioDoc, videoDoc, pdfDoc))).thenReturn(true);
        doNothing().when(furtherEvidenceService)
                .issue(List.of(pdfDoc), caseData, APPELLANT_EVIDENCE, List.of(), null);

        handler.handle(CallbackType.SUBMITTED,
                HandlerHelper.buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, REISSUE_FURTHER_EVIDENCE));

        verify(furtherEvidenceService).issue(
                argThat(argument -> argument.size() == 1 && argument.getFirst() == pdfDoc),
                eq(caseData), eq(APPELLANT_EVIDENCE), eq(List.of()), eq(null)
        );
    }


    private static List<OtherPartyOption> getOtherPartyOptions(YesNo resendToOtherParty, YesNo resendToOtherPartyRep) {
        List<OtherPartyOption> otherPartyOptions = new ArrayList<>();
        if (resendToOtherParty != null) {
            otherPartyOptions.add(OtherPartyOption
                .builder()
                .value(OtherPartyOptionDetails
                    .builder()
                    .otherPartyOptionId("3")
                    .otherPartyOptionName("OPAppointee OP3 - Appointee")
                    .resendToOtherParty(resendToOtherParty)
                    .build())
                .build());
        }

        if (resendToOtherPartyRep != null) {
            otherPartyOptions.add(OtherPartyOption
                .builder()
                .value(OtherPartyOptionDetails
                    .builder()
                    .otherPartyOptionId("4")
                    .otherPartyOptionName("OP3 - Representative")
                    .resendToOtherParty(resendToOtherPartyRep)
                    .build())
                .build());
        }

        return otherPartyOptions;
    }
}
