package uk.gov.hmcts.reform.sscs.ccd.presubmit.managerelatedcases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.AssociatedCaseLinkHelper;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@ExtendWith(MockitoExtension.class)
class ManageRelatedCasesAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsA;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBeforeA;

    @Mock
    private CcdService ccdService;

    @Mock
    private AssociatedCaseLinkHelper associatedCaseLinkHelper;

    @Mock
    private IdamService idamService;

    private ManageRelatedCasesAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseDataA;
    private SscsCaseData sscsCaseDataBeforeA;
    private SscsCaseData sscsCaseDataB;
    private SscsCaseData sscsCaseDataC;
    private SscsCaseDetails sscsCaseDetailsB;
    private SscsCaseDetails sscsCaseDetailsC;

    @BeforeEach
    void setUp() {
        handler = new ManageRelatedCasesAboutToSubmitHandler(ccdService, associatedCaseLinkHelper, idamService);

        sscsCaseDataA = SscsCaseData.builder().ccdCaseId("1").appeal(Appeal.builder().build()).build();
        sscsCaseDataBeforeA = SscsCaseData.builder().ccdCaseId("1").appeal(Appeal.builder().build()).build();
        sscsCaseDataB = SscsCaseData.builder().ccdCaseId("2").appeal(Appeal.builder().build()).build();
        sscsCaseDataC = SscsCaseData.builder().ccdCaseId("3").appeal(Appeal.builder().build()).build();
        final SscsCaseDetails sscsCaseDetailsA = SscsCaseDetails.builder().id(1L).data(sscsCaseDataA).build();
        sscsCaseDetailsB = SscsCaseDetails.builder().id(2L).data(sscsCaseDataB).build();
        sscsCaseDetailsC = SscsCaseDetails.builder().id(3L).data(sscsCaseDataC).build();

        lenient().when(callback.getEvent()).thenReturn(EventType.ASSOCIATE_CASE);
        lenient().when(callback.getCaseDetails()).thenReturn(caseDetailsA);
        lenient().when(caseDetailsA.getCaseData()).thenReturn(sscsCaseDataA);
        lenient().when(caseDetailsBeforeA.getCaseData()).thenReturn(sscsCaseDataBeforeA);
        lenient().when(ccdService.getByCaseId(eq(1L), any())).thenReturn(sscsCaseDetailsA);
        lenient().when(ccdService.getByCaseId(eq(2L), any())).thenReturn(sscsCaseDetailsB);
        lenient().when(ccdService.getByCaseId(eq(3L), any())).thenReturn(sscsCaseDetailsC);
        lenient().when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    void givenAboutToSubmitAndAssociateCaseEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(final CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void givenANonAssociateCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot handle associate case about to submit callback");
    }

    @Test
    void givenCaseAWithLinkedCaseB_andCaseBHasNoLink_thenCallAssociatedCaseLinkHelper() {
        sscsCaseDataA.setAssociatedCase(caseLinks("2"));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper).addLinkToOtherAssociatedCasesV2Enabled(List.of(sscsCaseDetailsB), "1");
        assertThat(response.getData()).isSameAs(sscsCaseDataA);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenCaseAWithLinkedCaseB_andCaseBLinkedOnlyToAnotherCase_thenCallAssociatedCaseLinkHelper() {
        sscsCaseDataA.setAssociatedCase(caseLinks("2"));
        sscsCaseDataB.setAssociatedCase(caseLinks("3"));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper).addLinkToOtherAssociatedCasesV2Enabled(List.of(sscsCaseDetailsB), "1");
    }

    @Test
    void givenCaseAWithLinkedCaseB_butCaseBHasLink_thenDoNotCallAssociatedCaseLinkHelper() {
        sscsCaseDataA.setAssociatedCase(caseLinks("2"));
        sscsCaseDataB.setAssociatedCase(caseLinks("1"));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper, never()).addLinkToOtherAssociatedCasesV2Enabled(any(), any());
    }

    @Test
    void givenCaseAWithLinkedCaseThatCannotBeRetrieved_thenDoNotCallAssociatedCaseLinkHelper() {
        sscsCaseDataA.setAssociatedCase(caseLinks("4"));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper, never()).addLinkToOtherAssociatedCasesV2Enabled(any(), any());
    }

    @Test
    void givenCaseAWithNullAssociatedCases_thenDoNotRetrieveOrLinkAnyCases() {
        sscsCaseDataA.setAssociatedCase(null);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(ccdService, never()).getByCaseId(anyLong(), any());
        verify(associatedCaseLinkHelper, never()).addLinkToOtherAssociatedCasesV2Enabled(any(), any());
    }

    @Test
    void givenNoCaseDetailsBefore_thenDoNotRemoveAnyLinks() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.empty());
        sscsCaseDataA.setAssociatedCase(caseLinks("2"));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper, never()).removeLinkToOtherAssociatedCases(any(), any());
    }

    @Test
    void givenCaseBeforeHasNoAssociatedCases_thenDoNotRemoveAnyLinks() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBeforeA));
        sscsCaseDataBeforeA.setAssociatedCase(new ArrayList<>());
        sscsCaseDataA.setAssociatedCase(caseLinks("2"));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper, never()).removeLinkToOtherAssociatedCases(any(), any());
    }

    @Test
    void givenCaseBRemovedFromCaseA_thenRemoveLinkFromCaseBOnly() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBeforeA));
        sscsCaseDataBeforeA.setAssociatedCase(caseLinks("2", "3"));
        sscsCaseDataA.setAssociatedCase(caseLinks("3"));
        sscsCaseDataC.setAssociatedCase(caseLinks("1"));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper).removeLinkToOtherAssociatedCases(List.of(sscsCaseDetailsB), "1");
        assertThat(response.getData().getAssociatedCase()).isEqualTo(caseLinks("3"));
    }

    @Test
    void givenAllCasesRemovedAndAssociatedCasesNull_thenRemoveLinkFromAllPreviouslyLinkedCases() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBeforeA));
        sscsCaseDataBeforeA.setAssociatedCase(caseLinks("2", "3"));
        sscsCaseDataA.setAssociatedCase(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper).removeLinkToOtherAssociatedCases(List.of(sscsCaseDetailsB, sscsCaseDetailsC), "1");
        assertThat(response.getData().getAssociatedCase()).isNull();
    }

    @Test
    void givenRemovedCaseCannotBeRetrieved_thenItIsExcludedFromRemoval() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBeforeA));
        sscsCaseDataBeforeA.setAssociatedCase(caseLinks("2", "4"));
        sscsCaseDataA.setAssociatedCase(new ArrayList<>());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper).removeLinkToOtherAssociatedCases(List.of(sscsCaseDetailsB), "1");
        assertThat(response.getData().getAssociatedCase()).isNull();
    }

    @Test
    void givenAllCasesRemovedAndAssociatedCasesEmpty_thenAssociatedCasesSetToNull() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBeforeA));
        sscsCaseDataBeforeA.setAssociatedCase(caseLinks("2"));
        sscsCaseDataA.setAssociatedCase(new ArrayList<>());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAssociatedCase()).isNull();
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenNoCasesRemoved_thenDoNoAttemptToRemoveAssociatedCases() {
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBeforeA));
        sscsCaseDataBeforeA.setAssociatedCase(caseLinks("2"));
        sscsCaseDataA.setAssociatedCase(caseLinks("2"));
        sscsCaseDataB.setAssociatedCase(caseLinks("1"));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper, never()).removeLinkToOtherAssociatedCases(List.of(), "1");
    }

    private static List<CaseLink> caseLinks(final String... caseReferences) {
        final List<CaseLink> caseLinks = new ArrayList<>();
        for (final String caseReference : caseReferences) {
            caseLinks.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference(caseReference).build()).build());
        }
        return caseLinks;
    }
}
