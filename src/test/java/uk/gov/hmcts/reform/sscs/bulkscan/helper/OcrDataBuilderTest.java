package uk.gov.hmcts.reform.sscs.bulkscan.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.OcrDataBuilder.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.OcrDataField;

public class OcrDataBuilderTest {

    @Test
    public void givenValidationOcrData_thenConvertIntoKeyValuePairs() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "person1_first_name");
        valueMap.put("value", "Bob");

        Map<String, Object> result = build(buildScannedValidationOcrData(valueMap));

        assertEquals("Bob", result.get("person1_first_name"));
    }

    @Test
    public void givenValidationOcrDataWithNullValue_thenConvertIntoKeyValuePairs() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "person1_first_name");
        valueMap.put("value", null);

        Map<String, Object> result = build(buildScannedValidationOcrData(valueMap));

        assertNull(result.get("person1_first_name"));
    }

    @Test
    public void givenValidationOcrDataWithEmptyStringValue_thenConvertIntoKeyValuePairsWithNullValue() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "person1_first_name");
        valueMap.put("value", "");

        Map<String, Object> result = build(buildScannedValidationOcrData(valueMap));

        assertNull(result.get("person1_first_name"));
    }

    @Test
    public void givenValidationOcrDataWithNullKeyAndNullValue_thenConvertIntoKeyValuePairs() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", null);
        valueMap.put("value", null);

        Map<String, Object> result = build(buildScannedValidationOcrData(valueMap));

        assertEquals(0, result.size());
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
