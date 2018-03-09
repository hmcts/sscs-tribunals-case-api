package uk.gov.hmcts.sscs.service.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.sscs.ccd.properties.CoreCaseDataProperties;
import uk.gov.hmcts.sscs.ccd.properties.IdamProperties;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.idam.Authorize;
import uk.gov.hmcts.sscs.service.idam.IdamApiClient;

@RunWith(MockitoJUnitRunner.class)
public class ReadCoreCaseDataServiceTest {

    @Mock
    private CoreCaseDataApi coreCaseDataApiMock;
    private ReadCoreCaseDataService readCoreCaseDataService;
    @Mock
    private CoreCaseDataProperties coreCaseDataPropertiesMock;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private IdamApiClient idamApiClient;
    @Mock
    private IdamProperties idamProperties;

    @Before
    public void setUp() {
        readCoreCaseDataService = new ReadCoreCaseDataService(new CoreCaseDataService(coreCaseDataApiMock,
            coreCaseDataPropertiesMock, authTokenGenerator, idamApiClient, idamProperties));
    }

    @Test
    public void givenACase_shouldSaveItIntoCcd() {
        //Given
        mockCoreCaseDataProperties();
        mockCaseDetails();
        when(idamApiClient.authorizeCodeType(
            anyString(),
            anyString(),
            anyString(),
            anyString())
        ).thenReturn(new Authorize("url", "code", ""));

        when(idamApiClient.authorizeToken(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString())
        ).thenReturn(new Authorize("", "", "accessToken"));
        mockIdamProrperties();

        //When
        CaseData caseData = readCoreCaseDataService.getCcdCaseData("1520116198612015");

        //Then
        assertNotNull(caseData);
        assertEquals("AB 22 55 66 B", caseData.getAppeal().getAppellant().getIdentity().getNino());
    }

    private void mockIdamProrperties() {
        IdamProperties.Oauth2 oauth2 = mock(IdamProperties.Oauth2.class);
        when(idamProperties.getOauth2()).thenReturn(oauth2);

        IdamProperties.Oauth2.User user = mock(IdamProperties.Oauth2.User.class);
        when(oauth2.getUser()).thenReturn(user);

        IdamProperties.Oauth2.Client client = mock(IdamProperties.Oauth2.Client.class);
        when(oauth2.getClient()).thenReturn(client);

        when(user.getEmail()).thenReturn("email");
        when(user.getPassword()).thenReturn("password");
    }

    private void mockCaseDetails() {
        when(coreCaseDataApiMock.readForCaseWorker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString())).thenReturn(CaseDataUtils.buildCaseDetails());
    }

    private void mockCoreCaseDataProperties() {
        when(coreCaseDataPropertiesMock.getUserId()).thenReturn("userId");
        when(coreCaseDataPropertiesMock.getJurisdictionId()).thenReturn("jurisdictionId");
        when(coreCaseDataPropertiesMock.getCaseTypeId()).thenReturn("caseTypeId");
    }
}
