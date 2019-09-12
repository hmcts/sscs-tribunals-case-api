package uk.gov.hmcts.reform.sscs.docassembly;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.DocAssemblyClient;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyRequest;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyResponse;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;

@Service
@Slf4j
public class GenerateFile {

    private final DocAssemblyClient docAssemblyClient;

    private final IdamService idamService;

    @Autowired
    public GenerateFile(DocAssemblyClient docAssemblyClient, IdamService idamService) {
        this.docAssemblyClient = docAssemblyClient;
        this.idamService = idamService;
    }

    public String assemble(GenerateFileParams params) {
        IdamTokens idamTokens = idamService.getIdamTokens();

        DocAssemblyRequest docAssemblyRequest = DocAssemblyRequest.builder()
                .templateId(params.getTemplateId())
                .outputType(params.getOutputType())
                .formPayload(params.getFormPayload())
                //renditionOutputLocation(params.getRenditionOutputLocation()) // currently missing. Needed this to replace the dm store file.
                .build();

        DocAssemblyResponse docAssemblyResponse = docAssemblyClient.generateOrder(
                params.getUserAuthentication(),
                idamTokens.getServiceAuthorization(),
                docAssemblyRequest
        );

        return docAssemblyResponse.getRenditionOutputLocation();
    }
}
