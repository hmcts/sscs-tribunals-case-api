package uk.gov.hmcts.reform.sscs.bulkscan.util;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPELLANT_ADDRESS_LINE1;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPELLANT_ADDRESS_LINE2;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPELLANT_ADDRESS_LINE3;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPELLANT_ADDRESS_LINE4;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPELLANT_DATE_OF_BIRTH;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPELLANT_FIRST_NAME;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPELLANT_LAST_NAME;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPELLANT_NINO;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPELLANT_PHONE;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPELLANT_POSTCODE;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPELLANT_TITLE;
import static uk.gov.hmcts.reform.sscs.bulkscan.TestDataConstants.APPOINTEE_COMPANY;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.areBooleansValid;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.convertBooleanToYesNoString;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.doValuesContradict;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.extractBooleanValue;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.extractValuesWhereBooleansValid;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.findBooleanExists;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.generateDateForCcd;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.getField;
import static uk.gov.hmcts.reform.sscs.bulkscan.util.SscsOcrDataUtil.hasPerson;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class SscsOcrDataUtilTest {

    final Map<String, Object> pairs = new HashMap<>();
    final Set<String> errors = new HashSet<>();

    @Test
    public void givenAPersonExists_thenReturnTrue() {
        pairs.put("person1_title", APPELLANT_TITLE);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenAPersonDoesNotExist_thenReturnTrue() {
        pairs.put("person1_title", APPELLANT_TITLE);
        assertFalse(hasPerson(pairs, "person2"));
    }

    @Test
    public void givenAPersonFirstNameExists_thenReturnTrue() {
        pairs.put("person1_first_name", APPELLANT_FIRST_NAME);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenAPersonLastNameExists_thenReturnTrue() {
        pairs.put("person1_last_name", APPELLANT_LAST_NAME);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenAPersonAddressLine1Exists_thenReturnTrue() {
        pairs.put("person1_address_line1", APPELLANT_ADDRESS_LINE1);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenAPersonAddressLine2Exists_thenReturnTrue() {
        pairs.put("person1_address_line2", APPELLANT_ADDRESS_LINE2);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenAPersonAddressLine3Exists_thenReturnTrue() {
        pairs.put("person1_address_line3", APPELLANT_ADDRESS_LINE3);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenAPersonAddressLine4Exists_thenReturnTrue() {
        pairs.put("person1_address_line4", APPELLANT_ADDRESS_LINE4);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenAPersonAddressPostcodeExists_thenReturnTrue() {
        pairs.put("person1_postcode", APPELLANT_POSTCODE);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenAPersonDateOfBirthExists_thenReturnTrue() {
        pairs.put("person1_dob", APPELLANT_DATE_OF_BIRTH);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenAPersonNinoExists_thenReturnTrue() {
        pairs.put("person1_nino", APPELLANT_NINO);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenAPersonCompanyExists_thenReturnTrue() {
        pairs.put("person1_company", APPOINTEE_COMPANY);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenAPersonPhoneExists_thenReturnTrue() {
        pairs.put("person1_phone", APPELLANT_PHONE);
        assertTrue(hasPerson(pairs, "person1"));
    }

    @Test
    public void givenBooleanExists_theReturnTrue() {
        assertTrue(findBooleanExists("test"));
    }

    @Test
    public void givenKeyIsNull_theReturnFalse() {
        pairs.put("test_check_null", null);
        assertFalse(findBooleanExists(getField(pairs,"test_check_null")));
    }

    @Test
    public void givenKeyIsBlank_theReturnFalse() {
        pairs.put("test_check_blank", "");
        assertFalse(findBooleanExists(getField(pairs,"test_check_blank")));
    }

    @Test
    public void givenKeyIsExists_theReturnTrue() {
        pairs.put("test_check", "test");
        assertTrue(findBooleanExists(getField(pairs,"test_check")));
    }

    @Test
    public void givenAMap_thenFindField() {
        pairs.put("person1_title", APPELLANT_TITLE);

        assertEquals(APPELLANT_TITLE, SscsOcrDataUtil.getField(pairs, "person1_title"));
    }

    @Test
    public void givenAMap_thenFindTrimField() {
        pairs.put("office", " PIP 1 ");

        assertEquals("PIP 1", SscsOcrDataUtil.getField(pairs, "office"));
    }

    @Test
    public void givenAFieldWithEmptyValue_thenReturnNull() {
        pairs.put("person1_title", null);

        assertNull(SscsOcrDataUtil.getField(pairs, "person1_title"));
    }

    @Test
    public void givenAMapWhichDoesNotContainField_thenReturnNull() {
        assertNull(SscsOcrDataUtil.getField(pairs, "test"));
    }

    @Test
    public void givenTwoBooleansContradict_thenReturnTrue() {
        pairs.put("hearing_type_oral", true);
        pairs.put("hearing_type_paper", true);

        assertTrue(doValuesContradict(pairs, new HashSet<>(), "hearing_type_oral", "hearing_type_paper"));
    }

    @Test
    public void givenTwoBooleansNotDefined_thenAreBooleansValid_ShouldReturnFalse_WithNoErrors() {
        assertFalse(areBooleansValid(pairs, errors, "hearing_type_oral", "hearing_type_paper"));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void givenOneBooleanNotDefinedAndOneBooleanValid_then_AreBooleansValid_ShouldReturnFalse_WithNoErrors() {
        pairs.put("hearing_type_oral", "No");
        assertFalse(areBooleansValid(pairs, errors, "hearing_type_oral", "hearing_type_paper"));
        assertTrue(errors.isEmpty());

    }

    @Test
    public void givenOneBooleanNotDefinedAndOneBooleanInvalid_then_AreBooleansValid_ShouldReturnFalse_WithOneError() {
        pairs.put("hearing_type_oral", "blah");
        assertFalse(areBooleansValid(pairs, errors, "hearing_type_oral", "hearing_type_paper"));
        assertEquals(1, errors.size());
        assertEquals("hearing_type_oral has an invalid value. Should be Yes/No or True/False", errors.iterator().next());
    }

    @Test
    public void givenTwoBooleansDefinedWithOneBooleanInvalid_then_AreBooleansValid_ShouldReturnFalse_WithOneError() {
        pairs.put("hearing_type_oral", "No");
        pairs.put("hearing_type_paper", "blah");
        assertFalse(areBooleansValid(pairs, errors, "hearing_type_oral", "hearing_type_paper"));
        assertEquals(1, errors.size());
    }

    @Test
    public void givenTwoBooleansDefinedWithBothBooleansInvalid_then_AreBooleansValid_ShouldReturnFalse_WithTwoErrors() {
        pairs.put("hearing_type_oral", "blah");
        pairs.put("hearing_type_paper", "blah");
        assertFalse(areBooleansValid(pairs, errors, "hearing_type_oral", "hearing_type_paper"));
        assertEquals(1, errors.size());
        assertEquals("hearing_type_oral has an invalid value. Should be Yes/No or True/False", errors.iterator().next());
    }

    @Test
    public void givenTwoBooleansDefinedWithBothBooleansNo_then_AreBooleansValid_ShouldReturnTrue_WithNoErrors() {
        pairs.put("hearing_type_oral", "no");
        pairs.put("hearing_type_paper", "no");
        assertTrue(areBooleansValid(pairs, errors, "hearing_type_oral", "hearing_type_paper"));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void givenTwoBooleansDefinedWithBothBooleansYes_then_AreBooleansValid_ShouldReturnTrue_WithNoErrors() {
        pairs.put("hearing_type_oral", "yes");
        pairs.put("hearing_type_paper", "yes");
        assertTrue(areBooleansValid(pairs, errors, "hearing_type_oral", "hearing_type_paper"));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void givenTwoBooleansNotDefined_then_extractValuesWhereBooleansValidBooleans_ShouldEmptyList_WithNoErrors() {
        List<String> extracted = extractValuesWhereBooleansValid(pairs, errors, Arrays.asList("hearing_type_oral", "hearing_type_paper"));
        assertTrue(extracted.isEmpty());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void givenOneBooleanNotDefinedAndOneBooleanValid_then_extractValuesWhereBooleansValidBooleans_ShouldReturnSingletonList_WithNoErrors() {
        pairs.put("hearing_type_oral", "No");
        List<String> extracted = extractValuesWhereBooleansValid(pairs, errors, Arrays.asList("hearing_type_oral", "hearing_type_paper"));
        assertEquals(1, extracted.size());
        assertEquals(List.of("hearing_type_oral"), extracted);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void givenOneBooleanNotDefinedAndOneBooleanInvalid_then_extractValuesWhereBooleansValidBooleans_ShouldReturnEmptyList_WithOneError() {
        pairs.put("hearing_type_oral", "blah");
        List<String> extracted = extractValuesWhereBooleansValid(pairs, errors, Arrays.asList("hearing_type_oral", "hearing_type_paper"));
        assertTrue(extracted.isEmpty());
        assertEquals(1, errors.size());
        assertEquals("hearing_type_oral has an invalid value. Should be Yes/No or True/False", errors.iterator().next());
    }

    @Test
    public void givenTwoBooleansDefinedWithOneBooleanInvalid_then_extractValuesWhereBooleansValidBooleans_ShouldReturnSingletonList_WithOneError() {
        pairs.put("hearing_type_oral", "No");
        pairs.put("hearing_type_paper", "blah");
        List<String> extracted = extractValuesWhereBooleansValid(pairs, errors, Arrays.asList("hearing_type_oral", "hearing_type_paper"));
        assertEquals(1, extracted.size());
        assertEquals(List.of("hearing_type_oral"), extracted);
        assertEquals(1, errors.size());
        assertEquals("hearing_type_paper has an invalid value. Should be Yes/No or True/False", errors.iterator().next());
    }

    @Test
    public void givenTwoBooleansDefinedWithBothBooleansInvalid_then_extractValuesWhereBooleansValidBooleans_ShouldReturnEmptyList_WithTwoErrors() {
        pairs.put("hearing_type_oral", "blah");
        pairs.put("hearing_type_paper", "blah");
        List<String> extracted = extractValuesWhereBooleansValid(pairs, errors, Arrays.asList("hearing_type_oral", "hearing_type_paper"));
        assertTrue(extracted.isEmpty());
        assertEquals(2, errors.size());
        Iterator<String> errorsIterator = errors.iterator();
        assertEquals("hearing_type_oral has an invalid value. Should be Yes/No or True/False", errorsIterator.next());
        assertEquals("hearing_type_paper has an invalid value. Should be Yes/No or True/False", errorsIterator.next());
    }

    @Test
    public void givenTwoBooleansDefinedWithBothBooleansNo_then_extractValuesWhereBooleansValidBooleans_ShouldReturnBothValues_WithNoErrors() {
        pairs.put("hearing_type_oral", "no");
        pairs.put("hearing_type_paper", "no");
        List<String> extracted = extractValuesWhereBooleansValid(pairs, errors, Arrays.asList("hearing_type_oral", "hearing_type_paper"));
        assertEquals(2, extracted.size());
        assertEquals(Arrays.asList("hearing_type_oral", "hearing_type_paper"), extracted);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void givenTwoBooleansDefinedWithBothBooleansYes_then_extractValuesWhereBooleansValidBooleans_ShouldReturnBothValues_WithNoErrors() {
        pairs.put("hearing_type_oral", "yes");
        pairs.put("hearing_type_paper", "yes");
        List<String> extracted = extractValuesWhereBooleansValid(pairs, errors, Arrays.asList("hearing_type_oral", "hearing_type_paper"));
        assertEquals(2, extracted.size());
        assertEquals(Arrays.asList("hearing_type_oral", "hearing_type_paper"), extracted);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void givenABooleanDefinedAsYes_then_extractBooleanValue_ShouldExtractTrueValue_WithNoErrors() {
        pairs.put("hearing_type_oral", "yes");
        boolean extractedValue = extractBooleanValue(pairs, errors, "hearing_type_oral");
        assertTrue(extractedValue);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void givenABooleanDefinedAsNo_then_extractBooleanValue_ShouldExtractFalseValue_WithNoErrors() {
        pairs.put("hearing_type_oral", "no");
        boolean extractedValue = extractBooleanValue(pairs, errors, "hearing_type_oral");
        assertFalse(extractedValue);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void givenABooleanDefinedIncorrectly_then_extractBooleanValue_ShouldExtractFalseValue_WithOneErrors() {
        pairs.put("hearing_type_oral", "incorrect");
        boolean extractedValue = extractBooleanValue(pairs, errors, "hearing_type_oral");
        assertFalse(extractedValue);
        assertEquals(1, errors.size());
        Iterator<String> errorsIterator = errors.iterator();
        assertEquals("hearing_type_oral has an invalid value. Should be Yes/No or True/False", errorsIterator.next());
    }

    @Test
    public void givenAnUndefinedBoolean_then_extractBooleanValue_ShouldExtractFalseValue_WithNoErrors() {
        boolean extractedValue = extractBooleanValue(pairs, errors, "hearing_type_oral");
        assertFalse(extractedValue);
        assertTrue(errors.isEmpty());
    }


    @Test
    public void givenTwoBooleansDoNotContradict_thenReturnFalse() {
        pairs.put("hearing_type_oral", true);
        pairs.put("hearing_type_paper", false);

        assertFalse(doValuesContradict(pairs, new HashSet<>(), "hearing_type_oral", "hearing_type_paper"));
    }

    @Test
    public void givenAValidBooleanValue_thenReturnTrue() {
        pairs.put("hearing_type_oral", true);

        assertTrue(areBooleansValid(pairs, errors, "hearing_type_oral"));
    }

    @Test
    public void givenMultipleBooleanValues_thenReturnTrue() {
        pairs.put("hearing_type_oral", true);
        pairs.put("hearing_type_paper", false);

        assertTrue(areBooleansValid(pairs, errors, "hearing_type_oral", "hearing_type_paper"));
    }

    @Test
    public void givenABooleanValueWithText_thenReturnFalse() {
        pairs.put("hearing_type_oral", "blue");

        assertFalse(areBooleansValid(pairs, errors, "hearing_type_oral"));
    }

    @Test
    public void givenTrue_thenReturnYes() {
        assertEquals("Yes", convertBooleanToYesNoString(true));
    }

    @Test
    public void givenFalse_thenReturnNo() {
        assertEquals("No", convertBooleanToYesNoString(false));
    }

    @Test
    public void givenAnOcrDate_thenConvertToCcdDateFormat() {
        pairs.put("hearingDate", "01/01/2018");

        assertEquals("2018-01-01", generateDateForCcd(pairs, errors, "hearingDate"));
    }

    @Test
    public void givenAnOcrDateWithNoLeadingZero_thenConvertToCcdDateFormat() {
        pairs.put("hearingDate", "1/1/2018");

        assertEquals("2018-01-01", generateDateForCcd(pairs, errors, "hearingDate"));
    }

    @Test
    public void givenAnOcrDateWithInvalidFormat_thenAddError() {
        pairs.put("hearingDate", "01/30/2018");

        generateDateForCcd(pairs, errors, "hearingDate");

        assertEquals("hearingDate is an invalid date field. Needs to be a valid date and in the format dd/mm/yyyy", errors.iterator().next());
    }

    @Test
    public void givenAnOcrInvalidDate_thenAddError() {
        pairs.put("hearingDate", "29/02/2018");

        generateDateForCcd(pairs, errors, "hearingDate");

        assertEquals("hearingDate is an invalid date field. Needs to be a valid date and in the format dd/mm/yyyy", errors.iterator().next());
    }

    @Test
    public void givenAnInvalidBooleanValue_thenAddError() {
        pairs.put("hearing_options_hearing_loop", "Yrs");

        areBooleansValid(pairs, errors, "hearing_options_hearing_loop");

        assertEquals("hearing_options_hearing_loop has an invalid value. Should be Yes/No or True/False", errors.iterator().next());
    }


}
