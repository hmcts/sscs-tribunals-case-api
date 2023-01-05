package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_WELSH_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;

import java.time.LocalDateTime;
import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.WelshFooterService;

@RunWith(JUnitParamsRunner.class)
public class UploadWelshDocumentsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private UploadWelshDocumentsAboutToSubmitHandler handler;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private WelshFooterService welshFooterService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UploadWelshDocumentsAboutToSubmitHandler(welshFooterService);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_WELSH_DOCUMENT);
        sscsCaseData = SscsCaseData.builder().state(State.VALID_APPEAL).appeal(Appeal.builder().build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters(method = "generateCanHandleScenarios")
    public void givenCanHandleIsCalled_shouldReturnCorrectResult(CallbackType callbackType,
                                                                 Callback<SscsCaseData> callback,
                                                                 boolean expectedResult) {
        boolean actualResult = handler.canHandle(callbackType, callback);
        assertEquals(expectedResult, actualResult);
    }


    @Test
    public void shouldAddAnErrorIfNoWelshDocumentSelected() {
        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, buildSscsDocuments(false), buildInvalidSscsWelshDocuments(DocumentType.SSCS1.getValue()), null, State.VALID_APPEAL);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNotNull(response);
        assertEquals("Please select a document to upload", response.getErrors().stream().findFirst().get());
    }


    @Test
    public void updateCaseWhenOnlyOneDocumentAndOnlyOneSetToRequestTranslationStatusToRequestTranslation() {
        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, buildSscsDocuments(false), buildSscsWelshDocuments(DocumentType.SSCS1.getValue()), null, State.VALID_APPEAL);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNotNull(caseData.getOriginalDocuments());
        assertEquals("english.pdf", caseData.getOriginalDocuments().getListItems().get(0).getCode());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE.getId(),
            caseData.getSscsDocument().get(0).getValue().getDocumentTranslationStatus().getId());
        assertEquals("No", caseData.getTranslationWorkOutstanding());
        assertEquals("english.pdf",
            caseData.getSscsWelshDocuments().get(0).getValue().getOriginalDocumentFileName());
        assertEquals("welsh",
            caseData.getSscsWelshDocuments().get(0).getValue().getDocumentLanguage());
        assertEquals(EventType.SEND_TO_DWP.getCcdType(), caseData.getSscsWelshPreviewNextEvent());
    }

    @Test
    public void updateCaseWhenOnlyOneDocumentAndMoreThanOneSetToRequestTranslationStatusToRequestTranslation() {
        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, buildSscsDocuments(true), buildSscsWelshDocuments(DocumentType.SSCS1.getValue()), null, State.VALID_APPEAL);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNotNull(caseData.getOriginalDocuments());
        assertEquals("english.pdf", caseData.getOriginalDocuments().getListItems().get(0).getCode());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE.getId(),
            caseData.getSscsDocument().get(0).getValue().getDocumentTranslationStatus().getId());
        assertEquals("Yes", caseData.getTranslationWorkOutstanding());
        assertEquals("english.pdf",
            caseData.getSscsWelshDocuments().get(0).getValue().getOriginalDocumentFileName());
        assertEquals("welsh",
            caseData.getSscsWelshDocuments().get(0).getValue().getDocumentLanguage());
        assertEquals(EventType.SEND_TO_DWP.getCcdType(), caseData.getSscsWelshPreviewNextEvent());

    }

    @Test
    public void updateCaseWhenRip1DwpDocumentSetToRequestTranslationStatusToRequestTranslation() {
        Callback<SscsCaseData> callback = buildCallback("rip1.pdf", UPLOAD_WELSH_DOCUMENT, null, buildSscsWelshDocuments("rip1Document"), buildDwpDocuments(), State.VALID_APPEAL);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNotNull(caseData.getOriginalDocuments());
        assertEquals("rip1.pdf", caseData.getOriginalDocuments().getListItems().get(0).getCode());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE.getId(),
                caseData.getDwpDocuments().get(0).getValue().getDocumentTranslationStatus().getId());
        assertEquals("No", caseData.getTranslationWorkOutstanding());
        assertEquals("rip1.pdf",
                caseData.getSscsWelshDocuments().get(0).getValue().getOriginalDocumentFileName());
        assertEquals("welsh",
                caseData.getSscsWelshDocuments().get(0).getValue().getDocumentLanguage());
        assertNull(caseData.getSscsWelshPreviewNextEvent());
    }

    @Test
    public void shouldUpdateWithDirectionIssuedWelshNextEventCorrectlyBasedOnDirectionNoticeDocumentType() {

        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, Arrays.asList(buildSscsDocument("filename", "docUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.DIRECTION_NOTICE.getValue(), "A")), buildSscsWelshDocuments(DocumentType.DIRECTION_NOTICE.getValue()), null, State.VALID_APPEAL);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(EventType.DIRECTION_ISSUED_WELSH.getCcdType(), caseData.getSscsWelshPreviewNextEvent());
    }

    @Test
    public void shouldUpdateWithDecisionIssuedWelshNextEventCorrectlyBasedOnDecisionNoticeDocumentType() {

        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, Arrays.asList(buildSscsDocument("filename", "docUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.DECISION_NOTICE.getValue(), "A")), buildSscsWelshDocuments(DocumentType.DECISION_NOTICE.getValue()), null, State.VALID_APPEAL);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(EventType.DECISION_ISSUED_WELSH.getCcdType(), caseData.getSscsWelshPreviewNextEvent());
    }

    @Test
    public void shouldUpdateWithDecisionIssuedWelshNextEventCorrectlyBasedOnReinstatementRequestDocumentType() {

        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, Arrays.asList(buildSscsDocument("filename", "docUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.REINSTATEMENT_REQUEST.getValue(), "A")), buildSscsWelshDocuments(DocumentType.REINSTATEMENT_REQUEST.getValue()), null, State.VALID_APPEAL);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(caseData.getSscsWelshPreviewNextEvent());
    }

    @Test
    public void shouldUpdateWithUploadWelshDocumentEventCorrectlyBasedOnAppellantEvidenceDocumentType() {

        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, Arrays.asList(buildSscsDocument("filename", "docUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.APPELLANT_EVIDENCE.getValue(), null)), buildSscsWelshDocuments(DocumentType.APPELLANT_EVIDENCE.getValue()), null, State.VALID_APPEAL);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(caseData.getSscsWelshPreviewNextEvent());

    }

    @Test
    public void shouldUpdateWithFinalDecisionIssuedWelshNextEventCorrectlyBasedOnDecisionNoticeDocumentType() {

        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, Arrays.asList(buildSscsDocument("filename", "docUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.FINAL_DECISION_NOTICE.getValue(), "A")), buildSscsWelshDocuments(DocumentType.FINAL_DECISION_NOTICE.getValue()), null, State.VALID_APPEAL);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.VALID_APPEAL);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(EventType.ISSUE_FINAL_DECISION_WELSH.getCcdType(), caseData.getSscsWelshPreviewNextEvent());
    }

    @Test
    public void shouldAddBundleAdditionForAppellantEvidenceDocumentType() {

        Callback<SscsCaseData> callback = buildCallback("Addition A - my filename.pdf", UPLOAD_WELSH_DOCUMENT, Arrays.asList(buildSscsDocument("Addition A - my filename.pdf", "docUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.APPELLANT_EVIDENCE.getValue(), "A")), buildSscsWelshDocuments(DocumentType.APPELLANT_EVIDENCE.getValue()), null, State.VALID_APPEAL);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String documentFooterText = "Appellant evidence";
        SscsWelshDocumentDetails welshDocumentDetails = caseData.getSscsWelshPreviewDocuments().get(0).getValue();
        when(welshFooterService.addFooter(welshDocumentDetails.getDocumentLink(), documentFooterText, "A")).thenReturn(DocumentLink.builder().documentFilename("New Doc").build());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(caseData.getSscsWelshPreviewNextEvent());
        SscsWelshDocumentDetails sscsWelshDocumentDetails = caseData.getSscsWelshDocuments().get(0).getValue();
        assertEquals("Addition A - my filename.pdf",
            sscsWelshDocumentDetails.getOriginalDocumentFileName());
        assertEquals("WEL-A",
            sscsWelshDocumentDetails.getBundleAddition());
        assertEquals("Addition WEL-A - my filename.pdf",
            sscsWelshDocumentDetails.getDocumentFileName());
        assertEquals("No",
            sscsWelshDocumentDetails.getEvidenceIssued());

    }

    @Test
    public void givenInterlocReviewStateshouldNotSetReviewState() {

        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, Arrays.asList(buildSscsDocument("english.pdf", "docUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.APPELLANT_EVIDENCE.getValue(), null)), buildSscsWelshDocuments(DocumentType.APPELLANT_EVIDENCE.getValue()), null, State.INTERLOCUTORY_REVIEW_STATE);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setState(State.INTERLOCUTORY_REVIEW_STATE);
        caseData.setWelshInterlocNextReviewState("reviewByTcw");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(REVIEW_BY_TCW, caseData.getInterlocReviewState());
        assertNull(caseData.getSscsWelshPreviewNextEvent());
    }

    private Object[] generateCanHandleScenarios() {
        Callback<SscsCaseData> callbackWithValidEventOption =
            buildCallback("callbackWithValidEventOption", UPLOAD_WELSH_DOCUMENT, buildSscsDocuments(false), buildSscsWelshDocuments(DocumentType.SSCS1.getValue()), null, State.VALID_APPEAL);
        return new Object[]{new Object[]{ABOUT_TO_SUBMIT, callbackWithValidEventOption, true}};
    }

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode, EventType eventType,
                                                 List<SscsDocument> sscsDocuments, List<SscsWelshDocument> welshDocuments, List<DwpDocument> dwpDocuments, State state) {

        final DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
            Collections.singletonList(new DynamicListItem(dynamicListItemCode, "label")));

        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .originalDocuments(dynamicList)
            .sscsDocument(sscsDocuments)
            .dwpDocuments(dwpDocuments)
            .sscsWelshPreviewDocuments(welshDocuments)
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            state, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), eventType, false);
    }

    private List<SscsDocument> buildSscsDocuments(boolean moreThanOneDoc) {

        SscsDocument sscs1Doc = buildSscsDocument("english.pdf", "/anotherUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.SSCS1.getValue(), "A");

        SscsDocument sscs2Doc = buildSscsDocument("anything.pdf", "/anotherUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.SSCS1.getValue(), "A");

        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscs1Doc);
        if (moreThanOneDoc) {
            sscsDocuments.add(sscs2Doc);
        }
        return sscsDocuments;
    }

    private List<DwpDocument> buildDwpDocuments() {

        DwpDocument dwpDoc1 = buildDwpDocument("english.mp3", "/anotherUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.AUDIO_DOCUMENT.getValue());

        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(dwpDoc1);
        return dwpDocuments;
    }

    private List<SscsWelshDocument> buildSscsWelshDocuments(String documentType) {
        return Arrays.asList(SscsWelshDocument.builder()
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

    private List<SscsWelshDocument> buildInvalidSscsWelshDocuments(String documentType) {
        return Arrays.asList(SscsWelshDocument.builder()
            .value(SscsWelshDocumentDetails.builder()
                .documentLanguage("welsh")
                .documentType(documentType)
                .build())
            .build());
    }

    private SscsDocument buildSscsDocument(String filename, String documentUrl, SscsDocumentTranslationStatus translationRequested, String documentType, String bundleAddition) {
        return SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl(documentUrl)
                    .documentFilename(filename)
                    .build())
                .documentTranslationStatus(translationRequested)
                .documentType(documentType)
                .documentFileName(filename)
                .bundleAddition(bundleAddition)
                .build())
            .build();
    }

    private DwpDocument buildDwpDocument(String filename, String documentUrl, SscsDocumentTranslationStatus translationRequested, String documentType) {
        return DwpDocument.builder()
                .value(DwpDocumentDetails.builder()
                        .avDocumentLink(DocumentLink.builder()
                                .documentUrl(documentUrl)
                                .documentFilename(filename)
                                .build())
                        .documentTranslationStatus(translationRequested)
                        .documentType(documentType)
                        .documentFileName(filename)
                        .documentLink(DocumentLink.builder()
                                .documentUrl("rip1url")
                                .documentFilename("rip1.pdf")
                                .build())
                        .build())
                .build();
    }

}
