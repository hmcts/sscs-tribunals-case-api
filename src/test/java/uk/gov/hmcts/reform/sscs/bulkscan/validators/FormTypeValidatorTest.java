package uk.gov.hmcts.reform.sscs.bulkscan.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.OcrDataBuilderTest.buildScannedValidationOcrData;

import com.networknt.schema.JsonSchema;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.OcrDataField;
import uk.gov.hmcts.reform.sscs.bulkscan.json.SscsJsonExtractor;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.domain.CaseResponse;

public class FormTypeValidatorTest {
    final SscsJsonExtractor sscsJsonExtractor = new SscsJsonExtractor();
    final FormTypeValidator validator = new FormTypeValidator(sscsJsonExtractor);
    private final SscsJsonExtractor mockExtractor = Mockito.mock(SscsJsonExtractor.class);

    @Test
    public void givenNewFieldsInV2OfTheForm_thenNoErrorsAreGiven() {
        Map<String, Object> pairs = new HashMap<>();
        pairs.put("is_benefit_type_pip", true);
        pairs.put("is_benefit_type_esa", false);
        pairs.put("is_benefit_type_uc", false);
        pairs.put("person1_email", "me@example.com");
        pairs.put("person1_want_sms_notifications", false);
        pairs.put("representative_email", "me@example.com");
        pairs.put("representative_mobile", "07770583222");
        pairs.put("representative_want_sms_notifications", true);

        @SuppressWarnings("unchecked")
        List<OcrDataField> scanOcrData = buildScannedValidationOcrData(pairs.entrySet().stream().map(f -> {
            HashMap<String, Object> valueMap = new HashMap<>();
            valueMap.put("name", f.getKey());
            valueMap.put("value", f.getValue());
            return valueMap;
        }).toArray(HashMap[]::new));

        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS1PEU.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertThat(response.getErrors()).isNull();
    }


    @Test
    public void givenAValidKeyValuePair_thenReturnAnEmptyCaseResponse() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "person1_first_name");
        valueMap.put("value", "Bob");

        List<OcrDataField> scanOcrData = buildScannedValidationOcrData(valueMap);
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS1.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertThat(response.getErrors()).isNull();
    }

    @Test
    public void givenAnInvalidKeyValuePair_thenReturnACaseResponseWithAnError() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "invalid_key");
        valueMap.put("value", "test");

        List<OcrDataField> scanOcrData = buildScannedValidationOcrData(valueMap);
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType("invalid_key").build();

        CaseResponse response = validator.validate("123456", exceptionRecord);
        assertThat("No valid form type was found. There needs to be a valid form_type on the OCR data or on the exception record.")
            .isEqualTo(response.getErrors().getFirst());
    }

    @Test
    public void givenMultipleInvalidKeyValuePairs_thenReturnACaseResponseWithMultipleErrors() {

        Map<String, Object> valueMap1 = new HashMap<>();
        Map<String, Object> valueMap2 = new HashMap<>();

        valueMap1.put("name", "invalid_key");
        valueMap1.put("value", "test");
        valueMap2.put("name", "invalid_key2");
        valueMap2.put("value", "test");

        List<OcrDataField> scanOcrData = buildScannedValidationOcrData(valueMap1, valueMap2);
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS1PE.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertThat(2).isEqualTo(response.getErrors().size());
        assertThat("$: property 'invalid_key' is not defined in the schema and the schema does not allow additional properties")
            .isEqualTo(response.getErrors().get(0));
        assertThat("$: property 'invalid_key2' is not defined in the schema and the schema does not allow additional properties")
            .isEqualTo(response.getErrors().get(1));
    }


    @Test
    public void givenValidChildSupportKeyValuePair_thenReturnAnEmptyCaseResponse() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "person1_child_maintenance_number");
        valueMap.put("value", "Test1234");

        List<OcrDataField> scanOcrData = buildScannedValidationOcrData(valueMap);
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS2.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertThat(response.getErrors()).isNull();
        assertThat(0).isEqualTo(response.getWarnings().size());
    }

    @Test
    public void givenAValidSscs2Fields_thenReturnValidCaseResponse() {
        Map<String, Object> pairs = getSscs2Map();

        @SuppressWarnings("unchecked")
        List<OcrDataField> scanOcrData = buildScannedValidationOcrData(pairs.entrySet().stream().map(f -> {
            HashMap<String, Object> valueMap = new HashMap<>();
            valueMap.put("name", f.getKey());
            valueMap.put("value", f.getValue());
            return valueMap;
        }).toArray(HashMap[]::new));

        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS2.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertThat(response.getErrors()).isNull();
        assertThat(0).isEqualTo(response.getWarnings().size());
    }

    @Test
    public void givenAppellantRoleKeyValuePair_thenReturnAnEmptyCaseResponse() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("is_paying_parent", "true");
        valueMap.put("is_receiving_parent", "false");
        valueMap.put("is_another_party", "false");
        valueMap.put("other_party_details", "Step Mother");

        List<OcrDataField> scanOcrData = buildScannedValidationOcrData(valueMap);

        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS2.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertThat(response.getErrors()).isNull();
        assertThat(0).isEqualTo(response.getWarnings().size());
    }

    @Test
    public void givenAValidBenefitTypesForSscs5_thenReturnValidCaseResponse() {
        Map<String, Object> pairs = getSscs5Map();

        @SuppressWarnings("unchecked")
        List<OcrDataField> scanOcrData = buildScannedValidationOcrData(pairs.entrySet().stream().map(f -> {
            HashMap<String, Object> valueMap = new HashMap<>();
            valueMap.put("name", f.getKey());
            valueMap.put("value", f.getValue());
            return valueMap;
        }).toArray(HashMap[]::new));

        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS5.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertThat(response.getErrors()).isNull();
        assertThat(0).isEqualTo(response.getWarnings().size());
    }

    @NotNull
    private static Map<String, Object> getSscs2Map() {
        Map<String, Object> pairs = new HashMap<>();
        pairs.put("is_paying_parent", true);
        pairs.put("is_receiving_parent", false);
        pairs.put("is_another_party", false);
        pairs.put("person1_child_maintenance_number", "123467");
        pairs.put("other_party_title", "Mr");
        pairs.put("other_party_first_name", "John");
        pairs.put("other_party_last_name", "Smith");
        pairs.put("is_other_party_address_known", "Yes");
        pairs.put("other_party_address_line1", "123 The Road");
        pairs.put("other_party_address_line2", "The Town");
        pairs.put("other_party_postcode", "RM1 1PT");
        return pairs;
    }

    @NotNull
    private static Map<String, Object> getSscs5Map() {
        Map<String, Object> pairs = new HashMap<>();
        pairs.put("is_benefit_type_tax_credit", true);
        pairs.put("is_benefit_type_guardians_allowance", false);
        pairs.put("is_benefit_type_tax_free_childcare", false);
        pairs.put("is_benefit_type_home_responsibilities_protection", false);
        pairs.put("is_benefit_type_child_benefit", false);
        pairs.put("is_benefit_type_30_hours_tax_free_childcare", false);
        pairs.put("is_benefit_type_guaranteed_minimum_pension", false);
        pairs.put("is_benefit_type_national_insurance_credits", false);
        return pairs;
    }

    @Test
    public void shouldReturnNullWhenSchemaFileIsMissing() {
        FormTypeValidator validator = new FormTypeValidator(mockExtractor);
        JsonSchema schema = invokeTryLoadSscsSchema(validator, "/nonexistent_schema.json");

        assertThat(schema).isNull();
    }

    @Test
    public void shouldReturnNullWhenSchemaFileIsMalformed() {
        FormTypeValidator validator = new FormTypeValidator(mockExtractor);
        JsonSchema schema = invokeTryLoadSscsSchema(validator, "/schema/malformed_schema.json");

        assertThat(schema).isNull();
    }

    private JsonSchema invokeTryLoadSscsSchema(FormTypeValidator validator, String path) {
        try {
            Method method = FormTypeValidator.class.getDeclaredMethod("tryLoadSscsSchema", String.class);
            method.setAccessible(true);
            return (JsonSchema) method.invoke(validator, path);
        } catch (Exception e) {
            throw new RuntimeException("Error invoking tryLoadSscsSchema", e);
        }
    }
}
