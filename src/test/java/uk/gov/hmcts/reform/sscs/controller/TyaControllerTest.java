package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.exception.DocumentNotFoundException;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.DocumentDownloadService;
import uk.gov.hmcts.reform.sscs.service.TribunalsService;



public class TyaControllerTest {

    private static final Long CASE_ID = 123456789L;
    private static final String URL = "http://test";
    private static final String SERVICE_AUTH = "service-auth";
    private static final String SERVICE_NAME = "sscs";

    @Mock
    private TribunalsService tribunalsService;

    @Mock
    private DocumentDownloadService documentDownloadService;

    @Mock
    private AuthorisationService authorisationService;

    private TyaController controller;

    @Before
    public void setUp() {
        openMocks(this);
        controller = new TyaController(tribunalsService, documentDownloadService, authorisationService);
    }

    @Test
    public void testToReturnAppealForGivenCaseReference() throws CcdException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        //Given
        when(tribunalsService.findAppeal(CASE_ID, false)).thenReturn(node);

        //When
        ResponseEntity<String> receivedAppeal = controller.getAppealByCaseId(CASE_ID, false);

        //Then
        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedAppeal.getBody(), equalTo(node.toString()));
    }

    @Test
    public void testToReturnMyaAppealForGivenCaseReference() throws CcdException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        //Given
        when(tribunalsService.findAppeal(CASE_ID, true)).thenReturn(node);

        //When
        ResponseEntity<String> receivedAppeal = controller.getAppealByCaseId(CASE_ID, true);

        //Then
        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedAppeal.getBody(), equalTo(node.toString()));
    }

    @Test(expected = AppealNotFoundException.class)
    public void testToThrowAppealNotFoundExceptionIfAppealNotFound() throws CcdException {
        //Given
        when(tribunalsService.findAppeal(CASE_ID, true)).thenThrow(
            new AppealNotFoundException(CASE_ID));

        //When
        controller.getAppealByCaseId(CASE_ID, true);
    }

    @Test
    public void testToReturnResourceForDocumentUrl() throws CcdException {
        ResponseEntity<Resource> responseEntity = ResponseEntity.of(Optional.of(new ByteArrayResource(new byte[0])));
        //Given
        when(documentDownloadService.downloadFile(URL)).thenReturn(responseEntity);
        when(authorisationService.authenticate(SERVICE_AUTH)).thenReturn(SERVICE_NAME);

        //When
        ResponseEntity<Resource> receivedDocument = controller.getAppealDocument(SERVICE_AUTH, URL);

        //Then
        verify(authorisationService).authenticate(SERVICE_AUTH);
        verify(authorisationService).assertIsAllowedToHandleCallback(SERVICE_NAME);
        assertThat(receivedDocument.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedDocument.getBody(), instanceOf(ByteArrayResource.class));
    }

    @Test(expected = DocumentNotFoundException.class)
    public void testToThrowDocumentNotFoundExceptionIfError() throws CcdException {
        //Given
        when(documentDownloadService.downloadFile(URL)).thenThrow(
                new DocumentNotFoundException());

        //When
        controller.getAppealDocument(SERVICE_AUTH, URL);
    }

    @Test(expected = ForbiddenException.class)
    public void testToThrowForbiddenExceptionForUnauthorizedService() throws CcdException {
        //Given
        String serviceAuth = "unauthorized-service-auth";
        String serviceName = "unauthorized-service";
        when(authorisationService.authenticate(serviceAuth)).thenReturn(serviceName);
        doThrow(new ForbiddenException("Service " + serviceName + " does not have permissions to request case creation")).when(authorisationService).assertIsAllowedToHandleCallback(serviceName);

        //When
        controller.getAppealDocument(serviceAuth, URL);
    }

}
