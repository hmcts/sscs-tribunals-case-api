package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.POST_HEARING_REVIEW;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType.CORRECTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType.LIBERTY_TO_APPLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType.PERMISSION_TO_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType.SET_ASIDE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType.STATEMENT_OF_REASONS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions.REFUSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions.REFUSE_SOR;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdCallbackMap;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.LibertyToApplyActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PermissionToAppealActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAside;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.StatementOfReasonsActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@ExtendWith(MockitoExtension.class)
class PostHearingReviewSubmittedHandlerTest {

    private static final String DOCUMENT_URL = "dm-store/documents/123";

    private static final String USER_AUTHORISATION = "Bearer token";
    public static final long CASE_ID = 1234L;

    private PostHearingReviewSubmittedHandler handler;

    @Mock
    private CcdCallbackMapService ccdCallbackMapService;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;


    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new PostHearingReviewSubmittedHandler(ccdCallbackMapService, updateCcdCaseService, idamService, true);

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
            .postHearing(PostHearing.builder()
                .reviewType(SET_ASIDE)
                .build())
            .build();
    }

    @Test
    void givenAValidSubmittedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(POST_HEARING_REVIEW);
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
        handler = new PostHearingReviewSubmittedHandler(ccdCallbackMapService, updateCcdCaseService, idamService, false);
        when(callback.getEvent()).thenReturn(POST_HEARING_REVIEW);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = SetAsideActions.class)
    void givenActionTypeSetAsideSelected_shouldReturnCallCorrectCallback(SetAsideActions value) {
        caseData.getPostHearing().setReviewType(SET_ASIDE);
        caseData.getPostHearing().getSetAside().setAction(value);

        verifyCcdCallbackCalledCorrectly(value);
    }

    @ParameterizedTest
    @EnumSource(value = SetAsideActions.class, names = {"GRANT", "REFUSE", "REFUSE_SOR"})
    void givenActionTypeSetAsideSelectedIsNotRefuse_shouldNotUpdateCaseWIthCcdService_whenCcdCallbackMapV2IsEnabled(SetAsideActions action) {
        caseData.getPostHearing().setReviewType(SET_ASIDE);
        caseData.getPostHearing().getSetAside().setAction(action);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        PostHearing returnedPostHearing = PostHearing.builder()
                .reviewType(SET_ASIDE)
                .setAside(SetAside.builder()
                        .action(action)
                        .build())
                .build();
        SscsCaseData returnedCase = SscsCaseData.builder()
                .ccdCaseId("555")
                .postHearing(returnedPostHearing)
                .build();
        Long caseId = Long.valueOf(caseData.getCcdCaseId());
        when(ccdCallbackMapService.handleCcdCallbackMapV2(eq(action), eq(caseId), isA(Consumer.class)))
                .thenReturn(returnedCase);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(ccdCallbackMapService, times(1))
                .handleCcdCallbackMapV2(eq(action), anyLong(), isA(Consumer.class));

        verifyNoInteractions(updateCcdCaseService);
    }

    @ParameterizedTest
    @EnumSource(value = YesNo.class, names = {"NO", "YES"})
    @NullSource
    void givenRefusedSetAsideSelected_andNoStatementOfReasons_whenCcdCallbackMapV2IsEnabled_shouldReturnCorrectCallback_andUpdateCase(YesNo requestSor) {
        caseData.getPostHearing().setReviewType(SET_ASIDE);
        caseData.getPostHearing().getSetAside().setAction(REFUSE);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        PostHearing returnedPostHearing = PostHearing.builder()
                .reviewType(SET_ASIDE)
                .setAside(SetAside.builder()
                        .action(REFUSE)
                        .requestStatementOfReasons(requestSor)
                        .build())
                .build();
        SscsCaseData returnedCase = SscsCaseData.builder()
                .state(State.DORMANT_APPEAL_STATE)
                .interlocReviewState(InterlocReviewState.NONE)
                .ccdCaseId("555")
                .postHearing(returnedPostHearing)
                .build();
        Long caseId = Long.valueOf(caseData.getCcdCaseId());
        when(ccdCallbackMapService.handleCcdCallbackMapV2(eq(REFUSE), eq(caseId), isA(Consumer.class)))
                .thenReturn(returnedCase);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(ccdCallbackMapService, times(1))
                .handleCcdCallbackMapV2(eq(REFUSE), anyLong(), isA(Consumer.class));

        assertThat(response.getData().getState()).isEqualTo(State.DORMANT_APPEAL_STATE);
        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.NONE);
    }

    @Test
    void givenRefusedSetAsideSelected_andStatementOfReasonsRequested_whenCcdCallbackMapV2IsEnabled_shouldReturnCallCorrectCallback() {
        caseData.getPostHearing().setReviewType(SET_ASIDE);
        caseData.getPostHearing().getSetAside().setAction(REFUSE);
        caseData.getPostHearing().getSetAside().setRequestStatementOfReasons(YES);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PostHearing returnedPostHearing = PostHearing.builder()
                .reviewType(SET_ASIDE)
                .setAside(SetAside.builder()
                        .action(REFUSE)
                        .requestStatementOfReasons(YES)
                        .build())
                .build();
        SscsCaseData returnedCase = SscsCaseData.builder()
                .state(State.POST_HEARING)
                .postHearing(returnedPostHearing)
                .interlocReviewState(InterlocReviewState.NONE)
                .ccdCaseId("555")
                .build();
        Long caseId = Long.valueOf(caseData.getCcdCaseId());

        when(ccdCallbackMapService.handleCcdCallbackMapV2(eq(REFUSE_SOR), eq(caseId), isA(Consumer.class)))
                .thenReturn(returnedCase);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(ccdCallbackMapService, times(1))
                .handleCcdCallbackMapV2(eq(REFUSE_SOR), anyLong(), isA(Consumer.class));

        verify(updateCcdCaseService, times(1)).triggerCaseEventV2(Long.valueOf(returnedCase.getCcdCaseId()),
                EventType.SOR_REQUEST.getCcdType(),
                "Send to hearing Judge for statement of reasons",
                "",
                idamService.getIdamTokens());

        assertThat(response.getData().getState()).isEqualTo(State.POST_HEARING);
        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.NONE);
    }

    @Test
    void givenRefusedSetAsideSelected_andStatementOfReasonsNotRequested_shouldReturnCallCorrectCallback() {
        caseData.getPostHearing().setReviewType(SET_ASIDE);
        caseData.getPostHearing().getSetAside().setAction(REFUSE);

        caseData.getPostHearing().getSetAside().setRequestStatementOfReasons(YesNo.NO);

        verifyCcdCallbackCalledCorrectly(SetAsideActions.REFUSE);
    }

    @ParameterizedTest
    @EnumSource(value = CorrectionActions.class, names = {"GRANT", "REFUSE"})
    void givenActionTypeCorrectionSelected_shouldReturnCallCorrectCallback(CorrectionActions value) {
        caseData.getPostHearing().setReviewType(CORRECTION);
        caseData.getPostHearing().getCorrection().setAction(value);

        verifyCcdCallbackCalledCorrectly(value);
    }

    @ParameterizedTest
    @EnumSource(value = StatementOfReasonsActions.class, names = {"GRANT", "REFUSE"})
    void givenActionTypeSorSelected_shouldReturnCallCorrectCallback(StatementOfReasonsActions value) {
        caseData.getPostHearing().setReviewType(STATEMENT_OF_REASONS);
        caseData.getPostHearing().getStatementOfReasons().setAction(value);

        verifyCcdCallbackCalledCorrectly(value);
    }

    @ParameterizedTest
    @EnumSource(value = PermissionToAppealActions.class, names = {"GRANT", "REFUSE", "REVIEW"})
    void givenActionTypePtaSelected_shouldReturnCallCorrectCallback(PermissionToAppealActions value) {
        caseData.getPostHearing().setReviewType(PERMISSION_TO_APPEAL);
        caseData.getPostHearing().getPermissionToAppeal().setAction(value);

        verifyCcdCallbackCalledCorrectly(value);
    }

    @ParameterizedTest
    @EnumSource(value = LibertyToApplyActions.class, names = {"GRANT", "REFUSE"})
    void givenActionTypeLtaSelected_shouldReturnCallCorrectCallback(LibertyToApplyActions value) {
        caseData.getPostHearing().setReviewType(LIBERTY_TO_APPLY);
        caseData.getPostHearing().getLibertyToApply().setAction(value);

        verifyCcdCallbackCalledCorrectly(value);
    }


    @Test
    void givenNoActionTypeSelected_shouldReturnWithTheCorrectErrorMessage() {
        caseData.getPostHearing().setReviewType(null);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("Invalid Action Post Hearing Application Type Selected null "
                + "or action selected as callback is null");
    }

    private void verifyCcdCallbackCalledCorrectly(CcdCallbackMap callbackMap) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(ccdCallbackMapService.handleCcdCallbackMapV2(eq(callbackMap), anyLong(), isA(Consumer.class)))
            .thenReturn(SscsCaseData.builder().ccdCaseId("123").build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(ccdCallbackMapService, times(1))
                .handleCcdCallbackMapV2(eq(callbackMap), anyLong(), isA(Consumer.class));
    }
}
