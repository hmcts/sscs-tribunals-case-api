package uk.gov.hmcts.sscs.service.referencedata;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;

public class RegionalProcessingCenterServiceTest {

    public static final String SSCS_LIVERPOOL = "SSCS Liverpool";

    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Before
    public void setUp() throws Exception {
        regionalProcessingCenterService = new RegionalProcessingCenterService();
    }


    @Test
    public void givenVenuesCvsFile_shouldLoadSccodeToRpcMap() {
        //When
        regionalProcessingCenterService.init();

        //Then
        Map<String, String> sccodeRegionalProcessingCentermap
                = regionalProcessingCenterService.getSccodeRegionalProcessingCentermap();
        assertThat(sccodeRegionalProcessingCentermap.size(), equalTo(244));
        assertThat(sccodeRegionalProcessingCentermap.get("SC038"), equalTo("SSCS Birmingham"));
        assertThat(sccodeRegionalProcessingCentermap.get("SC001"), equalTo("SSCS Leeds"));
        assertThat(sccodeRegionalProcessingCentermap.get("SC293"), equalTo("SSCS Cardiff"));
    }


    @Test
    public void givenRpcMetaData_shouldLoadRpcMetadataToMap() {
        //When
        regionalProcessingCenterService.init();

        //Then
        Map<String, RegionalProcessingCenter> regionalProcessingCenterMap
                = regionalProcessingCenterService.getRegionalProcessingCenterMap();

        assertThat(regionalProcessingCenterMap.size(), equalTo(6));
        RegionalProcessingCenter regionalProcessingCenter = regionalProcessingCenterMap.get(SSCS_LIVERPOOL);
        assertThat(regionalProcessingCenter.getName(), equalTo("LIVERPOOL"));
        assertThat(regionalProcessingCenter.getAddress1(), equalTo("HM Courts & Tribunals Service"));
        assertThat(regionalProcessingCenter.getAddress2(), equalTo("Social Security & Child Support Appeals"));
        assertThat(regionalProcessingCenter.getAddress3(), equalTo("Prudential Buildings"));
        assertThat(regionalProcessingCenter.getAddress4(), equalTo("36 Dale Street"));
        assertThat(regionalProcessingCenter.getCity(), equalTo("LIVERPOOL"));
        assertThat(regionalProcessingCenter.getPostcode(), equalTo("L2 5UZ"));
        assertThat(regionalProcessingCenter.getPhoneNumber(), equalTo("0300 123 1142"));
        assertThat(regionalProcessingCenter.getFaxNumber(), equalTo("0870 324 0109"));

    }

    @Test
    public void shouldReturnRegionalProcessingCenterForGivenAppealReferenceNumber() {
        //Given
        String referenceNumber = "SC274/13/00010";
        regionalProcessingCenterService.init();

        //When
        RegionalProcessingCenter regionalProcessingCenter =
                regionalProcessingCenterService.getByScReferenceCode(referenceNumber);

        //Then
        assertThat(regionalProcessingCenter.getName(), equalTo("LIVERPOOL"));
        assertThat(regionalProcessingCenter.getAddress1(), equalTo("HM Courts & Tribunals Service"));
        assertThat(regionalProcessingCenter.getAddress2(), equalTo("Social Security & Child Support Appeals"));
        assertThat(regionalProcessingCenter.getAddress3(), equalTo("Prudential Buildings"));
        assertThat(regionalProcessingCenter.getAddress4(), equalTo("36 Dale Street"));
        assertThat(regionalProcessingCenter.getCity(), equalTo("LIVERPOOL"));
        assertThat(regionalProcessingCenter.getPostcode(), equalTo("L2 5UZ"));
        assertThat(regionalProcessingCenter.getPhoneNumber(), equalTo("0300 123 1142"));
        assertThat(regionalProcessingCenter.getFaxNumber(), equalTo("0870 324 0109"));


    }

    @Test
    public void shouldReturnBirminghamRegionalProcessingCenterAsDefault() {

        //Given
        String referenceNumber = "SC000/13/00010";
        regionalProcessingCenterService.init();

        //When
        RegionalProcessingCenter regionalProcessingCenter =
                regionalProcessingCenterService.getByScReferenceCode(referenceNumber);

        //Then
        assertThat(regionalProcessingCenter.getName(), equalTo("BIRMINGHAM"));
        assertThat(regionalProcessingCenter.getAddress1(), equalTo("HM Courts & Tribunals Service"));
        assertThat(regionalProcessingCenter.getAddress2(), equalTo("Social Security & Child Support Appeals"));
        assertThat(regionalProcessingCenter.getAddress3(), equalTo("Administrative Support Centre"));
        assertThat(regionalProcessingCenter.getAddress4(), equalTo("PO Box 14620"));
        assertThat(regionalProcessingCenter.getCity(), equalTo("BIRMINGHAM"));
        assertThat(regionalProcessingCenter.getPostcode(), equalTo("B16 6FR"));
        assertThat(regionalProcessingCenter.getPhoneNumber(), equalTo("0300 123 1142"));
        assertThat(regionalProcessingCenter.getFaxNumber(), equalTo("0126 434 7983"));

    }
}