package uk.gov.hmcts.reform.sscs.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Collections;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

public class IntegrationTestHelper {

    private IntegrationTestHelper() {

    }

    public static MockHttpServletRequestBuilder getRequestWithAuthHeader(String json, CallbackType callbackType, String pageId) {
        return getRequestWithAuthHeader(json, getEndpoint(callbackType) + "?pageId=" + pageId);
    }

    public static MockHttpServletRequestBuilder getRequestWithAuthHeader(String json, CallbackType callbackType) {
        return getRequestWithAuthHeader(json, getEndpoint(callbackType));
    }

    public static MockHttpServletRequestBuilder getRequestWithAuthHeader(String json, String endpoint) {

        return getRequestWithoutAuthHeader(json, endpoint)
            .header(HttpHeaders.AUTHORIZATION, "Bearer userToken")
            .header(AuthorisationService.SERVICE_AUTHORISATION_HEADER, "some-auth-header");
    }

    public static MockHttpServletRequestBuilder getRequestWithoutAuthHeader(String json, String endpoint) {

        return post(endpoint)
            .contentType(APPLICATION_JSON)
             .content(json);
    }

    public static void assertHttpStatus(HttpServletResponse response, HttpStatus status) {
        assertThat(response.getStatus()).isEqualTo(status.value());
    }

    public static UploadResponse createUploadResponse() {
        UploadResponse response = mock(UploadResponse.class);
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(response.getEmbedded()).thenReturn(embedded);
        Document document = createDocument();
        when(embedded.getDocuments()).thenReturn(Collections.singletonList(document));
        return response;
    }

    private static Document createDocument() {
        Document document = new Document();
        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = "some location";
        links.self = link;
        document.links = links;
        return document;
    }

    private static String getEndpoint(CallbackType callbackType) {
        switch (callbackType) {
            case MID_EVENT: return "/ccdMidEvent";
            case ABOUT_TO_START: return "/ccdAboutToStart";
            case ABOUT_TO_SUBMIT: return "/ccdAboutToSubmit";
            case SUBMITTED: return "/ccdSubmittedEvent";
            default:
                throw new IllegalStateException("Unexpected value: " + callbackType);
        }
    }
}
