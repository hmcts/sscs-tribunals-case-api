package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.HMCTS_RESPONSE_REVIEWED;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
public class HmctsResponseReviewedMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @InjectMocks
    private HmctsResponseReviewedMidEventHandler midEventHandler;

    @Test
    void shouldReturnTrueIfIbcaCaseOnCanHandle() {
        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .build();

        when(callback.getEvent()).thenReturn(HMCTS_RESPONSE_REVIEWED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertTrue(midEventHandler.canHandle(MID_EVENT, callback));
    }

    @Test
    void shouldReturnFalseIfNotIbcaCaseOnCanHandle() {
        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode("015")
                .build();

        when(callback.getEvent()).thenReturn(HMCTS_RESPONSE_REVIEWED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertFalse(midEventHandler.canHandle(MID_EVENT, callback));
    }

    @Test
    void shouldReturnFalseIfNotHmctsResponseReviewedEventOnCanHandle() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(midEventHandler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void shouldNotReturnAnyErrorsIfIssueCodeIsNotDD() {
        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .issueCodeIbcaOnly("OX")
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(empty()));
    }

    @Test
    public void shouldReturnErrorsIfIssueCodeIsDD() {
        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .issueCodeIbcaOnly("DD")
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), hasSize(1));
        assertTrue(response.getErrors().contains("You cannot proceed with DD as the case Issue code. Please select another issue code before proceeding."));
    }
}
