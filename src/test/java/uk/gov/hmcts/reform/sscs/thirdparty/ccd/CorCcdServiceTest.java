package uk.gov.hmcts.reform.sscs.thirdparty.ccd;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CreateCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.ReadCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.SearchCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdClient;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class CorCcdServiceTest {

    private CitizenCcdClient ccdClient;
    private CorCcdService corCcdService;
    private IdamTokens idamTokens;
    private long caseId;

    @Before
    public void setUp() throws Exception {
        ccdClient = mock(CitizenCcdClient.class);
        IdamService idamService = mock(IdamService.class);
        String authToken = "authToken";
        String serviceAuthToken = "serviceAuthToken";
        String userId = "userId";
        idamTokens = IdamTokens.builder().idamOauth2Token(authToken).serviceAuthorization(serviceAuthToken).userId(userId).build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        String jurisdictionId = "jurisdictionId";
        String caseTypeId = "caseTypeId";
        CcdRequestDetails ccdRequestDetails = CcdRequestDetails.builder()
                .jurisdictionId(jurisdictionId)
                .caseTypeId(caseTypeId)
                .build();
        corCcdService = new CorCcdService(
                mock(CreateCcdCaseService.class),
                mock(SearchCcdCaseService.class),
                mock(UpdateCcdCaseService.class),
                mock(ReadCcdCaseService.class),
                ccdClient,
                idamService,
                ccdRequestDetails,
                mock(CoreCaseDataApi.class)
        );
        caseId = 123L;
    }

    @Test
    public void canAddUserToCase() {
        String userToAdd = "userToAdd";
        corCcdService.addUserToCase(userToAdd, caseId);

        verify(ccdClient).addUserToCase(idamTokens, userToAdd, caseId);
    }

    @Test
    public void canRemoveAUser() {
        String userToRemove = "userToRemove";
        corCcdService.removeUserFromCase(userToRemove, caseId);

        verify(ccdClient).removeUserFromCase(idamTokens, userToRemove, caseId);
    }

}
