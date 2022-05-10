package uk.gov.hmcts.reform.sscs.functional.mya;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.client.methods.RequestBuilder.*;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.hmcts.reform.sscs.service.evidence.EvidenceUploadService.DM_STORE_USER_ID;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@Slf4j
public class SscsMyaBackendRequests {
    private final IdamTokens idamTokens;
    private final CitizenIdamService citizenIdamService;
    private String baseUrl;
    private CloseableHttpClient client;
    private EvidenceManagementService evidenceManagementService;


    public SscsMyaBackendRequests(IdamService idamService, CitizenIdamService citizenIdamService, String baseUrl, CloseableHttpClient client, EvidenceManagementService evidenceManagementService) {
        this.idamTokens = idamService.getIdamTokens();
        this.citizenIdamService = citizenIdamService;
        this.baseUrl = baseUrl;
        this.client = client;
        this.evidenceManagementService = evidenceManagementService;
    }

    public JSONArray getOnlineHearingForCitizen(String tya, String email) throws IOException {
        String uri = (StringUtils.isNotBlank(tya)) ? "/api/citizen/" + tya : "/api/citizen";
        HttpResponse getOnlineHearingResponse = getRequest(uri, email);

        // Retry if failed first time
        if (getOnlineHearingResponse.getStatusLine().getStatusCode() != HttpStatus.OK.value()) {
            getOnlineHearingResponse = getRequest(uri, email);
        }

        assertThat(getOnlineHearingResponse.getStatusLine().getStatusCode(), is(HttpStatus.OK.value()));

        String responseBody = EntityUtils.toString(getOnlineHearingResponse.getEntity());

        return new JSONArray(responseBody);
    }

    public JSONObject assignCaseToUser(String tya, String email, String postcode) throws IOException {
        StringEntity entity = new StringEntity("{\"email\":\"" + email + "\", \"postcode\":\"" + postcode + "\"}", APPLICATION_JSON);

        HttpResponse response = postRequest("/api/citizen/" + tya, entity, email);
        assertThat(response.getStatusLine().getStatusCode(), is(HttpStatus.OK.value()));

        String responseBody = EntityUtils.toString(response.getEntity());

        return new JSONObject(responseBody);
    }

    public void logUserWithCase(Long caseId) throws IOException {
        StringEntity entity = new StringEntity(EMPTY, APPLICATION_JSON);

        HttpResponse response = putRequest("/api/citizen/cases/" + caseId + "/log", entity);
        assertThat(response.getStatusLine().getStatusCode(), is(HttpStatus.NO_CONTENT.value()));
    }

    public CreatedCcdCase createOralCase(String emailAddress) throws IOException {
        HttpResponse createCaseResponse = client.execute(post(baseUrl + "/api/case?hearingType=oral&email=" + emailAddress)
                .setHeader("Content-Length", "0")
                .build());

        assertThat(createCaseResponse.getStatusLine().getStatusCode(), is(HttpStatus.CREATED.value()));

        String responseBody = EntityUtils.toString(createCaseResponse.getEntity());
        JSONObject jsonObject = new JSONObject(responseBody);
        System.out.println("Case id " + jsonObject.getString("id"));
        return new CreatedCcdCase(
                jsonObject.getString("id"),
                jsonObject.getString("appellant_tya"),
                jsonObject.getString("joint_party_tya"),
                jsonObject.getString("representative_tya")
        );
    }

    public void uploadHearingEvidence(String hearingId, String fileName) throws IOException {
        HttpEntity data = MultipartEntityBuilder.create()
                .setContentType(ContentType.MULTIPART_FORM_DATA)
                .addBinaryBody("file",
                        Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(fileName)),
                        ContentType.IMAGE_PNG,
                        fileName)
                .build();

        HttpResponse response = putRequest("/api/continuous-online-hearings/" + hearingId + "/evidence", data);
        assertThat(response.getStatusLine().getStatusCode(), is(HttpStatus.OK.value()));
    }

    public void deleteUploadEvidence(Long caseId, String evidenceId) throws IOException {
        HttpResponse response = client.execute(addHeaders(delete(baseUrl + "/api/continuous-online-hearings/" + caseId + "/evidence/" + evidenceId)).build());
        assertThat(response.getStatusLine().getStatusCode(), is(HttpStatus.NO_CONTENT.value()));
    }

    public void submitHearingEvidence(String hearingId, String description) throws IOException {
        HttpResponse response = postRequest("/api/continuous-online-hearings/" + hearingId + "/evidence",
                new StringEntity("{\n"
                        + "  \"body\": \"" + description + "\",\n"
                        + "  \"idamEmail\": \"mya-sscs-6920@mailinator.com\"\n"
                        + "}", APPLICATION_JSON)
        );
        assertThat(response.getStatusLine().getStatusCode(), is(HttpStatus.NO_CONTENT.value()));
    }

    public JSONArray getDraftHearingEvidence(String hearingId) throws IOException {
        HttpResponse response = getRequest("/api/continuous-online-hearings/" + hearingId + "/evidence");
        assertThat(response.getStatusLine().getStatusCode(), is(HttpStatus.OK.value()));

        String responseBody = EntityUtils.toString(response.getEntity());

        return new JSONArray(responseBody);
    }

    public void uploadAppellantStatement(String hearingId, String statement) throws IOException {
        String uri = "/api/continuous-online-hearings/" + hearingId + "/statement";
        String stringEntity = "{\n"
                + "  \"body\": \"statement\",\n"
                + "  \"tya\": \"Q9jE2FQuRR\"\n"
                + "}";
        HttpResponse getQuestionResponse = postRequest(uri, new StringEntity(stringEntity, APPLICATION_JSON));

        assertThat(getQuestionResponse.getStatusLine().getStatusCode(), is(HttpStatus.NO_CONTENT.value()));
    }

    public String getCoversheet(String caseId) throws IOException {
        CloseableHttpResponse getCoverSheetResponse = getRequest("/api/continuous-online-hearings/" + caseId + "/evidence/coversheet");

        assertThat(getCoverSheetResponse.getStatusLine().getStatusCode(), is(HttpStatus.OK.value()));
        Header fileNameHeader = getCoverSheetResponse.getFirstHeader("Content-Disposition");
        return ContentDisposition.parse(fileNameHeader.getValue()).getFilename();
    }


    public String updateSubscription(String appellantTya, String userEmail) throws IOException {
        HttpResponse response = postRequest(format("/appeals/%s/subscriptions/%s", appellantTya, appellantTya),
                new StringEntity(format("{ \"subscription\" : {\"email\" : \"%s\"}}", userEmail), APPLICATION_JSON));

        assertThat(response.getStatusLine().getStatusCode(), is(HttpStatus.OK.value()));
        return EntityUtils.toString(response.getEntity());
    }

    public void unsubscribeSubscription(String appellantTya, String userEmail) throws IOException {
        HttpResponse response = client.execute(addHeaders(delete(format("%s/appeals/%s/subscriptions/%s", baseUrl, appellantTya, appellantTya))).build());
        assertThat(response.getStatusLine().getStatusCode(), is(HttpStatus.OK.value()));
    }

    private RequestBuilder addHeaders(RequestBuilder requestBuilder) {
        return requestBuilder
                .setHeader(HttpHeaders.AUTHORIZATION, idamTokens.getIdamOauth2Token())
                .setHeader("ServiceAuthorization", idamTokens.getServiceAuthorization());
    }

    private RequestBuilder addHeaders(RequestBuilder requestBuilder, String email) {
        String userToken = citizenIdamService.getUserToken(email, "Apassword123");
        return requestBuilder
                .setHeader(HttpHeaders.AUTHORIZATION, userToken)
                .setHeader("ServiceAuthorization", idamTokens.getServiceAuthorization());
    }

    private CloseableHttpResponse getRequest(String url) throws IOException {
        return client.execute(addHeaders(get(baseUrl + url))
                .build());
    }

    private CloseableHttpResponse getRequest(String url, String email) throws IOException {
        return client.execute(addHeaders(get(baseUrl + url), email)
                .build());
    }

    private CloseableHttpResponse putRequest(String url, HttpEntity body) throws IOException {
        return client.execute(addHeaders(put(baseUrl + url))
                .setEntity(body)
                .build());
    }

    private CloseableHttpResponse postRequest(String url, HttpEntity body) throws IOException {
        return client.execute(addHeaders(post(baseUrl + url))
                .setEntity(body)
                .build());
    }

    private CloseableHttpResponse postRequest(String url, HttpEntity body, String email) throws IOException {
        return client.execute(addHeaders(post(baseUrl + url), email)
                .setEntity(body)
                .build());
    }

    public HttpResponse midEvent(HttpEntity body, String postfixUrl) throws IOException {
        return client.execute(addHeaders(post(format("%s/ccdMidEvent%s", baseUrl, postfixUrl))
            .setHeader("Accept", APPLICATION_JSON.toString())
            .setHeader("Content-type", APPLICATION_JSON.toString())).setEntity(body).build());
    }

    public byte[] toBytes(String documentUrl) {
        return evidenceManagementService.download(
                URI.create(documentUrl),
                DM_STORE_USER_ID
        );
    }
}
