package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.exception.DocumentNotFoundException;
import uk.gov.hmcts.reform.sscs.service.DocumentDownloadService;
import uk.gov.hmcts.reform.sscs.service.TribunalsService;

@ExtendWith(MockitoExtension.class)
public class TyaControllerTest {

    private static final Long CASE_ID = 123456789L;
    private static final String URL = "http://test";

    @Mock
    private TribunalsService tribunalsService;
    @Mock
    private DocumentDownloadService documentDownloadService;

    private TyaController controller;

    @BeforeEach
    public void setUp() {
        controller = new TyaController(tribunalsService, documentDownloadService);
    }

    @Test
    public void testToReturnAppealForGivenCaseReference() throws CcdException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        //Given
        when(tribunalsService.findAppeal(CASE_ID, false)).thenReturn(node);

        //When
        ResponseEntity<String> receivedAppeal = controller.getAppealByCaseId(SERVICE_AUTH, CASE_ID, false);

        //Then
        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedAppeal.getBody(), equalTo(node.toString()));
        verify(authorisationService).allowOnlySscs(SERVICE_AUTH);
    }

    @Test
    public void testToReturnMyaAppealForGivenCaseReference() throws CcdException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        //Given
        when(tribunalsService.findAppeal(CASE_ID, true)).thenReturn(node);

        //When
        ResponseEntity<String> receivedAppeal = controller.getAppealByCaseId(SERVICE_AUTH, CASE_ID, true);

        //Then
        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedAppeal.getBody(), equalTo(node.toString()));
        verify(authorisationService).allowOnlySscs(SERVICE_AUTH);
    }

    @Test
    public void testToThrowAppealNotFoundExceptionIfAppealNotFound() throws CcdException {
        //Given
        when(tribunalsService.findAppeal(CASE_ID, true)).thenThrow(
            new AppealNotFoundException(CASE_ID));

        //When / Then
        assertThrows(AppealNotFoundException.class, () -> controller.getAppealByCaseId(SERVICE_AUTH, CASE_ID, true));
        verify(authorisationService).allowOnlySscs(SERVICE_AUTH);
    }

    @Test
    public void testToReturnResourceForDocumentUrl() throws CcdException {
        ResponseEntity<Resource> responseEntity = ResponseEntity.of(Optional.of(new ByteArrayResource(new byte[0])));
        //Given
        when(documentDownloadService.downloadFile(URL)).thenReturn(responseEntity);

        //When
        ResponseEntity<Resource> receivedDocument = controller.getAppealDocument(URL);

        //Then
        assertThat(receivedDocument.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedDocument.getBody(), instanceOf(ByteArrayResource.class));
    }

    @Test
    public void testToThrowDocumentNotFoundExceptionIfError() throws CcdException {
        //Given
        when(documentDownloadService.downloadFile(URL)).thenThrow(new DocumentNotFoundException());
        when(documentDownloadService.downloadFile(URL)).thenThrow(
                new DocumentNotFoundException());

        //When / Then
        assertThrows(DocumentNotFoundException.class, () -> controller.getAppealDocument(SERVICE_AUTH, URL));
    }

    @Test
    public void testToThrowForbiddenExceptionForUnauthorizedServiceForDocumentEndpoint() throws CcdException {
        //Given
        String serviceAuth = "unauthorized-service-auth";
        String serviceName = "unauthorized-service";
        when(authorisationService.authenticate(serviceAuth)).thenReturn(serviceName);
        doThrow(new ForbiddenException("Service " + serviceName + "is not authorized for this action"))
                .when(authorisationService).allowOnlySscs(serviceName);

        //When / Then
        assertThrows(DocumentNotFoundException.class, () -> controller.getAppealDocument(URL));
    }

    @Test
    public void testToThrowForbiddenExceptionForUnauthorizedServiceForAppealsEndpoint() throws CcdException {
        String serviceAuth = "unauthorized-service-auth";
        String serviceName = "unauthorized-service";
        when(authorisationService.authenticate(serviceAuth)).thenReturn(serviceName);
        doThrow(new ForbiddenException("Service " + serviceName + " is not authorized for this action"))
                .when(authorisationService).allowOnlySscs(serviceName);

        assertThrows(ForbiddenException.class, () -> controller.getAppealByCaseId(serviceAuth, CASE_ID, false));
    }

}
