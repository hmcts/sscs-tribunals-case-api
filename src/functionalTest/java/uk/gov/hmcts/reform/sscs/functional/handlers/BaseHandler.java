package uk.gov.hmcts.reform.sscs.functional.handlers;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.functional.sya.SubmitHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@Slf4j
public class BaseHandler {

    protected static final String CREATED_BY_FUNCTIONAL_TEST = "created by functional test";
    private static final List<String> DWP_DOCUMENT_TYPES = Arrays.stream(DwpDocumentType.values())
        .map(DwpDocumentType::getValue)
        .collect(Collectors.toList());
    @Autowired
    protected CcdService ccdService;

    @Autowired
    private IdamService idamService;

    @Autowired
    SubmitHelper submitHelper;

    @Autowired
    PdfStoreService pdfStoreService;

    @Autowired
    protected SscsCaseCallbackDeserializer deserializer;

    @Autowired
    protected ObjectMapper mapper;

    protected IdamTokens idamTokens;

    @Value("${test-url}")
    protected String testUrl;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
        idamTokens = idamService.getIdamTokens();
    }

    protected SscsCaseDetails addDocumentsToCase(SscsCaseData sscsCaseData, List<UploadDocument> docs) {
        final List<SscsDocument> sscsDocuments = docs.stream()
            .flatMap(doc -> pdfStoreService.store(doc.getData(), doc.getFilename(), doc.getDocumentType())
                .stream()
                .peek(sscsDoc -> sscsDoc.getValue().setBundleAddition(doc.getBundleAddition()))
                .peek(sscsDoc -> updateEditedDocument(doc.isHasEditedDocumentLink(), sscsDoc)))
            .collect(Collectors.toList());

        sscsCaseData.setSscsDocument(sscsDocuments.stream()
            .filter(doc -> !DWP_DOCUMENT_TYPES.contains(doc.getValue().getDocumentType()))
            .collect(Collectors.toList()));

        sscsCaseData.setDwpDocuments(sscsDocuments.stream()
            .filter(doc -> DWP_DOCUMENT_TYPES.contains(doc.getValue().getDocumentType()))
            .map(this::toDwpDocument)
            .collect(Collectors.toList()));

        return runEvent(sscsCaseData, EventType.UPDATE_CASE_ONLY);
    }

    private DwpDocument toDwpDocument(SscsDocument sscsDoc) {
        return DwpDocument.builder().value(DwpDocumentDetails.builder()
            .documentLink(sscsDoc.getValue().getDocumentLink())
            .documentType(sscsDoc.getValue().getDocumentType())
            .editedDocumentLink(sscsDoc.getValue().getEditedDocumentLink())
            .bundleAddition(sscsDoc.getValue().getBundleAddition())
            .documentDateTimeAdded(LocalDateTime.now())
            .build()).build();
    }

    private void updateEditedDocument(boolean hasEditedDocumentLink, SscsDocument doc) {
        if (hasEditedDocumentLink) {
            doc.getValue().setEditedDocumentLink(doc.getValue().getDocumentLink());
        }
    }

    public SscsCaseDetails runEvent(final SscsCaseData sscsCaseData, final EventType eventType) {
        return ccdService.updateCase(sscsCaseData, Long.valueOf(sscsCaseData.getCcdCaseId()), eventType.getCcdType(), CREATED_BY_FUNCTIONAL_TEST, CREATED_BY_FUNCTIONAL_TEST, idamTokens);
    }

    protected SscsCaseDetails createCase() {
        final SscsCaseData sscsCaseData = buildSscsCaseDataForTesting("Bowie", submitHelper.getRandomNino());
        sscsCaseData.getAppeal().getMrnDetails().setMrnDate(submitHelper.getRandomMrnDate().toString());
        return ccdService.createCase(sscsCaseData,
            EventType.CREATE_WITH_DWP_TEST_CASE.getCcdType(), CREATED_BY_FUNCTIONAL_TEST, CREATED_BY_FUNCTIONAL_TEST, idamTokens);
    }

    protected SscsCaseDetails getByCaseId(Long id) {
        return ccdService.getByCaseId(id, idamTokens);
    }

    protected SscsCaseDetails createCaseInResponseReceivedState(int retry) throws Exception {
        return ccdService.createCase(buildSscsCaseDataForTesting("Mercury", "JK 77 33 22 Z"),
            EventType.CREATE_RESPONSE_RECEIVED_TEST_CASE.getCcdType(), CREATED_BY_FUNCTIONAL_TEST,
            CREATED_BY_FUNCTIONAL_TEST, idamTokens);
    }

    protected SscsCaseDetails createCaseInWithDwpState(int retry) throws Exception {
        return ccdService.createCase(buildSscsCaseDataForTesting("Lennon", "BB 22 55 66 B"),
            EventType.CREATE_WITH_DWP_TEST_CASE.getCcdType(), CREATED_BY_FUNCTIONAL_TEST,
            CREATED_BY_FUNCTIONAL_TEST, idamTokens);
    }

    protected String getMyaResponse(int retry, Long caseId) {
        Response response = null;
        while (retry > 0 && (response == null || response.statusCode() != HttpStatus.OK.value())) {
            response = RestAssured
                .given()
                .when()
                .get("appeals?mya=true&caseId=" + caseId);
            retry--;
        }

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        return response.then().extract().body().asString();
    }

    private SscsCaseDetails updateCase(int retry, SscsCaseData caseData, Long caseId, String eventType, String summary, String description, IdamTokens idamTokens) throws Exception {
        while (retry > 0) {
            retry--;
            Thread.sleep(5000);
            try {
                log.info("UpdateCase for caseId {} retry count {}", caseId, retry);
                return ccdService.updateCase(caseData, caseId, eventType, summary, description, idamTokens);
            } catch (FeignException feignException) {
                log.info("UpdateCase failed for caseId {} with error {}", caseId, feignException.getMessage());
                if (feignException.status() < HttpStatus.INTERNAL_SERVER_ERROR.value() || retry <= 0) {
                    throw feignException;
                }
            }
        }
        throw new Exception("UpdateCase failed for caseId:" + caseId);
    }


    public static String getJsonCallbackForTest(String path) throws IOException {
        String pathName = Objects.requireNonNull(BaseHandler.class.getClassLoader().getResource(path)).getFile();
        return FileUtils.readFileToString(new File(pathName), StandardCharsets.UTF_8.name());
    }

    public static String getJsonCallbackForTestAndReplace(String fileLocation, List<String> replaceKeys, List<String> replaceValues) throws IOException {
        String result = getJsonCallbackForTest(fileLocation);
        for (int i = 0; i < replaceKeys.size(); i++) {
            result = result.replace(replaceKeys.get(i), replaceValues.get(i));
        }
        return result;
    }


    protected static SscsCaseData buildSscsCaseDataForTesting(final String surname, final String nino, final SscsCaseData sscsCaseData) {
        SscsCaseData testCaseData;
        if (sscsCaseData == null) {
            testCaseData = buildCaseData(surname, nino);
        } else {
            testCaseData = buildCaseData(surname, nino, sscsCaseData);
        }

        addFurtherEvidenceActionData(testCaseData);

        Subscription appellantSubscription = Subscription.builder()
            .tya("app-appeal-number")
            .email("sscstest+notify@greencroftconsulting.com")
            .mobile("07398785050")
            .subscribeEmail(YES)
            .subscribeSms(YES)
            .reason("")
            .build();
        Subscription appointeeSubscription = Subscription.builder()
            .tya("appointee-appeal-number")
            .email("sscstest+notify@greencroftconsulting.com")
            .mobile("07398785050")
            .subscribeEmail(YES)
            .subscribeSms(YES)
            .reason("")
            .build();
        Subscription supporterSubscription = Subscription.builder()
            .tya("")
            .email("sscstest+notify@greencroftconsulting.com")
            .mobile("07398785050")
            .subscribeEmail("")
            .subscribeSms("")
            .reason("")
            .build();
        Subscription representativeSubscription = Subscription.builder()
            .tya("rep-appeal-number")
            .email("sscstest+notify@greencroftconsulting.com")
            .mobile("07398785050")
            .subscribeEmail(YES)
            .subscribeSms(YES)
            .build();
        Subscriptions subscriptions = Subscriptions.builder()
            .appellantSubscription(appellantSubscription)
            .supporterSubscription(supporterSubscription)
            .representativeSubscription(representativeSubscription)
            .appointeeSubscription(appointeeSubscription)
            .build();

        testCaseData.setSubscriptions(subscriptions);
        return testCaseData;
    }


    protected static SscsCaseData buildSscsCaseDataForTesting(final String surname, final String nino) {
        SscsCaseData testCaseData = buildCaseData(surname, nino);

        addFurtherEvidenceActionData(testCaseData);

        Subscription appellantSubscription = Subscription.builder()
            .tya("app-appeal-number")
            .email("sscstest+notify@greencroftconsulting.com")
            .mobile("07398785050")
            .subscribeEmail(YES)
            .subscribeSms(YES)
            .reason("")
            .build();
        Subscription appointeeSubscription = Subscription.builder()
            .tya("appointee-appeal-number")
            .email("sscstest+notify@greencroftconsulting.com")
            .mobile("07398785050")
            .subscribeEmail(YES)
            .subscribeSms(YES)
            .reason("")
            .build();
        Subscription supporterSubscription = Subscription.builder()
            .tya("")
            .email("sscstest+notify@greencroftconsulting.com")
            .mobile("07398785050")
            .subscribeEmail("")
            .subscribeSms("")
            .reason("")
            .build();
        Subscription representativeSubscription = Subscription.builder()
            .tya("rep-appeal-number")
            .email("sscstest+notify@greencroftconsulting.com")
            .mobile("07398785050")
            .subscribeEmail(YES)
            .subscribeSms(YES)
            .build();
        Subscriptions subscriptions = Subscriptions.builder()
            .appellantSubscription(appellantSubscription)
            .supporterSubscription(supporterSubscription)
            .representativeSubscription(representativeSubscription)
            .appointeeSubscription(appointeeSubscription)
            .build();

        testCaseData.setSubscriptions(subscriptions);
        return testCaseData;
    }

    private static void addFurtherEvidenceActionData(SscsCaseData testCaseData) {
        testCaseData.setInterlocReviewState(null);
        if (testCaseData.getFurtherEvidenceAction() != null) {
            return;
        }
        DynamicListItem value = new DynamicListItem("informationReceivedForInterlocJudge", "any");
        DynamicList furtherEvidenceActionList = new DynamicList(value, Collections.singletonList(value));
        testCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
    }

    private static SscsCaseData buildCaseData(final String surname, final String nino) {
        return buildCaseData(surname, nino, new SscsCaseData());

    }


    private static SscsCaseData buildCaseData(final String surname, final String nino, final SscsCaseData sscsCaseData) {
        Name name = Name.builder()
            .title("Mr")
            .firstName("User")
            .lastName(surname)
            .build();
        Address address = Address.builder()
            .line1("123 Hairy Lane")
            .line2("Off Hairy Park")
            .town("Hairyfield")
            .county("Kent")
            .postcode("TS1 1ST")
            .build();
        Contact contact = Contact.builder()
            .email("mail@email.com")
            .phone("01234567890")
            .mobile("01234567890")
            .build();
        Identity identity = Identity.builder()
            .dob("1904-03-10")
            .nino(nino)
            .build();

        Appellant appellant = Appellant.builder()
            .name(name)
            .address(address)
            .contact(contact)
            .identity(identity)
            .build();

        BenefitType benefitType = BenefitType.builder()
            .code("PIP")
            .build();

        HearingOptions hearingOptions = HearingOptions.builder()
            .wantsToAttend(YES)
            .languageInterpreter(YES)
            .languages("Spanish")
            .signLanguageType("A sign language")
            .arrangements(Arrays.asList("hearingLoop", "signLanguageInterpreter"))
            .other("Yes, this...")
            .build();

        MrnDetails mrnDetails = MrnDetails.builder()
            .mrnDate("2018-06-29")
            .dwpIssuingOffice("1")
            .mrnLateReason("Lost my paperwork")
            .build();

        final Appeal appeal = Appeal.builder()
            .appellant(appellant)
            .benefitType(benefitType)
            .hearingOptions(hearingOptions)
            .mrnDetails(mrnDetails)
            .signer("Signer")
            .hearingType("oral")
            .receivedVia("Online")
            .build();


        Subscription appellantSubscription = Subscription.builder()
            .tya("app-appeal-number")
            .email("appellant@email.com")
            .mobile("")
            .subscribeEmail(YES)
            .subscribeSms(YES)
            .reason("")
            .build();
        Subscriptions subscriptions = Subscriptions.builder()
            .appellantSubscription(appellantSubscription)
            .build();
        sscsCaseData.setSubscriptions(subscriptions);
        sscsCaseData.setAppeal(appeal);
        sscsCaseData.setRegion("CARDIFF");
        sscsCaseData.setCreatedInGapsFrom(READY_TO_LIST.getId());
        sscsCaseData.setCaseReference("SC068/17/00013");
        sscsCaseData.setCaseCreated(LocalDate.now().toString());
        return sscsCaseData;

    }

    public String serializeSscsCallback(Callback<SscsCaseData> callback) {
        try {
            return this.mapper.writeValueAsString(callback);
        } catch (IOException var3) {
            throw new IllegalArgumentException("Could not serialize caseData", var3);
        }
    }
}
