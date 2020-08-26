package uk.gov.hmcts.reform.sscs.functional.handlers;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static uk.gov.hmcts.reform.sscs.functional.ccd.UpdateCaseInCcdTest.buildSscsCaseDataForTesting;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class BaseHandler {

    protected static final String CREATED_BY_FUNCTIONAL_TEST = "created by functional test";

    @Autowired
    protected CcdService ccdService;
    @Autowired
    private IdamService idamService;

    protected IdamTokens idamTokens;
    protected CaseDetails<SscsCaseData> caseDetails;
    @Value("${test-url}")
    private String testUrl;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
        idamTokens = idamService.getIdamTokens();
    }

    protected CaseDetails<SscsCaseData> createCaseInWithDwpStateUsingGivenCallback(String filePath) throws Exception {
        SscsCaseDetails caseDetails = ccdService.createCase(buildSscsCaseDataForTestingWithValidMobileNumbers(),
            EventType.VALID_APPEAL_CREATED.getCcdType(), CREATED_BY_FUNCTIONAL_TEST,
            CREATED_BY_FUNCTIONAL_TEST, idamTokens);

        Thread.sleep(5000);

        ccdService.updateCase(caseDetails.getData(), caseDetails.getId(), EventType.SENT_TO_DWP.getCcdType(),
            CREATED_BY_FUNCTIONAL_TEST, CREATED_BY_FUNCTIONAL_TEST, idamTokens);

        return createCaseDetailsUsingGivenCallback(caseDetails.getId(), filePath);
    }

    protected SscsCaseDetails createCaseInResponseReceivedState() throws Exception {
        SscsCaseDetails caseDetails = ccdService.createCase(buildSscsCaseDataForTesting("Mercury", "JK 77 33 22 Z"),
                EventType.VALID_APPEAL_CREATED.getCcdType(), CREATED_BY_FUNCTIONAL_TEST,
                CREATED_BY_FUNCTIONAL_TEST, idamTokens);

        Thread.sleep(5000);

        return ccdService.updateCase(caseDetails.getData(), caseDetails.getId(), EventType.DWP_RESPOND.getCcdType(),
                CREATED_BY_FUNCTIONAL_TEST, CREATED_BY_FUNCTIONAL_TEST, idamTokens);
    }

    protected SscsCaseDetails createCaseInWithDwpState() throws Exception {
        SscsCaseDetails caseDetails = ccdService.createCase(buildSscsCaseDataForTesting("Lennon", "BB 22 55 66 B"),
                EventType.VALID_APPEAL_CREATED.getCcdType(), CREATED_BY_FUNCTIONAL_TEST,
                CREATED_BY_FUNCTIONAL_TEST, idamTokens);

        Thread.sleep(5000);

        return ccdService.updateCase(caseDetails.getData(), caseDetails.getId(), EventType.SENT_TO_DWP.getCcdType(),
                CREATED_BY_FUNCTIONAL_TEST, CREATED_BY_FUNCTIONAL_TEST, idamTokens);
    }

    protected SscsCaseDetails createCaseInValidAppealCreatedState() throws IOException {
        return ccdService.createCase(buildSscsCaseDataForTesting("Bono", "AZ 44 88 99 Y"),
                EventType.VALID_APPEAL_CREATED.getCcdType(), CREATED_BY_FUNCTIONAL_TEST,
                CREATED_BY_FUNCTIONAL_TEST, idamTokens);
    }

    private CaseDetails<SscsCaseData> createCaseDetailsUsingGivenCallback(Long id, String filePath)
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
        return sscsCaseCallbackDeserializer.deserialize(getJsonCallbackForTestAndReplaceCcdId(id, filePath)).getCaseDetails();
    }

    private String getJsonCallbackForTestAndReplaceCcdId(long caseDetailsId, String filePath) throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource(filePath)).getFile();
        String jsonCallback = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        return jsonCallback.replace("CASE_ID_TO_BE_REPLACED", String.valueOf(caseDetailsId));
    }


    public static String getJsonCallbackForTest(String path) throws IOException {
        String pathName = Objects.requireNonNull(BaseHandler.class.getClassLoader().getResource(path)).getFile();
        return FileUtils.readFileToString(new File(pathName), StandardCharsets.UTF_8.name());
    }


}
