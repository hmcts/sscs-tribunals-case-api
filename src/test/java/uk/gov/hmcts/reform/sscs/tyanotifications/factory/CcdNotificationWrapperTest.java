package uk.gov.hmcts.reform.sscs.tyanotifications.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import junitparams.Parameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.reference.data.model.ConfidentialityPartyMembers;
import uk.gov.hmcts.reform.sscs.reference.data.model.ConfidentialityType;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;

public class CcdNotificationWrapperTest {

    private CcdNotificationWrapper ccdNotificationWrapper;
    private static final String PAPER = "paper";
    private static final String ORAL = "oral";

    @ParameterizedTest
    @CsvSource({"paper,PAPER", "oral,ORAL"})
    public void should_returnAccordingAppealHearingType_when_hearingTypeIsPresent(String hearingType,
                                                                                  AppealHearingType expected) {
        ccdNotificationWrapper = buildCcdNotificationWrapper(hearingType);

        assertThat(ccdNotificationWrapper.getHearingType(), is(expected));
    }

    private CcdNotificationWrapper buildCcdNotificationWrapper(String hearingType) {
        return new CcdNotificationWrapper(
            NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                    .appeal(Appeal.builder()
                        .hearingType(hearingType)
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder().build())
                        .representativeSubscription(Subscription.builder().build())
                        .build())
                    .build())
                .build()
        );
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithRep(NotificationEventType notificationEventType) {
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, null, Representative.builder().hasRepresentative("Yes").build(), false);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(NotificationEventType notificationEventType, Representative rep) {
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, null, rep, true);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndRep(NotificationEventType notificationEventType) {
        Appointee appointee = Appointee.builder()
            .name(Name.builder().firstName("Ap").lastName("Pointee").build())
            .address(Address.builder().line1("Appointee Line 1").town("Appointee Town").county("Appointee County").postcode("AP9 0IN").build())
            .build();
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, appointee, Representative.builder().hasRepresentative("Yes").build(), true);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventType(NotificationEventType notificationEventType) {
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, null, null, false);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventType(NotificationEventType notificationEventType, Appointee appointee, Representative representative, boolean jointParty) {
        Appellant appellant = Appellant.builder().build();
        List<HearingRecordingRequest> releasedHearings = new ArrayList<>();
        Subscription appointeeSubscription = null;
        if (null != appointee) {
            appellant.setAppointee(appointee);
            appointeeSubscription = Subscription.builder()
                .email("appointee@test.com")
                .subscribeEmail("Yes")
                .build();
            HearingRecordingRequest appointeeHearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty(PartyItemList.APPELLANT.getCode()).build()).build();
            releasedHearings.add(appointeeHearingRecordingRequest);
        }

        Subscription repSubscription = null;
        if (null != representative) {
            representative = Representative.builder()
                .hasRepresentative("Yes")
                .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                .build();
            repSubscription = Subscription.builder()
                .email("rep@test.com")
                .subscribeEmail("Yes")
                .build();
            HearingRecordingRequest repHearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty(PartyItemList.REPRESENTATIVE.getCode()).build()).build();
            releasedHearings.add(repHearingRecordingRequest);
        }

        Subscription jointPartySubscription = null;
        YesNo jointPartyYesNo = NO;
        Name jointPartyName = null;
        if (jointParty) {
            jointPartyYesNo = YES;
            jointPartyName = Name.builder().title("Madam").firstName("Jon").lastName("Party").build();
            jointPartySubscription = Subscription.builder()
                .email("joint@test.com")
                .subscribeEmail("Yes")
                .build();
            HearingRecordingRequest jointPartyHearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty(PartyItemList.JOINT_PARTY.getCode()).build()).build();
            releasedHearings.add(jointPartyHearingRecordingRequest);
        }

        return new CcdNotificationWrapper(
            NotificationSscsCaseDataWrapper.builder()
                .oldSscsCaseData(SscsCaseData.builder().build())
                .newSscsCaseData(SscsCaseData.builder()
                    .jointParty(JointParty.builder()
                        .hasJointParty(jointPartyYesNo)
                        .name(jointPartyName)
                        .build())
                    .appeal(Appeal.builder()
                        .appellant(appellant)
                        .hearingType("cor")
                        .rep(representative)
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder().build())
                        .representativeSubscription(repSubscription)
                        .appointeeSubscription(appointeeSubscription)
                        .jointPartySubscription(jointPartySubscription)
                        .build())
                    .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().citizenReleasedHearings(releasedHearings).build())
                    .build())
                .notificationEventType(notificationEventType)
                .build()

        );
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(
        NotificationEventType notificationEventType, String hearingType) {
        return buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType, null);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(
        NotificationEventType notificationEventType, String hearingType, DirectionType directionType) {
        Appointee appointee = Appointee.builder()
            .name(Name.builder().firstName("Ap").lastName("Pointee").build())
            .address(Address.builder().line1("Appointee Line 1").town("Appointee Town").county("Appointee County").postcode("AP9 0IN").build())
            .build();

        DynamicList dynamicList = null;
        if (directionType != null) {
            dynamicList = new DynamicList(directionType.toString());
        }

        return new CcdNotificationWrapper(
            NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                    .jointParty(JointParty.builder()
                        .hasJointParty(YES)
                        .jointPartyAddressSameAsAppellant(YES)
                        .name(Name.builder().title("Madam").firstName("Jon").lastName("Party").build())
                        .build())
                    .appeal(Appeal.builder()
                        .hearingType(hearingType)
                        .appellant(Appellant.builder().appointee(appointee).build())
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(
                            Subscription.builder()
                                .email("appellant@test.com")
                                .subscribeEmail("Yes")
                                .build()
                        )
                        .appointeeSubscription(
                            Subscription.builder()
                                .email("appointee@test.com")
                                .subscribeEmail("Yes")
                                .build()
                        )
                        .jointPartySubscription(
                            Subscription.builder()
                                .email("jointParty@test.com")
                                .subscribeEmail("Yes")
                                .build()
                        )
                        .build())
                    .directionTypeDl(dynamicList)
                    .build())
                .notificationEventType(notificationEventType)
                .build()

        );
    }

    private CcdNotificationWrapper buildNotificationWrapperWithOtherParty(NotificationEventType notificationEventType, List<CcdValue<OtherParty>> otherParties) {
        Appointee appointee = Appointee.builder()
            .name(Name.builder().firstName("Ap").lastName("Pointee").build())
            .address(Address.builder().line1("Appointee Line 1").town("Appointee Town").county("Appointee County").postcode("AP9 0IN").build())
            .build();
        return new CcdNotificationWrapper(
            NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder().otherParties(otherParties)
                    .appeal(Appeal.builder()
                        .hearingType(ORAL)
                        .appellant(Appellant.builder().appointee(appointee).build())
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(
                            Subscription.builder()
                                .email("appellant@test.com")
                                .subscribeEmail("Yes")
                                .build()
                        )
                        .build())
                    .build())
                .notificationEventType(notificationEventType)
                .build());

    }

    private List<CcdValue<OtherParty>> buildOtherPartyData(boolean sendNewOtherPartyNotification, boolean hasAppointee, boolean hasRep) {
        return List.of(CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id("1")
                .otherPartySubscription(Subscription.builder().email("other@party").subscribeEmail("Yes").build())
                .sendNewOtherPartyNotification(sendNewOtherPartyNotification ? YesNo.YES : null)
                .isAppointee(hasAppointee ? YesNo.YES.getValue() : YesNo.NO.getValue())
                .appointee(Appointee.builder()
                    .id("2")
                    .name(Name.builder()
                        .firstName("First")
                        .lastName("Last")
                        .build())
                    .build())
                .rep(Representative.builder()
                    .id("3")
                    .hasRepresentative(hasRep ? YesNo.YES.getValue() : YesNo.NO.getValue())
                    .name(Name.builder()
                        .firstName("First")
                        .lastName("Last")
                        .build())
                    .build())
                .build())
            .build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPEAL_LAPSED", "HMCTS_APPEAL_LAPSED", "DWP_APPEAL_LAPSED", "APPEAL_WITHDRAWN", "EVIDENCE_RECEIVED",
        "POSTPONEMENT", "HEARING_BOOKED", "SYA_APPEAL_CREATED", "VALID_APPEAL_CREATED",
        "RESEND_APPEAL_CREATED", "APPEAL_RECEIVED", "ADJOURNED", "ISSUE_FINAL_DECISION_WELSH",
        "PROCESS_AUDIO_VIDEO", "PROCESS_AUDIO_VIDEO_WELSH", "ACTION_POSTPONEMENT_REQUEST", "ACTION_POSTPONEMENT_REQUEST_WELSH",
        "APPEAL_DORMANT", "DWP_RESPONSE_RECEIVED", "STRUCK_OUT", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH"})
    public void givenSubscriptions_shouldGetAppellantAndRepSubscriptionTypeList(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(2, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
        Assertions.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    @ParameterizedTest
    @EnumSource(value = NotificationEventType.class, names = {"DEATH_OF_APPELLANT", "PROVIDE_APPOINTEE_DETAILS"})
    public void givenSubscriptions_shouldGetAppointeeAndRepSubscriptionTypeList(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndRep(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(2, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assertions.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    private static Stream<Arguments> provideForShouldGetSubscriptionTypeListWithAppointee() {
        return Stream.of(
            Arguments.of(SYA_APPEAL_CREATED, "cor"),
            Arguments.of(DWP_RESPONSE_RECEIVED, "oral"),
            Arguments.of(DWP_RESPONSE_RECEIVED, "paper"),
            Arguments.of(HMCTS_APPEAL_LAPSED, "paper"),
            Arguments.of(HMCTS_APPEAL_LAPSED, "oral"),
            Arguments.of(DWP_APPEAL_LAPSED, "paper"),
            Arguments.of(DWP_APPEAL_LAPSED, "oral"),
            Arguments.of(SUBSCRIPTION_UPDATED, "paper"),
            Arguments.of(VALID_APPEAL_CREATED, "cor"),
            Arguments.of(RESEND_APPEAL_CREATED, "cor")
        );
    }

    @ParameterizedTest
    @MethodSource("provideForShouldGetSubscriptionTypeListWithAppointee")
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointee(NotificationEventType notificationEventType, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(1, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
    }

    private static Stream<Arguments> shouldGetSubscriptionTypeListWithAppointeeAndJointParty() {
        return Stream.of(
            Arguments.of(APPEAL_LAPSED, "paper"),
            Arguments.of(APPEAL_LAPSED, "oral"),
            Arguments.of(EVIDENCE_REMINDER, "oral"),
            Arguments.of(EVIDENCE_REMINDER, "paper"),
            Arguments.of(APPEAL_DORMANT, "paper"),
            Arguments.of(APPEAL_DORMANT, "oral"),
            Arguments.of(ADJOURNED, "paper"),
            Arguments.of(ADJOURNED, "oral"),
            Arguments.of(POSTPONEMENT, "paper"),
            Arguments.of(POSTPONEMENT, "oral"),
            Arguments.of(EVIDENCE_RECEIVED, "paper"),
            Arguments.of(EVIDENCE_RECEIVED, "oral"),
            Arguments.of(APPEAL_WITHDRAWN, "paper"),
            Arguments.of(STRUCK_OUT, "oral"),
            Arguments.of(STRUCK_OUT, "paper"),
            Arguments.of(DIRECTION_ISSUED, "oral"),
            Arguments.of(DIRECTION_ISSUED, "paper"),
            Arguments.of(DIRECTION_ISSUED_WELSH, "oral"),
            Arguments.of(DIRECTION_ISSUED_WELSH, "paper"),
            Arguments.of(DWP_UPLOAD_RESPONSE, "paper"),
            Arguments.of(PROCESS_AUDIO_VIDEO, "oral"),
            Arguments.of(PROCESS_AUDIO_VIDEO_WELSH, "paper"),
            Arguments.of(ACTION_POSTPONEMENT_REQUEST, "paper"),
            Arguments.of(ACTION_POSTPONEMENT_REQUEST_WELSH, "paper"),
            Arguments.of(DWP_UPLOAD_RESPONSE, "oral"),
            Arguments.of(HEARING_BOOKED, "oral"),
            Arguments.of(HEARING_BOOKED, "paper"),
            Arguments.of(HEARING_REMINDER, "oral"),
            Arguments.of(HEARING_REMINDER, "paper"),
            Arguments.of(ISSUE_ADJOURNMENT_NOTICE, "paper"),
            Arguments.of(ISSUE_ADJOURNMENT_NOTICE_WELSH, "oral")
        );
    }

    @ParameterizedTest
    @MethodSource("shouldGetSubscriptionTypeListWithAppointeeAndJointParty")
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointeeAndJointParty(NotificationEventType notificationEventType, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(2, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assertions.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(1).getSubscriptionType());
    }

    @ParameterizedTest
    @MethodSource("getDirectionIssuedSubscriptionBasedOnConfidentialityForAppellantAndRepresentative")
    public void givenSubscriptions_shouldGetAppellantAndRepSubscriptionTypeWhenConfidentialIsSelected(NotificationEventType notificationEventType, String confidentialityType, List<String> chosenMembers, List<SubscriptionType> requiredMembers) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(notificationEventType);
        SscsCaseData sscsCaseData = ccdNotificationWrapper.getNewSscsCaseData();
        sscsCaseData.setConfidentialityType(confidentialityType);
        chosenMembers.forEach(o -> createPartiesOnTheCase(sscsCaseData, o));

        assertDirectionsNoticeConfidentiality(sscsCaseData, chosenMembers, requiredMembers);
    }

    @ParameterizedTest
    @MethodSource("getDirectionIssuedSubscriptionBasedOnConfidentialityForRestOfTheOtherParties")
    public void givenSubscriptions_shouldGetAppointeeAndJointPartyTypeWhenConfidentialIsSelected(NotificationEventType notificationEventType, String confidentialityType, List<String> chosenMembers, List<SubscriptionType> requiredMembers, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType);
        SscsCaseData sscsCaseData = ccdNotificationWrapper.getNewSscsCaseData();
        sscsCaseData.setConfidentialityType(confidentialityType);
        chosenMembers.forEach(o -> createPartiesOnTheCase(sscsCaseData, o));
        assertDirectionsNoticeConfidentiality(sscsCaseData, chosenMembers, requiredMembers);
    }

    public void assertDirectionsNoticeConfidentiality(SscsCaseData sscsCaseData, List<String> chosenMembers, List<SubscriptionType> requiredMembers) {
        sscsCaseData.setSendDirectionNoticeToAppellantOrAppointee(chosenMembers.contains(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode()) ? YesNo.YES : YesNo.NO);
        sscsCaseData.setSendDirectionNoticeToFTA(chosenMembers.contains(ConfidentialityPartyMembers.FTA.getCode()) ? YesNo.YES : YesNo.NO);

        YesNo hasRepresentative = chosenMembers.contains(ConfidentialityPartyMembers.REPRESENTATIVE.getCode()) ? YesNo.YES : YesNo.NO;
        sscsCaseData.setSendDirectionNoticeToRepresentative(hasRepresentative);

        YesNo hasOtherPartyRep = chosenMembers.contains(ConfidentialityPartyMembers.OTHER_PARTY_REP.getCode()) ? YesNo.YES : YesNo.NO;
        sscsCaseData.setSendDirectionNoticeToOtherPartyRep(hasOtherPartyRep);
        YesNo hasOtherPartyAppointee = chosenMembers.contains(ConfidentialityPartyMembers.OTHER_PARTY_APPOINTEE.getCode()) ? YesNo.YES : YesNo.NO;
        sscsCaseData.setSendDirectionNoticeToOtherPartyAppointee(hasOtherPartyAppointee);

        YesNo hasOtherParties = chosenMembers.contains(ConfidentialityPartyMembers.OTHER_PARTY.getCode()) ? YesNo.YES : YesNo.NO;
        sscsCaseData.setSendDirectionNoticeToOtherParty(hasOtherParties);

        YesNo hasJointParty = chosenMembers.contains(ConfidentialityPartyMembers.JOINT_PARTY.getCode()) ? YesNo.YES : YesNo.NO;
        sscsCaseData.setSendDirectionNoticeToJointParty(hasJointParty);

        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(requiredMembers.size(), subsWithTypeList.size());
        subsWithTypeList.forEach(o -> Assertions.assertTrue(requiredMembers.contains(o.getSubscriptionType())));
    }

    private void createPartiesOnTheCase(SscsCaseData sscsCaseData, String partyMember) {
        CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .name(Name.builder().title("Mr").firstName("Harrison").lastName("Kane").build())
            .address(Address.builder()
                .line1("First Floor")
                .line2("My Building")
                .town("222 Corporation Street")
                .county("Glasgow")
                .postcode("GL11 6TF")
                .build())
            .build()).build();
        Representative rep = Representative.builder()
            .name(Name.builder().firstName("Harry").lastName("Potter").build())
            .hasRepresentative("Yes").build();

        if (ConfidentialityPartyMembers.REPRESENTATIVE.getCode().equals(partyMember)) {
            sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("yes").build());

        } else if (ConfidentialityPartyMembers.OTHER_PARTY_REP.getCode().equals(partyMember)) {
            otherParty.getValue().setRep(rep);
            sscsCaseData.setOtherParties(List.of(otherParty));

        } else if (ConfidentialityPartyMembers.OTHER_PARTY_APPOINTEE.getCode().equals(partyMember)) {
            Appointee appointee = Appointee.builder()
                .name(Name.builder().firstName("APPOINTEE").lastName("Test").build())
                .build();
            otherParty.getValue().setAppointee(appointee);
            otherParty.getValue().setIsAppointee("Yes");
            sscsCaseData.setOtherParties(List.of(otherParty));

        } else if (ConfidentialityPartyMembers.OTHER_PARTY.getCode().equals(partyMember)) {
            sscsCaseData.setOtherParties(List.of(otherParty));

        } else if (ConfidentialityPartyMembers.JOINT_PARTY.getCode().equals(partyMember)) {
            JointParty jointParty = JointParty.builder()
                .hasJointParty(YES)
                .name(Name.builder().firstName("Joint").lastName("Party").build())
                .build();
            sscsCaseData.setJointParty(jointParty);
        }
    }

    @Test
    public void givenSubscriptionForAppellantRepAndJointParty_shouldGetSubscriptionTypeListForAppellantAndJointPartyOnlyWhenBothGranted() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(REVIEW_CONFIDENTIALITY_REQUEST, Representative.builder().hasRepresentative("Yes").build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(2, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
        Assertions.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    public void givenSubscriptionForAppellantRepAndJointParty_shouldGetSubscriptionTypeListForAppellantOnlyWhenOnlyAppellantGranted() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(REVIEW_CONFIDENTIALITY_REQUEST, Representative.builder().hasRepresentative("Yes").build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeJointParty(null);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(1, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenSubscriptionForAppellantRepAndJointParty_shouldGetSubscriptionTypeListForJointPartyOnlyWhenOnlyJointPartyGranted() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(REVIEW_CONFIDENTIALITY_REQUEST, Representative.builder().hasRepresentative("Yes").build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeAppellant(null);
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(1, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenSubscriptionForAppellantRepAndJointParty_shouldGetSubscriptionTypeListForJointPartyOnlyWhenOnlyJointPartyIsNewlyGranted() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(REVIEW_CONFIDENTIALITY_REQUEST, Representative.builder().hasRepresentative("Yes").build());
        ccdNotificationWrapper.getOldSscsCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        ccdNotificationWrapper.getOldSscsCaseData().setConfidentialityRequestOutcomeJointParty(null);
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(1, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @ParameterizedTest
    @MethodSource("getEventTypeFilteredWithAppellant")
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppellant(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(1, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    private static Stream<Arguments> provideShouldGetSubscriptionTypeListWithAppointeeAndJointPartyDirection() {
        return Stream.of(
            Arguments.of(DIRECTION_ISSUED, "paper", DirectionType.PROVIDE_INFORMATION),
            Arguments.of(DIRECTION_ISSUED, "oral", DirectionType.PROVIDE_INFORMATION),
            Arguments.of(DIRECTION_ISSUED, "paper", DirectionType.APPEAL_TO_PROCEED),
            Arguments.of(DIRECTION_ISSUED, "oral", DirectionType.APPEAL_TO_PROCEED),
            Arguments.of(DIRECTION_ISSUED, "paper", DirectionType.GRANT_EXTENSION),
            Arguments.of(DIRECTION_ISSUED, "oral", DirectionType.GRANT_EXTENSION),
            Arguments.of(DIRECTION_ISSUED, "paper", DirectionType.REFUSE_EXTENSION),
            Arguments.of(DIRECTION_ISSUED, "oral", DirectionType.REFUSE_EXTENSION)
        );
    }

    @ParameterizedTest
    @MethodSource("provideShouldGetSubscriptionTypeListWithAppointeeAndJointPartyDirection")
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointeeAndJointPartyDirection(NotificationEventType notificationEventType, String hearingType, DirectionType directionType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType, directionType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(2, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assertions.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    public void givenRequestForInformationForAppointeeWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(REQUEST_FOR_INFORMATION, ORAL);
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.APPELLANT.getCode(), PartyItemList.APPELLANT.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(1, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenRequestForInformationForAppellantWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(REQUEST_FOR_INFORMATION);
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.APPELLANT.getCode(), PartyItemList.APPELLANT.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(1, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenRequestForInformationForRepWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(REQUEST_FOR_INFORMATION);
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.REPRESENTATIVE.getCode(), PartyItemList.REPRESENTATIVE.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(1, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenRequestForInformationForJointPartyWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(REQUEST_FOR_INFORMATION, PAPER);
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.JOINT_PARTY.getCode(), PartyItemList.JOINT_PARTY.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(1, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenProcessHearingRequestForRepWithSubscription_shouldSendProcessHearingRequestNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(ACTION_HEARING_RECORDING_REQUEST);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(1, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenProcessHearingRequestForJointPartyWithSubscription_shouldSendProcessHearingRequestNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(ACTION_HEARING_RECORDING_REQUEST, null);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(1, subsWithTypeList.size());
        Assertions.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenProcessHearingRequestForNoPartyWithSubscription_shouldNotSendProcessHearingRequestNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(ACTION_HEARING_RECORDING_REQUEST, null, null, false);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertTrue(subsWithTypeList.isEmpty());
    }

    @Test
    public void givenNoOtherPartyInTheCase_thenReturnEmptySubscription() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, null);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions(ccdNotificationWrapper.getNewSscsCaseData(), ccdNotificationWrapper.getNotificationType());
        Assertions.assertTrue(subsWithTypeList.isEmpty());
    }

    @Test
    public void givenUpdateOtherPartyDataEventAndSendNotificationFlagIsNotSetInOtherParty_thenReturnEmptySubscription() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, buildOtherPartyData(false, true, true));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions(ccdNotificationWrapper.getNewSscsCaseData(), ccdNotificationWrapper.getNotificationType());
        Assertions.assertTrue(subsWithTypeList.isEmpty());
    }

    @Test
    public void givenUpdateOtherPartyDataEventAndSendNotificationFlagIsSetInOtherPartyWithAppointee_thenReturnAllOtherPartySubscription() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, buildOtherPartyData(true, true, true));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions(ccdNotificationWrapper.getNewSscsCaseData(), ccdNotificationWrapper.getNotificationType());
        Assertions.assertEquals(2, subsWithTypeList.size());
    }

    @Test
    public void givenUpdateOtherPartyDataEventAndSendNotificationFlagIsSetInOtherPartyWithNoAppointee_thenReturnAllOtherPartySubscription() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, buildOtherPartyData(true, false, true));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions(ccdNotificationWrapper.getNewSscsCaseData(), ccdNotificationWrapper.getNotificationType());
        Assertions.assertEquals(2, subsWithTypeList.size());
    }

    @Test
    public void givenUpdateOtherPartyDataEventAndSendNotificationFlagIsSetInOtherPartyWithNoAppointee_thenReturnAllOtherPartySubscription2() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, buildOtherPartyData(true, true, true));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions(ccdNotificationWrapper.getNewSscsCaseData(), ccdNotificationWrapper.getNotificationType());
        Assertions.assertEquals(2, subsWithTypeList.size());
    }

    @ParameterizedTest
    @EnumSource(value = NotificationEventType.class, names = {
        "ADJOURNED",
        "ADMIN_APPEAL_WITHDRAWN",
        "APPEAL_DORMANT",
        "APPEAL_LAPSED",
        "APPEAL_WITHDRAWN",
        "DECISION_ISSUED",
        "DECISION_ISSUED_WELSH",
        "DIRECTION_ISSUED",
        "DIRECTION_ISSUED_WELSH",
        "DWP_APPEAL_LAPSED",
        "DWP_RESPONSE_RECEIVED",
        "DWP_UPLOAD_RESPONSE",
        "EVIDENCE_RECEIVED",
        "EVIDENCE_REMINDER",
        "HEARING_BOOKED",
        "HEARING_REMINDER",
        "HMCTS_APPEAL_LAPSED",
        "ISSUE_ADJOURNMENT_NOTICE",
        "ISSUE_ADJOURNMENT_NOTICE_WELSH",
        "ISSUE_FINAL_DECISION",
        "ISSUE_FINAL_DECISION_WELSH",
        "NON_COMPLIANT",
        "POSTPONEMENT",
        "PROCESS_AUDIO_VIDEO",
        "PROCESS_AUDIO_VIDEO_WELSH",
        "REQUEST_FOR_INFORMATION",
        "STRUCK_OUT",
        "SUBSCRIPTION_CREATED",
        "SUBSCRIPTION_OLD",
        "SUBSCRIPTION_UPDATED"})
    public void givenNotificationForOtherParty_thenReturnAllOtherPartySubscription(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(notificationEventType, buildOtherPartyData(false, false, false));

        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.APPELLANT.getCode(), PartyItemList.APPELLANT.getLabel()), new ArrayList<>()));

        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assertions.assertEquals(2, subsWithTypeList.size());
    }

    @SuppressWarnings({"unused"})
    private static Object[] getEventTypeFilteredWithAppellant() {
        return Arrays.stream(values())
            .filter(type -> !(type.equals(APPEAL_LAPSED)
                || type.equals(HMCTS_APPEAL_LAPSED)
                || type.equals(DWP_APPEAL_LAPSED)
                || type.equals(APPEAL_WITHDRAWN)
                || type.equals(EVIDENCE_RECEIVED)
                || type.equals(CASE_UPDATED)
                || type.equals(APPEAL_DORMANT)
                || type.equals(ADJOURNED)
                || type.equals(APPEAL_RECEIVED)
                || type.equals(POSTPONEMENT)
                || type.equals(SUBSCRIPTION_UPDATED)
                || type.equals(HEARING_BOOKED)
                || type.equals(STRUCK_OUT)
                || type.equals(REQUEST_FOR_INFORMATION)
                || type.equals(NON_COMPLIANT)
                || type.equals(REVIEW_CONFIDENTIALITY_REQUEST)
                || type.equals(ACTION_HEARING_RECORDING_REQUEST)
                || type.equals(UPDATE_OTHER_PARTY_DATA)
            )).toArray();
    }

    @SuppressWarnings({"Indentation", "unused"})
    private static Object[] getDirectionIssuedSubscriptionBasedOnConfidentialityForAppellantAndRepresentative() {
        return new Object[]{
            new Object[]{UPDATE_OTHER_PARTY_DATA, null, List.of(), List.of()},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.GENERAL.getCode(), List.of(), List.of(SubscriptionType.APPELLANT, SubscriptionType.REPRESENTATIVE)},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode()), List.of(SubscriptionType.APPELLANT)},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.REPRESENTATIVE.getCode()), List.of(SubscriptionType.REPRESENTATIVE)},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode(), ConfidentialityPartyMembers.REPRESENTATIVE.getCode()), List.of(SubscriptionType.APPELLANT, SubscriptionType.REPRESENTATIVE)},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.GENERAL.getCode(), List.of(), List.of(SubscriptionType.APPELLANT, SubscriptionType.REPRESENTATIVE)},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode()), List.of(SubscriptionType.APPELLANT)},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.REPRESENTATIVE.getCode()), List.of(SubscriptionType.REPRESENTATIVE)},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode(), ConfidentialityPartyMembers.REPRESENTATIVE.getCode()), List.of(SubscriptionType.APPELLANT, SubscriptionType.REPRESENTATIVE)},
        };
    }

    @SuppressWarnings({"Indentation", "unused"})
    private static Object[] getDirectionIssuedSubscriptionBasedOnConfidentialityForRestOfTheOtherParties() {
        return new Object[]{
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.GENERAL.getCode(), List.of(), List.of(SubscriptionType.APPOINTEE, SubscriptionType.JOINT_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.GENERAL.getCode(), List.of(), List.of(SubscriptionType.APPOINTEE, SubscriptionType.JOINT_PARTY), ORAL},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode()), List.of(SubscriptionType.APPOINTEE), PAPER},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode()), List.of(SubscriptionType.APPOINTEE), ORAL},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.JOINT_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.JOINT_PARTY), ORAL},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode(), ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.APPOINTEE, SubscriptionType.JOINT_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode(), ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.APPOINTEE, SubscriptionType.JOINT_PARTY), ORAL},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY_REP.getCode(), ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.OTHER_PARTY, SubscriptionType.JOINT_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY_REP.getCode(), ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.OTHER_PARTY, SubscriptionType.JOINT_PARTY), ORAL},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY.getCode(), ConfidentialityPartyMembers.OTHER_PARTY.getCode()), List.of(SubscriptionType.OTHER_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY.getCode(), ConfidentialityPartyMembers.OTHER_PARTY.getCode()), List.of(SubscriptionType.OTHER_PARTY), ORAL},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY_APPOINTEE.getCode()), List.of(SubscriptionType.OTHER_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY_APPOINTEE.getCode()), List.of(SubscriptionType.OTHER_PARTY), ORAL},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.GENERAL.getCode(), List.of(), List.of(SubscriptionType.APPOINTEE, SubscriptionType.JOINT_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.GENERAL.getCode(), List.of(), List.of(SubscriptionType.APPOINTEE, SubscriptionType.JOINT_PARTY), ORAL},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode()), List.of(SubscriptionType.APPOINTEE), PAPER},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode()), List.of(SubscriptionType.APPOINTEE), ORAL},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.JOINT_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.JOINT_PARTY), ORAL},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode(), ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.APPOINTEE, SubscriptionType.JOINT_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode(), ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.APPOINTEE, SubscriptionType.JOINT_PARTY), ORAL},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY_REP.getCode(), ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.OTHER_PARTY, SubscriptionType.JOINT_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY_REP.getCode(), ConfidentialityPartyMembers.JOINT_PARTY.getCode()), List.of(SubscriptionType.OTHER_PARTY, SubscriptionType.JOINT_PARTY), ORAL},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY.getCode(), ConfidentialityPartyMembers.OTHER_PARTY.getCode()), List.of(SubscriptionType.OTHER_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY.getCode(), ConfidentialityPartyMembers.OTHER_PARTY.getCode()), List.of(SubscriptionType.OTHER_PARTY), ORAL},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY_APPOINTEE.getCode()), List.of(SubscriptionType.OTHER_PARTY), PAPER},
            new Object[]{DIRECTION_ISSUED_WELSH, ConfidentialityType.CONFIDENTIAL.getCode(), List.of(ConfidentialityPartyMembers.OTHER_PARTY_APPOINTEE.getCode()), List.of(SubscriptionType.OTHER_PARTY), ORAL},
        };
    }
}
