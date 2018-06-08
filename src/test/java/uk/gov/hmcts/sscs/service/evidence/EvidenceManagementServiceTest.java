package uk.gov.hmcts.sscs.service.evidence;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;

public class EvidenceManagementServiceTest {

    public static final String SERVICE_AUTHORIZATION = "service-authorization";

    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private DocumentUploadClientApi documentUploadClientApi;

    private EvidenceManagementService evidenceManagementService;

    @Before
    public void setUp() {
        initMocks(this);
        evidenceManagementService = new EvidenceManagementService(authTokenGenerator, documentUploadClientApi);
    }

    @Test
    public void shouldCallUploadDocumentManagementClient() {

        List<MultipartFile> files = Collections.emptyList();

        UploadResponse expectedUploadResponse = mock(UploadResponse.class);

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION);
        when(documentUploadClientApi.upload(any(), eq(SERVICE_AUTHORIZATION), eq(files)))
                .thenReturn(expectedUploadResponse);

        UploadResponse actualUploadedResponse = evidenceManagementService.upload(files);

        verify(documentUploadClientApi, times(1))
                .upload(any(), eq(SERVICE_AUTHORIZATION), eq(files));

        assertThat(actualUploadedResponse, Matchers.equalTo(expectedUploadResponse));
    }

}
