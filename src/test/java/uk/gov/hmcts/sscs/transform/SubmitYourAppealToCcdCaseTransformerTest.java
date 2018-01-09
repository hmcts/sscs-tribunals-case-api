package uk.gov.hmcts.sscs.transform;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.corecase.ExcludeDates;
import uk.gov.hmcts.sscs.domain.wrapper.*;

public class SubmitYourAppealToCcdCaseTransformerTest {

    private SubmitYourAppealToCcdCaseTransformer transformer;
    private SyaCaseWrapper testData;
    private SyaAppellant appellant;
    private SyaReasonsForAppealing reasonsForAppealing;
    private SyaHearing hearing;
    private SyaRepresentative representative;
    private SyaSmsNotify smsNotify;
    private SyaMrn mrn;

    @Before
    public void setup() {
        transformer = new SubmitYourAppealToCcdCaseTransformer();
        testData();
    }

    public SyaCaseWrapper testData() {
        testData = new SyaCaseWrapper();

        testData.setAppellant(setupAppellantData());
        testData.setReasonsForAppealing(setupReasonsForAppealingData());
        testData.setHearing(setupHearingData());
        testData.setRepresentative(setupRepresentativeData());
        testData.setSmsNotify(setupSmsNotifyData());
        testData.setMrn(setupMrnData());

        testData.setBenefitType("Personal Independence Payment (PIP)");
        testData.setIsAppointee(false);

        return testData;
    }

    public SyaAppellant setupAppellantData() {
        appellant = new SyaAppellant();

        appellant.setTitle("Mr");
        appellant.setFirstName("Harry");
        appellant.setLastName("Potter");
        appellant.setDob(LocalDate.of(1990, Month.MAY, 1));
        appellant.setNino("AB877533C");

        SyaContactDetails contactDetails = new SyaContactDetails();

        contactDetails.setAddressLine1("123 Hairy Lane");
        contactDetails.setAddressLine2("Off Hairy Park");
        contactDetails.setTownCity("Hairyfield");
        contactDetails.setCounty("Kent");
        contactDetails.setPostCode("TN32 6PL");
        contactDetails.setPhoneNumber("07411222222");
        contactDetails.setEmailAddress("harry.potter@wizards.com");
        appellant.setContactDetails(contactDetails);

        return appellant;
    }

    public SyaReasonsForAppealing setupReasonsForAppealingData() {
        reasonsForAppealing = new SyaReasonsForAppealing();

        reasonsForAppealing.setReasons("Here are my reasons for appealing...");
        reasonsForAppealing.setOtherReasons("Nope, not today anyway!");

        return reasonsForAppealing;
    }

    public SyaHearing setupHearingData() {
        hearing = new SyaHearing();

        hearing.setScheduleHearing(true);
        hearing.setAnythingElse("Nothing else today.");
        hearing.setWantsSupport(true);
        hearing.setWantsToAttend(true);
        hearing.setDatesCantAttend(new String[] {"25/01/1972"});
        return hearing;
    }

    public SyaRepresentative setupRepresentativeData() {
        representative = new SyaRepresentative();

        representative.setFirstName("Hermione");
        representative.setLastName("Granger");
        representative.setOrganisation("Harry Potter Entertainments Ltd");

        SyaContactDetails contactDetails = new SyaContactDetails();

        contactDetails.setAddressLine1("991 Harlow Road");
        contactDetails.setAddressLine2("Off Jam Park");
        contactDetails.setTownCity("Tunbridge Wells");
        contactDetails.setCounty("Kent");
        contactDetails.setPostCode("TN32 6PL");
        contactDetails.setPhoneNumber("07411666666");
        contactDetails.setEmailAddress("hermione.granger@wizards.com");
        representative.setContactDetails(contactDetails);

        return representative;
    }

    public SyaSmsNotify setupSmsNotifyData() {
        smsNotify = new SyaSmsNotify();

        smsNotify.setWantsSmsNotifications(true);
        smsNotify.setSmsNumber("07411222222");
        smsNotify.setUseSameNumber(true);

        return smsNotify;
    }

    public SyaMrn setupMrnData() {
        mrn = new SyaMrn();

        mrn.setDwpIssuingOffice("8");
        mrn.setDate(LocalDate.of(2017, Month.DECEMBER, 1));
        mrn.setReasonForBeingLate("I was ill");
        mrn.setReasonForNoMrn("Forgot");

        return mrn;
    }

    @Test
    public void convertWrapper() {
        CcdCase ccdCase = transformer.convertSyaToCcdCase(testData);

        assertEquals(testData.getBenefitType(), ccdCase.getAppeal().getBenefit()
                .getFullDesciption());
        assertEquals(testData.getIsAppointee(), ccdCase.getIsAppointee());

        //Appellant
        assertEquals(appellant.getContactDetails().getAddressLine1(),
                ccdCase.getAppellant().getAddress().getLine1());
        assertEquals(appellant.getContactDetails().getAddressLine2(),
                ccdCase.getAppellant().getAddress().getLine2());
        assertEquals(appellant.getContactDetails().getTownCity(),
                ccdCase.getAppellant().getAddress().getTown());
        assertEquals(appellant.getContactDetails().getCounty(),
                ccdCase.getAppellant().getAddress().getCounty());
        assertEquals(appellant.getContactDetails().getPostCode(),
                ccdCase.getAppellant().getAddress().getPostcode());
        assertEquals(appellant.getContactDetails().getEmailAddress(),
                ccdCase.getAppellant().getEmail());
        assertEquals(appellant.getContactDetails().getPhoneNumber(),
                ccdCase.getAppellant().getPhone());

        assertEquals(appellant.getDob(), ccdCase.getAppellant().getDateOfBirth());
        assertEquals(appellant.getFirstName(), ccdCase.getAppellant().getName().getFirst());
        assertEquals(appellant.getLastName(), ccdCase.getAppellant().getName().getSurname());
        assertEquals(appellant.getNino(), ccdCase.getAppellant().getNino());
        assertEquals(appellant.getTitle(), ccdCase.getAppellant().getName().getTitle());

        //ReasonsForAppealing
        assertEquals(reasonsForAppealing.getReasons(),
                ccdCase.getReasonsForAppealing().getReasons());
        assertEquals(reasonsForAppealing.getOtherReasons(),
                ccdCase.getReasonsForAppealing().getOtherReasons());

        //Hearing
        //TODO add assertions for arrangements
        assertEquals(hearing.getAnythingElse(),
                ccdCase.getHearings().get(0).getAdditionalInformation());
        assertEquals(hearing.getDatesCantAttend()[0],
                ccdCase.getHearings().get(0).getExcludeDates()[0].getStart());
        assertEquals(hearing.getScheduleHearing(),
                ccdCase.getHearings().get(0).getScheduleHearing());
        assertEquals(hearing.getWantsSupport(), ccdCase.getHearings().get(0).getWantsSupport());
        assertEquals(hearing.getWantsToAttend(), ccdCase.getHearings().get(0).getWantsToAttend());

        //Representative
        assertEquals(representative.getContactDetails().getAddressLine1(),
                ccdCase.getRepresentative().getAddress().getLine1());
        assertEquals(representative.getContactDetails().getAddressLine2(),
                ccdCase.getRepresentative().getAddress().getLine2());
        assertEquals(representative.getContactDetails().getTownCity(),
                ccdCase.getRepresentative().getAddress().getTown());
        assertEquals(representative.getContactDetails().getCounty(),
                ccdCase.getRepresentative().getAddress().getCounty());
        assertEquals(representative.getContactDetails().getPostCode(),
                ccdCase.getRepresentative().getAddress().getPostcode());
        assertEquals(representative.getContactDetails().getEmailAddress(),
                ccdCase.getRepresentative().getEmail());
        assertEquals(representative.getContactDetails().getPhoneNumber(),
                ccdCase.getRepresentative().getPhone());

        assertEquals(representative.getFirstName(),
                ccdCase.getRepresentative().getName().getFirst());
        assertEquals(representative.getLastName(),
                ccdCase.getRepresentative().getName().getSurname());
        assertEquals(representative.getOrganisation(),
                ccdCase.getRepresentative().getOrganisation());

        //SmsNotify
        assertEquals(smsNotify.getSmsNumber(), ccdCase.getSmsNotify().getSmsNumber());
        assertEquals(smsNotify.isUseSameNumber(), ccdCase.getSmsNotify().isUseSameNumber());
        assertEquals(smsNotify.isWantsSmsNotifications(),
                ccdCase.getSmsNotify().isWantsSmsNotifications());

        //Appeal
        assertEquals(mrn.getDate(), ccdCase.getAppeal().getDateOfDecision());
        assertEquals(mrn.getDwpIssuingOffice(), ccdCase.getAppeal().getOriginatingOffice());
        assertEquals(mrn.getReasonForBeingLate(), ccdCase.getAppeal().getReasonForBeingLate());
        assertEquals(mrn.getReasonForNoMrn(), ccdCase.getAppeal().getReasonForNoMrn());
    }


    @Test
    public void convertExcludedDates() {
        ExcludeDates[] excludeDates = transformer.convertExcludedDates(
            new String[] {"25/01/1972"});

        ExcludeDates[] expected = new ExcludeDates[]{new ExcludeDates("25/01/1972", null)};

        assertArrayEquals(expected, excludeDates);
    }
}