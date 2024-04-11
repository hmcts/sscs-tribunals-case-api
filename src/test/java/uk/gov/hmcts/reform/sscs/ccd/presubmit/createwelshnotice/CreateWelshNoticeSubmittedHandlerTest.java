package uk.gov.hmcts.reform.sscs.ccd.presubmit.createwelshnotice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_WELSH_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DIRECTION_ISSUED_WELSH;

import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.After;
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
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class CreateWelshNoticeSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private CreateWelshNoticeSubmittedHandler handler;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private AutoCloseable openedMocks;

    @Before
    public void setUp() {
        openedMocks = MockitoAnnotations.openMocks(this);
        handler = new CreateWelshNoticeSubmittedHandler(updateCcdCaseService, idamService);
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build())
            .sscsWelshPreviewNextEvent(EventType.DIRECTION_ISSUED_WELSH.getCcdType())
            .build();
        when(callback.getEvent()).thenReturn(CREATE_WELSH_NOTICE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @After
    public void tearDown() throws Exception {
        openedMocks.close();
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

        given(updateCcdCaseService.triggerCaseEventV2(anyLong(), eq(DIRECTION_ISSUED_WELSH.getCcdType()), anyString(),
            anyString(), eq(idamTokens)))
            .willReturn(SscsCaseDetails.builder().data(caseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(updateCcdCaseService).should(times(1))
            .triggerCaseEventV2(anyLong(), eq(DIRECTION_ISSUED_WELSH.getCcdType()), anyString(),
                anyString(), eq(idamTokens));

        assertNull(caseData.getSscsWelshPreviewNextEvent());

    }

    private Object[] generateCanHandleScenarios() {
        Callback<SscsCaseData> callbackWithValidEventOption = buildCallback(EventType.DIRECTION_ISSUED_WELSH.getCcdType());
        return new Object[] {new Object[] {SUBMITTED, buildCallback(DIRECTION_ISSUED_WELSH.getCcdType()), true},
            new Object[] {ABOUT_TO_SUBMIT, buildCallback(EventType.DIRECTION_ISSUED_WELSH.getCcdType()), false},
            new Object[] {SUBMITTED, buildCallback(null), false}
        };
    }

    private Callback<SscsCaseData> buildCallback(String sscsWelshPreviewNextEvent) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .sscsWelshPreviewNextEvent(sscsWelshPreviewNextEvent)
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            State.VALID_APPEAL, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), CREATE_WELSH_NOTICE, false);
    }

}
