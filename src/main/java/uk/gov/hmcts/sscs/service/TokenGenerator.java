package uk.gov.hmcts.sscs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.auth.provider.service.token.CachedServiceTokenGenerator;

@Service
public class TokenGenerator {
	private final ServiceTokenGenerator serviceTokenGenerator;

	@Autowired
	public TokenGenerator(@Qualifier("cachedServiceTokenGenerator") final ServiceTokenGenerator serviceTokenGenerator){
		this.serviceTokenGenerator = serviceTokenGenerator;
	}

	public String getBearerToken() {
		return serviceTokenGenerator.generate();
	}
}
