package uk.gov.hmcts.reform.sscs.tyanotifications.personalisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationConfiguration.PersonalisationKey.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.APPEAL_RECEIVED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationConfiguration;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;

public class SyaAppealCreatedAndReceivedPersonalisationTest extends PersonalisationTest {

    private static final String CASE_ID = "54321";
    private static final String YES = "yes";

    SscsCaseData response;

    @InjectMocks
    @Resource
    SyaAppealCreatedAndReceivedPersonalisation syaAppealCreatedAndReceivedPersonalisation;

    @Spy
    private PersonalisationConfiguration syaAppealCreatedPersonalisationConfiguration;

    @Before
    public void setup() {
        openMocks(this);
        Map<String, String> englishMap = new HashMap<>();
        englishMap.put(ATTENDING_HEARING.name(), "Attending the hearing: ");
        englishMap.put(YESSTRING.name(), "yes");
        englishMap.put(NOSTRING.name(), "no");
        englishMap.put(DATES_NOT_ATTENDING.name(), "Dates you can't attend: ");
        englishMap.put(DATE_OF_MRN.name(), "Date of MRN: ");
        englishMap.put(REASON_FOR_LATE_APPEAL.name(), "Reason for late appeal: ");
        englishMap.put(REASON_FOR_NO_MRN.name(), "Reason for no MRN: ");
        englishMap.put(PersonalisationConfiguration.PersonalisationKey.NAME.name(), "Name: ");
        englishMap.put(DATE_OF_BIRTH.name(), "Date of birth: ");
        englishMap.put(NINO.name(), "National Insurance number: ");
        englishMap.put(ADDRESS.name(), "Address: ");
        englishMap.put(EMAIL.name(), "Email: ");
        englishMap.put(PHONE.name(), "Phone: ");
        englishMap.put(RECEIVE_TEXT_MESSAGE_REMINDER.name(), "Receive text message reminders: ");
        englishMap.put(MOBILE.name(), "Mobile number: ");
        englishMap.put(HAVE_AN_APPOINTEE.name(), "Have an appointee: ");
        englishMap.put(NOT_PROVIDED.name(), "Not provided");
        englishMap.put(HAVE_A_REPRESENTATIVE.name(), "Have a representative: ");
        englishMap.put(ORGANISATION.name(), "Organisation: ");
        englishMap.put(WHAT_DISAGREE_WITH.name(), "What you disagree with: ");
        englishMap.put(WHY_DISAGREE_WITH.name(), "Why you disagree with it: ");
        englishMap.put(ANYTHING.name(), "Anything else you want to tell the tribunal: ");
        englishMap.put(LANGUAGE_INTERPRETER.name(), "Language interpreter: ");
        englishMap.put(SIGN_INTERPRETER.name(), "Sign interpreter: ");
        englishMap.put(HEARING_LOOP.name(), "Hearing loop: ");
        englishMap.put(DISABLED_ACCESS.name(), "Disabled access: ");
        englishMap.put(OTHER_ARRANGEMENTS.name(), "Any other arrangements: ");
        englishMap.put(REQUIRED.name(), "Required");
        englishMap.put(NOT_REQUIRED.name(), "Not required");
        englishMap.put(OTHER.name(), "Other");
        englishMap.put(CHILD_MAINTENANCE_NUMBER.name(), "Child maintenance number: ");

        Map<String, String> welshMap = new HashMap<>();
        welshMap.put(ATTENDING_HEARING.name(), "Ydych chi'n bwriadu mynychu'r gwrandawiad: ");
        welshMap.put(YESSTRING.name(), "ydw");
        welshMap.put(NOSTRING.name(), "nac ydw");
        welshMap.put(DATES_NOT_ATTENDING.name(), "Dyddiadau na allwch fynychu: ");
        welshMap.put(DATE_OF_MRN.name(), "Dyddiad yr MRN: ");
        welshMap.put(REASON_FOR_LATE_APPEAL.name(), "Rheswm dros apêl hwyr: ");
        welshMap.put(REASON_FOR_NO_MRN.name(), "Rheswm dros ddim MRN: ");
        welshMap.put(PersonalisationConfiguration.PersonalisationKey.NAME.name(), "Enw: ");
        welshMap.put(DATE_OF_BIRTH.name(), "Dyddiad geni: ");
        welshMap.put(NINO.name(), "Rhif Yswiriant Gwladol: ");
        welshMap.put(ADDRESS.name(), "Cyfeiriad: ");
        welshMap.put(EMAIL.name(), "E-bost: ");
        welshMap.put(PHONE.name(), "Rhif ffôn: ");
        welshMap.put(RECEIVE_TEXT_MESSAGE_REMINDER.name(), "Eisiau negeseuon testun atgoffa: ");
        welshMap.put(MOBILE.name(), "Rhif ffôn symudol: ");
        welshMap.put(HAVE_AN_APPOINTEE.name(), "A oes gennych chi benodai: ");
        welshMap.put(NOT_PROVIDED.name(), "Nis ddarparwyd");
        welshMap.put(HAVE_A_REPRESENTATIVE.name(), "A oes gennych chi gynrychiolydd: ");
        welshMap.put(ORGANISATION.name(), "Sefydliad: ");
        welshMap.put(WHAT_DISAGREE_WITH.name(), "Beth ydych chi’n anghytuno ag o: ");
        welshMap.put(WHY_DISAGREE_WITH.name(), "Pam ydych chi’n anghytuno ag o: ");
        welshMap.put(ANYTHING.name(), "Unrhyw beth arall yr hoffech ddweud wrth y tribiwnlys: ");
        welshMap.put(LANGUAGE_INTERPRETER.name(), "Dehonglydd iaith arwyddion: ");
        welshMap.put(SIGN_INTERPRETER.name(), "Dehonglydd iaith arwyddion: ");
        welshMap.put(HEARING_LOOP.name(), "Dolen glyw: ");
        welshMap.put(DISABLED_ACCESS.name(), "Mynediad i bobl anab: ");
        welshMap.put(OTHER_ARRANGEMENTS.name(), "Unrhyw drefniadau eraill: ");
        welshMap.put(REQUIRED.name(), "Gofynnol");
        welshMap.put(NOT_REQUIRED.name(), "Dim yn ofynnol");
        welshMap.put(OTHER.name(), "Arall");
        welshMap.put(CHILD_MAINTENANCE_NUMBER.name(), "Child maintenance number placeholder: ");

        Map<LanguagePreference, Map<String, String>> personalisations = new HashMap<>();
        personalisations.put(LanguagePreference.ENGLISH, englishMap);
        personalisations.put(LanguagePreference.WELSH, welshMap);

        super.setup();

        syaAppealCreatedPersonalisationConfiguration.setPersonalisation(personalisations);
    }

    @Test
    public void givenAnAppeal_setMrnDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("3 May 2018")
                    .mrnLateReason("My train was cancelled.")
                    .mrnMissingReason("My dog ate my homework.")
                    .build())
                .build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setMrnDetails(new HashMap<>(), response);

        assertEquals("Date of MRN: 3 May 2018\n"
                + "\nReason for late appeal: My train was cancelled.\n"
                + "\nReason for no MRN: My dog ate my homework.",
            result.get(MRN_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppeal_setMrnDetailsForTemplateWhenReasonForNoMrnMissing() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().mrnDate("3 May 2018").mrnLateReason("My train was cancelled.").build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setMrnDetails(new HashMap<>(), response);

        assertEquals("Date of MRN: 3 May 2018\n"
                + "\nReason for late appeal: My train was cancelled.",
            result.get(MRN_DETAILS_LITERAL));
        assertNull("Welsh mrn details should be set", result.get(HEARING_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenAnAppeal_setMrnDetailsForTemplateWhenReasonForNoMrnMissing_welsh() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .languagePreferenceWelsh("Yes")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().mrnDate("2018-05-03").mrnLateReason("My train was cancelled.").build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setMrnDetails(new HashMap<>(), response);

        assertEquals("Date of MRN: 2018-05-03\n"
                + "\nReason for late appeal: My train was cancelled.",
            result.get(MRN_DETAILS_LITERAL));

        assertEquals("Dyddiad yr MRN: 3 Mai 2018\n"
                + "\nRheswm dros apêl hwyr: My train was cancelled.",
            result.get(MRN_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenAnAppeal_setYourDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build())
                    .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .contact(Contact.builder().email("manish.sharma@gmail.com").phone("0797 243 8179").build())
                    .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Name: Manish Sharma\n"
                + "\nDate of birth: 12 March 1971\n"
                + "\nNational Insurance number: NP 27 28 67 B\n"
                + "\nAddress: 122 Breach Street, The Village, My town, Cardiff, CF11 2HB\n"
                + "\nEmail: manish.sharma@gmail.com\n"
                + "\nPhone: 0797 243 8179",
            result.get(YOUR_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppeal_setYourDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("1971-03-12").build())
                    .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .contact(Contact.builder().email("manish.sharma@gmail.com").phone("0797 243 8179").build())
                    .build()).build())
            .languagePreferenceWelsh("Yes")
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Name: Manish Sharma\n"
                + "\nDate of birth: 1971-03-12\n"
                + "\nNational Insurance number: NP 27 28 67 B\n"
                + "\nAddress: 122 Breach Street, The Village, My town, Cardiff, CF11 2HB\n"
                + "\nEmail: manish.sharma@gmail.com\n"
                + "\nPhone: 0797 243 8179",
            result.get(YOUR_DETAILS_LITERAL));

        assertEquals("Enw: Manish Sharma\n"
                + "\nDyddiad geni: 12 Mawrth 1971\n"
                + "\nRhif Yswiriant Gwladol: NP 27 28 67 B\n"
                + "\nCyfeiriad: 122 Breach Street, The Village, My town, Cardiff, CF11 2HB\n"
                + "\nE-bost: manish.sharma@gmail.com\n"
                + "\nRhif ffôn: 0797 243 8179",
            result.get(YOUR_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenAnAppealWithNoEmailOrPhoneProvided_setYourDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build())
                    .address(Address.builder().line1("122 Breach Street").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .contact(Contact.builder().build())
                    .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Name: Manish Sharma\n"
                + "\nDate of birth: 12 March 1971\n"
                + "\nNational Insurance number: NP 27 28 67 B\n"
                + "\nAddress: 122 Breach Street, My town, Cardiff, CF11 2HB\n"
                + "\nEmail: Not provided\n"
                + "\nPhone: Not provided",
            result.get(YOUR_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithNoEmailOrPhoneProvided_setYourDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("1971-03-12").build())
                    .address(Address.builder().line1("122 Breach Street").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .contact(Contact.builder().build())
                    .build()).build())
            .languagePreferenceWelsh("yes")
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Name: Manish Sharma\n"
                + "\nDate of birth: 1971-03-12\n"
                + "\nNational Insurance number: NP 27 28 67 B\n"
                + "\nAddress: 122 Breach Street, My town, Cardiff, CF11 2HB\n"
                + "\nEmail: Not provided\n"
                + "\nPhone: Not provided",
            result.get(YOUR_DETAILS_LITERAL));

        assertEquals("Enw: Manish Sharma\n"
                + "\nDyddiad geni: 12 Mawrth 1971\n"
                + "\nRhif Yswiriant Gwladol: NP 27 28 67 B\n"
                + "\nCyfeiriad: 122 Breach Street, My town, Cardiff, CF11 2HB\n"
                + "\nE-bost: Nis ddarparwyd\n"
                + "\nRhif ffôn: Nis ddarparwyd",
            result.get(YOUR_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenAnAppealWithTextMessageReminders_setTextMessageReminderDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .subscribeSms("Yes").wantSmsNotifications("Yes")
                    .mobile("07955555708").build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setTextMessageReminderDetails(new HashMap<>(), response.getSubscriptions().getAppellantSubscription());

        assertEquals("Receive text message reminders: yes\n"
                + "\nMobile number: 07955555708",
            result.get(TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithTextMessageReminders_setTextMessageReminderDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .subscribeSms("Yes").wantSmsNotifications("Yes")
                    .mobile("07955555708").build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setTextMessageReminderDetails(new HashMap<>(), response.getSubscriptions().getAppellantSubscription());

        assertEquals("Receive text message reminders: yes\n"
                + "\nMobile number: 07955555708",
            result.get(TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));

        assertEquals("Eisiau negeseuon testun atgoffa: ydw\n"
                + "\nRhif ffôn symudol: 07955555708",
            result.get(TEXT_MESSAGE_REMINDER_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenAnAppealWithNoTextMessageReminders_setTextMessageReminderDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .subscribeSms("No").build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setTextMessageReminderDetails(new HashMap<>(), response.getSubscriptions().getAppellantSubscription());

        assertEquals("Receive text message reminders: no",
            result.get(TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithNoTextMessageReminders_setTextMessageReminderDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .subscribeSms("No").build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setTextMessageReminderDetails(new HashMap<>(), response.getSubscriptions().getAppellantSubscription());

        assertEquals("Receive text message reminders: no",
            result.get(TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));

        assertEquals("Eisiau negeseuon testun atgoffa: nac ydw",
            result.get(TEXT_MESSAGE_REMINDER_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenAnAppealWithAppointee_setAppointeeDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().appellant(Appellant.builder()
                .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build())
                .address(Address.builder().line1("122 Breach Street").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                .appointee(Appointee.builder().name(Name.builder().firstName("Peter").lastName("Smith").build())
                    .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                    .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                    .identity(Identity.builder().dob("12 March 1981").build())
                    .build())
                .contact(Contact.builder().build()).build()).build()).build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setAppointeeDetails(new HashMap<>(), response);

        assertEquals("Have an appointee: yes\n"
                + "\nName: Peter Smith\n"
                + "\nDate of birth: 12 March 1981\n"
                + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nEmail: peter.smith@cab.org.uk\n"
                + "\nPhone: 03444 77 1010",
            result.get(APPOINTEE_DETAILS_LITERAL));

        assertNull(result.get(APPOINTEE_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenAnAppealWithAppointee_setAppointeeDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .languagePreferenceWelsh("yes")
            .appeal(Appeal.builder().appellant(Appellant.builder()
                .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                .identity(Identity.builder().nino("NP 27 28 67 B").dob("1971-03-12").build())
                .address(Address.builder().line1("122 Breach Street").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                .appointee(Appointee.builder().name(Name.builder().firstName("Peter").lastName("Smith").build())
                    .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                    .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                    .identity(Identity.builder().dob("1971-03-12").build())
                    .build())
                .contact(Contact.builder().build()).build()).build()).build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setAppointeeDetails(new HashMap<>(), response);

        assertEquals("Have an appointee: yes\n"
                + "\nName: Peter Smith\n"
                + "\nDate of birth: 1971-03-12\n"
                + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nEmail: peter.smith@cab.org.uk\n"
                + "\nPhone: 03444 77 1010",
            result.get(APPOINTEE_DETAILS_LITERAL));

        assertEquals("A oes gennych chi benodai: ydw\n"
                + "\nEnw: Peter Smith\n"
                + "\nDyddiad geni: 12 Mawrth 1971\n"
                + "\nCyfeiriad: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nE-bost: peter.smith@cab.org.uk\n"
                + "\nRhif ffôn: 03444 77 1010",
            result.get(APPOINTEE_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenAnAppealWithAppointeeAndNoEmailOrPhone_setAppointeeDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().appellant(Appellant.builder().appointee(Appointee.builder()
                    .name(Name.builder().firstName("Peter").lastName("Smith").build())
                    .identity(Identity.builder().dob("12 March 1981").build())
                    .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                    .contact(Contact.builder().build())
                    .build()).build())
                .build()).build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setAppointeeDetails(new HashMap<>(), response);

        assertEquals("Have an appointee: yes\n"
                + "\nName: Peter Smith\n"
                + "\nDate of birth: 12 March 1981\n"
                + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nEmail: Not provided\n"
                + "\nPhone: Not provided",
            result.get(APPOINTEE_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithNoAppointee_setAppointeeDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().appellant(Appellant.builder()
                    .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                    .address(Address.builder().line1("122 Breach Street").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .build())
                .build()).build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setAppointeeDetails(new HashMap<>(), response);

        assertEquals("Have an appointee: no",
            result.get(APPOINTEE_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithRepresentative_setRepresentativeDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(YES)
                .name(Name.builder().firstName("Peter").lastName("Smith").build())
                .organisation("Citizens Advice")
                .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n"
                + "\nName: Peter Smith\n"
                + "\nOrganisation: Citizens Advice\n"
                + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nEmail: peter.smith@cab.org.uk\n"
                + "\nPhone: 03444 77 1010",
            result.get(REPRESENTATIVE_DETAILS_LITERAL));

        assertNull(result.get(REPRESENTATIVE_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenAnAppealWithRepresentativeAndAppellantHasNoOtherParty_setDefaultOtherPartyDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")

            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(name)
                    .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .contact(Contact.builder().build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build()).build())
                .rep(Representative.builder().hasRepresentative(YES)
                    .name(Name.builder().firstName("Peter").lastName("Smith").build())
                    .organisation("Citizens Advice")
                    .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                    .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                    .build())
                .hearingOptions(HearingOptions.builder().arrangements(new ArrayList<>()).build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(APPEAL_RECEIVED).build(),
            new SubscriptionWithType(subscriptions.getRepresentativeSubscription(), REPRESENTATIVE,
                response.getAppeal().getAppellant(), response.getAppeal().getRep()));

        assertNull(result.get(REPRESENTATIVE_DETAILS_LITERAL_WELSH));
        assertEquals("No", result.get(SHOW_OTHER_PARTY_DETAILS));
        assertEquals("", result.get(OTHER_PARTY_DETAILS));
        assertEquals("", result.get(OTHER_PARTY_DETAILS_WELSH));
        assertThat(result.get(YOUR_DETAILS_LITERAL).toString()).doesNotContain("Child maintenance number:");
    }

    @Test
    public void givenAnAppealWithRepresentativeAndAppellantHasOtherParty_setOtherPartyDetailsForTemplate() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .name(Name.builder().title("Mr").firstName("Harrison").lastName("Kane").build())
            .address(Address.builder().line1("First Floor").line2("My Building").town("222 Corporation Street").county("Glasgow").postcode("GL11 6TF").build())
            .build()).build();
        otherPartyList.add(ccdValue);

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(name)
                    .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .contact(Contact.builder().build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build()).build())
                .rep(Representative.builder().hasRepresentative(YES)
                    .name(Name.builder().firstName("Peter").lastName("Smith").build())
                    .organisation("Citizens Advice")
                    .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                    .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                    .build())
                .hearingOptions(HearingOptions.builder().arrangements(new ArrayList<>()).build()).build())
            .otherParties(otherPartyList)
            .childMaintenanceNumber("123456")
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(APPEAL_RECEIVED).build(),
            new SubscriptionWithType(subscriptions.getRepresentativeSubscription(), REPRESENTATIVE,
                response.getAppeal().getAppellant(), response.getAppeal().getRep()));

        assertEquals("Yes", result.get(SHOW_OTHER_PARTY_DETAILS));

        assertEquals("Name: Harrison Kane\n"
                + "\nAddress: First Floor, My Building, 222 Corporation Street, Glasgow, GL11 6TF\n\n",
            result.get(OTHER_PARTY_DETAILS));
        assertNull(result.get(OTHER_PARTY_DETAILS_WELSH));
        assertThat(result.get(YOUR_DETAILS_LITERAL).toString()).contains("Child maintenance number: 123456");
    }

    @Test
    public void givenAnAppealWithRepresentativeAndAppellantHasTwoOtherParties_setOtherPartiesDetailsForTemplate() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .name(Name.builder().title("Mr").firstName("Harrison").lastName("Kane").build())
            .address(Address.builder().line1("First Floor").line2("My Building").town("222 Corporation Street").county("Glasgow").postcode("GL11 6TF").build())
            .build()).build();
        CcdValue<OtherParty> ccdValue2 = CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .name(Name.builder().title("Mr").firstName("Lucas").lastName("Moura").build())
            .address(Address.builder().line1("Second Floor").line2("My House").town("333 Corporation Street").county("London").postcode("EC1 6TF").build())
            .build()).build();
        otherPartyList.add(ccdValue);
        otherPartyList.add(ccdValue2);

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(name)
                    .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .contact(Contact.builder().build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build()).build())
                .rep(Representative.builder().hasRepresentative(YES)
                    .name(Name.builder().firstName("Peter").lastName("Smith").build())
                    .organisation("Citizens Advice")
                    .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                    .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                    .build())
                .hearingOptions(HearingOptions.builder().arrangements(new ArrayList<>()).build()).build())
            .otherParties(otherPartyList)
            .childMaintenanceNumber("123456")
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(APPEAL_RECEIVED).build(),
            new SubscriptionWithType(subscriptions.getRepresentativeSubscription(), REPRESENTATIVE,
                response.getAppeal().getAppellant(), response.getAppeal().getRep()));

        assertEquals("Yes", result.get(SHOW_OTHER_PARTY_DETAILS));

        assertEquals("Name: Harrison Kane\n"
                + "\nAddress: First Floor, My Building, 222 Corporation Street, Glasgow, GL11 6TF\n\n"
                + "Name: Lucas Moura\n"
                + "\nAddress: Second Floor, My House, 333 Corporation Street, London, EC1 6TF\n\n",
            result.get(OTHER_PARTY_DETAILS));
        assertNull(result.get(OTHER_PARTY_DETAILS_WELSH));
        assertThat(result.get(YOUR_DETAILS_LITERAL).toString()).contains("Child maintenance number: 123456");
    }

    @Test
    public void givenAnAppealWithOtherPartyWithEmptyNameAndEmptyAddress_setOtherPartiesDetailsForTemplate() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .name(null)
            .address(null)
            .build()).build();
        otherPartyList.add(ccdValue);

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(name)
                    .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .contact(Contact.builder().build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build()).build())
                .rep(Representative.builder().hasRepresentative(YES)
                    .name(Name.builder().firstName("Peter").lastName("Smith").build())
                    .organisation("Citizens Advice")
                    .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                    .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                    .build())
                .hearingOptions(HearingOptions.builder().arrangements(new ArrayList<>()).build()).build())
            .otherParties(otherPartyList)
            .childMaintenanceNumber("123456")
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(APPEAL_RECEIVED).build(),
            new SubscriptionWithType(subscriptions.getRepresentativeSubscription(), REPRESENTATIVE,
                response.getAppeal().getAppellant(), response.getAppeal().getRep()));

        assertEquals("Yes", result.get(SHOW_OTHER_PARTY_DETAILS));

        assertEquals("Name: Not provided\n"
                + "\nAddress: Not provided\n\n",
            result.get(OTHER_PARTY_DETAILS));
        assertNull(result.get(OTHER_PARTY_DETAILS_WELSH));
        assertThat(result.get(YOUR_DETAILS_LITERAL).toString()).contains("Child maintenance number: 123456");
    }

    @Test
    public void givenAnAppealWithRepresentative_setRepresentativeDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .languagePreferenceWelsh("yes")
            .appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(YES)
                .name(Name.builder().firstName("Peter").lastName("Smith").build())
                .organisation("Citizens Advice")
                .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n"
                + "\nName: Peter Smith\n"
                + "\nOrganisation: Citizens Advice\n"
                + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nEmail: peter.smith@cab.org.uk\n"
                + "\nPhone: 03444 77 1010",
            result.get(REPRESENTATIVE_DETAILS_LITERAL));

        assertEquals("A oes gennych chi gynrychiolydd: ydw\n"
                + "\nEnw: Peter Smith\n"
                + "\nSefydliad: Citizens Advice\n"
                + "\nCyfeiriad: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nE-bost: peter.smith@cab.org.uk\n"
                + "\nRhif ffôn: 03444 77 1010",
            result.get(REPRESENTATIVE_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenAnAppealWithRepresentativeAndNoEmailOrPhoneOrOrganisationProvided_setRepresentativeDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(YES)
                .name(Name.builder().firstName("Peter").lastName("Smith").build())
                .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                .contact(Contact.builder().build())
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n"
                + "\nName: Peter Smith\n"
                + "\nOrganisation: Not provided\n"
                + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nEmail: Not provided\n"
                + "\nPhone: Not provided",
            result.get(REPRESENTATIVE_DETAILS_LITERAL));
        assertNull(result.get(REPRESENTATIVE_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenAnAppealWithRepresentativeAndNoEmailOrPhoneOrOrganisationProvided_setRepresentativeDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .languagePreferenceWelsh("yes")
            .appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(YES)
                .name(Name.builder().firstName("Peter").lastName("Smith").build())
                .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                .contact(Contact.builder().build())
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n"
                + "\nName: Peter Smith\n"
                + "\nOrganisation: Not provided\n"
                + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nEmail: Not provided\n"
                + "\nPhone: Not provided",
            result.get(REPRESENTATIVE_DETAILS_LITERAL));

        assertEquals("A oes gennych chi gynrychiolydd: ydw\n"
                + "\nEnw: Peter Smith\n"
                + "\nSefydliad: Nis ddarparwyd\n"
                + "\nCyfeiriad: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nE-bost: Nis ddarparwyd\n"
                + "\nRhif ffôn: Nis ddarparwyd",
            result.get(REPRESENTATIVE_DETAILS_LITERAL_WELSH));


    }

    @Test
    public void givenAnAppealWithNoRepresentative_setRepresentativeDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder()
                .build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: no",
            result.get(REPRESENTATIVE_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithOneReasonForAppealing_setReasonForAppealingDetailsForTemplate() {
        List<AppealReason> appealReasonList = new ArrayList<>();
        AppealReason reason = AppealReason.builder().value(AppealReasonDetails.builder().reason("I want to appeal").description("Because I do").build()).build();
        appealReasonList.add(reason);

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().appealReasons(AppealReasons.builder().reasons(appealReasonList).otherReasons("Some other reason").build())
                .build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("What you disagree with: I want to appeal\n"
                + "\nWhy you disagree with it: Because I do\n"
                + "\nAnything else you want to tell the tribunal: Some other reason",
            result.get(REASONS_FOR_APPEALING_DETAILS_LITERAL));
        assertNull(result.get(REASONS_FOR_APPEALING_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenASyaAppealWithOneReasonForAppealing_setReasonForAppealingDetailsForTemplate_Welsh() {
        List<AppealReason> appealReasonList = new ArrayList<>();
        AppealReason reason = AppealReason.builder().value(AppealReasonDetails.builder().reason("I want to appeal").description("Because I do").build()).build();
        appealReasonList.add(reason);

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().appealReasons(AppealReasons.builder().reasons(appealReasonList).otherReasons("Some other reason").build())
                .build())
            .languagePreferenceWelsh("yes")
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("What you disagree with: I want to appeal\n"
                + "\nWhy you disagree with it: Because I do\n"
                + "\nAnything else you want to tell the tribunal: Some other reason",
            result.get(REASONS_FOR_APPEALING_DETAILS_LITERAL));

        assertEquals("Beth ydych chi’n anghytuno ag o: I want to appeal\n"
                + "\nPam ydych chi’n anghytuno ag o: Because I do\n"
                + "\nUnrhyw beth arall yr hoffech ddweud wrth y tribiwnlys: Some other reason",
            result.get(REASONS_FOR_APPEALING_DETAILS_LITERAL_WELSH));

    }

    @Test
    public void givenASyaAppealWithMultipleReasonsForAppealing_setReasonForAppealingDetailsForTemplate() {
        List<AppealReason> appealReasonList = new ArrayList<>();
        AppealReason reason1 = AppealReason.builder().value(AppealReasonDetails.builder().reason("I want to appeal").description("Because I do").build()).build();
        AppealReason reason2 = AppealReason.builder().value(AppealReasonDetails.builder().reason("I want to appeal again").description("I'm in the mood").build()).build();
        appealReasonList.add(reason1);
        appealReasonList.add(reason2);

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().appealReasons(AppealReasons.builder().reasons(appealReasonList).otherReasons("Some other reason").build())
                .build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("What you disagree with: I want to appeal\n"
                + "\nWhy you disagree with it: Because I do\n"
                + "\nWhat you disagree with: I want to appeal again\n"
                + "\nWhy you disagree with it: I'm in the mood\n"
                + "\nAnything else you want to tell the tribunal: Some other reason",
            result.get(REASONS_FOR_APPEALING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithNoAppealReasons_setReasonForAppealingDetailsForTemplate() {

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().appealReasons(AppealReasons.builder().build())
                .build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("Anything else you want to tell the tribunal: Not provided",
            result.get(REASONS_FOR_APPEALING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithNoAppealReasons_setReasonForAppealingDetailsForTemplate_Welsh() {

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().appealReasons(AppealReasons.builder().build())
                .build())
            .languagePreferenceWelsh("yes")
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("Anything else you want to tell the tribunal: Not provided",
            result.get(REASONS_FOR_APPEALING_DETAILS_LITERAL));

        assertEquals("Unrhyw beth arall yr hoffech ddweud wrth y tribiwnlys: Nis ddarparwyd",
            result.get(REASONS_FOR_APPEALING_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithNoExcludedDates_setHearingDetailsForTemplate() {

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("yes")
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes",
            result.get(HEARING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithOneExcludedDate_setHearingDetailsForTemplate() {

        List<ExcludeDate> excludeDates = new ArrayList<>();

        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("2018-01-03").build()).build());

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("yes")
                .excludeDates(excludeDates)
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes\n"
                + "\nDates you can't attend: 3 January 2018",
            result.get(HEARING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithMultipleExcludedDates_setHearingDetailsForTemplateAndIgnoreEndDateRange() {

        List<ExcludeDate> excludeDates = new ArrayList<>();

        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("2018-01-03").build()).build());
        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("2018-01-05").end("2018-01-07").build()).build());

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("yes")
                .excludeDates(excludeDates)
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes\n"
                + "\nDates you can't attend: 3 January 2018, 5 January 2018",
            result.get(HEARING_DETAILS_LITERAL));
        assertNull("Welsh details not be set ", result.get(HEARING_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithMultipleExcludedDates_setHearingDetailsForTemplateAndIgnoreEndDateRange_Welsh() {

        List<ExcludeDate> excludeDates = new ArrayList<>();

        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("2018-01-03").build()).build());
        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("2018-01-05").end("2018-01-07").build()).build());

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .languagePreferenceWelsh("Yes")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("yes")
                .excludeDates(excludeDates)
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes\n"
                + "\nDates you can't attend: 3 January 2018, 5 January 2018",
            result.get(HEARING_DETAILS_LITERAL));

        assertEquals("Ydych chi'n bwriadu mynychu'r gwrandawiad: ydw\n"
                + "\nDyddiadau na allwch fynychu: 3 Ionawr 2018, 5 Ionawr 2018",
            result.get(HEARING_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenASyaAppealWithHearingArrangements_setHearingArrangementsForTemplate() {

        List<String> arrangementList = new ArrayList<>();

        arrangementList.add("signLanguageInterpreter");
        arrangementList.add("hearingLoop");
        arrangementList.add("disabledAccess");

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                .arrangements(arrangementList)
                .languageInterpreter("Yes")
                .other("Other")
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Required\n"
                + "\nSign interpreter: Required\n"
                + "\nHearing loop: Required\n"
                + "\nDisabled access: Required\n"
                + "\nAny other arrangements: Other",
            result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL));
        assertNull(result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL_WELSH));

    }

    @Test
    public void givenASyaAppealWithHearingArrangements_setHearingArrangementsForTemplate_Welsh() {

        List<String> arrangementList = new ArrayList<>();

        arrangementList.add("signLanguageInterpreter");
        arrangementList.add("hearingLoop");
        arrangementList.add("disabledAccess");

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .languagePreferenceWelsh("yes")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                .arrangements(arrangementList)
                .languageInterpreter("Yes")
                .other("Other")
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Required\n"
                + "\nSign interpreter: Required\n"
                + "\nHearing loop: Required\n"
                + "\nDisabled access: Required\n"
                + "\nAny other arrangements: Other",
            result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL));

        assertEquals("Dehonglydd iaith arwyddion: Gofynnol\n"
                + "\nDehonglydd iaith arwyddion: Gofynnol\n"
                + "\nDolen glyw: Gofynnol\n"
                + "\nMynediad i bobl anab: Gofynnol\n"
                + "\nUnrhyw drefniadau eraill: Other",
            result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL_WELSH));

    }

    @Test
    public void givenASyaAppealWithNoLanguageInterpreter_setHearingArrangementsForTemplate() {

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                .languageInterpreter("No")
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Not required\n"
                + "\nSign interpreter: Not required\n"
                + "\nHearing loop: Not required\n"
                + "\nDisabled access: Not required\n"
                + "\nAny other arrangements: Not required",
            result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL));
        assertNull(result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void givenASyaAppealWithNoLanguageInterpreter_setHearingArrangementsForTemplate_Welsh() {

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .languagePreferenceWelsh("yes")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                .languageInterpreter("No")
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Not required\n"
                + "\nSign interpreter: Not required\n"
                + "\nHearing loop: Not required\n"
                + "\nDisabled access: Not required\n"
                + "\nAny other arrangements: Not required",
            result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL));

        assertEquals("Dehonglydd iaith arwyddion: Dim yn ofynnol\n"
                + "\nDehonglydd iaith arwyddion: Dim yn ofynnol\n"
                + "\nDolen glyw: Dim yn ofynnol\n"
                + "\nMynediad i bobl anab: Dim yn ofynnol\n"
                + "\nUnrhyw drefniadau eraill: Dim yn ofynnol",
            result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL_WELSH));

    }

    @Test
    public void givenASyaAppealWithNoHearingArrangements_setHearingArrangementsForTemplate() {

        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                .build()).build())
            .build();

        Map<String, Object> result = syaAppealCreatedAndReceivedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Not required\n"
                + "\nSign interpreter: Not required\n"
                + "\nHearing loop: Not required\n"
                + "\nDisabled access: Not required\n"
                + "\nAny other arrangements: Not required",
            result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL));
    }

    @Test
    public void getOptionalFieldTest() {

        final Name emptyName = Name.builder().build();
        final Name firstName = Name.builder().firstName("FIRST").build();
        final Name lastName = Name.builder().lastName("LAST").build();
        final Name bothName = Name.builder().firstName("FIRST").lastName("LAST").build();
        final Name allName = Name.builder().title("MX").firstName("FIRST").lastName("LAST").build();

        assertEquals("expected", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(null, "expected"));
        assertEquals("expected", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(emptyName.getFullName(), "expected"));
        assertEquals("expected", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(emptyName.getFullNameNoTitle(), "expected"));
        assertEquals("null FIRST null", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(firstName.getFullName(), "expected"));
        assertEquals("FIRST null", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(firstName.getFullNameNoTitle(), "expected"));
        assertEquals("null null LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(lastName.getFullName(), "expected"));
        assertEquals("null LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(lastName.getFullNameNoTitle(), "expected"));
        assertEquals("null FIRST LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(bothName.getFullName(), "expected"));
        assertEquals("FIRST LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(bothName.getFullNameNoTitle(), "expected"));
        assertEquals("MX FIRST LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(allName.getFullName(), "expected"));
        assertEquals("FIRST LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(allName.getFullNameNoTitle(), "expected"));
    }

}
