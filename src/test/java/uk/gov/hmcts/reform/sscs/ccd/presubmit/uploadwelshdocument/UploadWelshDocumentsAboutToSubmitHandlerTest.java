package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.WelshFooterService;

@ExtendWith(MockitoExtension.class)
class UploadWelshDocumentsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String ENGLISH_PDF = "english.pdf";

    @InjectMocks
    private UploadWelshDocumentsAboutToSubmitHandler handler;

    @Mock
    private WelshFooterService welshFooterService;

    @Test
    void givenCanHandleIsCalled_shouldReturnCorrectResult() {
        final Callback<SscsCaseData> callbackWithValidEventOption =
            buildCallback("callbackWithValidEventOption", buildSscsDocuments(false), buildSscsWelshDocuments(DocumentType.SSCS1.getValue()), null, State.VALID_APPEAL);

        final boolean actualResult = handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callbackWithValidEventOption);

        assertThat(actualResult).isTrue();
    }

    @Test
    void shouldAddAnErrorIfNoWelshDocumentSelected() {
        final Callback<SscsCaseData> callback = buildCallback(ENGLISH_PDF, buildSscsDocuments(false), buildInvalidSscsWelshDocuments(DocumentType.SSCS1.getValue()), null, State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response).isNotNull();
        assertThat(response.getErrors().stream().findFirst()).contains("Please select a document to upload");
    }

    @Test
    void updateCaseWhenOnlyOneDocumentAndOnlyOneSetToRequestTranslationStatusToRequestTranslation() {
        final Callback<SscsCaseData> callback = buildCallback(ENGLISH_PDF,
            buildSscsDocuments(false), buildSscsWelshDocuments(DocumentType.SSCS1.getValue()), null, State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getOriginalDocuments()).isNotNull();
        assertThat(caseData.getOriginalDocuments().getListItems().getFirst().getCode()).isEqualTo(ENGLISH_PDF);
        assertThat(caseData.getSscsDocument().getFirst().getValue().getDocumentTranslationStatus().getId())
            .isEqualTo(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE.getId());
        assertThat(caseData.getTranslationWorkOutstanding()).isEqualTo("No");
        assertThat(caseData.getSscsWelshDocuments().getFirst().getValue().getOriginalDocumentFileName()).isEqualTo(ENGLISH_PDF);
        assertThat(caseData.getSscsWelshDocuments().getFirst().getValue().getDocumentLanguage()).isEqualTo("welsh");
        assertThat(caseData.getSscsWelshPreviewNextEvent()).isEqualTo(EventType.SEND_TO_DWP.getCcdType());
    }

    @Test
    void updateCaseWhenOnlyOneDocumentAndMoreThanOneSetToRequestTranslationStatusToRequestTranslation() {
        final Callback<SscsCaseData> callback = buildCallback(ENGLISH_PDF,
            buildSscsDocuments(true), buildSscsWelshDocuments(DocumentType.SSCS1.getValue()), null, State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getOriginalDocuments()).isNotNull();
        assertThat(caseData.getOriginalDocuments().getListItems().getFirst().getCode()).isEqualTo(ENGLISH_PDF);
        assertThat(caseData.getSscsDocument().getFirst().getValue().getDocumentTranslationStatus().getId())
            .isEqualTo(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE.getId());
        assertThat(caseData.getTranslationWorkOutstanding()).isEqualTo("Yes");
        assertThat(caseData.getSscsWelshDocuments().getFirst().getValue().getOriginalDocumentFileName()).isEqualTo(ENGLISH_PDF);
        assertThat(caseData.getSscsWelshDocuments().getFirst().getValue().getDocumentLanguage()).isEqualTo("welsh");
        assertThat(caseData.getSscsWelshPreviewNextEvent()).isEqualTo(EventType.SEND_TO_DWP.getCcdType());
    }

    @Test
    void updateCaseWhenRip1DwpDocumentSetToRequestTranslationStatusToRequestTranslation() {
        final Callback<SscsCaseData> callback = buildCallback("rip1.pdf", null, buildSscsWelshDocuments("rip1Document"), buildDwpDocuments(), State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getOriginalDocuments()).isNotNull();
        assertThat(caseData.getOriginalDocuments().getListItems().getFirst().getCode()).isEqualTo("rip1.pdf");
        assertThat(caseData.getDwpDocuments().getFirst().getValue().getDocumentTranslationStatus().getId())
            .isEqualTo(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE.getId());
        assertThat(caseData.getTranslationWorkOutstanding()).isEqualTo("No");
        assertThat(caseData.getSscsWelshDocuments().getFirst().getValue().getOriginalDocumentFileName()).isEqualTo("rip1.pdf");
        assertThat(caseData.getSscsWelshDocuments().getFirst().getValue().getDocumentLanguage()).isEqualTo("welsh");
        assertThat(caseData.getSscsWelshPreviewNextEvent()).isNull();
    }

    @Test
    void shouldUpdateWithDirectionIssuedWelshNextEventCorrectlyBasedOnDirectionNoticeDocumentType() {
        final Callback<SscsCaseData> callback = buildCallback(ENGLISH_PDF, singletonList(
            buildSscsDocument(ENGLISH_PDF, "docUrl",
                DocumentType.DIRECTION_NOTICE.getValue(), "A")), buildSscsWelshDocuments(DocumentType.DIRECTION_NOTICE.getValue()), null, State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getSscsWelshPreviewNextEvent()).isEqualTo(EventType.DIRECTION_ISSUED_WELSH.getCcdType());
    }

    @Test
    void shouldUpdateWithDecisionIssuedWelshNextEventCorrectlyBasedOnDecisionNoticeDocumentType() {
        final Callback<SscsCaseData> callback = buildCallback(ENGLISH_PDF,
            singletonList(buildSscsDocument(ENGLISH_PDF, "docUrl", DocumentType.DECISION_NOTICE.getValue(), "A")), buildSscsWelshDocuments(DocumentType.DECISION_NOTICE.getValue()), null, State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getSscsWelshPreviewNextEvent()).isEqualTo(EventType.DECISION_ISSUED_WELSH.getCcdType());
    }

    @Test
    void shouldUpdateWithDecisionIssuedWelshNextEventCorrectlyBasedOnReinstatementRequestDocumentType() {
        final Callback<SscsCaseData> callback = buildCallback(ENGLISH_PDF,
            singletonList(buildSscsDocument(ENGLISH_PDF, "docUrl", DocumentType.REINSTATEMENT_REQUEST.getValue(), "A")), buildSscsWelshDocuments(DocumentType.REINSTATEMENT_REQUEST.getValue()), null, State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getSscsWelshPreviewNextEvent()).isEqualTo(UPDATE_CASE_ONLY.getCcdType());
    }

    @Test
    void shouldUpdateWithUploadWelshDocumentEventCorrectlyBasedOnAppellantEvidenceDocumentType() {
        final Callback<SscsCaseData> callback = buildCallback(ENGLISH_PDF,
            singletonList(buildSscsDocument(ENGLISH_PDF, "docUrl", DocumentType.APPELLANT_EVIDENCE.getValue(), null)), buildSscsWelshDocuments(DocumentType.APPELLANT_EVIDENCE.getValue()), null, State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getSscsWelshPreviewNextEvent()).isNull();
    }

    @Test
    void shouldUpdateWithFinalDecisionIssuedWelshNextEventCorrectlyBasedOnDecisionNoticeDocumentType() {
        final Callback<SscsCaseData> callback = buildCallback(ENGLISH_PDF,
            singletonList(buildSscsDocument(ENGLISH_PDF, "docUrl", DocumentType.FINAL_DECISION_NOTICE.getValue(), "A")), buildSscsWelshDocuments(DocumentType.FINAL_DECISION_NOTICE.getValue()), null, State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getSscsWelshPreviewNextEvent()).isEqualTo(EventType.ISSUE_FINAL_DECISION_WELSH.getCcdType());
    }

    @Test
    void shouldAddBundleAdditionForAppellantEvidenceDocumentType() {
        final Callback<SscsCaseData> callback = buildCallback("Addition A - my filename.pdf",
            singletonList(buildSscsDocument("Addition A - my filename.pdf", "docUrl", DocumentType.APPELLANT_EVIDENCE.getValue(), "A")), buildSscsWelshDocuments(DocumentType.APPELLANT_EVIDENCE.getValue()), null, State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        final String documentFooterText = "Appellant evidence";
        final SscsWelshDocumentDetails welshDocumentDetails = caseData.getSscsWelshPreviewDocuments().getFirst().getValue();
        when(welshFooterService.addFooter(welshDocumentDetails.getDocumentLink(), documentFooterText, "WEL-A")).thenReturn(DocumentLink.builder().documentFilename("New Doc").build());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getSscsWelshPreviewNextEvent()).isNull();
        final SscsWelshDocumentDetails sscsWelshDocumentDetails = caseData.getSscsWelshDocuments().getFirst().getValue();
        assertThat(sscsWelshDocumentDetails.getOriginalDocumentFileName()).isEqualTo("Addition A - my filename.pdf");
        assertThat(sscsWelshDocumentDetails.getBundleAddition()).isEqualTo("WEL-A");
        assertThat(sscsWelshDocumentDetails.getDocumentFileName()).isEqualTo("Addition WEL-A - my filename.pdf");
        assertThat(sscsWelshDocumentDetails.getEvidenceIssued()).isEqualTo("No");
    }

    @Test
    void shouldAddNewWelshDocumentToTheFrontOfExistingSscsWelshDocuments() {
        final Callback<SscsCaseData> callback = buildCallback(ENGLISH_PDF, null, buildSscsWelshDocuments(DocumentType.SSCS1.getValue()), null, State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        final SscsWelshDocument existingWelshDocument = SscsWelshDocument.builder()
            .value(SscsWelshDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("existing-welsh.pdf").build())
                .documentLanguage("welsh")
                .build())
            .build();
        final List<SscsWelshDocument> existingWelshDocuments = new ArrayList<>();
        existingWelshDocuments.add(existingWelshDocument);
        caseData.setSscsWelshDocuments(existingWelshDocuments);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getSscsWelshDocuments()).hasSize(2);
        assertThat(caseData.getSscsWelshDocuments().getFirst().getValue().getOriginalDocumentFileName()).isEqualTo(ENGLISH_PDF);
        assertThat(caseData.getSscsWelshDocuments().get(1)).isEqualTo(existingWelshDocument);
    }

    @Test
    void shouldAddMultipleNewWelshDocumentsToTheFrontOfTheListInReverseOrder() {
        final SscsWelshDocument previewDocument1 = SscsWelshDocument.builder()
            .value(SscsWelshDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("welsh1.pdf").build())
                .documentLanguage("welsh")
                .documentType(DocumentType.SSCS1.getValue())
                .build())
            .build();
        final SscsWelshDocument previewDocument2 = SscsWelshDocument.builder()
            .value(SscsWelshDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("welsh2.pdf").build())
                .documentLanguage("welsh")
                .documentType(DocumentType.SSCS1.getValue())
                .build())
            .build();

        final Callback<SscsCaseData> callback = buildCallback(ENGLISH_PDF, null, List.of(previewDocument1, previewDocument2), null, State.VALID_APPEAL);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getSscsWelshDocuments()).hasSize(2);
        assertThat(caseData.getSscsWelshDocuments().getFirst().getValue().getDocumentLink().getDocumentFilename()).isEqualTo("welsh2.pdf");
        assertThat(caseData.getSscsWelshDocuments().get(1).getValue().getDocumentLink().getDocumentFilename()).isEqualTo("welsh1.pdf");
    }

    @Test
    void givenInterlocReviewStateshouldNotSetReviewState() {
        final Callback<SscsCaseData> callback = buildCallback(ENGLISH_PDF,
            singletonList(buildSscsDocument(ENGLISH_PDF, "docUrl", DocumentType.APPELLANT_EVIDENCE.getValue(), null)), buildSscsWelshDocuments(DocumentType.APPELLANT_EVIDENCE.getValue()), null, State.INTERLOCUTORY_REVIEW_STATE);

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.INTERLOCUTORY_REVIEW_STATE);
        caseData.setWelshInterlocNextReviewState("reviewByTcw");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(caseData.getInterlocReviewState()).isEqualTo(REVIEW_BY_TCW);
        assertThat(caseData.getSscsWelshPreviewNextEvent()).isNull();
    }

    private Callback<SscsCaseData> buildCallback(final String dynamicListItemCode,
        final List<SscsDocument> sscsDocuments, final List<SscsWelshDocument> welshDocuments, final List<DwpDocument> dwpDocuments, final State state) {

        final DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
            singletonList(new DynamicListItem(dynamicListItemCode, "label")));

        final SscsCaseData sscsCaseData = SscsCaseData.builder()
            .originalDocuments(dynamicList)
            .sscsDocument(sscsDocuments)
            .dwpDocuments(dwpDocuments)
            .sscsWelshPreviewDocuments(welshDocuments)
            .build();
        final CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            state, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), EventType.UPLOAD_WELSH_DOCUMENT, false);
    }

    private List<SscsDocument> buildSscsDocuments(final boolean moreThanOneDoc) {
        final SscsDocument sscs1Doc = buildSscsDocument(ENGLISH_PDF, "/anotherUrl", DocumentType.SSCS1.getValue(), "A");
        final SscsDocument sscs2Doc = buildSscsDocument("anything.pdf", "/anotherUrl", DocumentType.SSCS1.getValue(), "A");

        final List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscs1Doc);
        if (moreThanOneDoc) {
            sscsDocuments.add(sscs2Doc);
        }
        return sscsDocuments;
    }

    private List<DwpDocument> buildDwpDocuments() {
        final DwpDocument dwpDoc1 = buildDwpDocument(DocumentType.AUDIO_DOCUMENT.getValue());

        final List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(dwpDoc1);
        return dwpDocuments;
    }

    private List<SscsWelshDocument> buildSscsWelshDocuments(final String documentType) {
        return singletonList(SscsWelshDocument.builder()
            .value(SscsWelshDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("/anotherUrl")
                    .documentFilename("welsh.pdf")
                    .build())
                .documentLanguage("welsh")
                .documentType(documentType)
                .build())
            .build());
    }

    private List<SscsWelshDocument> buildInvalidSscsWelshDocuments(final String documentType) {
        return singletonList(SscsWelshDocument.builder()
            .value(SscsWelshDocumentDetails.builder()
                .documentLanguage("welsh")
                .documentType(documentType)
                .build())
            .build());
    }

    private SscsDocument buildSscsDocument(final String filename, final String documentUrl, final String documentType, final String bundleAddition) {
        return SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl(documentUrl)
                    .documentFilename(filename)
                    .build())
                .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)
                .documentType(documentType)
                .documentFileName(filename)
                .bundleAddition(bundleAddition)
                .build())
            .build();
    }

    private DwpDocument buildDwpDocument(final String documentType) {
        return DwpDocument.builder()
                .value(DwpDocumentDetails.builder()
                        .avDocumentLink(DocumentLink.builder()
                                .documentUrl("/anotherUrl")
                                .documentFilename("english.mp3")
                                .build())
                        .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)
                        .documentType(documentType)
                        .documentFileName("english.mp3")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("rip1url")
                                .documentFilename("rip1.pdf")
                                .build())
                        .build())
                .build();
    }

}
