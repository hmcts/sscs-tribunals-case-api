package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@ExtendWith(MockitoExtension.class)
public class IssueAdjournmentNoticeSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private IssueAdjournmentNoticeSubmittedHandler handler;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new IssueAdjournmentNoticeSubmittedHandler(true, ccdService, idamService);

        caseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .state(HEARING)
            .adjournment(Adjournment.builder()
                .generateNotice(YES)
                .typeOfHearing(AdjournCaseTypeOfHearing.VIDEO)
                .canCaseBeListedRightAway(YES)
                .areDirectionsBeingMadeToParties(NO)
                .directionsDueDateDaysOffset(AdjournCaseDaysOffset.FOURTEEN_DAYS)
                .directionsDueDate(LocalDate.now().plusMonths(1))
                .typeOfNextHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE)
                .nextHearingVenue(AdjournCaseNextHearingVenue.SOMEWHERE_ELSE)
                .nextHearingVenueSelected(new DynamicList(new DynamicListItem("",""), List.of(new DynamicListItem("", ""))))
                .panelMembersExcluded(AdjournCasePanelMembersExcluded.NO)
                .nextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD)
                .nextHearingListingDuration(1)
                .nextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS)
                .interpreterRequired(NO)
                .interpreterLanguage(new DynamicList("Spanish"))
                .nextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER)
                .nextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD)
                .nextHearingFirstAvailableDateAfterDate(null)
                .nextHearingFirstAvailableDateAfterPeriod(AdjournCaseNextHearingPeriod.NINETY_DAYS)
                .nextHearingDateOrTime("")
                .reasons(List.of(new CollectionItem<>(null, "")))
                .additionalDirections(List.of(new CollectionItem<>(null, "")))
                .previewDocument(DocumentLink.builder().build())
                .generatedDate(LocalDate.now())
                .adjournmentInProgress(YES)
                .build())
            .build();
    }

    @Test
    void givenAValidSubmittedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.ISSUE_ADJOURNMENT_NOTICE);
        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @Test
    void givenAnInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenAnInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenAdjournmentEnabledFalse_thenReturnFalse() {
        handler = new IssueAdjournmentNoticeSubmittedHandler(false, ccdService, idamService);
        when(callback.getEvent()).thenReturn(EventType.ISSUE_ADJOURNMENT_NOTICE);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenAnIssueAdjournmentEvent_thenRemoveAllPropertiesExceptAdjournmentInProgressWhichIsSetToNo() {
        when(callback.getEvent()).thenReturn(EventType.ISSUE_ADJOURNMENT_NOTICE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        Adjournment adjournment = response.getData().getAdjournment();

        assertThat(adjournment).hasAllNullFieldsOrPropertiesExcept("adjournmentInProgress");
        assertThat(adjournment.getAdjournmentInProgress()).isEqualTo(null);
    }

}
