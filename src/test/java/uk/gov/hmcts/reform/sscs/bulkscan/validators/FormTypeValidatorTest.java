package uk.gov.hmcts.reform.sscs.bulkscan.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.OcrDataBuilderTest.buildScannedValidationOcrData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.CaseResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.OcrDataField;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.bulkscan.json.SscsJsonExtractor;



public class FormTypeValidatorTest {


    SscsJsonExtractor sscsJsonExtractor = new SscsJsonExtractor();
    FormTypeValidator validator = new FormTypeValidator(sscsJsonExtractor);

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
        List scanOcrData = buildScannedValidationOcrData(pairs.entrySet().stream().map(f -> {
            HashMap<String, Object> valueMap = new HashMap<>();
            valueMap.put("name", f.getKey());
            valueMap.put("value", f.getValue());
            return valueMap;
        }).toArray(HashMap[]::new));

        @SuppressWarnings("unchecked")
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS1PEU.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertNull(response.getErrors());
    }


    @Test
    public void givenAValidKeyValuePair_thenReturnAnEmptyCaseResponse() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "person1_first_name");
        valueMap.put("value", "Bob");

        List<OcrDataField> scanOcrData = buildScannedValidationOcrData(valueMap);
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS1.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertNull(response.getErrors());
    }

    @Test
    public void givenAnInvalidKeyValuePair_thenReturnACaseResponseWithAnError() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "invalid_key");
        valueMap.put("value", "test");

        List<OcrDataField> scanOcrData = buildScannedValidationOcrData(valueMap);
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType("invalid_key").build();

        CaseResponse response = validator.validate("123456", exceptionRecord);
        assertEquals("No valid form type was found. There needs to be a valid form_type on the OCR data or on the exception record.", response.getErrors().get(0));
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
        assertEquals(2, response.getErrors().size());
        assertEquals("#: extraneous key [invalid_key] is not permitted", response.getErrors().get(0));
        assertEquals("#: extraneous key [invalid_key2] is not permitted", response.getErrors().get(1));
    }


    @Test
    public void givenValidChildSupportKeyValuePair_thenReturnAnEmptyCaseResponse() {
        Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("name", "person1_child_maintenance_number");
        valueMap.put("value", "Test1234");

        List<OcrDataField> scanOcrData = buildScannedValidationOcrData(valueMap);
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS2.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertNull(response.getErrors());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenAValidSscs2Fields_thenReturnValidCaseResponse() {
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

        @SuppressWarnings("unchecked")
        List scanOcrData = buildScannedValidationOcrData(pairs.entrySet().stream().map(f -> {
            HashMap<String, Object> valueMap = new HashMap<>();
            valueMap.put("name", f.getKey());
            valueMap.put("value", f.getValue());
            return valueMap;
        }).toArray(HashMap[]::new));

        @SuppressWarnings("unchecked")
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS2.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertNull(response.getErrors());
        assertEquals(0, response.getWarnings().size());
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
        assertNull(response.getErrors());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenAValidBenefitTypesForSscs5_thenReturnValidCaseResponse() {
        Map<String, Object> pairs = new HashMap<>();
        pairs.put("is_benefit_type_tax_credit", true);
        pairs.put("is_benefit_type_guardians_allowance", false);
        pairs.put("is_benefit_type_tax_free_childcare", false);
        pairs.put("is_benefit_type_home_responsibilities_protection", false);
        pairs.put("is_benefit_type_child_benefit", false);
        pairs.put("is_benefit_type_30_hours_tax_free_childcare", false);
        pairs.put("is_benefit_type_guaranteed_minimum_pension", false);
        pairs.put("is_benefit_type_national_insurance_credits", false);

        @SuppressWarnings("unchecked")
        List scanOcrData = buildScannedValidationOcrData(pairs.entrySet().stream().map(f -> {
            HashMap<String, Object> valueMap = new HashMap<>();
            valueMap.put("name", f.getKey());
            valueMap.put("value", f.getValue());
            return valueMap;
        }).toArray(HashMap[]::new));

        @SuppressWarnings("unchecked")
        ExceptionRecord exceptionRecord = ExceptionRecord.builder().ocrDataFields(scanOcrData).formType(FormType.SSCS5.toString()).build();

        CaseResponse response = validator.validate("caseId", exceptionRecord);
        assertNull(response.getErrors());
        assertEquals(0, response.getWarnings().size());
    }
}
