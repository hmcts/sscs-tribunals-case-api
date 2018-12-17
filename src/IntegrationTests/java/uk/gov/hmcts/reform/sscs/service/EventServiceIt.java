package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.model.NotificationEventType.CREATE_APPEAL_PDF;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EventServiceIt {

    @Autowired
    private CcdService ccdService;

    @Autowired
    private CcdClient ccdClient;

    @Autowired
    private SscsCcdConvertService sscsCcdConvertService;

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
    public void givenACaseWithoutPdfShouldCreateAppealPdf() {
        SscsCaseDetails caseDetails = ccdService.createCase(createAppealPdfCaseData(), idamTokens);
        assertNull(caseDetails.getData().getSscsDocument());

        eventService.handleEvent(CREATE_APPEAL_PDF, extractCaseData(caseDetails));

        SscsCaseDetails updatedCaseDetails = findCaseInCcd(caseDetails.getId());

        assertNotNull(updatedCaseDetails.getData().getSscsDocument());
        assertEquals(1, updatedCaseDetails.getData().getSscsDocument().size());
    }

    private SscsCaseData extractCaseData(SscsCaseDetails caseDetails) {
        return caseDetails.getData().toBuilder().ccdCaseId(caseDetails.getId().toString()).build();
    }

    private SscsCaseData createAppealPdfCaseData() {
        SscsCaseData caseData = CaseDataUtils.buildCaseData();
        Appointee appointee = Appointee.builder()
                .name(Name.builder()
                        .firstName("Oscar")
                        .lastName("Giles")
                        .build())
                .address(caseData.getAppeal().getAppellant().getAddress())
                .build();
        caseData.getAppeal().getAppellant().setAppointee(appointee);
        return caseData;
    }

    private SscsCaseDetails findCaseInCcd(Long caseId) {
        return sscsCcdConvertService.getCaseDetails(ccdClient.readForCaseworker(idamTokens, caseId));
    }

}

