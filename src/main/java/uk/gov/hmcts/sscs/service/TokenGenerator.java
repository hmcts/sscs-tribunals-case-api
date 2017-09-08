package uk.gov.hmcts.sscs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.auth.provider.service.token.CachedServiceTokenGenerator;

@Service
public class TokenGenerator {
	private final CachedServiceTokenGenerator cachedServiceTokenGenerator;

	@Autowired
	public TokenGenerator(CachedServiceTokenGenerator cachedServiceTokenGenerator){
		this.cachedServiceTokenGenerator = cachedServiceTokenGenerator;
	}

	public String getBearerToken() {
		return cachedServiceTokenGenerator.generate();
	}
}
