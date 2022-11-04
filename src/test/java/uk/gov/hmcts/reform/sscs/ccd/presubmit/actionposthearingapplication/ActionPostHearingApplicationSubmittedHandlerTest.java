package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionposthearingapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.CORRECTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.LIBERTY_TO_APPLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.PERMISSION_TO_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.SET_ASIDE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes.STATEMENT_OF_REASONS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ACTION_POST_HEARING_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingApplication;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdCallbackMap;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
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

@ExtendWith(MockitoExtension.class)
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
    private IdamTokens idamTokens;

    @BeforeEach
    void setUp() {
        handler = new ActionPostHearingApplicationSubmittedHandler(ccdService, idamService, true);

        idamTokens = IdamTokens.builder().build();

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
    }

    @Test
    void givenAValidSubmittedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(ACTION_POST_HEARING_APPLICATION);
        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new ActionPostHearingApplicationSubmittedHandler(ccdService, idamService, false);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(
        value = SetAsideActions.class,
        names = {"GRANT", "ISSUE_DIRECTIONS"})
    void givenActionTypeSetAsideSelected_shouldReturnCallCorrectCallback(SetAsideActions value) {
        caseData.getActionPostHearingApplication().setTypeSelected(SET_ASIDE);
        caseData.getActionPostHearingApplication().getActionSetAside().setAction(value);

        verifyCcdUpdatedCorrectly(value, null);
    }

    @Test
    void givenActionTypeSetAsideSelected_shouldReturnCallCorrectCallback() {
        caseData.getActionPostHearingApplication().setTypeSelected(SET_ASIDE);
        caseData.getActionPostHearingApplication().getActionSetAside().setAction(SetAsideActions.REFUSE);

        verifyCcdUpdatedCorrectly(SetAsideActions.REFUSE, DwpState.SET_ASIDE_REFUSED.getId());
    }

    @Test
    void givenActionTypeSetAsideWithSorSelected_shouldReturnCallCorrectCallback() {
        caseData.getActionPostHearingApplication().setTypeSelected(SET_ASIDE);
        caseData.getActionPostHearingApplication().getActionSetAside().setAction(SetAsideActions.REFUSE);
        caseData.getActionPostHearingApplication().getActionSetAside().setRequestStatementOfReasons(YES);

        verifyCcdUpdatedCorrectly(SetAsideActions.REFUSE_SOR, null);
    }

    @ParameterizedTest
    @EnumSource(value = CorrectionActions.class)
    void givenActionTypeCorrectionSelected_shouldReturnCallCorrectCallback(CorrectionActions value) {
        caseData.getActionPostHearingApplication().setTypeSelected(CORRECTION);
        caseData.getActionPostHearingApplication().getActionCorrection().setAction(value);

        verifyCcdUpdatedCorrectly(value, null);
    }

    @ParameterizedTest
    @EnumSource(value = StatementOfReasonsActions.class)
    void givenActionTypeSorSelected_shouldReturnCallCorrectCallback(StatementOfReasonsActions value) {
        caseData.getActionPostHearingApplication().setTypeSelected(STATEMENT_OF_REASONS);
        caseData.getActionPostHearingApplication().getActionStatementOfReasons().setAction(value);

        verifyCcdUpdatedCorrectly(value, null);
    }

    @ParameterizedTest
    @EnumSource(value = PermissionToAppealActions.class)
    void givenActionTypePtaSelected_shouldReturnCallCorrectCallback(PermissionToAppealActions value) {
        caseData.getActionPostHearingApplication().setTypeSelected(PERMISSION_TO_APPEAL);
        caseData.getActionPostHearingApplication().getActionPermissionToAppeal().setAction(value);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        assertThat(response.getData().getDwpState()).isNull();

        verifyNoInteractions(ccdService);
    }

    @ParameterizedTest
    @EnumSource(value = LibertyToApplyActions.class)
    void givenActionTypeLtaSelected_shouldReturnCallCorrectCallback(LibertyToApplyActions value) {
        caseData.getActionPostHearingApplication().setTypeSelected(LIBERTY_TO_APPLY);
        caseData.getActionPostHearingApplication().getActionLibertyToApply().setAction(value);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        assertThat(response.getData().getDwpState()).isNull();

        verifyNoInteractions(ccdService);
    }


    @Test
    void givenNoActionTypeSelected_shouldReturnWithTheCorrectErrorMessage() {
        caseData.getActionPostHearingApplication().setTypeSelected(null);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1).containsOnly(
            "Invalid Action Post Hearing Application Type Selected null or action selected as callback is null");
    }

    @Test
    void givenNonLaCase_shouldReturnErrorWithCorrectMessage() {
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder()
            .hearingRoute(GAPS)
            .build());

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("Cannot process Action Post Hearing Application on non Scheduling & Listing Case");
    }

    private void verifyCcdUpdatedCorrectly(CcdCallbackMap callbackMap, String dwpState) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetailsCallback.getData()).thenReturn(caseData);

        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        when(ccdService.updateCase(caseData, CASE_ID, callbackMap.getCallbackEvent().getCcdType(),
            callbackMap.getCallbackSummary(), callbackMap.getCallbackDescription(),
            idamTokens))
            .thenReturn(caseDetailsCallback);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        assertThat(response.getData().getDwpState()).isEqualTo(dwpState);

        verify(ccdService, times(1))
            .updateCase(caseData, CASE_ID, callbackMap.getCallbackEvent().getCcdType(),
                callbackMap.getCallbackSummary(), callbackMap.getCallbackDescription(),
                idamTokens);
    }
}
