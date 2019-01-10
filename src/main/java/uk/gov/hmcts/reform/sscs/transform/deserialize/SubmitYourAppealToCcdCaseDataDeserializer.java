package uk.gov.hmcts.reform.sscs.transform.deserialize;

import static uk.gov.hmcts.reform.sscs.utility.AppealNumberGenerator.generateAppealNumber;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.wrapper.*;
import uk.gov.hmcts.reform.sscs.util.Norm;

public class SubmitYourAppealToCcdCaseDataDeserializer {

    private static final String YES = "Yes";
    private static final String NO = "No";
    private static final String ORAL = "oral";
    private static final String PAPER = "paper";

    private SubmitYourAppealToCcdCaseDataDeserializer() {
        // Empty
    }

    public static SscsCaseData convertSyaToCcdCaseData(SyaCaseWrapper syaCaseWrapper) {
        Appeal appeal = getAppeal(syaCaseWrapper);

        Subscriptions subscriptions = populateSubscriptions(syaCaseWrapper);

        List<SscsDocument> sscsDocuments =  getEvidenceDocumentDetails(syaCaseWrapper);

        return SscsCaseData.builder()
                .caseCreated(LocalDate.now().toString())
                .generatedSurname(syaCaseWrapper.getAppellant().getLastName())
                .generatedEmail(syaCaseWrapper.getContactDetails().getEmailAddress())
                .generatedMobile(getPhoneNumberWithOutSpaces(syaCaseWrapper.getContactDetails().getPhoneNumber()))
                .generatedNino(syaCaseWrapper.getAppellant().getNino())
                .generatedDob(syaCaseWrapper.getAppellant().getDob().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .appeal(appeal)
                .subscriptions(subscriptions)
                .sscsDocument(sscsDocuments)
                .evidencePresent(hasEvidence(sscsDocuments))
                .build();
    }

    public static SscsCaseData convertSyaToCcdCaseData(SyaCaseWrapper syaCaseWrapper, String region, RegionalProcessingCenter rpc) {
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper);

        return caseData.toBuilder()
                .region(region)
                .regionalProcessingCenter(rpc).build();
    }


    private static Appeal getAppeal(SyaCaseWrapper syaCaseWrapper) {

        MrnDetails mrnDetails = getMrnDetails(syaCaseWrapper);

        Appellant appellant = getAppellant(syaCaseWrapper);

        BenefitType benefitType = BenefitType.builder()
                .code(syaCaseWrapper.getBenefitType().getCode())
                .description(syaCaseWrapper.getBenefitType().getDescription())
                .build();

        HearingOptions hearingOptions = getHearingOptions(syaCaseWrapper.getSyaHearingOptions());

        AppealReasons appealReasons = getReasonsForAppealing(syaCaseWrapper.getReasonsForAppealing());

        Representative representative = getRepresentative(syaCaseWrapper);

        return Appeal.builder()
                .mrnDetails(mrnDetails)
                .appellant(appellant)
                .benefitType(benefitType)
                .hearingOptions(hearingOptions)
                .appealReasons(appealReasons)
                .rep(representative)
                .signer(syaCaseWrapper.getSignAndSubmit().getSigner())
                .hearingType(hearingOptions.getWantsToAttend().equals(YES) ? ORAL : PAPER)
                .receivedVia("Online")
                .build();
    }

    private static MrnDetails getMrnDetails(SyaCaseWrapper syaCaseWrapper) {
        return MrnDetails.builder()
                .dwpIssuingOffice(Norm.dwpIssuingOffice(syaCaseWrapper.getMrn().getDwpIssuingOffice()))
                .mrnDate(syaCaseWrapper.getMrn().getDate() != null ? syaCaseWrapper.getMrn().getDate().toString() :
                        null)
                .mrnLateReason(syaCaseWrapper.getMrn().getReasonForBeingLate())
                .mrnMissingReason(syaCaseWrapper.getMrn().getReasonForNoMrn())
                .build();

    }

    private static Appellant getAppellant(SyaCaseWrapper syaCaseWrapper) {

        SyaAppellant syaAppellant = syaCaseWrapper.getAppellant();

        Name name = Name.builder()
                .title(syaAppellant.getTitle())
                .firstName(syaAppellant.getFirstName())
                .lastName(syaAppellant.getLastName())
                .build();

        SyaContactDetails contactDetails = syaCaseWrapper.getContactDetails();

        Address address = Address.builder()
                .line1(contactDetails.getAddressLine1())
                .line2(contactDetails.getAddressLine2())
                .town(contactDetails.getTownCity())
                .county(contactDetails.getCounty())
                .postcode(contactDetails.getPostCode())
                .build();

        Contact contact = Contact.builder()
                .email(contactDetails.getEmailAddress())
                .mobile(getPhoneNumberWithOutSpaces(contactDetails.getPhoneNumber()))
                .build();

        Identity identity = Identity.builder()
                .dob(syaAppellant.getDob().toString())
                .nino(syaAppellant.getNino())
                .build();

        Appointee appointee = getAppointee(syaCaseWrapper);

        String useSameAddress = (syaCaseWrapper.getAppellant().getIsAddressSameAsAppointee() == null || !syaCaseWrapper.getAppellant().getIsAddressSameAsAppointee())
            ? "No"
            : "Yes";

        return Appellant.builder()
                .name(name)
                .address(address)
                .contact(contact)
                .identity(identity)
                .appointee(appointee)
                .isAddressSameAsAppointee(useSameAddress)
                .build();
    }

    private static Appointee getAppointee(SyaCaseWrapper syaCaseWrapper) {

        SyaAppointee syaAppointee = syaCaseWrapper.getAppointee();

        if (null != syaAppointee) {
            Name name = Name.builder()
                .title(syaAppointee.getTitle())
                .firstName(syaAppointee.getFirstName())
                .lastName(syaAppointee.getLastName())
                .build();

            Address address = Address.builder()
                .line1(syaAppointee.getContactDetails().getAddressLine1())
                .line2(syaAppointee.getContactDetails().getAddressLine2())
                .town(syaAppointee.getContactDetails().getTownCity())
                .county(syaAppointee.getContactDetails().getCounty())
                .postcode(syaAppointee.getContactDetails().getPostCode())
                .build();

            Contact contact = Contact.builder()
                .email(syaAppointee.getContactDetails().getEmailAddress())
                .mobile(getPhoneNumberWithOutSpaces(syaAppointee.getContactDetails().getPhoneNumber()))
                .build();

            Identity identity = Identity.builder()
                .dob(syaAppointee.getDob().toString())
                .build();

            return Appointee.builder()
                .name(name)
                .address(address)
                .contact(contact)
                .identity(identity)
                .build();
        } else {
            return null;
        }
    }

    private static AppealReasons getReasonsForAppealing(
            SyaReasonsForAppealing syaReasonsForAppealing) {

        List<AppealReason> appealReasons = new ArrayList<>();

        for (Reason reason : syaReasonsForAppealing.getReasons()) {
            AppealReasonDetails appealReasonDetails = AppealReasonDetails.builder()
                    .reason(reason.getReasonForAppealing())
                    .description(reason.getWhatYouDisagreeWith())
                    .build();
            AppealReason appealReason = AppealReason.builder()
                    .value(appealReasonDetails)
                    .build();
            appealReasons.add(appealReason);
        }

        return AppealReasons.builder()
                .reasons(appealReasons)
                .otherReasons(syaReasonsForAppealing.getOtherReasons())
                .build();
    }

    private static HearingOptions getHearingOptions(SyaHearingOptions syaHearingOptions) {

        HearingOptions hearingOptions;

        if (syaHearingOptions.getWantsToAttend()) {

            String languageInterpreter = null;
            List<String> arrangements = null;
            String wantsSupport = syaHearingOptions.getWantsSupport() ? YES : NO;
            if (syaHearingOptions.getWantsSupport()) {
                languageInterpreter = syaHearingOptions.getArrangements().getLanguageInterpreter() ? YES : NO;
                arrangements = getArrangements(syaHearingOptions.getArrangements());
            }

            String scheduleHearing = syaHearingOptions.getScheduleHearing() ? YES : NO;
            List<ExcludeDate> excludedDates = null;
            if (syaHearingOptions.getScheduleHearing()) {
                excludedDates = getExcludedDates(syaHearingOptions.getDatesCantAttend());
            }

            hearingOptions = HearingOptions.builder()
                    .wantsToAttend(YES)
                    .wantsSupport(wantsSupport)
                    .languageInterpreter(languageInterpreter)
                    .languages(syaHearingOptions.getInterpreterLanguageType())
                    .signLanguageType(syaHearingOptions.getSignLanguageType())
                    .scheduleHearing(scheduleHearing)
                    .arrangements(arrangements)
                    .excludeDates(excludedDates)
                    .other(syaHearingOptions.getAnythingElse())
                    .build();
        } else {
            hearingOptions = HearingOptions.builder()
                    .wantsToAttend(NO)
                    .build();
        }
        return hearingOptions;
    }

    private static List<ExcludeDate> getExcludedDates(String[] dates) {
        List<ExcludeDate> excludeDates = new ArrayList<>();
        for (String date : dates) {
            DateRange dateRange = DateRange.builder()
                    .start(getLocalDate(date))
                    .end(getLocalDate(date))
                    .build();
            excludeDates.add(ExcludeDate.builder().value(dateRange).build());
        }
        return excludeDates;
    }

    private static List<String> getArrangements(SyaArrangements syaArrangements) {

        List<String> arrangements = new ArrayList<>();

        if (syaArrangements.getSignLanguageInterpreter()) {
            arrangements.add("signLanguageInterpreter");
        }

        if (syaArrangements.getHearingLoop()) {
            arrangements.add("hearingLoop");
        }

        if (syaArrangements.getAccessibleHearingRoom()) {
            arrangements.add("disabledAccess");
        }

        return arrangements;
    }

    private static Subscriptions populateSubscriptions(SyaCaseWrapper syaCaseWrapper) {

        return Subscriptions.builder()
                .appellantSubscription(!syaCaseWrapper.getIsAppointee() ? getAppellantSubscription(syaCaseWrapper) : null)
                .appointeeSubscription(syaCaseWrapper.getIsAppointee() ? getAppointeeSubscription(syaCaseWrapper) : null)
                .representativeSubscription(getRepresentativeSubscription(syaCaseWrapper))
                .build();
    }

    private static Subscription getAppellantSubscription(SyaCaseWrapper syaCaseWrapper) {

        SyaSmsNotify smsNotify = syaCaseWrapper.getSmsNotify();

        String subscribeSms = smsNotify.isWantsSmsNotifications() ? YES : NO;

        String email = syaCaseWrapper.getContactDetails().getEmailAddress();
        String wantEmailNotifications = StringUtils.isNotBlank(email) ? YES : NO;

        String mobile = syaCaseWrapper.getContactDetails().getPhoneNumber();
        return Subscription.builder()
                .wantSmsNotifications(smsNotify.isWantsSmsNotifications() ? YES : NO)
                .subscribeSms(subscribeSms)
                .tya(generateAppealNumber())
                .mobile(getPhoneNumberWithOutSpaces(smsNotify.isWantsSmsNotifications() ? smsNotify.getSmsNumber() : mobile))
                .subscribeEmail(wantEmailNotifications)
                .email(email)
                .build();
    }

    private static Subscription getRepresentativeSubscription(SyaCaseWrapper syaCaseWrapper) {

        if (syaCaseWrapper.hasRepresentative()) {

            boolean emailAddressExists = StringUtils
                    .isNotBlank(syaCaseWrapper.getRepresentative().getContactDetails().getEmailAddress());
            boolean phoneNumberExists =  StringUtils
                    .isNotBlank(syaCaseWrapper.getRepresentative().getContactDetails().getPhoneNumber());

            return Subscription.builder()
                    .wantSmsNotifications(phoneNumberExists ? YES : NO)
                    .subscribeSms(phoneNumberExists ? YES : NO)
                    .mobile(getPhoneNumberWithOutSpaces(syaCaseWrapper
                            .getRepresentative().getContactDetails().getPhoneNumber()))
                    .subscribeEmail(emailAddressExists ? YES : NO)
                    .email(syaCaseWrapper.getRepresentative().getContactDetails().getEmailAddress())
                    .tya(generateAppealNumber())
                    .build();
        }

        return null;

    }

    private static Subscription getAppointeeSubscription(SyaCaseWrapper syaCaseWrapper) {

        SyaSmsNotify smsNotify = syaCaseWrapper.getSmsNotify();

        String subscribeSms = smsNotify.isWantsSmsNotifications() ? YES : NO;

        String email = syaCaseWrapper.getAppointee().getContactDetails().getEmailAddress();
        String wantEmailNotifications = StringUtils.isNotBlank(email) ? YES : NO;

        String mobile = syaCaseWrapper.getAppointee().getContactDetails().getPhoneNumber();
        return Subscription.builder()
                .wantSmsNotifications(smsNotify.isWantsSmsNotifications() ? YES : NO)
                .subscribeSms(subscribeSms)
                .tya(generateAppealNumber())
                .mobile(getPhoneNumberWithOutSpaces(smsNotify.isWantsSmsNotifications() ? smsNotify.getSmsNumber() : mobile))
                .subscribeEmail(wantEmailNotifications)
                .email(email)
                .build();
    }

    private static Representative getRepresentative(SyaCaseWrapper syaCaseWrapper) {

        Representative representative;

        if (syaCaseWrapper.hasRepresentative()) {

            SyaRepresentative syaRepresentative = syaCaseWrapper.getRepresentative();

            Name name = Name.builder()
                    .title(syaRepresentative.getTitle())
                    .firstName(syaRepresentative.getFirstName())
                    .lastName(syaRepresentative.getLastName())
                    .build();

            Address address = Address.builder()
                    .line1(syaRepresentative.getContactDetails().getAddressLine1())
                    .line2(syaRepresentative.getContactDetails().getAddressLine2())
                    .town(syaRepresentative.getContactDetails().getTownCity())
                    .county(syaRepresentative.getContactDetails().getCounty())
                    .postcode(syaRepresentative.getContactDetails().getPostCode())
                    .build();

            Contact contact = Contact.builder()
                    .email(syaRepresentative.getContactDetails().getEmailAddress())
                    .mobile(getPhoneNumberWithOutSpaces(syaRepresentative.getContactDetails().getPhoneNumber()))
                    .build();

            representative = Representative.builder()
                    .hasRepresentative(YES)
                    .organisation(syaRepresentative.getOrganisation())
                    .name(name)
                    .address(address)
                    .contact(contact)
                    .build();
        } else {
            representative = Representative.builder()
                    .hasRepresentative(NO)
                    .build();
        }

        return representative;
    }

    private static String getLocalDate(String dateStr) {
        LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        return localDate.toString();
    }

    private static List<SscsDocument> getEvidenceDocumentDetails(SyaCaseWrapper syaCaseWrapper) {
        List<SyaEvidence> evidences = syaCaseWrapper.getReasonsForAppealing().getEvidences();

        if (null != evidences && !evidences.isEmpty()) {
            return evidences.stream()
                    .map(syaEvidence -> {
                        DocumentLink documentLink = DocumentLink.builder().documentUrl(syaEvidence.getUrl()).build();
                        SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder()
                                .documentFileName(syaEvidence.getFileName())
                                .documentDateAdded(syaEvidence.getUploadedDate().format(DateTimeFormatter.ISO_DATE))
                                .documentLink(documentLink)
                                .build();
                        return SscsDocument.builder().value(sscsDocumentDetails).build();
                    }).collect(Collectors.toList());
        }
        return null;
    }

    private static String getPhoneNumberWithOutSpaces(String phoneNumber) {
        if (StringUtils.isNotBlank(phoneNumber)) {
            return phoneNumber.replaceAll("\\s", "");
        }
        return phoneNumber;
    }

    private static String hasEvidence(List<SscsDocument> sscsDocuments) {
        return (null == sscsDocuments || sscsDocuments.isEmpty()) ? NO : YES;
    }
}
