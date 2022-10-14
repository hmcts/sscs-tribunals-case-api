package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionposthearingapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.CORRECTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.LIBERTY_TO_APPLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.PERMISSION_TO_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.SET_ASIDE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.STATEMENT_OF_REASONS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ACTION_POST_HEARING_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingApplication;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdCallbackMap;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.LibertyToApplyActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PermissionToAppealActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.StatementOfReasonsActions;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class ActionPostHearingApplicationSubmittedHandlerTest {

    private static final String DOCUMENT_URL = "dm-store/documents/123";

    private static final String USER_AUTHORISATION = "Bearer token";
    public static final long CASE_ID = 1234L;

    private ActionPostHearingApplicationSubmittedHandler handler;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private SscsCaseDetails caseDetailsCallback;

    private SscsCaseData caseData;
    private AutoCloseable autoCloseable;
    private IdamTokens idamTokens;

    @Before
    public void setUp() {
        autoCloseable = openMocks(this);
        handler = new ActionPostHearingApplicationSubmittedHandler(ccdService, idamService, true);

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        when(callback.getEvent()).thenReturn(ACTION_POST_HEARING_APPLICATION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        caseData = SscsCaseData.builder()
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST).build())
            .ccdCaseId(String.valueOf(CASE_ID))
            .documentGeneration(DocumentGeneration.builder()
                .directionNoticeContent("Body Content")
                .build())
            .documentStaging(DocumentStaging.builder()
                .previewDocument(DocumentLink.builder()
                    .documentUrl(DOCUMENT_URL)
                    .documentBinaryUrl(DOCUMENT_URL + "/binary")
                    .documentFilename("decisionIssued.pdf")
                    .build())
                .build())
            .actionPostHearingApplication(ActionPostHearingApplication.builder()
                .typeSelected(SET_ASIDE)
                .build())
            .build();

        when(caseDetailsCallback.getData()).thenReturn(caseData);
        when(caseDetails.getCaseData()).thenReturn(caseData);
    }

    @After
    public void after() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void givenAValidSubmittedEvent_thenReturnTrue() {
        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @Test
    public void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new ActionPostHearingApplicationSubmittedHandler(ccdService, idamService, false);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }


    @Test
    @Parameters({"GRANT", "REFUSE", "ISSUE_DIRECTIONS"})
    public void givenActionTypeSetAsideSelected_shouldReturnCallCorrectCallback(SetAsideActions value) {
        caseData.getActionPostHearingApplication().setTypeSelected(SET_ASIDE);
        caseData.getActionPostHearingApplication().getActionSetAside().setAction(value);

        verifyCcdUpdatedCorrectly(value);
    }

    @Test
    public void givenActionTypeSetAsideWithSorSelected_shouldReturnCallCorrectCallback() {
        caseData.getActionPostHearingApplication().setTypeSelected(SET_ASIDE);
        caseData.getActionPostHearingApplication().getActionSetAside().setAction(SetAsideActions.REFUSE);
        caseData.getActionPostHearingApplication().getActionSetAside().setRequestStatementOfReasons(YES);

        verifyCcdUpdatedCorrectly(SetAsideActions.REFUSE_SOR);
    }


    @Test
    @Parameters({"GRANT", "REFUSE", "SEND_TO_JUDGE"})
    public void givenActionTypeCorrectionSelected_shouldReturnCallCorrectCallback(CorrectionActions value) {
        caseData.getActionPostHearingApplication().setTypeSelected(CORRECTION);
        caseData.getActionPostHearingApplication().getActionCorrection().setAction(value);

        verifyCcdUpdatedCorrectly(value);
    }

    @Test
    @Parameters({"GRANT", "REFUSE", "ISSUE_DIRECTIONS", "WRITE"})
    public void givenActionTypeSorSelected_shouldReturnCallCorrectCallback(StatementOfReasonsActions value) {
        caseData.getActionPostHearingApplication().setTypeSelected(STATEMENT_OF_REASONS);
        caseData.getActionPostHearingApplication().getActionStatementOfReasons().setAction(value);

        verifyCcdUpdatedCorrectly(value);
    }

    @Test
    @Parameters({"REFUSE"})
    public void givenActionTypePtaSelected_shouldReturnCallCorrectCallback(PermissionToAppealActions value) {
        caseData.getActionPostHearingApplication().setTypeSelected(PERMISSION_TO_APPEAL);
        caseData.getActionPostHearingApplication().getActionPermissionToAppeal().setAction(value);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verifyNoInteractions(ccdService);
    }

    @Test
    @Parameters({"REFUSE"})
    public void givenActionTypeLtaSelected_shouldReturnCallCorrectCallback(LibertyToApplyActions value) {
        caseData.getActionPostHearingApplication().setTypeSelected(LIBERTY_TO_APPLY);
        caseData.getActionPostHearingApplication().getActionLibertyToApply().setAction(value);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verifyNoInteractions(ccdService);
    }


    @Test
    public void givenNoActionTypeSelected_shouldReturnWithTheCorrectErrorMessage() {
        caseData.getActionPostHearingApplication().setTypeSelected(null);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1).containsOnly(
            "Invalid Action Post Hearing Application Type Selected null or action selected as callback is null");
    }

    @Test
    public void givenNonLaCase_shouldReturnErrorWithCorrectMessage() {
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder()
            .hearingRoute(GAPS)
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("Cannot process Action Post Hearing Application on non Scheduling & Listing Case");
    }

    private void verifyCcdUpdatedCorrectly(CcdCallbackMap callbackMap) {
        when(ccdService.updateCase(caseData, CASE_ID, callbackMap.getCallbackEvent().getCcdType(),
            callbackMap.getCallbackSummary(), callbackMap.getCallbackDescription(),
            idamTokens))
            .thenReturn(caseDetailsCallback);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verify(ccdService, times(1))
            .updateCase(caseData, CASE_ID, callbackMap.getCallbackEvent().getCcdType(),
                callbackMap.getCallbackSummary(), callbackMap.getCallbackDescription(),
                idamTokens);
    }
}
