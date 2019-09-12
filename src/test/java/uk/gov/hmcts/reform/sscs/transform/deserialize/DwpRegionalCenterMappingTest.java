//package uk.gov.hmcts.reform.sscs.transform.deserialize;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertFalse;
//
//import java.util.Optional;
//import junitparams.JUnitParamsRunner;
//import junitparams.Parameters;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//@RunWith(JUnitParamsRunner.class)
//public class DwpRegionalCenterMappingTest {
//    @Test
//    public void givenANonExistingDwpIssuingOffice_shouldReturnEmptyOptional() {
//        Optional<String> currentResult = DwpRegionalCenterMapping.getDwpRegionForGivenDwpIssuingOfficeNum("11");
//        assertFalse(currentResult.isPresent());
//    }
//
//    @Test
//    @Parameters({"10,Newport", "6,Blackpool", "7,Blackpool", "2,Glasgow", "4,Glasgow"})
//    public void givenExistingDwpIssuingOffice_shouldReturnMappedRegionCenter(String issuingOfficeNum,
//                                                                             String expectedRegionCenter) {
//        Optional<String> currentResult = DwpRegionalCenterMapping.getDwpRegionForGivenDwpIssuingOfficeNum(issuingOfficeNum);
//        assertEquals(expectedRegionCenter, currentResult.get());
//    }
//}