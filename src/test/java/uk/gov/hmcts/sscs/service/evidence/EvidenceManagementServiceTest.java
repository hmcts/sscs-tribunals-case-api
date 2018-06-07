package uk.gov.hmcts.sscs.service.evidence;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.sscs.service.idam.IdamService;

public class EvidenceManagementServiceTest {

    public static final String OAUTH_TOKEN = "oauth-token";
    public static final String SERVICE_AUTHORIZATION = "service-authorization";
    @Mock
    private IdamService idamService;

    @Mock
    private DocumentUploadClientApi documentUploadClientApi;

    @Mock
    private List<MultipartFile> multipartFileList;

    @Mock
    private UploadResponse uploadResponse;

    private EvidenceManagementService evidenceManagementService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        evidenceManagementService = new EvidenceManagementService(idamService, documentUploadClientApi);
    }

    @Test
    public void shouldCallUploadDocumentManagementClient() {

        when(idamService.generateServiceAuthorization()).thenReturn(SERVICE_AUTHORIZATION);
        when(idamService.getIdamOauth2Token()).thenReturn(OAUTH_TOKEN);
        when(documentUploadClientApi.upload(OAUTH_TOKEN, SERVICE_AUTHORIZATION, multipartFileList))
                .thenReturn(uploadResponse);

        UploadResponse actualUploadedResponse = evidenceManagementService.upload(multipartFileList);


        verify(documentUploadClientApi, times(1))
                .upload(OAUTH_TOKEN, SERVICE_AUTHORIZATION, multipartFileList);

        assertThat(actualUploadedResponse, Matchers.equalTo(uploadResponse));
    }
}