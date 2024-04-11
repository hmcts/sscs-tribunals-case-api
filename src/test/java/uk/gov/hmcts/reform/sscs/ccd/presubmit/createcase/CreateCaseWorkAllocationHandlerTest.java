package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
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

    @ParameterizedTest
    @CsvSource({
        "VALID_APPEAL_CREATED, false",
        "VALID_APPEAL_CREATED, true",
        "DRAFT_TO_VALID_APPEAL_CREATED, false",
        "DRAFT_TO_VALID_APPEAL_CREATED, true",
        "NON_COMPLIANT, false","NON_COMPLIANT, false",
        "NON_COMPLIANT, false","NON_COMPLIANT, true",
        "DRAFT_TO_NON_COMPLIANT, false",
        "DRAFT_TO_NON_COMPLIANT, true",
        "INCOMPLETE_APPLICATION_RECEIVED, false",
        "INCOMPLETE_APPLICATION_RECEIVED, true",
        "DRAFT_TO_INCOMPLETE_APPLICATION, false",
        "DRAFT_TO_INCOMPLETE_APPLICATION, true"
    })
    public void givenValidEvent_thenReturnTrue(EventType eventType, boolean workAllocationFeature) {
        CreateCaseWorkAllocationHandler handler = new CreateCaseWorkAllocationHandler(workAllocationFeature);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, createCallBack(eventType)));
    }

    @ParameterizedTest
    @CsvSource({
        "UPLOAD_DOCUMENT, true",
        "UPLOAD_DOCUMENT, false"
    })
    public void givenAnInvalidEvent_thenReturnFalse(EventType eventType, boolean workAllocationFeature) {
        CreateCaseWorkAllocationHandler handler = new CreateCaseWorkAllocationHandler(workAllocationFeature);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, createCallBack(eventType)));
    }

    @ParameterizedTest
    @CsvSource({
        "ABOUT_TO_START, true",
        "ABOUT_TO_START, false",
        "MID_EVENT, true",
        "MID_EVENT, false",
        "SUBMITTED, true",
        "SUBMITTED, false"
    })
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType, boolean workAllocationFeature) {
        CreateCaseWorkAllocationHandler handler = new CreateCaseWorkAllocationHandler(workAllocationFeature);
        assertFalse(handler.canHandle(callbackType, createCallBack(EventType.VALID_APPEAL_CREATED)));
    }

    @ParameterizedTest
    @CsvSource({
        "VALID_APPEAL_CREATED, false, YES",
        "VALID_APPEAL_CREATED, true, NO",
        "DRAFT_TO_VALID_APPEAL_CREATED, false, YES",
        "DRAFT_TO_VALID_APPEAL_CREATED, true, NO",
        "NON_COMPLIANT, false, YES",
        "NON_COMPLIANT, true, NO",
        "DRAFT_TO_NON_COMPLIANT, false, YES",
        "DRAFT_TO_NON_COMPLIANT, true, NO",
        "INCOMPLETE_APPLICATION_RECEIVED, false, YES",
        "INCOMPLETE_APPLICATION_RECEIVED, true, NO",
        "DRAFT_TO_INCOMPLETE_APPLICATION, false, YES",
        "DRAFT_TO_INCOMPLETE_APPLICATION, true, NO"
    })
    public void whenNewCaseCreated_setFieldPreWorkAllocationCorrectly(EventType eventType, boolean workAllocationFeature, String preWorkAllocation) {
        CreateCaseWorkAllocationHandler handler = new CreateCaseWorkAllocationHandler(workAllocationFeature);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(eventType), USER_AUTHORISATION);
        assertEquals(YesNo.valueOf(preWorkAllocation), response.getData().getPreWorkAllocation());
    }

    private Callback<SscsCaseData> createCallBack(EventType event) {
        SscsCaseData caseData = SscsCaseData.builder().build();

        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.VALID_APPEAL, caseData, LocalDateTime.now(), "Benefit");

        return new Callback<>(caseDetails, Optional.empty(), event, false);
    }
}
