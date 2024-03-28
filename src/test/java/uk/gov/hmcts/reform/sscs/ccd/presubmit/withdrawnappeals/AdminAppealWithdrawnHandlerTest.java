package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.WITHDRAWAL_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.WITHDRAWAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.model.PoDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;

@RunWith(JUnitParamsRunner.class)
public class AdminAppealWithdrawnHandlerTest extends AdminAppealWithdrawnBase {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private ListAssistHearingMessageHelper hearingMessageHelper;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;


    private static final String USER_AUTHORISATION = "Bearer token";
    public static final String ADMIN_APPEAL_WITHDRAWN_CALLBACK_JSON = "adminAppealWithdrawnCallback.json";
    private AdminAppealWithdrawnHandler handler = new AdminAppealWithdrawnHandler(hearingMessageHelper, false);

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,ADMIN_APPEAL_WITHDRAWN,true",
        "ABOUT_TO_START,ADMIN_APPEAL_WITHDRAWN,false",
        "SUBMITTED,ADMIN_APPEAL_WITHDRAWN,false",
        "MID_EVENT,ADMIN_APPEAL_WITHDRAWN,false",
        "ABOUT_TO_SUBMIT,ISSUE_FURTHER_EVIDENCE,false",
        "null,ADMIN_APPEAL_WITHDRAWN,false",
        "ABOUT_TO_SUBMIT,null,false",
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, boolean expectedResult)
        throws IOException {
        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenEvent(eventType,
            "adminAppealWithdrawnCallback.json"));
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handleDoesNotAddNewDocumentToSscsDocuments() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualResult = handler.handle(
            CallbackType.ABOUT_TO_SUBMIT, buildTestCallbackGivenEvent(ADMIN_APPEAL_WITHDRAWN,
                ADMIN_APPEAL_WITHDRAWN_CALLBACK_JSON), USER_AUTHORISATION);

        String expectedCaseData = fetchData("callback/withdrawnappeals/adminAppealWithdrawnExpectedCaseData.json");
        assertEquals(WITHDRAWAL_RECEIVED, actualResult.getData().getDwpState());
        assertEquals(1, actualResult.getData().getSscsDocument().size());
        assertThatJson(actualResult.getData())
            .whenIgnoringPaths(
                "jointPartyId",
                "appeal.appellant.appointee.id",
                "appeal.appellant.id",
                "appeal.rep.id",
                "appeal.hearingOptions",
                "correction",
                "correctionBodyContent",
                "bodyContent",
                "correctionGenerateNotice",
                "generateNotice",
                "dateAdded",
                "directionNoticeContent",
                "libertyToApply",
                "libertyToApplyBodyContent",
                "libertyToApplyGenerateNotice",
                "permissionToAppeal",
                "postHearingRequestType",
                "postHearingReviewType",
                "previewDocument",
                "setAside",
                "signedBy",
                "signedRole",
                "statementOfReasons",
                "statementOfReasonsBodyContent",
                "statementOfReasonsGenerateNotice",
                "poAttendanceConfirmed")
            .isEqualTo(expectedCaseData);
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_START,ADMIN_APPEAL_WITHDRAWN",
        "ABOUT_TO_SUBMIT,null",
        "null,ADMIN_APPEAL_WITHDRAWN"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType)
        throws IOException {
        handler.handle(callbackType, buildTestCallbackGivenEvent(eventType, ADMIN_APPEAL_WITHDRAWN_CALLBACK_JSON), USER_AUTHORISATION);
    }

    @Test
    public void sendCancellationForAdminAppeal_eligibleForCancelHearings() {
        handler = new AdminAppealWithdrawnHandler(hearingMessageHelper, true);

        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .state(State.READY_TO_LIST)
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                        .hearingRoute(HearingRoute.LIST_ASSIST)
                        .build())
                .build();

        when(callback.getEvent()).thenReturn(ADMIN_APPEAL_WITHDRAWN);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetailsBefore.getState()).thenReturn(State.READY_TO_LIST);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()), eq(CancellationReason.WITHDRAWN));
    }

    @Test
    public void movesWithdrawalDocumentToSscsDocumentsCollection() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualResult = handler.handle(
                CallbackType.ABOUT_TO_SUBMIT, buildTestCallbackGivenEvent(ADMIN_APPEAL_WITHDRAWN,
                        "adminAppealWithdrawnCallbackWithdrawalDocument.json"), USER_AUTHORISATION);

        assertEquals(WITHDRAWAL_RECEIVED, actualResult.getData().getDwpState());
        assertThatJson(actualResult.getData().getSscsDocument().size()).isEqualTo(1);
        assertEquals(LocalDate.now().toString(), actualResult.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals(WITHDRAWAL_REQUEST.getValue(), actualResult.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals("withdrawnDoc.pdf", actualResult.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenAppealWithdrawn_thenClearPoFields() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .state(State.READY_TO_LIST)
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                        .hearingRoute(HearingRoute.LIST_ASSIST)
                        .build())
                .poAttendanceConfirmed(YES)
                .presentingOfficersDetails(PoDetails.builder().name(Name.builder().build()).build())
                .presentingOfficersHearingLink("link")
                .build();

        when(callback.getEvent()).thenReturn(ADMIN_APPEAL_WITHDRAWN);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetailsBefore.getState()).thenReturn(State.READY_TO_LIST);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getPoAttendanceConfirmed()).isEqualTo(NO);
        assertThat(sscsCaseData.getPresentingOfficersDetails()).isEqualTo(PoDetails.builder().build());
        assertThat(sscsCaseData.getPresentingOfficersHearingLink()).isNull();
    }
}
