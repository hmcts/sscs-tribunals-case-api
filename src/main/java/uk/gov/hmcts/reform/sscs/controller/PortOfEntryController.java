package uk.gov.hmcts.reform.sscs.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.UkPortOfEntry;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/port-of-entries")
public class PortOfEntryController {

    @GetMapping
    public List<Map<String, String>> getPortOfEntries() {
        return Arrays.stream(UkPortOfEntry.values())
            .map(enumVal -> Map.of(
                "label", enumVal.getLabel(),
                "trafficType", enumVal.getTrafficType(),
                "locationCode", enumVal.getLocationCode()))
            .collect(Collectors.toList());
    }
}
