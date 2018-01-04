package uk.gov.hmcts.sscs.transform;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.corecase.*;
import uk.gov.hmcts.sscs.domain.wrapper.*;

@Service
public class SubmitYourAppealToCcdCaseTransformer {

    public CcdCase convertSyaToCcdCase(SyaCaseWrapper syaCaseWrapper) {

        CcdCase ccdCase = new CcdCase();
        List<Hearing> hearings = new ArrayList<>();

        ccdCase.setAppellant(convertAppellant(syaCaseWrapper.getAppellant()));
        ccdCase.setRepresentative(convertRepresentative(syaCaseWrapper.getRepresentative()));
        ccdCase.setReasonsForAppealing(convertReasonsForAppealing(
                syaCaseWrapper.getReasonsForAppealing()));

        hearings.add(convertHearing(syaCaseWrapper.getHearing()));
        ccdCase.setHearings(hearings);
        ccdCase.setSmsNotify(convertSmsNotify(syaCaseWrapper.getSmsNotify()));
        ccdCase.setAppeal(convertSyaDataToAppeal(syaCaseWrapper));
        ccdCase.setIsAppointee(syaCaseWrapper.getIsAppointee());

        return ccdCase;
    }

    private Appellant convertAppellant(SyaAppellant syaAppellant) {
        Appellant appellant = new Appellant();

        appellant.setEmail(syaAppellant.getContactDetails().getEmailAddress());
        appellant.setPhone(syaAppellant.getContactDetails().getPhoneNumber());

        appellant.setAddress(convertAddress(syaAppellant.getContactDetails().getAddressLine1(),
                syaAppellant.getContactDetails().getAddressLine2(),
                syaAppellant.getContactDetails().getTownCity(),
                syaAppellant.getContactDetails().getCounty(),
                syaAppellant.getContactDetails().getPostCode()));

        appellant.setName(convertName(syaAppellant.getTitle(), syaAppellant.getFirstName(),
                syaAppellant.getLastName()));
        appellant.setDateOfBirth(syaAppellant.getDob());
        appellant.setNino(syaAppellant.getNino());

        return appellant;
    }

    private Representative convertRepresentative(SyaRepresentative syaRepresentative) {
        Representative representative = new Representative();

        representative.setEmail(syaRepresentative.getContactDetails().getEmailAddress());
        representative.setPhone(syaRepresentative.getContactDetails().getPhoneNumber());

        representative.setAddress(convertAddress(
                syaRepresentative.getContactDetails().getAddressLine1(),
                syaRepresentative.getContactDetails().getAddressLine2(),
                syaRepresentative.getContactDetails().getTownCity(),
                syaRepresentative.getContactDetails().getCounty(),
                syaRepresentative.getContactDetails().getPostCode()));

        representative.setName(convertName(null, syaRepresentative.getFirstName(),
                syaRepresentative.getLastName()));

        representative.setOrganisation(syaRepresentative.getOrganisation());

        return representative;
    }

    private ReasonsForAppealing convertReasonsForAppealing(
            SyaReasonsForAppealing syaReasonsForAppealing) {

        ReasonsForAppealing reasonsForAppealing = new ReasonsForAppealing();

        reasonsForAppealing.setReasons(syaReasonsForAppealing.getReasons());
        reasonsForAppealing.setOtherReasons(syaReasonsForAppealing.getOtherReasons());

        return reasonsForAppealing;
    }

    private Hearing convertHearing(SyaHearing syaHearing) {
        Hearing hearing = new Hearing();

        hearing.setAdditionalInformation(syaHearing.getAnythingElse());
        hearing.setLanguageInterpreterRequired(isArrangementInSya(syaHearing.getArrangements(),
                "Language interpreter"));
        hearing.setSignLanguageRequired(isArrangementInSya(syaHearing.getArrangements(),
                "Sign language interpreter"));
        hearing.setHearingLoopRequired(isArrangementInSya(syaHearing.getArrangements(),
                "Hearing loop"));
        hearing.setHasDisabilityNeeds(isArrangementInSya(syaHearing.getArrangements(),
                "Disabled access"));
        hearing.setExcludeDates(convertExcludedDates(syaHearing.getDatesCantAttend()));
        hearing.setScheduleHearing(syaHearing.getScheduleHearing());
        hearing.setWantsSupport(syaHearing.getWantsSupport());
        hearing.setWantsToAttend(syaHearing.getWantsToAttend());

        return hearing;
    }

    private SmsNotify convertSmsNotify(SyaSmsNotify syaSmsNotify) {
        SmsNotify smsNotify = new SmsNotify();

        smsNotify.setSmsNumber(syaSmsNotify.getSmsNumber());
        smsNotify.setUseSameNumber(syaSmsNotify.isUseSameNumber());
        smsNotify.setWantsSmsNotifications(syaSmsNotify.isWantsSmsNotifications());

        return smsNotify;
    }

    private Appeal convertSyaDataToAppeal(SyaCaseWrapper syaCaseWrapper) {
        Appeal appeal = new Appeal();
        SyaMrn syaMrn = syaCaseWrapper.getMrn();

        appeal.setDateOfDecision(syaMrn.getDate());
        // TODO: Is this the right field to map to?
        appeal.setOriginatingOffice(syaMrn.getDwpIssuingOffice());
        appeal.setReasonForBeingLate(syaMrn.getReasonForBeingLate());
        appeal.setReasonForNoMrn(syaMrn.getReasonForNoMrn());

        appeal.setBenefit(Benefit.getBenefitByFullDescription(syaCaseWrapper.getBenefitType()));

        return appeal;
    }

    private Name convertName(String title, String firstName, String lastName) {
        Name name = new Name();

        name.setTitle(title);
        name.setFirst(firstName);
        name.setSurname(lastName);

        return name;
    }

    private Address convertAddress(String line1, String line2, String town, String county,
                                   String postCode) {
        Address address = new Address();

        address.setLine1(line1);
        address.setLine2(line2);
        address.setTown(town);
        address.setCounty(county);
        address.setPostcode(postCode);

        return address;
    }

    public Boolean isArrangementInSya(String[] arrangements, String arrangementToFind) {
        for (String arrangement: arrangements) {
            if (arrangement.contains(arrangementToFind)) {
                return true;
            }
        }

        return false;
    }

    public ExcludeDates[] convertExcludedDates(String[] dates) {
        List<ExcludeDates> excludedDates = new ArrayList();
        for (String date : dates) {
            //MVP only allows user to input a single date, not a range
            excludedDates.add(new ExcludeDates(date, null));
        }

        return excludedDates.toArray(new ExcludeDates[excludedDates.size()]);
    }

}
