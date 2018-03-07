package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.hmcts.sscs.domain.corecase.Appeal;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.exception.CcdException;

@RunWith(MockitoJUnitRunner.class)
public class AppealNumberGeneratorTest {

    @Mock
    private CcdService ccdService;

    private AppealNumberGenerator appealNumberGenerator;

    @Before
    public void setup() {
        appealNumberGenerator = new AppealNumberGenerator(ccdService);
    }

    @Test
    public void shouldGenerateAppealNumberWhenCcdReturnNullObject() throws CcdException {

        given(ccdService.findCcdCaseDetailsByAppealNumber(anyString())).willReturn(null);

        String appealNumber = appealNumberGenerator.generate();

        verify(ccdService).findCcdCaseDetailsByAppealNumber(anyString());

        assertNotNull(appealNumber);
    }

    @Test
    public void shouldGenerateAppealNumberWhenCcdReturnEmptyObject() throws CcdException {

        given(ccdService.findCcdCaseDetailsByAppealNumber(anyString())).willReturn(new CcdCase());

        String appealNumber = appealNumberGenerator.generate();

        verify(ccdService).findCcdCaseDetailsByAppealNumber(anyString());

        assertNotNull(appealNumber);
    }

    @Test
    public void shouldGenerateAppealNumberInSecondAttempt() throws Exception {

        Appeal appeal = new Appeal();
        appeal.setAppealNumber("abcd1efgh2");
        CcdCase ccdCase = new CcdCase();
        ccdCase.setAppeal(appeal);

        when(ccdService.findCcdCaseDetailsByAppealNumber(anyString())).thenReturn(ccdCase, null);

        appealNumberGenerator.generate();

        verify(ccdService, times(2)).findCcdCaseDetailsByAppealNumber(anyString());
    }

    @Test
    public void shouldGenerateAppealNumberInThirdAttempt() throws Exception {

        Appeal appeal = new Appeal();
        appeal.setAppealNumber("abcd1efgh2");
        CcdCase ccdCase = new CcdCase();
        ccdCase.setAppeal(appeal);

        when(ccdService.findCcdCaseDetailsByAppealNumber(anyString())).thenReturn(ccdCase, ccdCase, null);

        appealNumberGenerator.generate();

        verify(ccdService, times(3)).findCcdCaseDetailsByAppealNumber(anyString());
    }

    @Test(expected = CcdException.class)
    public void shouldThrowExceptionTryingThreeAttempts() throws Exception {

        Appeal appeal = new Appeal();
        appeal.setAppealNumber("abcd1efgh2");
        CcdCase ccdCase = new CcdCase();
        ccdCase.setAppeal(appeal);

        given(ccdService.findCcdCaseDetailsByAppealNumber(anyString())).willReturn(ccdCase);

        appealNumberGenerator.generate();
    }
}