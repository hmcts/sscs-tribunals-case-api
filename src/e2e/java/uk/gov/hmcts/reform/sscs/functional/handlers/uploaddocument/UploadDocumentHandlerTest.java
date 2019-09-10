package uk.gov.hmcts.reform.sscs.functional.handlers.uploaddocument;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.functional.ccd.UpdateCaseInCcdTest.buildSscsCaseDataForTestingWithValidMobileNumbers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class UploadDocumentHandlerTest {

    private static final String CREATED_BY_FUNCTIONAL_TEST = "created by functional test";
    @Value("${test-url}")
    private String testUrl;
    @Autowired
    private IdamService idamService;
    @Autowired
    private CcdService ccdService;
    private IdamTokens idamTokens;
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() throws IOException {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
        idamTokens = idamService.getIdamTokens();
        caseDetails = createCaseInWithDwpState();
    }

    private CaseDetails<SscsCaseData> createCaseInWithDwpState() throws IOException {
        SscsCaseDetails caseDetails = ccdService.createCase(buildSscsCaseDataForTestingWithValidMobileNumbers(),
            EventType.VALID_APPEAL_CREATED.getCcdType(), CREATED_BY_FUNCTIONAL_TEST,
            CREATED_BY_FUNCTIONAL_TEST, idamTokens);

        ccdService.updateCase(caseDetails.getData(), caseDetails.getId(), EventType.SENT_TO_DWP.getCcdType(),
            CREATED_BY_FUNCTIONAL_TEST, CREATED_BY_FUNCTIONAL_TEST, idamTokens);

        return updateCaseDataWithSomeDocsAndAMoreRealisticBodyForTheUploadDocumentEvent(caseDetails.getId());
    }

    private CaseDetails<SscsCaseData> updateCaseDataWithSomeDocsAndAMoreRealisticBodyForTheUploadDocumentEvent(Long id)
        throws IOException {
        Jackson2ObjectMapperBuilder objectMapperBuilder =
            new Jackson2ObjectMapperBuilder()
                .featuresToEnable(READ_ENUMS_USING_TO_STRING)
                .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
                .serializationInclusion(JsonInclude.Include.NON_ABSENT);

        ObjectMapper mapper = objectMapperBuilder.createXmlMapper(false).build();
        mapper.registerModule(new JavaTimeModule());

        SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer = new SscsCaseCallbackDeserializer(mapper);
        return sscsCaseCallbackDeserializer.deserialize(getJsonCallbackForTest(id)).getCaseDetails();
    }

    private String getJsonCallbackForTest(long caseDetailsId) throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource("uploadDocumentCallback.json")).getFile();
        String jsonCallback = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        return jsonCallback.replace("CASE_ID_TO_BE_REPLACED", String.valueOf(caseDetailsId));
    }

    @Test
    public void givenUploadDocumentEventIsTriggered_shouldUploadDocument() {
        SscsCaseDetails actualCase = ccdService.updateCase(caseDetails.getCaseData(), caseDetails.getId(),
            EventType.UPLOAD_DOCUMENT.getCcdType(), CREATED_BY_FUNCTIONAL_TEST, CREATED_BY_FUNCTIONAL_TEST, idamTokens);
        assertEquals(2, actualCase.getData().getSscsDocument().size());
        assertEquals(State.WITH_DWP.getId(), actualCase.getState());
    }

}

