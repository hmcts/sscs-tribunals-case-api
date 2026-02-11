package uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.docmosis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.IBCA_URL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.SscsCaseDataUtils.getWelshDate;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.ADDRESS_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.LETTER_ADDRESS_LINE_1;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.LETTER_ADDRESS_LINE_2;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.LETTER_ADDRESS_LINE_3;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.LETTER_ADDRESS_LINE_4;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.LETTER_ADDRESS_POSTCODE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.docmosis.PdfLetterService.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.docmosis.PdfLetterService.WELSH_GENERATED_DATE_LITERAL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.DocmosisTemplatesConfig;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.properties.EvidenceProperties;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.docmosis.PdfCoverSheet;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.NotificationProcessingServiceTest;

@RunWith(JUnitParamsRunner.class)
public class PdfLetterServiceTest {

    private final DocmosisPdfService docmosisPdfService = mock(DocmosisPdfService.class);

    private static final Map<LanguagePreference, Map<String, String>> TEMPLATE_NAMES = new ConcurrentHashMap<>();
    private static final DocmosisTemplatesConfig DOCMOSIS_TEMPLATES_CONFIG = new DocmosisTemplatesConfig();
    private static final EvidenceProperties EVIDENCE_PROPERTIES = new EvidenceProperties();
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-d");
    private static EvidenceProperties.EvidenceAddress EVIDENCE_ADDRESS = new EvidenceProperties.EvidenceAddress();
    private static final Subscription EMPTY_SUBSCRIPTION = Subscription.builder().build();

    static {
        TEMPLATE_NAMES.put(LanguagePreference.ENGLISH, Collections.singletonMap(APPEAL_RECEIVED.getId(), "my01.doc"));
        TEMPLATE_NAMES.put(LanguagePreference.WELSH, Collections.singletonMap(APPEAL_RECEIVED.getId(), "my01.doc"));
        DOCMOSIS_TEMPLATES_CONFIG.setCoversheets(TEMPLATE_NAMES);
        DOCMOSIS_TEMPLATES_CONFIG.setHmctsWelshImgKey("welshhmcts");
        DOCMOSIS_TEMPLATES_CONFIG.setHmctsWelshImgVal("welshhmcts.png");

        EVIDENCE_ADDRESS.setLine1("line1");
        EVIDENCE_ADDRESS.setLine2("line2");
        EVIDENCE_ADDRESS.setLine3("line3");
        EVIDENCE_ADDRESS.setScottishLine3("scottishLine1");
        EVIDENCE_ADDRESS.setTown("town");
        EVIDENCE_ADDRESS.setCounty("county");
        EVIDENCE_ADDRESS.setPostcode("postcode");
        EVIDENCE_ADDRESS.setScottishPostcode("scottishPostcode");
        EVIDENCE_PROPERTIES.setAddress(EVIDENCE_ADDRESS);
    }

    private final PdfLetterService pdfLetterService =
        new PdfLetterService(docmosisPdfService, DOCMOSIS_TEMPLATES_CONFIG, EVIDENCE_PROPERTIES);

    private final Appellant appellant = Appellant.builder()
        .name(Name.builder().firstName("Ap").lastName("pellant").build())
        .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
        .appointee(Appointee.builder().build())
        .build();

    private final Representative representative = Representative.builder()
        .name(Name.builder().firstName("Rep").lastName("resentative").build())
        .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RP9 3LL").build())
        .build();

    @Test
    @Parameters({"APPELLANT, Yes, true", "APPELLANT, Yes, false", "REPRESENTATIVE, No, true", "REPRESENTATIVE, No, false"})
    public void willCreateAPdfToTheCorrectAddress(final SubscriptionType subscriptionType, String isScottish, boolean isScottishPoBoxFeatureEnabled) {
        NotificationWrapper wrapper = NotificationProcessingServiceTest.buildBaseWrapper(
            APPEAL_RECEIVED,
            appellant,
            representative,
            null
        );

        EVIDENCE_ADDRESS.setScottishPoBoxFeatureEnabled(isScottishPoBoxFeatureEnabled);
        wrapper.getNewSscsCaseData().setIsScottishCase(isScottish);

        Entity entity = subscriptionType.equals(SubscriptionType.APPELLANT)
            ? appellant : representative;
        pdfLetterService.buildCoversheet(wrapper, new SubscriptionWithType(EMPTY_SUBSCRIPTION, subscriptionType,
            appellant, entity));

        Address address = subscriptionType.equals(SubscriptionType.APPELLANT)
            ? appellant.getAddress() : representative.getAddress();
        Name name = subscriptionType.equals(SubscriptionType.APPELLANT)
            ? appellant.getName() : representative.getName();
        String expectedLine3 = "Yes".equalsIgnoreCase(isScottish) && isScottishPoBoxFeatureEnabled ? EVIDENCE_ADDRESS.getScottishLine3() : EVIDENCE_ADDRESS.getLine3();
        String expectedPostcode = "Yes".equalsIgnoreCase(isScottish) && isScottishPoBoxFeatureEnabled ? EVIDENCE_ADDRESS.getScottishPostcode() : EVIDENCE_ADDRESS.getPostcode();

        PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(wrapper.getCaseId(),
            name.getFullNameNoTitle(),
            address.getLine1(),
            address.getTown(),
            address.getCounty(),
            address.getPostcode(),
            "",
            EVIDENCE_ADDRESS.getLine2(),
            expectedLine3,
            EVIDENCE_ADDRESS.getTown(),
            expectedPostcode,
            DOCMOSIS_TEMPLATES_CONFIG.getHmctsImgVal(),
            DOCMOSIS_TEMPLATES_CONFIG.getHmctsWelshImgVal());
        verify(docmosisPdfService).createPdf(eq(pdfCoverSheet), eq("my01.doc"));
    }

    @Test
    @Parameters({"Yes, true", "Yes, false", "No, true", "No, false"})
    public void willCreateAPdfToTheCorrectIbcAddress(String isScottish, boolean isScottishPoBoxFeatureEnabled) {
        NotificationWrapper wrapper = NotificationProcessingServiceTest.buildBaseWrapper(
            APPEAL_RECEIVED,
            appellant,
            representative,
            null
        );

        EVIDENCE_ADDRESS.setScottishPoBoxFeatureEnabled(isScottishPoBoxFeatureEnabled);
        wrapper.getNewSscsCaseData().setIsScottishCase(isScottish);
        wrapper.getNewSscsCaseData().setBenefitCode("093");

        pdfLetterService.buildCoversheet(wrapper, new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT,
            appellant, appellant));

        Address address = appellant.getAddress();
        String expectedLine3 = "Yes".equalsIgnoreCase(isScottish) && isScottishPoBoxFeatureEnabled ? EVIDENCE_ADDRESS.getScottishLine3() : EVIDENCE_ADDRESS.getIbcAddressLine3();
        String expectedPostcode = "Yes".equalsIgnoreCase(isScottish) && isScottishPoBoxFeatureEnabled ? EVIDENCE_ADDRESS.getScottishPostcode() : EVIDENCE_ADDRESS.getIbcAddressPostcode();

        PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(wrapper.getCaseId(),
            appellant.getName().getFullNameNoTitle(),
            address.getLine1(),
            address.getTown(),
            address.getCounty(),
            address.getPostcode(),
            "",
            EVIDENCE_ADDRESS.getIbcAddressLine2(),
            expectedLine3,
            EVIDENCE_ADDRESS.getTown(),
            expectedPostcode,
            DOCMOSIS_TEMPLATES_CONFIG.getHmctsImgVal(),
            DOCMOSIS_TEMPLATES_CONFIG.getHmctsWelshImgVal());
        verify(docmosisPdfService).createPdf(eq(pdfCoverSheet), eq("my01.doc"));
    }

    @Test
    public void willGenerateALetter() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        Mockito.reset(docmosisPdfService);
        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn(baos.toByteArray());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn(baos.toByteArray());
        baos.close();
        doc.close();
        NotificationWrapper wrapper = NotificationProcessingServiceTest.buildBaseWrapper(
            APPEAL_RECEIVED,
            appellant,
            representative,
            null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        byte[] letter = pdfLetterService.generateLetter(wrapper, notification,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT, appellant, appellant));
        byte[] coversheet = pdfLetterService.buildCoversheet(wrapper,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT, appellant, appellant));
        assertTrue(ArrayUtils.isNotEmpty(letter));
        assertTrue(ArrayUtils.isNotEmpty(coversheet));
        verify(docmosisPdfService).createPdfFromMap(any(), eq(notification.getDocmosisLetterTemplate()));
        verify(docmosisPdfService).createPdf(any(), anyString());
    }

    @Test
    public void willGenerateALetter_Welsh() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        Mockito.reset(docmosisPdfService);
        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn(baos.toByteArray());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn(baos.toByteArray());
        baos.close();
        doc.close();
        NotificationWrapper wrapper = NotificationProcessingServiceTest.buildBaseWrapper(
            APPEAL_RECEIVED,
            appellant,
            representative,
            null
        );
        wrapper.getNewSscsCaseData().setLanguagePreferenceWelsh("Yes");
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        byte[] letter = pdfLetterService.generateLetter(wrapper, notification,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT, appellant, appellant));
        byte[] coversheet = pdfLetterService.buildCoversheet(wrapper,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT, appellant, appellant));
        assertTrue(ArrayUtils.isNotEmpty(letter));
        assertTrue(ArrayUtils.isNotEmpty(coversheet));
        ArgumentCaptor<Map<String, Object>> placeholderCaptor = ArgumentCaptor.forClass(Map.class);
        verify(docmosisPdfService).createPdfFromMap(placeholderCaptor.capture(), eq(notification.getDocmosisLetterTemplate()));
        verify(docmosisPdfService).createPdf(any(), anyString());
        Map<String, Object> placeholderCaptorValue = placeholderCaptor.getValue();
        assertEquals("Welsh generated date", getWelshDate().apply(placeholderCaptorValue.get(GENERATED_DATE_LITERAL),
            dateTimeFormatter), placeholderCaptorValue.get(WELSH_GENERATED_DATE_LITERAL));
    }


    @Test
    public void givenAnAddressWithMoreThan45CharactersInEachLine_willTruncateAddressAndGenerateALetter() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        Mockito.reset(docmosisPdfService);
        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn(baos.toByteArray());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn(baos.toByteArray());
        baos.close();
        doc.close();

        Name name = Name.builder().firstName("Jimmy").lastName("AVeryLongNameWithLotsaAdLotsAndLotsOfCharacters").build();

        Address address = Address.builder()
            .line1("MyFirstVeryVeryLongAddressLineWithLotsOfCharacters")
            .line2("MySecondVeryVeryLongAddressLineWithLotsOfCharacters")
            .town("MyTownVeryVeryLongAddressLineWithLotsOfCharacters")
            .county("MyCountyVeryVeryLongAddressLineWithLotsOfCharacters")
            .postcode("L2 5UZ").build();

        appellant.setName(name);
        appellant.setAddress(address);

        NotificationWrapper wrapper = NotificationProcessingServiceTest.buildBaseWrapper(
            APPEAL_RECEIVED,
            appellant,
            representative,
            null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        byte[] letter = pdfLetterService.generateLetter(wrapper, notification,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT, appellant, appellant));
        assertTrue(ArrayUtils.isNotEmpty(letter));

        ArgumentCaptor<Map<String, Object>> placeholderCaptor = ArgumentCaptor.forClass(Map.class);

        verify(docmosisPdfService).createPdfFromMap(placeholderCaptor.capture(), eq(notification.getDocmosisLetterTemplate()));
        assertEquals("Jimmy AVeryLongNameWithLotsaAdLotsAndLotsOfCh", placeholderCaptor.getValue().get(ADDRESS_NAME));
        assertEquals("MyFirstVeryVeryLongAddressLineWithLotsOfChara", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_1));
        assertEquals("MySecondVeryVeryLongAddressLineWithLotsOfChar", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_2));
        assertEquals("MyTownVeryVeryLongAddressLineWithLotsOfCharac", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_3));
        assertEquals("MyCountyVeryVeryLongAddressLineWithLotsOfChar", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_4));
        assertEquals("www.gov.uk/appeal-benefit-decision", placeholderCaptor.getValue().get(SSCS_URL_LITERAL));
        assertEquals("L2 5UZ", placeholderCaptor.getValue().get(LETTER_ADDRESS_POSTCODE));
        assertNull(placeholderCaptor.getValue().get(WELSH_GENERATED_DATE_LITERAL));
    }

    @Test
    public void givenAnIbcCase_willGenerateALetterWithIbcUrl() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        Mockito.reset(docmosisPdfService);
        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn(baos.toByteArray());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn(baos.toByteArray());
        baos.close();
        doc.close();

        Name name = Name.builder().firstName("Jimmy").lastName("lastName").build();

        Address address = Address.builder()
            .line1("line1")
            .line2("line2")
            .town("town")
            .county("county")
            .postcode("L2 5UZ").build();

        appellant.setName(name);
        appellant.setAddress(address);

        NotificationWrapper wrapper = NotificationProcessingServiceTest.buildBaseWrapper(
            APPEAL_RECEIVED,
            appellant,
            representative,
            null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        wrapper.getNewSscsCaseData().setBenefitCode("093");
        byte[] letter = pdfLetterService.generateLetter(wrapper, notification,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT, appellant, appellant));
        assertTrue(ArrayUtils.isNotEmpty(letter));

        ArgumentCaptor<Map<String, Object>> placeholderCaptor = ArgumentCaptor.forClass(Map.class);

        verify(docmosisPdfService).createPdfFromMap(placeholderCaptor.capture(), eq(notification.getDocmosisLetterTemplate()));
        assertEquals("Jimmy lastName", placeholderCaptor.getValue().get(ADDRESS_NAME));
        assertEquals("line1", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_1));
        assertEquals("line2", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_2));
        assertEquals("town", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_3));
        assertEquals("county", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_4));
        assertEquals(IBCA_URL, placeholderCaptor.getValue().get(SSCS_URL_LITERAL));
        assertEquals("L2 5UZ", placeholderCaptor.getValue().get(LETTER_ADDRESS_POSTCODE));
        assertNull(placeholderCaptor.getValue().get(WELSH_GENERATED_DATE_LITERAL));
    }

    @Test
    public void givenAnAddressForARepWithNoName_thenAddressToOrganisationIfExists() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        Mockito.reset(docmosisPdfService);
        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn(baos.toByteArray());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn(baos.toByteArray());
        baos.close();
        doc.close();

        Address address = Address.builder()
            .line1("FirstLine")
            .line2("SecondLine")
            .town("Town")
            .county("County")
            .postcode("L2 5UZ").build();

        representative.setName(null);
        representative.setAddress(address);
        representative.setOrganisation("My Company");

        NotificationWrapper wrapper = NotificationProcessingServiceTest.buildBaseWrapper(
            APPEAL_RECEIVED,
            appellant,
            representative,
            null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        byte[] letter = pdfLetterService.generateLetter(wrapper, notification,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE, appellant, representative));
        assertTrue(ArrayUtils.isNotEmpty(letter));

        ArgumentCaptor<Map<String, Object>> placeholderCaptor = ArgumentCaptor.forClass(Map.class);

        verify(docmosisPdfService).createPdfFromMap(placeholderCaptor.capture(), eq(notification.getDocmosisLetterTemplate()));
        
        assertEquals("My Company", placeholderCaptor.getValue().get(ADDRESS_NAME));
        assertEquals("FirstLine", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_1));
        assertEquals("SecondLine", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_2));
        assertEquals("Town", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_3));
        assertEquals("County", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_4));
        assertEquals("L2 5UZ", placeholderCaptor.getValue().get(LETTER_ADDRESS_POSTCODE));
    }

    @Test
    public void givenAnAddressForARepWithNoName_thenAddressToSirAndMadamIfOrganisationDoesNotExists() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        Mockito.reset(docmosisPdfService);
        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn(baos.toByteArray());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn(baos.toByteArray());
        baos.close();
        doc.close();

        Address address = Address.builder()
            .line1("FirstLine")
            .line2("SecondLine")
            .town("Town")
            .county("County")
            .postcode("L2 5UZ").build();

        representative.setName(null);
        representative.setAddress(address);
        representative.setOrganisation(null);

        NotificationWrapper wrapper = NotificationProcessingServiceTest.buildBaseWrapper(
            APPEAL_RECEIVED,
            appellant,
            representative,
            null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        byte[] letter = pdfLetterService.generateLetter(wrapper, notification,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE, appellant, representative));
        assertTrue(ArrayUtils.isNotEmpty(letter));

        ArgumentCaptor<Map<String, Object>> placeholderCaptor = ArgumentCaptor.forClass(Map.class);

        verify(docmosisPdfService).createPdfFromMap(placeholderCaptor.capture(), eq(notification.getDocmosisLetterTemplate()));
        assertEquals("Sir / Madam", placeholderCaptor.getValue().get(ADDRESS_NAME));
        assertEquals("FirstLine", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_1));
        assertEquals("SecondLine", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_2));
        assertEquals("Town", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_3));
        assertEquals("County", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_4));
        assertEquals("L2 5UZ", placeholderCaptor.getValue().get(LETTER_ADDRESS_POSTCODE));
    }

    @Test
    public void willNotGenerateALetterIfNoDocmosisTemplateExists() {
        NotificationWrapper wrapper = NotificationProcessingServiceTest.buildBaseWrapper(
            APPEAL_RECEIVED,
            appellant,
            representative,
            null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId(null).build()).placeholders(new HashMap<>()).build();

        byte[] bytes = pdfLetterService.generateLetter(wrapper, notification,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE, appellant, representative));
        verifyNoInteractions(docmosisPdfService);
        assertTrue(ArrayUtils.isEmpty(bytes));
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void willHandleLoadingAnInvalidPdf() {
        NotificationWrapper wrapper = NotificationProcessingServiceTest.buildBaseWrapper(
            APPEAL_RECEIVED,
            appellant,
            representative,
            null
        );
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("some.doc").build()).placeholders(new HashMap<>()).build();

        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn("Invalid PDF".getBytes());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn("Invalid PDF".getBytes());
        pdfLetterService.generateLetter(wrapper, notification,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE, appellant, representative));
        pdfLetterService.buildCoversheet(wrapper,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.REPRESENTATIVE, appellant, representative));
    }

    @Test
    public void givenAnAddressWithMoreThan45CharactersInEachLine_willTruncateAddressAndGenerateALetter_welsh() throws IOException {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        Mockito.reset(docmosisPdfService);
        when(docmosisPdfService.createPdfFromMap(any(), anyString())).thenReturn(baos.toByteArray());
        when(docmosisPdfService.createPdf(any(), anyString())).thenReturn(baos.toByteArray());
        baos.close();
        doc.close();

        Name name = Name.builder().firstName("Jimmy").lastName("AVeryLongNameWithLotsaAdLotsAndLotsOfCharacters").build();

        Address address = Address.builder()
            .line1("MyFirstVeryVeryLongAddressLineWithLotsOfCharacters")
            .line2("MySecondVeryVeryLongAddressLineWithLotsOfCharacters")
            .town("MyTownVeryVeryLongAddressLineWithLotsOfCharacters")
            .county("MyCountyVeryVeryLongAddressLineWithLotsOfCharacters")
            .postcode("L2 5UZ").build();

        appellant.setName(name);
        appellant.setAddress(address);

        NotificationWrapper wrapper = NotificationProcessingServiceTest.buildBaseWrapper(
            APPEAL_RECEIVED,
            appellant,
            representative,
            null
        );
        wrapper.getNewSscsCaseData().setLanguagePreferenceWelsh("Yes");
        Notification notification = Notification.builder().template(Template.builder().docmosisTemplateId("docmosis.doc").build()).placeholders(new HashMap<>()).build();
        byte[] letter = pdfLetterService.generateLetter(wrapper, notification,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, SubscriptionType.APPELLANT, appellant, appellant));
        assertTrue(ArrayUtils.isNotEmpty(letter));

        ArgumentCaptor<Map<String, Object>> placeholderCaptor = ArgumentCaptor.forClass(Map.class);

        verify(docmosisPdfService).createPdfFromMap(placeholderCaptor.capture(), eq(notification.getDocmosisLetterTemplate()));
        assertEquals("Jimmy AVeryLongNameWithLotsaAdLotsAndLotsOfCh", placeholderCaptor.getValue().get(ADDRESS_NAME));
        assertEquals("MyFirstVeryVeryLongAddressLineWithLotsOfChara", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_1));
        assertEquals("MySecondVeryVeryLongAddressLineWithLotsOfChar", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_2));
        assertEquals("MyTownVeryVeryLongAddressLineWithLotsOfCharac", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_3));
        assertEquals("MyCountyVeryVeryLongAddressLineWithLotsOfChar", placeholderCaptor.getValue().get(LETTER_ADDRESS_LINE_4));
        assertEquals("L2 5UZ", placeholderCaptor.getValue().get(LETTER_ADDRESS_POSTCODE));
        assertNotNull(placeholderCaptor.getValue().get("welsh_generated_date"));
        assertEquals("welshhmcts.png", placeholderCaptor.getValue().get(DOCMOSIS_TEMPLATES_CONFIG.getHmctsWelshImgKey()));
    }
}
