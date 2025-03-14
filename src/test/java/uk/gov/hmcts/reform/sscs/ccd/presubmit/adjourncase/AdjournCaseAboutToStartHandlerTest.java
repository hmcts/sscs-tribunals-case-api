package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.HmcHearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;

@ExtendWith(MockitoExtension.class)
class AdjournCaseAboutToStartHandlerTest {

    @Mock
    private DynamicListLanguageUtil dynamicListLanguageUtil;

    private static final String USER_AUTHORISATION = "Bearer token";
    private AdjournCaseAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {
        handler = new AdjournCaseAboutToStartHandler(dynamicListLanguageUtil);

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
                .disabilityQualifiedPanelMemberName("")
                .medicallyQualifiedPanelMemberName("")
                .otherPanelMemberName("")
                .panelMember1(JudicialUserBase.builder().build())
                .panelMember2(JudicialUserBase.builder().build())
                .panelMember3(JudicialUserBase.builder().build())
                .nextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD)
                .nextHearingListingDuration(1)
                .nextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS)
                .interpreterRequired(NO)
                .interpreterLanguage(new DynamicList("Spanish"))
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
    void givenCaseHasAdjournedFieldsPopulated_andNoDraft_thenClearTransientFields() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        List<SscsDocument> documentList = new ArrayList<>();
        sscsCaseData.setSscsDocument(documentList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getData().getAdjournment()).hasAllNullFieldsOrProperties();

    }

    @Test
    void givenCaseHasAdjournedFieldsPopulated_andDraftDocument_thenDoNotClearTransientFields() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        List<SscsDocument> documentList = new ArrayList<>();
        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DocumentType.DRAFT_ADJOURNMENT_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));
        sscsCaseData.setSscsDocument(documentList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertTrue(response.getData().getAdjournment().getAdjournmentInProgress().toBoolean());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    void givenANonAdjournCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    void givenANonAboutToStartCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    void givenANullCaseDetails_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(null);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    void givenANullCaseData_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(null);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThatThrownBy(() -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenNullAppealNullTypeOfHearing_thenDoNotError() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setAppeal(null);
        sscsCaseData.setHmcHearingType(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
    }

    @Test
    void givenNullHearingOptionsNullTypeOfHearing_thenDoNotError() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.getAppeal().setHearingOptions(null);
        sscsCaseData.setHmcHearingType(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
    }

    @Test
    void givenSubstantiveHearingSetInCase_thenDoNotError() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setHmcHearingType(HmcHearingType.SUBSTANTIVE);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
    }

    @Test
    void givenSubstantiveHearingSetInOverride_thenDoNotError() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setHmcHearingType(HmcHearingType.DIRECTION_HEARINGS);
        sscsCaseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder()
            .overrideFields(OverrideFields.builder()
                .hmcHearingType(HmcHearingType.SUBSTANTIVE)
                .build())
            .build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
    }

    @Test
    void givenDirectionHearingSetInCase_thenError() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setHmcHearingType(HmcHearingType.DIRECTION_HEARINGS);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getErrors())
            .hasSize(1)
            .contains("In order to run this event the hearing type must be substantive, please update the hearing type to proceed");
    }

    @Test
    void givenDirectionHearingSetInOverride_thenError() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setHmcHearingType(HmcHearingType.SUBSTANTIVE);
        sscsCaseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder()
            .overrideFields(OverrideFields.builder()
                .hmcHearingType(HmcHearingType.DIRECTION_HEARINGS)
                .build())
            .build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getErrors())
            .hasSize(1)
            .contains("In order to run this event the hearing type must be substantive, please update the hearing type to proceed");
    }

}
