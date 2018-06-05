package uk.gov.hmcts.sscs.service.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@RunWith(MockitoJUnitRunner.class)
public class ReadCoreCaseDataServiceTest {

    @Mock
    private CoreCaseDataApi coreCaseDataApiMock;
    @Mock
    private IdamService idamServiceMock;
    @Mock
    private CoreCaseDataService coreCaseDataServiceMock;

    private ReadCoreCaseDataService readCoreCaseDataService;

    @Before
    public void setUp() {
        readCoreCaseDataService = new ReadCoreCaseDataService(
            coreCaseDataServiceMock, idamServiceMock
        );
    }

    @Test
    public void givenACaseId_shouldRetrieveCaseFromCcd() {
        //Given
        when(coreCaseDataApiMock.readForCaseWorker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString())).thenReturn(CaseDataUtils.buildCaseDetails());
        when(coreCaseDataServiceMock.getEventRequestData(eq("emptyEvent")))
                .thenReturn(EventRequestData.builder().build());
        when(idamServiceMock.generateServiceAuthorization())
                .thenReturn("s2s token");
        when(coreCaseDataServiceMock.getCoreCaseDataApi()).thenReturn(coreCaseDataApiMock);

        //When
        CaseData caseData = readCoreCaseDataService.getCcdCaseDataByCaseId("1520116198612015");

        //Then
        assertNotNull(caseData);
        verify(coreCaseDataApiMock).readForCaseWorker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString());
        assertEquals("AB 22 55 66 B", caseData.getAppeal().getAppellant().getIdentity().getNino());
    }

    @Test
    public void givenAnAppealNumber_shouldRetrieveCaseFromCcd() {
        //Given
        when(coreCaseDataApiMock.searchForCaseworker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyMap())).thenReturn(CaseDataUtils.buildCaseDetailsList());
        when(coreCaseDataServiceMock.getEventRequestData(eq("emptyEvent")))
                .thenReturn(EventRequestData.builder().build());
        when(idamServiceMock.generateServiceAuthorization())
                .thenReturn("s2s token");
        when(coreCaseDataServiceMock.getCoreCaseDataApi()).thenReturn(coreCaseDataApiMock);

        //When
        CaseData caseData = readCoreCaseDataService.getCcdCaseDataByAppealNumber("abcde12345");

        //Then
        assertNotNull(caseData);
        verify(coreCaseDataApiMock).searchForCaseworker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyMap());
        assertEquals("AB 22 55 66 B", caseData.getAppeal().getAppellant().getIdentity().getNino());
    }
}
