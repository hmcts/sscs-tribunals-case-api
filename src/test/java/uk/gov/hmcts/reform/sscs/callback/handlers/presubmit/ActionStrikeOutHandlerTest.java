package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit;

import static java.time.LocalDateTime.now;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ACTION_STRIKE_OUT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Optional;
import junitparams.converters.Nullable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.ActionStrikeOutHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.PoDetails;

@ExtendWith(MockitoExtension.class)
public class ActionStrikeOutHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private SscsCaseData sscsCaseData;
    private CaseDetails<SscsCaseData> caseDetails;
    private Callback<SscsCaseData> callback;

    private ActionStrikeOutHandler actionStrikeOutHandler;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().build();
        caseDetails = new CaseDetails<>(123L, "SSCS", WITH_DWP, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), READY_TO_LIST, false);

        actionStrikeOutHandler = new ActionStrikeOutHandler();
    }


    @ParameterizedTest
    @CsvSource({
        "ACTION_STRIKE_OUT, ABOUT_TO_SUBMIT, true"
    })
    public void givenEvent_thenCanHandle(EventType eventType, CallbackType callbackType, boolean expected) {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, false);

        assertEquals(expected, actionStrikeOutHandler.canHandle(callbackType, callback));
    }

    @Test
    public void givenNullCallback_shouldThrowException() {
        assertThrows(NullPointerException.class, () -> actionStrikeOutHandler.canHandle(ABOUT_TO_SUBMIT, null));
    }

    @ParameterizedTest
    @CsvSource({
        "ACTION_STRIKE_OUT, strikeOut, STRIKE_OUT_ACTIONED",
        "ACTION_STRIKE_OUT, ,null",
        "ACTION_STRIKE_OUT, null,null",
    })
    public void givenEvent_thenSetDwpStateToExpected(EventType eventType, @Nullable String decisionType,
                                                     @Nullable String expectedDwpStateStr) {
        DwpState expectedDwpState = "null".equals(expectedDwpStateStr) ? null : DwpState.valueOf(expectedDwpStateStr);
        sscsCaseData.setDecisionType(decisionType);
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, false);

        var response = actionStrikeOutHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(expectedDwpState));
        if (StringUtils.isBlank(decisionType)) {
            String error = response.getErrors().stream().findFirst().orElse("");
            assertEquals("The decision type is not \"strike out\". We cannot proceed.", error);
        }
    }

    @Test
    public void throwExceptionIfCannotHandleEventType() {
        sscsCaseData = SscsCaseData.builder().dwpState(DwpState.IN_PROGRESS).build();
        caseDetails = new CaseDetails<>(123L, "SSCS", WITH_DWP, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), CASE_UPDATED, false);

        assertThrows(IllegalStateException.class,
                () -> actionStrikeOutHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    public void givenActionStrikeOut_thenClearPoFields() {
        sscsCaseData.setDecisionType("strikeOut");
        sscsCaseData.setPoAttendanceConfirmed(YES);
        sscsCaseData.setPresentingOfficersDetails(PoDetails.builder().name(Name.builder().build()).build());
        sscsCaseData.setPresentingOfficersHearingLink("link");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), ACTION_STRIKE_OUT, false);

        actionStrikeOutHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertThat(sscsCaseData.getPoAttendanceConfirmed()).isEqualTo(NO);
        Assertions.assertThat(sscsCaseData.getPresentingOfficersDetails()).isEqualTo(PoDetails.builder().build());
        Assertions.assertThat(sscsCaseData.getPresentingOfficersHearingLink()).isNull();
    }
}
