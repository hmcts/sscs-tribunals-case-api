package uk.gov.hmcts.reform.sscs.evidence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.document.domain.UploadResponse.Embedded;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.Classification;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
@AutoConfigureMockMvc
public class EvidenceDocumentUploadEndpointIt {

    @MockitoBean
    protected AirLookupService airLookupService;
    public static final String AUTH_TOKEN = "authToken";

    public static final String DUMMY_OAUTH_2_TOKEN = "oauth2Token";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @MockitoBean
    private DocumentUploadClientApi documentUploadClientApi;

    @MockitoBean
    AuthorisationService authorisationService;

    @Mock
    private UploadResponse uploadResponse;

    @Mock
    private Embedded value;

    @BeforeEach
    public void setUp() throws Exception {
        given(authTokenGenerator.generate()).willReturn(AUTH_TOKEN);
    }

    @Test
    public void shouldStoreTheEvidenceDocumentAndReturnMetadata() throws Exception {
        given(documentUploadClientApi.upload(eq(DUMMY_OAUTH_2_TOKEN), eq(AUTH_TOKEN), eq("sscs"),
            eq(Arrays.asList("caseworker", "citizen")), eq(Classification.RESTRICTED), any()))
            .willReturn(uploadResponse);
        Embedded embedded = new Embedded();
        when(uploadResponse.getEmbedded()).thenReturn(embedded);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDDocument doc = new PDDocument();
            doc.addPage(new PDPage());
            doc.save(baos);
            doc.close();
            final byte[] bytes = baos.toByteArray();
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
            mockMvc.perform(MockMvcRequestBuilders.multipart("/evidence/upload")
                    .file("file", bytes)
                    .characterEncoding("UTF-8")
                    .header("ServiceAuthorization", AUTH_TOKEN))
                    .andExpect(status().is(200));

        }
    }
}
