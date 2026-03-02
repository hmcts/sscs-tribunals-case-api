package uk.gov.hmcts.reform.sscs.service.metadataprovider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.AWAIT_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
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

@ExtendWith(MockitoExtension.class)
class EnableAddOtherPartyDataMetadataFieldProviderTest {

    private static final String FIELD_ID = "[INJECTED_DATA.ENABLE_ADD_OTHER_PARTY_DATA]";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Test
    void shouldReturnEmptyWhenDisabled() {
        final EnableAddOtherPartyDataMetadataFieldProvider provider =
            new EnableAddOtherPartyDataMetadataFieldProvider(false);

        assertThat(provider.provide(callback)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideFieldWithYesScenarios")
    void shouldReturnFieldWithYesWhenEnabled(Benefit benefit, State state) {
        setupCallback(benefit);
        when(caseDetails.getState()).thenReturn(state);
        final EnableAddOtherPartyDataMetadataFieldProvider provider =
            new EnableAddOtherPartyDataMetadataFieldProvider(true);

        final Optional<CaseViewField> result = provider.provide(callback);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(FIELD_ID);
        assertThat(result.get().getValue()).isEqualTo(YES);
    }

    @ParameterizedTest
    @MethodSource("provideFieldWithNoScenarios")
    void shouldReturnFieldWithNoWhenEnabled(Benefit benefit) {
        setupCallback(benefit);
        final EnableAddOtherPartyDataMetadataFieldProvider provider =
            new EnableAddOtherPartyDataMetadataFieldProvider(true);

        final Optional<CaseViewField> result = provider.provide(callback);

        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isEqualTo(NO);
    }

    @Test
    void shouldReturnFieldWithCorrectMetadataWhenEnabled() {
        setupCallback(Benefit.CHILD_SUPPORT);
        final EnableAddOtherPartyDataMetadataFieldProvider provider =
            new EnableAddOtherPartyDataMetadataFieldProvider(true);

        final CaseViewField field = provider.provide(callback).orElseThrow();

        assertThat(field.isMetadata()).isTrue();
        assertThat(field.getFieldTypeDefinition().getId()).isEqualTo("Text");
        assertThat(field.getFieldTypeDefinition().getType()).isEqualTo("Label");
    }

    private static Stream<Arguments> provideFieldWithYesScenarios() {
        return Stream.of(
            Arguments.of(Benefit.CHILD_SUPPORT, AWAIT_OTHER_PARTY_DATA),
            Arguments.of(Benefit.UC, WITH_DWP)
        );
    }

    private static Stream<Arguments> provideFieldWithNoScenarios() {
        return Stream.of(
            Arguments.of(Benefit.CHILD_SUPPORT, WITH_DWP),
            Arguments.of(Benefit.UC, AWAIT_OTHER_PARTY_DATA),
            Arguments.of(Benefit.PIP, AWAIT_OTHER_PARTY_DATA)
        );
    }

    private void setupCallback(final Benefit benefit) {
        final SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(benefit.getShortName()).build())
                .build())
            .build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
    }
}