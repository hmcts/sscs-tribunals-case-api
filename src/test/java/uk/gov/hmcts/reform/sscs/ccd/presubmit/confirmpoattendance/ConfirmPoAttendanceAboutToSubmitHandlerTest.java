package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpoattendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PoDetails;

@ExtendWith(MockitoExtension.class)
public class ConfirmPoAttendanceAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    private ConfirmPoAttendanceAboutToSubmitHandler handler;


    @BeforeEach
    void setUp() {
        handler = new ConfirmPoAttendanceAboutToSubmitHandler();

        caseData = SscsCaseData.builder().build();
    }

    @Test
    void givenAValidMidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(CONFIRM_PO_ATTENDANCE);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenHandlerIsCalledAndPoAttendanceIsYes_thenKeepPoValues() {
        PoDetails poDetails = PoDetails.builder().contact(Contact.builder().email("sds").mobile("sdsd").build()).build();
        caseData.setPoAttendanceConfirmed(YesNo.YES);
        caseData.setPresentingOfficersDetails(poDetails);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getEvent()).thenReturn(CONFIRM_PO_ATTENDANCE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getPresentingOfficersDetails()).isEqualTo(poDetails);
    }

    @Test
    void givenHandlerIsCalledAndPoAttendanceIsNo_thenRemovePoValues() {
        PoDetails poDetails = PoDetails.builder().contact(Contact.builder().email("sds").mobile("sdsd").build()).build();
        caseData.setPoAttendanceConfirmed(YesNo.NO);
        caseData.setPresentingOfficersDetails(poDetails);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getEvent()).thenReturn(CONFIRM_PO_ATTENDANCE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getPresentingOfficersDetails()).isEqualTo(PoDetails.builder().build());
    }
}
