package uk.gov.hmcts.reform.sscs.service;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface TotpAuthenticator {
    String issueOneTimePassword(String base32Key)
            throws InvalidKeyException, NoSuchAlgorithmException;

    boolean isOneTimePasswordValid(String base32Key, String token)
            throws InvalidKeyException, NoSuchAlgorithmException;
}
