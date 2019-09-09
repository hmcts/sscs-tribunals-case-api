package uk.gov.hmcts.reform.sscs.docassembly;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.docassembly.DocAssemblyClient;
import uk.gov.hmcts.reform.docassembly.domain.DocAssemblyResponse;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(MockitoJUnitRunner.class)
public class GenerateFileTest {

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private DocAssemblyClient docAssemblyClient;

    @Mock
    private IdamService idamService;

    @Mock
    private DocAssemblyResponse response;

    private GenerateFile generateFile;

    @Before
    public void setUp() {
        generateFile = new GenerateFile(authTokenGenerator, docAssemblyClient, idamService);
    }

    @Test
    public void willCallGenerateOrderOnDocAssemblyClient() {
        when(idamService.generateServiceAuthorization()).thenReturn("authString");
        when(authTokenGenerator.generate()).thenReturn("token");
        when(docAssemblyClient.generateOrder(anyString(), anyString(), any())).thenReturn(response);
        when(response.getRenditionOutputLocation()).thenReturn("test");
        String output = generateFile.assemble();
        assertEquals("test", output);
    }
}
