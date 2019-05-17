package uk.gov.hmcts.reform.sscs.functional.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.reform.sscs.functional.TestHelper.buildSscsCaseDataForTesting;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.EventService;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class EventServiceTest {

    @Autowired
    private CcdService ccdService;

    @Autowired
    private CcdClient ccdClient;

    @Autowired
    private IdamService idamService;

    @Autowired
    private EventService eventService;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void givenAValidAppealEvent_thenUpdateTheStateOfCaseToWithDwp() {
        SscsCaseData caseData = buildSscsCaseDataForTesting();
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated",
                "Appeal created summary", "Appeal created description",
                idamTokens);

        assertNotNull(caseDetails);
        caseData.setCaseReference("SC123/12/78765");
        caseData.setCcdCaseId(String.valueOf(caseDetails.getId()));

        eventService.handleEvent(EventType.VALID_APPEAL, caseData);
        assertEquals("withDwp", findStateOfCaseInCcd(caseDetails.getId()));
    }

    @Test
    public void givenAnInterlocValidAppealEvent_thenUpdateTheStateOfCaseToWithDwp() {
        SscsCaseData caseData = buildSscsCaseDataForTesting();
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated",
                "Appeal created summary", "Appeal created description",
                idamTokens);

        assertNotNull(caseDetails);
        caseData.setCaseReference("SC123/12/78765");
        caseData.setCcdCaseId(String.valueOf(caseDetails.getId()));

        eventService.handleEvent(EventType.INTERLOC_VALID_APPEAL, caseData);
        assertEquals("withDwp", findStateOfCaseInCcd(caseDetails.getId()));
    }

    public String findStateOfCaseInCcd(Long id) {
        return ccdService.getByCaseId(id, idamTokens).getState();
    }

}
