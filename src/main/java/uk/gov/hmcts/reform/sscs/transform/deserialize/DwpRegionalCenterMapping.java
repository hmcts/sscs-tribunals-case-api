package uk.gov.hmcts.reform.sscs.transform.deserialize;

import java.util.Optional;
import java.util.stream.Stream;

public enum DwpRegionalCenterMapping {
    NEWCASTLE("Newcastle", "1"),
    GLASGOW("Glasgow", "2,4"),
    BELLEVALE("Bellevale", "3"),
    SPRINGBURN("Springburn", "5"),
    BLACKPOOL("Blackpool", "6,7,8,9"),
    NEWPORT("Newport", "10");


    private String dwpRegion;
    private String dwpIssuingOfficeNumber;

    DwpRegionalCenterMapping(String dwpRegion, String dwpIssuingOfficeNumber) {
        this.dwpRegion = dwpRegion;
        this.dwpIssuingOfficeNumber = dwpIssuingOfficeNumber;
    }

    public static Optional<String> getDwpRegionForGivenDwpIssuingOfficeNum(String issuingOfficeNum) {
        return Stream.of(DwpRegionalCenterMapping.values())
            .filter(dwp -> dwp.dwpIssuingOfficeNumber.contains(issuingOfficeNum))
            .map(dwp -> dwp.dwpRegion)
            .findFirst();
    }

}
