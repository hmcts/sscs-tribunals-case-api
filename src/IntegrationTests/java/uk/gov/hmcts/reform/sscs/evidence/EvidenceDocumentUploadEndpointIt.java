package uk.gov.hmcts.reform.sscs.evidence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClient;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:config/application_it.properties")
@AutoConfigureMockMvc
public class EvidenceDocumentUploadEndpointIt {


    public static final String AUTH_TOKEN = "authToken";

    public static final String DUMMY_OAUTH_2_TOKEN = "oauth2Token";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private CaseDocumentClient documentUploadClientApi;

    @MockBean
    IdamService idamService;

    @Mock
    private UploadResponse uploadResponse;

    @Before
    public void setUp() throws Exception {
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder()
                .idamOauth2Token(DUMMY_OAUTH_2_TOKEN).serviceAuthorization(AUTH_TOKEN).build());
    }

    @Test
    public void shouldThrow404ErrorIfNotEvidenceDocumentSubmitted() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockMvc.perform(MockMvcRequestBuilders.multipart("/evidence/upload"))
            .andExpect(status().is(404));

    }

    @Test
    public void shouldStoreTheEvidenceDocumentAndReturnMetadata() throws Exception {
        List<Document> documents = new ArrayList<>();
        when(uploadResponse.getDocuments()).thenReturn(documents);

        when(documentUploadClientApi.uploadDocuments(any(), any(), eq("Benefit"), eq("SSCS"),
             any()))
            .thenReturn(uploadResponse);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDDocument doc = new PDDocument();
            doc.addPage(new PDPage());
            doc.save(baos);
            doc.close();
            final byte[] bytes = baos.toByteArray();
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
            mockMvc.perform(MockMvcRequestBuilders.multipart("/evidence/upload")
                    .file("file", bytes)
                    .characterEncoding("UTF-8"))
                    .andExpect(status().is(200));

        }
    }
}
