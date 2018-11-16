package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.EmailSendFailedException;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;
import uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CreateCaseInCcdTest {

    @Autowired
    private CcdService ccdService;

    @Autowired
    private CcdClient ccdClient;

    @Autowired
    private CcdRequestDetails ccdRequestDetails;

    @Autowired
    private IdamService idamService;

    @Autowired
    private SubmitAppealService submitAppealService;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void givenACaseShouldBeSavedIntoCcd() {
        SscsCaseDetails caseDetails = ccdService.createCase(CaseDataUtils.buildCaseData(), idamTokens);
        assertNotNull(caseDetails);
    }

    @Test
    public void givenASyaCaseShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        SscsCaseData caseData = new SubmitYourAppealToCcdCaseDataDeserializer().convertSyaToCcdCaseData(syaCaseWrapper,
                rpc.getName(), rpc);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, idamTokens);
        assertNotNull(caseDetails);
    }

    @Test
    public void givenASyaCaseShouldBeSavedIntoCcdViaSubmitAppealService() {
        try {
            submitAppealService.submitAppeal(ALL_DETAILS.getDeserializeMessage());
        } catch (EmailSendFailedException | PdfGenerationException ep) {
            assertTrue(true);
        } catch (Exception ex) {
            fail();
        }

    }

    @Test
    public void givenASyaCaseWithoutAMatchingRpcShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();

        SscsCaseData caseData = new SubmitYourAppealToCcdCaseDataDeserializer().convertSyaToCcdCaseData(syaCaseWrapper);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, idamTokens);
        assertNotNull(caseDetails);
    }
}
