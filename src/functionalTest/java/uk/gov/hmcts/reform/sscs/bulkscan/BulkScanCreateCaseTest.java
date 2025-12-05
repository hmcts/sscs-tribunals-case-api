package uk.gov.hmcts.reform.sscs.bulkscan;

import static org.awaitility.Awaitility.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INCOMPLETE_APPLICATION_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.FormType.SSCS2;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.AWAIT_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INCOMPLETE_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import java.time.Duration;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@lombok.extern.slf4j.Slf4j
@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class BulkScanCreateCaseTest {

    private IdamTokens idamTokens;

    @Autowired
    private IdamService idamService;

    @Autowired
    private CcdService ccdService;

    @BeforeAll
    static void applySettings() {
        ignoreExceptionByDefault(AssertionFailedError.class);
        setDefaultTimeout(Duration.ofSeconds(60));
        setDefaultPollInterval(Duration.ofSeconds(1));
        setDefaultPollDelay(Duration.ZERO);
    }

    @BeforeEach
    void setUp() {
        idamTokens = idamService.getIdamTokens();
    }

    public SscsCaseDetails createPaperCase(
        EventType eventType,
        String benefitCode,
        String benefitDescription) {

        SscsCaseData minimalCaseData = CaseDataUtils.buildMinimalCaseData();

        SscsCaseData caseData = minimalCaseData.toBuilder()
            .appeal(minimalCaseData.getAppeal().toBuilder()
                .benefitType(BenefitType.builder()
                    .code(benefitCode)
                    .description(benefitDescription)
                    .build())
                .receivedVia("Paper")
                .build())
            .benefitCode(benefitCode)
            .formType(SSCS2)
            .build();

        //TODO could remove
        assertEquals("Paper", caseData.getAppeal().getReceivedVia());
        assertNotNull(caseData.getAppeal().getBenefitType());
        assertEquals(benefitCode, caseData.getAppeal().getBenefitType().getCode());

        return ccdService.createCase(caseData, eventType.getCcdType(),
            "FT createPaperCase Created this Case",
            "FT createPaperCase Created this Case",
            idamTokens);

    }

    @Test
    void shouldRetrieveCaseById() {

        SscsCaseDetails ccdCaseDetails = ccdService.getByCaseId(1764843894280640L, idamTokens);
        assertNotNull(ccdCaseDetails);

    }

    @Test
    void shouldCreateChildSupportCaseWithValidAppealState() {

        String benefitCode = Benefit.CHILD_SUPPORT.getBenefitCode();
        String benefitDescription = Benefit.CHILD_SUPPORT.getDescription();

        SscsCaseDetails sscsCaseDetails = createPaperCase(VALID_APPEAL_CREATED, benefitCode, benefitDescription);

        assertEquals(VALID_APPEAL.getId(), sscsCaseDetails.getState());

        //confirm in DB
        SscsCaseDetails ccdCaseDetails = ccdService.getByCaseId(sscsCaseDetails.getId(), idamTokens);
        assertEquals(VALID_APPEAL.getId(), ccdCaseDetails.getState());

        //confirm updated State on follow up event
        await().until(
            () -> {
                SscsCaseDetails updatedCaseDetails = ccdService.getByCaseId(sscsCaseDetails.getId(), idamTokens);
                log.info("Case State: {}", updatedCaseDetails.getState());
                return AWAIT_OTHER_PARTY_DATA.getId().equals(updatedCaseDetails.getState());
            }
        );

    }

    @Test
    void shouldCreateChildSupportCaseWithIncompleteState() {

        String benefitCode = Benefit.CHILD_SUPPORT.getBenefitCode();
        String benefitDescription = Benefit.CHILD_SUPPORT.getDescription();

        SscsCaseDetails sscsCaseDetails = createPaperCase(INCOMPLETE_APPLICATION_RECEIVED, benefitCode, benefitDescription);

        assertEquals(INCOMPLETE_APPLICATION.getId(), sscsCaseDetails.getState());

        SscsCaseDetails ccdCaseDetails = ccdService.getByCaseId(sscsCaseDetails.getId(), idamTokens);
        assertEquals(INCOMPLETE_APPLICATION.getId(), ccdCaseDetails.getState());
    }

    @Test
    void shouldCreateChildSupportCaseWithInterlocutoryReviewState() {

        String benefitCode = Benefit.CHILD_SUPPORT.getBenefitCode();
        String benefitDescription = Benefit.CHILD_SUPPORT.getDescription();

        final SscsCaseDetails sscsCaseDetails = createPaperCase(NON_COMPLIANT, benefitCode, benefitDescription);

        assertEquals(INTERLOCUTORY_REVIEW_STATE.getId(), sscsCaseDetails.getState());

        SscsCaseDetails ccdCaseDetails = ccdService.getByCaseId(sscsCaseDetails.getId(), idamTokens);
        assertEquals(INTERLOCUTORY_REVIEW_STATE.getId(), ccdCaseDetails.getState());
    }


}
