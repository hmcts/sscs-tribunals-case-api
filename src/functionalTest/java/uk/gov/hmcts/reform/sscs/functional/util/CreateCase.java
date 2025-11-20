package uk.gov.hmcts.reform.sscs.functional.util;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.ESA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV2;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@SpringBootTest
@Slf4j
public class CreateCase {

    @Autowired private CcdService ccdService;
    @Autowired private IdamService idamService;
    private IdamTokens idamTokens;

    @BeforeEach
    public void setup() {
        // Using the system user
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void shouldCreateEsaCaseWithCorrectData() {

        Benefit benefit = ESA;

        // Get and update test data
        SscsCaseData caseData =
            convertSyaToCcdCaseDataV2(ALL_DETAILS.getDeserializeMessage(), false, new SscsCaseData());

        // Create case in CCD and verify
        SscsCaseDetails caseDetails =
            ccdService.createCase(
                caseData,
                CREATE_TEST_CASE.getCcdType(),
                "SSCS: Creating a Test Case from FT",
                "Test Case",
                idamTokens);

        assertThat(caseDetails).isNotNull();
        assertThat(caseDetails.getState()).isNotNull().isEqualTo(State.VALID_APPEAL.getId());

        BenefitType benefitType =
            BenefitType.builder()
                .code(benefit.getShortName())
                .description(benefit.getDescription())
                .build();

        caseData.setWcaAppeal(YES);
        caseData.setBenefitCode(benefit.getBenefitCode());
        caseData.getAppeal().setBenefitType(benefitType);

        // Run another event to update the Case data and change the State
        SscsCaseData updatedCaseData =
            ccdService
                .updateCase(
                    caseData,
                    caseDetails.getId(),
                    UPDATE_CASE_ONLY
                        .getCcdType(), // The user triggering the event needs to have permissions
                    "Update Case from functional test",
                    "Test case",
                    idamTokens)
                .getData();

        assertThat(updatedCaseData.getWcaAppeal()).isEqualTo(YES);
        assertThat(updatedCaseData.getAppeal().getBenefitType()).isEqualTo(benefitType);
        assertThat(updatedCaseData.getBenefitCode()).isEqualTo(benefit.getBenefitCode());

        SscsCaseDetails fullUpdatedCase =
            ccdService.getByCaseId(Long.valueOf(updatedCaseData.getCcdCaseId()), idamTokens);

        assertThat(fullUpdatedCase.getState()).isEqualTo(State.VALID_APPEAL.getId());

        log.info("Case ID is {}", fullUpdatedCase.getData().getCcdCaseId());

    }

    @Test
    public void shouldCreateUcCaseWithCorrectData() {

        Benefit benefit = UC;

        // Get and update test data
        SscsCaseData caseData =
            convertSyaToCcdCaseDataV2(ALL_DETAILS.getDeserializeMessage(), false, new SscsCaseData());

        // Create case in CCD and verify
        SscsCaseDetails caseDetails =
            ccdService.createCase(
                caseData,
                CREATE_TEST_CASE.getCcdType(),
                "SSCS: Creating a Test Case from FT",
                "Test Case",
                idamTokens);

        assertThat(caseDetails).isNotNull();
        assertThat(caseDetails.getState()).isNotNull().isEqualTo(State.VALID_APPEAL.getId());

        BenefitType benefitType =
            BenefitType.builder()
                .code(benefit.getShortName())
                .description(benefit.getDescription())
                .build();

        caseData.setWcaAppeal(YES);
        caseData.setBenefitCode(benefit.getBenefitCode());
        caseData.getAppeal().setBenefitType(benefitType);

        // Run another event to update the Case data and change the State
        SscsCaseData updatedCaseData =
            ccdService
                .updateCase(
                    caseData,
                    caseDetails.getId(),
                    UPDATE_CASE_ONLY
                        .getCcdType(), // The user triggering the event needs to have permissions
                    "Update Case from functional test",
                    "Test case",
                    idamTokens)
                .getData();

        assertThat(updatedCaseData.getWcaAppeal()).isEqualTo(YES);
        assertThat(updatedCaseData.getAppeal().getBenefitType()).isEqualTo(benefitType);
        assertThat(updatedCaseData.getBenefitCode()).isEqualTo(benefit.getBenefitCode());

        SscsCaseDetails fullUpdatedCase =
            ccdService.getByCaseId(Long.valueOf(updatedCaseData.getCcdCaseId()), idamTokens);

        assertThat(fullUpdatedCase.getState()).isEqualTo(State.VALID_APPEAL.getId());

        log.info("Case ID is {}", fullUpdatedCase.getData().getCcdCaseId());

    }

}

