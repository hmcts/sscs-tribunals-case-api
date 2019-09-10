package uk.gov.hmcts.reform.sscs.docassembly;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.DocAssemblyClient;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyRequest;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyResponse;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

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

    public String assemble() {
        IdamTokens idamTokens = idamService.getIdamTokens();

        DocAssemblyRequest docAssemblyRequest = DocAssemblyRequest.builder()
                .templateId("TB-SCS-GNO-ENG-00091.docx")
                .outputType(OutputType.PDF)
                .formPayload(null)
                .build();

        DocAssemblyResponse docAssemblyResponse = docAssemblyClient.generateOrder(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                docAssemblyRequest
        );

        return docAssemblyResponse.getRenditionOutputLocation();
    }
}
