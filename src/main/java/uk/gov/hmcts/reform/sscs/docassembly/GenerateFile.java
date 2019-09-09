package uk.gov.hmcts.reform.sscs.docassembly;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.docassembly.DocAssemblyClient;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyRequest;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyResponse;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
@Slf4j
public class GenerateFile {

    private final AuthTokenGenerator authTokenGenerator;

    private final DocAssemblyClient docAssemblyClient;

    private final IdamService idamService;

    @Autowired
    public GenerateFile(AuthTokenGenerator authTokenGenerator, DocAssemblyClient docAssemblyClient, IdamService idamService) {
        this.authTokenGenerator = authTokenGenerator;
        this.docAssemblyClient = docAssemblyClient;
        this.idamService = idamService;
    }

    public String assemble() {
        final String authorisation = idamService.generateServiceAuthorization();

        DocAssemblyRequest docAssemblyRequest = DocAssemblyRequest.builder()
                .templateId("TB-SCS-GNO-ENG-00091.docx")
                .outputType(OutputType.PDF)
                .formPayload(null)
                .build();

        DocAssemblyResponse docAssemblyResponse = docAssemblyClient.generateOrder(
                authorisation,
                authTokenGenerator.generate(),
                docAssemblyRequest
        );

        return docAssemblyResponse.getRenditionOutputLocation();
    }
}
