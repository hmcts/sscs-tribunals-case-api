package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.*;

import java.util.Arrays;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

@RunWith(JUnitParamsRunner.class)
public class FurtherEvidencePlaceholderServiceTest {

    private static final Address REP_ADDRESS = Address.builder()
        .line1("HM Courts & Tribunals Service Reps")
        .town("Social Security & Child Support Appeals Reps")
        .county("Prudential Buildings Reps")
        .postcode("L2 5UZ")
        .build();

    private static final Address APPOINTEE_ADDRESS = Address.builder()
        .line1("HM Courts & Tribunals Service Appointee")
        .town("Social Security & Child Support Appeals Appointee")
        .county("Prudential Buildings Appointee")
        .postcode("L2 5UZ")
        .build();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private PlaceholderService placeholderService;
    @Mock
    private DwpAddressLookupService dwpAddressLookup;

    @InjectMocks
    private FurtherEvidencePlaceholderService furtherEvidencePlaceholderService;

    SscsCaseData sscsCaseDataWithAppointee;

    SscsCaseData sscsCaseDataWithRep;

    SscsCaseData sscsCaseDataWithRepNoName;

    SscsCaseData sscsCaseDataWithRepNullName;

    SscsCaseData sscsCaseDataWithRepEmptyName;

    SscsCaseData sscsCaseDataWithRepNoNameNoOrg;

    SscsCaseData sscsCaseDataWithRepNoNameEmptyOrg;

    SscsCaseData sscsCaseDataWithRepNoNameNullOrg;

    @Captor
    ArgumentCaptor<Address> captor;

    @Before
    public void setup() {
        sscsCaseDataWithAppointee = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .appellant(Appellant.builder()
                    .appointee(Appointee.builder()
                        .name(Name.builder().title("Mr").firstName("Terry").lastName("Appointee").build())
                        .identity(Identity.builder().nino("JT0123456B").build())
                        .address(APPOINTEE_ADDRESS)
                        .build())
                    .isAppointee("Yes")
                    .build())
                .build())
            .build();

        sscsCaseDataWithRep = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .rep(Representative.builder()
                    .name(Name.builder().title("Mr").firstName("Terry").lastName("Rep").build())
                    .address(REP_ADDRESS)
                    .build())
                .build())
            .build();

        sscsCaseDataWithRepNoName = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .rep(Representative.builder()
                    .organisation("Nandos")
                    .address(REP_ADDRESS)
                    .build())
                .build())
            .build();

        sscsCaseDataWithRepNullName = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .rep(Representative.builder()
                    .name(Name.builder().title(null).firstName(null).lastName(null).build())
                    .organisation("Nandos")
                    .address(REP_ADDRESS)
                    .build())
                .build())
            .build();

        sscsCaseDataWithRepEmptyName = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .rep(Representative.builder()
                    .name(Name.builder().title("").firstName("").lastName("").build())
                    .organisation("Nandos")
                    .address(REP_ADDRESS)
                    .build())
                .build())
            .build();

        sscsCaseDataWithRepNoNameNoOrg = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .rep(Representative.builder()
                    .address(REP_ADDRESS)
                    .build())
                .build())
            .build();

        sscsCaseDataWithRepNoNameEmptyOrg = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .rep(Representative.builder()
                    .organisation("")
                    .address(REP_ADDRESS)
                    .build())
                .build())
            .build();

        sscsCaseDataWithRepNoNameNullOrg = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .rep(Representative.builder()
                    .organisation(null)
                    .address(REP_ADDRESS)
                    .build())
                .build())
            .build();

    }

    @Test
    public void givenAnAppellant_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(PlaceholderHelper.buildCaseData(), APPELLANT_LETTER, null);
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertEquals("Terry Tibbs", actual.get("name"));
        assertEquals("HM Courts & Tribunals Service", captor.getValue().getLine1());
        assertEquals("Down the road", captor.getValue().getLine2());
        assertEquals("Social Security & Child Support Appeals", captor.getValue().getTown());
        assertEquals("Prudential Buildings", captor.getValue().getCounty());
        assertEquals("L2 5UZ", captor.getValue().getPostcode());
    }

    @Test
    public void givenAnAppellantWithALongNameExceeding45Characters_thenGenerateThePlaceholdersWithTruncatedName() {
        SscsCaseData caseData = PlaceholderHelper.buildCaseData();
        caseData.getAppeal().getAppellant().setName(Name.builder().firstName("Jimmy").lastName("AVeryLongNameWithLotsaAdLotsAndLotsOfCharacters").build());
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(caseData, APPELLANT_LETTER, null);
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertEquals("Jimmy AVeryLongNameWithLotsaAdLotsAndLotsOfCh", actual.get("name"));
    }

    @Test
    public void givenAnAppointee_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(sscsCaseDataWithAppointee, APPELLANT_LETTER, null);
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertEquals("Terry Appointee", actual.get("name"));
        assertEquals("HM Courts & Tribunals Service Appointee", captor.getValue().getLine1());
        assertEquals("Social Security & Child Support Appeals Appointee", captor.getValue().getTown());
        assertEquals("Prudential Buildings Appointee", captor.getValue().getCounty());
        assertEquals("L2 5UZ", captor.getValue().getPostcode());
    }

    @Test
    public void givenARep_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(sscsCaseDataWithRep, REPRESENTATIVE_LETTER, null);
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertEquals("Terry Rep", actual.get("name"));
        assertEquals("HM Courts & Tribunals Service Reps", captor.getValue().getLine1());
        assertEquals("Social Security & Child Support Appeals Reps", captor.getValue().getTown());
        assertEquals("Prudential Buildings Reps", captor.getValue().getCounty());
        assertEquals("L2 5UZ", captor.getValue().getPostcode());
    }

    @Test
    public void givenARepWithNoNameButOrg_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(sscsCaseDataWithRepNoName, REPRESENTATIVE_LETTER, null);
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertEquals("Nandos", actual.get("name"));
    }

    @Test
    public void givenARepWithEmptyNameButOrg_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(sscsCaseDataWithRepEmptyName, REPRESENTATIVE_LETTER, null);
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertEquals("Nandos", actual.get("name"));
    }

    @Test
    public void givenARepWithNullNameButOrg_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(sscsCaseDataWithRepNullName, REPRESENTATIVE_LETTER, null);
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertEquals("Nandos", actual.get("name"));
    }

    @Test
    public void givenARepWithNoNameNoOrg_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(sscsCaseDataWithRepNoNameNoOrg, REPRESENTATIVE_LETTER, null);
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertEquals("Sir/Madam", actual.get("name"));
    }

    @Test
    public void givenARepWithNoNameEmptyOrg_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(sscsCaseDataWithRepNoNameEmptyOrg, REPRESENTATIVE_LETTER, null);
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertEquals("Sir/Madam", actual.get("name"));
    }

    @Test
    public void givenARepWithNoNameNullOrg_thenGenerateThePlaceholders() {
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(sscsCaseDataWithRepNoNameNullOrg, REPRESENTATIVE_LETTER, null);
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertEquals("Sir/Madam", actual.get("name"));
    }

    @Test
    @Parameters({
        ",, Sir/Madam, NO",
        "  ,  , Sir/Madam, NO",
        "John, Smith, John Smith, NO",
        "John, Smith, John Smith, YES"
    })
    public void givenAJointPartyWithNameAndAddress_thenGeneratePlaceholders(@Nullable String firstName,
                                                                            @Nullable String lastName,
                                                                            String expectedName,
                                                                            YesNo sameAddressAsAppellant) {

        final Address appellantAddress = Address.builder().postcode("W1 1AA").build();
        SscsCaseData caseData = SscsCaseData.builder()
            .jointParty(JointParty.builder()
                .hasJointParty(YES)
                .address(REP_ADDRESS)
                .name(Name.builder().title("Mr").firstName(firstName).lastName(lastName).build())
                .jointPartyAddressSameAsAppellant(sameAddressAsAppellant)
                .build())
            .appeal(Appeal.builder()
                .appellant(Appellant.builder().address(appellantAddress).build())
                .benefitType(BenefitType.builder().code("PIP").build())
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .build())
            .build();
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(caseData, JOINT_PARTY_LETTER, null);
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertThat(actual.get("name"), is(expectedName));
        if (sameAddressAsAppellant.toBoolean()) {
            assertThat(appellantAddress, is(captor.getValue()));
        } else {
            assertThat(REP_ADDRESS, is(captor.getValue()));
        }
    }

    @Test
    @Parameters({
        ",, Sir/Madam, OTHER_PARTY_LETTER",
        "  ,  , Sir/Madam, OTHER_PARTY_LETTER",
        "John, Smith, John Smith, OTHER_PARTY_LETTER",
        ",, Sir/Madam, OTHER_PARTY_REP_LETTER",
        "  ,  , Sir/Madam, OTHER_PARTY_REP_LETTER",
        "John, Smith, John Smith, OTHER_PARTY_REP_LETTER",
    })
    public void givenAnOtherPartyWithNameAndAddress_thenGeneratePlaceholders(@Nullable String firstName,
                                                                             @Nullable String lastName,
                                                                             String expectedName,
                                                                             FurtherEvidenceLetterType furtherEvidenceLetterType) {

        final Address otherPartyAddress = Address.builder().postcode("W1 1AA").build();
        SscsCaseData caseData = SscsCaseData.builder()
            .otherParties(Arrays.asList(buildOtherParty("1", Name.builder().firstName(firstName).lastName(lastName).build(), otherPartyAddress)))
            .appeal(Appeal.builder().build())
            .build();
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(caseData, furtherEvidenceLetterType, "1");
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertThat(actual.get("name"), is(expectedName));
        assertThat(otherPartyAddress, is(captor.getValue()));
    }

    private CcdValue<OtherParty> buildOtherParty(String id, Name name, Address address) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .name(name)
                .address(address)
                .build()).build();
    }

    @Test
    @Parameters({
        ",, Sir/Madam, OTHER_PARTY_LETTER",
        "  ,  , Sir/Madam, OTHER_PARTY_LETTER",
        "John, Smith, John Smith, OTHER_PARTY_LETTER",
        ",, Sir/Madam, OTHER_PARTY_REP_LETTER",
        "  ,  , Sir/Madam, OTHER_PARTY_REP_LETTER",
        "John, Smith, John Smith, OTHER_PARTY_REP_LETTER",
    })
    public void givenAnOtherPartyAppointeeWithNameAndAddress_thenGeneratePlaceholders(@Nullable String firstName,
                                                                                      @Nullable String lastName,
                                                                                      String expectedName,
                                                                                      FurtherEvidenceLetterType furtherEvidenceLetterType) {

        final Address otherPartyAddress = Address.builder().postcode("W1 1AA").build();
        SscsCaseData caseData = SscsCaseData.builder()
            .otherParties(Arrays.asList(buildOtherPartyAppointee("1", Name.builder().firstName(firstName).lastName(lastName).build(), otherPartyAddress)))
            .appeal(Appeal.builder().build())
            .build();
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(caseData, furtherEvidenceLetterType, "1");
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertThat(actual.get("name"), is(expectedName));
        assertThat(otherPartyAddress, is(captor.getValue()));
    }

    private CcdValue<OtherParty> buildOtherPartyAppointee(String id, Name name, Address address) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .appointee(
                    Appointee.builder()
                        .id(id)
                        .name(name)
                        .address(address)
                        .build())
                .build()).build();
    }

    @Test
    @Parameters({
        ",, Sir/Madam, OTHER_PARTY_LETTER",
        "  ,  , Sir/Madam, OTHER_PARTY_LETTER",
        "John, Smith, John Smith, OTHER_PARTY_LETTER",
        ",, Sir/Madam, OTHER_PARTY_REP_LETTER",
        "  ,  , Sir/Madam, OTHER_PARTY_REP_LETTER",
        "John, Smith, John Smith, OTHER_PARTY_REP_LETTER",
    })
    public void givenAnOtherPartyRepWithNameAndAddress_thenGeneratePlaceholders(@Nullable String firstName,
                                                                                @Nullable String lastName,
                                                                                String expectedName,
                                                                                FurtherEvidenceLetterType furtherEvidenceLetterType) {

        final Address otherPartyAddress = Address.builder().postcode("W1 1AA").build();
        SscsCaseData caseData = SscsCaseData.builder()
            .otherParties(Arrays.asList(buildOtherPartyRep("1", Name.builder().firstName(firstName).lastName(lastName).build(), otherPartyAddress)))
            .appeal(Appeal.builder().build())
            .build();
        Map<String, Object> actual = furtherEvidencePlaceholderService.populatePlaceholders(caseData, furtherEvidenceLetterType, "1");
        verify(placeholderService).build(any(), any(), captor.capture(), eq(null));

        assertThat(actual.get("name"), is(expectedName));
        assertThat(otherPartyAddress, is(captor.getValue()));
    }

    private CcdValue<OtherParty> buildOtherPartyRep(String id, Name name, Address address) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .rep(
                    Representative.builder()
                        .id(id)
                        .name(name)
                        .address(address)
                        .build())
                .build()).build();
    }
}
