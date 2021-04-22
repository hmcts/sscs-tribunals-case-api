package uk.gov.hmcts.reform.sscs.ccd.presubmit.removelinkforcase;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class RemoveLinkForCaseAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private RemoveLinkForCaseAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    private SscsCaseData sscsCaseData;
    private SscsCaseData sscsCaseDataBefore;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new RemoveLinkForCaseAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.REMOVE_LINK_FOR_CASE);

        sscsCaseDataBefore = SscsCaseData.builder()
                .ccdCaseId("1234")
                .associatedCase(buildCaseLink("1", "2", "3", "4"))
                .linkedCase(buildCaseLink("1", "2", "3", "4"))
                .appeal(Appeal.builder().build())
                .build();

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .associatedCase(buildCaseLink("1", "2", "4"))
                .linkedCase(buildCaseLink("1", "2", "4"))
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);
    }

    private List<CaseLink> buildCaseLink(String... refs) {
        return stream(refs).map(caseRef -> CaseLink.builder().value(CaseLinkDetails.builder().caseReference(caseRef).build()).build()).collect(toList());
    }

    @Test
    public void givenANonHandleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void shouldReturnAnErrorWhenThereWereNoCasesLinksAndAssociatedCases() {
        sscsCaseDataBefore.setLinkedCase(null);
        sscsCaseData.setLinkedCase(null);
        sscsCaseDataBefore.setAssociatedCase(null);
        sscsCaseData.setAssociatedCase(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("There are no case links and associated case to remove."));
    }

    @Test
    public void shouldNotReturnAnErrorWhenThereWereNoCasesLinksButRemoveAssociatedCases() {
        sscsCaseDataBefore.setLinkedCase(null);
        sscsCaseData.setLinkedCase(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getAssociatedCase().stream().map(f -> f.getValue().getCaseReference()).collect(joining()), is("124"));
    }

    @Test
    public void shouldNotReturnAnErrorWhenThereWereNoAssociatedCasesButRemoveCasesLink() {
        sscsCaseDataBefore.setAssociatedCase(null);
        sscsCaseData.setAssociatedCase(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getLinkedCase().stream().map(f -> f.getValue().getCaseReference()).collect(joining()), is("124"));
    }

    @Test
    public void shouldReturnAnErrorWhenCaseLinksAndAssociatedCaseAreUnchanged() {
        sscsCaseData.setLinkedCase(sscsCaseDataBefore.getLinkedCase());
        sscsCaseData.setAssociatedCase(sscsCaseDataBefore.getAssociatedCase());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("No case links or associated case have been selected to remove from the case."));
    }

    @Test
    public void shouldReturnAnErrorWhenCaseLinksAreAdded() {
        sscsCaseData.setLinkedCase(buildCaseLink("1", "5"));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Cannot add a case link."));
    }

    @Test
    public void shouldReturnAnErrorWhenAssociatedCasesAreAdded() {
        sscsCaseData.setAssociatedCase(buildCaseLink("1", "5"));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Cannot add a associated case."));
    }

    @Test
    public void shouldRemoveACaseLinkAndAssociatedCase() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getLinkedCase().stream().map(f -> f.getValue().getCaseReference()).collect(joining()), is("124"));
        assertThat(response.getData().getAssociatedCase().stream().map(f -> f.getValue().getCaseReference()).collect(joining()), is("124"));
    }
}
