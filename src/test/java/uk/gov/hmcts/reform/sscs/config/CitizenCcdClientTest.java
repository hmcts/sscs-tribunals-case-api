package uk.gov.hmcts.reform.sscs.config;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.CaseAccessApi;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
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

    @Test
    public void shouldInvokeCoreCaseDataApiWhenSearchingForCitizenAllCasesNonDormant() {
        CaseDetails caseDetails = CaseDetails.builder().id(123L).build();
        SearchResult searchResult = SearchResult.builder()
                .cases(List.of(caseDetails))
                .build();
        when(coreCaseDataApi.searchCases(eq("token"), eq("s2s"), eq("Benefit"), anyString()))
                .thenReturn(searchResult);

        List<CaseDetails> result = citizenCcdClient.searchForCitizenAllCasesNonDormant(idamTokens);

        assertEquals(List.of(caseDetails), result);
        verify(coreCaseDataApi)
                .searchCases(eq("token"), eq("s2s"), eq("Benefit"), anyString());
    }

    @Test
    public void shouldReturnEmptyListWhenSearchingForCitizenAllCasesNonDormantAndSearchResultIsNull() {
        when(coreCaseDataApi.searchCases(eq("token"), eq("s2s"), eq("Benefit"), anyString()))
                .thenReturn(null);

        List<CaseDetails> result = citizenCcdClient.searchForCitizenAllCasesNonDormant(idamTokens);

        assertEquals(emptyList(), result);
        verify(coreCaseDataApi)
                .searchCases(eq("token"), eq("s2s"), eq("Benefit"), anyString());
    }


}
