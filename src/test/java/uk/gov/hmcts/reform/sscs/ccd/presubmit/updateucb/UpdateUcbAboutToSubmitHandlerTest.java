package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateucb;

import static java.time.LocalDateTime.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_UCB;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@ExtendWith(MockitoExtension.class)
public class UpdateUcbAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private SscsCaseData sscsCaseData;
    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;

    private UpdateUcbAboutToSubmitHandler handler;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .build();
        caseDetails =
                new CaseDetails<>(1234L, "SSCS", State.READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.empty(), UPDATE_UCB, false);
        handler = new UpdateUcbAboutToSubmitHandler();
    }

    @Test
    public void givenANonUpdateUcbCaseEvent_thenReturnFalse() {
        callback = new Callback<>(caseDetails, Optional.empty(), APPEAL_RECEIVED, false);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, Optional.empty(), APPEAL_RECEIVED, false);

        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    public void givenUpdateUcb_setToYes_thenNoChange() {
        sscsCaseData.setDwpUcb(YES.getValue());
        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDwpUcb(), is(YES.getValue()));
    }

    @Test
    public void givenUpdateUcb_setToNo_thenSetToNull() {
        sscsCaseData.setDwpUcb(NO.getValue());
        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDwpUcb(), is(nullValue()));
    }
}
