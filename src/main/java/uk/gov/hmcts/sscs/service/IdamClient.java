package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
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
import uk.gov.hmcts.sscs.exception.IdamException;

@Service
public class IdamClient {
    private static final Logger LOG = getLogger(IdamClient.class);

    private String userId;
    private String idamApiUrl;
    private String role;
    private RestTemplate restTemplate;

    @Autowired
    IdamClient(@Value("${idam.user.id}") String userId,
               @Value("${idam.api}") String idamApiUrl,
               @Value("${idam.role}") String role,
               RestTemplate restTemplate) {
        this.userId = userId;
        this.idamApiUrl = idamApiUrl;
        this.role = role;
        this.restTemplate = restTemplate;
    }


    public String post(String path) throws IdamException {
        ResponseEntity<String> responseEntity;
        try {
            String authUrl = idamApiUrl + path;
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("id", userId);
            body.add("role", role);
            HttpEntity<MultiValueMap<String, String>> requestEntity
                    = new HttpEntity(body, new LinkedMultiValueMap<>());
            responseEntity = restTemplate.postForEntity(authUrl,requestEntity,String.class);
            return responseEntity.getBody();
        } catch (Exception e) {
            LOG.error("Error in auth client: ", e);
            throw new IdamException("Error in auth client: " + e.getMessage());
        }
    }
}
