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

    private IdamService idamService;
    private DocumentUploadClientApi documentUploadClientApi;

    @Autowired
    public EvidenceManagementService(IdamService idamService, DocumentUploadClientApi documentUploadClientApi) {
        this.idamService = idamService;
        this.documentUploadClientApi = documentUploadClientApi;
    }

    public UploadResponse upload(List<MultipartFile> multipartFileList) {
        String serviceAuthorization = idamService.generateServiceAuthorization();
        String oauth2Token = idamService.getIdamOauth2Token();
        return documentUploadClientApi.upload(oauth2Token, serviceAuthorization, multipartFileList);
    }
}
