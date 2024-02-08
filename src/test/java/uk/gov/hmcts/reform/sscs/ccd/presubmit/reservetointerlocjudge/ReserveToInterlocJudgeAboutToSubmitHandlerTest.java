package uk.gov.hmcts.reform.sscs.ccd.presubmit.reservetointerlocjudge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;

@ExtendWith(MockitoExtension.class)
public class ReserveToInterlocJudgeAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private ReserveToInterlocJudgeAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        handler = new ReserveToInterlocJudgeAboutToSubmitHandler(true);
        sscsCaseData = SscsCaseData.builder().build();
    }

    @Test
    void givenValidCallback_thenReturnTrue() {
        given(callback.getEvent()).willReturn(EventType.RESERVE_TO_INTERLOC_JUDGE);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    void givenInvalidEventType_thenReturnFalse() {
        given(callback.getEvent()).willReturn(EventType.ADD_HEARING);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void handReserveToInterlocJudgeWhenIsSetAndReserveToIsNo() {
        given(callback.getEvent()).willReturn(EventType.RESERVE_TO_INTERLOC_JUDGE);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        JudicialUserBase reservedJudge = new JudicialUserBase("idamId", "personalCode");
        sscsCaseData.setJudgeReserved(YesNo.NO);
        sscsCaseData.setReservedToJudgeInterloc(reservedJudge);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                ABOUT_TO_SUBMIT,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getReservedToJudgeInterloc()).isNull();
    }

    @Test
    void handReserveToInterlocJudgeWhenIsSetAndReserveToIsYes() {
        given(callback.getEvent()).willReturn(EventType.RESERVE_TO_INTERLOC_JUDGE);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        JudicialUserBase reservedJudge = new JudicialUserBase("idamId", "personalCode");
        sscsCaseData.setJudgeReserved(YesNo.YES);
        sscsCaseData.setReservedToJudgeInterloc(reservedJudge);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                ABOUT_TO_SUBMIT,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getReservedToJudgeInterloc()).isEqualTo(reservedJudge);
    }
}
