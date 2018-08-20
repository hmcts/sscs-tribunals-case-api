package uk.gov.hmcts.sscs.service.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
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

    public static final String EMPTY_EVENT = "emptyEvent";
    public static final String S2S_TOKEN = "s2s token";
    public static final String CASE_ID = "1520116198612015";
    public static final String APPEAL_NUMBER = "abcde12345";

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
    public void givenAnAppealNumber_shouldRetrieveCaseFromCcd() {
        //Given

        EventRequestData eventRequestData = EventRequestData.builder()
            .userId("user-id")
            .eventId(EMPTY_EVENT)
            .caseTypeId("case-type-id")
            .userToken("user-token")
            .jurisdictionId("jurisdiction-id")
            .ignoreWarning(true)
            .build();

        when(coreCaseDataApiMock.searchForCaseworker(
            eventRequestData.getUserToken(),
            S2S_TOKEN,
            eventRequestData.getUserId(),
            eventRequestData.getJurisdictionId(),
            eventRequestData.getCaseTypeId(),
            ImmutableMap.of("case.subscriptions.appellantSubscription.tya", APPEAL_NUMBER)
        )).thenReturn(CaseDataUtils.buildCaseDetailsList());

        when(coreCaseDataServiceMock.getEventRequestData(eq("emptyEvent"))).thenReturn(eventRequestData);
        when(coreCaseDataServiceMock.getCoreCaseDataApi()).thenReturn(coreCaseDataApiMock);

        when(idamServiceMock.generateServiceAuthorization()).thenReturn(S2S_TOKEN);

        //When
        CaseData caseData = readCoreCaseDataService.getCcdCaseDataByAppealNumber(APPEAL_NUMBER);

        //Then
        assertNotNull(caseData);
        verify(coreCaseDataApiMock).searchForCaseworker(anyString(), anyString(), anyString(), anyString(), anyString(),
            anyMap());
        assertEquals("AB 22 55 66 B", caseData.getAppeal().getAppellant().getIdentity().getNino());
    }
}
