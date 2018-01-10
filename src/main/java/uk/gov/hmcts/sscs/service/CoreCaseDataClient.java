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

import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.exception.CcdException;

@Service
public class CoreCaseDataClient {
    private static final Logger LOG = getLogger(CoreCaseDataClient.class);

    private String ccdApiUrl;
    private RestTemplate restTemplate;

    @Autowired
    CoreCaseDataClient(@Value("${ccd.service.api}") String ccdApiUrl, RestTemplate restTemplate) {
        this.ccdApiUrl = ccdApiUrl;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<Object> sendRequest(String userToken,
                                      String serviceToken,
                                      String path,
                                      HttpMethod httpMethod,
                                              Map<String,Object> requestBody) throws CcdException {
        try {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", userToken);
            headers.add("ServiceAuthorization", serviceToken);
            headers.add("Content-Type", "application/json");
            HttpEntity requestEntity = new HttpEntity(requestBody, headers);
            String url = ccdApiUrl + path;
            ResponseEntity<Object> responseEntity =
                restTemplate.exchange(url,httpMethod,requestEntity,Object.class);
            return responseEntity;
        } catch (Exception ex) {
            LOG.error("Error while sending request to CCD api: ", ex);
            throw new CcdException("Error while sending request to CCD api: " + ex.getMessage());
        }
    }

    public ResponseEntity<Object> post(String userToken,
                                              String serviceToken,
                                              String path,
                                              Map<String,Object> requestBody) throws CcdException {
        try {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", userToken);
            headers.add("ServiceAuthorization", serviceToken);
            headers.add("Content-Type", "application/json");
            HttpEntity requestEntity = new HttpEntity(requestBody, headers);
            String url = ccdApiUrl + path;
            ResponseEntity<Object> responseEntity =
                    restTemplate.postForEntity(url,requestEntity,Object.class);
            return responseEntity;
        } catch (Exception ex) {
            LOG.error("Error while sending request to CCD api: ", ex);
            throw new CcdException("Error while POSTing new case to CCD api: " + ex.getMessage());
        }
    }

    public ResponseEntity<CcdCase> get(String userToken, String serviceToken, String path) throws CcdException {
        try {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", userToken);
            headers.add("ServiceAuthorization", serviceToken);
            headers.add("Content-Type", "application/json");
            String url = ccdApiUrl + path;
            System.out.println("####            Header Details    ############\n" + headers.toString());
            ResponseEntity<CcdCase> responseEntity = restTemplate.getForEntity(url, CcdCase.class);
            return responseEntity;
        } catch (Exception ex) {
            LOG.error("Error while getting a case from CCD api: ", ex);
            throw new CcdException("Error while getting a case from CCD api: " + ex.getMessage());
        }
    }

}
