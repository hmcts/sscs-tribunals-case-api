package uk.gov.hmcts.reform.sscs.bulkscan.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

@RunWith(MockitoJUnitRunner.class)
public class CaseManagementLocationServiceTest {

    public static final String BRADFORD = "Bradford";
    public static final String EPIMS_ID = "1234";
    @Mock
    private RefDataService refDataService;

    @Mock
    private VenueService venueService;

    private CaseManagementLocationService caseManagementLocationService;

    public void setupCaseManagementLocationService(boolean feature) {
        caseManagementLocationService = new CaseManagementLocationService(
            refDataService, venueService, feature);
    }

    @Test
    public void shouldNotRetrieveCaseManagementLocation_givenCaseAccessManagementFeatureIsDisabled() {
        setupCaseManagementLocationService(false);
        RegionalProcessingCenter regionalProcessingCentre = RegionalProcessingCenter.builder().build();

        Optional<CaseManagementLocation> caseManagementLocation =
            caseManagementLocationService.retrieveCaseManagementLocation("venue",
                regionalProcessingCentre);

        assertTrue(caseManagementLocation.isEmpty());
    }

    @Test
    public void shouldNotRetrieveCaseManagementLocation_givenBlankProcessingVenue() {
        setupCaseManagementLocationService(true);

        RegionalProcessingCenter regionalProcessingCentre = RegionalProcessingCenter.builder().build();

        Optional<CaseManagementLocation> caseManagementLocation =
            caseManagementLocationService.retrieveCaseManagementLocation("", regionalProcessingCentre);

        assertTrue(caseManagementLocation.isEmpty());
    }

    @Test
    public void shouldNotRetrieveCaseManagementLocation_givenBlankPostcode() {
        setupCaseManagementLocationService(true);

        Optional<CaseManagementLocation> caseManagementLocation =
            caseManagementLocationService.retrieveCaseManagementLocation("venue", null);

        assertTrue(caseManagementLocation.isEmpty());
    }

    @Test
    public void shouldNotRetrieveCaseManagementLocation_givenInvalidProcessingVenue() {
        setupCaseManagementLocationService(true);
        RegionalProcessingCenter regionalProcessingCentre = RegionalProcessingCenter.builder().build();

        when(venueService.getEpimsIdForVenue(BRADFORD)).thenReturn(EPIMS_ID);

        Optional<CaseManagementLocation> caseManagementLocation =
            caseManagementLocationService.retrieveCaseManagementLocation(BRADFORD,
                regionalProcessingCentre);

        assertTrue(caseManagementLocation.isEmpty());
    }

    @Test
    public void shouldNotRetrieveCaseManagementLocation_givenInvalidCourtVenue() {
        setupCaseManagementLocationService(true);
        RegionalProcessingCenter regionalProcessingCentre = RegionalProcessingCenter.builder().build();

        when(venueService.getEpimsIdForVenue(BRADFORD)).thenReturn(EPIMS_ID);
        when(refDataService.getCourtVenueRefDataByEpimsId(EPIMS_ID)).thenReturn(CourtVenue.builder().courtStatus("Open").build());

        Optional<CaseManagementLocation> caseManagementLocation =
            caseManagementLocationService.retrieveCaseManagementLocation(BRADFORD,
                regionalProcessingCentre);

        assertTrue(caseManagementLocation.isEmpty());
    }

    @Test
    public void shouldRetrieveCaseManagementLocation_givenValidProcessingVenue_andPostcode() {
        setupCaseManagementLocationService(true);
        RegionalProcessingCenter regionalProcessingCentre = RegionalProcessingCenter.builder().epimsId("rpcEpimsId").build();

        when(venueService.getEpimsIdForVenue(BRADFORD)).thenReturn(EPIMS_ID);
        when(refDataService.getCourtVenueRefDataByEpimsId(EPIMS_ID)).thenReturn(CourtVenue.builder()
            .regionId("regionId").courtStatus("Open").build());

        Optional<CaseManagementLocation> caseManagementLocation =
            caseManagementLocationService.retrieveCaseManagementLocation(BRADFORD, regionalProcessingCentre);

        assertTrue(caseManagementLocation.isPresent());
        CaseManagementLocation result = caseManagementLocation.get();
        assertEquals("rpcEpimsId", result.getBaseLocation());
        assertEquals("regionId", result.getRegion());
    }
}
