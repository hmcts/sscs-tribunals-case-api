package uk.gov.hmcts.sscs.functional.ccd;

import static org.junit.Assert.*;
import static uk.gov.hmcts.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS;
import static uk.gov.hmcts.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.exception.EmailSendFailedException;
import uk.gov.hmcts.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;
import uk.gov.hmcts.sscs.service.CcdService;
import uk.gov.hmcts.sscs.service.SubmitAppealService;
import uk.gov.hmcts.sscs.service.ccd.CaseDataUtils;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CreateCaseInCcdTest {

    @Autowired
    private CcdService ccdService;

    @Autowired
    private SubmitAppealService submitAppealService;

    @Test
    public void givenACaseShouldBeSavedIntoCcd() {
        CaseDetails caseDetails = ccdService.createCase(CaseDataUtils.buildCaseData());
        assertNotNull(caseDetails);
    }

    @Test
    public void givenASyaCaseShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        CaseData caseData = new SubmitYourAppealToCcdCaseDataDeserializer().convertSyaToCcdCaseData(syaCaseWrapper,
                rpc.getName(), rpc);
        CaseDetails caseDetails = ccdService.createCase(caseData);
        assertNotNull(caseDetails);
    }

    @Test
    @Ignore
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

        CaseData caseData = new SubmitYourAppealToCcdCaseDataDeserializer().convertSyaToCcdCaseData(syaCaseWrapper);
        CaseDetails caseDetails = ccdService.createCase(caseData);
        assertNotNull(caseDetails);
    }
}
