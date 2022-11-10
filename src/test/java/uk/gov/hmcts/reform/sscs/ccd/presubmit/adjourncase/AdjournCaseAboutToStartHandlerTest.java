package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@ExtendWith(MockitoExtension.class)
class AdjournCaseAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private AdjournCaseAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {
        handler = new AdjournCaseAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .appeal(Appeal.builder().build())
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
                .disabilityQualifiedPanelMemberName("")
                .medicallyQualifiedPanelMemberName("")
                .otherPanelMemberName("")
                .nextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD)
                .nextHearingListingDuration(1)
                .nextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS)
                .interpreterRequired(NO)
                .interpreterLanguage(new DynamicList(new DynamicListItem("spanish", "Spanish"), List.of()))
                .nextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER)
                .nextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD)
                .nextHearingDateOrTime("")
                .nextHearingFirstAvailableDateAfterDate(LocalDate.now())
                .nextHearingFirstAvailableDateAfterPeriod(AdjournCaseNextHearingPeriod.NINETY_DAYS)
                .reasons(List.of(new CollectionItem<>(null, "")))
                .additionalDirections(List.of(new CollectionItem<>(null, "")))
                .isAdjournmentInProgress(YES)
                .build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenANonAdjournCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenCaseHasAdjournedFieldsPopulatedAndNoDraftAdjournedDocs_thenClearTransientFields() {

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(ADJOURNMENT_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));

        sscsCaseData.setSscsDocument(documentList);

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getAdjournment().getGenerateNotice()).isNull();
        assertThat(sscsCaseData.getAdjournment().getTypeOfHearing()).isNull();
        assertThat(sscsCaseData.getAdjournment().getCanCaseBeListedRightAway()).isNull();
        assertThat(sscsCaseData.getAdjournment().getAreDirectionsBeingMadeToParties()).isNull();
        assertThat(sscsCaseData.getAdjournment().getDirectionsDueDateDaysOffset()).isNull();
        assertThat(sscsCaseData.getAdjournment().getDirectionsDueDate()).isNull();
        assertThat(sscsCaseData.getAdjournment().getTypeOfNextHearing()).isNull();
        assertThat(sscsCaseData.getAdjournment().getNextHearingVenue()).isNull();
        assertThat(sscsCaseData.getAdjournment().getNextHearingVenueSelected()).isNull();
        assertThat(sscsCaseData.getAdjournment().getPanelMembersExcluded()).isNull();
        assertThat(sscsCaseData.getAdjournment().getDisabilityQualifiedPanelMemberName()).isNull();
        assertThat(sscsCaseData.getAdjournment().getMedicallyQualifiedPanelMemberName()).isNull();
        assertThat(sscsCaseData.getAdjournment().getOtherPanelMemberName()).isNull();
        assertThat(sscsCaseData.getAdjournment().getNextHearingListingDurationType()).isNull();
        assertThat(sscsCaseData.getAdjournment().getNextHearingListingDuration()).isNull();
        assertThat(sscsCaseData.getAdjournment().getNextHearingListingDurationUnits()).isNull();
        assertThat(sscsCaseData.getAdjournment().getInterpreterRequired()).isNull();
        assertThat(sscsCaseData.getAdjournment().getInterpreterLanguage()).isNull();
        assertThat(sscsCaseData.getAdjournment().getNextHearingDateType()).isNull();
        assertThat(sscsCaseData.getAdjournment().getNextHearingDateOrPeriod()).isNull();
        assertThat(sscsCaseData.getAdjournment().getNextHearingDateOrTime()).isNull();
        assertThat(sscsCaseData.getAdjournment().getNextHearingFirstAvailableDateAfterDate()).isNull();
        assertThat(sscsCaseData.getAdjournment().getNextHearingFirstAvailableDateAfterPeriod()).isNull();
        assertThat(sscsCaseData.getAdjournment().getReasons()).isNull();
        assertThat(sscsCaseData.getAdjournment().getAdditionalDirections()).isNull();
    }

    @Test
    void givenCaseHasAdjournedFieldsPopulatedAndDraftAdjournedDocs_thenDoNotClearTransientFields() {

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DRAFT_ADJOURNMENT_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));

        sscsCaseData.setSscsDocument(documentList);

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        Adjournment adjournment = sscsCaseData.getAdjournment();
        assertThat(adjournment.getGenerateNotice()).isEqualTo(YES);
        assertThat(adjournment.getTypeOfHearing()).isEqualTo(AdjournCaseTypeOfHearing.VIDEO);
        assertThat(adjournment.getCanCaseBeListedRightAway()).isEqualTo(YES);
        assertThat(adjournment.getAreDirectionsBeingMadeToParties()).isEqualTo(NO);
        assertThat(adjournment.getDirectionsDueDateDaysOffset()).isEqualTo(AdjournCaseDaysOffset.FOURTEEN_DAYS);
        assertThat(adjournment.getDirectionsDueDate()).isEqualTo(LocalDate.now().plusMonths(1));
        assertThat(adjournment.getTypeOfNextHearing()).isEqualTo(AdjournCaseTypeOfHearing.FACE_TO_FACE);
        assertThat(adjournment.getNextHearingVenue()).isEqualTo(AdjournCaseNextHearingVenue.SOMEWHERE_ELSE);
        assertThat(adjournment.getNextHearingVenueSelected().getValue().getCode()).isEmpty();
        assertThat(adjournment.getPanelMembersExcluded()).isEqualTo(AdjournCasePanelMembersExcluded.NO);
        assertThat(adjournment.getDisabilityQualifiedPanelMemberName()).isEmpty();
        assertThat(adjournment.getMedicallyQualifiedPanelMemberName()).isEmpty();
        assertThat(adjournment.getOtherPanelMemberName()).isEmpty();
        assertThat(adjournment.getNextHearingListingDurationType()).isEqualTo(AdjournCaseNextHearingDurationType.STANDARD);
        assertThat(adjournment.getNextHearingListingDuration()).isEqualTo(1);
        assertThat(adjournment.getNextHearingListingDurationUnits()).isEqualTo(AdjournCaseNextHearingDurationUnits.SESSIONS);
        assertThat(adjournment.getInterpreterRequired()).isEqualTo(NO);
        assertThat(adjournment.getInterpreterLanguage()).isEqualTo(new DynamicList(new DynamicListItem("spanish", "Spanish"), List.of()));
        assertThat(adjournment.getNextHearingDateType()).isEqualTo(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER);
        assertThat(adjournment.getNextHearingDateOrPeriod()).isEqualTo(AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD);
        assertThat(adjournment.getNextHearingDateOrTime()).isEmpty();
        assertThat(adjournment.getNextHearingFirstAvailableDateAfterDate()).isEqualTo(LocalDate.now());
        assertThat(adjournment.getNextHearingFirstAvailableDateAfterPeriod()).isEqualTo(AdjournCaseNextHearingPeriod.NINETY_DAYS);
        assertThat(adjournment.getReasons()).isEqualTo(List.of(new CollectionItem<>(null, "")));
        assertThat(adjournment.getAdditionalDirections()).isEqualTo(List.of(new CollectionItem<>(null, "")));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThatThrownBy(() ->
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION)
            ).isInstanceOf(IllegalStateException.class);
    }
}
