package uk.gov.hmcts.reform.sscs.bulkscan.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;

@RunWith(JUnitParamsRunner.class)
public class FuzzyMatcherServiceTest {

    private static final String CASE_ID = "123489";

    private FuzzyMatcherService fuzzyMatcherService;

    @Before
    public void setup() {
        fuzzyMatcherService = new FuzzyMatcherService();
    }

    @Test
    @Parameters({"personal", "personal test", "PERSONAL", "independence", "personal independence", "personal independence payment", "independence test", "PIP", "p.i.p."})
    public void givenAWord_thenFuzzyMatchToPip(String ocrValue) {
        String result = fuzzyMatcherService.matchBenefitType(CASE_ID, ocrValue);

        assertEquals("PIP", result);
    }

    @Test
    @Parameters({"employment", "employment test", "EMPLOYMENT", "E.S.A", "employmentsupportallowance", "Employment Support Allowance"})
    public void givenAWord_thenFuzzyMatchToEsa(String ocrValue) {
        String result = fuzzyMatcherService.matchBenefitType(CASE_ID, ocrValue);

        assertEquals("ESA", result);
    }

    @Test
    @Parameters({"universal", "universal test", "UNIVERSAL", "credit", "UC", "U.C", "UniversalCredit", "Universal Credit"})
    public void givenAWord_thenFuzzyMatchToUc(String ocrValue) {
        String result = fuzzyMatcherService.matchBenefitType(CASE_ID, ocrValue);

        assertEquals("UC", result);
    }

    @Test
    @Parameters({
        "unknown",
        "sdfhkjsdh",
        "fisls",
        "livewitre",
        "dlsb",
        "iida",
        "personel",
        "fish",
        "band",
        "esaz",
        "ipip",
        "AND",
        "of",
        "Allowance",
        "Benefit",
        "Pension",
        "pension;'",
        "",
        "*",
        "&",
        "...",
        "-$56w;'&4545[]()@£%^",
        "\\,\\,\\,",
        "support",
        "SupporT",
        "Sup.porT"
    })
    public void unknownCodeWillBeReturned(String unknownCode) {
        assertThat(fuzzyMatcherService.matchBenefitType(CASE_ID, unknownCode), is(unknownCode));
    }

    @Test
    @Parameters({
        "Job Disablement",
        "livi Attendance",
        "Maternity fund",
        "injuries Job",
        "disablement death income"
    })
    public void wordsContainsMultipleBenefitKeywordsWillNotMatch(String multiWordMatch) {
        assertThat(fuzzyMatcherService.matchBenefitType(CASE_ID, multiWordMatch), is(multiWordMatch));
    }

    @Test
    @Parameters({
        "personal independence something, PIP",
        "disability living something, DLA",
        "Baa carers care something, CARERS_ALLOWANCE",
    })
    public void multipleWordsContainsSameBenefitKeywordsWillMatch(String multiWordMatch, Benefit matchedBenefit) {
        assertThat(fuzzyMatcherService.matchBenefitType(CASE_ID, multiWordMatch), is(matchedBenefit.getShortName()));
    }

    @Test
    @Parameters({
        "pip, PIP",
        "pip test, PIP",
        "esa, ESA",
        "esa test, ESA",
        "uc, UC",
        "uc test, UC",
        "Attendance, ATTENDANCE_ALLOWANCE",
        "Attendance blah, ATTENDANCE_ALLOWANCE",
        "att, ATTENDANCE_ALLOWANCE",
        "A.A, ATTENDANCE_ALLOWANCE",
        "AA, ATTENDANCE_ALLOWANCE",
        "Disability, DLA",
        "Disability allowance, DLA",
        "Disability blah, DLA",
        "Living, DLA",
        "livi, DLA",
        "My livi benefit, DLA",
        "livin, DLA",
        "My livin benefit, DLA",
        "D.L.A, DLA",
        "D.L.A, DLA",
        "DLA, DLA",
        "DLA test, DLA",
        "Disability Living Allowance, DLA",
        "My living benefit, DLA",
        "Income, INCOME_SUPPORT",
        "inc, INCOME_SUPPORT",
        "inco, INCOME_SUPPORT",
        "La inco baah, INCOME_SUPPORT",
        "incom, INCOME_SUPPORT",
        "Ba incom cake, INCOME_SUPPORT",
        "Income Support, INCOME_SUPPORT",
        "incomesupport, INCOME_SUPPORT",
        "income support test, INCOME_SUPPORT",
        "My income test, INCOME_SUPPORT",
        "incomesupport test, INCOME_SUPPORT",
        "I.S, INCOME_SUPPORT",
        "Injuries, IIDB",
        "Disablement, IIDB",
        "IIDB, IIDB",
        "My IIDB benefit, IIDB",
        "I.I.D.B, IIDB",
        "Industrial Injuries Disablement Benefit, IIDB",
        "Job, JSA",
        "JSA, JSA",
        "J.S.A, JSA",
        "Seeker's, JSA",
        "Seekers, JSA",
        "Job Seekers Allowance, JSA",
        "Jab Seekers, JSA",
        "Job Seeker's Allowance, JSA",
        "Jobseeker’s Allowance (JSA), JSA",
        "jobseekersallowance, JSA",
        "jobseekersAllowance, JSA",
        "Jobseeker’s Allowance, JSA",
        "Social, SOCIAL_FUND",
        "Fund, SOCIAL_FUND",
        "SF, SOCIAL_FUND",
        "Social Fund, SOCIAL_FUND",
        "socialfund, SOCIAL_FUND",
        "PC, PENSION_CREDIT",
        "P.C, PENSION_CREDIT",
        "Pension Credits, PENSION_CREDIT",
        "Pension Credit, PENSION_CREDIT",
        "pensioncredits, PENSION_CREDIT",
        "pensioncredit, PENSION_CREDIT",
        "Retirement, RETIREMENT_PENSION",
        "Ret, RETIREMENT_PENSION",
        "Reti, RETIREMENT_PENSION",
        "Retir, RETIREMENT_PENSION",
        "Retire, RETIREMENT_PENSION",
        "Retirem, RETIREMENT_PENSION",
        "Retireme, RETIREMENT_PENSION",
        "Retiremen, RETIREMENT_PENSION",
        "RP, RETIREMENT_PENSION",
        "R.P, RETIREMENT_PENSION",
        "Retirement Pension, RETIREMENT_PENSION",
        "retirementpension, RETIREMENT_PENSION",
        "My Reti Pension, RETIREMENT_PENSION",
        "My Retir Pension, RETIREMENT_PENSION",
        "My Retire Pension, RETIREMENT_PENSION",
        "My Retirem Pension, RETIREMENT_PENSION",
        "My Retireme Pension, RETIREMENT_PENSION",
        "My Retiremen Pension, RETIREMENT_PENSION",
        "BB, BEREAVEMENT_BENEFIT",
        "B.B, BEREAVEMENT_BENEFIT",
        "Bereavement Benefit, BEREAVEMENT_BENEFIT",
        "bereavementbenefit, BEREAVEMENT_BENEFIT",
        "Carers, CARERS_ALLOWANCE",
        "Care, CARERS_ALLOWANCE",
        "My Care Benefit, CARERS_ALLOWANCE",
        "Car, CARERS_ALLOWANCE",
        "C.A, CARERS_ALLOWANCE",
        "Carer's Allowance, CARERS_ALLOWANCE",
        "Carers Allowance, CARERS_ALLOWANCE",
        "Carers something, CARERS_ALLOWANCE",
        "carersallowance, CARERS_ALLOWANCE",
        "Maternity, MATERNITY_ALLOWANCE",
        "Maternity something, MATERNITY_ALLOWANCE",
        "Mat, MATERNITY_ALLOWANCE",
        "Mate, MATERNITY_ALLOWANCE",
        "Mater, MATERNITY_ALLOWANCE",
        "Matern, MATERNITY_ALLOWANCE",
        "Materni, MATERNITY_ALLOWANCE",
        "Maternit, MATERNITY_ALLOWANCE",
        "M.A, MATERNITY_ALLOWANCE",
        "Maternity Allowance, MATERNITY_ALLOWANCE",
        "maternityallowance, MATERNITY_ALLOWANCE",
        "BSPS, BEREAVEMENT_SUPPORT_PAYMENT_SCHEME",
        "B.S.P.S, BEREAVEMENT_SUPPORT_PAYMENT_SCHEME",
        "Bereavement Support Payment Scheme, BEREAVEMENT_SUPPORT_PAYMENT_SCHEME",
        "bereavementsupportpaymentscheme, BEREAVEMENT_SUPPORT_PAYMENT_SCHEME",
        "Death, INDUSTRIAL_DEATH_BENEFIT",
        "Death blah, INDUSTRIAL_DEATH_BENEFIT",
        "Deat, INDUSTRIAL_DEATH_BENEFIT",
        "lo Deat cop, INDUSTRIAL_DEATH_BENEFIT",
        "I.D.B, INDUSTRIAL_DEATH_BENEFIT",
        "Industrial Death Benefit, INDUSTRIAL_DEATH_BENEFIT",
        "industrialdeathbenefit, INDUSTRIAL_DEATH_BENEFIT",
    })
    public void matchBenefitType(String code, Benefit expectedBenefit) {
        final String result = fuzzyMatcherService.matchBenefitType(CASE_ID, code);
        assertThat(result, is(expectedBenefit.getShortName()));
    }
}
