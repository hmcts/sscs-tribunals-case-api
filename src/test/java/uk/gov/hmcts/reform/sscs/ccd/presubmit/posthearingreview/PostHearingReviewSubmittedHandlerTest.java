package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService.ConditionalUpdateResult;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


@ExtendWith(MockitoExtension.class)
class PostHearingReviewSubmittedHandlerTest {

    private static final String DOCUMENT_URL = "dm-store/documents/123";

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final long CASE_ID = 1234L;
    private static final String USER_ID = "16";
    private static final String AUTHORIZATION = "authorization";
    private static final String SERVICE_AUTHORIZATION = "serviceAuthorization";

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

    @Captor
    private ArgumentCaptor<Function<SscsCaseDetails, ConditionalUpdateResult>> functionArgumentCaptor;

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
    @EnumSource(value = SetAsideActions.class, names = {"GRANT"})
    void givenActionTypeSetAsideSelectedIsNotRefuse_shouldNotUpdateCaseWIthCcdService(SetAsideActions action) {
        caseData.getPostHearing().setReviewType(SET_ASIDE);
        caseData.getPostHearing().getSetAside().setAction(action);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(idamService.getIdamTokens())
                .thenReturn(IdamTokens.builder()
                        .idamOauth2Token(AUTHORIZATION)
                        .serviceAuthorization(SERVICE_AUTHORIZATION)
                        .userId(USER_ID)
                        .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(updateCcdCaseService).updateCaseV2Conditional(
                any(Long.class),
                eq(EventType.SOR_REQUEST.getCcdType()),
                any(IdamTokens.class),
                functionArgumentCaptor.capture()
        );

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();

        assertThat(functionArgumentCaptor.getValue().apply(sscsCaseDetails).willCommit()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = YesNo.class, names = {"NO"})
    @NullSource
    void givenRefusedSetAsideSelected_andNoStatementOfReasons_shouldReturnCorrectCallback_andUpdateCase(YesNo requestSor) {
        caseData.getPostHearing().setReviewType(SET_ASIDE);
        caseData.getPostHearing().getSetAside().setAction(REFUSE);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(idamService.getIdamTokens())
                .thenReturn(IdamTokens.builder()
                        .idamOauth2Token(AUTHORIZATION)
                        .serviceAuthorization(SERVICE_AUTHORIZATION)
                        .userId(USER_ID)
                        .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(updateCcdCaseService).updateCaseV2Conditional(
                any(Long.class),
                eq(EventType.SOR_REQUEST.getCcdType()),
                any(IdamTokens.class),
                functionArgumentCaptor.capture()
        );

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();

        assertThat(functionArgumentCaptor.getValue().apply(sscsCaseDetails).willCommit()).isFalse();
    }

    @Test
    void givenRefusedSetAsideSelected_andStatementOfReasonsRequested_shouldReturnCallCorrectCallback() {
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

        when(idamService.getIdamTokens())
                .thenReturn(IdamTokens.builder()
                        .idamOauth2Token(AUTHORIZATION)
                        .serviceAuthorization(SERVICE_AUTHORIZATION)
                        .userId(USER_ID)
                        .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(updateCcdCaseService).updateCaseV2Conditional(
                any(Long.class),
                eq(EventType.SOR_REQUEST.getCcdType()),
                any(IdamTokens.class),
                functionArgumentCaptor.capture()
        );

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(returnedCase).build();

        assertThat(functionArgumentCaptor.getValue().apply(sscsCaseDetails).willCommit()).isTrue();

        assertThat(response.getErrors()).isEmpty();

        verify(ccdCallbackMapService, times(1))
                .handleCcdCallbackMap(REFUSE_SOR, returnedCase);

        assertThat(sscsCaseDetails.getData().getState()).isEqualTo(State.POST_HEARING);
        assertThat(sscsCaseDetails.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.NONE);
    }

    @Test
    void givenRefusedSetAsideSelected_andStatementOfReasonsNotRequested_shouldReturnCallCorrectCallback() {
        caseData.getPostHearing().setReviewType(SET_ASIDE);
        caseData.getPostHearing().getSetAside().setAction(REFUSE);

        caseData.getPostHearing().getSetAside().setRequestStatementOfReasons(YesNo.NO);

        verifyCcdCallbackCalledCorrectly(SetAsideActions.REFUSE);
    }

    @ParameterizedTest
    @EnumSource(value = CorrectionActions.class)
    void givenActionTypeCorrectionSelected_shouldReturnCallCorrectCallback(CorrectionActions value) {
        caseData.getPostHearing().setReviewType(CORRECTION);
        caseData.getPostHearing().getCorrection().setAction(value);

        verifyCcdCallbackCalledCorrectly(value);
    }

    @ParameterizedTest
    @EnumSource(value = StatementOfReasonsActions.class)
    void givenActionTypeSorSelected_shouldReturnCallCorrectCallback(StatementOfReasonsActions value) {
        caseData.getPostHearing().setReviewType(STATEMENT_OF_REASONS);
        caseData.getPostHearing().getStatementOfReasons().setAction(value);

        verifyCcdCallbackCalledCorrectly(value);
    }

    @ParameterizedTest
    @EnumSource(value = PermissionToAppealActions.class)
    void givenActionTypePtaSelected_shouldReturnCallCorrectCallback(PermissionToAppealActions value) {
        caseData.getPostHearing().setReviewType(PERMISSION_TO_APPEAL);
        caseData.getPostHearing().getPermissionToAppeal().setAction(value);

        verifyCcdCallbackCalledCorrectly(value);
    }

    @ParameterizedTest
    @EnumSource(value = LibertyToApplyActions.class)
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
        when(idamService.getIdamTokens())
                .thenReturn(IdamTokens.builder()
                        .idamOauth2Token(AUTHORIZATION)
                        .serviceAuthorization(SERVICE_AUTHORIZATION)
                        .userId(USER_ID)
                        .build());

        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(ccdCallbackMapService.handleCcdCallbackMap(callbackMap, caseData))
            .thenReturn(SscsCaseData.builder().ccdCaseId("123").build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(updateCcdCaseService).updateCaseV2Conditional(
                any(Long.class),
                eq(EventType.SOR_REQUEST.getCcdType()),
                any(IdamTokens.class),
                functionArgumentCaptor.capture()
        );

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();

        functionArgumentCaptor.getValue().apply(sscsCaseDetails);

        //assertThat(functionArgumentCaptor.getValue().apply(sscsCaseDetails).willCommit()).isTrue();

        assertThat(response.getErrors()).isEmpty();

        verify(ccdCallbackMapService, times(1))
            .handleCcdCallbackMap(callbackMap, caseData);
    }
}
