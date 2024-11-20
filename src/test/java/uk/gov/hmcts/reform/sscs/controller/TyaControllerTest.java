package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.exception.DocumentNotFoundException;
import uk.gov.hmcts.reform.sscs.service.DocumentDownloadService;
import uk.gov.hmcts.reform.sscs.service.TribunalsService;


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
        openMocks(this);
        controller = new TyaController(tribunalsService, documentDownloadService);
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

    @Test
    public void testToThrowAppealNotFoundExceptionIfAppealNotFound() {
        assertThrows(AppealNotFoundException.class, () -> {
            //Given
            when(tribunalsService.findAppeal(CASE_ID, true)).thenThrow(
                new AppealNotFoundException(CASE_ID));

            //When
            controller.getAppealByCaseId(CASE_ID, true);
        });
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
    public void testToThrowDocumentNotFoundExceptionIfError() {
        assertThrows(DocumentNotFoundException.class, () -> {
            //Given
            when(documentDownloadService.downloadFile(URL)).thenThrow(
                new DocumentNotFoundException());

            //When
            controller.getAppealDocument(URL);
        });
    }

}
