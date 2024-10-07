package uk.gov.hmcts.reform.sscs.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.UkPortOfEntry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PortOfEntryControllerTest {

    private PortOfEntryController portOfEntryController;

    @BeforeEach
    void setUp() {
        portOfEntryController = new PortOfEntryController();
    }

    @Test
    void shouldReturnAllPortOfEntriesWithExpectedFields() {
        List<Map<String, String>> result = portOfEntryController.getPortOfEntries();

        assertNotNull(result);
        assertFalse(result.isEmpty(), "The result should not be empty");

        Map<String, String> aberdeenEntry = Map.of(
            "label", "Aberdeen",
            "trafficType", "Sea traffic",
            "locationCode", "GB000434"
        );

        assertTrue(result.contains(aberdeenEntry), "The result should contain the entry for Aberdeen");

        result.forEach(entry -> {
            assertTrue(entry.containsKey("label"), "Each entry should have a 'label' field");
            assertTrue(entry.containsKey("trafficType"), "Each entry should have a 'trafficType' field");
            assertTrue(entry.containsKey("locationCode"), "Each entry should have a 'locationCode' field");
        });
    }

    @Test
    void shouldMatchEnumValues() {
        List<Map<String, String>> result = portOfEntryController.getPortOfEntries();

        for (UkPortOfEntry entry : UkPortOfEntry.values()) {
            Map<String, String> expectedEntry = Map.of(
                "label", entry.getLabel(),
                "trafficType", entry.getTrafficType(),
                "locationCode", entry.getLocationCode()
            );
            assertTrue(result.contains(expectedEntry), "Result should contain the expected entry for " + entry.getLabel());
        }
    }
}
