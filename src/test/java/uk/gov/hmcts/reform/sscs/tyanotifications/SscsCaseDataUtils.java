package uk.gov.hmcts.reform.sscs.tyanotifications;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;

public final class SscsCaseDataUtils {

    public static final String CASE_ID = "1234";

    private SscsCaseDataUtils() {
    }

    public static SscsCaseData buildSscsCaseData(String caseReference, String subscribeEmail, String subscribeSms) {
        return buildSscsCaseData(
            caseReference,
            subscribeEmail,
            subscribeSms,
            EventType.APPEAL_RECEIVED, "oral"
        );
    }

    public static SscsCaseData buildSscsCaseData(
        String caseReference,
        String subscribeEmail,
        String subscribeSms,
        EventType eventType,
        String hearingType
    ) {
        return builderSscsCaseData(caseReference, subscribeEmail, subscribeSms, eventType, hearingType).build();
    }

    public static SscsCaseData buildSscsCaseDataWelsh(
        String caseReference,
        String subscribeEmail,
        String subscribeSms,
        EventType eventType,
        String hearingType
    ) {
        SscsCaseData caseData = builderSscsCaseData(caseReference, subscribeEmail, subscribeSms, eventType, hearingType).build();
        caseData.setLanguagePreferenceWelsh("Yes");
        return caseData;
    }


    public static SscsCaseData.SscsCaseDataBuilder builderSscsCaseData(
        String caseReference,
        String subscribeEmail,
        String subscribeSms,
        EventType eventType,
        String hearingType
    ) {
        Name name = Name.builder()
            .title("Mr")
            .firstName("User")
            .lastName("Test")
            .build();
        Contact contact = Contact.builder()
            .email("test-mail@hmcts.net")
            .phone("01234567890")
            .build();
        Identity identity = Identity.builder()
            .dob("1904-03-10")
            .nino("AB 22 55 66 B")
            .build();
        Address appellantAddress = Address.builder()
            .line1("1 Appellant Ave")
            .town("Appellanton")
            .county("Appellanty")
            .postcode("TS1 1ST")
            .build();
        Appellant appellant = Appellant.builder()
            .name(name)
            .address(appellantAddress)
            .contact(contact)
            .identity(identity)
            .build();

        HearingOptions hearingOptions = HearingOptions.builder()
            .wantsToAttend("Yes")
            .wantsSupport("Yes")
            .languageInterpreter("Yes")
            .other("No")
            .build();

        final Appeal appeal = Appeal.builder()
            .appellant(appellant)
            .benefitType(BenefitType.builder().code("ESA").build())
            .hearingOptions(hearingOptions)
            .hearingType(hearingType)
            .build();

        Event events = Event.builder()
            .value(EventDetails.builder()
                .type(eventType.getCcdType())
                .description("Some Events")
                .date("2017-05-24T14:01:18.243")
                .build())
            .build();

        Subscription appellantSubscription = Subscription.builder()
            .tya("")
            .email("sscstest+notify@greencroftconsulting.com")
            .mobile("07398785050")
            .subscribeEmail(subscribeEmail)
            .subscribeSms(subscribeSms).wantSmsNotifications(subscribeSms)
            .build();
        Subscription representativeSubscription = Subscription.builder()
            .tya("")
            .email("")
            .mobile("")
            .subscribeEmail("No")
            .subscribeSms("No").wantSmsNotifications("No")
            .build();
        Subscription jointPartySubscription = Subscription.builder()
            .tya("")
            .email("")
            .mobile("")
            .subscribeEmail("No")
            .subscribeSms("No").wantSmsNotifications("No")
            .build();
        Subscriptions subscriptions = Subscriptions.builder()
            .appellantSubscription(appellantSubscription)
            .representativeSubscription(representativeSubscription)
            .jointPartySubscription(jointPartySubscription)
            .build();

        return SscsCaseData.builder()
            .caseReference(caseReference)
            .appeal(appeal)
            .events(Collections.singletonList(events))
            .subscriptions(subscriptions)
            .jointParty(JointParty.builder()
                .hasJointParty(YES)
                .jointPartyAddressSameAsAppellant(NO)
                .name(Name.builder()
                    .title("mr")
                    .firstName("Jon")
                    .lastName("Party")
                    .build())
                .address(Address.builder()
                    .line1("1 Appellant Ave")
                    .town("Appellanton")
                    .county("Appellanty")
                    .postcode("TS1 1ST")
                    .build())
                .build());
    }

    public static SscsCaseData subscribeRep(SscsCaseData sscsCaseData) {

        Representative rep = Representative.builder()
            .name(Name.builder().firstName("Harry").lastName("Potter").build())
            .hasRepresentative("Yes").build();

        sscsCaseData.getAppeal().setRep(rep);

        Subscription representativeSubscription = Subscription.builder()
            .tya("")
            .email("sscstest+notify@greencroftconsulting.com")
            .mobile("07398785050")
            .subscribeEmail("Yes")
            .subscribeSms("Yes").wantSmsNotifications("Yes")
            .build();

        Subscriptions subscriptions = Subscriptions.builder()
            .representativeSubscription(representativeSubscription)
            .build();

        sscsCaseData.setSubscriptions(subscriptions);

        return sscsCaseData;
    }

    public static CcdNotificationWrapper buildBasicCcdNotificationWrapper(NotificationEventType notificationType) {
        return buildBasicCcdNotificationWrapper(notificationType, null);
    }

    public static CcdNotificationWrapper buildBasicCcdNotificationWrapper(NotificationEventType notificationType,
                                                                          String hearingType) {
        return new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .notificationEventType(notificationType)
            .newSscsCaseData(
                SscsCaseData.builder()
                    .appeal(Appeal.builder()
                        .hearingType(hearingType)
                        .build())
                    .ccdCaseId(CASE_ID)
                    .events(Collections.emptyList())
                    .hearings(Collections.emptyList()).build())
            .build());
    }

    public static CcdNotificationWrapper buildBasicCcdNotificationWrapperWithEvent(
        NotificationEventType notificationType,
        EventType eventType,
        String eventDate
    ) {
        Event event = Event
            .builder()
            .value(EventDetails
                .builder()
                .date(eventDate)
                .type(eventType.getCcdType())
                .build()
            )
            .build();

        return new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .notificationEventType(notificationType)
            .newSscsCaseData(
                SscsCaseData.builder()
                    .ccdCaseId(CASE_ID)
                    .events(Collections.singletonList(event))
                    .hearings(Collections.emptyList())
                    .build())
            .build());
    }

    public static CcdNotificationWrapper buildBasicCcdNotificationWrapperWithHearing(
        NotificationEventType notificationType,
        String hearingDate,
        String hearingTime
    ) {
        Hearing hearing = Hearing
            .builder()
            .value(HearingDetails
                .builder()
                .hearingDate(hearingDate)
                .time(hearingTime)
                .build()
            )
            .build();

        return new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .notificationEventType(notificationType)
            .newSscsCaseData(SscsCaseData.builder()
                .ccdCaseId(CASE_ID)
                .events(Collections.emptyList())
                .hearings(Collections.singletonList(hearing))
                .build())
            .build());
    }

    public static CcdNotificationWrapper buildBasicCcdNotificationWrapperWithHearingList(
        NotificationEventType notificationType, List<Hearing> hearings
    ) {
        return new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .notificationEventType(notificationType)
            .newSscsCaseData(SscsCaseData.builder()
                .ccdCaseId(CASE_ID)
                .events(Collections.emptyList())
                .hearings(hearings)
                .build())
            .build());
    }

    public static CcdNotificationWrapper buildBasicCcdNotificationWrapperWithHearingAndHearingType(
        NotificationEventType notificationType,
        HearingType hearingType,
        String hearingDate,
        String hearingTime
    ) {
        Hearing hearing = Hearing
            .builder()
            .value(HearingDetails
                .builder()
                .hearingDate(hearingDate)
                .time(hearingTime)
                .build()
            )
            .build();

        return new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .notificationEventType(notificationType)
            .newSscsCaseData(SscsCaseData.builder()
                .ccdCaseId(CASE_ID).appeal(Appeal.builder().hearingType(hearingType.getValue()).build())
                .events(Collections.emptyList())
                .hearings(Collections.singletonList(hearing))
                .build())
            .build());
    }

    public static void addEventTypeToCase(SscsCaseData response, EventType eventType) {
        Date now = new Date();
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");

        Event events = Event.builder()
            .value(EventDetails.builder()
                .type(eventType.getCcdType())
                .description(eventType.getCcdType())
                .date(dt1.format(now))
                .build())
            .build();

        List<Event> addedEvents = new ArrayList<>(response.getEvents());
        addedEvents.add(events);
        response.setEvents(addedEvents);
    }

    public static void addEvidence(SscsCaseData response) {
        List<Document> documents = new ArrayList<>();

        Document doc = Document.builder().value(DocumentDetails.builder()
            .dateReceived("2016-01-01")
            .evidenceType("Medical")
            .evidenceProvidedBy("Caseworker").build()).build();

        documents.add(doc);

        Evidence evidence = Evidence.builder().documents(documents).build();

        response.setEvidence(evidence);
    }

    public static List<Hearing> addHearing(SscsCaseData response, Integer hearingDaysFromNow) {
        return addHearing(response, hearingDaysFromNow, null);
    }

    public static List<Hearing> addHearing(SscsCaseData response, Integer hearingDaysFromNow, String adjourned) {
        Hearing hearing = Hearing.builder().value(HearingDetails.builder()
            .hearingDate(LocalDate.now().plusDays(hearingDaysFromNow).toString())
            .adjourned(adjourned)
            .time("23:59")
            .venue(Venue.builder()
                .name("The venue")
                .address(Address.builder()
                    .line1("12 The Road Avenue")
                    .line2("Village")
                    .town("Aberdeen")
                    .county("Aberdeenshire")
                    .postcode("TS3 3ST").build())
                .googleMapLink("http://www.googlemaps.com/aberdeenvenue")
                .build()).build()).build();

        List<Hearing> hearingsList = new ArrayList<>();
        hearingsList.add(hearing);
        response.setHearings(hearingsList);

        return hearingsList;
    }

    public static void addAppointee(SscsCaseData response) {
        Appointee appointee = Appointee.builder()
            .name(Name.builder()
                .firstName("Appointee")
                .lastName("User")
                .build())
            .build();
        Subscription appointeeSubscription = Subscription.builder()
            .email("sscstest+notify2@greencroftconsulting.com")
            .mobile("07398785051")
            .subscribeEmail("Yes")
            .subscribeSms("Yes").wantSmsNotifications("Yes")
            .build();
        Subscriptions subscriptions = response.getSubscriptions().toBuilder()
            .appointeeSubscription(appointeeSubscription).build();

        response.getAppeal().getAppellant().setAppointee(appointee);
        response.setSubscriptions(subscriptions);
    }

    public static HearingOptions addHearingOptions(SscsCaseData response, String wantsToAttend) {
        HearingOptions options = HearingOptions.builder()
            .wantsToAttend(wantsToAttend)
            .build();

        return options;
    }

    public static BiFunction<Object, DateTimeFormatter, String> getWelshDate() {
        return (date, dateTimeFormatter) -> Optional.ofNullable(date).map(data -> {
            LocalDate localDate = LocalDate.parse((String) data, dateTimeFormatter);
            return LocalDateToWelshStringConverter.convert(localDate);
        }).orElse("No date present for translation");
    }
}

