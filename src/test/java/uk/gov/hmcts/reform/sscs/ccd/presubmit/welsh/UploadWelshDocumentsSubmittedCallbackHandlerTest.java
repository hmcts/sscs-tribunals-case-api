package uk.gov.hmcts.reform.sscs.ccd.presubmit.welsh;


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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocuments;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_WELSH_DOCUMENT;

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
    }

    @Test
    @Parameters(method = "generateCanHandleScenarios")
    public void givenCanHandleIsCalled_shouldReturnCorrectResult(CallbackType callbackType,
                                                                 Callback<SscsCaseData> callback,
                                                                 boolean expectedResult) {
        boolean actualResult = handler.canHandle(callbackType, callback);

        assertEquals(expectedResult, actualResult);
    }

    private Object[] generateCanHandleScenarios() {
        Callback<SscsCaseData> callbackWithValidEventOption =
                buildCallback("callbackWithValidEventOption", UPLOAD_WELSH_DOCUMENT, false);
        return new Object[]{
                new Object[]{SUBMITTED, callbackWithValidEventOption, true}
        };
    }

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode, EventType eventType,
                                                 boolean moreThanOneDoc) {
        DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
                Collections.singletonList(new DynamicListItem(dynamicListItemCode, "label")));

        SscsDocument sscs1Doc = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder()
                                .documentUrl("/anotherUrl")
                                .documentFilename("english.pdf")
                                .build())
                        .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)
                        .documentType("sscs1")
                        .build())
                .build();

        SscsDocument sscs2Doc = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder()
                                .documentUrl("/anotherUrl")
                                .documentFilename("anything.pdf")
                                .build())
                        .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)
                        .documentType("sscs1")
                        .build())
                .build();

        SscsWelshDocuments sscs1WelshDoc = SscsWelshDocuments.builder()
                .value(SscsWelshDocumentDetails.builder()
                        .documentLink(DocumentLink.builder()
                                .documentUrl("/anotherUrl")
                                .documentFilename("welsh.pdf")
                                .build())
                        .documentLanguage("welsh")
                        .documentType("sscs1")
                        .build())
                .build();

        List<SscsDocument> oneDoc = new ArrayList<>();
        oneDoc.add(sscs1Doc);
        if(moreThanOneDoc) {
            oneDoc.add(sscs2Doc);
        }

        List<SscsWelshDocuments> welshDoc = new ArrayList<>();
        welshDoc.add(sscs1WelshDoc);

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .originalDocuments(dynamicList)
                .sscsDocument(oneDoc)
                .sscsWelshPreviewDocuments(welshDoc)
                .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.VALID_APPEAL, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), eventType, false);
    }

    @Test
    public void updateCaseWhenOnlyOneDocumentAndOnlyOneSetToRequestTranslationStatusTORequestTranslation() {
        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, false);
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);
        given(ccdService.updateCase(captor.capture(), anyLong(), eq("uploadWelshDocument"),
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
    public void updateCaseWhenOnlyOneDocumentAndMorethanOneSetToRequestTranslationStatusTORequestTranslation() {
        Callback<SscsCaseData> callback = buildCallback("english.pdf", UPLOAD_WELSH_DOCUMENT, true);
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);
        given(ccdService.updateCase(captor.capture(), anyLong(), eq("uploadWelshDocument"),
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

    }
}