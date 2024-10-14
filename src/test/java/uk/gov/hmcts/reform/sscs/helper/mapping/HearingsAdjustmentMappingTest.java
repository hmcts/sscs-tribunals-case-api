package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment.HEARING_LOOP;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment.SIGN_LANGUAGE_INTERPRETER;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment.STEP_FREE_WHEELCHAIR_ACCESS;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.exception.InvalidMappingException;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment;

class HearingsAdjustmentMappingTest {


    @DisplayName("When a valid adjustments are given in hearingOptions getIndividualsAdjustments returns the correct list")
    @Test
    void testGetIndividualsAdjustments() throws InvalidMappingException {
        List<String> adjustments = List.of(
            "signLanguageInterpreter",
            "hearingLoop",
            "disabledAccess");

        HearingOptions hearingOptions = HearingOptions.builder()
            .arrangements(adjustments)
            .build();

        List<Adjustment> result = HearingsAdjustmentMapping.getIndividualsAdjustments(hearingOptions);

        assertThat(result)
            .hasSize(3)
            .containsExactlyInAnyOrder(
                SIGN_LANGUAGE_INTERPRETER,
                HEARING_LOOP,
                STEP_FREE_WHEELCHAIR_ACCESS);
    }

    @DisplayName("When a null hearing options is given getIndividualsAdjustments returns a empty list")
    @Test
    void testGetIndividualsAdjustmentsNull() throws InvalidMappingException {
        List<Adjustment> result = HearingsAdjustmentMapping.getIndividualsAdjustments(null);

        assertThat(result).isEmpty();
    }

    @DisplayName("When a valid adjustments are given getAdjustments returns the correct list")
    @Test
    void testGetAdjustments() throws InvalidMappingException {
        List<String> adjustments = List.of(
            "signLanguageInterpreter",
            "hearingLoop",
            "disabledAccess");

        List<Adjustment> result = HearingsAdjustmentMapping.getAdjustments(adjustments);

        assertThat(result)
            .hasSize(3)
            .containsExactlyInAnyOrder(
                SIGN_LANGUAGE_INTERPRETER,
                HEARING_LOOP,
                STEP_FREE_WHEELCHAIR_ACCESS);
    }

    @DisplayName("When a empty list of adjustments are given getAdjustments returns a empty list")
    @Test
    void testGetAdjustmentsEmptyList() throws InvalidMappingException {
        List<Adjustment> result = HearingsAdjustmentMapping.getAdjustments(List.of());

        assertThat(result).isEmpty();
    }

    @DisplayName("When a null value is given getAdjustments returns a empty list")
    @Test
    void testGetAdjustmentsNull() throws InvalidMappingException {
        List<Adjustment> result = HearingsAdjustmentMapping.getAdjustments(null);

        assertThat(result).isEmpty();
    }

    @DisplayName("When a valid adjustment is given getAdjustment returns the correct value")
    @ParameterizedTest
    @CsvSource(value = {
        "signLanguageInterpreter,SIGN_LANGUAGE_INTERPRETER",
        "hearingLoop,HEARING_LOOP",
        "disabledAccess,STEP_FREE_WHEELCHAIR_ACCESS",
    }, nullValues = {"null"})
    void testGetAdjustment(String value, Adjustment expected) throws InvalidMappingException {
        Adjustment result = HearingsAdjustmentMapping.getAdjustment(value);

        assertThat(result).isEqualTo(expected);
    }

    @DisplayName("When a invalid adjustment is given getAdjustment throws an error with the correct message")
    @ParameterizedTest
    @ValueSource(strings = {"Test"})
    @EmptySource
    void testGetAdjustment(String value) {
        assertThatExceptionOfType(InvalidMappingException.class)
            .isThrownBy(() -> HearingsAdjustmentMapping.getAdjustment(value))
            .withMessageContaining("The adjustment '%s' given cannot be mapped", value);
    }
}
