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
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingVenue;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCasePanelMembersExcluded;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTime;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
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
                .nextHearingVenueSelected(new DynamicList("testListItem"))
                .panelMembersExcluded(AdjournCasePanelMembersExcluded.NO)
                .disabilityQualifiedPanelMemberName(new DynamicList(""))
                .medicallyQualifiedPanelMemberName(new DynamicList(""))
                .otherPanelMemberName(new DynamicList(""))
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
                .time(AdjournCaseTime.builder().build())
                .reasons(List.of(new CollectionItem<>(null, "")))
                .additionalDirections(List.of(new CollectionItem<>(null, "")))
                .previewDocument(DocumentLink.builder().build())
                .generatedDate(LocalDate.now())
                .adjournmentInProgress(YES)
                .build())
            .build();
    }

    @Test
    void givenCaseHasAdjournedFieldsPopulatedAndNoDraftAdjournedDocs_thenClearTransientFields_andSetAdjournmentInProgressToNoIfFeatureFlagEnabled() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true); // TODO SSCS-10951

        handleAboutToStartWithDocumentType(ADJOURNMENT_NOTICE);

        assertThat(sscsCaseData.getAdjournment()).hasAllNullFieldsOrPropertiesExcept("adjournmentInProgress");
        assertThat(sscsCaseData.getAdjournment().getAdjournmentInProgress()).isEqualTo(NO);
    }

    @Test
    void givenCaseHasAdjournedFieldsPopulatedAndNoDraftAdjournedDocs_thenClearTransientFields() { // TODO SSCS-10951
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", false);

        handleAboutToStartWithDocumentType(ADJOURNMENT_NOTICE);

        assertThat(sscsCaseData.getAdjournment()).hasAllNullFieldsOrProperties();
    }

    @Test
    void givenCaseHasAdjournedFieldsPopulatedAndDraftAdjournedDocs_thenDoNotClearTransientFields() {
        handleAboutToStartWithDocumentType(DRAFT_ADJOURNMENT_NOTICE);

        assertThat(sscsCaseData.getAdjournment()).hasNoNullFieldsOrProperties();
    }

    private void handleAboutToStartWithDocumentType(DocumentType documentType) {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(documentType.getValue()).build();
        documentList.add(new SscsDocument(details));

        sscsCaseData.setSscsDocument(documentList);

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
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
