package uk.gov.hmcts.reform.sscs.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@RequiredArgsConstructor
public class SscsCallbackOrchestratorService {

    private final SscsCallbackOrchestratorApi sscsCallbackOrchestratorApi;

    private final IdamService idamService;

    public ResponseEntity<String> send(String caseData) {
        IdamTokens idamTokens = idamService.getIdamTokens();
        return sscsCallbackOrchestratorApi
            .send(idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                caseData);
    }
}

