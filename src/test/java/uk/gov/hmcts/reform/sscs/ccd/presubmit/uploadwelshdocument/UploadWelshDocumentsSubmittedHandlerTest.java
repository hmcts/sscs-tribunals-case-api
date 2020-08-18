package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_WELSH_DOCUMENT;

import java.time.LocalDateTime;
import java.util.Optional;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class UploadWelshDocumentsSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private UploadWelshDocumentsSubmittedHandler handler;

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
        handler = new UploadWelshDocumentsSubmittedHandler(ccdService, idamService);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_WELSH_DOCUMENT);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build())
            .sscsWelshPreviewNextEvent("sendToDwp")
            .build();
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
    public void shouldCallUpdateCaseWithCorrectEvent() {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(ccdService.updateCase(caseData, callback.getCaseDetails().getId(), EventType.SEND_TO_DWP.getCcdType(),
            "Upload welsh document",
            "Upload welsh document", idamTokens))
            .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        verify(ccdService).updateCase(caseData, callback.getCaseDetails().getId(), EventType.SEND_TO_DWP.getCcdType(),
            "Upload welsh document", "Upload welsh document", idamTokens);
        assertNull(caseData.getSscsWelshPreviewNextEvent());

    }

    private Object[] generateCanHandleScenarios() {
        Callback<SscsCaseData> callbackWithValidEventOption = buildCallback(EventType.SEND_TO_DWP.getCcdType());
        return new Object[] {new Object[] {SUBMITTED, buildCallback("sendToDwp"), true},
            new Object[] {ABOUT_TO_SUBMIT, buildCallback(EventType.SEND_TO_DWP.getCcdType()), false},
            new Object[] {SUBMITTED, buildCallback(null), false}
        };
    }

    private Callback<SscsCaseData> buildCallback(String sscsWelshPreviewNextEvent) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .sscsWelshPreviewNextEvent(sscsWelshPreviewNextEvent)
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            State.VALID_APPEAL, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), UPLOAD_WELSH_DOCUMENT, false);
    }

}
