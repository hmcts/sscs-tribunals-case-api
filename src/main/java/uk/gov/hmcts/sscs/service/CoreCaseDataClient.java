package uk.gov.hmcts.sscs.service;

import org.apache.catalina.connector.Response;
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

import static org.slf4j.LoggerFactory.getLogger;

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

	public ResponseEntity<String> sendRequest(String userToken, String serviceToken, String path, HttpMethod httpMethod, String requestBody) {
		try {
			MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
			headers.add("Authorization", "Bearer " + userToken);
			headers.add("ServiceAuthorization", serviceToken);
			headers.add("Content-Type", "application/json");
			HttpEntity requestEntity = new HttpEntity(requestBody, headers);
			ResponseEntity<String> responseEntity = restTemplate.exchange(ccdApiUrl + path, httpMethod, requestEntity, String.class);
			return responseEntity;
		} catch (Exception ex) {
			LOG.error("Error while sending request to CCD api: ", ex);
			return null;
		}
	}
}
