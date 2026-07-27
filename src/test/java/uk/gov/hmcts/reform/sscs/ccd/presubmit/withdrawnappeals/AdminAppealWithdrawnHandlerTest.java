package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import static java.time.LocalDateTime.now;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.WITHDRAWAL_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.WITHDRAWAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import junitparams.converters.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.model.PoDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;

@ExtendWith(MockitoExtension.class)
public class AdminAppealWithdrawnHandlerTest extends AdminAppealWithdrawnBase {

    private static final String USER_AUTHORISATION = "Bearer token";
    public static final String ADMIN_APPEAL_WITHDRAWN_CALLBACK_JSON = "adminAppealWithdrawnCallback.json";

    @Mock
    private ListAssistHearingMessageHelper hearingMessageHelper;
    @Mock
    private AddNoteService addNoteService;

    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;

    private AdminAppealWithdrawnHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new AdminAppealWithdrawnHandler(hearingMessageHelper, addNoteService, false);
    }

    @ParameterizedTest
    @CsvSource({
        "ABOUT_TO_SUBMIT,ADMIN_APPEAL_WITHDRAWN,true",
        "ABOUT_TO_START,ADMIN_APPEAL_WITHDRAWN,false",
        "SUBMITTED,ADMIN_APPEAL_WITHDRAWN,false",
        "MID_EVENT,ADMIN_APPEAL_WITHDRAWN,false",
        "ABOUT_TO_SUBMIT,ISSUE_FURTHER_EVIDENCE,false",
        "null,ADMIN_APPEAL_WITHDRAWN,false",
        "ABOUT_TO_SUBMIT,null,false",
    })
    public void canHandle(@Nullable String callbackTypeStr, @Nullable String eventTypeStr, boolean expectedResult)
        throws IOException {
        CallbackType callbackType = callbackTypeStr.equals("null") ? null : CallbackType.valueOf(callbackTypeStr);
        EventType eventType = eventTypeStr.equals("null") ? null : EventType.valueOf(eventTypeStr);

        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenEvent(eventType,
            "adminAppealWithdrawnCallback.json"));

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handleDoesNotAddNewDocumentToSscsDocuments() throws IOException {
        var actualResult = handler.handle(
                ABOUT_TO_SUBMIT,
                buildTestCallbackGivenEvent(ADMIN_APPEAL_WITHDRAWN, ADMIN_APPEAL_WITHDRAWN_CALLBACK_JSON),
                USER_AUTHORISATION
        );
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
                "sscsDocument[0].id",
                "poAttendanceConfirmed")
            .isEqualTo(expectedCaseData);
        verifyNoInteractions(hearingMessageHelper);
    }

    @ParameterizedTest
    @CsvSource({
        "ABOUT_TO_START,ADMIN_APPEAL_WITHDRAWN",
        "ABOUT_TO_SUBMIT,null",
        "null,ADMIN_APPEAL_WITHDRAWN"
    })
    public void handleCornerCaseScenarios(@Nullable String callbackTypeStr, @Nullable String eventTypeStr) {
        CallbackType callbackType = callbackTypeStr.equals("null") ? null : CallbackType.valueOf(callbackTypeStr);
        EventType eventType = eventTypeStr.equals("null") ? null : EventType.valueOf(eventTypeStr);
        assertThrows(IllegalStateException.class, () -> handler.handle(
                callbackType,
                buildTestCallbackGivenEvent(eventType, ADMIN_APPEAL_WITHDRAWN_CALLBACK_JSON),
                USER_AUTHORISATION
        ));
    }

    @Test
    public void sendCancellationForAdminAppeal_eligibleForCancelHearings() {
        handler = new AdminAppealWithdrawnHandler(hearingMessageHelper, addNoteService, true);

        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .state(READY_TO_LIST)
                .schedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(HearingRoute.LIST_ASSIST)
                        .build())
                .build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), ADMIN_APPEAL_WITHDRAWN, false);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(hearingMessageHelper)
                .sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()), eq(CancellationReason.WITHDRAWN));
    }

    @Test
    public void movesWithdrawalDocumentToSscsDocumentsCollection() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualResult = handler.handle(
                ABOUT_TO_SUBMIT, buildTestCallbackGivenEvent(ADMIN_APPEAL_WITHDRAWN,
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
                .state(READY_TO_LIST)
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                        .hearingRoute(HearingRoute.LIST_ASSIST)
                        .build())
                .poAttendanceConfirmed(YES)
                .presentingOfficersDetails(PoDetails.builder().name(Name.builder().build()).build())
                .presentingOfficersHearingLink("link")
                .build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), ADMIN_APPEAL_WITHDRAWN, false);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getPoAttendanceConfirmed()).isEqualTo(NO);
        assertThat(sscsCaseData.getPresentingOfficersDetails()).isEqualTo(PoDetails.builder().build());
        assertThat(sscsCaseData.getPresentingOfficersHearingLink()).isNull();
    }
}
