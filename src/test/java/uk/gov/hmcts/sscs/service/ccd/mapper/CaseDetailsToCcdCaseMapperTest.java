package uk.gov.hmcts.sscs.service.ccd.mapper;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.service.ccd.CaseDataUtils;

public class CaseDetailsToCcdCaseMapperTest {

    private CaseDetailsToCcdCaseMapper caseDetailsToCcdCaseMapper;

    @Before
    public void setUp() {
        caseDetailsToCcdCaseMapper = new CaseDetailsToCcdCaseMapper();
    }

    @Test
    public void givenCaseDetails_shouldReturnCcdCase() {
        CcdCase ccdCase = caseDetailsToCcdCaseMapper.map(CaseDataUtils.buildCaseDetails());
        assertEquals(getCcdCase(), ccdCase);
    }

    private CcdCase getCcdCase() {
        CcdCase ccdCase = new CcdCase();
        ccdCase.setCaseReference("SC068/17/00013");
        ccdCase.setBenefitType("1325");
        return ccdCase;
    }
}
