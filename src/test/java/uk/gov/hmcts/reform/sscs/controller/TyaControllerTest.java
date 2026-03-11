package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.exception.DocumentNotFoundException;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.DocumentDownloadService;
import uk.gov.hmcts.reform.sscs.service.TribunalsService;



@ExtendWith(MockitoExtension.class)
public class TyaControllerTest {

    private static final Long CASE_ID = 123456789L;
    private static final String URL = "http://test";
    private static final String SERVICE_AUTH = "service-auth";

    @Mock
    private TribunalsService tribunalsService;

    @Mock
    private DocumentDownloadService documentDownloadService;

    @Mock
    private AuthorisationService authorisationService;

    private TyaController controller;

    @BeforeEach
    public void setUp() {
        controller = new TyaController(tribunalsService, documentDownloadService, authorisationService);
    }

    @Test
    public void testToReturnAppealForGivenCaseReference() throws CcdException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        when(tribunalsService.findAppeal(CASE_ID, false)).thenReturn(node);

        ResponseEntity<String> receivedAppeal = controller.getAppealByCaseId(CASE_ID, false);

        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedAppeal.getBody(), equalTo(node.toString()));
    }

    @Test
    public void testToReturnMyaAppealForGivenCaseReference() throws CcdException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        when(tribunalsService.findAppeal(CASE_ID, true)).thenReturn(node);

        ResponseEntity<String> receivedAppeal = controller.getAppealByCaseId(CASE_ID, true);

        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedAppeal.getBody(), equalTo(node.toString()));
    }

    @Test
    public void testToThrowAppealNotFoundExceptionIfAppealNotFound() throws CcdException {
        when(tribunalsService.findAppeal(CASE_ID, true)).thenThrow(new AppealNotFoundException(CASE_ID));

        assertThrows(AppealNotFoundException.class, () -> controller.getAppealByCaseId(CASE_ID, true));
    }

    @Test
    public void testToReturnResourceForDocumentUrl() throws CcdException {
        ResponseEntity<Resource> responseEntity = ResponseEntity.of(Optional.of(new ByteArrayResource(new byte[0])));
        when(documentDownloadService.downloadFile(URL)).thenReturn(responseEntity);

        ResponseEntity<Resource> receivedDocument = controller.getAppealDocument(SERVICE_AUTH, URL);

        verify(authorisationService).allowOnlySscs(SERVICE_AUTH);
        assertThat(receivedDocument.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedDocument.getBody(), instanceOf(ByteArrayResource.class));
    }

    @Test
    public void testToThrowDocumentNotFoundExceptionIfError() throws CcdException {
        when(documentDownloadService.downloadFile(URL)).thenThrow(new DocumentNotFoundException());
        assertThrows(DocumentNotFoundException.class, () -> controller.getAppealDocument(SERVICE_AUTH, URL));
    }

    @Test
    public void testToThrowForbiddenExceptionForUnauthorizedService() throws CcdException {
        String serviceAuth = "unauthorized-service-auth";
        String serviceName = "unauthorized-service";
        doThrow(new ForbiddenException("Service " + serviceName + "is not authorized for this action"))
                .when(authorisationService).allowOnlySscs(serviceAuth);

        assertThrows(ForbiddenException.class, () -> controller.getAppealDocument(serviceAuth, URL));
    }

}
