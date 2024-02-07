package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@RunWith(JUnitParamsRunner.class)
public class CreateCaseWorkAllocationHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private CreateCaseWorkAllocationHandler handler = new CreateCaseWorkAllocationHandler(true);

    @ParameterizedTest
    @CsvSource({
        "VALID_APPEAL_CREATED",
        "DRAFT_TO_VALID_APPEAL_CREATED",
        "NON_COMPLIANT",
        "DRAFT_TO_NON_COMPLIANT",
        "INCOMPLETE_APPLICATION_RECEIVED",
        "DRAFT_TO_INCOMPLETE_APPLICATION",
    })
    public void givenValidEvent_thenReturnTrue(EventType eventType) {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, createCallBack(eventType)));
    }

    @ParameterizedTest
    @CsvSource({
        "UPLOAD_DOCUMENT"
    })
    public void givenAnInvalidEvent_thenReturnFalse(EventType eventType) {
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, createCallBack(eventType)));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, createCallBack(EventType.VALID_APPEAL_CREATED)));
    }

    @ParameterizedTest
    @CsvSource({
        "VALID_APPEAL_CREATED",
        "DRAFT_TO_VALID_APPEAL_CREATED",
        "NON_COMPLIANT",
        "DRAFT_TO_NON_COMPLIANT",
        "INCOMPLETE_APPLICATION_RECEIVED",
        "DRAFT_TO_INCOMPLETE_APPLICATION",
    })
    public void whenWorkAllocationEnabled_setFieldPreWorkAllocationToNo(EventType event) {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(event), USER_AUTHORISATION);
        assertEquals(YesNo.NO, response.getData().getPreWorkAllocation());
    }

    private Callback<SscsCaseData> createCallBack(EventType event) {
        SscsCaseData caseData = SscsCaseData.builder().build();

        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.VALID_APPEAL, caseData, LocalDateTime.now(), "Benefit");

        return new Callback<>(caseDetails, Optional.empty(), event, false);
    }
}
