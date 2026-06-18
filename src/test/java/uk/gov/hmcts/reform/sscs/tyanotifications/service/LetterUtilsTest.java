package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_1;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_2;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_3;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_4;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_POSTCODE;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_1_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_2_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_3_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_4_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_5_LITERAL;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.DWP;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.HMCTS;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.REP_SALUTATION;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.ADDRESS_LINE_1;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.ADDRESS_LINE_2;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.ADDRESS_LINE_3;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.ADDRESS_LINE_4;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.ADDRESS_LINE_5;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.LIBERTY_TO_APPLY_REQUEST;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.SYA_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.LetterType.DOCMOSIS;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.LetterType.GOV_NOTIFY;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.LetterType.PLACEHOLDER_SERVICE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.addBlankPageAtTheEndIfOddPage;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.buildBundledLetter;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.buildBundledLetterFromPdfs;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.getAddressPlaceholders;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.getAddressToUseForLetter;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.getNameToUseForLetter;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.lines;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationServiceTest.APPELLANT_WITH_ADDRESS;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.SendNotificationServiceTest.APPELLANT_WITH_ADDRESS_AND_APPOINTEE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.SendNotificationServiceTest.REP_WITH_ADDRESS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Party;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReasonableAdjustmentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;

class LetterUtilsTest {
    private static final Subscription EMPTY_SUBSCRIPTION = Subscription.builder().build();

    private Appellant appellantWithId;
    private JointParty jointPartyWithId;
    private SscsCaseData caseData;

    @Test
    void useAppellantAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(SYA_APPEAL_CREATED, APPELLANT_WITH_ADDRESS, null,
            null);

        assertThat(getAddressToUseForLetter(wrapper,
            getSubscriptionWithType(APPELLANT, APPELLANT_WITH_ADDRESS, APPELLANT_WITH_ADDRESS))).isEqualTo(
            APPELLANT_WITH_ADDRESS.getAddress());
    }

    @Test
    void useAppointeeAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE, null, null);

        assertThat(getAddressToUseForLetter(wrapper, getSubscriptionWithType(APPOINTEE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee()))).isEqualTo(
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getAddress());
    }

    @Test
    void useRepAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE, REP_WITH_ADDRESS, null);

        assertThat(getAddressToUseForLetter(wrapper,
            getSubscriptionWithType(REPRESENTATIVE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE, REP_WITH_ADDRESS))).isEqualTo(
            REP_WITH_ADDRESS.getAddress());
    }

    @Test
    void useAppellantNameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(SYA_APPEAL_CREATED, APPELLANT_WITH_ADDRESS, null,
            null);

        assertThat(getNameToUseForLetter(wrapper,
            getSubscriptionWithType(APPELLANT, APPELLANT_WITH_ADDRESS, APPELLANT_WITH_ADDRESS))).isEqualTo(
            APPELLANT_WITH_ADDRESS.getName().getFullNameNoTitle());
    }

    @Test
    void useAppointeeNameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE, null, null);

        assertThat(getNameToUseForLetter(wrapper, getSubscriptionWithType(APPOINTEE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee()))).isEqualTo(
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getName().getFullNameNoTitle());
    }

    @Test
    void useJointPartyAddressForLetter() {
        Address jointPartyAddress = Address.builder().county("county").line1("line1").line2("line2").postcode("EN1 1AF").build();
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperJointParty(SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE, Name.builder().title("Mr").firstName("Joint").lastName("Party").build(),
            jointPartyAddress, null);
        assertThat(getAddressToUseForLetter(wrapper,
            getSubscriptionWithType(JOINT_PARTY, wrapper.getNewSscsCaseData().getJointParty(),
                wrapper.getNewSscsCaseData().getJointParty()))).isEqualTo(jointPartyAddress);
        assertThat(getNameToUseForLetter(wrapper,
            getSubscriptionWithType(JOINT_PARTY, wrapper.getNewSscsCaseData().getJointParty(),
                wrapper.getNewSscsCaseData().getJointParty()))).isEqualTo("Joint Party");
    }

    @Test
    void useAppellantAddressForJointPartyIfSameAsAppellantLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperJointParty(SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE, Name.builder().title("Mrs").firstName("Betty").lastName("Bloom").build(), null,
            null);
        Address appellantAddress = wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
        assertThat(getAddressToUseForLetter(wrapper,
            getSubscriptionWithType(JOINT_PARTY, wrapper.getNewSscsCaseData().getJointParty(),
                wrapper.getNewSscsCaseData().getJointParty()))).isEqualTo(appellantAddress);
        assertThat(getNameToUseForLetter(wrapper,
            getSubscriptionWithType(JOINT_PARTY, wrapper.getNewSscsCaseData().getJointParty(),
                wrapper.getNewSscsCaseData().getJointParty()))).isEqualTo("Betty Bloom");
    }

    @ParameterizedTest
    @MethodSource("repNamesForLetters")
    void useRepNameForLetter(Name name, String expectedResult) {
        Representative rep = Representative
            .builder()
            .name(name)
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE, rep, null);

        assertThat(getNameToUseForLetter(wrapper,
            getSubscriptionWithType(REPRESENTATIVE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE, rep))).isEqualTo(expectedResult);
    }

    @Test
    void successfulBundleLetter() throws IOException {
        byte[] sampleDirectionText = IOUtils.toByteArray(
            requireNonNull(getClass().getClassLoader().getResourceAsStream("pdf/direction-text.pdf")));
        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(
            requireNonNull(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf")));

        assertThat(buildBundledLetter(sampleDirectionCoversheet, sampleDirectionText)).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void willAddABlankPageAtTheEndIfAnOddPageIsGiven(int pages) throws IOException {
        PDDocument originalDocument = new PDDocument();

        // Create a new blank page and add it to the originalDocument
        PDPage blankPage = new PDPage();
        for (int i = 1; i <= pages; i++) {
            originalDocument.addPage(blankPage);
        }
        assertThat(originalDocument.getNumberOfPages()).isEqualTo(pages);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        originalDocument.save(baos);
        originalDocument.close();
        byte[] bytes = baos.toByteArray();
        baos.close();

        byte[] newBytes = addBlankPageAtTheEndIfOddPage(bytes);
        PDDocument newDocument = Loader.loadPDF(newBytes);
        int expectedPages = (pages % 2 == 0) ? pages : pages + 1;
        assertThat(newDocument.getNumberOfPages()).isEqualTo(expectedPages);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void buildBundledLetter_listVariant_throwsBulkPrintExceptionWhenDocumentListIsNullOrEmpty(final List<byte[]> documents) {
        assertThatThrownBy(() -> buildBundledLetter(documents))
            .isInstanceOf(BulkPrintException.class)
            .hasMessage("Failed to merge documents: document list is empty");
    }

    @Test
    void buildBundledLetter_listVariant_returnsSingleDocumentUnchangedWhenListHasOneElement() throws IOException {
        final byte[] pdf = createPdfWithPages(1);

        assertThat(buildBundledLetter(List.of(pdf))).isEqualTo(pdf);
    }

    @Test
    void buildBundledLetter_listVariant_mergesMultipleDocuments() throws IOException {
        final byte[] first = createPdfWithPages(2);
        final byte[] second = createPdfWithPages(2);

        final byte[] result = buildBundledLetter(List.of(first, second));

        try (PDDocument merged = Loader.loadPDF(result)) {
            assertThat(merged.getNumberOfPages()).isEqualTo(4);
        }
    }

    @Test
    void buildBundledLetter_listVariant_addsBlankPageBeforeMergingWhenCurrentPageCountIsOdd() throws IOException {
        final byte[] singlePage = createPdfWithPages(1);

        final byte[] result = buildBundledLetter(List.of(singlePage, singlePage));

        try (PDDocument merged = Loader.loadPDF(result)) {
            assertThat(merged.getNumberOfPages()).isEqualTo(3); // 1 + 1 blank + 1
        }
    }

    @Test
    void buildBundledLetter_listVariant_skipsNullDocumentsInList() throws IOException {
        final byte[] validPdf = createPdfWithPages(2);
        final List<byte[]> docs = new ArrayList<>();
        docs.add(validPdf);
        docs.add(null);

        final byte[] result = buildBundledLetter(docs);

        try (PDDocument merged = Loader.loadPDF(result)) {
            assertThat(merged.getNumberOfPages()).isEqualTo(2);
        }
    }

    @Test
    void buildBundledLetter_listVariant_throwsBulkPrintExceptionForInvalidFirstDocument() {
        final byte[] invalidPdf = "not a pdf".getBytes(StandardCharsets.UTF_8);
        final List<byte[]> invalidPdfList = List.of(invalidPdf, invalidPdf);

        assertThatThrownBy(() -> buildBundledLetter(invalidPdfList))
            .isInstanceOf(BulkPrintException.class)
            .hasMessageContaining("Failed to merge documents with exception");
    }

    @Test
    void buildBundledLetter_listVariant_throwsBulkPrintExceptionForInvalidSubsequentDocument() throws IOException {
        final byte[] validFirst = createPdfWithPages(1);
        final byte[] invalidSecond = "not a pdf".getBytes(StandardCharsets.UTF_8);
        final List<byte[]> documents = List.of(validFirst, invalidSecond);

        assertThatThrownBy(() -> buildBundledLetter(documents))
            .isInstanceOf(BulkPrintException.class)
            .hasMessageContaining("Failed to merge documents with exception");
    }

    @Test
    void buildBundledLetterFromPdfs_returnsSingleDocumentUnchanged() throws IOException {
        final byte[] pdfBytes = createPdfWithPages(1);

        assertThat(buildBundledLetterFromPdfs(List.of(new Pdf(pdfBytes, "test.pdf")))).isEqualTo(pdfBytes);
    }

    @Test
    void buildBundledLetterFromPdfs_mergesMultiplePdfs() throws IOException {
        final byte[] first = createPdfWithPages(2);
        final byte[] second = createPdfWithPages(2);

        final byte[] result = buildBundledLetterFromPdfs(List.of(new Pdf(first, "first.pdf"), new Pdf(second, "second.pdf")));

        try (PDDocument merged = Loader.loadPDF(result)) {
            assertThat(merged.getNumberOfPages()).isEqualTo(4);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPELLANT", "JOINT_PARTY", "APPOINTEE", "REPRESENTATIVE"})
    void isAlternativeLetterFormatRequired(SubscriptionType subscriptionType) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperWithReasonableAdjustment();
        assertThat(LetterUtils.isAlternativeLetterFormatRequired(wrapper,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, subscriptionType, null, null))).isTrue();
    }

    @Test
    void givenAnOtherParty_thenIsAlternativeLetterFormatRequired() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue
            .<OtherParty>builder()
            .value(OtherParty
                .builder()
                .id("1")
                .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .build())
            .build();
        otherPartyList.add(ccdValue);

        SscsCaseData sscsCaseData = SscsCaseData.builder().otherParties(otherPartyList).build();
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(sscsCaseData)
            .oldSscsCaseData(sscsCaseData)
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY, ccdValue.getValue(),
            ccdValue.getValue());
        subscriptionWithType.setPartyId("1");
        assertThat(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType)).isTrue();
    }

    @Test
    void givenAnOtherPartyWithAppointeeThatWantsReasonableAdjustment_thenIsAlternativeLetterFormatRequiredForAppointee() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue
            .<OtherParty>builder()
            .value(OtherParty
                .builder()
                .id("1")
                .appointee(Appointee.builder().id("2").build())
                .isAppointee("Yes")
                .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.NO).build())
                .appointeeReasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .build())
            .build();
        otherPartyList.add(ccdValue);

        SscsCaseData sscsCaseData = SscsCaseData.builder().otherParties(otherPartyList).build();
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(sscsCaseData)
            .oldSscsCaseData(sscsCaseData)
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY, ccdValue.getValue(),
            ccdValue.getValue().getAppointee());
        subscriptionWithType.setPartyId("2");
        assertThat(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType)).isTrue();
    }

    @Test
    void givenAnOtherPartyWithRepThatWantsReasonableAdjustment_thenIsAlternativeLetterFormatRequiredForOtherPartyRep() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue
            .<OtherParty>builder()
            .value(OtherParty
                .builder()
                .id("1")
                .rep(Representative.builder().id("3").hasRepresentative("Yes").build())
                .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.NO).build())
                .repReasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .build())
            .build();
        otherPartyList.add(ccdValue);

        SscsCaseData sscsCaseData = SscsCaseData.builder().otherParties(otherPartyList).build();
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(sscsCaseData)
            .oldSscsCaseData(sscsCaseData)
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY, ccdValue.getValue(),
            ccdValue.getValue().getAppointee());
        subscriptionWithType.setPartyId("3");
        assertThat(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType)).isTrue();
    }

    @Test
    void givenAnOtherPartyWithReasonableAdjustmentAndSubscriptionIsSearchingForDifferentPartyId_thenNoAlternativeLetterFormatRequired() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue
            .<OtherParty>builder()
            .value(OtherParty
                .builder()
                .id("1")
                .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .build())
            .build();
        otherPartyList.add(ccdValue);

        SscsCaseData sscsCaseData = SscsCaseData.builder().otherParties(otherPartyList).build();
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(sscsCaseData)
            .oldSscsCaseData(sscsCaseData)
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY, ccdValue.getValue(),
            ccdValue.getValue());
        subscriptionWithType.setPartyId("2");
        assertThat(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType)).isFalse();
    }

    @Test
    void givenAnOtherPartyNoReasonableAdjustmentRequired_thenNoAlternativeLetterFormatRequired() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue
            .<OtherParty>builder()
            .value(OtherParty
                .builder()
                .id("1")
                .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.NO).build())
                .build())
            .build();
        otherPartyList.add(ccdValue);

        SscsCaseData sscsCaseData = SscsCaseData.builder().otherParties(otherPartyList).build();
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(sscsCaseData)
            .oldSscsCaseData(sscsCaseData)
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY, ccdValue.getValue(),
            ccdValue.getValue());
        subscriptionWithType.setPartyId("1");
        assertThat(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"OTHER_PARTY, 4", "OTHER_PARTY, 3", "OTHER_PARTY, 2"})
    void useOtherPartyLetterNameAndAddress(SubscriptionType subscriptionType, String otherPartyId) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperOtherParty(SYA_APPEAL_CREATED,
            Appellant.builder().build(), null);
        final Address expectedAddress = getExpectedAddress(otherPartyId, wrapper);

        assertThat(LetterUtils.getAddressToUseForLetter(wrapper,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, subscriptionType, null, null, otherPartyId))).isEqualTo(expectedAddress);

        final String expectedName = getExpectedName(otherPartyId, wrapper);
        assertThat(LetterUtils.getNameToUseForLetter(wrapper,
            new SubscriptionWithType(EMPTY_SUBSCRIPTION, subscriptionType, null, null, otherPartyId))).isEqualTo(expectedName);

    }

    @BeforeEach
    void setup() {
        appellantWithId = Appellant
            .builder()
            .id("APP123456")
            .name(Name.builder().firstName("Tom").lastName("Cat").build())
            .address(Address
                .builder()
                .line1("Appellant Line 1")
                .town("Appellant Town")
                .county("Appellant County")
                .postcode("AP9 3LL")
                .build())
            .build();

        jointPartyWithId = JointParty
            .builder()
            .id("JP123456")
            .name(Name.builder().firstName("Joint").lastName("Party").build())
            .address(Address
                .builder()
                .line1("Appellant Line 1")
                .town("Appellant Town")
                .county("Appellant County")
                .postcode("AP9 3LL")
                .build())
            .build();

        Representative representative = Representative
            .builder()
            .id("REP123456")
            .name(Name.builder().title("Mr.").firstName("Representative").lastName("Appellant").build())
            .build();

        caseData = SscsCaseData
            .builder()
            .jointParty(jointPartyWithId)
            .otherParties(buildOtherPartyData())
            .appeal(Appeal.builder().appellant(appellantWithId).rep(representative).build())
            .build();
    }

    @DisplayName("When sender is appellant, representative or Joint party" + " then return name of corresponding party")
    @ParameterizedTest
    @CsvSource({"appellant,Tom Cat", "representative,Representative Appellant", "jointParty,Joint Party", "jointParty1,"})
    void testGetNameForSenderRepresentative(String senderType, String senderName) {
        DynamicList sender = new DynamicList(new DynamicListItem(senderType, senderType), new ArrayList<>());
        caseData.setOriginalSender(sender);
        assertThat(LetterUtils.getNameForSender(caseData)).isEqualTo(senderName == null ? "" : senderName);
    }

    @Test
    void testGetNameForDwpSender() {
        DynamicList sender = new DynamicList(new DynamicListItem(DWP.getCode(), DWP.getCode()), new ArrayList<>());
        caseData.setOriginalSender(sender);
        assertThat(LetterUtils.getNameForSender(caseData)).isEqualTo(DWP.getLabel());
    }

    @Test
    void testGetNameForHmctsSender() {
        DynamicList sender = new DynamicList(new DynamicListItem(HMCTS.getCode(), HMCTS.getCode()), new ArrayList<>());
        caseData.setOriginalSender(sender);
        assertThat(LetterUtils.getNameForSender(caseData)).isEqualTo(HMCTS.getLabel());
    }

    @DisplayName("When sender is null then return empty string")
    @Test
    void getNameForSender_senderIsNull_returnEmpty() {
        caseData.setOriginalSender(null);
        assertThat(LetterUtils.getNameForSender(caseData)).isEmpty();
    }

    @DisplayName("When sender is an Other Party or his representative "
        + "then return name of respective other party or his representative.")
    @ParameterizedTest
    @CsvSource({"otherPartyOP123456,Other,Party", "otherPartyRepOPREP123456,OtherParty,Representative"})
    void getOtherPartyName_senderIsValid_returnName(String senderId, String firstName, String lastName) {
        DynamicList sender = new DynamicList(new DynamicListItem(senderId, senderId), new ArrayList<>());
        caseData.setOriginalSender(sender);
        assertThat(LetterUtils.getOtherPartyName(caseData)).isEqualTo(
            Optional.of(Name.builder().firstName(firstName).lastName(lastName).build()));
    }

    @DisplayName("When sender is an invalid Other Party or his representative " + "then return empty.")
    @ParameterizedTest
    @ValueSource(strings = {"otherPartyInvalid", "otherPartyRepInvalid"})
    void getOtherPartyName_senderIsInValid_returnEmpty(String senderId) {
        DynamicList sender = new DynamicList(new DynamicListItem(senderId, senderId), new ArrayList<>());
        caseData.setOriginalSender(sender);
        assertThat(LetterUtils.getOtherPartyName(caseData)).isNotPresent();
    }

    @DisplayName("When the notification event type is other than ACTION_FURTHER_EVIDENCE or POST_HEARING_REQUEST "
        + "then return empty string.")
    @Test
    void getNotificationTypeForActionFurtherEvidence_InvalidActionType_returnEmpty() {
        DynamicList sender = new DynamicList(
            new DynamicListItem("appellant", "Other party 1 - Representative - R Basker R Nadar"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, SYA_APPEAL_CREATED);

        assertThat(
            LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, SubscriptionWithType.builder().build())).isEmpty();
    }

    @DisplayName("When sender and subscriber is appellant then return confirmation")
    @ParameterizedTest
    @ValueSource(strings = {"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    void getNotificationTypeForActionFurtherEvidence_ValidActionTypeAndValidSubscriber_returnConfirmation(
        NotificationEventType eventType) {
        DynamicList sender = new DynamicList(new DynamicListItem("appellant", "Appellant"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, eventType);
        SubscriptionWithType type = SubscriptionWithType.builder().party(appellantWithId).build();
        assertThat(LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type)).isEqualTo("confirmation");
    }

    @DisplayName("When sender is an appellant, representative or joint party and subscriber is other than sender "
        + "then return notification.")
    @ParameterizedTest
    @ValueSource(strings = {"appellant", "representative", "jointParty"})
    void getNotificationTypeForActionFurtherEvidence_ValidActionTypeAndInValidSubscriber_returnNotice(String requester) {
        DynamicList sender = new DynamicList(new DynamicListItem(requester, requester), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        Appellant appellant = Appellant
            .builder()
            .name(Name.builder().firstName("Tom").lastName("Cat").build())
            .address(Address
                .builder()
                .line1("Appellant Line 1")
                .town("Appellant Town")
                .county("Appellant County")
                .postcode("AP9 3LL")
                .build())
            .build();

        SubscriptionWithType type = SubscriptionWithType.builder().party(appellant).partyId(appellant.getId()).build();
        assertThat(LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type)).isEqualTo("notice");
    }

    @DisplayName("When sender and subscriber is a Joint party then return confirmation.")
    @ParameterizedTest
    @ValueSource(strings = {"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    void getNotificationTypeForActionFurtherEvidence_ValidJointPartySub_returnConfirmation(NotificationEventType eventType) {
        DynamicList sender = new DynamicList(new DynamicListItem("jointParty", "jointParty"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, eventType);
        SubscriptionWithType type = SubscriptionWithType.builder().party(jointPartyWithId).build();
        assertThat(LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type)).isEqualTo("confirmation");
    }

    @DisplayName("When sender is an other party and subscriber is other than sender then return notification.")
    @ParameterizedTest
    @ValueSource(strings = {"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    void getNotificationTypeForActionFurtherEvidence_InValidOtherPartySubscriber_returnNotice(NotificationEventType eventType) {
        DynamicList sender = new DynamicList(new DynamicListItem("otherParty", "otherParty"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, eventType);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("Invalid").build();

        assertThat(LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type)).isEqualTo("notice");
    }

    @DisplayName("When other party or his representative send the request "
        + "then other party and his representative should receive confirmation.")
    @ParameterizedTest
    @CsvSource({"otherPartyOP123456,OP123456", "otherPartyOP123456,otherPartyOPREP123456", "otherPartyOPREP123456,OPREP123456", "otherPartyOPREP123456,OP123456"})
    void getNotificationTypeForActionFurtherEvidence_ValidOtherPartyAndRepSub_returnConfirmation(String senderType,
        String subscriber) {
        DynamicList sender = new DynamicList(new DynamicListItem(senderType, senderType), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId(subscriber).build();
        assertThat(LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type)).isEqualTo("confirmation");
    }

    @DisplayName("When sender and subscriber is an other party or his representative then return true")
    @ParameterizedTest
    @CsvSource({"OPREP123456,OPREP123456", "OPREP123456,OP123456"})
    void isValidOtherPartyRepresentative_ValidOtherPartyAndRep_returnTrue(String senderId, String subscriptionId) {
        List<CcdValue<OtherParty>> otherParties = buildOtherPartyData();
        assertThat(LetterUtils.isValidOtherPartyRepresentative(subscriptionId, senderId, otherParties.getFirst())).isTrue();
    }

    @DisplayName("When the sender/original requester is other than representative then return false.")
    @ParameterizedTest
    @ValueSource(strings = {"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    void isValidAppellantRepresentativeForSetAsideRequest_givenNonRepresentative_thenReturnFalse(
        NotificationEventType eventType) {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyOP123456", "OPREP123456"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, eventType);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("otherPartyOPREP123456").build();
        assertThat(LetterUtils.isValidAppellantRepresentativeForSetAsideRequest(wrapper, type)).isFalse();
    }

    @DisplayName("When the sender/original requester is representative and subscriber is appellant or representative"
        + " then return true.")
    @ParameterizedTest
    @ValueSource(strings = {"APP123456", "REP123456"})
    void isValidAppellantRepresentativeForSetAsideRequest_givenValidRepresentative_thenReturnTrue(String partyId) {
        DynamicList sender = new DynamicList(new DynamicListItem("representative", "representative"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        Appellant party = Appellant.builder().id(partyId).build();
        SubscriptionWithType type = SubscriptionWithType.builder().party(party).build();
        assertThat(LetterUtils.isValidAppellantRepresentativeForSetAsideRequest(wrapper, type)).isTrue();
    }

    @DisplayName("When the subscription id is null then return false.")
    @Test
    void isValidOtherParty_givenSubscriberIsNull_thenReturnFalse() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyOPREP123456", "OPREP123456"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        SubscriptionWithType type = SubscriptionWithType.builder().build();
        assertThat(LetterUtils.isValidOtherParty(wrapper, type)).isFalse();
    }

    @DisplayName("When the subscription id is invalid then return false.")
    @ParameterizedTest
    @ValueSource(strings = {"jointParty", "otherParty"})
    void isValidOtherParty_givenSubscriberIsInValid_thenReturnFalse(String senderType) {
        DynamicList sender = new DynamicList(new DynamicListItem(senderType, senderType), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("test").build();
        assertThat(LetterUtils.isValidOtherParty(wrapper, type)).isFalse();
    }

    @DisplayName("When the other party data present and valid then return true.")
    @Test
    void isValidOtherParty_givenSubscriberIsValid_thenReturnTrue() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyOP123456", "otherParty"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("OP123456").build();
        assertThat(LetterUtils.isValidOtherParty(wrapper, type)).isTrue();
    }

    NotificationSscsCaseDataWrapper buildBaseWrapperWithCaseData(SscsCaseData caseData, NotificationEventType type) {
        return NotificationSscsCaseDataWrapper.builder().newSscsCaseData(caseData).notificationEventType(type).build();
    }

    @DisplayName("lines returns expected values when given a valid UK address object.")
    @Test
    void lines_returns_expected_for_UK_address() {
        Address testAddress = Address
            .builder()
            .line1("Somerset House")
            .line2("Strand")
            .town("London")
            .county("Greater London")
            .postcode("WC2R 1LA")
            .build();

        List<String> addressLines = lines(testAddress);
        List<String> expectedLines = List.of("Somerset House", "Strand", "London", "Greater London", "WC2R 1LA");

        for (int i = 0; i < addressLines.size(); i++) {
            assertThat(addressLines.get(i)).isEqualTo(expectedLines.get(i));
        }
    }

    @DisplayName("lines returns expected values when given a shorter UK address object.")
    @Test
    void lines_returns_expected_for_short_UK_address() {
        Address testAddress = Address.builder().line1("Elba").town("Duns").postcode("TD11 3RY").build();

        List<String> addressLines = lines(testAddress);
        List<String> expectedLines = List.of("Elba", "Duns", "TD11 3RY");

        for (int i = 0; i < addressLines.size(); i++) {
            assertThat(addressLines.get(i)).isEqualTo(expectedLines.get(i));
        }
    }

    @DisplayName("lines returns expected values when given a valid UK address object with inMainlandUK YES.")
    @Test
    void lines_returns_expected_for_UK_address_with_inMainlandUK() {
        Address testAddress = Address
            .builder()
            .line1("Somerset House")
            .line2("Strand")
            .town("London")
            .county("Greater London")
            .postcode("WC2R 1LA")
            .inMainlandUk(YesNo.YES)
            .build();

        List<String> addressLines = lines(testAddress);
        List<String> expectedLines = List.of("Somerset House", "Strand", "London", "Greater London", "WC2R 1LA");

        for (int i = 0; i < addressLines.size(); i++) {
            assertThat(addressLines.get(i)).isEqualTo(expectedLines.get(i));
        }
    }

    @DisplayName("lines returns expected values when given a valid international address object.")
    @Test
    void lines_returns_expected_for_International_address() {
        Address testAddress = Address
            .builder()
            .line1("Catherdrale Notre-Dame de Paris")
            .line2("6 Parvis Notre-dame - Pl. Jean-Paul II")
            .town("Paris")
            .county("Ile-de-France")
            .postcode("75004")
            .country("France")
            .inMainlandUk(YesNo.NO)
            .build();

        List<String> addressLines = lines(testAddress);
        List<String> expectedLines = List.of("Catherdrale Notre-Dame de Paris", "6 Parvis Notre-dame - Pl. Jean-Paul II", "Paris",
            "75004", "France");

        for (int i = 0; i < addressLines.size(); i++) {
            assertThat(addressLines.get(i)).isEqualTo(expectedLines.get(i));
        }
    }

    @DisplayName("lines returns expected values when international address object doesn't include postcode.")
    @Test
    void lines_returns_expected_for_International_address_without_Postcode() {
        Address testAddress = Address
            .builder()
            .line1("Catherdrale Notre-Dame de Paris")
            .line2("6 Parvis Notre-dame - Pl. Jean-Paul II")
            .town("Paris")
            .county("Ile-de-France")
            .country("France")
            .inMainlandUk(YesNo.NO)
            .build();

        List<String> addressLines = lines(testAddress);
        List<String> expectedLines = List.of("Catherdrale Notre-Dame de Paris", "6 Parvis Notre-dame - Pl. Jean-Paul II", "Paris",
            "France");

        for (int i = 0; i < addressLines.size(); i++) {
            assertThat(addressLines.get(i)).isEqualTo(expectedLines.get(i));
        }
    }

    @DisplayName("lines filters out null address lines for UK addresses.")
    @Test
    void lines_removes_null_values() {
        Address testAddress = Address
            .builder()
            .line1("Test House")
            .line2(null)
            .town("Test Town")
            .county("Test County")
            .postcode("Test Postcode")
            .build();

        List<String> addressLines = lines(testAddress);
        List<String> expectedLines = List.of("Test House", "Test Town", "Test County", "Test Postcode");

        for (int i = 0; i < addressLines.size(); i++) {
            assertThat(addressLines.get(i)).isEqualTo(expectedLines.get(i));
        }
    }

    @DisplayName("getAddressPlaceholders returns expected values for a UK address.")
    @Test
    void getAddressPlaceholders_returnsExpectedValuesUkAddress() {
        Address testAddress = Address
            .builder()
            .line1("Somerset House")
            .line2("Strand")
            .town("London")
            .county("Greater London")
            .postcode("WC2R 1LA")
            .inMainlandUk(YesNo.YES)
            .build();

        Map<String, Object> placeholders = getAddressPlaceholders(testAddress, true, DOCMOSIS);

        assertThat(placeholders)
            .containsEntry(LETTER_ADDRESS_LINE_1, "Somerset House")
            .containsEntry(LETTER_ADDRESS_LINE_2, "Strand")
            .containsEntry(LETTER_ADDRESS_LINE_3, "London")
            .containsEntry(LETTER_ADDRESS_LINE_4, "Greater London")
            .containsEntry(LETTER_ADDRESS_POSTCODE, "WC2R 1LA");

    }

    @DisplayName("getAddressPlaceholders returns expected values for a UK address.")
    @Test
    void getAddressPlaceholders_returnsExpectedValuesShortUkAddress() {
        Address testAddress = Address.builder().line1("Elba").town("Duns").postcode("TD11 3RY").build();

        Map<String, Object> placeholders = getAddressPlaceholders(testAddress, true, DOCMOSIS);

        assertThat(placeholders)
            .containsEntry(LETTER_ADDRESS_LINE_1, "Elba")
            .containsEntry(LETTER_ADDRESS_LINE_2, "Duns")
            .containsEntry(LETTER_ADDRESS_LINE_3, "TD11 3RY");
    }

    @DisplayName("getAddressPlaceholders returns expected values when using sendLetterNotificationToAddress address placeholders.")
    @Test
    void getAddressPlaceholders_returnsExpectedValuesWhenUsingSendLetterNotificationToAddressPlaceholders() {
        String fullNameNoTitle = "Jane Doe";
        Address testAddress = Address
            .builder()
            .line1("Somerset House")
            .line2("Strand")
            .town("London")
            .county("Greater London")
            .postcode("WC2R 1LA")
            .inMainlandUk(YesNo.YES)
            .build();

        Map<String, Object> placeholders = getAddressPlaceholders(testAddress, true, GOV_NOTIFY);

        placeholders.put(ADDRESS_LINE_1, fullNameNoTitle);

        assertThat(placeholders)
            .containsEntry(ADDRESS_LINE_1, "Jane Doe")
            .containsEntry(ADDRESS_LINE_2, "Somerset House")
            .containsEntry(ADDRESS_LINE_3, "Strand")
            .containsEntry(ADDRESS_LINE_4, "London")
            .containsEntry(ADDRESS_LINE_5, "Greater London")
            .containsEntry(POSTCODE_LITERAL, "WC2R 1LA");

    }

    @Test
    void getAddressPlaceholders_returnsExpectedValuesInternationalAddress() {
        Address testAddress = Address
            .builder()
            .line1("Catherdrale Notre-Dame de Paris")
            .line2("6 Parvis Notre-dame - Pl. Jean-Paul II")
            .town("Paris")
            .county("Ile-de-France")
            .country("France")
            .inMainlandUk(YesNo.NO)
            .build();

        Map<String, Object> placeholders = getAddressPlaceholders(testAddress, true, DOCMOSIS);

        assertThat(placeholders)
            .containsEntry(LETTER_ADDRESS_LINE_1, "Catherdrale Notre-Dame de Paris")
            .containsEntry(LETTER_ADDRESS_LINE_2, "6 Parvis Notre-dame - Pl. Jean-Paul II")
            .containsEntry(LETTER_ADDRESS_LINE_3, "Paris")
            .containsEntry(LETTER_ADDRESS_LINE_4, "France");

    }

    @Test
    void getAddressPlaceholders_returnsExpectedValuesInternationalAddressNoPostcode() {
        Address testAddress = Address
            .builder()
            .line1("Catherdrale Notre-Dame de Paris")
            .line2("6 Parvis Notre-dame - Pl. Jean-Paul II")
            .town("Paris")
            .county("Ile-de-France")
            .postcode("75004")
            .country("France")
            .inMainlandUk(YesNo.NO)
            .build();

        Map<String, Object> placeholders = getAddressPlaceholders(testAddress, true, DOCMOSIS);

        assertThat(placeholders)
            .containsEntry(LETTER_ADDRESS_LINE_1, "Catherdrale Notre-Dame de Paris")
            .containsEntry(LETTER_ADDRESS_LINE_2, "6 Parvis Notre-dame - Pl. Jean-Paul II")
            .containsEntry(LETTER_ADDRESS_LINE_3, "Paris")
            .containsEntry(LETTER_ADDRESS_LINE_4, "75004")
            .containsEntry(LETTER_ADDRESS_POSTCODE, "France");
    }

    @DisplayName("getAddressPlaceholders returns the expected keys for Docmosis.")
    @Test
    void getAddressPlaceholders_returnsExpectedKeys_for_Docmosis() {
        Address testAddress = Address
            .builder()
            .line1("Catherdrale Notre-Dame de Paris")
            .line2("6 Parvis Notre-dame - Pl. Jean-Paul II")
            .town("Paris")
            .county("Ile-de-France")
            .postcode("75004")
            .country("France")
            .inMainlandUk(YesNo.NO)
            .build();

        List<String> addressConstants = List.of(LETTER_ADDRESS_LINE_1, LETTER_ADDRESS_LINE_2, LETTER_ADDRESS_LINE_3,
            LETTER_ADDRESS_LINE_4, LETTER_ADDRESS_POSTCODE);

        Map<String, Object> actualPlaceholders = getAddressPlaceholders(testAddress, true, DOCMOSIS);

        for (String addressConstant : addressConstants) {
            assertThat(
                actualPlaceholders.containsKey(addressConstant) && actualPlaceholders.get(addressConstant) != null).isTrue();
        }
    }

    @DisplayName("getAddressPlaceholders returns the expected keys for Gov_Notify.")
    @Test
    void getAddressPlaceholders_returnsExpectedKeys_for_Gov_Notify() {
        String fullNameNoTitle = "Jane Doe";
        Address testAddress = Address
            .builder()
            .line1("Catherdrale Notre-Dame de Paris")
            .line2("6 Parvis Notre-dame - Pl. Jean-Paul II")
            .town("Paris")
            .county("Ile-de-France")
            .postcode("75004")
            .country("France")
            .inMainlandUk(YesNo.NO)
            .build();

        List<String> addressConstants = List.of(ADDRESS_LINE_2, ADDRESS_LINE_3, ADDRESS_LINE_4, ADDRESS_LINE_5, POSTCODE_LITERAL);

        Map<String, Object> actualPlaceholders = getAddressPlaceholders(testAddress, true, GOV_NOTIFY);

        actualPlaceholders.put(ADDRESS_LINE_1, fullNameNoTitle);

        for (String addressConstant : addressConstants) {
            assertThat(
                actualPlaceholders.containsKey(addressConstant) && actualPlaceholders.get(addressConstant) != null).isTrue();
        }
    }

    @DisplayName("getAddressPlaceholders returns the expected keys for Placeholder_Service.")
    @Test
    void getAddressPlaceholders_returnsExpectedKeys_for_Placeholder_Service() {
        Address testAddress = Address
            .builder()
            .line1("Catherdrale Notre-Dame de Paris")
            .line2("6 Parvis Notre-dame - Pl. Jean-Paul II")
            .town("Paris")
            .county("Ile-de-France")
            .postcode("75004")
            .country("France")
            .inMainlandUk(YesNo.NO)
            .build();

        List<String> addressConstants = List.of(RECIPIENT_ADDRESS_LINE_1_LITERAL, RECIPIENT_ADDRESS_LINE_2_LITERAL,
            RECIPIENT_ADDRESS_LINE_3_LITERAL, RECIPIENT_ADDRESS_LINE_4_LITERAL, RECIPIENT_ADDRESS_LINE_5_LITERAL);

        Map<String, Object> actualPlaceholders = getAddressPlaceholders(testAddress, true, PLACEHOLDER_SERVICE);

        for (String addressConstant : addressConstants) {
            assertThat(
                actualPlaceholders.containsKey(addressConstant) && actualPlaceholders.get(addressConstant) != null).isTrue();
        }
    }

    @DisplayName("getAddressPlaceholders truncates address lines over 45 characters when truncate is true.")
    @Test
    void getAddressPlaceholders_truncatesLongAddressLinesWhenTruncateTrue() {
        Address testAddress = Address
            .builder()
            .line1(
                "This is a very long test address, it should be truncated to a shorter string when getAddressPlaceholders truncate argument is true")
            .line2("Test Street")
            .town("Test Town")
            .county("Test County")
            .postcode("TT1 1TT")
            .build();

        Map<String, Object> placeholders = getAddressPlaceholders(testAddress, true, GOV_NOTIFY);

        assertThat(placeholders)
            .containsEntry(ADDRESS_LINE_2, "This is a very long test address, it should b")
            .containsEntry(ADDRESS_LINE_3, "Test Street")
            .containsEntry(ADDRESS_LINE_4, "Test Town")
            .containsEntry(ADDRESS_LINE_5, "Test County")
            .containsEntry(POSTCODE_LITERAL, "TT1 1TT");
    }

    @DisplayName("getAddressPlaceholders keeps address lines long when truncate is false.")
    @Test
    void getAddressPlaceholders_keepsLongAddressLinesWhenTruncateFalse() {
        Address testAddress = Address
            .builder()
            .line1(
                "This is a very long test address, it should be long string when getAddressPlaceholders truncate argument is false")
            .line2("Test Street")
            .town("Test Town")
            .county("Test County")
            .postcode("TT1 1TT")
            .build();

        Map<String, Object> placeholders = getAddressPlaceholders(testAddress, false, GOV_NOTIFY);

        assertThat(placeholders)
            .containsEntry(ADDRESS_LINE_2,
                "This is a very long test address, it should be long string when getAddressPlaceholders truncate argument is false")
            .containsEntry(ADDRESS_LINE_3, "Test Street")
            .containsEntry(ADDRESS_LINE_4, "Test Town")
            .containsEntry(ADDRESS_LINE_5, "Test County")
            .containsEntry(POSTCODE_LITERAL, "TT1 1TT");

    }

    private static Stream<Object[]> repNamesForLetters() {
        return Stream.of(new Object[]{Name.builder().firstName("Re").lastName("Presentative").build(), "Re Presentative"},
            new Object[]{Name.builder().build(), REP_SALUTATION},
            new Object[]{Name.builder().firstName("undefined").lastName("undefined").build(), REP_SALUTATION});
    }

    @NotNull
    private SubscriptionWithType getSubscriptionWithType(SubscriptionType subscriptionType, Party party, Entity entity) {
        return new SubscriptionWithType(Subscription.builder().build(), subscriptionType, party, entity);
    }

    private byte[] createPdfWithPages(final int numberOfPages) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            for (int i = 0; i < numberOfPages; i++) {
                document.addPage(new PDPage(PDRectangle.A4));
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private Address getExpectedAddress(final String otherPartyId, final NotificationWrapper wrapper) {
        return requireNonNull(wrapper
            .getNewSscsCaseData()
            .getOtherParties()
            .stream()
            .map(CcdValue::getValue)
            .flatMap(
                op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getId(), op.getAppointee().getAddress()) : null,
                    Pair.of(op.getId(), op.getAddress()),
                    (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), op.getRep().getAddress()) : null))
            .filter(Objects::nonNull)
            .filter(p -> p.getRight() != null && p.getLeft() != null)
            .filter(pair -> pair.getLeft().equals(String.valueOf(otherPartyId)))
            .findFirst()
            .map(Pair::getRight)
            .orElse(null));
    }

    private String getExpectedName(final String otherPartyId, final NotificationWrapper wrapper) {
        return requireNonNull(wrapper
            .getNewSscsCaseData()
            .getOtherParties()
            .stream()
            .map(CcdValue::getValue)
            .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getId(), op.getAppointee().getName()) : null,
                Pair.of(op.getId(), op.getName()),
                (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), op.getRep().getName()) : null))
            .filter(Objects::nonNull)
            .filter(p -> p.getRight() != null && p.getLeft() != null)
            .filter(pair -> pair.getLeft().equals(String.valueOf(otherPartyId)))
            .findFirst()
            .map(Pair::getRight)
            .map(Name::getFullNameNoTitle)
            .orElse(""));
    }

    private List<CcdValue<OtherParty>> buildOtherPartyData() {
        return List.of(CcdValue
            .<OtherParty>builder()
            .value(OtherParty
                .builder()
                .id("OP123456")
                .name(Name.builder().firstName("Other").lastName("Party").build())
                .otherPartySubscription(Subscription.builder().email("other@party").subscribeEmail("Yes").build())
                .rep(Representative
                    .builder()
                    .id("OPREP123456")
                    .name(Name.builder().firstName("OtherParty").lastName("Representative").build())
                    .hasRepresentative(YesNo.YES.getValue())
                    .build())
                .build())
            .build(), CcdValue
            .<OtherParty>builder()
            .value(OtherParty
                .builder()
                .id("OP7890123")
                .name(Name.builder().firstName("Other 1").lastName("Party 1").build())
                .otherPartySubscription(Subscription.builder().email("other@party").subscribeEmail("Yes").build())
                .rep(Representative.builder().hasRepresentative(YesNo.NO.getValue()).build())
                .build())
            .build());
    }
}