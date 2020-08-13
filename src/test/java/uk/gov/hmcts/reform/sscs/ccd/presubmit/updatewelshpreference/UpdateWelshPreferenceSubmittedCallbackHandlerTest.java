package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatewelshpreference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_WELSH_PREFERENCE;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import junitparams.JUnitParamsRunner;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class UpdateWelshPreferenceSubmittedCallbackHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private UpdateWelshPreferenceSubmittedCallbackHandler handler;
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;
    @Mock
    private Callback<SscsCaseData> callback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UpdateWelshPreferenceSubmittedCallbackHandler(ccdService, idamService);
        when(callback.getEvent()).thenReturn(UPDATE_WELSH_PREFERENCE);
    }

    @Test
    public void givenCanHandleIsCalled_shouldReturnCorrectResult() {
        boolean actualResult = handler.canHandle(SUBMITTED, callback);
        assertTrue(actualResult);
    }

    @Test
    public void handleWhenLanguagePreferenceIsEnglish() {
        Callback<SscsCaseData> callback = buildCallback();
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());
        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);
        given(ccdService.updateCase(captor.capture(), anyLong(), eq(UPDATE_WELSH_PREFERENCE.getCcdType()),
                anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        assertNotNull(captor.getValue().getTranslationWorkOutstanding());
        assertEquals("No",captor.getValue().getTranslationWorkOutstanding());
    }

    @Test
    public void handleWhenLanguagePreferenceIsWelsh() {
        Callback<SscsCaseData> callback = buildCallback();
        callback.getCaseDetails().getCaseData().setTranslationWorkOutstanding("Yes");
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("Yes");
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());
        ArgumentCaptor<SscsCaseData> captor = ArgumentCaptor.forClass(SscsCaseData.class);
        given(ccdService.updateCase(captor.capture(), anyLong(), eq(UPDATE_WELSH_PREFERENCE.getCcdType()),
                anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        assertNotNull(captor.getValue().getTranslationWorkOutstanding());
        assertEquals("Yes",captor.getValue().getTranslationWorkOutstanding());
    }

    private Callback<SscsCaseData> buildCallback() {
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


        List<SscsDocument> oneDoc = new ArrayList<>();
        oneDoc.add(sscs1Doc);
        oneDoc.add(sscs2Doc);

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsDocument(oneDoc)
                .translationWorkOutstanding("Yes")
                .languagePreferenceWelsh("No")
                .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.VALID_APPEAL, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), UPDATE_WELSH_PREFERENCE, false);
    }
}