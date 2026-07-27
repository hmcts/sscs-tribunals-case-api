package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.time.LocalDateTime.now;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_CHALLENGE_VALIDITY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REINSTATE_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.WELSH_TRANSLATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;

import java.time.LocalDate;
import java.util.Optional;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;


@ExtendWith(MockitoExtension.class)
public class InterlocServiceHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    AddNoteService addNoteService;
    @Mock
    DwpDocumentService dwpDocumentService;

    private InterlocServiceHandler handler;

    private SscsCaseData sscsCaseData;

    private CaseDetails<SscsCaseData> caseDetails;
    private Callback<SscsCaseData> callback;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().directionDueDate("01/02/2020").build();
        caseDetails = new CaseDetails<>(1234L, "SSCS", WITH_DWP, sscsCaseData, now(), "Benefit");

        handler = new InterlocServiceHandler(addNoteService, dwpDocumentService);
    }

    @ParameterizedTest
    @CsvSource({
        "TCW_DIRECTION_ISSUED, ABOUT_TO_SUBMIT, true",
        "JUDGE_DIRECTION_ISSUED, ABOUT_TO_SUBMIT, true",
        "TCW_REFER_TO_JUDGE, ABOUT_TO_SUBMIT, true",
        "NON_COMPLIANT, ABOUT_TO_SUBMIT, true",
        "NON_COMPLIANT_SEND_TO_INTERLOC, ABOUT_TO_SUBMIT, true",
        "REINSTATE_APPEAL, ABOUT_TO_SUBMIT, true",
        "TCW_DECISION_APPEAL_TO_PROCEED, ABOUT_TO_SUBMIT, true",
        "JUDGE_DECISION_APPEAL_TO_PROCEED, ABOUT_TO_SUBMIT, true",
        "SEND_TO_ADMIN, ABOUT_TO_SUBMIT, true",
        "DWP_CHALLENGE_VALIDITY, ABOUT_TO_SUBMIT, true",
        "DWP_CHALLENGE_VALIDITY, ABOUT_TO_START, false",
        "DWP_CHALLENGE_VALIDITY, SUBMITTED, false",
        "DWP_CHALLENGE_VALIDITY, MID_EVENT, false",
    })
    public void givenEvent_thenCanHandle(EventType eventType, CallbackType callbackType, boolean expected) {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, false);

        assertEquals(expected, handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenNullCallback_shouldThrowException() {
        assertThrows(NullPointerException.class, () -> handler.canHandle(null, callback));
    }

    @ParameterizedTest
    @CsvSource({
        "TCW_DIRECTION_ISSUED, AWAITING_INFORMATION",
        "JUDGE_DIRECTION_ISSUED, AWAITING_INFORMATION",
        "TCW_DECISION_APPEAL_TO_PROCEED, NONE",
        "JUDGE_DECISION_APPEAL_TO_PROCEED, NONE",
        "SEND_TO_ADMIN, AWAITING_ADMIN_ACTION"
    })
    public void givenEvent_thenSetInterlocReviewStateToExpected(EventType eventType,
                                                                InterlocReviewState expectedInterlocReviewState) {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(expectedInterlocReviewState));

    }

    @ParameterizedTest
    @CsvSource({
        "TCW_REFER_TO_JUDGE, REVIEW_BY_JUDGE, 0",
        "DWP_CHALLENGE_VALIDITY, REVIEW_BY_TCW, 1",
        "DWP_REQUEST_TIME_EXTENSION, REVIEW_BY_TCW, 0"
    })
    public void givenEvent_thenSetInterlocReviewStateAndSetInterlocReferralDateToExpectedAndDoNotClearDirectionDueDate(
            EventType eventType, InterlocReviewState expectedInterlocReviewState, int verifyCount
    ) {
        var challengeValidityDoc = DwpResponseDocument.builder().build();
        sscsCaseData.setDwpChallengeValidityDocument(challengeValidityDoc);
        sscsCaseData.setTempNoteDetail("test note");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(expectedInterlocReviewState));
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now()));
        assertThat(response.getData().getDirectionDueDate(), is("01/02/2020"));
        var expectedDoc = verifyCount == 0 ? challengeValidityDoc : null;
        assertEquals(expectedDoc, response.getData().getDwpChallengeValidityDocument());
        assertNotEquals("reinstated", response.getData().getOutcome());
        verify(dwpDocumentService, times(verifyCount))
                .addToDwpDocuments(eq(sscsCaseData), eq(challengeValidityDoc), eq(DWP_CHALLENGE_VALIDITY));
        verify(addNoteService).addNote(eq(USER_AUTHORISATION), eq(sscsCaseData), eq("test note"));
    }

    @ParameterizedTest
    @CsvSource({
        "NON_COMPLIANT, REVIEW_BY_TCW",
        "NON_COMPLIANT_SEND_TO_INTERLOC, REVIEW_BY_TCW",
        "REINSTATE_APPEAL, AWAITING_ADMIN_ACTION"
    })
    public void givenEvent_thenSetInterlocReviewStateAndSetInterlocReferralDateToExpectedAndClearDirectionDueDate(
            EventType eventType,
            InterlocReviewState expectedInterlocReviewState) {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, false);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(expectedInterlocReviewState));
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now()));
        assertNull(response.getData().getDirectionDueDate());
        var expectedOutcome = REINSTATE_APPEAL.equals(eventType) ? "reinstated" : null;
        assertEquals(expectedOutcome, response.getData().getOutcome());
        verifyNoInteractions(dwpDocumentService);
        verify(addNoteService).addNote(eq(USER_AUTHORISATION), eq(sscsCaseData), eq(null));
    }

    @ParameterizedTest
    @CsvSource({
        "NON_COMPLIANT, REVIEW_BY_TCW",
        "NON_COMPLIANT_SEND_TO_INTERLOC, REVIEW_BY_TCW",
        "REINSTATE_APPEAL, AWAITING_ADMIN_ACTION"
    })
    public void givenWelshCaseEvent_thenSetInterlocReviewStateAndSetInterlocReferralDateAndClearDirectionDueDate(
            EventType eventType, InterlocReviewState expectedInterlocReviewState) {

        sscsCaseData.setLanguagePreferenceWelsh("Yes");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(WELSH_TRANSLATION));
        assertThat(response.getData().getWelshInterlocNextReviewState(),
                is(expectedInterlocReviewState.getCcdDefinition()));
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now()));
        assertNull(response.getData().getDirectionDueDate());
    }

    @Test
    public void throwExceptionIfCannotHandleEventType() {
        sscsCaseData = SscsCaseData.builder().interlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE).build();
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), CASE_UPDATED, false);

        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }
}
