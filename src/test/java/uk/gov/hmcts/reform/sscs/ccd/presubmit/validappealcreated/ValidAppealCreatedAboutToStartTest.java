package uk.gov.hmcts.reform.sscs.ccd.presubmit.validappealcreated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNoUnknown;

@ExtendWith(MockitoExtension.class)
class ValidAppealCreatedAboutToStartTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private final ValidAppealCreatedAboutToStart handler = new ValidAppealCreatedAboutToStart();

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().build()).build())
            .build();
    }

    @Test
    void givenAValidAppealCreatedAboutToStartEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(VALID_APPEAL_CREATED);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    void givenANonValidAppealCreatedEvent_thenReturnFalse(final EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    void givenANonAboutToStartCallbackType_thenReturnFalse(final CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void givenANonCanHandleCallback_thenThrowException() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThatThrownBy(() -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot handle callback");
    }

    @Test
    void givenACallback_thenSetConfidentialityRequirementDynamicList() {
        when(callback.getEvent()).thenReturn(VALID_APPEAL_CREATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        final var response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        final var confidentialityRequirement = response.getData().getAppeal().getAppellant().getConfidentialityRequirement();

        assertThat(confidentialityRequirement.getValue()).isEqualTo(new DynamicListItem(null, null));
        assertThat(confidentialityRequirement.getListItems())
            .extracting(DynamicListItem::getCode, DynamicListItem::getLabel)
            .containsExactly(
                tuple(YesNoUnknown.YES.name(), YesNoUnknown.YES.toString()),
                tuple(YesNoUnknown.UNKNOWN.name(), YesNoUnknown.UNKNOWN.toString()),
                tuple(YesNoUnknown.NO.name(), YesNoUnknown.NO.toString()));
    }
}
