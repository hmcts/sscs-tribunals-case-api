package uk.gov.hmcts.sscs.transform.deserialize;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.corecase.*;
import uk.gov.hmcts.sscs.domain.wrapper.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubmitYourAppealToCcdCaseDeserializer {

    public CcdCase convertSyaToCcdCase(SyaCaseWrapper syaCaseWrapper) {

        CcdCase ccdCase = new CcdCase();

        ccdCase.setAppellant(convertAppellant(syaCaseWrapper.getAppellant()));
        ccdCase.setRepresentative(convertRepresentative(syaCaseWrapper.getRepresentative()));
        ccdCase.setReasonsForAppealing(convertReasonsForAppealing(
                syaCaseWrapper.getReasonsForAppealing()));

        List<Hearing> hearings = new ArrayList<>();
        hearings.add(convertHearing(syaCaseWrapper.getHearing()));
        ccdCase.setHearings(hearings);

        ccdCase.setSmsNotify(convertSmsNotify(syaCaseWrapper.getSmsNotify()));
        ccdCase.setAppeal(convertSyaDataToAppeal(syaCaseWrapper));
        ccdCase.setIsAppointee(syaCaseWrapper.getIsAppointee());
        ccdCase.setBenefitType(syaCaseWrapper.getBenefitType().getCode());
        ccdCase.getAppeal().setBenefit(Benefit.getBenefitByType(syaCaseWrapper.getBenefitType().getCode()));

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

        hearing.setExcludeDates(convertExcludedDates(syaHearing.getDatesCantAttend()));
        hearing.setScheduleHearing(syaHearing.getScheduleHearing());
        hearing.setWantsSupport(syaHearing.getWantsSupport());
        hearing.setWantsToAttend(syaHearing.getWantsToAttend());

        if (null != syaHearing.getArrangements()) {
            hearing.setHasDisabilityNeeds(syaHearing.getArrangements().getDisabledAccess());
            hearing.setHearingLoopRequired(syaHearing.getArrangements().getHearingLoop());
            hearing.setLanguageInterpreterRequired(syaHearing.getArrangements().getLanguageInterpreter());
            hearing.setSignLanguageRequired(syaHearing.getArrangements().getSignLanguageInterpreter());
        }


        return hearing;
    }

    private SmsNotify convertSmsNotify(SyaSmsNotify syaSmsNotify) {
        SmsNotify smsNotify = new SmsNotify();

        smsNotify.setSmsNumber(syaSmsNotify.getSmsNumber());
        smsNotify.setUseSameNumber(syaSmsNotify.isUseSameNumber());

        return smsNotify;
    }

    private Appeal convertSyaDataToAppeal(SyaCaseWrapper syaCaseWrapper) {
        Appeal appeal = new Appeal();
        SyaMrn syaMrn = syaCaseWrapper.getMrn();

        appeal.setDateOfDecision(syaMrn.getDate());

        // Is this the right field to map to?
        appeal.setOriginatingOffice(syaMrn.getDwpIssuingOffice());
        appeal.setReasonForBeingLate(syaMrn.getReasonForBeingLate());
        appeal.setReasonForNoMrn(syaMrn.getReasonForNoMrn());

        appeal.setBenefit(Benefit.getBenefitByFullDescription(syaCaseWrapper.getBenefitType().getCode()));

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


    public ExcludeDates[] convertExcludedDates(String[] dates) {
        List<ExcludeDates> excludedDates = new ArrayList();
        for (String date : dates) {
            //MVP only allows user to input a single date, not a range
            excludedDates.add(new ExcludeDates(date, null));
        }

        return excludedDates.toArray(new ExcludeDates[excludedDates.size()]);
    }

}
