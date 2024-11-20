package uk.gov.hmcts.reform.sscs.tyanotifications.personalisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.REPRESENTATIVE_NAME;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants;

public class WithRepresentativePersonalisationTest {

    private WithRepresentativePersonalisation withRepresentativePersonalisation =
        new WithRepresentativePersonalisation();

    @ParameterizedTest
    @MethodSource("generateSscsCaseDataForTest")
    public void givenSscsCaseData_shouldSetRepresentativeNameIfPresent(
        SscsCaseData sscsCaseData, String expected) {
        Map<String, Object> personalisation = withRepresentativePersonalisation.setRepresentativeName(
            new HashMap<>(), sscsCaseData);
        assertEquals(expected, personalisation.get(REPRESENTATIVE_NAME));
    }

    @SuppressWarnings({"unused"})
    private static Object[] generateSscsCaseDataForTest() {
        SscsCaseData sscsCaseDataWithRepsFlagYes = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .hasRepresentative("yes")
                    .name(Name.builder()
                        .firstName("Manish")
                        .lastName("Sharma")
                        .title("Mrs")
                        .build())
                    .build())
                .build())
            .build();
        SscsCaseData sscsCaseDataWithRepsFlagNo = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .hasRepresentative("no")
                    .name(Name.builder()
                        .firstName("Manish")
                        .lastName("Sharma")
                        .title("Mrs")
                        .build())
                    .build())
                .build())
            .build();
        SscsCaseData sscsCaseDataWithRepsOrgOnlyFlagYes = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .hasRepresentative("yes")
                    .name(Name.builder().build())
                    .organisation("organisation")
                    .build())
                .build())
            .build();
        SscsCaseData sscsCaseDataWithRepsOrgOnlyFlagNo = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .hasRepresentative("no")
                    .name(Name.builder().build())
                    .organisation("organisation")
                    .build())
                .build())
            .build();
        SscsCaseData sscsCaseDataWithNoReps = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(null)
                .build())
            .build();
        SscsCaseData sscsCaseDataWithEmptyReps = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder().build())
                .build())
            .build();
        SscsCaseData sscsCaseDataWithEmptyRepsAndEmptyNamesFlagYes = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .hasRepresentative("yes")
                    .name(Name.builder().build())
                    .build())
                .build())
            .build();
        return new Object[]{
            new Object[]{sscsCaseDataWithRepsFlagYes, "Manish Sharma"},
            new Object[]{sscsCaseDataWithRepsFlagNo, null},
            new Object[]{sscsCaseDataWithRepsOrgOnlyFlagYes, AppConstants.REP_SALUTATION},
            new Object[]{sscsCaseDataWithRepsOrgOnlyFlagNo, null},
            new Object[]{sscsCaseDataWithNoReps, null},
            new Object[]{sscsCaseDataWithEmptyReps, null},
            new Object[]{sscsCaseDataWithEmptyRepsAndEmptyNamesFlagYes, AppConstants.REP_SALUTATION}
        };
    }

}
