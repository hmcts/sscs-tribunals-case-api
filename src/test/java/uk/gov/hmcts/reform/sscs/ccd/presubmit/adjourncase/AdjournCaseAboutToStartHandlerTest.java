package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingVenue;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCasePanelMembersExcluded;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

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
                .interpreterLanguage("spanish")
                .nextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER)
                .nextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD)
                .nextHearingDateOrTime("")
                .nextHearingFirstAvailableDateAfterDate(LocalDate.now())
                .nextHearingFirstAvailableDateAfterPeriod(AdjournCaseNextHearingPeriod.NINETY_DAYS)
                .reasons(List.of(new CollectionItem<>(null, "")))
                .additionalDirections(List.of(new CollectionItem<>(null, "")))
                .adjournmentInProgress(YES)
                .build())
            .build();
    }

    @Test
    void givenCaseHasAdjournedFieldsPopulatedAndNoDraftAdjournedDocs_thenClearTransientFields() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(ADJOURNMENT_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));

        sscsCaseData.setSscsDocument(documentList);

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getAdjournment()).hasAllNullFieldsOrProperties();
    }

    @Test
    void givenCaseHasAdjournedFieldsPopulatedAndDraftAdjournedDocs_thenDoNotClearTransientFields() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

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
        assertThat(adjournment.getInterpreterLanguage()).isEqualTo("spanish");
        assertThat(adjournment.getNextHearingDateType()).isEqualTo(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER);
        assertThat(adjournment.getNextHearingDateOrPeriod()).isEqualTo(AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD);
        assertThat(adjournment.getNextHearingDateOrTime()).isEmpty();
        assertThat(adjournment.getNextHearingFirstAvailableDateAfterDate()).isEqualTo(LocalDate.now());
        assertThat(adjournment.getNextHearingFirstAvailableDateAfterPeriod()).isEqualTo(AdjournCaseNextHearingPeriod.NINETY_DAYS);
        assertThat(adjournment.getReasons()).isEqualTo(List.of(new CollectionItem<>(null, "")));
        assertThat(adjournment.getAdditionalDirections()).isEqualTo(List.of(new CollectionItem<>(null, "")));
        assertThat(adjournment.getAdjournmentInProgress()).isEqualTo(YES);
    }

    @Test
    void givenANonAdjournCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    void givenANonAboutToStartCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThatThrownBy(() -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class);
    }

}
