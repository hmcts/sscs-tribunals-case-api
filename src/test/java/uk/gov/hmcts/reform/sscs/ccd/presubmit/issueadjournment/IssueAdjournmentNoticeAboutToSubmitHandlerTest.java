package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
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

    protected static Validator validator = Validation
            .byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Before
    public void setUp() {
        openMocks(this);
        handler = new IssueAdjournmentNoticeAboutToSubmitHandler(footerService, validator);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_ADJOURNMENT_NOTICE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DRAFT_ADJOURNMENT_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .sscsDocument(documentList)
            .state(HEARING)
            .adjournCaseGenerateNotice("")
            .adjournCaseTypeOfHearing("")
            .adjournCaseCanCaseBeListedRightAway("")
            .adjournCaseAreDirectionsBeingMadeToParties("")
            .adjournCaseDirectionsDueDateDaysOffset("")
            .adjournCaseDirectionsDueDate("")
            .adjournCaseTypeOfNextHearing("")
            .adjournCaseNextHearingVenue("")
            .adjournCaseNextHearingVenueSelected(new DynamicList(new DynamicListItem("",""), Arrays.asList(new DynamicListItem("", ""))))
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
            .adjournCaseReasons(Arrays.asList(new CollectionItem(null, "")))
            .adjournCaseAdditionalDirections(Arrays.asList(new CollectionItem(null, "")))
        .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonIssueAdjournmentEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnIssueAdjournmentEvent_thenCreateAdjournmentWithFooterAndSetStatesAndClearDraftDoc() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setAdjournCaseDirectionsDueDate(LocalDate.now().plusDays(1).toString());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(ADJOURNMENT_NOTICE), any(), eq(null), eq(null), eq(null));


        assertEquals(DwpState.ADJOURNMENT_NOTICE_ISSUED.getId(), sscsCaseData.getDwpState());
        assertEquals(LocalDate.now().plusDays(1).toString(), sscsCaseData.getDirectionDueDate());
        assertEquals(0, (int) sscsCaseData.getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue())).count());
    }

    @Test
    public void givenAnIssueAdjournmentEvent_thenCreateAdjournmentWithFooterAndTranslationRequired() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setAdjournCaseDirectionsDueDate(LocalDate.now().plusDays(1).toString());
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("yes");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(ADJOURNMENT_NOTICE), any(), eq(null), eq(null), eq(TRANSLATION_REQUIRED));


        assertEquals(null, sscsCaseData.getDwpState());
        assertEquals(InterlocReviewState.WELSH_TRANSLATION.getId(), sscsCaseData.getInterlocReviewState());
        assertEquals("Yes", sscsCaseData.getTranslationWorkOutstanding());
    }

    @Test
    public void givenAnIssueAdjournmentEventWithDueDate_thenCreateAdjournmentWithGivenDueDate() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setAdjournCaseDirectionsDueDate(LocalDate.now().plusDays(1).toString());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(LocalDate.now().plusDays(1).toString(), sscsCaseData.getDirectionDueDate());
    }

    @Test
    public void givenAnIssueAdjournmentEventWithDueDateDaysOffset_thenCreateAdjournmentWithGivenDueDateOffset() {
        callback.getCaseDetails().getCaseData().setAdjournCaseDirectionsDueDateDaysOffset("7");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(LocalDate.now().plusDays(7).toString(), sscsCaseData.getDirectionDueDate());
    }

    @Test
    public void givenAnIssueAdjournmentEventWithDirectionsToAllParties_thenSetStateToNotListable() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setAdjournCaseAreDirectionsBeingMadeToParties("Yes");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(NOT_LISTABLE, sscsCaseData.getState());
    }

    @Test
    public void givenAnIssueAdjournmentEventWithNoDirections_thenSetStateToReadyToList() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setAdjournCaseAreDirectionsBeingMadeToParties("No");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(READY_TO_LIST, sscsCaseData.getState());
    }

    @Test
    public void givenAnIssueAdjournmentEventForWelshCase0_thenTheCaseStateShoyleStayUnchanged() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("yes");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(HEARING, sscsCaseData.getState());
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
