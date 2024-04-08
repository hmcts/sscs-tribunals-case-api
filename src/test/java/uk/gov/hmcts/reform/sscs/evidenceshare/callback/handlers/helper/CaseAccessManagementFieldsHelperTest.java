package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.FormType.SSCS1;
import static uk.gov.hmcts.reform.sscs.ccd.domain.FormType.SSCS5;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.helper.CaseAccessManagementFieldsHelper.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

class CaseAccessManagementFieldsHelperTest {

    @Test
    void shouldSetCategories_givenValidBenefitTypeCode() {
        SscsCaseData testCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("taxFreeChildcare")
                    .build())
                .build())
            .build();

        setCategories(testCaseData);

        assertThat(testCaseData.getCaseAccessManagementFields().getCaseAccessCategory()).isEqualTo("tax-freeChildcare");
        assertThat(testCaseData.getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode()).isEqualTo("taxFreeChildcare");
    }

    @Test
    void shouldNotSetCategories_givenBenefitTypeCodeIsNull() {
        SscsCaseData testCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().build())
            .build();

        setCategories(testCaseData);

        assertNull(testCaseData.getCaseAccessManagementFields().getCaseAccessCategory());
        assertNull(testCaseData.getCaseAccessManagementFields().getCaseManagementCategory());
    }

    @Test
    void shouldNotSetCategories_givenBenefitTypeCodeIsBlank() {
        SscsCaseData testCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("")
                    .build())
                .build())
            .build();

        setCategories(testCaseData);

        assertNull(testCaseData.getCaseAccessManagementFields().getCaseAccessCategory());
        assertNull(testCaseData.getCaseAccessManagementFields().getCaseManagementCategory());
    }

    @Test
    void shouldSetCaseNames_givenValidAppellantName() {
        SscsCaseData testCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .build())
                    .build())
                .build())
            .build();

        setCaseNames(testCaseData);

        assertThat(testCaseData.getCaseAccessManagementFields().getCaseNameHmctsInternal()).isEqualTo("John Doe");
        assertThat(testCaseData.getCaseAccessManagementFields().getCaseNameHmctsRestricted()).isEqualTo("John Doe");
        assertThat(testCaseData.getCaseAccessManagementFields().getCaseNamePublic()).isEqualTo("John Doe");
    }

    @Test
    void shouldNotSetCaseNames_givenAppellantNameIsNull() {
        SscsCaseData testCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder().build())
                .build())
            .build();

        setCaseNames(testCaseData);

        assertNull(testCaseData.getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertNull(testCaseData.getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertNull(testCaseData.getCaseAccessManagementFields().getCaseNamePublic());
    }

    @ParameterizedTest
    @CsvSource({
        "taxCredit,HMRC",
        "pensionCredit,DWP"
    })
    void shouldSetOgdType_givenValidBenefitType(String testCode, String expectedOgdType) {
        SscsCaseData testCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code(testCode)
                    .build())
                .build())
            .build();

        setOgdType(testCaseData);

        assertThat(testCaseData.getCaseAccessManagementFields().getOgdType()).isEqualTo(expectedOgdType);
    }

    @Test
    void shouldNotSetOgdType_givenAppealIsNull() {
        SscsCaseData testCaseData = SscsCaseData.builder().build();

        setOgdType(testCaseData);

        assertNull(testCaseData.getCaseAccessManagementFields().getOgdType());
    }

    @Test
    void shouldNotSetOgdType_givenBenefitTypeIsNull() {
        SscsCaseData testCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().build())
            .build();

        setOgdType(testCaseData);

        assertNull(testCaseData.getCaseAccessManagementFields().getOgdType());
    }

    @Test
    void shouldReturnTrue_givenValidAppellantName() {
        Appeal testAppeal = Appeal.builder()
            .appellant(Appellant.builder()
                .name(Name.builder()
                    .firstName("Lewis")
                    .lastName("Hamilton")
                    .build())
                .build())
            .build();

        assertTrue(hasAppellantName(testAppeal));
    }

    @Test
    void shouldReturnFalse_givenAppealIsNull() {
        assertFalse(hasAppellantName(null));
    }

    @Test
    void shouldReturnFalse_givenAppellantIsNull() {
        assertFalse(hasAppellantName(Appeal.builder().build()));
    }

    @Test
    void shouldReturnFalse_givenAppellantNameIsNull() {
        assertFalse(hasAppellantName(Appeal.builder()
            .appellant(Appellant.builder().build())
            .build()));
    }

    @Test
    void shouldReturnFalse_givenAppellantNamePropertiesAreNull() {
        assertFalse(hasAppellantName(Appeal.builder()
            .appellant(Appellant.builder()
                .name(Name.builder().build())
                .build())
            .build()));
    }

    @Test
    void shouldReturnFalse_givenAppellantFirstNameIsNull() {
        assertFalse(hasAppellantName(Appeal.builder()
            .appellant(Appellant.builder()
                .name(Name.builder()
                    .lastName("Verstappen")
                    .build())
                .build())
            .build()));
    }

    @Test
    void shouldReturnFalse_givenAppellantLastNameIsNull() {
        assertFalse(hasAppellantName(Appeal.builder()
            .appellant(Appellant.builder()
                .name(Name.builder()
                    .firstName("Max")
                    .build())
                .build())
            .build()));
    }

    @Test
    void shouldReturnTrue_givenHmrcBenefitType() {
        SscsCaseData testCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("taxCredit")
                    .build())
                .build())
            .build();

        assertTrue(isHmrcBenefit(testCaseData));
    }

    @Test
    void shouldReturnFalse_givenNonHmrcBenefitType() {
        SscsCaseData testCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("pensionCredit")
                    .build())
                .build())
            .build();

        assertFalse(isHmrcBenefit(testCaseData));
    }

    @Test
    void shouldReturnTrue_givenHmrcFormType() {
        SscsCaseData testCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("pensionCredit")
                    .build())
                .build())
            .formType(SSCS5)
            .build();

        assertFalse(isHmrcBenefit(testCaseData));
    }

    @Test
    void shouldReturnFalse_givenNonHmrcFormType() {
        SscsCaseData testCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("pensionCredit")
                    .build())
                .build())
            .formType(SSCS1)
            .build();

        assertFalse(isHmrcBenefit(testCaseData));
    }

}
