package uk.gov.hmcts.reform.sscs.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.domain.CountryOfResidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.UkPortOfEntry;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/country-of-residences")
public class CountryOfResidenceController {

    @GetMapping
    public List<Map<String, String>> getCountryOfResidence() {
        return Arrays.stream(CountryOfResidence.values())
            .map(enumVal -> Map.of(
                "label", enumVal.getLabel(),
                "officialName", enumVal.getOfficialName()))
            .collect(Collectors.toList());
    }
}
