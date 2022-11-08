package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.NOT_LISTABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@ExtendWith(MockitoExtension.class)
class IssueAdjournmentNoticeAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueAdjournmentNoticeAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    private SscsCaseData sscsCaseData;

    protected static Validator validator = Validation
            .byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @BeforeEach
    void setUp() {
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
            .adjournCaseNextHearingVenueSelected(new DynamicList(
                new DynamicListItem("",""), List.of(new DynamicListItem("", ""))))
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
            .adjournCaseReasons(List.of(new CollectionItem<>(null, "")))
            .adjournCaseAdditionalDirections(List.of(new CollectionItem<>(null, "")))
            .isAdjournmentInProgress(YES)
        .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenANonIssueAdjournmentEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAnIssueAdjournmentEvent_thenCreateAdjournmentWithFooterAndSetStatesAndClearDraftDoc() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setAdjournCasePreviewDocument(docLink);
        sscsCaseData.setAdjournCaseDirectionsDueDate(LocalDate.now().plusDays(1).toString());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(ADJOURNMENT_NOTICE), any(), eq(null), eq(null), eq(null));


        assertThat(this.sscsCaseData.getDwpState()).isEqualTo(DwpState.ADJOURNMENT_NOTICE_ISSUED.getId());
        assertThat(this.sscsCaseData.getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(1).toString());
        assertThat(this.sscsCaseData.getSscsDocument().stream()
            .filter(f -> f.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue()))).isEmpty();
        verifyTemporaryAdjournCaseFieldsAreCleared(sscsCaseData);
    }

    private void verifyTemporaryAdjournCaseFieldsAreCleared(SscsCaseData sscsCaseData) {
        assertThat(sscsCaseData.getAdjournCaseDirectionsDueDate()).isNull();
        assertThat(sscsCaseData.getAdjournCaseGenerateNotice()).isNull();
        assertThat(sscsCaseData.getAdjournCaseTypeOfHearing()).isNull();
        assertThat(sscsCaseData.getAdjournCaseCanCaseBeListedRightAway()).isNull();
        assertThat(sscsCaseData.getAdjournCaseAreDirectionsBeingMadeToParties()).isNull();
        assertThat(sscsCaseData.getAdjournCaseDirectionsDueDateDaysOffset()).isNull();
        assertThat(sscsCaseData.getAdjournCaseDirectionsDueDate()).isNull();
        assertThat(sscsCaseData.getAdjournCaseTypeOfNextHearing()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingVenue()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingVenueSelected()).isNull();
        assertThat(sscsCaseData.getAdjournCasePanelMembersExcluded()).isNull();
        assertThat(sscsCaseData.getAdjournCaseDisabilityQualifiedPanelMemberName()).isNull();
        assertThat(sscsCaseData.getAdjournCaseMedicallyQualifiedPanelMemberName()).isNull();
        assertThat(sscsCaseData.getAdjournCaseOtherPanelMemberName()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingListingDurationType()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingListingDuration()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingListingDurationUnits()).isNull();
        assertThat(sscsCaseData.getAdjournCaseInterpreterRequired()).isNull();
        assertThat(sscsCaseData.getAdjournCaseInterpreterLanguage()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingDateType()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingDateOrPeriod()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingDateOrTime()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate()).isNull();
        assertThat(sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterPeriod()).isNull();
        assertThat(sscsCaseData.getAdjournCaseTime()).isNull();
        assertThat(sscsCaseData.getAdjournCaseReasons()).isNull();
        assertThat(sscsCaseData.getAdjournCaseAdditionalDirections()).isNull();
        assertThat(sscsCaseData.getIsAdjournmentInProgress()).isNull();
    }

    @Test
    void givenAnIssueAdjournmentEvent_thenCreateAdjournmentWithFooterAndTranslationRequired() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setAdjournCaseDirectionsDueDate(LocalDate.now().plusDays(1).toString());
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("yes");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(ADJOURNMENT_NOTICE), any(), eq(null), eq(null), eq(TRANSLATION_REQUIRED));


        assertThat(sscsCaseData.getDwpState()).isNull();
        assertThat(sscsCaseData.getInterlocReviewState()).isEqualTo(InterlocReviewState.WELSH_TRANSLATION.getId());
        assertThat(sscsCaseData.getTranslationWorkOutstanding()).isEqualTo("Yes");
    }

    @Test
    void givenAnIssueAdjournmentEventWithDueDate_thenCreateAdjournmentWithGivenDueDate() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setAdjournCaseDirectionsDueDate(LocalDate.now().plusDays(1).toString());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(1).toString());
    }

    @Test
    void givenAnIssueAdjournmentEventWithDueDateDaysOffset_thenCreateAdjournmentWithGivenDueDateOffset() {
        callback.getCaseDetails().getCaseData().setAdjournCaseDirectionsDueDateDaysOffset("7");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getDirectionDueDate()).isEqualTo(LocalDate.now().plusDays(7).toString());
    }

    @Test
    void givenAnIssueAdjournmentEventWithDirectionsToAllParties_thenSetStateToNotListable() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setAdjournCaseAreDirectionsBeingMadeToParties("Yes");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getState()).isEqualTo(NOT_LISTABLE);
    }

    @Test
    void givenAnIssueAdjournmentEventWithNoDirections_thenSetStateToReadyToList() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setAdjournCaseAreDirectionsBeingMadeToParties("No");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getState()).isEqualTo(READY_TO_LIST);
    }

    @Test
    void givenAnIssueAdjournmentEventForWelshCase0_thenTheCaseStateShoyleStayUnchanged() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("yes");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getState()).isEqualTo(HEARING);
    }

    @Test
    void givenAnIssueAdjournmentEventAndNoDraftAdjournmentOnCase_thenDisplayAnError() {
        callback.getCaseDetails().getCaseData().setAdjournCasePreviewDocument(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("There is no Draft Adjournment Notice on the case so adjournment cannot be issued");
    }

    @Test
    void givenANonPdfDecisionNotice_thenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.setAdjournCasePreviewDocument(docLink);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("You need to upload PDF documents only");
        assertThat(sscsCaseData.getSscsDocument().stream()
            .filter(f -> f.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue()))).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class);
    }
}
