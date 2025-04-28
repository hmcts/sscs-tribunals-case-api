package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist.ReadyToListAboutToSubmitHandler.EXISTING_HEARING_WARNING;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist.ReadyToListAboutToSubmitHandler.GAPS_CASE_WARNING;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@ExtendWith(MockitoExtension.class)
public class ReadyToListAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String CASE_ID = "1234";

    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    private ReadyToListAboutToSubmitHandler handler;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().ccdCaseId(CASE_ID)
                .createdInGapsFrom(READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();
        caseDetails =
                new CaseDetails<>(1L,null, null, sscsCaseData, null, null);
        callback = new Callback<>(caseDetails, empty(), EventType.READY_TO_LIST, false);

        handler = new ReadyToListAboutToSubmitHandler();
    }

    @ParameterizedTest
    @CsvSource({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        callback = new Callback<>(caseDetails, empty(), eventType, false);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAGapsCaseOnSubmitReturnWarning() {
        sscsCaseData.setSchedulingAndListingFields(
                SchedulingAndListingFields.builder().hearingRoute(HearingRoute.GAPS).build()
        );

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,
                callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(1, response.getWarnings().size());
        assertEquals(GAPS_CASE_WARNING, response.getWarnings().iterator().next());
    }

    @Test
    public void givenAGapsCaseOnSubmitIgnoreWarningIIgnoreWarningsFieldIsYes() {
        sscsCaseData.setSchedulingAndListingFields(
                SchedulingAndListingFields.builder().hearingRoute(HearingRoute.GAPS).build()
        );
        sscsCaseData.setIgnoreCallbackWarnings(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,
                callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
    }

    @ParameterizedTest
    @CsvSource({"YES", "NO"})
    public void givenAListAssistCaseIfAHearingExistsInTheFutureThenReturnWarning(YesNo ignoreCallbackWarnings) {
        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
                .hearingDate(LocalDate.now().minusDays(10).toString())
                .start(now().minusDays(10))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(5).toString())
                .start(now().plusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build()).build();

        sscsCaseData.setHearings(List.of(hearing1, hearing2));
        sscsCaseData.setRegion("TEST");
        sscsCaseData.setIgnoreCallbackWarnings(ignoreCallbackWarnings);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        if (ignoreCallbackWarnings == NO) {
            assertEquals(1, response.getWarnings().size());
            assertTrue(response.getWarnings().contains(EXISTING_HEARING_WARNING));
        } else {
            assertEquals(0, response.getWarnings().size());
        }
    }

    @Test
    public void respondWithNoErrorsIfCreatedFromGapsIsAtReadyToList() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), EventType.APPEAL_RECEIVED, false);

        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }
}
