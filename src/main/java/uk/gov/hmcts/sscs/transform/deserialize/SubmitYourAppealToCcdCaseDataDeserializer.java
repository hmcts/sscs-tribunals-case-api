package uk.gov.hmcts.sscs.transform.deserialize;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.sscs.domain.wrapper.*;
import uk.gov.hmcts.sscs.model.ccd.*;

@Service
public class SubmitYourAppealToCcdCaseDataDeserializer {

    private static final String YES = "Yes";
    private static final String NO = "No";

    public CaseData convertSyaToCcdCaseData(SyaCaseWrapper syaCaseWrapper) {

        Appeal appeal = getAppeal(syaCaseWrapper);

        Subscriptions subscriptions = getAppellantSubscription(syaCaseWrapper);

        return CaseData.builder()
                .caseCreated(LocalDate.now().toString())
                .appeal(appeal)
                .subscriptions(subscriptions)
                .build();
    }

    private Appeal getAppeal(SyaCaseWrapper syaCaseWrapper) {

        MrnDetails mrnDetails = getMrnDetails(syaCaseWrapper);

        Appellant appellant = getAppellant(syaCaseWrapper);

        BenefitType benefitType = BenefitType.builder()
                .code(syaCaseWrapper.getBenefitType().getCode())
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
                .build();
    }

    private MrnDetails getMrnDetails(SyaCaseWrapper syaCaseWrapper) {
        return MrnDetails.builder()
                .dwpIssuingOffice(syaCaseWrapper.getMrn().getDwpIssuingOffice())
                .mrnDate(syaCaseWrapper.getMrn().getDate() != null ? syaCaseWrapper.getMrn().getDate().toString() :
                        null)
                .mrnLateReason(syaCaseWrapper.getMrn().getReasonForBeingLate())
                .mrnMissingReason(syaCaseWrapper.getMrn().getReasonForNoMrn())
                .build();

    }

    private Appellant getAppellant(SyaCaseWrapper syaCaseWrapper) {

        SyaAppellant syaAppellant = syaCaseWrapper.getAppellant();

        Name name = Name.builder()
                .title(syaAppellant.getTitle())
                .firstName(syaAppellant.getFirstName())
                .lastName(syaAppellant.getLastName())
                .build();

        Address address = Address.builder()
                .line1(syaAppellant.getContactDetails().getAddressLine1())
                .line2(syaAppellant.getContactDetails().getAddressLine2())
                .town(syaAppellant.getContactDetails().getTownCity())
                .county(syaAppellant.getContactDetails().getCounty())
                .postcode(syaAppellant.getContactDetails().getPostCode())
                .build();

        Contact contact = Contact.builder()
                .email(syaAppellant.getContactDetails().getEmailAddress())
                .mobile(syaAppellant.getContactDetails().getPhoneNumber())
                .build();

        Identity identity = Identity.builder()
                .dob(syaAppellant.getDob().toString())
                .nino(syaAppellant.getNino())
                .build();

        return Appellant.builder()
                .name(name)
                .address(address)
                .contact(contact)
                .identity(identity)
                .isAppointee(syaCaseWrapper.getIsAppointee() ? YES : NO)
                .build();
    }

    private AppealReasons getReasonsForAppealing(
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

    private HearingOptions getHearingOptions(SyaHearingOptions syaHearingOptions) {

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

    private List<ExcludeDate> getExcludedDates(String[] dates) {
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

    private List<String> getArrangements(SyaArrangements syaArrangements) {

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

    private Subscriptions getAppellantSubscription(SyaCaseWrapper syaCaseWrapper) {

        SyaSmsNotify smsNotify = syaCaseWrapper.getSmsNotify();

        String wantNotifications = smsNotify.isWantsSmsNotifications() ? YES : NO;
        String email = syaCaseWrapper.getAppellant().getContactDetails().getEmailAddress();
        String mobile = syaCaseWrapper.getAppellant().getContactDetails().getPhoneNumber();
        Subscription subscription = Subscription.builder()
                .wantSmsNotifications(wantNotifications)
                .subscribeSms(wantNotifications)
                .mobile(smsNotify.isWantsSmsNotifications() ? smsNotify.getSmsNumber() : mobile)
                .subscribeEmail(StringUtils.isNotBlank(email) ? YES : NO)
                .email(email)
                .build();

        return Subscriptions.builder()
                .appellantSubscription(subscription)
                .build();
    }

    private Representative getRepresentative(SyaCaseWrapper syaCaseWrapper) {

        Representative representative;

        if (syaCaseWrapper.hasRepresentative()) {

            SyaRepresentative syaRepresentative = syaCaseWrapper.getRepresentative();

            Name name = Name.builder()
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
                    .mobile(syaRepresentative.getContactDetails().getPhoneNumber())
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

    private String getLocalDate(String dateStr) {
        LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        return localDate.toString();
    }
}
