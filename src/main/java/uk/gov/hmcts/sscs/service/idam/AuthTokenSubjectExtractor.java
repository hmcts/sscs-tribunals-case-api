package uk.gov.hmcts.sscs.service.idam;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.exceptions.JwtDecodingException;

@Service
public class AuthTokenSubjectExtractor {

    public String extract(String token) {
        try {

            DecodedJWT jwt = JWT.decode(
                token.replaceFirst("^Bearer\\s+", "")
            );

            return jwt.getSubject();

        } catch (JWTDecodeException e) {
            throw new JwtDecodingException("Auth Token cannot be decoded", e);
        }
    }

}
