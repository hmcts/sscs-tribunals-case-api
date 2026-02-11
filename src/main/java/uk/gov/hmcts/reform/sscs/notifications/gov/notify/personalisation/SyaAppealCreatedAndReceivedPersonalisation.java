package uk.gov.hmcts.reform.sscs.notifications.gov.notify.personalisation;

import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.ANYTHING;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.DATE_OF_BIRTH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.DATE_OF_MRN;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.DATE_OF_RDN;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.HAVE_AN_APPOINTEE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.HAVE_A_REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.IBCA_REFERENCE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.NINO;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.ORGANISATION;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.REASON_FOR_LATE_APPEAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.REASON_FOR_NO_MRN;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.RECEIVE_TEXT_MESSAGE_REMINDER;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.WHAT_DISAGREE_WITH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.WHY_DISAGREE_WITH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey.getYesNoKey;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.APPOINTEE_DETAILS_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.APPOINTEE_DETAILS_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.APPOINTEE_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.HEARING_DETAILS_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.HEARING_DETAILS_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.MRN_DETAILS_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.MRN_DETAILS_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.OTHER_PARTY_DETAILS;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.OTHER_PARTY_DETAILS_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.REPRESENTATIVE_DETAILS_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.REPRESENTATIVE_DETAILS_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.SHOW_OTHER_PARTY_DETAILS;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.YOUR_DETAILS_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.YOUR_DETAILS_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.NotificationUtils.hasAppointee;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasons;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;

@Component
public class SyaAppealCreatedAndReceivedPersonalisation extends WithRepresentativePersonalisation {

    private static final String NOT_PROVIDED = "Not provided";
    private static final String YES = "yes";
    private static final String NO = "no";
    static final String TWO_NEW_LINES = "\n\n";
    static final String NOT_REQUIRED = "Not required";
    static final String REQUIRED = "Required";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter longFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");

    @Autowired
    private PersonalisationConfiguration syaAppealCreatedPersonalisationConfiguration;

    @Override
    protected Map<String, Object> create(NotificationSscsCaseDataWrapper responseWrapper, SubscriptionWithType subscriptionWithType) {
        Map<String, Object> personalisation = super.create(responseWrapper, subscriptionWithType);
        SscsCaseData ccdResponse = responseWrapper.getNewSscsCaseData();

        setMrnDetails(personalisation, ccdResponse);
        setYourDetails(personalisation, ccdResponse);
        setAppointeeName(personalisation, ccdResponse);
        setAppointeeDetails(personalisation, ccdResponse);
        setTextMessageReminderDetails(personalisation, subscriptionWithType.getSubscription());
        setRepresentativeDetails(personalisation, ccdResponse);
        setOtherPartyDetails(personalisation, ccdResponse);
        setReasonsForAppealingDetails(personalisation, ccdResponse);
        setHearingDetails(personalisation, ccdResponse);
        setHearingArrangementDetails(personalisation, ccdResponse);

        return personalisation;
    }

    public Map<String, Object> setMrnDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(MRN_DETAILS_LITERAL, buildMrnDetails(ccdResponse.getAppeal().getMrnDetails(), syaAppealCreatedPersonalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH), UnaryOperator.identity(), ccdResponse.isIbcCase()));

        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(MRN_DETAILS_LITERAL_WELSH,
                buildMrnDetails(ccdResponse.getAppeal().getMrnDetails(), syaAppealCreatedPersonalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH), this::convertLocalDateToWelshDateWithDefaultNotProvided, ccdResponse.isIbcCase()));
        }
        return personalisation;
    }

    private String buildMrnDetails(MrnDetails mrnDetails, Map<String, String> titleText, UnaryOperator<String> mrnDate, boolean isIbcCase) {

        List<String> details = new ArrayList<>();

        if (mrnDetails != null) {
            if (mrnDetails.getMrnDate() != null) {
                if (isIbcCase) {
                    details.add(titleText.get(DATE_OF_RDN.name()) + mrnDate.apply(mrnDetails.getMrnDate()));
                } else {
                    details.add(titleText.get(DATE_OF_MRN.name()) + mrnDate.apply(mrnDetails.getMrnDate()));
                }
            }

            if (mrnDetails.getMrnLateReason() != null) {
                details.add(titleText.get(REASON_FOR_LATE_APPEAL.name()) + mrnDetails.getMrnLateReason());
            }

            if (mrnDetails.getMrnMissingReason() != null) {
                details.add(titleText.get(REASON_FOR_NO_MRN.name()) + mrnDetails.getMrnMissingReason());
            }
        }

        return StringUtils.join(details.toArray(), TWO_NEW_LINES);
    }

    private String convertLocalDateToWelshDateWithDefaultNotProvided(String date) {
        if (NOT_PROVIDED.equals(date)) {
            return syaAppealCreatedPersonalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH).get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name());
        }
        LocalDate localDate = LocalDate.parse(date, formatter);
        return LocalDateToWelshStringConverter.convert(localDate);
    }

    public Map<String, Object> setYourDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(YOUR_DETAILS_LITERAL,
            buildYourDetails(ccdResponse, syaAppealCreatedPersonalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH), UnaryOperator.identity()));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(YOUR_DETAILS_LITERAL_WELSH,
                buildYourDetails(ccdResponse, syaAppealCreatedPersonalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH), this::convertLocalDateToWelshDateString));
        }
        return personalisation;
    }

    private String buildYourDetails(SscsCaseData ccdResponse, Map<String, String> titleText, UnaryOperator<String> convertDate) {
        Appeal appeal = ccdResponse.getAppeal();

        String yourDetails = titleText.get(PersonalisationConfiguration.PersonalisationKey.NAME.name()) + appeal.getAppellant().getName().getFullNameNoTitle() + TWO_NEW_LINES
            + titleText.get(DATE_OF_BIRTH.name()) + convertDate.apply(getOptionalField(appeal.getAppellant().getIdentity().getDob(), NOT_PROVIDED))
            + TWO_NEW_LINES + getNinoOrIbcaReferenceDetailsLine(ccdResponse, titleText)
            + TWO_NEW_LINES + titleText.get(PersonalisationConfiguration.PersonalisationKey.ADDRESS.name()) + appeal.getAppellant().getAddress().getFullAddress() + TWO_NEW_LINES
            + titleText.get(PersonalisationConfiguration.PersonalisationKey.EMAIL.name()) + getOptionalField(appeal.getAppellant().getContact().getEmail(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()))
            + TWO_NEW_LINES + titleText.get(PersonalisationConfiguration.PersonalisationKey.PHONE.name()) + getOptionalField(getPhoneOrMobile(appeal.getAppellant().getContact()), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()));

        if (ccdResponse.getChildMaintenanceNumber() != null) {
            yourDetails = yourDetails + TWO_NEW_LINES + titleText.get(PersonalisationConfiguration.PersonalisationKey.CHILD_MAINTENANCE_NUMBER.name()) + ccdResponse.getChildMaintenanceNumber();
        }
        return yourDetails;
    }

    public Map<String, Object> setTextMessageReminderDetails(Map<String, Object> personalisation, Subscription subscription) {
        personalisation.put(TEXT_MESSAGE_REMINDER_DETAILS_LITERAL, buildTextMessageDetails(subscription, syaAppealCreatedPersonalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH)));
        personalisation.put(TEXT_MESSAGE_REMINDER_DETAILS_LITERAL_WELSH, buildTextMessageDetails(subscription, syaAppealCreatedPersonalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH)));
        return personalisation;
    }

    private String buildTextMessageDetails(Subscription subscription, Map<String, String> titleText) {
        StringBuilder buildTextMessage = new StringBuilder()
            .append(titleText.get(RECEIVE_TEXT_MESSAGE_REMINDER.name()))
            .append(null != subscription && null != subscription.getSubscribeSms()
                ? titleText.get(getYesNoKey(subscription.getSubscribeSms().toLowerCase(Locale.ENGLISH))) : titleText.get(getYesNoKey(NO)));

        if (null != subscription && subscription.isSmsSubscribed()) {
            buildTextMessage
                .append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.MOBILE.name()))
                .append(subscription.getMobile());
        }

        return buildTextMessage.toString();
    }

    private Map<String, Object> setAppointeeName(Map<String, Object> personalisation, SscsCaseData sscsCaseData) {
        Appointee appointee = sscsCaseData.getAppeal().getAppellant().getAppointee();
        if (hasAppointee(appointee, sscsCaseData.getAppeal().getAppellant().getIsAppointee())) {
            personalisation.put(APPOINTEE_NAME, String.format("%s %s",
                appointee.getName().getFirstName(),
                appointee.getName().getLastName()));
        }
        return personalisation;
    }


    public Map<String, Object> setAppointeeDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        String isAppointee = ccdResponse.getAppeal().getAppellant().getIsAppointee();
        personalisation.put(APPOINTEE_DETAILS_LITERAL, buildAppointeeDetails(ccdResponse.getAppeal().getAppellant().getAppointee(), isAppointee, syaAppealCreatedPersonalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH), UnaryOperator.identity()));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(APPOINTEE_DETAILS_LITERAL_WELSH, buildAppointeeDetails(ccdResponse.getAppeal().getAppellant().getAppointee(), isAppointee, syaAppealCreatedPersonalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH), this::convertLocalDateToWelshDateString));
        }
        return personalisation;
    }

    private String buildAppointeeDetails(Appointee appointee, String isAppointee, Map<String, String> titleText, UnaryOperator<String> convertDate) {
        String hasAppointee = hasAppointee(appointee, isAppointee) ? YES : NO;

        StringBuilder appointeeBuilder = new StringBuilder()
            .append(titleText.get(HAVE_AN_APPOINTEE.name()))
            .append(titleText.get(getYesNoKey(hasAppointee)));

        if (StringUtils.equalsIgnoreCase(YES, hasAppointee)) {
            appointeeBuilder.append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.NAME.name())).append(appointee.getName().getFullNameNoTitle()).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.DATE_OF_BIRTH.name())).append(convertDate.apply(appointee.getIdentity().getDob())).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.ADDRESS.name())).append(appointee.getAddress().getFullAddress()).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.EMAIL.name())).append(getOptionalField(appointee.getContact().getEmail(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.PHONE.name()))
                .append(getOptionalField(getPhoneOrMobile(appointee.getContact()), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name())));
        }
        return appointeeBuilder.toString();
    }

    public Map<String, Object> setRepresentativeDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(REPRESENTATIVE_DETAILS_LITERAL, buildRepresentativeDetails(ccdResponse.getAppeal().getRep(), syaAppealCreatedPersonalisationConfiguration.personalisation.get(LanguagePreference.ENGLISH)));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(REPRESENTATIVE_DETAILS_LITERAL_WELSH, buildRepresentativeDetails(ccdResponse.getAppeal().getRep(), syaAppealCreatedPersonalisationConfiguration.personalisation.get(LanguagePreference.WELSH)));
        }
        return personalisation;
    }

    private String buildRepresentativeDetails(Representative representative, Map<String, String> titleText) {
        String hasRepresentative = (representative != null
            && StringUtils.equalsIgnoreCase(YES, representative.getHasRepresentative())) ? YES : NO;

        StringBuilder representativeBuilder = new StringBuilder()
            .append(titleText.get(HAVE_A_REPRESENTATIVE.name()))
            .append(titleText.get(getYesNoKey(hasRepresentative)));

        if (representative != null && representative.getName() != null && StringUtils.equalsIgnoreCase(YES, hasRepresentative)) {
            representativeBuilder.append(TWO_NEW_LINES + titleText.get(PersonalisationConfiguration.PersonalisationKey.NAME.name())).append(getOptionalField(representative.getName().getFullNameNoTitle(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES)
                .append(titleText.get(ORGANISATION.name())).append(getOptionalField(representative.getOrganisation(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.ADDRESS.name())).append(representative.getAddress().getFullAddress()).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.EMAIL.name())).append(getOptionalField(representative.getContact().getEmail(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES)
                .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.PHONE.name()))
                .append(getOptionalField(getPhoneOrMobile(representative.getContact()), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name())));
        }
        return representativeBuilder.toString();
    }

    public Map<String, Object> setOtherPartyDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        if (ccdResponse.getOtherParties() != null && !ccdResponse.getOtherParties().isEmpty()) {
            personalisation.put(SHOW_OTHER_PARTY_DETAILS, "Yes");
            personalisation.put(OTHER_PARTY_DETAILS,
                buildOtherPartyDetails(ccdResponse.getOtherParties(), syaAppealCreatedPersonalisationConfiguration.personalisation.get(LanguagePreference.ENGLISH)));
            if (ccdResponse.isLanguagePreferenceWelsh()) {
                personalisation.put(OTHER_PARTY_DETAILS_WELSH, buildOtherPartyDetails(ccdResponse.getOtherParties(), syaAppealCreatedPersonalisationConfiguration.personalisation.get(LanguagePreference.WELSH)));
            }
        } else {
            personalisation.put(SHOW_OTHER_PARTY_DETAILS, "No");
            personalisation.put(OTHER_PARTY_DETAILS, "");
            personalisation.put(OTHER_PARTY_DETAILS_WELSH, "");
        }
        return personalisation;
    }

    private String buildOtherPartyDetails(List<CcdValue<OtherParty>> otherParties, Map<String, String> titleText) {
        StringBuilder otherPartyBuilder = new StringBuilder();

        for (CcdValue<OtherParty> otherParty : otherParties) {
            if (otherParty != null) {
                String name = otherParty.getValue().getName() != null ? otherParty.getValue().getName().getFullNameNoTitle() : null;
                String address = otherParty.getValue().getAddress() != null ? otherParty.getValue().getAddress().getFullAddress() : null;
                otherPartyBuilder.append(titleText.get(PersonalisationConfiguration.PersonalisationKey.NAME.name())).append(getOptionalField(name, titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES)
                    .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.ADDRESS.name())).append(getOptionalField(address, titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name()))).append(TWO_NEW_LINES);
            }
        }

        return otherPartyBuilder.toString();
    }

    public Map<String, Object> setReasonsForAppealingDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        personalisation.put(REASONS_FOR_APPEALING_DETAILS_LITERAL, buildReasonsForAppealingDetails(ccdResponse.getAppeal().getAppealReasons(), syaAppealCreatedPersonalisationConfiguration.personalisation.get(LanguagePreference.ENGLISH)));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(REASONS_FOR_APPEALING_DETAILS_LITERAL_WELSH, buildReasonsForAppealingDetails(ccdResponse.getAppeal().getAppealReasons(), syaAppealCreatedPersonalisationConfiguration.personalisation.get(LanguagePreference.WELSH)));
        }
        return personalisation;
    }

    private String buildReasonsForAppealingDetails(AppealReasons appealReasons, Map<String, String> titleText) {
        StringBuilder appealReasonsBuilder = new StringBuilder();

        if (appealReasons != null && appealReasons.getReasons() != null && !appealReasons.getReasons().isEmpty()) {
            for (AppealReason reason : appealReasons.getReasons()) {
                appealReasonsBuilder.append(titleText.get(WHAT_DISAGREE_WITH.name())).append(reason.getValue().getReason()).append(TWO_NEW_LINES)
                    .append(titleText.get(WHY_DISAGREE_WITH.name())).append(reason.getValue().getDescription()).append(TWO_NEW_LINES);
            }
        }

        if (appealReasons != null) {
            appealReasonsBuilder.append(titleText.get(ANYTHING.name()))
                .append(getOptionalField(appealReasons.getOtherReasons(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_PROVIDED.name())));
        }
        return appealReasonsBuilder.toString();
    }

    public Map<String, Object> setHearingDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        HearingOptions hearingOptions = ccdResponse.getAppeal().getHearingOptions();
        personalisation.put(HEARING_DETAILS_LITERAL,
            buildHearingDetails(hearingOptions, syaAppealCreatedPersonalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH), this::convertLocalDateToLongDateString));
        if (ccdResponse.isLanguagePreferenceWelsh()) {
            personalisation.put(HEARING_DETAILS_LITERAL_WELSH,
                buildHearingDetails(hearingOptions, syaAppealCreatedPersonalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH), this::convertLocalDateToWelshDateString));
        }
        return personalisation;
    }

    private String buildHearingDetails(HearingOptions hearingOptions, Map<String, String> titleText, UnaryOperator<String> convertor) {
        String wantsToAttend = hearingOptions.getWantsToAttend() != null ? hearingOptions.getWantsToAttend() : "No";
        String decisionKey = getYesNoKey(wantsToAttend.toLowerCase(Locale.ENGLISH));
        StringBuilder hearingOptionsBuilder = new StringBuilder()
            .append(titleText.get(PersonalisationConfiguration.PersonalisationKey.ATTENDING_HEARING.name()))
            .append(titleText.get(decisionKey));

        if (StringUtils.equalsIgnoreCase(hearingOptions.getWantsToAttend(), YES) && hearingOptions.getExcludeDates() != null && !hearingOptions.getExcludeDates().isEmpty()) {
            hearingOptionsBuilder.append(TWO_NEW_LINES + titleText.get(PersonalisationConfiguration.PersonalisationKey.DATES_NOT_ATTENDING.name()));

            StringJoiner joiner = new StringJoiner(", ");

            for (ExcludeDate excludeDate : hearingOptions.getExcludeDates()) {
                joiner.add(buildDateRangeString(excludeDate.getValue(), convertor));
            }
            hearingOptionsBuilder.append(joiner.toString());
        }
        return hearingOptionsBuilder.toString();
    }

    private String buildDateRangeString(DateRange range, UnaryOperator<String> convertor) {

        if (range.getStart() != null) {
            return convertor.apply(range.getStart());
        }
        return StringUtils.EMPTY;
    }

    private String convertLocalDateToLongDateString(String localDateString) {
        LocalDate localDate = LocalDate.parse(localDateString, formatter);
        return localDate.format(longFormatter);
    }

    private String convertLocalDateToWelshDateString(String localDateString) {
        LocalDate localDate = LocalDate.parse(localDateString, formatter);
        return LocalDateToWelshStringConverter.convert(localDate);
    }

    public static String getOptionalField(String field, String text) {
        return field == null || StringUtils.equalsIgnoreCase("null", field)
            || StringUtils.equalsIgnoreCase("null null", field)
            || StringUtils.equalsIgnoreCase("null null null", field)
            || StringUtils.isBlank(field) ? text : field;
    }

    private String getPhoneOrMobile(Contact contact) {
        if (null != contact) {
            return null != contact.getPhone() ? contact.getPhone() : contact.getMobile();
        } else {
            return null;
        }
    }

    private String getNinoOrIbcaReferenceDetailsLine(SscsCaseData ccdResponse, Map<String, String> titleText) {
        if (ccdResponse.isIbcCase()) {
            return titleText.get(IBCA_REFERENCE.name()) + ccdResponse.getAppeal().getAppellant().getIdentity().getIbcaReference();
        } else {
            return titleText.get(NINO.name()) + ccdResponse.getAppeal().getAppellant().getIdentity().getNino();
        }
    }
}
