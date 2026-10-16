package uk.gov.hmcts.reform.sscs.ccd.presubmit.associatecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
class AssociateCaseMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String SET_TO_NO_ERROR = "Set Has related appeal(s) to 'No'";
    private static final String SET_TO_YES_ERROR = "Set Has related appeal(s) to 'Yes'";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private AssociateCaseMidEventHandler handler;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        handler = new AssociateCaseMidEventHandler();
        sscsCaseData = SscsCaseData.builder().ccdCaseId("1").build();

        lenient().when(callback.getEvent()).thenReturn(EventType.ASSOCIATE_CASE);
        lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
        lenient().when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenMidEventAndAssociateCaseEvent_thenReturnTrue() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    void givenANonMidEventCallbackType_thenReturnFalse(final CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void givenANonAssociateCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void throwsExceptionIfItCannotHandleTheCallback() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThatThrownBy(() -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @MethodSource("linkedCasesScenarios")
    void givenLinkedCasesBooleanAndAssociatedCases_thenReturnExpectedErrors(final String linkedCasesBoolean,
                                                                            final List<CaseLink> associatedCases,
                                                                            final List<String> expectedErrors) {
        sscsCaseData.setLinkedCasesBoolean(linkedCasesBoolean);
        sscsCaseData.setAssociatedCase(associatedCases);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getData()).isSameAs(sscsCaseData);
        assertThat(response.getErrors()).containsExactlyInAnyOrderElementsOf(expectedErrors);
    }

    private static Stream<Arguments> linkedCasesScenarios() {
        return Stream.of(
            Arguments.of("Yes", null, List.of(SET_TO_NO_ERROR)),
            Arguments.of("Yes", new ArrayList<>(), List.of(SET_TO_NO_ERROR)),
            Arguments.of("Yes", caseLinks("2"), List.of()),
            Arguments.of("No", caseLinks("2"), List.of(SET_TO_YES_ERROR)),
            Arguments.of("No", new ArrayList<>(), List.of()),
            Arguments.of("No", null, List.of()),
            Arguments.of(null, caseLinks("2"), List.of()),
            Arguments.of(null, null, List.of())
        );
    }

    private static List<CaseLink> caseLinks(final String caseReference) {
        final List<CaseLink> caseLinks = new ArrayList<>();
        caseLinks.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference(caseReference).build()).build());
        return caseLinks;
    }
}
