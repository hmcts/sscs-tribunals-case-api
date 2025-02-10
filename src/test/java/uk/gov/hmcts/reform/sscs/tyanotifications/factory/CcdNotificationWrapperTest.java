package uk.gov.hmcts.reform.sscs.tyanotifications.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.ACTION_HEARING_RECORDING_REQUEST;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.ADJOURNED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.APPEAL_DORMANT;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.APPEAL_LAPSED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.DIRECTION_ISSUED_WELSH;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.DWP_APPEAL_LAPSED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.EVIDENCE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.HEARING_BOOKED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.HMCTS_APPEAL_LAPSED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.POSTPONEMENT;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.REQUEST_FOR_INFORMATION;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.REVIEW_CONFIDENTIALITY_REQUEST;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.values;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecordingCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.reference.data.model.ConfidentialityPartyMembers;
import uk.gov.hmcts.reform.sscs.reference.data.model.ConfidentialityType;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;

@RunWith(JUnitParamsRunner.class)
public class CcdNotificationWrapperTest {

    private CcdNotificationWrapper ccdNotificationWrapper;
    private static final String PAPER = "paper";
    private static final String ORAL = "oral";

    @Test
    @Parameters({"paper, PAPER", "oral, ORAL"})
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

    @Test
    @Parameters({"APPEAL_LAPSED", "HMCTS_APPEAL_LAPSED", "DWP_APPEAL_LAPSED", "APPEAL_WITHDRAWN", "EVIDENCE_RECEIVED",
        "POSTPONEMENT", "HEARING_BOOKED", "SYA_APPEAL_CREATED", "VALID_APPEAL_CREATED",
        "RESEND_APPEAL_CREATED", "APPEAL_RECEIVED", "ADJOURNED", "ISSUE_FINAL_DECISION_WELSH",
        "PROCESS_AUDIO_VIDEO", "PROCESS_AUDIO_VIDEO_WELSH", "ACTION_POSTPONEMENT_REQUEST", "ACTION_POSTPONEMENT_REQUEST_WELSH",
        "APPEAL_DORMANT", "DWP_RESPONSE_RECEIVED", "STRUCK_OUT", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH"})
    public void givenSubscriptions_shouldGetAppellantAndRepSubscriptionTypeList(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters({"DEATH_OF_APPELLANT", "PROVIDE_APPOINTEE_DETAILS"})
    public void givenSubscriptions_shouldGetAppointeeAndRepSubscriptionTypeList(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndRep(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters({"SYA_APPEAL_CREATED, cor", "DWP_RESPONSE_RECEIVED, oral",
        "DWP_RESPONSE_RECEIVED, paper", "HMCTS_APPEAL_LAPSED, paper", "HMCTS_APPEAL_LAPSED, oral",
        "DWP_APPEAL_LAPSED, paper", "DWP_APPEAL_LAPSED, oral", "SUBSCRIPTION_UPDATED, paper",
        "VALID_APPEAL_CREATED, cor", "RESEND_APPEAL_CREATED, cor"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointee(NotificationEventType notificationEventType, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters({"APPEAL_LAPSED, paper", "APPEAL_LAPSED, oral", "EVIDENCE_REMINDER, oral", "EVIDENCE_REMINDER, paper",
        "APPEAL_DORMANT, paper", "APPEAL_DORMANT, oral", "ADJOURNED, paper", "ADJOURNED, oral", "POSTPONEMENT, paper", "POSTPONEMENT, oral",
        "EVIDENCE_RECEIVED, paper", "EVIDENCE_RECEIVED, oral", "APPEAL_WITHDRAWN, paper", "STRUCK_OUT, oral", "STRUCK_OUT, paper", "DIRECTION_ISSUED, oral", "DIRECTION_ISSUED, paper",
        "DIRECTION_ISSUED_WELSH, oral", "DIRECTION_ISSUED_WELSH, paper", "DWP_UPLOAD_RESPONSE, paper",
        "PROCESS_AUDIO_VIDEO, oral", "PROCESS_AUDIO_VIDEO_WELSH, paper", "ACTION_POSTPONEMENT_REQUEST, paper", "ACTION_POSTPONEMENT_REQUEST_WELSH, paper",
        "DWP_UPLOAD_RESPONSE, oral", "HEARING_BOOKED, oral", "HEARING_BOOKED, paper", "HEARING_REMINDER, oral", "HEARING_REMINDER, paper",
        "ISSUE_ADJOURNMENT_NOTICE, paper", "ISSUE_ADJOURNMENT_NOTICE_WELSH, oral"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointeeAndJointParty(NotificationEventType notificationEventType, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters(method = "getDirectionIssuedSubscriptionBasedOnConfidentialityForAppellantAndRepresentative")
    public void givenSubscriptions_shouldGetAppellantAndRepSubscriptionTypeWhenConfidentialIsSelected(NotificationEventType notificationEventType, String confidentialityType, List<String> chosenMembers, List<SubscriptionType> requiredMembers) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(notificationEventType);
        SscsCaseData sscsCaseData = ccdNotificationWrapper.getNewSscsCaseData();
        sscsCaseData.setConfidentialityType(confidentialityType);
        chosenMembers.forEach(o -> createPartiesOnTheCase(sscsCaseData, o));

        assertDirectionsNoticeConfidentiality(sscsCaseData, chosenMembers, requiredMembers);
    }

    @Test
    @Parameters(method = "getDirectionIssuedSubscriptionBasedOnConfidentialityForRestOfTheOtherParties")
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
        Assert.assertEquals(requiredMembers.size(), subsWithTypeList.size());
        subsWithTypeList.forEach(o -> Assert.assertTrue(requiredMembers.contains(o.getSubscriptionType())));
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
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    public void givenSubscriptionForAppellantRepAndJointParty_shouldGetSubscriptionTypeListForAppellantOnlyWhenOnlyAppellantGranted() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(REVIEW_CONFIDENTIALITY_REQUEST, Representative.builder().hasRepresentative("Yes").build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeJointParty(null);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenSubscriptionForAppellantRepAndJointParty_shouldGetSubscriptionTypeListForJointPartyOnlyWhenOnlyJointPartyGranted() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(REVIEW_CONFIDENTIALITY_REQUEST, Representative.builder().hasRepresentative("Yes").build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeAppellant(null);
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenSubscriptionForAppellantRepAndJointParty_shouldGetSubscriptionTypeListForJointPartyOnlyWhenOnlyJointPartyIsNewlyGranted() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(REVIEW_CONFIDENTIALITY_REQUEST, Representative.builder().hasRepresentative("Yes").build());
        ccdNotificationWrapper.getOldSscsCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        ccdNotificationWrapper.getOldSscsCaseData().setConfidentialityRequestOutcomeJointParty(null);
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters(method = "getEventTypeFilteredWithAppellant")
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppellant(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters({"DIRECTION_ISSUED, paper, PROVIDE_INFORMATION", "DIRECTION_ISSUED, oral, PROVIDE_INFORMATION",
        "DIRECTION_ISSUED, paper, APPEAL_TO_PROCEED", "DIRECTION_ISSUED, oral, APPEAL_TO_PROCEED",
        "DIRECTION_ISSUED, paper, GRANT_EXTENSION", "DIRECTION_ISSUED, oral, GRANT_EXTENSION",
        "DIRECTION_ISSUED, paper, REFUSE_EXTENSION", "DIRECTION_ISSUED, oral, REFUSE_EXTENSION",})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointeeAndJointPartyDirection(NotificationEventType notificationEventType, String hearingType, DirectionType directionType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType, directionType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    public void givenRequestForInformationForAppointeeWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(REQUEST_FOR_INFORMATION, ORAL);
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.APPELLANT.getCode(), PartyItemList.APPELLANT.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenRequestForInformationForAppellantWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(REQUEST_FOR_INFORMATION);
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.APPELLANT.getCode(), PartyItemList.APPELLANT.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenRequestForInformationForRepWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(REQUEST_FOR_INFORMATION);
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.REPRESENTATIVE.getCode(), PartyItemList.REPRESENTATIVE.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenRequestForInformationForJointPartyWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(REQUEST_FOR_INFORMATION, PAPER);
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.JOINT_PARTY.getCode(), PartyItemList.JOINT_PARTY.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenProcessHearingRequestForRepWithSubscription_shouldSendProcessHearingRequestNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(ACTION_HEARING_RECORDING_REQUEST);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenProcessHearingRequestForJointPartyWithSubscription_shouldSendProcessHearingRequestNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(ACTION_HEARING_RECORDING_REQUEST, null);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenProcessHearingRequestForNoPartyWithSubscription_shouldNotSendProcessHearingRequestNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(ACTION_HEARING_RECORDING_REQUEST, null, null, false);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertTrue(subsWithTypeList.isEmpty());
    }

    @Test
    public void givenNoOtherPartyInTheCase_thenReturnEmptySubscription() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, null);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions(ccdNotificationWrapper.getNewSscsCaseData(), ccdNotificationWrapper.getNotificationType());
        Assert.assertTrue(subsWithTypeList.isEmpty());
    }

    @Test
    public void givenUpdateOtherPartyDataEventAndSendNotificationFlagIsNotSetInOtherParty_thenReturnEmptySubscription() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, buildOtherPartyData(false, true, true));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions(ccdNotificationWrapper.getNewSscsCaseData(), ccdNotificationWrapper.getNotificationType());
        Assert.assertTrue(subsWithTypeList.isEmpty());
    }

    @Test
    public void givenUpdateOtherPartyDataEventAndSendNotificationFlagIsSetInOtherPartyWithAppointee_thenReturnAllOtherPartySubscription() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, buildOtherPartyData(true, true, true));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions(ccdNotificationWrapper.getNewSscsCaseData(), ccdNotificationWrapper.getNotificationType());
        Assertions.assertThat(subsWithTypeList)
            .hasSize(2)
            .extracting(SubscriptionWithType::getPartyId)
            .containsOnly("2", "3");
    }

    @Test
    public void givenUpdateOtherPartyDataEventAndSendNotificationFlagIsSetInOtherPartyWithNoAppointee_thenReturnAllOtherPartySubscription() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, buildOtherPartyData(true, false, true));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions(ccdNotificationWrapper.getNewSscsCaseData(), ccdNotificationWrapper.getNotificationType());
        Assertions.assertThat(subsWithTypeList)
            .hasSize(2)
            .extracting(SubscriptionWithType::getPartyId)
            .containsOnly("1", "3");
    }

    @Test
    public void givenUpdateOtherPartyDataEventAndSendNotificationFlagIsSetInOtherPartyWithNoAppointee_thenReturnAllOtherPartySubscription2() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, buildOtherPartyData(true, true, true));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions(ccdNotificationWrapper.getNewSscsCaseData(), ccdNotificationWrapper.getNotificationType());
        Assertions.assertThat(subsWithTypeList)
            .hasSize(2)
            .extracting(SubscriptionWithType::getPartyId)
            .containsOnly("2", "3");
    }

    @Test
    @Parameters({
        "ADJOURNED",
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
        Assertions.assertThat(subsWithTypeList)
            .hasSize(2)
            .extracting(SubscriptionWithType::getPartyId)
            .contains("1");
    }

    @SuppressWarnings({"unused"})
    private Object[] getEventTypeFilteredWithAppellant() {
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
    private Object[] getDirectionIssuedSubscriptionBasedOnConfidentialityForAppellantAndRepresentative() {
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
    private Object[] getDirectionIssuedSubscriptionBasedOnConfidentialityForRestOfTheOtherParties() {
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
