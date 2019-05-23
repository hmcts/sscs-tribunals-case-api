package uk.gov.hmcts.reform.sscs.service.converter;

import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.model.draft.SessionAppellantNino;
import uk.gov.hmcts.reform.sscs.model.draft.SessionAppointee;
import uk.gov.hmcts.reform.sscs.model.draft.SessionBenefitType;
import uk.gov.hmcts.reform.sscs.model.draft.SessionCheckMrn;
import uk.gov.hmcts.reform.sscs.model.draft.SessionContactDetails;
import uk.gov.hmcts.reform.sscs.model.draft.SessionCreateAccount;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDate;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDatesCantAttend;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDob;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDwpIssuingOffice;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDwpIssuingOfficeEsa;
import uk.gov.hmcts.reform.sscs.model.draft.SessionEnterMobile;
import uk.gov.hmcts.reform.sscs.model.draft.SessionEvidenceProvide;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHaveAMrn;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHaveContactedDwp;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHearingArrangement;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHearingArrangements;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHearingArrangementsSelection;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHearingAvailability;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHearingSupport;
import uk.gov.hmcts.reform.sscs.model.draft.SessionMrnDate;
import uk.gov.hmcts.reform.sscs.model.draft.SessionMrnOverOneMonthLate;
import uk.gov.hmcts.reform.sscs.model.draft.SessionMrnOverThirteenMonthsLate;
import uk.gov.hmcts.reform.sscs.model.draft.SessionName;
import uk.gov.hmcts.reform.sscs.model.draft.SessionNoMrn;
import uk.gov.hmcts.reform.sscs.model.draft.SessionOtherReasonForAppealing;
import uk.gov.hmcts.reform.sscs.model.draft.SessionPostcodeChecker;
import uk.gov.hmcts.reform.sscs.model.draft.SessionReasonForAppealing;
import uk.gov.hmcts.reform.sscs.model.draft.SessionReasonForAppealingItem;
import uk.gov.hmcts.reform.sscs.model.draft.SessionRepName;
import uk.gov.hmcts.reform.sscs.model.draft.SessionRepresentative;
import uk.gov.hmcts.reform.sscs.model.draft.SessionRepresentativeDetails;
import uk.gov.hmcts.reform.sscs.model.draft.SessionSameAddress;
import uk.gov.hmcts.reform.sscs.model.draft.SessionSendToNumber;
import uk.gov.hmcts.reform.sscs.model.draft.SessionSmsConfirmation;
import uk.gov.hmcts.reform.sscs.model.draft.SessionTextReminders;
import uk.gov.hmcts.reform.sscs.model.draft.SessionTheHearing;
import uk.gov.hmcts.reform.sscs.utility.PhoneNumbersUtil;

@Service
public class ConvertSscsCaseDataIntoSessionDraft implements ConvertAintoBService<SscsCaseData, SessionDraft> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public SessionDraft convert(SscsCaseData caseData) {
        Preconditions.checkNotNull(caseData);
        Preconditions.checkNotNull(caseData.getAppeal());

        Appeal appeal = caseData.getAppeal();
        return SessionDraft.builder()
            .benefitType(buildSessionBenefitType(appeal.getBenefitType()))
            .postcode(buildSessionPostcode())
            .createAccount(buildSessionCreateAccount())
            .haveAMrn(buildHaveAMrn(appeal))
            .mrnDate(buildMrnDate(appeal))
            .checkMrn(buildCheckMrn(appeal))
            .mrnOverThirteenMonthsLate(buildMrnOverThirteenMonthsLate(appeal))
            .mrnOverOneMonthLate(buildMrnOverOneMonthLate(appeal))
            .haveContactedDwp(buildHaveContactedDwp(appeal))
            .noMrn(buildNoMrn(appeal))
            .dwpIssuingOffice(buildDwpIssuingOffice(appeal))
            .dwpIssuingOfficeEsa(buildDwpIssuingOfficeEsa(appeal))
            .appointeeName(buildName(getAppointeeName(appeal)))
            .appointeeDob(buildDob(getAppointeeIdentity(appeal)))
            .appointeeContactDetails(buildContactDetails(getAppointeeAddress(appeal), getAppointeeContact(appeal)))
            .appointee(buildAppointee(appeal))
            .appellantName(buildName(getAppellantName(appeal)))
            .appellantDob(buildDob(getAppellantIdentity(appeal)))
            .appellantNino(buildAppellantNino(appeal))
            .appellantContactDetails(buildContactDetails(getAppellantAddress(appeal), getAppellantContact(appeal)))
            .sameAddress(buildSameAddress(appeal))
            .textReminders(buildTextReminders(caseData.getSubscriptions()))
            .sendToNumber(buildSendToNumber(caseData))
            .enterMobile(buildEnterMobile(caseData))
            .smsConfirmation(buildSmsConfirmation(caseData))
            .representative(buildRepresentative(appeal))
            .representativeDetails(buildRepresentativeDetails(appeal))
            .reasonForAppealing(buildReasonForAppealing(appeal))
            .otherReasonForAppealing(buildOtherReasonForAppealing(appeal))
            .evidenceProvide(buildEvidenceProvide(caseData.getEvidencePresent()))
            .theHearing(buildTheHearing(appeal))
            .hearingSupport(buildHearingSupport(appeal))
            .hearingArrangements(buildHearingArrangements(appeal))
            .hearingAvailability(buildHearingAvailability(appeal))
            .datesCantAttend(buildDatesCantAttend(appeal))
            .build();
    }

    private SessionEnterMobile buildEnterMobile(SscsCaseData caseData) {
        SessionSendToNumber sendToNumber = buildSendToNumber(caseData);
        if (sendToNumber != null && StringUtils.isNotBlank(sendToNumber.getUseSameNumber())
            && "no".equals(sendToNumber.getUseSameNumber())) {
            Subscription subscription = caseData.getSubscriptions().getAppellantSubscription();
            if (subscriptionIsNull(subscription)) {
                subscription = caseData.getSubscriptions().getAppointeeSubscription();
            }
            return new SessionEnterMobile(subscription.getMobile());
        }
        return null;
    }

    private SessionMrnOverOneMonthLate buildMrnOverOneMonthLate(Appeal appeal) {
        if (mrnDatePresent(appeal) && isMrnOverPlusMonthsLate(appeal.getMrnDetails().getMrnDate(), 1L)
            && !isMrnOverPlusMonthsLate(appeal.getMrnDetails().getMrnDate(), 13L)
            && appeal.getMrnDetails().getMrnLateReason() != null) {
            return new SessionMrnOverOneMonthLate(appeal.getMrnDetails().getMrnLateReason());
        }
        return null;
    }

    private boolean isMrnOverPlusMonthsLate(String mrnDate, long plusMonths) {
        LocalDate mrnDateParsed = LocalDate.parse(mrnDate, DATE_FORMATTER);
        return mrnDateParsed.plusMonths(plusMonths).isBefore(LocalDate.now());
    }

    private boolean mrnDetailsPresent(Appeal appeal) {
        return appeal.getMrnDetails() != null;
    }

    private boolean mrnDatePresent(Appeal appeal) {
        return mrnDetailsPresent(appeal) && StringUtils.isNotBlank(appeal.getMrnDetails().getMrnDate());
    }

    private SessionMrnOverThirteenMonthsLate buildMrnOverThirteenMonthsLate(Appeal appeal) {
        if (mrnDatePresent(appeal) && isMrnOverPlusMonthsLate(appeal.getMrnDetails().getMrnDate(), 13L)
            && appeal.getMrnDetails().getMrnLateReason() != null) {
            return new SessionMrnOverThirteenMonthsLate(appeal.getMrnDetails().getMrnLateReason());
        }
        return null;
    }

    private Boolean hasHearingOptions(Appeal appeal) {
        return appeal.getHearingOptions() != null;
    }

    private SessionTheHearing buildTheHearing(Appeal appeal) {
        if (!hasHearingOptions(appeal) || appeal.getHearingOptions().getWantsToAttend() == null) {
            return null;
        } else {
            return new SessionTheHearing(appeal.getHearingOptions().getWantsToAttend().toLowerCase());
        }
    }

    private SessionHearingSupport buildHearingSupport(Appeal appeal) {
        if (!hasHearingOptions(appeal) || appeal.getHearingOptions().getWantsSupport() == null) {
            return null;
        }

        return new SessionHearingSupport(appeal.getHearingOptions().getWantsSupport().toLowerCase());
    }

    private SessionHearingArrangement getArrangement(String requested, String matchValue, String language) {
        SessionHearingArrangement arrangement = null;
        if (StringUtils.isNotBlank(requested)) {
            if (requested.equalsIgnoreCase(matchValue)) {
                arrangement = new SessionHearingArrangement(true, language);
            } else {
                arrangement = new SessionHearingArrangement(false);
            }
        }

        return arrangement;
    }

    private SessionHearingArrangement getArrangement(String requested, List<String> list, String matchValue, String language) {
        SessionHearingArrangement arrangement = null;
        if (StringUtils.isNotBlank(requested)) {
            if (list != null && !list.isEmpty() && list.contains(matchValue)) {
                arrangement = new SessionHearingArrangement(true, language);
            } else {
                arrangement = new SessionHearingArrangement(false);
            }
        }

        return arrangement;
    }

    private SessionHearingArrangement getArrangement(List<String> list, String matchValue) {
        return new SessionHearingArrangement(list != null && !list.isEmpty() && list.contains(matchValue));
    }

    private SessionHearingArrangements buildHearingArrangements(Appeal appeal) {
        if (!hasHearingOptions(appeal) || hasNoArrangements(appeal)) {
            return null;
        }

        SessionHearingArrangement languageInterpreter = getArrangement(
            appeal.getHearingOptions().getLanguageInterpreter(),
            "yes",
            appeal.getHearingOptions().getLanguages()
        );

        SessionHearingArrangement signLanguage = getArrangement(
            appeal.getHearingOptions().getSignLanguageType(),
            appeal.getHearingOptions().getArrangements(),
            "signLanguageInterpreter",
            appeal.getHearingOptions().getSignLanguageType()
        );

        SessionHearingArrangement hearingLoop = getArrangement(
            appeal.getHearingOptions().getArrangements(),
            ("hearingLoop")
        );

        SessionHearingArrangement disabledAccess = getArrangement(
            appeal.getHearingOptions().getArrangements(),
            ("disabledAccess")
        );

        SessionHearingArrangement anythingElse = getArrangement(
            appeal.getHearingOptions().getOther(),
            appeal.getHearingOptions().getOther(),
            appeal.getHearingOptions().getOther()
        );

        return new SessionHearingArrangements(
            new SessionHearingArrangementsSelection(
                languageInterpreter,
                signLanguage,
                hearingLoop,
                disabledAccess,
                anythingElse
            )
        );
    }

    private boolean hasNoArrangements(Appeal appeal) {
        return appeal.getHearingOptions().getLanguageInterpreter() == null
            && appeal.getHearingOptions().getSignLanguageType() == null
            && (appeal.getHearingOptions().getArrangements() == null
            || !appeal.getHearingOptions().getArrangements().contains("signLanguageInterpreter")
            || !appeal.getHearingOptions().getArrangements().contains("hearingLoop")
            || !appeal.getHearingOptions().getArrangements().contains("disabledAccess"));
    }

    private SessionHearingAvailability buildHearingAvailability(Appeal appeal) {
        if (!hasHearingOptions(appeal) || StringUtils.isBlank(appeal.getHearingOptions().getScheduleHearing())) {
            return null;
        }

        return new SessionHearingAvailability(appeal.getHearingOptions().getScheduleHearing().toLowerCase());
    }

    private SessionDatesCantAttend buildDatesCantAttend(Appeal appeal) {
        if (!hasHearingOptions(appeal)
            || appeal.getHearingOptions().getExcludeDates() == null
            || appeal.getHearingOptions().getExcludeDates().isEmpty()) {
            return null;
        }

        List<SessionDate> dates = appeal.getHearingOptions().getExcludeDates()
            .stream()
            .map(f -> new SessionDate(f.getValue()))
            .collect(Collectors.toList());
        return new SessionDatesCantAttend(dates);
    }

    private boolean hasRep(Appeal appeal) {
        return appeal.getRep() != null;
    }

    private SessionRepresentative buildRepresentative(Appeal appeal) {
        if (!hasRep(appeal) || StringUtils.isBlank(appeal.getRep().getHasRepresentative())) {
            return null;
        }
        return new SessionRepresentative(StringUtils.lowerCase(appeal.getRep().getHasRepresentative()));
    }

    private SessionRepresentativeDetails buildRepresentativeDetails(Appeal appeal) {
        SessionRepresentative sessionRepresentative = buildRepresentative(appeal);
        if (!hasRep(appeal)
            || sessionRepresentative == null
            || "no".equalsIgnoreCase(sessionRepresentative.getHasRepresentative())) {
            return null;
        }

        SessionRepName repName = null;
        if (appeal.getRep().getName() != null) {
            repName = new SessionRepName(
                appeal.getRep().getName().getTitle(),
                appeal.getRep().getName().getFirstName(),
                appeal.getRep().getName().getLastName()
            );
        }

        boolean hasAddress = appeal.getRep().getAddress() != null;
        boolean hasContact = appeal.getRep().getContact() != null;

        return new SessionRepresentativeDetails(
            repName,
            hasAddress ? appeal.getRep().getAddress().getLine1() : null,
            hasAddress ? appeal.getRep().getAddress().getLine2() : null,
            hasAddress ? appeal.getRep().getAddress().getTown() : null,
            hasAddress ? appeal.getRep().getAddress().getCounty() : null,
            hasAddress ? appeal.getRep().getAddress().getPostcode() : null,
            hasContact ? appeal.getRep().getContact().getMobile() : null,
            hasContact ? appeal.getRep().getContact().getEmail() : null
        );
    }

    private SessionMrnDate buildMrnDate(Appeal appeal) {
        if (!mrnDatePresent(appeal)) {
            return null;
        }

        LocalDate mrnDetailsDate = LocalDate.parse(appeal.getMrnDetails().getMrnDate());
        String day = String.valueOf(mrnDetailsDate.getDayOfMonth());
        String month = String.valueOf(mrnDetailsDate.getMonthValue());
        String year = String.valueOf(mrnDetailsDate.getYear());

        return new SessionMrnDate(new SessionDate(day, month, year));
    }

    private SessionCheckMrn buildCheckMrn(Appeal appeal) {
        if (appeal.getMrnDetails() == null || appeal.getMrnDetails().getMrnLateReason() == null) {
            return null;
        }
        return new SessionCheckMrn("yes");
    }

    private SessionHaveContactedDwp buildHaveContactedDwp(Appeal appeal) {
        if (!mrnDetailsPresent(appeal) || StringUtils.isBlank(appeal.getMrnDetails().getMrnMissingReason())) {
            return null;
        }

        return new SessionHaveContactedDwp("yes");
    }

    private SessionNoMrn buildNoMrn(Appeal appeal) {
        if (!mrnDetailsPresent(appeal) || StringUtils.isBlank(appeal.getMrnDetails().getMrnMissingReason())) {
            return null;
        }

        return new SessionNoMrn(appeal.getMrnDetails().getMrnMissingReason());
    }

    private SessionHaveAMrn buildHaveAMrn(Appeal appeal) {
        if (!mrnDetailsPresent(appeal)
            || (!mrnDatePresent(appeal) && StringUtils.isBlank(appeal.getMrnDetails().getMrnMissingReason()))) {
            return null;
        }

        return new SessionHaveAMrn(mrnDatePresent(appeal) ? "yes" : "no");
    }

    private SessionCreateAccount buildSessionCreateAccount() {
        return new SessionCreateAccount("yes");
    }

    private SessionPostcodeChecker buildSessionPostcode() {
        return new SessionPostcodeChecker("n29ed");
    }

    private SessionBenefitType buildSessionBenefitType(BenefitType benefitType) {
        if (benefitType == null) {
            return null;
        }
        return new SessionBenefitType(benefitType.getDescription() + " (" + benefitType.getCode() + ")");
    }

    private SessionDwpIssuingOffice buildDwpIssuingOffice(Appeal appeal) {
        if (mrnDatePresent(appeal)
            && StringUtils.isNotBlank(appeal.getMrnDetails().getDwpIssuingOffice())
            && "PIP".equalsIgnoreCase(appeal.getBenefitType().getCode())) {
            int firstBracket = appeal.getMrnDetails().getDwpIssuingOffice().indexOf('(') + 1;
            int secondBracket = appeal.getMrnDetails().getDwpIssuingOffice().lastIndexOf(')');
            return new SessionDwpIssuingOffice(
                appeal.getMrnDetails().getDwpIssuingOffice().substring(firstBracket, secondBracket));
        }
        return null;
    }

    private SessionDwpIssuingOfficeEsa buildDwpIssuingOfficeEsa(Appeal appeal) {
        if (mrnDatePresent(appeal)
            && StringUtils.isNotBlank(appeal.getMrnDetails().getDwpIssuingOffice())
            && "ESA".equalsIgnoreCase(appeal.getBenefitType().getCode())) {
            return new SessionDwpIssuingOfficeEsa(appeal.getMrnDetails().getDwpIssuingOffice());
        }
        return null;
    }

    private SessionAppointee buildAppointee(Appeal appeal) {
        if (appeal == null
            || appeal.getAppellant() == null
            || appeal.getAppellant().getIsAppointee() == null) {
            return null;
        }

        return new SessionAppointee(appeal.getAppellant().getIsAppointee().toLowerCase());
    }

    private Name getAppellantName(Appeal appeal) {
        if (appeal.getAppellant() == null) {
            return null;
        }

        return appeal.getAppellant().getName();
    }

    private Name getAppointeeName(Appeal appeal) {
        if (appeal.getAppellant() == null
            || appeal.getAppellant().getAppointee() == null) {
            return null;
        }

        return appeal.getAppellant().getAppointee().getName();
    }

    private SessionName buildName(Name name) {
        if (name == null || name.getTitle() == null) {
            return null;
        }

        return new SessionName(
            name.getTitle(),
            name.getFirstName(),
            name.getLastName()
        );
    }

    private Identity getAppellantIdentity(Appeal appeal) {
        if (appeal == null || appeal.getAppellant() == null) {
            return null;
        }

        return appeal.getAppellant().getIdentity();
    }

    private Identity getAppointeeIdentity(Appeal appeal) {
        if (appeal.getAppellant() == null || appeal.getAppellant().getAppointee() == null) {
            return null;
        }

        return appeal.getAppellant().getAppointee().getIdentity();
    }

    private SessionDob buildDob(Identity identity) {
        if (identity == null || identity.getDob() == null) {
            return null;
        }

        LocalDate mrdDetailsDate = LocalDate.parse(identity.getDob());
        String day = String.valueOf(mrdDetailsDate.getDayOfMonth());
        String month = String.valueOf(mrdDetailsDate.getMonthValue());
        String year = String.valueOf(mrdDetailsDate.getYear());
        SessionDate mrnDateDetails = new SessionDate(day, month, year);

        return new SessionDob(mrnDateDetails);
    }

    private SessionAppellantNino buildAppellantNino(Appeal appeal) {
        if (appeal.getAppellant() == null
            || appeal.getAppellant().getIdentity() == null
            || appeal.getAppellant().getIdentity().getNino() == null) {
            return null;
        }

        return new SessionAppellantNino(appeal.getAppellant().getIdentity().getNino());
    }

    private Address getAppellantAddress(Appeal appeal) {
        if (appeal.getAppellant() == null) {
            return null;
        }

        return appeal.getAppellant().getAddress();
    }

    private Address getAppointeeAddress(Appeal appeal) {
        if (appeal.getAppellant() == null || appeal.getAppellant().getAppointee() == null) {
            return null;
        }

        return appeal.getAppellant().getAppointee().getAddress();
    }

    private Contact getAppellantContact(Appeal appeal) {
        if (appeal.getAppellant() == null) {
            return null;
        }

        return appeal.getAppellant().getContact();
    }

    private Contact getAppointeeContact(Appeal appeal) {
        if (appeal.getAppellant() == null || appeal.getAppellant().getAppointee() == null) {
            return null;
        }

        return appeal.getAppellant().getAppointee().getContact();
    }

    private SessionContactDetails buildContactDetails(Address address, Contact contact) {
        if (address == null || address.getLine1() == null) {
            return null;
        }

        return new SessionContactDetails(
            address.getLine1(),
            address.getLine2(),
            address.getTown(),
            address.getCounty(),
            address.getPostcode(),
            contact.getMobile(),
            contact.getEmail()
        );
    }

    private SessionSameAddress buildSameAddress(Appeal appeal) {
        if (appeal == null
            || appeal.getAppellant() == null
            || appeal.getAppellant().getIsAddressSameAsAppointee() == null
            || (getAppellantAddress(appeal) == null && getAppointeeAddress(appeal) == null)
        ) {
            return null;
        }

        return new SessionSameAddress(appeal.getAppellant().getIsAddressSameAsAppointee().toLowerCase());
    }

    private SessionTextReminders buildTextReminders(Subscriptions subscriptions) {
        if (subscribeSmsInSubscriptionsIsNull(subscriptions)) {
            return null;
        }
        if (!subscribeSmsIsNull(subscriptions.getAppellantSubscription())) {
            return getSessionTextReminders(subscriptions.getAppellantSubscription());
        }
        return getSessionTextReminders(subscriptions.getAppointeeSubscription());
    }

    private SessionTextReminders getSessionTextReminders(Subscription subscription) {
        return "no".equalsIgnoreCase(subscription.getSubscribeSms())
            ? new SessionTextReminders("no")
            : new SessionTextReminders("yes");
    }

    private SessionSendToNumber buildSendToNumber(SscsCaseData caseData) {
        if (!contactIsNull(caseData) && !mobileInSubscriptionIsNull(caseData.getSubscriptions())) {
            Contact contact = caseData.getAppeal().getAppellant().getContact();
            if (contact.getMobile() == null) {
                contact = caseData.getAppeal().getAppellant().getAppointee().getContact();
            }
            String cleanNumber = PhoneNumbersUtil.cleanPhoneNumber(contact.getMobile()).orElse(contact.getMobile());
            Subscription subscription = caseData.getSubscriptions().getAppellantSubscription();
            if (subscriptionIsNull(subscription)) {
                subscription = caseData.getSubscriptions().getAppointeeSubscription();
            }
            String result = contact.getMobile().equals(subscription.getMobile())
                || cleanNumber.equals(subscription.getMobile()) ? "yes" : "no";
            return new SessionSendToNumber(result);
        }
        return null;

    }

    private SessionSmsConfirmation buildSmsConfirmation(SscsCaseData caseData) {
        SessionSendToNumber sessionSendToNumber = buildSendToNumber(caseData);
        if (sessionSendToNumber != null && StringUtils.isNotBlank(sessionSendToNumber.getUseSameNumber())) {
            return new SessionSmsConfirmation();
        }
        return null;
    }

    private boolean contactIsNull(SscsCaseData caseData) {
        if (caseData == null || caseData.getAppeal() == null || caseData.getAppeal().getAppellant() == null) {
            return true;
        }
        if (caseData.getAppeal().getAppellant().getContact() != null) {
            return false;
        }
        return caseData.getAppeal().getAppellant().getAppointee() == null
            || caseData.getAppeal().getAppellant().getAppointee().getContact() == null;
    }

    private boolean subscribeSmsInSubscriptionsIsNull(Subscriptions subscriptions) {
        if (subscriptions != null) {
            boolean appellantSubscribeSmsIsNull = subscribeSmsIsNull(subscriptions.getAppellantSubscription());
            boolean appointeeSubscribeSmsIsNull = subscribeSmsIsNull(subscriptions.getAppointeeSubscription());
            return appellantSubscribeSmsIsNull && appointeeSubscribeSmsIsNull;
        }
        return true;
    }

    private boolean subscribeSmsIsNull(Subscription subscription) {
        return subscription == null || subscription.getSubscribeSms() == null;
    }

    private boolean mobileInSubscriptionIsNull(Subscriptions subscriptions) {
        if (subscriptions == null) {
            return true;
        }
        return subscriptionIsNull(subscriptions.getAppellantSubscription())
            && subscriptionIsNull(subscriptions.getAppointeeSubscription());
    }

    private boolean subscriptionIsNull(Subscription subscription) {
        return subscription == null || subscription.getSubscribeSms() == null || subscription.getMobile() == null;
    }

    private SessionReasonForAppealing buildReasonForAppealing(Appeal appeal) {
        if (appeal == null
            || appeal.getAppealReasons() == null
            || appeal.getAppealReasons().getReasons() == null) {
            return null;
        }

        return new SessionReasonForAppealing(buildReasonForAppealingItems(appeal.getAppealReasons().getReasons()));
    }

    private List<SessionReasonForAppealingItem> buildReasonForAppealingItems(List<AppealReason> appealReasons) {
        List<SessionReasonForAppealingItem> items = new ArrayList<>();
        for (AppealReason appealReason : appealReasons) {
            items.add(
                new SessionReasonForAppealingItem(
                    appealReason.getValue().getReason(),
                    appealReason.getValue().getDescription()
                )
            );
        }

        return items;
    }

    private SessionOtherReasonForAppealing buildOtherReasonForAppealing(Appeal appeal) {
        if (appeal == null
            || appeal.getAppealReasons() == null
            || appeal.getAppealReasons().getOtherReasons() == null) {
            return null;
        }

        return new SessionOtherReasonForAppealing(appeal.getAppealReasons().getOtherReasons());
    }

    private SessionEvidenceProvide buildEvidenceProvide(String evidenceProvide) {
        if (StringUtils.isEmpty(evidenceProvide)) {
            return null;
        }
        return new SessionEvidenceProvide(StringUtils.lowerCase(evidenceProvide));
    }

}
