package uk.gov.hmcts.reform.sscs.functional.util;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.ESA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV2;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CreateCaseTest {

    private final CcdService ccdService;
    private final IdamService idamService;

    private IdamTokens idamTokens;

    @BeforeEach
    void setup() {
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    void shouldCreateAnEsaCase() {
        shouldCreateCaseWithCorrectData(ESA, State.VALID_APPEAL);
    }

    @Test
    void shouldCreateAUcCase() {
        shouldCreateCaseWithCorrectData(UC, State.VALID_APPEAL);
    }

    private void shouldCreateCaseWithCorrectData(Benefit benefit, State state) {
        final SscsCaseData caseData = parseJson();
        final SscsCaseDetails caseDetails = createCase(caseData);

        assertAppealState(caseDetails, state.getId());

        BenefitType benefitType = BenefitType.builder()
            .code(benefit.getShortName())
            .description(benefit.getDescription())
            .build();

        caseData.setWcaAppeal(YES);
        caseData.setBenefitCode(benefit.getBenefitCode());
        caseData.getAppeal().setBenefitType(benefitType);

        SscsCaseData updatedCaseData = updatedCaseData(caseData, caseDetails);

        assertThat(updatedCaseData.getWcaAppeal()).isEqualTo(YES);
        assertThat(updatedCaseData.getAppeal().getBenefitType()).isEqualTo(benefitType);
        assertThat(updatedCaseData.getBenefitCode()).isEqualTo(benefit.getBenefitCode());

        SscsCaseDetails fullUpdatedCase =
            ccdService.getByCaseId(Long.parseLong(updatedCaseData.getCcdCaseId()), idamTokens);

        assertThat(fullUpdatedCase.getState()).isEqualTo(state.toString());
        log.info("Case ID is {}", fullUpdatedCase.getData().getCcdCaseId());
    }

    private SscsCaseData updatedCaseData(SscsCaseData caseData, SscsCaseDetails caseDetails) {
        return ccdService.updateCase(
            caseData,
            caseDetails.getId(),
            UPDATE_CASE_ONLY.getCcdType(),
            "Update Case from functional test",
            "Test case",
            idamTokens
        ).getData();
    }

    private SscsCaseDetails createCase(SscsCaseData caseData) {
        return ccdService.createCase(
            caseData,
            CREATE_TEST_CASE.getCcdType(),
            "SSCS: Creating a Test Case from FT",
            "Test Case",
            idamTokens
        );
    }

    private void assertAppealState(SscsCaseDetails caseDetails, String expectedState) {
        assertThat(caseDetails).isNotNull();
        assertThat(caseDetails.getState())
            .isNotNull()
            .isEqualTo(expectedState);
    }

    private static SscsCaseData parseJson() {
        return convertSyaToCcdCaseDataV2(
            ALL_DETAILS.getDeserializeMessage(),
            false,
            new SscsCaseData()
        );
    }
}
