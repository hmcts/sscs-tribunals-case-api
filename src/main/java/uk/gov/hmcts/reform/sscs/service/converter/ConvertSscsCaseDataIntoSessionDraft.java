package uk.gov.hmcts.reform.sscs.service.converter;

import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.draft.*;
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
            .appointee(buildAppointee(appeal))
            .appellantName(buildAppellantName(appeal))
            .appellantDob(buildAppellantDob(appeal))
            .appellantNino(buildAppellantNino(appeal))
            .appellantContactDetails(buildAppellantContactDetails(appeal))
            .textReminders(buildTextReminders(caseData.getSubscriptions()))
            .sendToNumber(buildSendToNumber(caseData))
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
        return appeal != null && appeal.getMrnDetails() != null;
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
        return appeal != null && appeal.getHearingOptions() != null;
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
            if (matchValue.equalsIgnoreCase(matchValue)) {
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
        if (!hasHearingOptions(appeal)) {
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
        return appeal != null && appeal.getRep() != null;
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
                appeal.getMrnDetails().getDwpIssuingOffice().substring(firstBracket, secondBracket)
            );
        } else {
            return null;
        }
    }

    private SessionAppointee buildAppointee(Appeal appeal) {
        if (appeal == null
            || appeal.getAppellant() == null
            || appeal.getAppellant().getIsAppointee() == null) {
            return null;
        }

        return new SessionAppointee(appeal.getAppellant().getIsAppointee().toLowerCase());
    }

    private SessionAppellantName buildAppellantName(Appeal appeal) {
        if (appeal.getAppellant() == null
            || appeal.getAppellant().getName() == null
            || appeal.getAppellant().getName().getTitle() == null) {
            return null;
        }

        return new SessionAppellantName(
            appeal.getAppellant().getName().getTitle(),
            appeal.getAppellant().getName().getFirstName(),
            appeal.getAppellant().getName().getLastName()
        );
    }

    private SessionAppellantDob buildAppellantDob(Appeal appeal) {
        if (appeal.getAppellant() == null
            || appeal.getAppellant().getIdentity() == null
            || appeal.getAppellant().getIdentity().getDob() == null) {
            return null;
        }

        LocalDate mrdDetailsDate = LocalDate.parse(appeal.getAppellant().getIdentity().getDob());
        String day = String.valueOf(mrdDetailsDate.getDayOfMonth());
        String month = String.valueOf(mrdDetailsDate.getMonthValue());
        String year = String.valueOf(mrdDetailsDate.getYear());
        SessionDate mrnDateDetails = new SessionDate(day, month, year);

        return new SessionAppellantDob(mrnDateDetails);
    }

    private SessionAppellantNino buildAppellantNino(Appeal appeal) {
        if (appeal.getAppellant() == null
            || appeal.getAppellant().getIdentity() == null
            || appeal.getAppellant().getIdentity().getNino() == null) {
            return null;
        }

        return new SessionAppellantNino(appeal.getAppellant().getIdentity().getNino());
    }

    private SessionAppellantContactDetails buildAppellantContactDetails(Appeal appeal) {
        if (appeal.getAppellant() == null
            || appeal.getAppellant().getAddress() == null
            || appeal.getAppellant().getAddress().getLine1() == null) {
            return null;
        }

        Address address = appeal.getAppellant().getAddress();
        Contact contact = appeal.getAppellant().getContact();

        return new SessionAppellantContactDetails(
            address.getLine1(),
            address.getLine2(),
            address.getTown(),
            address.getCounty(),
            address.getPostcode(),
            contact.getMobile(),
            contact.getEmail()
        );
    }

    private SessionTextReminders buildTextReminders(Subscriptions subscriptions) {
        if (subscribeSmsInSubscriptionIsNull(subscriptions)) {
            return null;
        }

        return "no".equalsIgnoreCase(subscriptions.getAppellantSubscription().getSubscribeSms())
            ? new SessionTextReminders("no")
            : new SessionTextReminders("yes");
    }

    private SessionSendToNumber buildSendToNumber(SscsCaseData caseData) {
        if (!appellantContactIsNull(caseData) && !mobileInSubscriptionIsNull(caseData.getSubscriptions())) {
            Contact contact = caseData.getAppeal().getAppellant().getContact();
            String cleanNumber = PhoneNumbersUtil.cleanPhoneNumber(contact.getMobile()).orElse(contact.getMobile());
            String result = contact.getMobile().equals(caseData.getSubscriptions().getAppellantSubscription().getMobile())
                || cleanNumber.equals(caseData.getSubscriptions().getAppellantSubscription().getMobile()) ? "yes" : "no";
            return new SessionSendToNumber(result);
        }
        return null;

    }

    private SessionSmsConfirmation buildSmsConfirmation(SscsCaseData caseData) {
        if (!appellantContactIsNull(caseData) && !mobileInSubscriptionIsNull(caseData.getSubscriptions())) {
            Contact contact = caseData.getAppeal().getAppellant().getContact();
            String cleanNumber = PhoneNumbersUtil.cleanPhoneNumber(contact.getMobile()).orElse(contact.getMobile());
            boolean result = contact.getMobile().equals(caseData.getSubscriptions().getAppellantSubscription().getMobile())
                || cleanNumber.equals(caseData.getSubscriptions().getAppellantSubscription().getMobile());
            if (result) {
                return new SessionSmsConfirmation();
            }
        }
        return null;
    }

    private boolean appellantContactIsNull(SscsCaseData caseData) {
        return caseData == null
            || caseData.getAppeal() == null
            || caseData.getAppeal().getAppellant() == null
            || caseData.getAppeal().getAppellant().getContact() == null;
    }

    private boolean subscribeSmsInSubscriptionIsNull(Subscriptions subscriptions) {
        return subscriptions == null
            || subscriptions.getAppellantSubscription() == null
            || subscriptions.getAppellantSubscription().getSubscribeSms() == null;
    }

    private boolean mobileInSubscriptionIsNull(Subscriptions subscriptions) {
        return subscriptions == null
            || subscriptions.getAppellantSubscription() == null
            || subscriptions.getAppellantSubscription().getSubscribeSms() == null
            || subscriptions.getAppellantSubscription().getMobile() == null;
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
