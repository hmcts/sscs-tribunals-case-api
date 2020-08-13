package uk.gov.hmcts.reform.sscs.ccd.presubmit.welsh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_WELSH_DOCUMENT;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;

import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class UploadWelshDocumentsSubmittedCallbackHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private UploadWelshDocumentsSubmittedCallbackHandler handler;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UploadWelshDocumentsSubmittedCallbackHandler(ccdService, idamService);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_WELSH_DOCUMENT);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());
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
    public void updateCaseWhenOnlyOneDocumentAndOnlyOneSetToRequestTranslationStatusToRequestTranslation() {
        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, buildSscsDocuments(false),buildSscsWelshDocuments(DocumentType.SSCS1.getValue()));

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);
        given(ccdService.updateCase(captor.capture(), anyLong(), eq(EventType.SEND_TO_DWP.getCcdType()),
                anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        assertNotNull(captor.getValue().getOriginalDocuments());
        assertEquals("english.pdf",captor.getValue().getOriginalDocuments().getListItems().get(0).getCode());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE.getId(),
                captor.getValue().getSscsDocument().get(0).getValue().getDocumentTranslationStatus().getId());
        assertEquals("No",captor.getValue().getTranslationWorkOutstanding());
        assertEquals("english.pdf",
                captor.getValue().getSscsWelshDocuments().get(0).getValue().getOriginalDocumentFileName());
        assertEquals("welsh",
                captor.getValue().getSscsWelshDocuments().get(0).getValue().getDocumentLanguage());
    }

    @Test
    public void updateCaseWhenOnlyOneDocumentAndMorethanOneSetToRequestTranslationStatusToRequestTranslation() {
        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, buildSscsDocuments(true), buildSscsWelshDocuments(DocumentType.SSCS1.getValue()));

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);
        given(ccdService.updateCase(captor.capture(), anyLong(), eq(EventType.SEND_TO_DWP.getCcdType()),
                anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        assertNotNull(captor.getValue().getOriginalDocuments());
        assertEquals("english.pdf",captor.getValue().getOriginalDocuments().getListItems().get(0).getCode());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE.getId(),
                captor.getValue().getSscsDocument().get(0).getValue().getDocumentTranslationStatus().getId());
        assertEquals("Yes",captor.getValue().getTranslationWorkOutstanding());
        assertEquals("english.pdf",
                captor.getValue().getSscsWelshDocuments().get(0).getValue().getOriginalDocumentFileName());
        assertEquals("welsh",
                captor.getValue().getSscsWelshDocuments().get(0).getValue().getDocumentLanguage());

        verify(ccdService).updateCase(captor.capture(), anyLong(), eq(EventType.SEND_TO_DWP.getCcdType()),
                anyString(), anyString(), any(IdamTokens.class));

    }

    @Test
    public void shouldUpdateWithDirectionIssuedWelshNextEventCorrectlyBasedOnDirectionNoticeDocumentType() {

        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, Arrays.asList(buildSscsDocument("filename","docUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.DIRECTION_NOTICE.getValue())), buildSscsWelshDocuments(DocumentType.DIRECTION_NOTICE.getValue()));

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);
        given(ccdService.updateCase(captor.capture(), anyLong(), eq(EventType.DIRECTION_ISSUED_WELSH.getCcdType()),
                anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        verify(ccdService).updateCase(captor.capture(), anyLong(), eq(EventType.DIRECTION_ISSUED_WELSH.getCcdType()),
                anyString(), anyString(), any(IdamTokens.class));
    }

    @Test
    public void shouldUpdateWithDecisionIssuedWelshNextEventCorrectlyBasedOnDecisionNoticeDocumentType() {

        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, Arrays.asList(buildSscsDocument("filename","docUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.DECISION_NOTICE.getValue())), buildSscsWelshDocuments(DocumentType.DIRECTION_NOTICE.getValue()));

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);
        given(ccdService.updateCase(captor.capture(), anyLong(), eq(EventType.DIRECTION_ISSUED_WELSH.getCcdType()),
                anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        verify(ccdService).updateCase(captor.capture(), anyLong(), eq(EventType.DIRECTION_ISSUED_WELSH.getCcdType()),
                anyString(), anyString(), any(IdamTokens.class));
    }

    @Test
    public void shouldUpdateWithUploadWelshDocumentEventCorrectlyBasedOnAppellantEvidenceDocumentType() {

        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, Arrays.asList(buildSscsDocument("filename","docUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.APPELLANT_EVIDENCE.getValue())), buildSscsWelshDocuments(DocumentType.APPELLANT_EVIDENCE.getValue()));

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);
        given(ccdService.updateCase(captor.capture(), anyLong(), eq(EventType.UPLOAD_WELSH_DOCUMENT.getCcdType()),
                anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        verify(ccdService).updateCase(captor.capture(), anyLong(), eq(UPLOAD_WELSH_DOCUMENT.getCcdType()),
                anyString(), anyString(), any(IdamTokens.class));
    }

    private Object[] generateCanHandleScenarios() {
        Callback<SscsCaseData> callbackWithValidEventOption =
                buildCallback("callbackWithValidEventOption", UPLOAD_WELSH_DOCUMENT, buildSscsDocuments(false),buildSscsWelshDocuments(DocumentType.SSCS1.getValue()));
        return new Object[] {
                new Object[]{SUBMITTED, callbackWithValidEventOption, true}
        };
    }

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode, EventType eventType,
                                                 List<SscsDocument> sscsDocuments ,List<SscsWelshDocument> welshDocuments ) {

        final DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
                Collections.singletonList(new DynamicListItem(dynamicListItemCode, "label")));

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .originalDocuments(dynamicList)
                .sscsDocument(sscsDocuments)
                .sscsWelshPreviewDocuments(welshDocuments)
                .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.VALID_APPEAL, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), eventType, false);
    }

    private List<SscsDocument> buildSscsDocuments(boolean moreThanOneDoc){

        SscsDocument sscs1Doc = buildSscsDocument("english.pdf", "/anotherUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.SSCS1.getValue());

        SscsDocument sscs2Doc = buildSscsDocument("anything.pdf", "/anotherUrl", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED, DocumentType.SSCS1.getValue());

        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscs1Doc);
        if (moreThanOneDoc) {
            sscsDocuments.add(sscs2Doc);
        }
        return sscsDocuments;
    }

    private List<SscsWelshDocument> buildSscsWelshDocuments(String documentType){
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

    private SscsDocument buildSscsDocument(String filename, String documentUrl, SscsDocumentTranslationStatus translationRequested, String documentType) {
        return SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder()
                                .documentUrl(documentUrl)
                                .documentFilename(filename)
                                .build())
                        .documentTranslationStatus(translationRequested)
                        .documentType(documentType)
                        .build())
                .build();
    }

}