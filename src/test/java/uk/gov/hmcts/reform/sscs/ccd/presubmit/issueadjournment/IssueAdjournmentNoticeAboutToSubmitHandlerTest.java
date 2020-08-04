package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.NOT_LISTABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@RunWith(JUnitParamsRunner.class)
public class IssueAdjournmentNoticeAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueAdjournmentNoticeAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    private SscsCaseData sscsCaseData;

    private SscsDocument document;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new IssueAdjournmentNoticeAboutToSubmitHandler(footerService);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_ADJOURNMENT);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DRAFT_ADJOURNMENT_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .sscsDocument(documentList)
            .adjournCaseGenerateNotice("")
            .adjournCaseTypeOfHearing("")
            .adjournCaseCanCaseBeListedRightAway("")
            .adjournCaseAreDirectionsBeingMadeToParties("")
            .adjournCaseDirectionsDueDateDaysOffset("")
            .adjournCaseDirectionsDueDate("")
            .adjournCaseTypeOfNextHearing("")
            .adjournCaseNextHearingVenue("")
            .adjournCaseNextHearingVenueSelected("")
            .adjournCasePanelMembersExcluded("")
            .adjournCaseDisabilityQualifiedPanelMemberName("")
            .adjournCaseMedicallyQualifiedPanelMemberName("")
            .adjournCaseOtherPanelMemberName("")
            .adjournCaseNextHearingListingDurationType("")
            .adjournCaseNextHearingListingDuration("")
            .adjournCaseNextHearingListingDurationUnits("")
            .adjournCaseInterpreterRequired("")
            .adjournCaseInterpreterLanguage("")
            .adjournCaseNextHearingDateType("")
            .adjournCaseNextHearingDateOrPeriod("")
            .adjournCaseNextHearingDateOrTime("")
            .adjournCaseNextHearingFirstAvailableDateAfterDate("")
            .adjournCaseNextHearingFirstAvailableDateAfterPeriod("")
            .adjournCaseNextHearingSpecificDate("")
            .adjournCaseNextHearingSpecificTime("")
            .adjournCaseReasons(Arrays.asList(new CollectionItem(null, "")))
            .adjournCaseAnythingElse("")
        .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonIssueAdjournmentEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnIssueAdjournmentEvent_thenCreatAdjournmentWithFooterAndSetStatesAndClearTransientFields() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setAdjournCaseDirectionsDueDate(LocalDate.now().toString());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(ADJOURNMENT_NOTICE), any(), eq(null), eq(null));

        assertEquals(DwpState.ADJOURNMENT_NOTICE_ISSUED.getId(), sscsCaseData.getDwpState());
        assertEquals(LocalDate.now().toString(), sscsCaseData.getDirectionDueDate());
        assertEquals(0, (int) sscsCaseData.getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue())).count());

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
        assertNull(sscsCaseData.getAdjournCaseNextHearingSpecificDate());
        assertNull(sscsCaseData.getAdjournCaseNextHearingSpecificTime());
        assertNull(sscsCaseData.getAdjournCaseReasons());
        assertNull(sscsCaseData.getAdjournCaseAnythingElse());
    }

    @Test
    public void givenAnIssueAdjournmentEventWithDueDate_thenCreateAdjournmentWithGivenDueDate() {
        callback.getCaseDetails().getCaseData().setAdjournCaseDirectionsDueDate(LocalDate.now().toString());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(LocalDate.now().toString(), sscsCaseData.getDirectionDueDate());
    }

    @Test
    public void givenAnIssueAdjournmentEventWithDueDateDaysOffset_thenCreateAdjournmentWithGivenDueDateOffset() {
        callback.getCaseDetails().getCaseData().setAdjournCaseDirectionsDueDateDaysOffset("7");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(LocalDate.now().plusDays(7).toString(), sscsCaseData.getDirectionDueDate());
    }

    @Test
    public void givenAnIssueAdjournmentEventWithDirectionsToAllParties_thenSetStateToNotListable() {
        callback.getCaseDetails().getCaseData().setAdjournCaseAreDirectionsBeingMadeToParties("Yes");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(NOT_LISTABLE, sscsCaseData.getState());
    }

    @Test
    public void givenAnIssueAdjournmentEventWithNoDirections_thenSetStateToReadyToList() {
        callback.getCaseDetails().getCaseData().setAdjournCaseAreDirectionsBeingMadeToParties("No");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(READY_TO_LIST, sscsCaseData.getState());
    }

    @Test
    public void givenAnIssueAdjournmentEventAndNoDraftAdjournmentOnCase_thenDisplayAnError() {
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no Draft Adjournment Notice on the case so adjournment cannot be issued", error);
    }

    @Test
    public void givenANonPdfDecisionNotice_thenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.setAdjournCasePreviewDocument(docLink);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You need to upload PDF documents only", error);
        assertEquals(1, (int) sscsCaseData.getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue())).count());
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
