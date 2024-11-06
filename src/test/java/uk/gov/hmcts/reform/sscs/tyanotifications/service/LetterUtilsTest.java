package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.DWP;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.HMCTS;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.REP_SALUTATION;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationServiceTest.APPELLANT_WITH_ADDRESS;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.SendNotificationServiceTest.APPELLANT_WITH_ADDRESS_AND_APPOINTEE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.SendNotificationServiceTest.REP_WITH_ADDRESS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class LetterUtilsTest {
    private static final Subscription EMPTY_SUBSCRIPTION = Subscription.builder().build();

    private Appellant appellantWithId;
    private JointParty jointPartyWithId;
    private SscsCaseData caseData;

    @Test
    public void useAppellantAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS.getAddress(), getAddressToUseForLetter(wrapper,
            getSubscriptionWithType(APPELLANT, APPELLANT_WITH_ADDRESS, APPELLANT_WITH_ADDRESS)));
    }

    @NotNull
    private SubscriptionWithType getSubscriptionWithType(SubscriptionType subscriptionType, Party party, Entity entity) {
        return new SubscriptionWithType(Subscription.builder().build(), subscriptionType, party, entity);
    }

    @Test
    public void useAppointeeAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getAddress(),
            getAddressToUseForLetter(wrapper, getSubscriptionWithType(APPOINTEE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
                APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee())));
    }

    @Test
    public void useRepAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            REP_WITH_ADDRESS,
            null
        );

        assertEquals(REP_WITH_ADDRESS.getAddress(), getAddressToUseForLetter(wrapper,
            getSubscriptionWithType(REPRESENTATIVE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE, REP_WITH_ADDRESS)));
    }

    @Test
    public void useAppellantNameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS.getName().getFullNameNoTitle(),
            getNameToUseForLetter(wrapper, getSubscriptionWithType(APPELLANT, APPELLANT_WITH_ADDRESS,
                APPELLANT_WITH_ADDRESS)));
    }

    @Test
    public void useAppointeeNameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getName().getFullNameNoTitle(),
            getNameToUseForLetter(wrapper,
                getSubscriptionWithType(APPOINTEE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
                    APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee())));
    }

    @Test
    public void useJointPartyAddressForLetter() {
        Address jointPartyAddress = Address.builder().county("county").line1("line1").line2("line2").postcode("EN1 1AF").build();
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperJointParty(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            Name.builder().title("Mr").firstName("Joint").lastName("Party").build(),
            jointPartyAddress,
            null
        );
        assertEquals(jointPartyAddress, getAddressToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY,
            wrapper.getNewSscsCaseData().getJointParty(), wrapper.getNewSscsCaseData().getJointParty())));
        assertEquals("Joint Party", getNameToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY,
            wrapper.getNewSscsCaseData().getJointParty(), wrapper.getNewSscsCaseData().getJointParty())));
    }

    @Test
    public void useAppellantAddressForJointPartyIfSameAsAppellantLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperJointParty(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            Name.builder()
                .title("Mrs")
                .firstName("Betty")
                .lastName("Bloom")
                .build(),
            null,
            null
        );
        Address appellantAddress = wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
        assertEquals(appellantAddress, getAddressToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY,
            wrapper.getNewSscsCaseData().getJointParty(), wrapper.getNewSscsCaseData().getJointParty())));
        assertEquals("Betty Bloom", getNameToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY,
            wrapper.getNewSscsCaseData().getJointParty(), wrapper.getNewSscsCaseData().getJointParty())));
    }

    @Test
    @Parameters(method = "repNamesForLetters")
    public void useRepNameForLetter(Name name, String expectedResult) {
        Representative rep = Representative.builder()
            .name(name)
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            rep,
            null
        );

        assertEquals(expectedResult, getNameToUseForLetter(wrapper,
            getSubscriptionWithType(REPRESENTATIVE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE, rep)));
    }

    private Object[] repNamesForLetters() {

        return new Object[]{
            new Object[]{Name.builder().firstName("Re").lastName("Presentative").build(), "Re Presentative"},
            new Object[]{Name.builder().build(), REP_SALUTATION},
            new Object[]{Name.builder().firstName("undefined").lastName("undefined").build(), REP_SALUTATION}
        };
    }

    @Test
    public void successfulBundleLetter() throws IOException {
        byte[] sampleDirectionText = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-text.pdf"));
        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));

        assertNotNull(buildBundledLetter(sampleDirectionCoversheet, sampleDirectionText));
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldNotBundleLetterWhenCoverSheetIsNull() throws IOException {
        byte[] sampleDirectionText = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-text.pdf"));

        buildBundledLetter(null, sampleDirectionText);
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldNotBundleLetterWhenAttachmentIsNull() throws IOException {
        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));

        buildBundledLetter(sampleDirectionCoversheet, null);
    }

    @Test
    @Parameters({"1", "2", "3", "4"})
    public void willAddABlankPageAtTheEndIfAnOddPageIsGiven(int pages) throws IOException {
        PDDocument originalDocument = new PDDocument();

        // Create a new blank page and add it to the originalDocument
        PDPage blankPage = new PDPage();
        for (int i = 1; i <= pages; i++) {
            originalDocument.addPage(blankPage);
        }
        assertEquals(pages, originalDocument.getNumberOfPages());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        originalDocument.save(baos);
        originalDocument.close();
        byte[] bytes = baos.toByteArray();
        baos.close();

        byte[] newBytes = addBlankPageAtTheEndIfOddPage(bytes);
        PDDocument newDocument = PDDocument.load(newBytes);
        int expectedPages = (pages % 2 == 0) ? pages : pages + 1;
        assertEquals(expectedPages, newDocument.getNumberOfPages());
    }

    @Test
    @Parameters({"APPELLANT", "JOINT_PARTY", "APPOINTEE", "REPRESENTATIVE"})
    public void isAlternativeLetterFormatRequired(SubscriptionType subscriptionType) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperWithReasonableAdjustment();
        assertTrue(LetterUtils.isAlternativeLetterFormatRequired(wrapper, new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            subscriptionType, null, null)));
    }

    @Test
    public void givenAnOtherParty_thenIsAlternativeLetterFormatRequired() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .id("1")
            .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
            .build()).build();
        otherPartyList.add(ccdValue);

        SscsCaseData caseData = SscsCaseData.builder()
            .otherParties(otherPartyList).build();
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(caseData)
            .oldSscsCaseData(caseData)
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY,
            ccdValue.getValue(), ccdValue.getValue());
        subscriptionWithType.setPartyId("1");
        assertTrue(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType));
    }

    @Test
    public void givenAnOtherPartyWithAppointeeThatWantsReasonableAdjustment_thenIsAlternativeLetterFormatRequiredForAppointee() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .id("1")
            .appointee(Appointee.builder().id("2").build())
            .isAppointee("Yes")
            .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.NO).build())
            .appointeeReasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
            .build()).build();
        otherPartyList.add(ccdValue);

        SscsCaseData caseData = SscsCaseData.builder()
            .otherParties(otherPartyList).build();
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(caseData)
            .oldSscsCaseData(caseData)
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY,
            ccdValue.getValue(), ccdValue.getValue().getAppointee());
        subscriptionWithType.setPartyId("2");
        assertTrue(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType));
    }

    @Test
    public void givenAnOtherPartyWithRepThatWantsReasonableAdjustment_thenIsAlternativeLetterFormatRequiredForOtherPartyRep() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .id("1")
            .rep(Representative.builder().id("3").hasRepresentative("Yes").build())
            .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.NO).build())
            .repReasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
            .build()).build();
        otherPartyList.add(ccdValue);

        SscsCaseData caseData = SscsCaseData.builder()
            .otherParties(otherPartyList).build();
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(caseData)
            .oldSscsCaseData(caseData)
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY,
            ccdValue.getValue(), ccdValue.getValue().getAppointee());
        subscriptionWithType.setPartyId("3");
        assertTrue(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType));
    }

    @Test
    public void givenAnOtherPartyWithReasonableAdjustmentAndSubscriptionIsSearchingForDifferentPartyId_thenNoAlternativeLetterFormatRequired() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .id("1")
            .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
            .build()).build();
        otherPartyList.add(ccdValue);

        SscsCaseData caseData = SscsCaseData.builder()
            .otherParties(otherPartyList).build();
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(caseData)
            .oldSscsCaseData(caseData)
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY,
            ccdValue.getValue(), ccdValue.getValue());
        subscriptionWithType.setPartyId("2");
        assertFalse(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType));
    }

    @Test
    public void givenAnOtherPartyNoReasonableAdjustmentRequired_thenNoAlternativeLetterFormatRequired() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .id("1")
            .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.NO).build())
            .build()).build();
        otherPartyList.add(ccdValue);

        SscsCaseData caseData = SscsCaseData.builder()
            .otherParties(otherPartyList).build();
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(caseData)
            .oldSscsCaseData(caseData)
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY,
            ccdValue.getValue(), ccdValue.getValue());
        subscriptionWithType.setPartyId("1");
        assertFalse(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType));
    }

    @Test
    @Parameters({"OTHER_PARTY, 4", "OTHER_PARTY, 3", "OTHER_PARTY, 2"})
    public void useOtherPartyLetterNameAndAddress(SubscriptionType subscriptionType, String otherPartyId) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperOtherParty(SYA_APPEAL_CREATED, Appellant.builder().build(), null);
        final Address expectedAddress = getExpectedAddress(otherPartyId, wrapper);

        assertThat(LetterUtils.getAddressToUseForLetter(wrapper,
                new SubscriptionWithType(EMPTY_SUBSCRIPTION, subscriptionType, null, null, otherPartyId)),
            is(expectedAddress));

        final String expectedName = getExpectedName(otherPartyId, wrapper);
        assertThat(LetterUtils.getNameToUseForLetter(wrapper,
                new SubscriptionWithType(EMPTY_SUBSCRIPTION, subscriptionType, null, null, otherPartyId)),
            is(expectedName));

    }

    private Address getExpectedAddress(final String otherPartyId, final NotificationWrapper wrapper) {
        return requireNonNull(wrapper.getNewSscsCaseData().getOtherParties().stream()
            .map(CcdValue::getValue)
            .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getId(), op.getAppointee().getAddress()) : null, Pair.of(op.getId(), op.getAddress()), (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), op.getRep().getAddress()) : null))
            .filter(Objects::nonNull)
            .filter(p -> p.getRight() != null && p.getLeft() != null)
            .filter(pair -> pair.getLeft().equals(String.valueOf(otherPartyId)))
            .findFirst()
            .map(Pair::getRight).orElse(null));
    }

    private String getExpectedName(final String otherPartyId, final NotificationWrapper wrapper) {
        return requireNonNull(wrapper.getNewSscsCaseData().getOtherParties().stream()
            .map(CcdValue::getValue)
            .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getId(), op.getAppointee().getName()) : null, Pair.of(op.getId(), op.getName()), (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), op.getRep().getName()) : null))
            .filter(Objects::nonNull)
            .filter(p -> p.getRight() != null && p.getLeft() != null)
            .filter(pair -> pair.getLeft().equals(String.valueOf(otherPartyId)))
            .findFirst()
            .map(Pair::getRight)
            .map(Name::getFullNameNoTitle)
            .orElse(""));
    }

    @Before
    public void setup() {
        appellantWithId = Appellant.builder()
            .id("APP123456")
            .name(Name.builder().firstName("Tom").lastName("Cat").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .build();

        jointPartyWithId = JointParty.builder()
            .id("JP123456")
            .name(Name.builder().firstName("Joint").lastName("Party").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .build();

        Representative representative = Representative.builder()
            .id("REP123456")
            .name(Name.builder()
                .title("Mr.")
                .firstName("Representative")
                .lastName("Appellant")
                .build())
            .build();

        caseData = SscsCaseData.builder()
            .jointParty(jointPartyWithId)
            .otherParties(buildOtherPartyData())
            .appeal(Appeal.builder()
                .appellant(appellantWithId)
                .rep(representative)
                .build())
            .build();
    }

    private List<CcdValue<OtherParty>> buildOtherPartyData() {
        return List.of(CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                    .id("OP123456")
                    .name(Name.builder()
                        .firstName("Other")
                        .lastName("Party")
                        .build())
                    .otherPartySubscription(Subscription.builder().email("other@party").subscribeEmail("Yes").build())
                    .rep(Representative.builder()
                        .id("OPREP123456")
                        .name(Name.builder()
                            .firstName("OtherParty")
                            .lastName("Representative")
                            .build())
                        .hasRepresentative(YesNo.YES.getValue())
                        .build())
                    .build())
                .build(),
            CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                    .id("OP7890123")
                    .name(Name.builder()
                        .firstName("Other 1")
                        .lastName("Party 1")
                        .build())
                    .otherPartySubscription(Subscription.builder().email("other@party").subscribeEmail("Yes").build())
                    .rep(Representative.builder()
                        .hasRepresentative(YesNo.NO.getValue())
                        .build())
                    .build())
                .build()
        );
    }

    @DisplayName("When sender is appellant, representative or Joint party"
        + " then return name of corresponding party")
    @Test
    @Parameters({"appellant,Tom Cat", "representative,Representative Appellant", "jointParty,Joint Party", "jointParty1, "})
    public void testGetNameForSenderRepresentative(String senderType, String senderName) {
        DynamicList sender = new DynamicList(new DynamicListItem(senderType, senderType), new ArrayList<>());
        caseData.setOriginalSender(sender);
        assertEquals(senderName, LetterUtils.getNameForSender(caseData));
    }

    @Test
    public void testGetNameForDwpSender() {
        DynamicList sender = new DynamicList(new DynamicListItem(DWP.getCode(), DWP.getCode()), new ArrayList<>());
        caseData.setOriginalSender(sender);
        assertEquals(DWP.getLabel(), LetterUtils.getNameForSender(caseData));
    }

    @Test
    public void testGetNameForHmctsSender() {
        DynamicList sender = new DynamicList(new DynamicListItem(HMCTS.getCode(), HMCTS.getCode()), new ArrayList<>());
        caseData.setOriginalSender(sender);
        assertEquals(HMCTS.getLabel(), LetterUtils.getNameForSender(caseData));
    }

    @DisplayName("When sender is null then return empty string")
    @Test
    public void getNameForSender_senderIsNull_returnEmpty() {
        caseData.setOriginalSender(null);
        assertEquals("", LetterUtils.getNameForSender(caseData));
    }

    @DisplayName("When sender is an Other Party or his representative "
        + "then return name of respective other party or his representative.")
    @Test
    @Parameters({"otherPartyOP123456,Other,Party", "otherPartyRepOPREP123456,OtherParty,Representative"})
    public void getOtherPartyName_senderIsValid_returnName(String senderId, String firstName, String lastName) {
        DynamicList sender = new DynamicList(new DynamicListItem(senderId, senderId), new ArrayList<>());
        caseData.setOriginalSender(sender);
        assertEquals(Optional.of(Name.builder()
            .firstName(firstName)
            .lastName(lastName)
            .build()), LetterUtils.getOtherPartyName(caseData));
    }

    @DisplayName("When sender is an invalid Other Party or his representative "
        + "then return empty.")
    @Test
    @Parameters({"otherPartyInvalid", "otherPartyRepInvalid"})
    public void getOtherPartyName_senderIsInValid_returnEmpty(String senderId) {
        DynamicList sender = new DynamicList(new DynamicListItem(senderId, senderId), new ArrayList<>());
        caseData.setOriginalSender(sender);
        assertEquals(Optional.empty(), LetterUtils.getOtherPartyName(caseData));
    }

    @DisplayName("When the notification event type is other than ACTION_FURTHER_EVIDENCE or POST_HEARING_REQUEST "
        + "then return empty string.")
    @Test
    public void getNotificationTypeForActionFurtherEvidence_InvalidActionType_returnEmpty() {
        DynamicList sender = new DynamicList(new DynamicListItem("appellant", "Other party 1 - Representative - R Basker R Nadar"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, SYA_APPEAL_CREATED);

        assertEquals("", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, SubscriptionWithType.builder().build()));
    }

    @DisplayName("When sender and subscriber is appellant then return confirmation")
    @Test
    @Parameters({"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    public void getNotificationTypeForActionFurtherEvidence_ValidActionTypeAndValidSubscriber_returnConfirmation(NotificationEventType eventType) {
        DynamicList sender = new DynamicList(new DynamicListItem("appellant", "Appellant"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, eventType);
        SubscriptionWithType type = SubscriptionWithType.builder().party(appellantWithId).build();
        assertEquals("confirmation", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender is an appellant, representative or joint party and subscriber is other than sender "
        + "then return notification.")
    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void getNotificationTypeForActionFurtherEvidence_ValidActionTypeAndInValidSubscriber_returnNotice(String requester) {
        DynamicList sender = new DynamicList(new DynamicListItem(requester, requester), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        Appellant appellant = Appellant.builder()
            .name(Name.builder().firstName("Tom").lastName("Cat").build())
            .address(Address.builder().line1("Appellant Line 1")
                .town("Appellant Town")
                .county("Appellant County")
                .postcode("AP9 3LL").build())
            .build();

        SubscriptionWithType type = SubscriptionWithType.builder().party(appellant).partyId(appellant.getId()).build();
        assertEquals("notice", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender and subscriber is a Joint party then return confirmation.")
    @Test
    @Parameters({"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    public void getNotificationTypeForActionFurtherEvidence_ValidJointPartySub_returnConfirmation(NotificationEventType eventType) {
        DynamicList sender = new DynamicList(new DynamicListItem("jointParty", "jointParty"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, eventType);
        SubscriptionWithType type = SubscriptionWithType.builder().party(jointPartyWithId).build();
        assertEquals("confirmation", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender is an other party and subscriber is other than sender then return notification.")
    @Test
    @Parameters({"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    public void getNotificationTypeForActionFurtherEvidence_InValidOtherPartySubscriber_returnNotice(NotificationEventType eventType) {
        DynamicList sender = new DynamicList(new DynamicListItem("otherParty", "otherParty"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, eventType);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("Invalid").build();

        assertEquals("notice", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When other party or his representative send the request "
        + "then other party and his representative should receive confirmation.")
    @Test
    @Parameters({"otherPartyOP123456,OP123456", "otherPartyOP123456,otherPartyOPREP123456", "otherPartyOPREP123456,OPREP123456", "otherPartyOPREP123456,OP123456"})
    public void getNotificationTypeForActionFurtherEvidence_ValidOtherPartyAndRepSub_returnConfirmation(String senderType, String subscriber) {
        DynamicList sender = new DynamicList(new DynamicListItem(senderType, senderType), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId(subscriber).build();
        assertEquals("confirmation", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender and subscriber is an other party or his representative then return true")
    @Test
    @Parameters({"OPREP123456,OPREP123456", "OPREP123456,OP123456"})
    public void isValidOtherPartyRepresentative_ValidOtherPartyAndRep_returnTrue(String senderId, String subscriptionId) {
        List<CcdValue<OtherParty>> otherParties = buildOtherPartyData();
        assertTrue(LetterUtils.isValidOtherPartyRepresentative(subscriptionId,
            senderId, otherParties.get(0)));
    }

    @DisplayName("When the sender/original requester is other than representative then return false.")
    @Test
    @Parameters({"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    public void isValidAppellantRepresentativeForSetAsideRequest_givenNonRepresentative_thenReturnFalse(NotificationEventType eventType) {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyOP123456", "OPREP123456"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, eventType);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("otherPartyOPREP123456").build();
        assertFalse(LetterUtils.isValidAppellantRepresentativeForSetAsideRequest(wrapper, type));
    }

    @DisplayName("When the sender/original requester is representative and subscriber is appellant or representative"
        + " then return true.")
    @Test
    @Parameters({"APP123456", "REP123456"})
    public void isValidAppellantRepresentativeForSetAsideRequest_givenValidRepresentative_thenReturnTrue(String partyId) {
        DynamicList sender = new DynamicList(new DynamicListItem("representative", "representative"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        Appellant party = Appellant.builder().id(partyId).build();
        SubscriptionWithType type = SubscriptionWithType.builder().party(party).build();
        assertTrue(LetterUtils.isValidAppellantRepresentativeForSetAsideRequest(wrapper, type));
    }

    @DisplayName("When the subscription id is null then return false.")
    @Test
    public void isValidOtherParty_givenSubscriberIsNull_thenReturnFalse() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyOPREP123456", "OPREP123456"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        SubscriptionWithType type = SubscriptionWithType.builder().build();
        assertFalse(LetterUtils.isValidOtherParty(wrapper, type));
    }

    @DisplayName("When the subscription id is invalid then return false.")
    @Test
    @Parameters({"jointParty", "otherParty"})
    public void isValidOtherParty_givenSubscriberIsInValid_thenReturnFalse(String senderType) {
        DynamicList sender = new DynamicList(new DynamicListItem(senderType, senderType), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("test").build();
        assertFalse(LetterUtils.isValidOtherParty(wrapper, type));
    }

    @DisplayName("When the other party data present and valid then return true.")
    @Test
    public void isValidOtherParty_givenSubscriberIsValid_thenReturnTrue() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyOP123456", "otherParty"), new ArrayList<>());
        caseData.setOriginalSender(sender);
        NotificationSscsCaseDataWrapper wrapper = buildBaseWrapperWithCaseData(caseData, LIBERTY_TO_APPLY_REQUEST);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("OP123456").build();
        assertTrue(LetterUtils.isValidOtherParty(wrapper, type));
    }

    public NotificationSscsCaseDataWrapper buildBaseWrapperWithCaseData(SscsCaseData caseData, NotificationEventType type) {
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(caseData)
            .notificationEventType(type)
            .build();
        return caseDataWrapper;
    }


    @DisplayName("lines returns expected values when given a valid UK address object.")
    @Test
    public void lines_returns_expected_for_UK_address() {
        Address testAddress = Address.builder()
                .line1("Somerset House")
                .line2("Strand")
                .town("London")
                .county("Greater London")
                .postcode("WC2R 1LA")
                .build();

        List<String> addressLines = lines(testAddress);
        List<String> expectedLines = List.of("Somerset House", "Strand", "London", "Greater London", "WC2R 1LA");

        for (int i = 0; i < addressLines.size(); i++) {
            assertEquals(expectedLines.get(i), addressLines.get(i));
        }
    }

    @DisplayName("lines returns expected values when given a valid UK address object with inMainlandUK YES.")
    @Test
    public void lines_returns_expected_for_UK_address_with_inMainlandUK() {
        Address testAddress = Address.builder()
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
            assertEquals(expectedLines.get(i), addressLines.get(i));
        }
    }

    @DisplayName("lines returns expected values when given a valid international address object.")
    @Test
    public void lines_returns_expected_for_International_address() {
        Address testAddress = Address.builder()
                .line1("Catherdrale Notre-Dame de Paris")
                .line2("6 Parvis Notre-dame - Pl. Jean-Paul II")
                .town("Paris")
                .county("Ile-de-France")
                .postcode("75004")
                .country("France")
                .inMainlandUk(YesNo.NO)
                .build();

        List<String> addressLines = lines(testAddress);
        List<String> expectedLines = List.of("Catherdrale Notre-Dame de Paris", "6 Parvis Notre-dame - Pl. Jean-Paul II", "Paris", "75004", "France");

        for (int i = 0; i < addressLines.size(); i++) {
            assertEquals(expectedLines.get(i), addressLines.get(i));
        }
    }

    @DisplayName("lines returns expected values when international address object doesn't include postcode.")
    @Test
    public void lines_returns_expected_for_International_address_without_Postcode() {
        Address testAddress = Address.builder()
                .line1("Catherdrale Notre-Dame de Paris")
                .line2("6 Parvis Notre-dame - Pl. Jean-Paul II")
                .town("Paris")
                .county("Ile-de-France")
                .country("France")
                .inMainlandUk(YesNo.NO)
                .build();

        List<String> addressLines = lines(testAddress);
        List<String> expectedLines = List.of("Catherdrale Notre-Dame de Paris", "6 Parvis Notre-dame - Pl. Jean-Paul II", "Paris", "France");

        for (int i = 0; i < addressLines.size(); i++) {
            assertEquals(expectedLines.get(i), addressLines.get(i));
        }
    }

    @DisplayName("lines filters out null address lines for UK addresses.")
    @Test
    public void lines_removes_null_values() {
        Address testAddress = Address.builder()
                .line1("Test House")
                .line2(null)
                .town("Test Town")
                .county("Test County")
                .postcode("Test Postcode")
                .build();

        List<String> addressLines = lines(testAddress);
        List<String> expectedLines = List.of("Test House", "Test Town", "Test County", "Test Postcode");

        for (int i = 0; i < addressLines.size(); i++) {
            assertEquals(expectedLines.get(i), addressLines.get(i));
        }
    }
}
