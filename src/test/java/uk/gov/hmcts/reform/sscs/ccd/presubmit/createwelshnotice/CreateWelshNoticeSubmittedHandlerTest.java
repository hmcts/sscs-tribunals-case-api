package uk.gov.hmcts.reform.sscs.ccd.presubmit.createwelshnotice;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_WELSH_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DIRECTION_ISSUED_WELSH;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@ExtendWith(MockitoExtension.class)
class CreateWelshNoticeSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @InjectMocks
    private CreateWelshNoticeSubmittedHandler handler;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    @ParameterizedTest
    @MethodSource("generateCanHandleScenarios")
    void givenCanHandleIsCalled_shouldReturnCorrectResult(CallbackType callbackType,
                                                                 Callback<SscsCaseData> callback,
                                                                 boolean expectedResult) {
        boolean actualResult = handler.canHandle(callbackType, callback);
        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    void shouldCallTriggerCaseEvent() {
        Callback<SscsCaseData> callback = buildCallback(DIRECTION_ISSUED_WELSH.getCcdType());
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        given(updateCcdCaseService.triggerCaseEventV2(anyLong(), eq(DIRECTION_ISSUED_WELSH.getCcdType()), anyString(),
            anyString(), eq(idamTokens)))
            .willReturn(SscsCaseDetails.builder().data(caseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        then(updateCcdCaseService).should(times(1))
            .triggerCaseEventV2(eq(123L), eq(DIRECTION_ISSUED_WELSH.getCcdType()), eq("Create Welsh notice"),
                eq("Create Welsh notice"), eq(idamTokens));

        Assertions.assertNull(caseData.getSscsWelshPreviewNextEvent());

    }

    private static Stream<Arguments> generateCanHandleScenarios() {
        return Stream.of(
            Arguments.arguments(SUBMITTED, buildCallback(DIRECTION_ISSUED_WELSH.getCcdType()), true),
            Arguments.arguments(ABOUT_TO_SUBMIT, buildCallback(DIRECTION_ISSUED_WELSH.getCcdType()), false),
            Arguments.arguments(SUBMITTED, buildCallback(null), false)
        );
    }

    private static Callback<SscsCaseData> buildCallback(String sscsWelshPreviewNextEvent) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .sscsWelshPreviewNextEvent(sscsWelshPreviewNextEvent)
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            State.VALID_APPEAL, sscsCaseData, LocalDateTime.now(), "Benefit-4106");
        return new Callback<>(caseDetails, Optional.empty(), CREATE_WELSH_NOTICE, false);
    }

}
