package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.util.DynamicListLangauageUtil;

@RunWith(JUnitParamsRunner.class)
public class AdjournCaseAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @InjectMocks
    private AdjournCaseAboutToStartHandler handler;

    @Mock
    private DynamicListLangauageUtil dynamicListLangauageUtil;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);

        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .appeal(Appeal.builder().build())
                .adjournCaseGenerateNotice("")
                .adjournCaseTypeOfHearing("")
                .adjournCaseCanCaseBeListedRightAway("")
                .adjournCaseAreDirectionsBeingMadeToParties("")
                .adjournCaseDirectionsDueDateDaysOffset("")
                .adjournCaseDirectionsDueDate("")
                .adjournCaseTypeOfNextHearing("")
                .adjournCaseNextHearingVenue("")
                .adjournCaseNextHearingVenueSelected(new DynamicList(new DynamicListItem("", ""), new ArrayList<>()))
                .adjournCasePanelMembersExcluded("")
                .adjournCaseDisabilityQualifiedPanelMemberName("")
                .adjournCaseMedicallyQualifiedPanelMemberName("")
                .adjournCaseOtherPanelMemberName("")
                .adjournCaseNextHearingListingDurationType("")
                .adjournCaseNextHearingListingDuration("")
                .adjournCaseNextHearingListingDurationUnits("")
                .adjournCaseInterpreterRequired("")
                .adjournCaseInterpreterLanguage(null)
                .adjournCaseNextHearingDateType("")
                .adjournCaseNextHearingDateOrPeriod("")
                .adjournCaseNextHearingDateOrTime("")
                .adjournCaseNextHearingFirstAvailableDateAfterDate("")
                .adjournCaseNextHearingFirstAvailableDateAfterPeriod("")
                .adjournCaseReasons(Arrays.asList(new CollectionItem(null, "")))
                .adjournCaseAdditionalDirections(Arrays.asList(new CollectionItem(null, "")))
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonAdjournCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenCaseHasAdjournedFieldsPopulatedAndNoDraftAdjournedDocs_thenClearTransientFields() {

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(ADJOURNMENT_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));

        sscsCaseData.setSscsDocument(documentList);

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertNull(sscsCaseData.getAdjournCaseGenerateNotice());
        assertNull(sscsCaseData.getAdjournCaseTypeOfHearing());
        assertNull(sscsCaseData.getAdjournCaseCanCaseBeListedRightAway());
        assertNull(sscsCaseData.getAdjournCaseAreDirectionsBeingMadeToParties());
        assertNull(sscsCaseData.getAdjournCaseDirectionsDueDateDaysOffset());
        assertNull(sscsCaseData.getAdjournCaseDirectionsDueDate());
        assertNull(sscsCaseData.getAdjournCaseTypeOfNextHearing());
        assertNull(sscsCaseData.getAdjournCaseNextHearingVenue());
        assertNull(sscsCaseData.getAdjournCaseNextHearingVenueSelected());
        assertNull(sscsCaseData.getAdjournCasePanelMembersExcluded());
        assertNull(sscsCaseData.getAdjournCaseDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getAdjournCaseMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getAdjournCaseOtherPanelMemberName());
        assertNull(sscsCaseData.getAdjournCaseNextHearingListingDurationType());
        assertNull(sscsCaseData.getAdjournCaseNextHearingListingDuration());
        assertNull(sscsCaseData.getAdjournCaseNextHearingListingDurationUnits());
        assertNull(sscsCaseData.getAdjournCaseInterpreterRequired());
        assertNull(sscsCaseData.getAdjournCaseInterpreterLanguage());
        assertNull(sscsCaseData.getAdjournCaseNextHearingDateType());
        assertNull(sscsCaseData.getAdjournCaseNextHearingDateOrPeriod());
        assertNull(sscsCaseData.getAdjournCaseNextHearingDateOrTime());
        assertNull(sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate());
        assertNull(sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterPeriod());
        assertNull(sscsCaseData.getAdjournCaseReasons());
        assertNull(sscsCaseData.getAdjournCaseAdditionalDirections());
    }

    @Test
    public void givenCaseHasAdjournedFieldsPopulatedAndDraftAdjournedDocs_thenDoNotClearTransientFields() {

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DRAFT_ADJOURNMENT_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));

        sscsCaseData.setSscsDocument(documentList);

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("", sscsCaseData.getAdjournCaseGenerateNotice());
        assertEquals("", sscsCaseData.getAdjournCaseTypeOfHearing());
        assertEquals("", sscsCaseData.getAdjournCaseCanCaseBeListedRightAway());
        assertEquals("", sscsCaseData.getAdjournCaseAreDirectionsBeingMadeToParties());
        assertEquals("", sscsCaseData.getAdjournCaseDirectionsDueDateDaysOffset());
        assertEquals("", sscsCaseData.getAdjournCaseDirectionsDueDate());
        assertEquals("", sscsCaseData.getAdjournCaseTypeOfNextHearing());
        assertEquals("", sscsCaseData.getAdjournCaseNextHearingVenue());
        assertEquals("", sscsCaseData.getAdjournCaseNextHearingVenueSelected().getValue().getCode());
        assertEquals("", sscsCaseData.getAdjournCasePanelMembersExcluded());
        assertEquals("", sscsCaseData.getAdjournCaseDisabilityQualifiedPanelMemberName());
        assertEquals("", sscsCaseData.getAdjournCaseMedicallyQualifiedPanelMemberName());
        assertEquals("", sscsCaseData.getAdjournCaseOtherPanelMemberName());
        assertEquals("", sscsCaseData.getAdjournCaseNextHearingListingDurationType());
        assertEquals("", sscsCaseData.getAdjournCaseNextHearingListingDuration());
        assertEquals("", sscsCaseData.getAdjournCaseNextHearingListingDurationUnits());
        assertEquals("", sscsCaseData.getAdjournCaseInterpreterRequired());
        assertEquals(null, sscsCaseData.getAdjournCaseInterpreterLanguage());
        assertEquals("", sscsCaseData.getAdjournCaseNextHearingDateType());
        assertEquals("", sscsCaseData.getAdjournCaseNextHearingDateOrPeriod());
        assertEquals("", sscsCaseData.getAdjournCaseNextHearingDateOrTime());
        assertEquals("", sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate());
        assertEquals("", sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterPeriod());
        assertEquals(Arrays.asList(new CollectionItem(null, "")), sscsCaseData.getAdjournCaseReasons());
        assertEquals("", Arrays.asList(new CollectionItem(null, "")), sscsCaseData.getAdjournCaseAdditionalDirections());
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
