package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatewelshpreference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_WELSH_PREFERENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.WELSH_TRANSLATION;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@RunWith(JUnitParamsRunner.class)
public class UpdateWelshPreferenceAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private UpdateWelshPreferenceAboutToSubmitHandler handler;
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private UserDetailsService userDetailsService;


    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        AddNoteService addNoteService = new AddNoteService(userDetailsService);
        handler = new UpdateWelshPreferenceAboutToSubmitHandler(addNoteService);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
                .forename("Chris").surname("Davis").build().getFullName());
        when(callback.getEvent()).thenReturn(UPDATE_WELSH_PREFERENCE);
    }

    @Test
    public void givenCanHandleIsCalled_shouldReturnCorrectResult() {
        boolean actualResult = handler.canHandle(ABOUT_TO_SUBMIT, callback);
        assertTrue(actualResult);
    }

    @Test
    public void handleWhenLanguagePreferenceIsEnglish() {
        Callback<SscsCaseData> callback = buildCallback();
        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("No",result.getData().getTranslationWorkOutstanding());
        assertEquals("No",result.getData().getLanguagePreferenceWelsh());
    }

    @Test
    public void handleWhenLanguagePreferenceIsWelsh() {
        Callback<SscsCaseData> callback = buildCallback();
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("Yes");
        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("Yes",result.getData().getLanguagePreferenceWelsh());
        assertEquals("Yes",result.getData().getTranslationWorkOutstanding());
    }

    @Test
    public void givenWelshTransaltionsInterlocStateAddNote() {
        Callback<SscsCaseData> callback = buildCallback();
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("No");
        callback.getCaseDetails().getCaseData().setInterlocReviewState(WELSH_TRANSLATION);
        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("No",result.getData().getLanguagePreferenceWelsh());
        assertEquals("No",result.getData().getTranslationWorkOutstanding());
        assertEquals(1,result.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Assigned to admin - Case no longer Welsh. Please cancel any Welsh translations",
                result.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
    }

    @Test
    public void handleWhenLanguagePreferenceIsWelshAndNoTranslationWorkOutstandingShouldStaySame() {
        Callback<SscsCaseData> callback = buildCallback();
        callback.getCaseDetails().getCaseData().setTranslationWorkOutstanding("No");
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("Yes");
        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("Yes",result.getData().getLanguagePreferenceWelsh());
        assertEquals("No",result.getData().getTranslationWorkOutstanding());
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
                State.VALID_APPEAL, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), UPDATE_WELSH_PREFERENCE, false);
    }
}
