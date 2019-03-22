package uk.gov.hmcts.reform.sscs.config;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class CitizenCcdClientTest {

    CitizenCcdClient citizenCcdClient;

    @Mock
    private CcdRequestDetails ccdRequestDetails;

    @Mock
    private CoreCaseDataApi coreCaseDataApi;

    @Mock
    private IdamTokens idamTockents;

    @Before
    public void setup() {
        initMocks(this);
        citizenCcdClient = new CitizenCcdClient(ccdRequestDetails, coreCaseDataApi);
        when(idamTockents.getIdamOauth2Token()).thenReturn("token");
        when(idamTockents.getServiceAuthorization()).thenReturn("s2s");
        when(idamTockents.getUserId()).thenReturn("1");
        when(ccdRequestDetails.getCaseTypeId()).thenReturn("Benefit");
        when(ccdRequestDetails.getJurisdictionId()).thenReturn("SSCS");
    }

    @Test
    public void shouldInvokeCoreCaseDataApi() {
        citizenCcdClient.startCaseForCitizen(idamTockents, "draft");
        citizenCcdClient.submitForCitizen(idamTockents, null);

        verify(coreCaseDataApi)
            .startForCitizen(eq("token"), eq("s2s"), eq("1"), eq("SSCS"), eq("Benefit"), eq("draft"));

        verify(coreCaseDataApi)
            .submitForCitizen(eq("token"), eq("s2s"), eq("1"), eq("SSCS"), eq("Benefit"), eq(true), isNull());
    }
}
