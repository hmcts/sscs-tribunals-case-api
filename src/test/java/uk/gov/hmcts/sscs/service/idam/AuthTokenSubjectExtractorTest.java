package uk.gov.hmcts.sscs.service.idam;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.exceptions.JwtDecodingException;

@RunWith(MockitoJUnitRunner.class)
public class AuthTokenSubjectExtractorTest {

    private AuthTokenSubjectExtractor authTokenSubjectExtractor;

    @Before
    public void setUp() {
        authTokenSubjectExtractor = new AuthTokenSubjectExtractor();
    }

    @Test
    public void shouldExtractSubjectFromJwt() {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1"
                       + "NiJ9.eyJzdWIiOiIxNiIsIm5hbWUiOiJ"
                       + "UZXN0IiwianRpIjoiMTIzNCIsImlhdCI"
                       + "6MTUyNjkyOTk1MiwiZXhwIjoxNTI2OTM"
                       + "zNTg5fQ.lZwrWNjG-y1Olo1qWocKIuq3"
                       + "_fdffVF8BTcR5l87FTg";

        assertThat(authTokenSubjectExtractor.extract(token), is("16"));
    }

    @Test
    public void shouldExtractSubjectFromJwtWithBearerType() {
        String token = "Bearer "
                       + "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1"
                       + "NiJ9.eyJzdWIiOiIxNiIsIm5hbWUiOiJ"
                       + "UZXN0IiwianRpIjoiMTIzNCIsImlhdCI"
                       + "6MTUyNjkyOTk1MiwiZXhwIjoxNTI2OTM"
                       + "zNTg5fQ.lZwrWNjG-y1Olo1qWocKIuq3"
                       + "_fdffVF8BTcR5l87FTg";

        assertThat(authTokenSubjectExtractor.extract(token), is("16"));
    }

    @Test(expected = JwtDecodingException.class)
    public void shouldThrowForMalformedJwt() {
        authTokenSubjectExtractor.extract("badgers");
    }

}
