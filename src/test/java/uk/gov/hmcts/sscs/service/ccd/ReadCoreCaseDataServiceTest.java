package uk.gov.hmcts.sscs.service.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;
import uk.gov.hmcts.sscs.model.ccd.CaseData;

@RunWith(MockitoJUnitRunner.class)
public class ReadCoreCaseDataServiceTest {

    @Mock
    private CoreCaseDataApi coreCaseDataApiMock;
    @Mock
    private CoreCaseDataService coreCaseDataServiceMock;

    private ReadCoreCaseDataService readCoreCaseDataService;

    @Before
    public void setUp() {
        readCoreCaseDataService = new ReadCoreCaseDataService(coreCaseDataServiceMock);
    }

    @Test
    public void givenACaseId_shouldReadFromCcd() {
        //Given
        mockCaseDetails();
        when(coreCaseDataServiceMock.getEventRequestData(eq("appealCreated")))
                .thenReturn(EventRequestData.builder().build());
        when(coreCaseDataServiceMock.generateServiceAuthorization())
                .thenReturn("s2s token");
        when(coreCaseDataServiceMock.getCoreCaseDataApi()).thenReturn(coreCaseDataApiMock);

        //When
        CaseData caseData = readCoreCaseDataService.getCcdCaseData("1520116198612015");

        //Then
        assertNotNull(caseData);
        assertEquals("AB 22 55 66 B", caseData.getAppeal().getAppellant().getIdentity().getNino());
    }

    private void mockCaseDetails() {
        when(coreCaseDataApiMock.readForCaseWorker(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString())).thenReturn(CaseDataUtils.buildCaseDetails());
    }
}
