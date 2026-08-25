package uk.gov.hmcts.reform.sscs.bulkscan.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.OcrDataBuilder.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.OcrDataField;

public class OcrDataBuilderTest {

    @Test
    void givenValidationOcrData_thenConvertIntoKeyValuePairs() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "person1_first_name");
        valueMap.put("value", "Bob");

        Map<String, Object> result = build(buildScannedValidationOcrData(valueMap));

        assertThat(result).containsEntry("person1_first_name", "Bob");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void givenValidationOcrDataWithBlankValue_thenConvertIntoKeyValuePairsWithNullValue(String value) {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "person1_first_name");
        valueMap.put("value", value);

        Map<String, Object> result = build(buildScannedValidationOcrData(valueMap));

        assertThat(result.get("person1_first_name")).isNull();
    }

    @Test
    void givenValidationOcrDataWithLeadingAndTrailingSpacesInValue_thenConvertIntoKeyValuePairsTrimmed() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "person1_first_name");
        valueMap.put("value", "  Bob  ");

        Map<String, Object> result = build(buildScannedValidationOcrData(valueMap));

        assertThat(result).containsEntry("person1_first_name", "Bob");
    }

    @Test
    void givenNullOcrData_thenReturnEmptyMap() {
        Map<String, Object> result = build(null);

        assertThat(result).isEmpty();
    }

    @Test
    void givenValidationOcrDataWithNullKeyAndNullValue_thenConvertIntoKeyValuePairs() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", null);
        valueMap.put("value", null);

        Map<String, Object> result = build(buildScannedValidationOcrData(valueMap));

        assertThat(result).isEmpty();
    }

    @SafeVarargs
    public static List<OcrDataField> buildScannedValidationOcrData(Map<String, Object>... valueMap) {
        List<OcrDataField> scannedOcrDataList = new ArrayList<>();

        for (Map<String, Object> values: valueMap) {
            String name = values.get("name") != null ? values.get("name").toString() : null;
            String value = values.get("value") != null ? values.get("value").toString() : null;
            scannedOcrDataList.add(new OcrDataField(name, value));
        }

        return scannedOcrDataList;
    }

}
