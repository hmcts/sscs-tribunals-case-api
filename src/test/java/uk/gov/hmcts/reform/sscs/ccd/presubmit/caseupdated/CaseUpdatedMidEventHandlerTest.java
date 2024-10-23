package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@ExtendWith(MockitoExtension.class)
public class CaseUpdatedMidEventHandlerTest {
    private static final String HEARING_ROUTE_ERROR_MESSAGE = "Hearing route must be List Assist";
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private SscsCaseData sscsCaseData;

    @InjectMocks
    private CaseUpdatedMidEventHandler midEventHandler;

    @Test
    void canHandleTest() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        assertTrue(midEventHandler.canHandle(MID_EVENT, callback));
    }


    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"CASE_UPDATED"})
    void cannotHandleTestForInvalidEvents(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(midEventHandler.canHandle(MID_EVENT, callback));
    }


    @ParameterizedTest
    @EnumSource(value = CallbackType.class, mode = EnumSource.Mode.EXCLUDE, names = {"MID_EVENT"})
    void cannotHandleTestForInvalidCallbackType(CallbackType callbackType) {
        assertFalse(midEventHandler.canHandle(callbackType, callback));
    }

    @Test
    void handleThrowsIfCanHandleFalse() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION));
        assertEquals("Cannot handle callback", exception.getMessage());
    }

    @Test
    void shouldDoNothingForNonIbcaCase() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(sscsCaseData.getBenefitCode()).thenReturn("000");
        PreSubmitCallbackResponse<SscsCaseData> expectedResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(new ReflectionEquals(expectedResponse).matches(actualResponse));
        assertEquals(actualResponse.getErrors().size(), 0);
    }

    @Test
    void shouldDoNothingForIbcaCaseListAssist() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(sscsCaseData.getBenefitCode()).thenReturn(IBCA_BENEFIT_CODE);
        when(sscsCaseData.getRegionalProcessingCenter()).thenReturn(RegionalProcessingCenter.builder().hearingRoute(HearingRoute.LIST_ASSIST).build());
        PreSubmitCallbackResponse<SscsCaseData> expectedResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(new ReflectionEquals(expectedResponse).matches(actualResponse));
        assertEquals(actualResponse.getErrors().size(), 0);
    }

    @Test
    void shouldReturnErrorsForIbcaCaseGaps() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(sscsCaseData.getBenefitCode()).thenReturn(IBCA_BENEFIT_CODE);
        when(sscsCaseData.getRegionalProcessingCenter()).thenReturn(RegionalProcessingCenter.builder().hearingRoute(HearingRoute.GAPS).build());
        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(response.getErrors().size(), 1);
        assertThat(response.getErrors()).contains(HEARING_ROUTE_ERROR_MESSAGE);
    }
}
