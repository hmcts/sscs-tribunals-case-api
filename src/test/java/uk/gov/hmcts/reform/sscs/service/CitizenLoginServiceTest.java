package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class CitizenLoginServiceTest extends AbstractCitizenLoginServiceTest {

    @Override
    boolean v2IsEnabled() {
        return false;
    }

    @Override
    void verifyFindAndUpdateCaseLastLoggedIntoMya(CcdService ccdService, UpdateCcdCaseService updateCcdCaseService, SscsCaseDetails expectedCase, long expectedCaseId, IdamTokens serviceIdamTokens) {
        verify(ccdService).updateCase(eq(expectedCase.getData()), eq(expectedCaseId), eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                anyString(), anyString(), eq(serviceIdamTokens));
    }
}