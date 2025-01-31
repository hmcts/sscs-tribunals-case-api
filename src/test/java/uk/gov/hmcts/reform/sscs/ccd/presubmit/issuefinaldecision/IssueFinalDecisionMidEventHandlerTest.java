package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.io.IOException;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class IssueFinalDecisionMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamClient idamClient;

    private SscsCaseData sscsCaseData;

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        handler = new IssueFinalDecisionMidEventHandler();

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getPageId()).thenReturn("previewDecisionNotice");

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenIssueFinalDecisionEventType_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);

        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonHandledCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenIssueFinalDecisionEventWithDeathOfAppellant_thenDisplayWarning() {
        sscsCaseData.setIsAppellantDeceased(YesNo.YES);
        when(callback.isIgnoreWarnings()).thenReturn(false);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String warning = response.getWarnings().stream().findFirst().orElse("");
        assertThat(warning, is("Appellant is deceased. Copy the draft decision and amend offline, then upload the offline version."));
    }

    @Test
    public void givenIssueFinalDecisionEventWithDeathOfAppellantWithIgnoreWarnings_thenDoNotDisplayWarning() {
        when(callback.isIgnoreWarnings()).thenReturn(true);
        sscsCaseData.setIsAppellantDeceased(YesNo.YES);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenIssueFinalDecisionEventWithDeathOfAppellantButNotOnTheCorrectPage_thenDoNotDisplayWarning() {
        when(callback.getPageId()).thenReturn("incorrectPage");
        when(callback.isIgnoreWarnings()).thenReturn(false);
        sscsCaseData.setIsAppellantDeceased(YesNo.YES);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenIssueFinalDecisionEventWithNoDeathOfAppellant_thenDoNotDisplayWarning() {
        sscsCaseData.setIsAppellantDeceased(YesNo.NO);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size(), is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheEvent() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }
}
