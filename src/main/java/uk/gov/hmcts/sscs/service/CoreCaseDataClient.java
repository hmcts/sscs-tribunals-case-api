package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import uk.gov.hmcts.sscs.domain.corecase.CcdCaseResponse;
import uk.gov.hmcts.sscs.exception.CcdException;

@Service
public class CoreCaseDataClient {
    private static final Logger LOG = getLogger(CoreCaseDataClient.class);

    private static final String AUTHORIZATION = "Authorization";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";


    private String ccdApiUrl;
    private RestTemplate restTemplate;

    @Autowired
    CoreCaseDataClient(@Value("${ccd.service.api.url}") String ccdApiUrl, RestTemplate restTemplate) {
        this.ccdApiUrl = ccdApiUrl;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<Object> sendRequest(String userToken,
                                      String serviceToken,
                                      String path,
                                      HttpMethod httpMethod,
                                      Map<String,Object> requestBody) throws CcdException {
        try {
            HttpEntity requestEntity = new HttpEntity(requestBody, buildCcdHeader(userToken, serviceToken));
            String url = ccdApiUrl + path;
            return restTemplate.exchange(url,httpMethod,requestEntity,Object.class);
        } catch (Exception ex) {
            String errorText = "Error while sending request to CCD api: ";
            LOG.error(errorText, ex);
            throw new CcdException(errorText + ex.getMessage());
        }
    }

    public ResponseEntity<Object> post(String userToken,
                                              String serviceToken,
                                              String path,
                                              Map<String,Object> requestBody) throws CcdException {
        try {
            HttpEntity requestEntity = new HttpEntity(requestBody, buildCcdHeader(userToken, serviceToken));
            String url = ccdApiUrl + path;
            return restTemplate.postForEntity(url,requestEntity,Object.class);
        } catch (Exception ex) {
            String errorText = "Error while POSTing new case to CCD api: ";
            LOG.error(errorText, ex);
            throw new CcdException(errorText + ex.getMessage());
        }
    }

    public ResponseEntity<CcdCaseResponse> get(String userToken, String serviceToken, String path) throws CcdException {
        try {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", userToken);
            headers.add("ServiceAuthorization", serviceToken);
            headers.add("Content-Type", "application/json");
            HttpEntity requestEntity = new HttpEntity(headers);
            String url = ccdApiUrl + path;
            return restTemplate.exchange(url,HttpMethod.GET,requestEntity,CcdCaseResponse.class);
        } catch (Exception ex) {
            String errorText = "Error while getting a case from CCD api: ";
            LOG.error(errorText, ex);
            throw new CcdException(errorText + ex.getMessage());
        }
    }

    private MultiValueMap<String, String> buildCcdHeader(String userToken, String serviceToken) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(AUTHORIZATION, userToken);
        headers.add(SERVICE_AUTHORIZATION, serviceToken);
        headers.add(CONTENT_TYPE, APPLICATION_JSON);

        return headers;
    }

}
