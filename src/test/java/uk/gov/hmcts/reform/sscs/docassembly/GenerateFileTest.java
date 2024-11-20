package uk.gov.hmcts.reform.sscs.docassembly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.DocAssemblyClient;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyResponse;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;

@ExtendWith(MockitoExtension.class)
public class GenerateFileTest {

    @Mock
    private DocAssemblyClient docAssemblyClient;

    @Mock
    private IdamService idamService;

    @Mock
    private DocAssemblyResponse response;

    private GenerateFile generateFile;

    @BeforeEach
    public void setUp() {
        generateFile = new GenerateFile(docAssemblyClient, idamService);
    }

    @Test
    public void willCallGenerateOrderOnDocAssemblyClient() {
        GenerateFileParams params = GenerateFileParams.builder()
                .templateId("templateId")
                .userAuthentication("Bearer Token")
                .build();
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().idamOauth2Token("oauth").serviceAuthorization("sscs").build());
        when(docAssemblyClient.generateOrder(eq(params.getUserAuthentication()), anyString(), any())).thenReturn(response);
        when(response.getRenditionOutputLocation()).thenReturn("test");
        String output = generateFile.assemble(params);
        assertEquals("test", output);
    }
}
