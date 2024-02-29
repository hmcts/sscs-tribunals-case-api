package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpoattendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIRM_PO_ATTENDANCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@ExtendWith(MockitoExtension.class)
public class ConfirmPoAttendanceMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    private ConfirmPoAttendanceMidEventHandler handler;


    @BeforeEach
    void setUp() {
        handler = new ConfirmPoAttendanceMidEventHandler();

        caseData = SscsCaseData.builder().poAttendanceConfirmed(YesNo.YES).build();
    }

    @Test
    void givenAValidMidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(CONFIRM_PO_ATTENDANCE);
        assertThat(handler.canHandle(MID_EVENT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenNoContactOrMobileNumberOnCase_addErrorToResponse() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getEvent()).thenReturn(CONFIRM_PO_ATTENDANCE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors()).contains("Add a contact or mobile number");
    }

    @Test
    void givenMobileNumberOnCase_thenSubmitEvent() {
        caseData.getPresentingOfficersDetails().setContact(Contact.builder().mobile("07415103842").build());

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getEvent()).thenReturn(CONFIRM_PO_ATTENDANCE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
    }

    @Test
    void givenPoAttendanceIsNoAndNoPhoneNumber_thenSubmitEvent() {
        caseData.setPoAttendanceConfirmed(YesNo.NO);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getEvent()).thenReturn(CONFIRM_PO_ATTENDANCE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
    }
}
