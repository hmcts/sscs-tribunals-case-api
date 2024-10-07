package uk.gov.hmcts.reform.sscs.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.CountryOfResidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.UkPortOfEntry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CountryOfResidenceControllerTest {

    private CountryOfResidenceController countryOfResidenceController;

    @BeforeEach
    void setUp() {
        countryOfResidenceController = new CountryOfResidenceController();
    }

    @Test
    void shouldReturnAllPortOfEntriesWithExpectedFields() {
        List<Map<String, String>> result = countryOfResidenceController.getCountryOfResidence();

        assertNotNull(result);
        assertFalse(result.isEmpty(), "The result should not be empty");

        Map<String, String> jordanEntry = Map.of(
            "label", "Jordan",
            "officialName", "The Hashemite Kingdom of Jordan"
        );

        assertTrue(result.contains(jordanEntry), "The result should contain the entry for Jordan");

        result.forEach(entry -> {
            assertTrue(entry.containsKey("label"), "Each entry should have a 'label' field");
            assertTrue(entry.containsKey("officialName"), "Each entry should have a 'officialName' field");
        });
    }

    @Test
    void shouldMatchEnumValues() {
        List<Map<String, String>> result = countryOfResidenceController.getCountryOfResidence();

        for (CountryOfResidence entry : CountryOfResidence.values()) {
            Map<String, String> expectedEntry = Map.of(
                "label", entry.getLabel(),
                "officialName", entry.getOfficialName()
            );
            assertTrue(result.contains(expectedEntry), "Result should contain the expected entry for " + entry.getLabel());
        }
    }
}
