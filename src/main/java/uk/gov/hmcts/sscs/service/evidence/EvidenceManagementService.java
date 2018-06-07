package uk.gov.hmcts.sscs.service.evidence;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.sscs.service.idam.IdamService;


@Service
public class EvidenceManagementService {

    public static final String DUMMY_OAUTH_2_TOKEN = "oauth2Token";
    private IdamService idamService;
    private DocumentUploadClientApi documentUploadClientApi;

    @Autowired
    public EvidenceManagementService(IdamService idamService, DocumentUploadClientApi documentUploadClientApi) {
        this.idamService = idamService;
        this.documentUploadClientApi = documentUploadClientApi;
    }

    public UploadResponse upload(List<MultipartFile> multipartFileList) {
        String serviceAuthorization = idamService.generateServiceAuthorization();
        return documentUploadClientApi.upload(DUMMY_OAUTH_2_TOKEN, serviceAuthorization, multipartFileList);
    }
}
