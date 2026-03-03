package uk.gov.hmcts.reform.sscs.service.metadataprovider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.AWAIT_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.CaseViewField;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@ExtendWith(MockitoExtension.class)
class EnableAddOtherPartyDataMetadataFieldProviderTest {

    private static final String FIELD_ID = "[INJECTED_DATA.ENABLE_ADD_OTHER_PARTY_DATA]";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @ParameterizedTest
    @MethodSource("provideValueScenarios")
    void shouldReturnExpectedValueForGivenBenefitStateAndFeatureFlags(final boolean cmFeatureEnabled,
        final boolean ucFeatureEnabled,
        final Benefit benefit,
        final State state,
        final YesNo expectedValue) {
        setupCallback(benefit);
        lenient().when(caseDetails.getState()).thenReturn(state);
        final EnableAddOtherPartyDataMetadataFieldProvider provider =
            new EnableAddOtherPartyDataMetadataFieldProvider(cmFeatureEnabled, ucFeatureEnabled);

        final Optional<CaseViewField> result = provider.provide(callback);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(FIELD_ID);
        assertThat(result.get().getValue()).isEqualTo(expectedValue);
    }

    private static Stream<Arguments> provideValueScenarios() {
        return Stream.of(
            Arguments.of(false, false, Benefit.CHILD_SUPPORT, AWAIT_OTHER_PARTY_DATA, NO),
            Arguments.of(false, false, Benefit.UC, WITH_DWP, NO),
            Arguments.of(true, false, Benefit.CHILD_SUPPORT, AWAIT_OTHER_PARTY_DATA, YES),
            Arguments.of(true, false, Benefit.CHILD_SUPPORT, WITH_DWP, NO),
            Arguments.of(true, false, Benefit.UC, WITH_DWP, NO),
            Arguments.of(true, false, Benefit.PIP, AWAIT_OTHER_PARTY_DATA, NO),
            Arguments.of(false, true, Benefit.UC, WITH_DWP, YES),
            Arguments.of(false, true, Benefit.UC, AWAIT_OTHER_PARTY_DATA, NO),
            Arguments.of(false, true, Benefit.CHILD_SUPPORT, AWAIT_OTHER_PARTY_DATA, NO),
            Arguments.of(false, true, Benefit.PIP, WITH_DWP, NO),
            Arguments.of(true, true, Benefit.CHILD_SUPPORT, AWAIT_OTHER_PARTY_DATA, YES),
            Arguments.of(true, true, Benefit.UC, WITH_DWP, YES),
            Arguments.of(true, true, Benefit.CHILD_SUPPORT, WITH_DWP, NO),
            Arguments.of(true, true, Benefit.UC, AWAIT_OTHER_PARTY_DATA, NO),
            Arguments.of(true, true, Benefit.PIP, WITH_DWP, NO)
        );
    }

    private void setupCallback(final Benefit benefit) {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(benefit.getShortName()).build())
                .build())
            .build();
        lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
        lenient().when(caseDetails.getCaseData()).thenReturn(caseData);
    }
}
