package uk.gov.hmcts.reform.sscs.bulkscan.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

public final class SscsOcrDataUtil {

    private static final String INVALID_YES_NO_ERROR_MESSAGE = " has an invalid value. Should be Yes/No or True/False";

    private SscsOcrDataUtil() {
    }

    public static Boolean hasPerson(Map<String, Object> pairs, String person) {

        return findBooleanExists(getField(pairs, person + "_title"), getField(pairs, person + "_first_name"),
            getField(pairs, person + "_last_name"), getField(pairs, person + "_address_line1"), getField(pairs, person + "_address_line2"),
            getField(pairs, person + "_address_line3"), getField(pairs, person + "_address_line4"), getField(pairs, person + "_postcode"),
            getField(pairs, person + "_dob"), getField(pairs, person + "_nino"), getField(pairs, person + "_company"), getField(pairs, person + "_phone"));
    }

    public static boolean findBooleanExists(String... values) {
        for (String v : values) {
            if (StringUtils.isNotBlank(v)) {
                return true;
            }
        }
        return false;
    }

    public static Boolean hasAddress(Map<String, Object> pairs, String person) {

        return findBooleanExists(getField(pairs, person + "_address_line1"), getField(pairs, person + "_address_line2"),
            getField(pairs, person + "_address_line3"), getField(pairs, person + "_address_line4"), getField(pairs, person + "_postcode"));
    }

    public static String getField(Map<String, Object> pairs, String field) {
        return pairs != null && pairs.containsKey(field) && pairs.get(field) != null ? StringUtils.trimToNull(pairs.get(field).toString()) : null;
    }

    public static boolean doValuesContradict(Map<String, Object> pairs, Set<String> errors, String value1, String value2) {
        if (pairs.get(value1).equals(pairs.get(value2))) {
            errors.add(value1 + " and " + value2 + " have contradicting values");
            return true;
        }
        return false;
    }

    /**
     * Returns true if and only if there are entries in pairs map for *all* the specified values that are
     * valid representations of booleans.
     * Adds an error for the first value specified that exists in pairs map that isn't a valid representation
     * of a boolean.
     */
    public static boolean areBooleansValid(Map<String, Object> pairs, Set<String> errors, String... values) {
        return Stream.of(values).allMatch(value -> checkBooleanValue(pairs, errors, value));
    }

    /**
     * Returns the subset of specified values for which there are entries in pairs map that are
     * valid representations of booleans.
     * Adds an error for the any value specified that exists in pairs map that isn't a valid representation
     * of a boolean
     */
    public static List<String> extractValuesWhereBooleansValid(Map<String, Object> pairs, Set<String> errors, List<String> values) {
        return values.stream().filter(value -> checkBooleanValue(pairs, errors, value)).collect(Collectors.toList());
    }

    public static boolean isExactlyZeroBooleanTrue(Map<String, Object> pairs, Set<String> errors, String... values) {
        return Stream.of(values)
            .noneMatch(value -> extractBooleanValue(pairs, errors, value));
    }

    public static boolean isExactlyOneBooleanTrue(Map<String, Object> pairs, Set<String> errors, String... values) {
        return Stream.of(values)
            .map(value -> extractBooleanValue(pairs, errors, value))
            .filter(Boolean::booleanValue).count() == 1;
    }

    public static boolean checkBooleanValue(Map<String, Object> pairs, Set<String> errors, String value) {
        if (pairs.get(value) != null) {
            boolean booleanValue = BooleanUtils.toBooleanObject(pairs.get(value).toString()) != null;
            if (booleanValue) {
                return true;
            } else {
                errors.add(value + INVALID_YES_NO_ERROR_MESSAGE);
            }
        }
        return false;
    }

    public static boolean extractBooleanValue(Map<String, Object> pairs, Set<String> errors, String value) {
        if (pairs.get(value) != null) {
            Boolean booleanValue = BooleanUtils.toBooleanObject(pairs.get(value).toString());
            if (booleanValue != null) {
                return booleanValue;
            } else {
                errors.add(value + INVALID_YES_NO_ERROR_MESSAGE);
            }
        }
        return false;
    }

    public static boolean extractBooleanValueWarning(Map<String, Object> pairs, List<String> warnings, String value) {
        if (pairs.get(value) != null) {
            Boolean booleanValue = BooleanUtils.toBooleanObject(pairs.get(value).toString());
            if (booleanValue != null) {
                return booleanValue;
            } else {
                warnings.add(value + INVALID_YES_NO_ERROR_MESSAGE);
            }
        }
        return false;
    }

    public static boolean getBoolean(Map<String, Object> pairs, Set<String> errors, String value) {
        return checkBooleanValue(pairs, errors, value) && BooleanUtils.toBoolean(pairs.get(value).toString());
    }

    public static String convertBooleanToYesNoString(boolean value) {
        return convertBooleanToYesNo(value).getValue();
    }

    public static YesNo convertBooleanToYesNo(boolean value) {
        return value ? YesNo.YES : YesNo.NO;
    }

    public static String generateDateForCcd(Map<String, Object> pairs, Set<String> errors, String fieldName) {
        if (pairs.containsKey(fieldName)) {
            return getDateForCcd(getField(pairs, fieldName), errors, fieldName + " is an invalid date field. Needs to be a valid date and in the format dd/mm/yyyy");
        }
        return null;
    }

    public static String getDateForCcd(String ocrField, Set<String> errors, String errorMessage) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[d/M/uuuu][ddMMuuuu]")
            .withResolverStyle(ResolverStyle.STRICT);

        if (!StringUtils.isEmpty(ocrField)) {
            try {
                return LocalDate.parse(ocrField, formatter).format(DateTimeFormatter.ofPattern("uuuu-MM-dd"));
            } catch (DateTimeParseException ex) {
                errors.add(errorMessage);
            }
        }
        return null;
    }

}
