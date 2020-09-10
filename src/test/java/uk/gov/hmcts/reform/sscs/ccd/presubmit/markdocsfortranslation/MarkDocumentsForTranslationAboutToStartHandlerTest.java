package uk.gov.hmcts.reform.sscs.ccd.presubmit.markdocsfortranslation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;


@RunWith(JUnitParamsRunner.class)
public class MarkDocumentsForTranslationAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private MarkDocumentsForTranslationAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new MarkDocumentsForTranslationAboutToStartHandler();
        when(callback.getEvent()).thenReturn(EventType.MARK_DOCS_FOR_TRANSATION);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }


    @Test
    public void handleNonWelshCase() {
        sscsCaseData = CaseDataUtils.buildMinimalCaseData();
        sscsCaseData.setLanguagePreferenceWelsh("No");
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        if (response.getErrors().stream().findAny().isPresent()) {
            assertEquals("Error: This action is only available for Welsh cases.",
                    response.getErrors().stream().findAny().get());
        }
    }

    @Test
    public void handleWelshCase() {
        sscsCaseData = CaseDataUtils.buildMinimalCaseData();
        sscsCaseData.setLanguagePreferenceWelsh("Yes");
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
    }
}