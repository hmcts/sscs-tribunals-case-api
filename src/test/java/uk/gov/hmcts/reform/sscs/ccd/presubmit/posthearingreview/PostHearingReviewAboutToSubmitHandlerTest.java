package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@ExtendWith(MockitoExtension.class)
class PostHearingReviewAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private PostHearingReviewAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new PostHearingReviewAboutToSubmitHandler(footerService, true);

        PostHearing postHearing = PostHearing.builder().requestType(PostHearingRequestType.SET_ASIDE).build();

        caseData = SscsCaseData.builder()
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST).build())
            .ccdCaseId("1234")
            .documentGeneration(DocumentGeneration.builder()
                .directionNoticeContent("Body Content")
                .build())
            .postHearing(postHearing)
            .build();
    }

    @Test
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(POST_HEARING_REVIEW);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new PostHearingReviewAboutToSubmitHandler(footerService,false);
        when(callback.getEvent()).thenReturn(POST_HEARING_REVIEW);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void shouldReturnWithoutError() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenSetAsideGranted_thenExcludePanelMembers() {
        JudicialUserBase judge = new JudicialUserBase("678", "1234");
        caseData.getPostHearing().setReviewType(PostHearingReviewType.SET_ASIDE);
        caseData.getPostHearing().getSetAside().setAction(SetAsideActions.GRANT);
        caseData.setHearings(List.of(Hearing.builder()
            .value(HearingDetails.builder()
                .panel(JudicialUserPanel.builder()
                    .assignedTo(judge)
                    .build())
                .build())
            .build()));

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getSchedulingAndListingFields()
            .getPanelMemberExclusions().getExcludedPanelMembers()
            .contains(new CollectionItem<>("", judge))).isTrue();
    }

    @Test
    void givenLtaGranted_thenReservedPanelMembers() {
        JudicialUserBase judge = new JudicialUserBase("678", "1234");
        caseData.getPostHearing().setReviewType(PostHearingReviewType.LIBERTY_TO_APPLY);
        caseData.getPostHearing().getLibertyToApply().setAction(LibertyToApplyActions.GRANT);
        caseData.setHearings(List.of(Hearing.builder()
            .value(HearingDetails.builder()
                .panel(JudicialUserPanel.builder()
                    .assignedTo(judge)
                    .build())
                .build())
            .build()));

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getSchedulingAndListingFields().getPanelMemberExclusions().getReservedPanelMembers().contains(new CollectionItem<>("", judge))).isTrue();
    }
}
