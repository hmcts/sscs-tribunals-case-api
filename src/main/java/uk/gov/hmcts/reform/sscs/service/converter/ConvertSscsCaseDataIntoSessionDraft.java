package uk.gov.hmcts.reform.sscs.service.converter;

import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.model.draft.SessionAppellantContactDetails;
import uk.gov.hmcts.reform.sscs.model.draft.SessionAppellantDob;
import uk.gov.hmcts.reform.sscs.model.draft.SessionAppellantName;
import uk.gov.hmcts.reform.sscs.model.draft.SessionAppellantNino;
import uk.gov.hmcts.reform.sscs.model.draft.SessionAppointee;
import uk.gov.hmcts.reform.sscs.model.draft.SessionBenefitType;
import uk.gov.hmcts.reform.sscs.model.draft.SessionCreateAccount;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDate;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDwpIssuingOffice;
import uk.gov.hmcts.reform.sscs.model.draft.SessionEvidenceProvide;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHaveAMrn;
import uk.gov.hmcts.reform.sscs.model.draft.SessionMrnDate;
import uk.gov.hmcts.reform.sscs.model.draft.SessionNotAttendingHearing;
import uk.gov.hmcts.reform.sscs.model.draft.SessionOtherReasonForAppealing;
import uk.gov.hmcts.reform.sscs.model.draft.SessionPostcodeChecker;
import uk.gov.hmcts.reform.sscs.model.draft.SessionReasonForAppealing;
import uk.gov.hmcts.reform.sscs.model.draft.SessionReasonForAppealingItem;
import uk.gov.hmcts.reform.sscs.model.draft.SessionSendToNumber;
import uk.gov.hmcts.reform.sscs.model.draft.SessionSmsConfirmation;
import uk.gov.hmcts.reform.sscs.model.draft.SessionTextReminders;
import uk.gov.hmcts.reform.sscs.utility.PhoneNumbersUtil;

@Service
public class ConvertSscsCaseDataIntoSessionDraft implements ConvertAintoBService<SscsCaseData, SessionDraft> {

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
            .dwpIssuingOffice(buildDwpIssuingOffice(appeal))
            .appointee(buildAppointee(appeal))
            .appellantName(buildAppellantName(appeal))
            .appellantDob(buildAppellantDob(appeal))
            .appellantNino(buildAppellantNino(appeal))
            .appellantContactDetails(buildAppellantContactDetails(appeal))
            .textReminders(buildTextReminders(caseData.getSubscriptions()))
            .sendToNumber(buildSendToNumber(caseData))
            .smsConfirmation(buildSmsConfirmation(caseData))
            .reasonForAppealing(buildReasonForAppealing(appeal))
            .otherReasonForAppealing(buildOtherReasonForAppealing(appeal))
            .evidenceProvide(buildEvidenceProvide(caseData))
            .notAttendingHearing(buildNotAttendingHearing(appeal))
            .build();
    }

    private SessionMrnDate buildMrnDate(Appeal appeal) {
        MrnDetails mrnDetails = appeal.getMrnDetails();
        if (StringUtils.isNotBlank(mrnDetails.getMrnDate())) {
            LocalDate mrdDetailsDate = LocalDate.parse(mrnDetails.getMrnDate());
            String day = String.valueOf(mrdDetailsDate.getDayOfMonth());
            String month = String.valueOf(mrdDetailsDate.getMonthValue());
            String year = String.valueOf(mrdDetailsDate.getYear());
            SessionDate mrnDateDetails = new SessionDate(day, month, year);
            return new SessionMrnDate(mrnDateDetails);
        }
        return null;
    }

    private SessionHaveAMrn buildHaveAMrn(Appeal appeal) {
        if (appeal == null
            || appeal.getMrnDetails() == null
            || appeal.getMrnDetails().getMrnDate() == null) {
            return null;
        }

        String haveAMrn = StringUtils.isBlank(appeal.getMrnDetails().getMrnDate()) ? "no" : "yes";
        return new SessionHaveAMrn(haveAMrn);
    }

    private SessionCreateAccount buildSessionCreateAccount() {
        return new SessionCreateAccount("yes");
    }

    private SessionPostcodeChecker buildSessionPostcode() {
        return new SessionPostcodeChecker("n29ed");
    }

    private SessionBenefitType buildSessionBenefitType(BenefitType benefitType) {
        return new SessionBenefitType(benefitType.getDescription() + " (" + benefitType.getCode() + ")");
    }

    private SessionDwpIssuingOffice buildDwpIssuingOffice(Appeal appeal) {
        if (StringUtils.isNotBlank(appeal.getMrnDetails().getDwpIssuingOffice())
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
        if (subscriptionIsNull(subscriptions)) {
            return null;
        }

        return "no".equalsIgnoreCase(subscriptions.getAppellantSubscription().getSubscribeSms())
            ? new SessionTextReminders("no")
            : new SessionTextReminders("yes");
    }

    private SessionSendToNumber buildSendToNumber(SscsCaseData caseData) {
        if (!appellantContactIsNull(caseData) && !subscriptionIsNull(caseData.getSubscriptions())) {
            Contact contact = caseData.getAppeal().getAppellant().getContact();
            String cleanNumber = PhoneNumbersUtil.cleanPhoneNumber(contact.getMobile()).orElse(contact.getMobile());
            String result = contact.getMobile().equals(caseData.getSubscriptions().getAppellantSubscription().getMobile())
                || cleanNumber.equals(caseData.getSubscriptions().getAppellantSubscription().getMobile()) ? "yes" : "no";
            return new SessionSendToNumber(result);
        }
        return null;

    }

    private SessionSmsConfirmation buildSmsConfirmation(SscsCaseData caseData) {
        if (!appellantContactIsNull(caseData) && !subscriptionIsNull(caseData.getSubscriptions())) {
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

    private boolean subscriptionIsNull(Subscriptions subscriptions) {
        return subscriptions == null
            || subscriptions.getAppellantSubscription() == null
            || subscriptions.getAppellantSubscription().getSubscribeSms() == null;
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

    private SessionEvidenceProvide buildEvidenceProvide(SscsCaseData caseData) {
        if (caseData == null || caseData.getSscsDocument() == null) {
            return null;
        }

        return caseData.getSscsDocument().isEmpty() ? new SessionEvidenceProvide("no")
            : new SessionEvidenceProvide(caseData.getEvidencePresent());
    }

    private SessionNotAttendingHearing buildNotAttendingHearing(Appeal appeal) {
        if (appeal == null
            || appeal.getHearingOptions() == null
            || appeal.getHearingOptions().getWantsToAttend() == null
            || "yes".equalsIgnoreCase(appeal.getHearingOptions().getWantsToAttend())) {
            return null;
        }

        return new SessionNotAttendingHearing();
    }
}
