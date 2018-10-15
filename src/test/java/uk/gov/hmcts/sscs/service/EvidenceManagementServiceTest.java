package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.exception.UnsupportedDocumentTypeException;

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

        assertEquals(actualUploadedResponse, expectedUploadResponse);
    }

    @Test(expected = UnsupportedDocumentTypeException.class)
    public void shouldThrowUnSupportedDocumentTypeExceptionIfAnyGivenDocumentTypeIsNotSupportedByDocumentStore() {
        List<MultipartFile> files = Collections.emptyList();

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION);
        when(documentUploadClientApi.upload(any(), eq(SERVICE_AUTHORIZATION), eq(files)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY));

        evidenceManagementService.upload(files);

    }

    @Test(expected = Exception.class)
    public void shouldRethrowAnyExceptionIfItsNotHttpClientErrorException() {
        List<MultipartFile> files = Collections.emptyList();

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION);
        when(documentUploadClientApi.upload(any(), eq(SERVICE_AUTHORIZATION), eq(files)))
                .thenThrow(new Exception("AppealNumber"));

        evidenceManagementService.upload(files);
    }
}
