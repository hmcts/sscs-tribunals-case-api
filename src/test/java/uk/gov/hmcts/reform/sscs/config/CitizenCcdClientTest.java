package uk.gov.hmcts.reform.sscs.config;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.CaseAccessApi;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class CitizenCcdClientTest {

    private CitizenCcdClient citizenCcdClient;

    @Mock
    private CcdRequestDetails ccdRequestDetails;

    @Mock
    private CoreCaseDataApi coreCaseDataApi;

    @Mock
    private CaseAccessApi caseAccessApi;

    @Mock
    private IdamTokens idamTokens;

    @Before
    public void setup() {
        openMocks(this);
        citizenCcdClient = new CitizenCcdClient(ccdRequestDetails, coreCaseDataApi, caseAccessApi, false);
        when(idamTokens.getIdamOauth2Token()).thenReturn("token");
        when(idamTokens.getServiceAuthorization()).thenReturn("s2s");
        when(idamTokens.getUserId()).thenReturn("1");
        when(ccdRequestDetails.getCaseTypeId()).thenReturn("Benefit");
        when(ccdRequestDetails.getJurisdictionId()).thenReturn("SSCS");
    }

    @Test
    public void shouldInvokeCoreCaseDataApiWhenCreatingADraft() {
        citizenCcdClient.startCaseForCitizen(idamTokens, "draft");
        citizenCcdClient.submitForCitizen(idamTokens, null);

        verify(coreCaseDataApi)
            .startForCitizen(eq("token"), eq("s2s"), eq("1"), eq("SSCS"), eq("Benefit"), eq("draft"));

        verify(coreCaseDataApi)
            .submitForCitizen(eq("token"), eq("s2s"), eq("1"), eq("SSCS"), eq("Benefit"), eq(true), isNull());
    }

    @Test
    public void shouldInvokeCoreCaseDataApiWhenUpdatingADraft() {
        String caseId = "1";
        citizenCcdClient.startEventForCitizen(idamTokens,  caseId, "draft");
        citizenCcdClient.submitEventForCitizen(idamTokens, caseId,null);

        verify(coreCaseDataApi)
                .startEventForCitizen(eq("token"), eq("s2s"), eq("1"), eq("SSCS"), eq("Benefit"), eq(caseId), eq("draft"));

        verify(coreCaseDataApi)
                .submitEventForCitizen(eq("token"), eq("s2s"), eq("1"), eq("SSCS"), eq("Benefit"), eq(caseId), eq(true), isNull());
    }

    @Test
    public void shouldInvokeCoreCaseDataApiWhenSearchingForADraftWhenElasticSearchDisabled() {
        citizenCcdClient.searchForCitizen(idamTokens);

        verify(coreCaseDataApi)
                .searchForCitizen(eq("token"), eq("s2s"), eq("1"), eq("SSCS"), eq("Benefit"), anyMap());

    }

    @Test
    public void shouldInvokeCoreCaseDataApiWhenSearchingForADraftWhenElasticSearchEnabled() {
        citizenCcdClient = new CitizenCcdClient(ccdRequestDetails, coreCaseDataApi, caseAccessApi, true);
        citizenCcdClient.searchForCitizen(idamTokens);


        verify(coreCaseDataApi)
                .searchCases(eq("token"), eq("s2s"), eq("Benefit"), anyString());

    }
}
