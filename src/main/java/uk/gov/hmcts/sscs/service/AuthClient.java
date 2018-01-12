package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.sscs.exception.AuthException;


@Service
public class AuthClient {
    private static final Logger LOG = getLogger(AuthClient.class);

    private String idamKey;
    private OtpGenerator otpGenerator;
    private String authApiUrl;
    private RestTemplate restTemplate;

    @Autowired
    AuthClient(@Value("${sscs.idam.key}") String idamKey,
               @Value("${auth.provider.service.api.url}") String authApiUrl,
               OtpGenerator otpGenerator, RestTemplate restTemplate) {
        this.idamKey = idamKey;
        this.otpGenerator = otpGenerator;
        this.authApiUrl = authApiUrl;
        this.restTemplate = restTemplate;
    }

    public String sendRequest(String path, HttpMethod httpMethod, String requestBody)
            throws AuthException {
        try {
            String otp = otpGenerator.issueOneTimePassword(idamKey);
            String authUrl = authApiUrl + path + "?microservice=sscs&oneTimePassword=" + otp;
            HttpEntity requestEntity = new HttpEntity(requestBody);
            ResponseEntity<String> responseEntity =
                    restTemplate.exchange(authUrl,httpMethod,requestEntity,String.class);
            return responseEntity.getBody();
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            LOG.error("Authentication error in auth client: ", e);
            throw new AuthException("Authorization error in auth client "
                    +
                    "while generating service key: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Error in auth client: ", e);
            throw new AuthException("Error while generating service key: "
                    + e.getMessage());
        }
    }
}
