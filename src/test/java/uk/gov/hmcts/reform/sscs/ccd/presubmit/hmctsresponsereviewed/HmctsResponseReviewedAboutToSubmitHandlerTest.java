package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class HmctsResponseReviewedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private HmctsResponseReviewedAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new HmctsResponseReviewedAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.HMCTS_RESPONSE_REVIEWED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .selectWhoReviewsCase(new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .benefitCode("002")
                .issueCode("CC")
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonHmctsResponseReviewedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAHmctsResponseReviewedEventWithNoDwpResponseDate_thenSetCaseCodeAndDefaultDwpResponseDateToToday() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenAHmctsResponseReviewedEventWithDwpResponseDate_thenSetCaseCodeAndUseProvidedDwpResponseDate() {
        callback.getCaseDetails().getCaseData().setDwpResponseDate(LocalDate.now().minusDays(1).toString());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().minusDays(1).toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenAHmctsResponseReviewedWithEmptyBenefitCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setBenefitCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Benefit code cannot be empty", error);
        }
    }

    @Test
    public void givenAHmctsResponseReviewedWithEmptyIssueCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be empty", error);
        }
    }

    @Test
    public void givenAHmctsResponseReviewedWithIssueCodeSetToDD_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode("DD");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be set to the default value of DD", error);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
